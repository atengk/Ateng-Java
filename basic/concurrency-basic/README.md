# 并发基础

并发基础主要解决 Java 程序中“多个任务如何同时推进”的问题。实际开发中，并发常用于提升接口吞吐量、减少阻塞等待、提高 CPU 利用率，以及处理异步任务、批量任务、定时任务和高并发请求。

## 并发编程概述

本节用于建立并发编程的基本概念，重点区分并发与并行、线程与进程，并说明 Java 并发编程在实际业务系统中的典型使用场景。

### 并发与并行

并发和并行都描述多个任务的执行关系，但二者关注点不同。

并发是指多个任务在同一时间段内交替执行。对于单核 CPU 来说，同一时刻只能执行一个任务，但 CPU 可以通过时间片轮转在多个任务之间快速切换，从宏观上看像是多个任务同时运行。

并行是指多个任务在同一时刻真正同时执行。并行通常依赖多核 CPU，不同任务可以分配到不同 CPU 核心上同时运行。

| 对比项 | 并发                            | 并行                          |
| ------ | ------------------------------- | ----------------------------- |
| 含义   | 多个任务在同一时间段内推进      | 多个任务在同一时刻同时执行    |
| 依赖   | 可以在单核或多核 CPU 上发生     | 通常依赖多核 CPU              |
| 重点   | 任务调度与切换                  | 任务同时执行                  |
| 示例   | 一个 CPU 在多个线程之间切换执行 | 多个 CPU 核心同时执行多个线程 |

在 Java 中，多线程既可以实现并发，也可能实现并行。是否真正并行，取决于 CPU 核心数、操作系统调度、JVM 线程调度以及任务本身是否具备并行执行条件。

### 线程与进程

进程是操作系统资源分配的基本单位。一个正在运行的 Java 程序通常对应一个 JVM 进程，该进程拥有独立的内存空间、文件句柄、网络连接等系统资源。

线程是 CPU 调度的基本单位。一个进程内部可以包含多个线程，多个线程共享进程的堆内存、方法区等资源，但每个线程拥有独立的程序计数器、虚拟机栈和本地方法栈。

| 对比项   | 进程                           | 线程                         |
| -------- | ------------------------------ | ---------------------------- |
| 定义     | 正在运行的程序实例             | 进程内部的执行单元           |
| 资源     | 拥有独立内存空间               | 共享进程资源                 |
| 创建成本 | 较高                           | 较低                         |
| 通信方式 | 进程间通信成本较高             | 线程间可通过共享变量通信     |
| 隔离性   | 隔离性强                       | 隔离性弱                     |
| 稳定性   | 一个进程崩溃一般不影响其他进程 | 一个线程异常可能影响整个进程 |

Java 中的 `main` 方法本身运行在主线程中。开发者创建的子线程与主线程属于同一个 JVM 进程，它们可以共享对象、集合、连接池等资源，因此并发编程必须重点关注线程安全问题。

### Java 并发编程的使用场景

Java 并发编程常用于需要提升处理效率、减少等待时间或异步解耦的场景。并发不是越多越好，线程数量过多会带来上下文切换、锁竞争、内存占用和排查复杂度上升等问题。

常见使用场景包括：

| 场景           | 说明                                        |
| -------------- | ------------------------------------------- |
| Web 请求处理   | Tomcat、Undertow 等容器通过线程处理多个请求 |
| 批量数据处理   | 多线程拆分大批量数据，提高处理速度          |
| 异步任务       | 下单后异步发送短信、邮件、站内信            |
| 定时任务       | 定时同步数据、清理临时文件、生成报表        |
| IO 密集型任务  | 文件读写、接口调用、数据库访问、MQ 消费     |
| CPU 密集型任务 | 图片处理、加解密、复杂计算、数据压缩        |
| 任务编排       | 多个接口并行调用后聚合结果                  |
| 消息消费       | 多线程消费 MQ 消息，提高消费吞吐量          |

并发编程适合处理可以拆分、可以异步、存在等待或可并行计算的任务。如果任务之间强依赖顺序，或者共享状态非常复杂，盲目使用多线程反而会增加系统风险。

## 线程基础

本节介绍 Java 创建和使用线程的基础方式，包括 `Thread`、`Runnable`、`Callable` 和 `Future`。实际项目中不建议频繁手动创建线程，更推荐通过线程池统一管理线程生命周期，但理解这些基础 API 是学习线程池和异步编程的前提。

如果示例项目已经引入 Hutool，可以使用 `ThreadUtil.sleep()` 简化休眠代码；未引入 Hutool 时，可替换为 JDK 原生的 `Thread.sleep()`。

可选依赖如下，用于示例中的 Hutool 工具类。

```xml
<!-- Hutool 工具包：用于简化线程休眠、字符串处理、集合处理等常见操作 -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.36</version>
</dependency>
```

### Thread

`Thread` 是 Java 中表示线程的基础类。创建线程的一种方式是继承 `Thread` 类，并重写 `run()` 方法。调用 `start()` 方法后，JVM 会创建新的线程并执行 `run()` 中的逻辑。

需要注意，直接调用 `run()` 方法不会启动新线程，它只是一次普通方法调用。只有调用 `start()` 方法，才会真正启动新的线程。

下面的示例演示继承 `Thread` 创建线程，并对比 `start()` 和 `run()` 的区别。

文件位置：`src/main/java/io/github/atengk/concurrent/basic/ThreadBasicDemo.java`

```java
package io.github.atengk.concurrent.basic;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Thread 基础使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ThreadBasicDemo {

    public static void main(String[] args) {
        Thread workerThread = new WorkerThread();

        log.info("主线程开始执行，线程名称：{}", Thread.currentThread().getName());

        // start() 会启动一个新的线程执行 run() 方法
        workerThread.start();

        // run() 是普通方法调用，不会启动新线程
        new WorkerThread().run();

        log.info("主线程继续执行，线程名称：{}", Thread.currentThread().getName());
    }

    /**
     * 自定义线程
     *
     * @author Ateng
     * @since 2026-05-15
     */
    static class WorkerThread extends Thread {

        /**
         * 执行线程任务
         */
        @Override
        public void run() {
            log.info("当前任务开始执行，线程名称：{}", Thread.currentThread().getName());
            ThreadUtil.sleep(1000);
            log.info("当前任务执行完成，线程名称：{}", Thread.currentThread().getName());
        }
    }
}
```

`Thread` 方式结构直观，但缺点也明显：Java 是单继承语言，继承了 `Thread` 后就无法再继承其他类；同时任务逻辑和线程对象绑定较紧，不利于复用。

### Runnable

`Runnable` 是一个函数式接口，只有一个 `run()` 方法。它表示一个没有返回值、不能直接抛出受检异常的任务。

相比继承 `Thread`，实现 `Runnable` 更推荐，因为它将“任务”和“线程”分离。任务负责描述要做什么，线程负责执行任务。

下面的示例演示使用 `Runnable` 创建线程，并通过 Lambda 简化写法。

文件位置：`src/main/java/io/github/atengk/concurrent/basic/RunnableDemo.java`

```java
package io.github.atengk.concurrent.basic;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Runnable 基础使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class RunnableDemo {

    public static void main(String[] args) {
        Runnable orderTask = new OrderRunnable("ORDER-1001");
        Thread orderThread = new Thread(orderTask, "order-worker-1");
        orderThread.start();

        Thread messageThread = new Thread(() -> {
            log.info("开始发送消息通知，线程名称：{}", Thread.currentThread().getName());
            ThreadUtil.sleep(800);
            log.info("消息通知发送完成，线程名称：{}", Thread.currentThread().getName());
        }, "message-worker-1");

        messageThread.start();
    }

    /**
     * 订单处理任务
     *
     * @author Ateng
     * @since 2026-05-15
     */
    static class OrderRunnable implements Runnable {

        private final String orderNo;

        public OrderRunnable(String orderNo) {
            this.orderNo = orderNo;
        }

        /**
         * 执行订单处理逻辑
         */
        @Override
        public void run() {
            log.info("开始处理订单，订单号：{}，线程名称：{}", orderNo, Thread.currentThread().getName());
            ThreadUtil.sleep(1000);
            log.info("订单处理完成，订单号：{}，线程名称：{}", orderNo, Thread.currentThread().getName());
        }
    }
}
```

`Runnable` 适合执行不需要返回结果的异步任务，例如日志记录、消息发送、缓存预热、文件清理等。

### Callable

`Callable` 也是用于描述任务的接口，但它比 `Runnable` 更强大。`Callable` 的 `call()` 方法可以返回结果，也可以抛出异常。

`Callable` 通常不会直接交给 `Thread` 执行，而是配合线程池的 `submit()` 方法使用。由于这里还没有展开线程池，可以先通过 `FutureTask` 来理解 `Callable` 的执行方式。

下面的示例演示使用 `Callable` 返回计算结果。

文件位置：`src/main/java/io/github/atengk/concurrent/basic/CallableDemo.java`

```java
package io.github.atengk.concurrent.basic;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Callable 基础使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class CallableDemo {

    public static void main(String[] args) {
        Callable<Integer> statisticsTask = () -> {
            log.info("开始统计订单数量，线程名称：{}", Thread.currentThread().getName());
            ThreadUtil.sleep(1000);
            return 120;
        };

        FutureTask<Integer> futureTask = new FutureTask<>(statisticsTask);
        Thread thread = new Thread(futureTask, "statistics-worker-1");
        thread.start();

        try {
            Integer count = futureTask.get();
            log.info("订单数量统计完成，统计结果：{}", count);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待统计任务时线程被中断", e);
        } catch (ExecutionException e) {
            log.error("统计任务执行异常", e);
        }
    }
}
```

`Callable` 适合需要异步计算结果的场景，例如并行查询多个接口、批量计算金额、生成统计结果、异步加载配置等。

### Future

`Future` 用于表示异步任务的执行结果。通过 `Future` 可以判断任务是否完成、取消任务、阻塞获取结果以及处理任务异常。

常用方法如下：

| 方法                                    | 说明                                 |
| --------------------------------------- | ------------------------------------ |
| `get()`                                 | 阻塞等待任务完成并获取结果           |
| `get(timeout, unit)`                    | 在指定时间内等待结果，超时会抛出异常 |
| `isDone()`                              | 判断任务是否完成                     |
| `isCancelled()`                         | 判断任务是否被取消                   |
| `cancel(boolean mayInterruptIfRunning)` | 尝试取消任务                         |

下面的示例演示 `FutureTask` 的超时获取和取消操作。

文件位置：`src/main/java/io/github/atengk/concurrent/basic/FutureDemo.java`

```java
package io.github.atengk.concurrent.basic;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Future 基础使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class FutureDemo {

    public static void main(String[] args) {
        Callable<String> remoteQueryTask = () -> {
            log.info("开始调用远程接口，线程名称：{}", Thread.currentThread().getName());
            ThreadUtil.sleep(3000);
            return "远程接口返回成功";
        };

        FutureTask<String> futureTask = new FutureTask<>(remoteQueryTask);
        Thread thread = new Thread(futureTask, "remote-query-worker-1");
        thread.start();

        try {
            String result = futureTask.get(1, TimeUnit.SECONDS);
            log.info("获取异步任务结果：{}", result);
        } catch (TimeoutException e) {
            boolean cancelled = futureTask.cancel(true);
            log.warn("获取异步任务结果超时，已尝试取消任务，取消结果：{}", cancelled);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待异步任务结果时线程被中断", e);
        } catch (Exception e) {
            log.error("异步任务执行异常", e);
        }

        log.info("任务是否完成：{}，任务是否取消：{}", futureTask.isDone(), futureTask.isCancelled());
    }
}
```

`Future` 的主要问题是 `get()` 会阻塞当前线程，并且多个异步任务之间的编排能力较弱。实际业务中，如果需要复杂的异步编排，更推荐使用后续章节中的 `CompletableFuture`。





## 线程生命周期

线程生命周期描述线程从创建到结束的完整状态变化。Java 使用 `Thread.State` 枚举表示线程状态，包括 `NEW`、`RUNNABLE`、`BLOCKED`、`WAITING`、`TIMED_WAITING` 和 `TERMINATED`。

需要注意，Java 层面的 `RUNNABLE` 同时包含操作系统层面的“就绪”和“运行”两类状态。也就是说，线程只要已经具备运行条件，在 JVM 中通常就会表现为 `RUNNABLE`，至于它当前是否真正占用 CPU 执行，由操作系统调度决定。

### 新建状态

新建状态对应 `NEW`。当线程对象被创建，但还没有调用 `start()` 方法时，线程处于新建状态。

此时线程只是一个普通 Java 对象，还没有被操作系统调度，也没有真正启动执行。

```java
Thread thread = new Thread(() -> {
    System.out.println("线程执行中");
});

System.out.println(thread.getState()); // NEW
```

新建状态的典型特点是：

| 特点             | 说明                                         |
| ---------------- | -------------------------------------------- |
| 是否启动         | 未启动                                       |
| 是否分配 CPU     | 未分配                                       |
| 是否执行 `run()` | 未执行                                       |
| 常见操作         | 创建线程对象、设置线程名称、设置是否守护线程 |

线程只能调用一次 `start()` 方法。一个已经启动过的线程，即使执行结束，也不能再次调用 `start()`，否则会抛出 `IllegalThreadStateException`。

### 就绪与运行状态

调用 `start()` 方法后，线程进入可运行状态。在 Java 中，这个状态统一表示为 `RUNNABLE`。

从操作系统角度看，`RUNNABLE` 可以细分为就绪和运行：

| 状态 | 说明                                    |
| ---- | --------------------------------------- |
| 就绪 | 线程已经具备运行条件，正在等待 CPU 调度 |
| 运行 | 线程正在占用 CPU 执行任务               |

Java 的 `Thread.State` 不区分就绪和运行，它们都属于 `RUNNABLE`。因此，看到线程状态为 `RUNNABLE` 时，只能说明线程可以运行或正在运行，不能精确判断它是否正在占用 CPU。

常见进入 `RUNNABLE` 的方式包括：

```java
thread.start();
```

或者线程从阻塞、等待、限时等待状态恢复后，也可能重新进入 `RUNNABLE`。

### 阻塞与等待状态

阻塞与等待都表示线程暂时无法继续向下执行，但原因不同。

`BLOCKED` 表示线程正在等待获取 `synchronized` 监视器锁。它通常发生在多个线程竞争同一把对象锁时。

`WAITING` 表示线程进入无限期等待，需要其他线程显式唤醒。例如调用 `Object.wait()`、`Thread.join()` 或 `LockSupport.park()`。

`TIMED_WAITING` 表示线程进入限时等待，到达指定时间后可以自动恢复。例如调用 `Thread.sleep()`、`Object.wait(timeout)`、`Thread.join(timeout)`。

| Java 状态       | 触发场景                                    | 是否自动恢复     |
| --------------- | ------------------------------------------- | ---------------- |
| `BLOCKED`       | 等待进入 `synchronized` 临界区              | 获取锁后恢复     |
| `WAITING`       | `wait()`、`join()`、`LockSupport.park()`    | 需要其他线程唤醒 |
| `TIMED_WAITING` | `sleep()`、`wait(timeout)`、`join(timeout)` | 超时后可恢复     |

下面的示例演示 `NEW`、`BLOCKED`、`WAITING`、`TIMED_WAITING` 和 `TERMINATED` 等常见状态。

文件位置：`src/main/java/io/github/atengk/concurrent/lifecycle/ThreadStateDemo.java`

```java
package io.github.atengk.concurrent.lifecycle;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.LockSupport;

/**
 * 线程生命周期状态示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ThreadStateDemo {

    private static final Object LOCK = new Object();

    public static void main(String[] args) throws InterruptedException {
        showNewAndTerminatedState();
        showTimedWaitingState();
        showBlockedState();
        showWaitingState();
    }

    private static void showNewAndTerminatedState() throws InterruptedException {
        Thread thread = new Thread(() -> log.info("线程开始执行"), "state-new-worker");

        log.info("线程创建后状态：{}", thread.getState());

        thread.start();
        thread.join();

        log.info("线程执行结束后状态：{}", thread.getState());
    }

    private static void showTimedWaitingState() throws InterruptedException {
        Thread thread = new Thread(() -> {
            log.info("线程进入限时等待");
            ThreadUtil.sleep(2000);
        }, "state-sleep-worker");

        thread.start();
        ThreadUtil.sleep(300);

        log.info("线程 sleep 时状态：{}", thread.getState());

        thread.join();
    }

    private static void showBlockedState() throws InterruptedException {
        Thread holderThread = new Thread(() -> {
            synchronized (LOCK) {
                log.info("线程已获取锁，准备持有锁一段时间");
                ThreadUtil.sleep(2000);
            }
        }, "lock-holder-worker");

        Thread blockedThread = new Thread(() -> {
            synchronized (LOCK) {
                log.info("线程获取锁成功");
            }
        }, "lock-blocked-worker");

        holderThread.start();
        ThreadUtil.sleep(300);

        blockedThread.start();
        ThreadUtil.sleep(300);

        log.info("线程等待 synchronized 锁时状态：{}", blockedThread.getState());

        holderThread.join();
        blockedThread.join();
    }

    private static void showWaitingState() throws InterruptedException {
        Thread waitingThread = new Thread(() -> {
            log.info("线程进入无限等待");
            LockSupport.park();
            log.info("线程被唤醒后继续执行");
        }, "state-waiting-worker");

        waitingThread.start();
        ThreadUtil.sleep(300);

        log.info("线程 park 后状态：{}", waitingThread.getState());

        LockSupport.unpark(waitingThread);
        waitingThread.join();
    }
}
```

### 终止状态

线程执行完成后会进入终止状态，对应 `TERMINATED`。

线程进入终止状态的常见原因包括：

| 原因                     | 说明                 |
| ------------------------ | -------------------- |
| `run()` 方法正常执行完成 | 最常见的结束方式     |
| 任务代码抛出未捕获异常   | 线程异常退出         |
| 线程被中断后主动结束     | 推荐的协作式停止方式 |

不建议使用 `Thread.stop()` 强制停止线程。该方法已经被废弃，因为它可能在任意位置终止线程，导致锁释放、共享数据不一致或资源未正确关闭。

推荐使用中断机制控制线程结束。线程中通过 `isInterrupted()` 判断中断标记，在合适的位置主动退出。

文件位置：`src/main/java/io/github/atengk/concurrent/lifecycle/ThreadInterruptDemo.java`

```java
package io.github.atengk.concurrent.lifecycle;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 线程中断退出示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ThreadInterruptDemo {

    public static void main(String[] args) throws InterruptedException {
        Thread workerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                log.info("线程正在执行任务，线程名称：{}", Thread.currentThread().getName());
                ThreadUtil.sleep(500);
            }

            log.warn("检测到中断标记，线程准备退出，线程名称：{}", Thread.currentThread().getName());
        }, "interrupt-worker");

        workerThread.start();

        ThreadUtil.sleep(2000);
        workerThread.interrupt();

        workerThread.join();

        log.info("线程最终状态：{}", workerThread.getState());
    }
}
```

## 线程池

线程池用于统一管理线程创建、复用、调度和销毁。生产环境中，不建议频繁手动 `new Thread()`，而应该通过线程池控制线程数量和任务队列，避免线程无限创建导致系统资源耗尽。

### 线程池的作用

线程池的核心作用是复用线程和控制并发规模。

如果每个任务都新建一个线程，会带来以下问题：

| 问题           | 说明                                    |
| -------------- | --------------------------------------- |
| 创建销毁成本高 | 线程创建和销毁都需要系统资源            |
| 上下文切换频繁 | 线程过多会导致 CPU 时间浪费在线程切换上 |
| 缺少统一管理   | 不方便监控、关闭、命名和异常处理        |
| 容易耗尽资源   | 高并发下可能创建大量线程，导致 OOM      |

线程池可以解决这些问题：

| 作用     | 说明                                                 |
| -------- | ---------------------------------------------------- |
| 线程复用 | 核心线程可长期存活，减少创建销毁成本                 |
| 控制并发 | 通过核心线程数、最大线程数和队列控制任务执行规模     |
| 任务排队 | 任务过多时进入阻塞队列等待执行                       |
| 拒绝保护 | 任务超出承载能力时触发拒绝策略                       |
| 统一监控 | 可以统计线程池活跃线程数、队列长度、完成任务数等指标 |

### ThreadPoolExecutor

`ThreadPoolExecutor` 是 Java 线程池的核心实现类。相比 `Executors` 工具类，直接使用 `ThreadPoolExecutor` 可以明确配置线程数量、队列大小、线程工厂和拒绝策略，更适合生产环境。

下面是一个推荐的线程池创建示例。

文件位置：`src/main/java/io/github/atengk/concurrent/pool/ThreadPoolExecutorDemo.java`

```java
package io.github.atengk.concurrent.pool;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ThreadPoolExecutor 基础使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ThreadPoolExecutorDemo {

    public static void main(String[] args) {
        AtomicInteger threadIndex = new AtomicInteger(1);

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,
                4,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("business-worker-" + threadIndex.getAndIncrement());
                    thread.setUncaughtExceptionHandler((t, e) -> log.error("线程执行异常，线程名称：{}", t.getName(), e));
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        try {
            for (int i = 1; i <= 12; i++) {
                int taskNo = i;
                executor.execute(() -> {
                    log.info("开始执行任务，任务编号：{}，线程名称：{}", taskNo, Thread.currentThread().getName());
                    ThreadUtil.sleep(1000);
                    log.info("任务执行完成，任务编号：{}，线程名称：{}", taskNo, Thread.currentThread().getName());
                });
            }
        } finally {
            executor.shutdown();
            log.info("线程池已发起关闭");
        }
    }
}
```

这个示例中，线程池最多允许 4 个线程同时工作，队列最多缓存 10 个任务。如果任务继续增加，会触发拒绝策略。使用自定义线程工厂可以为线程设置业务化名称，便于日志定位和问题排查。

### 核心参数

`ThreadPoolExecutor` 的核心构造参数如下：

```java
public ThreadPoolExecutor(
        int corePoolSize,
        int maximumPoolSize,
        long keepAliveTime,
        TimeUnit unit,
        BlockingQueue<Runnable> workQueue,
        ThreadFactory threadFactory,
        RejectedExecutionHandler handler
)
```

各参数含义如下：

| 参数              | 说明                                                   |
| ----------------- | ------------------------------------------------------ |
| `corePoolSize`    | 核心线程数，线程池长期保留的基础线程数量               |
| `maximumPoolSize` | 最大线程数，线程池允许创建的最大线程数量               |
| `keepAliveTime`   | 非核心线程空闲存活时间                                 |
| `unit`            | `keepAliveTime` 的时间单位                             |
| `workQueue`       | 任务阻塞队列                                           |
| `threadFactory`   | 线程工厂，用于自定义线程名称、是否守护线程、异常处理器 |
| `handler`         | 拒绝策略，当线程和队列都满时触发                       |

线程池任务执行流程可以简化理解为：

1. 当前线程数小于 `corePoolSize`，优先创建核心线程执行任务。
2. 当前线程数达到 `corePoolSize` 后，新任务进入阻塞队列。
3. 队列满后，如果当前线程数小于 `maximumPoolSize`，继续创建非核心线程。
4. 当前线程数达到 `maximumPoolSize` 且队列已满，触发拒绝策略。

常用阻塞队列包括：

| 队列                    | 说明                                     |
| ----------------------- | ---------------------------------------- |
| `ArrayBlockingQueue`    | 有界数组队列，推荐用于生产环境，容量明确 |
| `LinkedBlockingQueue`   | 链表队列，可有界也可近似无界             |
| `SynchronousQueue`      | 不存储任务，任务必须直接交给线程执行     |
| `PriorityBlockingQueue` | 支持优先级排序的无界队列                 |
| `DelayQueue`            | 支持延迟执行的无界队列                   |

生产环境建议优先使用有界队列，例如 `ArrayBlockingQueue` 或指定容量的 `LinkedBlockingQueue`，避免任务无限堆积导致内存溢出。

### 拒绝策略

当线程池无法继续接收任务时，会触发拒绝策略。JDK 内置了 4 种常见拒绝策略。

| 拒绝策略              | 说明                                            |
| --------------------- | ----------------------------------------------- |
| `AbortPolicy`         | 直接抛出 `RejectedExecutionException`，默认策略 |
| `CallerRunsPolicy`    | 由提交任务的线程自己执行任务                    |
| `DiscardPolicy`       | 直接丢弃新任务，不抛异常                        |
| `DiscardOldestPolicy` | 丢弃队列中最旧的任务，再尝试提交新任务          |

生产环境中，通常不建议使用静默丢弃类策略，例如 `DiscardPolicy`，因为任务丢失不容易被发现。更常见的选择是 `AbortPolicy` 或自定义拒绝策略，通过日志、监控、告警或降级逻辑明确暴露问题。

下面示例演示自定义拒绝策略，在线程池过载时记录错误日志。

文件位置：`src/main/java/io/github/atengk/concurrent/pool/RejectPolicyDemo.java`

```java
package io.github.atengk.concurrent.pool;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池拒绝策略示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class RejectPolicyDemo {

    public static void main(String[] args) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                1,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                runnable -> new Thread(runnable, "reject-worker"),
                (runnable, pool) -> log.error(
                        "线程池任务被拒绝，活跃线程数：{}，队列任务数：{}，已完成任务数：{}",
                        pool.getActiveCount(),
                        pool.getQueue().size(),
                        pool.getCompletedTaskCount()
                )
        );

        for (int i = 1; i <= 5; i++) {
            int taskNo = i;
            executor.execute(() -> {
                log.info("开始执行任务，任务编号：{}", taskNo);
                ThreadUtil.sleep(2000);
                log.info("任务执行完成，任务编号：{}", taskNo);
            });
        }

        executor.shutdown();
    }
}
```

### 线程池使用规范

线程池使用不当会导致性能下降、任务堆积、线程泄漏或服务不可用。实际开发中应遵循以下规范。

| 规范                            | 说明                                   |
| ------------------------------- | -------------------------------------- |
| 不建议使用 `Executors` 快捷方法 | 容易创建无界队列或无限线程，风险不可控 |
| 必须指定线程名称                | 便于日志排查、链路追踪和监控定位       |
| 队列必须有界                    | 避免任务无限堆积导致 OOM               |
| 合理设置线程数                  | 根据 CPU 密集型或 IO 密集型任务调整    |
| 设置拒绝策略                    | 任务过载时要有明确处理方式             |
| 任务中处理异常                  | 避免异常被吞掉或导致任务静默失败       |
| 应用关闭时关闭线程池            | 避免线程泄漏                           |
| 不同业务隔离线程池              | 避免慢任务拖垮核心业务                 |

线程数可以按任务类型进行粗略估算：

| 任务类型   | 建议                                                         |
| ---------- | ------------------------------------------------------------ |
| CPU 密集型 | 线程数接近 CPU 核心数，例如 `CPU 核心数 + 1`                 |
| IO 密集型  | 线程数可以适当大于 CPU 核心数，具体根据等待时间和压测结果确定 |
| 混合型任务 | 建议按业务拆分线程池，通过压测确定参数                       |

生产环境中的线程池参数不应只依赖公式，最终应结合接口耗时、任务峰值、队列长度、CPU 使用率、GC 情况和压测结果调整。

## 线程安全

线程安全是并发编程中的核心问题。多个线程同时访问共享变量、共享集合或共享对象时，如果没有正确的同步控制，就可能出现数据错乱、结果不一致、脏读、覆盖更新等问题。

### 线程安全问题

线程安全问题通常发生在以下条件同时满足时：

1. 存在多个线程。
2. 多个线程访问共享数据。
3. 至少一个线程会修改共享数据。
4. 修改操作不是天然安全的。

下面的代码演示多个线程同时对共享计数器执行自增操作，最终结果可能小于预期值。

文件位置：`src/main/java/io/github/atengk/concurrent/safety/UnsafeCounterDemo.java`

```java
package io.github.atengk.concurrent.safety;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 非线程安全计数器示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class UnsafeCounterDemo {

    private static int count = 0;

    public static void main(String[] args) {
        List<Thread> threadList = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 1; j <= 1000; j++) {
                    count++;
                }
            }, "unsafe-counter-worker-" + i);

            threadList.add(thread);
            thread.start();
        }

        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待线程执行完成时被中断", e);
            }
        }

        log.info("理论结果：{}，实际结果：{}", 10 * 1000, count);

        ThreadUtil.sleep(500);
    }
}
```

`count++` 看起来是一行代码，但它不是原子操作。它大致包含读取、加一、写回三个步骤。多个线程同时执行时，可能读取到相同的旧值，然后分别写回，导致部分更新丢失。

### 原子性

原子性表示一个或多个操作要么全部执行完成，要么完全不执行，中间过程不会被其他线程打断。

`count++` 不具备原子性，因为它包含多个步骤：

```java
int temp = count;
temp = temp + 1;
count = temp;
```

解决原子性问题的常见方式包括：

| 方式            | 说明                                           |
| --------------- | ---------------------------------------------- |
| `synchronized`  | 通过内置锁保证同一时刻只有一个线程执行临界区   |
| `ReentrantLock` | 通过显式锁控制临界区                           |
| 原子类          | 使用 `AtomicInteger`、`AtomicLong` 等 CAS 工具 |
| 避免共享状态    | 通过局部变量、不可变对象或线程隔离减少共享数据 |

下面示例使用 `AtomicInteger` 保证自增操作的原子性。

文件位置：`src/main/java/io/github/atengk/concurrent/safety/AtomicCounterDemo.java`

```java
package io.github.atengk.concurrent.safety;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 原子类计数器示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class AtomicCounterDemo {

    private static final AtomicInteger COUNT = new AtomicInteger(0);

    public static void main(String[] args) {
        List<Thread> threadList = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 1; j <= 1000; j++) {
                    COUNT.incrementAndGet();
                }
            }, "atomic-counter-worker-" + i);

            threadList.add(thread);
            thread.start();
        }

        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待线程执行完成时被中断", e);
            }
        }

        log.info("理论结果：{}，实际结果：{}", 10 * 1000, COUNT.get());
    }
}
```

对于简单计数场景，`AtomicInteger` 比 `synchronized` 更轻量。对于高并发热点计数场景，后续可以使用 `LongAdder` 降低竞争。

### 可见性

可见性表示一个线程修改共享变量后，其他线程能够及时看到最新值。

在多线程环境中，每个线程可能会使用 CPU 缓存、工作内存或寄存器中的变量副本。如果一个线程修改了共享变量，其他线程不一定能立即感知到变化。

典型问题如下：

```java
private static boolean running = true;

while (running) {
    // 执行业务逻辑
}
```

如果一个线程将 `running` 改为 `false`，另一个线程可能仍然看不到最新值，导致循环无法及时结束。

解决可见性问题的常见方式包括：

| 方式           | 说明                                      |
| -------------- | ----------------------------------------- |
| `volatile`     | 保证变量修改对其他线程可见                |
| `synchronized` | 进入和退出同步块时会建立内存可见性        |
| `Lock`         | 加锁和释放锁也能保证可见性                |
| 原子类         | 原子类内部通常基于 `volatile` 和 CAS 实现 |

下面示例使用 `volatile` 控制线程停止。

文件位置：`src/main/java/io/github/atengk/concurrent/safety/VisibilityDemo.java`

```java
package io.github.atengk.concurrent.safety;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * volatile 可见性示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class VisibilityDemo {

    private static volatile boolean running = true;

    public static void main(String[] args) throws InterruptedException {
        Thread workerThread = new Thread(() -> {
            while (running) {
                // 模拟持续运行的任务
            }

            log.warn("检测到 running=false，线程退出");
        }, "visibility-worker");

        workerThread.start();

        ThreadUtil.sleep(1000);

        running = false;
        log.info("主线程已修改 running=false");

        workerThread.join();
        log.info("线程执行结束");
    }
}
```

`volatile` 只能保证可见性和一定程度的有序性，不能保证复合操作的原子性。因此，`volatile int count` 执行 `count++` 仍然不是线程安全的。

### 有序性

有序性表示程序执行顺序与代码编写顺序之间的关系。

为了提升执行效率，编译器、JIT 和 CPU 可能会在不影响单线程结果的前提下进行指令重排。但在多线程环境中，如果没有正确的同步约束，指令重排可能导致其他线程观察到不符合预期的中间状态。

典型场景是对象初始化和状态标记：

```java
data = loadData();
ready = true;
```

从单线程视角看，先加载数据，再标记准备完成，没有问题。但在多线程环境中，如果发生指令重排，其他线程可能先看到 `ready = true`，随后读取 `data` 时发现数据还没有完全初始化。

解决有序性问题的常见方式包括：

| 方式           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| `volatile`     | 禁止相关指令重排                                             |
| `synchronized` | 通过锁建立 happens-before 关系                               |
| `Lock`         | 加锁和释放锁建立顺序约束                                     |
| 并发工具类     | 如 `CountDownLatch`、`Future`、`CompletableFuture` 等建立任务完成关系 |

下面示例使用 `volatile` 保证状态标记的可见性和有序性。

文件位置：`src/main/java/io/github/atengk/concurrent/safety/OrderingDemo.java`

```java
package io.github.atengk.concurrent.safety;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * volatile 有序性示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class OrderingDemo {

    private static String data;

    private static volatile boolean ready = false;

    public static void main(String[] args) throws InterruptedException {
        Thread readerThread = new Thread(() -> {
            while (!ready) {
                // 等待数据准备完成
            }

            log.info("读取到数据：{}", data);
        }, "ordering-reader");

        Thread writerThread = new Thread(() -> {
            data = "业务数据";
            ready = true;
            log.info("数据写入完成，已更新 ready=true");
        }, "ordering-writer");

        readerThread.start();

        ThreadUtil.sleep(500);

        writerThread.start();

        readerThread.join();
        writerThread.join();
    }
}
```

线程安全问题可以归纳为三类：原子性、可见性和有序性。后续学习 `synchronized`、`ReentrantLock`、`volatile`、原子类、并发集合和并发工具类时，本质上都是围绕这三类问题展开。



## 锁机制

锁机制用于控制多个线程对共享资源的访问顺序，核心目标是保证线程安全。Java 中常见锁包括 `synchronized`、`ReentrantLock`、`ReadWriteLock` 和基于线程阻塞唤醒的 `LockSupport`。

锁主要解决并发中的原子性和可见性问题。线程进入加锁区域后，其他竞争同一把锁的线程必须等待；线程释放锁后，其他线程可以看到该线程在锁内对共享变量的修改结果。

### synchronized

`synchronized` 是 Java 内置锁，也称为监视器锁。它可以修饰实例方法、静态方法，也可以修饰代码块。

`synchronized` 的使用方式如下：

| 使用方式     | 锁对象                |
| ------------ | --------------------- |
| 修饰实例方法 | 当前实例对象 `this`   |
| 修饰静态方法 | 当前类的 `Class` 对象 |
| 修饰代码块   | 手动指定的锁对象      |

`synchronized` 具备两个核心特性：

| 特性   | 说明                                             |
| ------ | ------------------------------------------------ |
| 原子性 | 同一时刻只有一个线程可以进入同一把锁保护的临界区 |
| 可见性 | 线程释放锁前的修改，对后续获取同一把锁的线程可见 |

下面示例使用 `synchronized` 修复普通 `int` 自增的线程安全问题。

文件位置：`src/main/java/io/github/atengk/concurrent/lock/SynchronizedCounterDemo.java`

```java
package io.github.atengk.concurrent.lock;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * synchronized 计数器示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class SynchronizedCounterDemo {

    private int count = 0;

    public static void main(String[] args) {
        SynchronizedCounterDemo demo = new SynchronizedCounterDemo();
        List<Thread> threadList = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 1; j <= 1000; j++) {
                    demo.increment();
                }
            }, "sync-counter-worker-" + i);

            threadList.add(thread);
            thread.start();
        }

        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待线程执行完成时被中断", e);
            }
        }

        log.info("理论结果：{}，实际结果：{}", 10 * 1000, demo.getCount());
    }

    /**
     * 增加计数
     */
    public synchronized void increment() {
        count++;
    }

    /**
     * 获取计数结果
     *
     * @return 当前计数
     */
    public synchronized int getCount() {
        return count;
    }
}
```

`synchronized` 使用简单，不需要手动释放锁。线程执行完同步代码块后会自动释放锁；如果同步代码中抛出异常，也会自动释放锁。

但 `synchronized` 也有一些限制：

| 限制               | 说明                                              |
| ------------------ | ------------------------------------------------- |
| 不支持公平锁配置   | 无法明确指定线程按等待顺序获取锁                  |
| 不支持尝试加锁     | 不能像 `tryLock()` 一样尝试获取锁后立即返回       |
| 不支持可中断获取锁 | 等待锁期间不能主动响应中断                        |
| 条件队列能力较弱   | 只能配合 `wait()`、`notify()`、`notifyAll()` 使用 |

简单同步场景优先使用 `synchronized`。如果需要更灵活的锁能力，再考虑 `ReentrantLock`。

### ReentrantLock

`ReentrantLock` 是 `java.util.concurrent.locks` 包下的可重入锁。它和 `synchronized` 一样可以保证临界区的原子性和可见性，但提供了更灵活的能力。

`ReentrantLock` 的常见能力包括：

| 能力         | 说明                                        |
| ------------ | ------------------------------------------- |
| 可重入       | 同一个线程可以重复获取同一把锁              |
| 可中断       | 等待锁时可以响应中断                        |
| 可尝试加锁   | `tryLock()` 获取不到锁时可以立即返回        |
| 可设置公平锁 | 构造方法中可以指定公平或非公平              |
| 多条件队列   | 可以创建多个 `Condition` 精细控制等待和唤醒 |

使用 `ReentrantLock` 时必须在 `finally` 中释放锁，避免业务异常导致锁无法释放。

下面示例演示 `ReentrantLock` 的标准使用方式。

文件位置：`src/main/java/io/github/atengk/concurrent/lock/ReentrantLockCounterDemo.java`

```java
package io.github.atengk.concurrent.lock;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLock 计数器示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ReentrantLockCounterDemo {

    private final ReentrantLock lock = new ReentrantLock();

    private int count = 0;

    public static void main(String[] args) {
        ReentrantLockCounterDemo demo = new ReentrantLockCounterDemo();
        List<Thread> threadList = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 1; j <= 1000; j++) {
                    demo.increment();
                }
            }, "lock-counter-worker-" + i);

            threadList.add(thread);
            thread.start();
        }

        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待线程执行完成时被中断", e);
            }
        }

        log.info("理论结果：{}，实际结果：{}", 10 * 1000, demo.getCount());
    }

    /**
     * 增加计数
     */
    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取计数结果
     *
     * @return 当前计数
     */
    public int getCount() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
```

下面示例演示 `tryLock()` 的用法。它适合需要避免线程长时间阻塞的场景，例如库存扣减、任务抢占、缓存刷新等。

文件位置：`src/main/java/io/github/atengk/concurrent/lock/ReentrantLockTryLockDemo.java`

```java
package io.github.atengk.concurrent.lock;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLock 尝试加锁示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ReentrantLockTryLockDemo {

    private static final ReentrantLock LOCK = new ReentrantLock();

    public static void main(String[] args) {
        Runnable task = () -> {
            boolean locked = false;

            try {
                locked = LOCK.tryLock(1, TimeUnit.SECONDS);
                if (!locked) {
                    log.warn("获取锁失败，放弃执行任务，线程名称：{}", Thread.currentThread().getName());
                    return;
                }

                log.info("获取锁成功，开始执行任务，线程名称：{}", Thread.currentThread().getName());
                ThreadUtil.sleep(2000);
                log.info("任务执行完成，线程名称：{}", Thread.currentThread().getName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待锁时线程被中断，线程名称：{}", Thread.currentThread().getName(), e);
            } finally {
                if (locked) {
                    LOCK.unlock();
                }
            }
        };

        new Thread(task, "try-lock-worker-1").start();
        new Thread(task, "try-lock-worker-2").start();
    }
}
```

`synchronized` 和 `ReentrantLock` 的常见区别如下：

| 对比项     | synchronized | ReentrantLock              |
| ---------- | ------------ | -------------------------- |
| 锁类型     | JVM 内置锁   | JUC 显式锁                 |
| 释放方式   | 自动释放     | 必须手动 `unlock()`        |
| 可重入     | 支持         | 支持                       |
| 公平锁     | 不支持配置   | 支持配置                   |
| 尝试加锁   | 不支持       | 支持 `tryLock()`           |
| 可中断等待 | 不支持       | 支持 `lockInterruptibly()` |
| 条件队列   | 单一等待队列 | 可创建多个 `Condition`     |

### ReadWriteLock

`ReadWriteLock` 是读写锁接口，常用实现类是 `ReentrantReadWriteLock`。它将锁拆分为读锁和写锁，适合读多写少的场景。

读写锁的规则如下：

| 操作组合   | 是否互斥           |
| ---------- | ------------------ |
| 读锁与读锁 | 不互斥，可以并发读 |
| 读锁与写锁 | 互斥               |
| 写锁与写锁 | 互斥               |

如果业务中读操作远多于写操作，使用读写锁可以提升并发性能。例如本地缓存、配置读取、字典数据、黑名单规则等场景。

下面示例使用 `ReentrantReadWriteLock` 实现一个简单本地缓存。

文件位置：`src/main/java/io/github/atengk/concurrent/lock/ReadWriteCacheDemo.java`

```java
package io.github.atengk.concurrent.lock;

import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ReadWriteLock 本地缓存示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ReadWriteCacheDemo {

    private final Map<String, String> cacheMap = MapUtil.newHashMap();

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public static void main(String[] args) {
        ReadWriteCacheDemo cacheDemo = new ReadWriteCacheDemo();

        cacheDemo.put("system:name", "Java 并发基础");
        cacheDemo.put("system:version", "1.0.0");

        Runnable readTask = () -> {
            String value = cacheDemo.get("system:name");
            log.info("读取缓存结果：{}，线程名称：{}", value, Thread.currentThread().getName());
        };

        new Thread(readTask, "cache-reader-1").start();
        new Thread(readTask, "cache-reader-2").start();

        new Thread(() -> {
            cacheDemo.put("system:name", "Java 并发进阶");
            log.info("更新缓存完成，线程名称：{}", Thread.currentThread().getName());
        }, "cache-writer-1").start();
    }

    /**
     * 写入缓存
     *
     * @param key 缓存键
     * @param value 缓存值
     */
    public void put(String key, String value) {
        readWriteLock.writeLock().lock();
        try {
            cacheMap.put(key, value);
            log.info("缓存写入成功，key：{}，value：{}", key, value);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * 读取缓存
     *
     * @param key 缓存键
     * @return 缓存值
     */
    public String get(String key) {
        readWriteLock.readLock().lock();
        try {
            return cacheMap.get(key);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }
}
```

读写锁并不适合所有场景。如果写操作非常频繁，读线程和写线程之间会频繁互斥，性能收益可能不明显，甚至增加锁管理成本。

### LockSupport

`LockSupport` 是 JUC 中用于阻塞和唤醒线程的底层工具类。它提供了 `park()` 和 `unpark(Thread thread)` 方法。

`LockSupport` 的核心方法如下：

| 方法                       | 说明                     |
| -------------------------- | ------------------------ |
| `park()`                   | 阻塞当前线程             |
| `parkNanos(long nanos)`    | 阻塞当前线程指定纳秒时间 |
| `parkUntil(long deadline)` | 阻塞当前线程直到指定时间 |
| `unpark(Thread thread)`    | 唤醒指定线程             |

`LockSupport` 和 `wait()`、`notify()` 的区别如下：

| 对比项         | LockSupport                       | wait / notify                       |
| -------------- | --------------------------------- | ----------------------------------- |
| 是否必须持有锁 | 不需要                            | 必须在 `synchronized` 中使用        |
| 唤醒对象       | 指定线程                          | 随机或全部等待线程                  |
| 唤醒顺序       | `unpark()` 可以先于 `park()` 调用 | `notify()` 先于 `wait()` 调用会丢失 |
| 使用层级       | 更底层，常用于并发框架            | 更偏传统线程协作                    |

下面示例演示使用 `LockSupport` 阻塞和唤醒线程。

文件位置：`src/main/java/io/github/atengk/concurrent/lock/LockSupportDemo.java`

```java
package io.github.atengk.concurrent.lock;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.LockSupport;

/**
 * LockSupport 阻塞唤醒示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class LockSupportDemo {

    public static void main(String[] args) {
        Thread workerThread = new Thread(() -> {
            log.info("线程准备进入 park 状态，线程名称：{}", Thread.currentThread().getName());

            LockSupport.park();

            log.info("线程被 unpark 唤醒，继续执行，线程名称：{}", Thread.currentThread().getName());
        }, "park-worker");

        workerThread.start();

        ThreadUtil.sleep(1000);

        log.info("主线程准备唤醒子线程");
        LockSupport.unpark(workerThread);
    }
}
```

`LockSupport` 使用的是“许可”机制。每个线程最多只有一个许可，`unpark()` 会发放许可，`park()` 会消费许可。如果先调用 `unpark()`，后调用 `park()`，线程不会阻塞，因为许可已经存在。

## volatile 关键字

`volatile` 是 Java 中轻量级的同步机制，主要用于保证共享变量的可见性，并在一定程度上禁止指令重排。

它适合用于状态标记、开关变量、单例双重检查中的实例引用等场景，但不适合用于复合更新操作，例如 `count++`。

### 可见性保证

`volatile` 修饰的变量被一个线程修改后，其他线程能够及时读取到最新值。

普通变量在多线程环境中可能出现一个线程修改后，另一个线程迟迟看不到变化的问题。`volatile` 可以要求线程读取变量时从主内存获取最新值，写入变量时将结果刷新到主内存。

下面示例使用 `volatile` 控制工作线程停止。

文件位置：`src/main/java/io/github/atengk/concurrent/volatilecase/VolatileVisibilityDemo.java`

```java
package io.github.atengk.concurrent.volatilecase;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * volatile 可见性示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class VolatileVisibilityDemo {

    private static volatile boolean running = true;

    public static void main(String[] args) throws InterruptedException {
        Thread workerThread = new Thread(() -> {
            while (running) {
                // 模拟任务持续运行
            }

            log.warn("检测到 running=false，工作线程退出");
        }, "volatile-worker");

        workerThread.start();

        ThreadUtil.sleep(1000);

        running = false;
        log.info("主线程已修改 running=false");

        workerThread.join();
        log.info("程序执行完成");
    }
}
```

适合使用 `volatile` 的变量通常具有以下特点：

| 条件           | 说明                           |
| -------------- | ------------------------------ |
| 写入操作简单   | 通常是直接赋值，不是复合计算   |
| 不依赖当前值   | 新值不依赖旧值                 |
| 只需要可见性   | 不要求多个操作整体具备原子性   |
| 状态标记类变量 | 例如开关、初始化标记、停止标记 |

### 禁止指令重排

`volatile` 还可以禁止与该变量相关的指令重排，保证特定操作顺序对其他线程可见。

典型场景是双重检查锁单例。如果实例变量没有使用 `volatile`，对象创建过程可能发生指令重排，其他线程可能拿到一个尚未完全初始化的对象。

对象创建大致可以分为三步：

```java
// 1. 分配对象内存
// 2. 初始化对象
// 3. 将引用指向分配的内存地址
instance = new Singleton();
```

如果第 2 步和第 3 步发生重排，其他线程可能判断 `instance != null`，但对象还没有初始化完成。

下面示例演示双重检查锁单例中 `volatile` 的使用。

文件位置：`src/main/java/io/github/atengk/concurrent/volatilecase/SingletonDemo.java`

```java
package io.github.atengk.concurrent.volatilecase;

import lombok.extern.slf4j.Slf4j;

/**
 * volatile 双重检查锁单例示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class SingletonDemo {

    private static volatile SingletonDemo instance;

    private SingletonDemo() {
        log.info("初始化 SingletonDemo 实例");
    }

    /**
     * 获取单例实例
     *
     * @return 单例实例
     */
    public static SingletonDemo getInstance() {
        if (instance == null) {
            synchronized (SingletonDemo.class) {
                if (instance == null) {
                    instance = new SingletonDemo();
                }
            }
        }

        return instance;
    }

    /**
     * 执行业务方法
     */
    public void execute() {
        log.info("执行单例业务方法");
    }

    public static void main(String[] args) {
        SingletonDemo singletonDemo = SingletonDemo.getInstance();
        singletonDemo.execute();
    }
}
```

在这个示例中，`volatile` 的作用不是保证 `new SingletonDemo()` 的原子性，而是禁止实例引用赋值过程发生不安全的指令重排，并保证其他线程能看到最新的实例引用。

### volatile 的使用限制

`volatile` 不是锁，不能替代 `synchronized` 或 `ReentrantLock`。它不能保证复合操作的原子性。

下面代码即使使用 `volatile`，也不是线程安全的：

```java
private static volatile int count = 0;

count++;
```

`count++` 包含读取、加一、写回三个步骤。`volatile` 只能保证每次读取的是较新的值，但不能保证这三个步骤作为一个整体不可分割。

下面示例演示 `volatile` 无法保证自增操作的原子性。

文件位置：`src/main/java/io/github/atengk/concurrent/volatilecase/VolatileAtomicLimitDemo.java`

```java
package io.github.atengk.concurrent.volatilecase;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * volatile 原子性限制示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class VolatileAtomicLimitDemo {

    private static volatile int count = 0;

    public static void main(String[] args) {
        List<Thread> threadList = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 1; j <= 1000; j++) {
                    count++;
                }
            }, "volatile-count-worker-" + i);

            threadList.add(thread);
            thread.start();
        }

        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待线程执行完成时被中断", e);
            }
        }

        log.info("理论结果：{}，实际结果：{}", 10 * 1000, count);
    }
}
```

`volatile` 的适用和不适用场景如下：

| 场景               | 是否适合 |
| ------------------ | -------- |
| 停止标记           | 适合     |
| 配置刷新标记       | 适合     |
| 初始化完成标记     | 适合     |
| 双重检查锁单例引用 | 适合     |
| 计数器自增         | 不适合   |
| 金额扣减           | 不适合   |
| 多字段一致性更新   | 不适合   |
| 复杂临界区保护     | 不适合   |

如果需要保证复合操作的线程安全，应使用锁、原子类或并发容器。

## 原子类

原子类位于 `java.util.concurrent.atomic` 包下，主要用于在无锁或低锁竞争场景下完成线程安全操作。它们通常基于 CAS 和 `volatile` 实现。

CAS 是 Compare And Set 的缩写，核心思想是：更新变量前先比较当前值是否等于期望值，如果相等则更新，否则说明变量已经被其他线程修改，本次更新失败并重试。

原子类常用于计数、状态更新、引用替换、统计累加等场景。

### AtomicInteger

`AtomicInteger` 用于对 `int` 类型变量进行线程安全操作，常见方法包括：

| 方法                                    | 说明               |
| --------------------------------------- | ------------------ |
| `incrementAndGet()`                     | 先自增，再返回新值 |
| `getAndIncrement()`                     | 先返回旧值，再自增 |
| `decrementAndGet()`                     | 先自减，再返回新值 |
| `addAndGet(int delta)`                  | 加指定值后返回新值 |
| `compareAndSet(int expect, int update)` | CAS 更新           |
| `get()`                                 | 获取当前值         |

下面示例使用 `AtomicInteger` 实现线程安全计数器。

文件位置：`src/main/java/io/github/atengk/concurrent/atomic/AtomicIntegerDemo.java`

```java
package io.github.atengk.concurrent.atomic;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AtomicInteger 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class AtomicIntegerDemo {

    private static final AtomicInteger COUNT = new AtomicInteger(0);

    public static void main(String[] args) {
        List<Thread> threadList = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 1; j <= 1000; j++) {
                    COUNT.incrementAndGet();
                }
            }, "atomic-int-worker-" + i);

            threadList.add(thread);
            thread.start();
        }

        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待线程执行完成时被中断", e);
            }
        }

        log.info("理论结果：{}，实际结果：{}", 10 * 1000, COUNT.get());
    }
}
```

`AtomicInteger` 适合中低并发下的计数、状态标记和简单数值更新。如果并发非常高，并且大量线程同时更新同一个变量，CAS 自旋重试可能增多，此时可以考虑 `LongAdder`。

### AtomicLong

`AtomicLong` 用于对 `long` 类型变量进行线程安全操作。它的使用方式和 `AtomicInteger` 类似，只是数值范围更大。

常见使用场景包括：

| 场景         | 说明               |
| ------------ | ------------------ |
| 全局请求计数 | 统计接口访问次数   |
| 任务编号生成 | 生成本地递增序号   |
| 消息消费统计 | 统计成功或失败数量 |
| 业务指标累加 | 记录总耗时、总数量 |

下面示例使用 `AtomicLong` 生成本地递增任务编号。

文件位置：`src/main/java/io/github/atengk/concurrent/atomic/AtomicLongDemo.java`

```java
package io.github.atengk.concurrent.atomic;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * AtomicLong 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class AtomicLongDemo {

    private static final AtomicLong TASK_NO_GENERATOR = new AtomicLong(100000L);

    public static void main(String[] args) {
        Runnable task = () -> {
            long taskNo = TASK_NO_GENERATOR.incrementAndGet();

            log.info("生成任务编号：{}，线程名称：{}", taskNo, Thread.currentThread().getName());

            ThreadUtil.sleep(500);
        };

        for (int i = 1; i <= 5; i++) {
            new Thread(task, "atomic-long-worker-" + i).start();
        }
    }
}
```

`AtomicLong` 可以保证单个 JVM 进程内的递增安全，但不能直接作为分布式全局 ID 方案。如果需要分布式唯一 ID，应使用数据库号段、Redis、雪花算法或专门的 ID 服务。

### AtomicReference

`AtomicReference` 用于对对象引用进行原子更新。它适合需要线程安全替换整个对象引用的场景。

常见使用场景包括：

| 场景           | 说明                             |
| -------------- | -------------------------------- |
| 配置对象热更新 | 将旧配置整体替换为新配置         |
| 缓存引用替换   | 原子替换本地缓存对象             |
| 状态对象切换   | 根据当前状态 CAS 更新为新状态    |
| 不可变对象更新 | 通过替换整个对象避免修改内部字段 |

下面示例使用 `AtomicReference` 实现配置对象的原子替换。

文件位置：`src/main/java/io/github/atengk/concurrent/atomic/AtomicReferenceDemo.java`

```java
package io.github.atengk.concurrent.atomic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

/**
 * AtomicReference 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class AtomicReferenceDemo {

    private static final AtomicReference<SystemConfig> CONFIG_REFERENCE = new AtomicReference<>(
            new SystemConfig("v1", true, 100)
    );

    public static void main(String[] args) {
        SystemConfig oldConfig = CONFIG_REFERENCE.get();
        SystemConfig newConfig = new SystemConfig("v2", false, 200);

        boolean updated = CONFIG_REFERENCE.compareAndSet(oldConfig, newConfig);

        log.info("配置更新结果：{}，当前配置：{}", updated, CONFIG_REFERENCE.get());
    }

    /**
     * 系统配置对象
     *
     * @author Ateng
     * @since 2026-05-15
     */
    @Getter
    @ToString
    @AllArgsConstructor
    static class SystemConfig {

        private final String version;

        private final Boolean enableCache;

        private final Integer maxRequestCount;
    }
}
```

使用 `AtomicReference` 时，推荐配合不可变对象。也就是说，对象内部字段尽量使用 `final`，每次变更时创建新对象并替换引用。这样可以减少共享对象内部状态被并发修改的风险。

### LongAdder

`LongAdder` 是高并发计数场景下常用的累加器。它通过分段累加降低多个线程竞争同一个变量的压力。

`AtomicLong` 在高并发下所有线程竞争同一个 CAS 变量，竞争激烈时会产生较多重试。`LongAdder` 内部会将热点更新分散到多个变量上，最终通过 `sum()` 汇总结果。

`AtomicLong` 和 `LongAdder` 的区别如下：

| 对比项                | AtomicLong             | LongAdder          |
| --------------------- | ---------------------- | ------------------ |
| 实现思路              | 单变量 CAS 更新        | 分段累加，最终求和 |
| 适合场景              | 精确更新、CAS 条件判断 | 高并发统计累加     |
| 读取结果              | `get()` 成本较低       | `sum()` 需要汇总   |
| 是否支持 CAS 条件更新 | 支持                   | 不支持             |
| 高并发写性能          | 竞争较明显             | 通常更好           |

下面示例使用 `LongAdder` 实现高并发计数。

文件位置：`src/main/java/io/github/atengk/concurrent/atomic/LongAdderDemo.java`

```java
package io.github.atengk.concurrent.atomic;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

/**
 * LongAdder 高并发累加示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class LongAdderDemo {

    private static final LongAdder REQUEST_COUNT = new LongAdder();

    public static void main(String[] args) {
        List<Thread> threadList = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 1; j <= 10000; j++) {
                    REQUEST_COUNT.increment();
                }
            }, "long-adder-worker-" + i);

            threadList.add(thread);
            thread.start();
        }

        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待线程执行完成时被中断", e);
            }
        }

        log.info("理论请求数：{}，实际请求数：{}", 20 * 10000, REQUEST_COUNT.sum());
    }
}
```

`LongAdder` 适合统计类场景，例如接口请求次数、消息消费数量、错误次数、命中次数等。但如果业务需要根据当前值执行条件更新，例如“当库存大于 0 时扣减”，不适合使用 `LongAdder`，应使用锁、数据库原子更新、Redis Lua 脚本或 `AtomicInteger.compareAndSet()` 等方案。





## 并发工具类

并发工具类主要用于线程之间的协作控制。相比直接使用 `wait()`、`notify()` 或手写锁逻辑，JUC 提供的工具类语义更清晰，也更适合业务开发。

常见工具类包括 `CountDownLatch`、`CyclicBarrier`、`Semaphore` 和 `Exchanger`。

### CountDownLatch

`CountDownLatch` 是倒计时门闩，用于让一个或多个线程等待其他线程完成任务后再继续执行。

它的核心方法如下：

| 方法                   | 说明                     |
| ---------------------- | ------------------------ |
| `countDown()`          | 计数减一                 |
| `await()`              | 阻塞等待计数归零         |
| `await(timeout, unit)` | 在指定时间内等待计数归零 |

`CountDownLatch` 的计数器只能递减，不能重置。因此它适合一次性等待场景，例如主线程等待多个子任务完成、服务启动时等待多个组件初始化完成、批量导入时等待多个分片处理完成。

下面示例演示主线程等待多个子任务执行完成。

文件位置：`src/main/java/io/github/atengk/concurrent/tool/CountDownLatchDemo.java`

这段代码使用 `CountDownLatch` 等待 5 个订单分片任务全部处理完成，适合批量任务汇总场景。

```java
package io.github.atengk.concurrent.tool;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CountDownLatch 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class CountDownLatchDemo {

    public static void main(String[] args) {
        int taskCount = 5;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger threadIndex = new AtomicInteger(1);

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                3,
                3,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                runnable -> new Thread(runnable, "order-batch-worker-" + threadIndex.getAndIncrement()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        for (int i = 1; i <= taskCount; i++) {
            int taskNo = i;
            executor.execute(() -> {
                try {
                    log.info("开始处理订单分片，分片编号：{}，线程名称：{}", taskNo, Thread.currentThread().getName());
                    ThreadUtil.sleep(1000);
                    log.info("订单分片处理完成，分片编号：{}", taskNo);
                } catch (Exception e) {
                    log.error("订单分片处理异常，分片编号：{}", taskNo, e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            if (completed) {
                log.info("所有订单分片处理完成，开始汇总结果");
            } else {
                log.warn("等待订单分片处理超时，剩余任务数：{}", latch.getCount());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待订单分片处理时线程被中断", e);
        } finally {
            executor.shutdown();
        }
    }
}
```

使用 `CountDownLatch` 时，需要特别注意 `countDown()` 应放在 `finally` 中。否则子任务发生异常时没有递减计数，等待线程可能一直阻塞。

`CountDownLatch` 的典型使用场景如下：

| 场景                 | 说明                             |
| -------------------- | -------------------------------- |
| 主线程等待多个子任务 | 多个任务完成后统一汇总           |
| 服务启动检查         | 等待缓存、配置、连接池初始化完成 |
| 批量数据处理         | 多个分片全部完成后继续执行       |
| 并发压测             | 多个线程同时等待开始信号         |

### CyclicBarrier

`CyclicBarrier` 是循环屏障，用于让一组线程互相等待，直到所有线程都到达屏障点后，再一起继续执行。

它和 `CountDownLatch` 的区别在于：

| 对比项         | CountDownLatch                 | CyclicBarrier              |
| -------------- | ------------------------------ | -------------------------- |
| 计数是否可重用 | 不可重用                       | 可以重用                   |
| 主要作用       | 一个或多个线程等待其他线程完成 | 一组线程互相等待           |
| 触发动作       | 计数归零后等待线程继续执行     | 所有线程到达屏障后一起继续 |
| 典型场景       | 主线程等待子任务结束           | 多阶段任务、并发对齐执行   |

`CyclicBarrier` 可以指定一个屏障任务。当所有线程都到达屏障点后，屏障任务会被执行一次。

下面示例演示多个线程分批到达屏障点后继续执行。

文件位置：`src/main/java/io/github/atengk/concurrent/tool/CyclicBarrierDemo.java`

这段代码模拟 6 个任务按每 3 个一组进行阶段同步，每组线程都到达屏障点后再继续执行后续逻辑。

```java
package io.github.atengk.concurrent.tool;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CyclicBarrier 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class CyclicBarrierDemo {

    public static void main(String[] args) {
        int parties = 3;
        AtomicInteger threadIndex = new AtomicInteger(1);

        CyclicBarrier barrier = new CyclicBarrier(parties, () ->
                log.info("当前批次所有线程已到达屏障点，准备统一进入下一阶段")
        );

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                3,
                3,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                runnable -> new Thread(runnable, "barrier-worker-" + threadIndex.getAndIncrement()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        for (int i = 1; i <= 6; i++) {
            int taskNo = i;
            executor.execute(() -> {
                try {
                    log.info("任务开始准备数据，任务编号：{}，线程名称：{}", taskNo, Thread.currentThread().getName());
                    ThreadUtil.sleep(500L * taskNo);

                    log.info("任务到达屏障点，任务编号：{}", taskNo);
                    barrier.await();

                    log.info("任务通过屏障点，继续执行后续逻辑，任务编号：{}", taskNo);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("任务等待屏障时被中断，任务编号：{}", taskNo, e);
                } catch (BrokenBarrierException e) {
                    log.error("屏障已损坏，任务编号：{}", taskNo, e);
                }
            });
        }

        executor.shutdown();
    }
}
```

`CyclicBarrier` 适合多线程分阶段协作。例如多线程计算时，所有线程完成第一阶段后，再统一进入第二阶段。

使用时需要注意，如果某个线程等待过程中被中断、超时或异常退出，屏障可能被破坏，其他等待线程会收到 `BrokenBarrierException`。

### Semaphore

`Semaphore` 是信号量，用于控制同时访问某个资源的线程数量。

它内部维护一组许可证。线程执行任务前先获取许可证，执行完成后释放许可证。如果许可证不足，后续线程会阻塞等待。

核心方法如下：

| 方法                        | 说明                             |
| --------------------------- | -------------------------------- |
| `acquire()`                 | 获取一个许可证，没有许可证时阻塞 |
| `tryAcquire()`              | 尝试获取许可证，失败立即返回     |
| `tryAcquire(timeout, unit)` | 在指定时间内尝试获取许可证       |
| `release()`                 | 释放一个许可证                   |
| `availablePermits()`        | 获取当前可用许可证数量           |

`Semaphore` 适合限流、资源池控制、并发访问控制等场景。例如限制同时下载文件的数量、限制同时调用第三方接口的请求数量、限制同时处理大文件的线程数量。

下面示例演示使用 `Semaphore` 控制最多 3 个线程同时访问接口。

文件位置：`src/main/java/io/github/atengk/concurrent/tool/SemaphoreDemo.java`

这段代码使用 `Semaphore` 控制第三方接口调用并发数，超过许可证数量的任务需要等待。

```java
package io.github.atengk.concurrent.tool;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Semaphore 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class SemaphoreDemo {

    public static void main(String[] args) {
        Semaphore semaphore = new Semaphore(3);
        AtomicInteger threadIndex = new AtomicInteger(1);

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                6,
                6,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                runnable -> new Thread(runnable, "api-call-worker-" + threadIndex.getAndIncrement()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        for (int i = 1; i <= 10; i++) {
            int taskNo = i;
            executor.execute(() -> {
                boolean acquired = false;

                try {
                    log.info("任务准备获取许可证，任务编号：{}", taskNo);
                    semaphore.acquire();
                    acquired = true;

                    log.info("任务获取许可证成功，开始调用接口，任务编号：{}，剩余许可证：{}",
                            taskNo, semaphore.availablePermits());

                    ThreadUtil.sleep(1500);

                    log.info("接口调用完成，任务编号：{}", taskNo);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("获取许可证时线程被中断，任务编号：{}", taskNo, e);
                } catch (Exception e) {
                    log.error("接口调用异常，任务编号：{}", taskNo, e);
                } finally {
                    if (acquired) {
                        semaphore.release();
                        log.info("任务释放许可证，任务编号：{}，剩余许可证：{}",
                                taskNo, semaphore.availablePermits());
                    }
                }
            });
        }

        executor.shutdown();
    }
}
```

`Semaphore` 本质上不是用来保护数据一致性的锁，而是用于限制并发数量。它可以和线程池配合使用：线程池控制任务执行线程数，`Semaphore` 进一步控制某类资源的访问并发度。

### Exchanger

`Exchanger` 用于两个线程之间交换数据。一个线程调用 `exchange()` 后会等待另一个线程也调用 `exchange()`，然后双方交换数据并继续执行。

它适合两个线程成对交换数据的场景，但在普通业务开发中使用频率相对较低。

核心方法如下：

| 方法                               | 说明                     |
| ---------------------------------- | ------------------------ |
| `exchange(V value)`                | 等待另一个线程交换数据   |
| `exchange(V value, timeout, unit)` | 在指定时间内等待交换数据 |

下面示例演示两个线程交换各自处理好的数据。

文件位置：`src/main/java/io/github/atengk/concurrent/tool/ExchangerDemo.java`

这段代码使用 `Exchanger` 模拟两个系统之间进行数据校对，双方准备好数据后互相交换并比对结果。

```java
package io.github.atengk.concurrent.tool;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Exchanger 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ExchangerDemo {

    public static void main(String[] args) {
        Exchanger<List<String>> exchanger = new Exchanger<>();

        Thread systemAThread = new Thread(() -> {
            try {
                List<String> systemAData = CollUtil.newArrayList("ORDER-1001", "ORDER-1002", "ORDER-1003");
                log.info("系统 A 数据准备完成：{}", systemAData);

                List<String> systemBData = exchanger.exchange(systemAData, 3, TimeUnit.SECONDS);
                log.info("系统 A 收到系统 B 数据：{}", systemBData);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("系统 A 数据交换时线程被中断", e);
            } catch (TimeoutException e) {
                log.error("系统 A 等待数据交换超时", e);
            }
        }, "system-a-worker");

        Thread systemBThread = new Thread(() -> {
            try {
                ThreadUtil.sleep(1000);

                List<String> systemBData = CollUtil.newArrayList("ORDER-1001", "ORDER-1002", "ORDER-1004");
                log.info("系统 B 数据准备完成：{}", systemBData);

                List<String> systemAData = exchanger.exchange(systemBData, 3, TimeUnit.SECONDS);
                log.info("系统 B 收到系统 A 数据：{}", systemAData);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("系统 B 数据交换时线程被中断", e);
            } catch (TimeoutException e) {
                log.error("系统 B 等待数据交换超时", e);
            }
        }, "system-b-worker");

        systemAThread.start();
        systemBThread.start();
    }
}
```

`Exchanger` 只能用于两个线程之间交换数据。如果需要多个线程之间共享结果，通常更适合使用阻塞队列、线程安全集合、`CompletableFuture` 或消息队列。

## CompletableFuture

`CompletableFuture` 是 Java 8 引入的异步编程工具，位于 `java.util.concurrent` 包下。它既可以表示一个异步任务的结果，也可以对多个异步任务进行组合、编排和异常处理。

相比 `Future`，`CompletableFuture` 的优势主要包括：

| 能力       | Future               | CompletableFuture                              |
| ---------- | -------------------- | ---------------------------------------------- |
| 获取结果   | 主要通过阻塞 `get()` | 支持回调和链式编排                             |
| 异步任务   | 支持                 | 支持                                           |
| 任务组合   | 弱                   | 强                                             |
| 异常处理   | 不方便               | 支持 `exceptionally`、`handle`、`whenComplete` |
| 主动完成   | 不方便               | 支持 `complete()`                              |
| 多任务聚合 | 不方便               | 支持 `allOf()`、`anyOf()`                      |

实际项目中，`CompletableFuture` 常用于并行调用多个接口、异步加载数据、异步发送通知、异步写日志、订单处理链路编排等场景。

### 异步任务

`CompletableFuture` 创建异步任务常用两种方法：

| 方法            | 说明                 |
| --------------- | -------------------- |
| `runAsync()`    | 执行无返回值异步任务 |
| `supplyAsync()` | 执行有返回值异步任务 |

如果不指定线程池，默认使用 `ForkJoinPool.commonPool()`。业务项目中不建议直接使用默认线程池，建议显式传入自定义线程池，避免不同业务任务互相影响。

下面示例演示 `runAsync()` 和 `supplyAsync()` 的基础用法。

文件位置：`src/main/java/io/github/atengk/concurrent/future/CompletableFutureAsyncDemo.java`

这段代码演示无返回值异步任务和有返回值异步任务，并通过自定义线程池执行。

```java
package io.github.atengk.concurrent.future;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CompletableFuture 异步任务示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class CompletableFutureAsyncDemo {

    public static void main(String[] args) {
        ThreadPoolExecutor executor = buildExecutor();

        try {
            CompletableFuture<Void> messageFuture = CompletableFuture.runAsync(() -> {
                log.info("开始异步发送消息通知，线程名称：{}", Thread.currentThread().getName());
                ThreadUtil.sleep(1000);
                log.info("消息通知发送完成");
            }, executor);

            CompletableFuture<String> orderFuture = CompletableFuture.supplyAsync(() -> {
                log.info("开始异步查询订单信息，线程名称：{}", Thread.currentThread().getName());
                ThreadUtil.sleep(1200);
                return "ORDER-1001";
            }, executor);

            messageFuture.join();

            String orderNo = orderFuture.join();
            log.info("获取订单查询结果：{}", orderNo);
        } finally {
            executor.shutdown();
        }
    }

    private static ThreadPoolExecutor buildExecutor() {
        AtomicInteger threadIndex = new AtomicInteger(1);

        return new ThreadPoolExecutor(
                4,
                4,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                runnable -> new Thread(runnable, "async-worker-" + threadIndex.getAndIncrement()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

`join()` 和 `get()` 都可以获取结果，但区别如下：

| 方法     | 异常类型                                                  | 使用特点             |
| -------- | --------------------------------------------------------- | -------------------- |
| `get()`  | 抛出受检异常 `ExecutionException`、`InterruptedException` | 需要显式捕获异常     |
| `join()` | 抛出运行时异常 `CompletionException`                      | 链式编排中使用更方便 |

### 任务编排

`CompletableFuture` 的核心价值在于任务编排。它可以把多个异步任务按照串行、并行、聚合、任意完成等方式组织起来。

常见编排方法如下：

| 方法            | 说明                             |
| --------------- | -------------------------------- |
| `thenApply()`   | 接收上一步结果并转换，有返回值   |
| `thenAccept()`  | 接收上一步结果并消费，无返回值   |
| `thenRun()`     | 不接收上一步结果，直接执行下一步 |
| `thenCompose()` | 将两个依赖关系的异步任务串联     |
| `thenCombine()` | 合并两个无依赖异步任务的结果     |
| `allOf()`       | 等待所有任务完成                 |
| `anyOf()`       | 任意一个任务完成即可继续         |

下面示例演示订单详情页常见的并行查询和结果聚合。

文件位置：`src/main/java/io/github/atengk/concurrent/future/CompletableFutureComposeDemo.java`

这段代码并行查询订单、用户和物流信息，全部完成后组装订单详情结果。

```java
package io.github.atengk.concurrent.future;

import cn.hutool.core.thread.ThreadUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CompletableFuture 任务编排示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class CompletableFutureComposeDemo {

    public static void main(String[] args) {
        ThreadPoolExecutor executor = buildExecutor();

        try {
            String orderNo = "ORDER-1001";

            CompletableFuture<OrderInfo> orderFuture = CompletableFuture.supplyAsync(() -> queryOrder(orderNo), executor);
            CompletableFuture<UserInfo> userFuture = CompletableFuture.supplyAsync(() -> queryUser(orderNo), executor);
            CompletableFuture<LogisticsInfo> logisticsFuture = CompletableFuture.supplyAsync(() -> queryLogistics(orderNo), executor);

            CompletableFuture<OrderDetail> detailFuture = CompletableFuture
                    .allOf(orderFuture, userFuture, logisticsFuture)
                    .thenApply(unused -> new OrderDetail(
                            orderFuture.join(),
                            userFuture.join(),
                            logisticsFuture.join()
                    ));

            OrderDetail detail = detailFuture.join();
            log.info("订单详情组装完成：{}", detail);
        } finally {
            executor.shutdown();
        }
    }

    private static OrderInfo queryOrder(String orderNo) {
        log.info("查询订单信息，订单号：{}", orderNo);
        ThreadUtil.sleep(800);
        return new OrderInfo(orderNo, "已支付");
    }

    private static UserInfo queryUser(String orderNo) {
        log.info("根据订单查询用户信息，订单号：{}", orderNo);
        ThreadUtil.sleep(1000);
        return new UserInfo("USER-1001", "张三");
    }

    private static LogisticsInfo queryLogistics(String orderNo) {
        log.info("查询物流信息，订单号：{}", orderNo);
        ThreadUtil.sleep(1200);
        return new LogisticsInfo("SF10000001", "运输中");
    }

    private static ThreadPoolExecutor buildExecutor() {
        AtomicInteger threadIndex = new AtomicInteger(1);

        return new ThreadPoolExecutor(
                6,
                6,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                runnable -> new Thread(runnable, "compose-worker-" + threadIndex.getAndIncrement()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Data
    @AllArgsConstructor
    static class OrderInfo {
        private String orderNo;
        private String status;
    }

    @Data
    @AllArgsConstructor
    static class UserInfo {
        private String userId;
        private String username;
    }

    @Data
    @AllArgsConstructor
    static class LogisticsInfo {
        private String trackingNo;
        private String status;
    }

    @Data
    @AllArgsConstructor
    static class OrderDetail {
        private OrderInfo orderInfo;
        private UserInfo userInfo;
        private LogisticsInfo logisticsInfo;
    }
}
```

上面这种方式适合多个任务之间没有强依赖关系的场景。例如订单详情页需要同时查询订单基础信息、用户信息、优惠信息、物流信息和售后信息。

如果后一个异步任务依赖前一个异步任务的结果，可以使用 `thenCompose()`。

文件位置：`src/main/java/io/github/atengk/concurrent/future/CompletableFutureThenComposeDemo.java`

这段代码演示先查询用户 ID，再根据用户 ID 查询用户积分，适合有前后依赖的异步链路。

```java
package io.github.atengk.concurrent.future;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CompletableFuture thenCompose 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class CompletableFutureThenComposeDemo {

    public static void main(String[] args) {
        ThreadPoolExecutor executor = buildExecutor();

        try {
            CompletableFuture<Integer> scoreFuture = CompletableFuture
                    .supplyAsync(() -> queryUserId("ORDER-1001"), executor)
                    .thenCompose(userId -> CompletableFuture.supplyAsync(() -> queryUserScore(userId), executor));

            Integer score = scoreFuture.join();
            log.info("用户积分查询完成，积分：{}", score);
        } finally {
            executor.shutdown();
        }
    }

    private static String queryUserId(String orderNo) {
        log.info("根据订单查询用户 ID，订单号：{}", orderNo);
        ThreadUtil.sleep(800);
        return "USER-1001";
    }

    private static Integer queryUserScore(String userId) {
        log.info("根据用户 ID 查询积分，用户 ID：{}", userId);
        ThreadUtil.sleep(800);
        return 980;
    }

    private static ThreadPoolExecutor buildExecutor() {
        AtomicInteger threadIndex = new AtomicInteger(1);

        return new ThreadPoolExecutor(
                4,
                4,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                runnable -> new Thread(runnable, "compose-chain-worker-" + threadIndex.getAndIncrement()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

`thenApply()` 和 `thenCompose()` 的区别需要特别注意：

| 方法            | 返回类型                 | 适合场景                     |
| --------------- | ------------------------ | ---------------------------- |
| `thenApply()`   | 普通结果                 | 对上一步结果做同步转换       |
| `thenCompose()` | 新的 `CompletableFuture` | 上一步结果决定下一个异步任务 |

如果返回值本身就是 `CompletableFuture`，通常使用 `thenCompose()`，避免出现嵌套的 `CompletableFuture<CompletableFuture<T>>`。

### 异常处理

`CompletableFuture` 的异常处理主要使用 `exceptionally()`、`handle()` 和 `whenComplete()`。

| 方法              | 是否能返回兜底结果 | 是否能访问正常结果 | 是否能访问异常 | 典型用途           |
| ----------------- | ------------------ | ------------------ | -------------- | ------------------ |
| `exceptionally()` | 能                 | 不能               | 能             | 异常兜底           |
| `handle()`        | 能                 | 能                 | 能             | 正常和异常统一转换 |
| `whenComplete()`  | 不能改变最终结果   | 能                 | 能             | 记录日志、释放资源 |

下面示例演示异步任务异常后的兜底处理。

文件位置：`src/main/java/io/github/atengk/concurrent/future/CompletableFutureExceptionDemo.java`

这段代码模拟远程接口调用失败，并使用 `exceptionally()` 返回默认值，避免异常继续向上传播。

```java
package io.github.atengk.concurrent.future;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CompletableFuture 异常处理示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class CompletableFutureExceptionDemo {

    public static void main(String[] args) {
        ThreadPoolExecutor executor = buildExecutor();

        try {
            CompletableFuture<String> userFuture = CompletableFuture
                    .supplyAsync(() -> queryRemoteUser("USER-1001"), executor)
                    .exceptionally(throwable -> {
                        log.error("远程用户信息查询失败，返回默认用户信息", throwable);
                        return "默认用户";
                    });

            String username = userFuture.join();
            log.info("最终用户名称：{}", username);
        } finally {
            executor.shutdown();
        }
    }

    private static String queryRemoteUser(String userId) {
        log.info("开始调用远程用户服务，用户 ID：{}", userId);
        ThreadUtil.sleep(800);

        throw new IllegalStateException("远程用户服务不可用");
    }

    private static ThreadPoolExecutor buildExecutor() {
        AtomicInteger threadIndex = new AtomicInteger(1);

        return new ThreadPoolExecutor(
                4,
                4,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                runnable -> new Thread(runnable, "exception-worker-" + threadIndex.getAndIncrement()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

如果需要同时处理成功和失败结果，可以使用 `handle()`。

文件位置：`src/main/java/io/github/atengk/concurrent/future/CompletableFutureHandleDemo.java`

这段代码使用 `handle()` 统一处理正常结果和异常结果，并转换为业务响应对象。

```java
package io.github.atengk.concurrent.future;

import cn.hutool.core.thread.ThreadUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CompletableFuture handle 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class CompletableFutureHandleDemo {

    public static void main(String[] args) {
        ThreadPoolExecutor executor = buildExecutor();

        try {
            CompletableFuture<QueryResult> resultFuture = CompletableFuture
                    .supplyAsync(() -> queryOrderAmount("ORDER-1001"), executor)
                    .handle((amount, throwable) -> {
                        if (throwable != null) {
                            log.error("订单金额查询失败，返回失败响应", throwable);
                            return new QueryResult(false, 0, "订单金额查询失败");
                        }

                        return new QueryResult(true, amount, "查询成功");
                    });

            QueryResult result = resultFuture.join();
            log.info("订单金额查询结果：{}", result);
        } finally {
            executor.shutdown();
        }
    }

    private static Integer queryOrderAmount(String orderNo) {
        log.info("开始查询订单金额，订单号：{}", orderNo);
        ThreadUtil.sleep(800);
        return 199;
    }

    private static ThreadPoolExecutor buildExecutor() {
        AtomicInteger threadIndex = new AtomicInteger(1);

        return new ThreadPoolExecutor(
                4,
                4,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                runnable -> new Thread(runnable, "handle-worker-" + threadIndex.getAndIncrement()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Data
    @AllArgsConstructor
    static class QueryResult {
        private Boolean success;
        private Integer data;
        private String message;
    }
}
```

`whenComplete()` 更适合做日志记录、指标统计、资源释放等旁路操作。它可以看到结果和异常，但通常不用于修改返回值。

```java
CompletableFuture<String> future = CompletableFuture
        .supplyAsync(() -> "业务结果")
        .whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("异步任务执行异常", throwable);
                return;
            }

            log.info("异步任务执行完成，结果：{}", result);
        });
```

异常处理的实践建议如下：

| 建议                   | 说明                                   |
| ---------------------- | -------------------------------------- |
| 子任务内部尽量明确日志 | 方便定位具体哪个异步任务失败           |
| 聚合任务要考虑部分失败 | 某些非核心信息可以降级返回             |
| 不要无脑吞异常         | 兜底时应记录日志或上报监控             |
| 关键链路避免静默失败   | 支付、扣库存、发货等核心链路应显式处理 |
| 使用超时控制           | 防止异步任务长期占用线程               |

### 线程池选择

`CompletableFuture` 如果不指定线程池，异步任务默认使用 `ForkJoinPool.commonPool()`。这在简单 Demo 中可以接受，但在生产环境中不推荐。

原因如下：

| 问题         | 说明                                        |
| ------------ | ------------------------------------------- |
| 线程池共享   | 多个业务共用默认线程池，容易互相影响        |
| 不便监控     | 线程名称不具备业务含义，排查困难            |
| 不便隔离     | 慢任务可能拖垮其他异步任务                  |
| 参数不可控   | 队列、线程数、拒绝策略不容易按业务调整      |
| 阻塞任务风险 | IO 阻塞任务占用默认池线程，影响其他计算任务 |

生产环境建议为不同业务配置独立线程池。例如订单查询线程池、消息通知线程池、报表导出线程池、第三方接口调用线程池等。

下面示例给出一个可以复用的业务线程池配置类。

文件位置：`src/main/java/io/github/atengk/concurrent/future/BusinessExecutorFactory.java`

这段代码封装业务线程池创建逻辑，统一设置线程名称、队列大小、拒绝策略和异常处理器。

```java
package io.github.atengk.concurrent.future;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务线程池工厂
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class BusinessExecutorFactory {

    private BusinessExecutorFactory() {
    }

    public static ThreadPoolExecutor build(String threadNamePrefix, int corePoolSize, int queueCapacity) {
        AtomicInteger threadIndex = new AtomicInteger(1);

        return new ThreadPoolExecutor(
                corePoolSize,
                corePoolSize,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                runnable -> {
                    Thread thread = new Thread(runnable, threadNamePrefix + "-" + threadIndex.getAndIncrement());
                    thread.setUncaughtExceptionHandler((t, e) ->
                            log.error("线程执行发生未捕获异常，线程名称：{}", t.getName(), e)
                    );
                    return thread;
                },
                (runnable, executor) -> {
                    log.error(
                            "线程池任务被拒绝，线程前缀：{}，活跃线程数：{}，队列任务数：{}，已完成任务数：{}",
                            threadNamePrefix,
                            executor.getActiveCount(),
                            executor.getQueue().size(),
                            executor.getCompletedTaskCount()
                    );

                    throw new java.util.concurrent.RejectedExecutionException("线程池任务被拒绝：" + threadNamePrefix);
                }
        );
    }
}
```

使用该线程池工厂执行 `CompletableFuture` 任务。

文件位置：`src/main/java/io/github/atengk/concurrent/future/CompletableFutureExecutorDemo.java`

这段代码演示不同业务使用不同线程池，避免查询任务和通知任务互相影响。

```java
package io.github.atengk.concurrent.future;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * CompletableFuture 线程池选择示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class CompletableFutureExecutorDemo {

    public static void main(String[] args) {
        ThreadPoolExecutor queryExecutor = BusinessExecutorFactory.build("order-query-worker", 4, 100);
        ThreadPoolExecutor notifyExecutor = BusinessExecutorFactory.build("notify-worker", 2, 50);

        try {
            CompletableFuture<String> orderFuture = CompletableFuture.supplyAsync(() -> {
                log.info("查询订单信息，线程名称：{}", Thread.currentThread().getName());
                ThreadUtil.sleep(1000);
                return "ORDER-1001";
            }, queryExecutor);

            CompletableFuture<Void> notifyFuture = orderFuture.thenAcceptAsync(orderNo -> {
                log.info("发送订单通知，订单号：{}，线程名称：{}", orderNo, Thread.currentThread().getName());
                ThreadUtil.sleep(800);
                log.info("订单通知发送完成，订单号：{}", orderNo);
            }, notifyExecutor);

            notifyFuture.join();
            log.info("异步流程执行完成");
        } finally {
            queryExecutor.shutdown();
            notifyExecutor.shutdown();
        }
    }
}
```

线程池选择建议如下：

| 任务类型       | 建议                                      |
| -------------- | ----------------------------------------- |
| CPU 密集型任务 | 线程数接近 CPU 核心数，避免过多上下文切换 |
| IO 密集型任务  | 可以适当增加线程数，但必须结合压测结果    |
| 第三方接口调用 | 独立线程池，并设置超时、限流、熔断        |
| 消息通知       | 独立线程池，允许失败重试或降级            |
| 报表导出       | 独立低优先级线程池，避免影响核心业务      |
| 核心交易链路   | 谨慎异步化，必须明确一致性和失败补偿策略  |

`CompletableFuture` 的常见使用规范如下：

| 规范           | 说明                                          |
| -------------- | --------------------------------------------- |
| 显式指定线程池 | 避免使用默认公共线程池                        |
| 区分业务线程池 | 不同业务按重要性和耗时隔离                    |
| 设置有界队列   | 防止任务无限堆积                              |
| 配置拒绝策略   | 线程池满时必须有明确处理                      |
| 增加异常处理   | 防止异步任务失败后无人感知                    |
| 避免过度异步   | 任务很轻或强依赖顺序时不必异步                |
| 注意事务边界   | Spring 事务上下文不会自动跨线程传播           |
| 注意请求上下文 | `ThreadLocal`、用户信息、链路 ID 需要显式传递 |





## 并发集合

并发集合用于解决多线程环境下集合读写的线程安全问题。普通集合如 `HashMap`、`ArrayList`、`LinkedList` 在并发修改时可能出现数据错乱、覆盖更新、死循环、异常或结果不一致。

Java 并发包中常见并发集合包括 `ConcurrentHashMap`、`CopyOnWriteArrayList`、`BlockingQueue` 和 `ConcurrentLinkedQueue`。它们各自适合不同场景，不能简单互相替代。

### ConcurrentHashMap

`ConcurrentHashMap` 是线程安全的哈希表，适合高并发读写场景。相比 `Hashtable` 或 `Collections.synchronizedMap()`，它在并发性能和锁粒度方面更适合现代 Java 应用。

`ConcurrentHashMap` 常见特点如下：

| 特点                 | 说明                                               |
| -------------------- | -------------------------------------------------- |
| 线程安全             | 支持多线程并发读写                                 |
| 高并发性能较好       | 读操作通常不加全局锁                               |
| 不允许 `null` 键和值 | 避免并发环境下语义歧义                             |
| 支持原子操作         | 如 `putIfAbsent()`、`computeIfAbsent()`、`merge()` |
| 适合缓存场景         | 常用于本地缓存、统计映射、任务状态表               |

需要注意，`ConcurrentHashMap` 保证的是单个方法调用的线程安全。如果多个操作组合在一起，仍然需要使用原子方法或额外同步控制。

例如下面这种写法不是严格安全的：

```java
if (!map.containsKey(key)) {
    map.put(key, value);
}
```

更推荐使用：

```java
map.putIfAbsent(key, value);
```

下面示例使用 `ConcurrentHashMap` 和 `LongAdder` 实现高并发词频统计。

文件位置：`src/main/java/io/github/atengk/concurrent/collection/ConcurrentHashMapDemo.java`

这段代码使用 `ConcurrentHashMap.computeIfAbsent()` 保证计数器初始化安全，再使用 `LongAdder` 降低高并发累加竞争。

```java
package io.github.atengk.concurrent.collection;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * ConcurrentHashMap 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ConcurrentHashMapDemo {

    private static final Map<String, LongAdder> WORD_COUNT_MAP = new ConcurrentHashMap<>();

    public static void main(String[] args) throws InterruptedException {
        List<String> wordList = CollUtil.newArrayList(
                "java", "spring", "java", "mysql", "redis",
                "java", "redis", "spring", "concurrent", "java"
        );

        Thread threadA = new Thread(() -> countWords(wordList), "word-count-worker-a");
        Thread threadB = new Thread(() -> countWords(wordList), "word-count-worker-b");

        threadA.start();
        threadB.start();

        threadA.join();
        threadB.join();

        WORD_COUNT_MAP.forEach((word, count) ->
                log.info("词频统计结果，word：{}，count：{}", word, count.sum())
        );
    }

    /**
     * 统计词频
     *
     * @param wordList 单词列表
     */
    private static void countWords(List<String> wordList) {
        for (String word : wordList) {
            WORD_COUNT_MAP.computeIfAbsent(word, key -> new LongAdder()).increment();
        }
    }
}
```

`ConcurrentHashMap` 常用方法如下：

| 方法                                       | 说明                        |
| ------------------------------------------ | --------------------------- |
| `putIfAbsent(key, value)`                  | key 不存在时才写入          |
| `computeIfAbsent(key, mappingFunction)`    | key 不存在时计算并写入      |
| `computeIfPresent(key, remappingFunction)` | key 存在时重新计算          |
| `compute(key, remappingFunction)`          | 根据 key 统一计算新值       |
| `merge(key, value, remappingFunction)`     | 合并旧值和新值              |
| `remove(key, value)`                       | key 和 value 都匹配时才删除 |

实际开发中，`ConcurrentHashMap` 常用于本地缓存、接口幂等记录、任务状态管理、连接映射、统计计数等场景。

### CopyOnWriteArrayList

`CopyOnWriteArrayList` 是线程安全的 List。它的核心思想是写时复制：读操作不加锁，写操作会复制一份新的数组，在新数组上修改完成后再替换原数组引用。

它适合读多写少的场景，例如配置列表、白名单、黑名单、监听器列表、菜单缓存等。

`CopyOnWriteArrayList` 的特点如下：

| 特点         | 说明                             |
| ------------ | -------------------------------- |
| 读操作性能好 | 读操作通常不加锁                 |
| 写操作成本高 | 每次写入都会复制数组             |
| 弱一致性迭代 | 迭代时看到的是创建迭代器时的快照 |
| 适合读多写少 | 不适合频繁写入场景               |

下面示例使用 `CopyOnWriteArrayList` 管理系统白名单。

文件位置：`src/main/java/io/github/atengk/concurrent/collection/CopyOnWriteArrayListDemo.java`

这段代码使用 `CopyOnWriteArrayList` 实现多线程安全读取白名单，并演示运行时动态新增白名单。

```java
package io.github.atengk.concurrent.collection;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CopyOnWriteArrayList 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class CopyOnWriteArrayListDemo {

    private static final CopyOnWriteArrayList<String> WHITE_LIST = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        WHITE_LIST.addAll(CollUtil.newArrayList("admin", "system", "ops"));

        Thread readThread = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                checkUser("admin");
                checkUser("guest");
                ThreadUtil.sleep(500);
            }
        }, "white-list-reader");

        Thread writeThread = new Thread(() -> {
            ThreadUtil.sleep(1000);
            WHITE_LIST.addIfAbsent("guest");
            log.info("已新增白名单用户：guest");
        }, "white-list-writer");

        readThread.start();
        writeThread.start();

        readThread.join();
        writeThread.join();

        log.info("最终白名单列表：{}", WHITE_LIST);
    }

    /**
     * 检查用户是否在白名单中
     *
     * @param username 用户名
     */
    private static void checkUser(String username) {
        boolean allowed = WHITE_LIST.contains(username);
        log.info("白名单检查结果，username：{}，allowed：{}", username, allowed);
    }
}
```

`CopyOnWriteArrayList` 不适合高频写入场景。例如订单列表、实时消息列表、频繁更新的任务列表不适合使用它。因为每次新增、删除或修改都可能复制底层数组，数据量大时成本较高。

### BlockingQueue

`BlockingQueue` 是阻塞队列接口，常用于生产者消费者模型。它既可以作为线程之间传递任务或数据的缓冲区，也可以用于线程池的任务队列。

`BlockingQueue` 的核心特点是：队列为空时，消费者获取数据可以阻塞等待；队列满时，生产者写入数据可以阻塞等待。

常见实现类如下：

| 实现类                  | 说明                               |
| ----------------------- | ---------------------------------- |
| `ArrayBlockingQueue`    | 基于数组的有界阻塞队列             |
| `LinkedBlockingQueue`   | 基于链表的阻塞队列，可指定容量     |
| `PriorityBlockingQueue` | 支持优先级排序的无界阻塞队列       |
| `DelayQueue`            | 延迟队列，元素到期后才能被消费     |
| `SynchronousQueue`      | 不存储元素，生产和消费必须直接匹配 |

常用方法如下：

| 方法                   | 队列满时     | 队列空时     | 说明             |
| ---------------------- | ------------ | ------------ | ---------------- |
| `add()`                | 抛异常       | -            | 插入失败抛异常   |
| `offer()`              | 返回 `false` | -            | 插入失败不阻塞   |
| `offer(timeout, unit)` | 等待指定时间 | -            | 超时返回 `false` |
| `put()`                | 一直阻塞     | -            | 队列满时阻塞     |
| `remove()`             | -            | 抛异常       | 获取失败抛异常   |
| `poll()`               | -            | 返回 `null`  | 获取失败不阻塞   |
| `poll(timeout, unit)`  | -            | 等待指定时间 | 超时返回 `null`  |
| `take()`               | -            | 一直阻塞     | 队列空时阻塞     |

下面示例使用 `ArrayBlockingQueue` 实现生产者消费者模型。

文件位置：`src/main/java/io/github/atengk/concurrent/collection/BlockingQueueDemo.java`

这段代码使用有界阻塞队列传递订单任务，生产者负责投递订单，消费者负责处理订单。

```java
package io.github.atengk.concurrent.collection;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * BlockingQueue 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class BlockingQueueDemo {

    private static final String STOP_SIGNAL = "STOP";

    private static final BlockingQueue<String> ORDER_QUEUE = new ArrayBlockingQueue<>(10);

    public static void main(String[] args) throws InterruptedException {
        Thread producerThread = new Thread(BlockingQueueDemo::produceOrder, "order-producer");
        Thread consumerThread = new Thread(BlockingQueueDemo::consumeOrder, "order-consumer");

        producerThread.start();
        consumerThread.start();

        producerThread.join();
        consumerThread.join();

        log.info("生产者消费者任务执行完成");
    }

    /**
     * 生产订单任务
     */
    private static void produceOrder() {
        try {
            for (int i = 1; i <= 5; i++) {
                String orderNo = "ORDER-" + i;
                boolean offered = ORDER_QUEUE.offer(orderNo, 1, TimeUnit.SECONDS);

                if (offered) {
                    log.info("订单任务投递成功，订单号：{}", orderNo);
                } else {
                    log.warn("订单任务投递失败，队列已满，订单号：{}", orderNo);
                }

                ThreadUtil.sleep(300);
            }

            ORDER_QUEUE.put(STOP_SIGNAL);
            log.info("生产者已发送停止信号");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("生产订单任务时线程被中断", e);
        }
    }

    /**
     * 消费订单任务
     */
    private static void consumeOrder() {
        try {
            while (true) {
                String orderNo = ORDER_QUEUE.poll(2, TimeUnit.SECONDS);

                if (orderNo == null) {
                    log.warn("等待订单任务超时，消费者退出");
                    break;
                }

                if (STOP_SIGNAL.equals(orderNo)) {
                    log.info("消费者收到停止信号，准备退出");
                    break;
                }

                log.info("开始处理订单，订单号：{}", orderNo);
                ThreadUtil.sleep(800);
                log.info("订单处理完成，订单号：{}", orderNo);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("消费订单任务时线程被中断", e);
        }
    }
}
```

生产环境中更推荐使用有界队列，避免任务无限堆积造成内存溢出。对于重要任务，还需要考虑失败重试、持久化、幂等处理和消费确认机制。

### ConcurrentLinkedQueue

`ConcurrentLinkedQueue` 是线程安全的非阻塞队列，基于链表结构实现，适合多线程并发入队和出队。

它和 `BlockingQueue` 的区别在于：`ConcurrentLinkedQueue` 不提供阻塞等待能力。如果队列为空，`poll()` 会立即返回 `null`，不会等待新元素进入队列。

`ConcurrentLinkedQueue` 的特点如下：

| 特点             | 说明                           |
| ---------------- | ------------------------------ |
| 线程安全         | 支持多线程并发入队和出队       |
| 非阻塞           | 队列为空时立即返回             |
| 无界队列         | 默认没有容量限制               |
| 适合轻量任务缓冲 | 常用于事件暂存、异步日志缓冲等 |
| 不适合背压控制   | 无容量限制，流量过大时可能堆积 |

下面示例使用 `ConcurrentLinkedQueue` 实现简单事件队列。

文件位置：`src/main/java/io/github/atengk/concurrent/collection/ConcurrentLinkedQueueDemo.java`

这段代码使用 `ConcurrentLinkedQueue` 收集多个线程产生的事件，再由消费者线程轮询处理。

```java
package io.github.atengk.concurrent.collection;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ConcurrentLinkedQueue 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ConcurrentLinkedQueueDemo {

    private static final Queue<String> EVENT_QUEUE = new ConcurrentLinkedQueue<>();

    private static final AtomicBoolean PRODUCING = new AtomicBoolean(true);

    public static void main(String[] args) throws InterruptedException {
        Thread producerA = new Thread(() -> produceEvent("A"), "event-producer-a");
        Thread producerB = new Thread(() -> produceEvent("B"), "event-producer-b");
        Thread consumer = new Thread(ConcurrentLinkedQueueDemo::consumeEvent, "event-consumer");

        producerA.start();
        producerB.start();
        consumer.start();

        producerA.join();
        producerB.join();

        PRODUCING.set(false);

        consumer.join();

        log.info("事件队列处理完成");
    }

    /**
     * 生产事件
     *
     * @param producerName 生产者名称
     */
    private static void produceEvent(String producerName) {
        for (int i = 1; i <= 5; i++) {
            String event = "EVENT-" + producerName + "-" + i;
            EVENT_QUEUE.offer(event);
            log.info("事件写入队列：{}", event);
            ThreadUtil.sleep(300);
        }
    }

    /**
     * 消费事件
     */
    private static void consumeEvent() {
        while (PRODUCING.get() || !EVENT_QUEUE.isEmpty()) {
            String event = EVENT_QUEUE.poll();

            if (event == null) {
                ThreadUtil.sleep(100);
                continue;
            }

            log.info("消费事件：{}", event);
        }
    }
}
```

`ConcurrentLinkedQueue` 适合非阻塞、轻量级、多生产者多消费者的队列场景。如果业务需要队列为空时阻塞等待，或者需要队列满时阻塞生产者，应使用 `BlockingQueue`。

## 常见问题

本节整理 Java 并发基础中常见面试题和开发问题。这些问题虽然看起来偏理论，但本质上都和线程状态、锁机制、线程池执行流程、内存可见性有关。

### sleep 与 wait 的区别

`Thread.sleep()` 和 `Object.wait()` 都可以让当前线程暂停执行，但二者语义完全不同。

| 对比项     | `sleep()`                  | `wait()`                                   |
| ---------- | -------------------------- | ------------------------------------------ |
| 所属类     | `Thread` 静态方法          | `Object` 实例方法                          |
| 是否释放锁 | 不释放锁                   | 释放当前对象监视器锁                       |
| 使用前提   | 任意位置都可以调用         | 必须在 `synchronized` 中调用               |
| 唤醒方式   | 时间到后自动恢复，或被中断 | 需要 `notify()`、`notifyAll()`、超时或中断 |
| 主要用途   | 暂停当前线程一段时间       | 线程间协作通信                             |
| 对应状态   | `TIMED_WAITING`            | `WAITING` 或 `TIMED_WAITING`               |

`sleep()` 的重点是让线程暂停一段时间，但不会释放锁。如果线程在 `synchronized` 内部调用 `sleep()`，其他线程仍然无法进入同一把锁保护的同步代码。

`wait()` 的重点是线程协作。线程调用 `wait()` 后会释放当前对象锁，并进入等待状态，直到其他线程调用 `notify()` 或 `notifyAll()` 唤醒它。

下面示例演示 `sleep()` 不释放锁。

文件位置：`src/main/java/io/github/atengk/concurrent/question/SleepLockDemo.java`

这段代码中线程 A 在同步代码块中调用 `sleep()`，线程 B 必须等待线程 A 退出同步代码块后才能获取锁。

```java
package io.github.atengk.concurrent.question;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * sleep 不释放锁示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class SleepLockDemo {

    private static final Object LOCK = new Object();

    public static void main(String[] args) {
        Runnable task = () -> {
            synchronized (LOCK) {
                log.info("获取锁成功，线程名称：{}", Thread.currentThread().getName());
                ThreadUtil.sleep(2000);
                log.info("准备释放锁，线程名称：{}", Thread.currentThread().getName());
            }
        };

        new Thread(task, "sleep-worker-a").start();
        ThreadUtil.sleep(300);
        new Thread(task, "sleep-worker-b").start();
    }
}
```

下面示例演示 `wait()` 会释放锁。

文件位置：`src/main/java/io/github/atengk/concurrent/question/WaitReleaseLockDemo.java`

这段代码中线程 A 调用 `wait()` 后释放锁，线程 B 可以进入同步代码块并唤醒线程 A。

```java
package io.github.atengk.concurrent.question;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * wait 释放锁示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class WaitReleaseLockDemo {

    private static final Object LOCK = new Object();

    public static void main(String[] args) {
        Thread waitThread = new Thread(() -> {
            synchronized (LOCK) {
                try {
                    log.info("线程进入等待状态，线程名称：{}", Thread.currentThread().getName());
                    LOCK.wait();
                    log.info("线程被唤醒后继续执行，线程名称：{}", Thread.currentThread().getName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("线程等待时被中断", e);
                }
            }
        }, "wait-worker");

        Thread notifyThread = new Thread(() -> {
            ThreadUtil.sleep(1000);

            synchronized (LOCK) {
                log.info("线程获取锁成功，准备唤醒等待线程，线程名称：{}", Thread.currentThread().getName());
                LOCK.notifyAll();
            }
        }, "notify-worker");

        waitThread.start();
        notifyThread.start();
    }
}
```

### synchronized 与 ReentrantLock 的区别

`synchronized` 和 `ReentrantLock` 都可以实现互斥访问，保证临界区代码的原子性和可见性。区别在于 `synchronized` 是 JVM 内置锁，使用更简单；`ReentrantLock` 是 JUC 显式锁，功能更灵活。

| 对比项     | `synchronized`       | `ReentrantLock`            |
| ---------- | -------------------- | -------------------------- |
| 实现层面   | JVM 内置监视器锁     | JUC 显式锁                 |
| 锁释放     | 自动释放             | 必须手动 `unlock()`        |
| 可重入     | 支持                 | 支持                       |
| 公平锁     | 不支持配置           | 支持公平锁和非公平锁       |
| 尝试加锁   | 不支持               | 支持 `tryLock()`           |
| 可中断等待 | 不支持               | 支持 `lockInterruptibly()` |
| 条件队列   | `wait()`、`notify()` | 支持多个 `Condition`       |
| 使用难度   | 简单                 | 稍复杂                     |
| 适合场景   | 普通同步代码         | 复杂锁控制场景             |

选择建议如下：

| 场景                 | 建议                                     |
| -------------------- | ---------------------------------------- |
| 简单临界区同步       | 优先使用 `synchronized`                  |
| 需要尝试获取锁       | 使用 `ReentrantLock.tryLock()`           |
| 需要等待锁时响应中断 | 使用 `ReentrantLock.lockInterruptibly()` |
| 需要公平锁           | 使用 `new ReentrantLock(true)`           |
| 需要多个条件队列     | 使用 `ReentrantLock` + `Condition`       |

使用 `ReentrantLock` 时，必须将 `unlock()` 放在 `finally` 中：

```java
lock.lock();
try {
    // 执行业务逻辑
} finally {
    lock.unlock();
}
```

如果忘记释放锁，会导致其他线程永久等待，进而引发线程堆积或服务不可用。

### submit 与 execute 的区别

`execute()` 和 `submit()` 都可以向线程池提交任务，但它们的返回值、异常表现和支持任务类型不同。

| 对比项   | `execute()`                        | `submit()`                     |
| -------- | ---------------------------------- | ------------------------------ |
| 所属接口 | `Executor`                         | `ExecutorService`              |
| 返回值   | 无返回值                           | 返回 `Future`                  |
| 支持任务 | `Runnable`                         | `Runnable`、`Callable`         |
| 异常表现 | 异常会被线程的未捕获异常处理器感知 | 异常会封装到 `Future` 中       |
| 获取结果 | 不支持                             | 可通过 `Future.get()` 获取结果 |
| 适合场景 | 只执行任务，不关心结果             | 需要结果或需要捕获任务异常     |

下面示例演示 `execute()` 和 `submit()` 的区别。

文件位置：`src/main/java/io/github/atengk/concurrent/question/ExecuteSubmitDemo.java`

这段代码分别使用 `execute()` 和 `submit()` 提交异常任务，展示异常处理差异。

```java
package io.github.atengk.concurrent.question;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * execute 和 submit 区别示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ExecuteSubmitDemo {

    public static void main(String[] args) {
        ThreadPoolExecutor executor = buildExecutor();

        try {
            executor.execute(() -> {
                log.info("execute 任务开始执行");
                throw new IllegalStateException("execute 任务异常");
            });

            Future<String> future = executor.submit(() -> {
                log.info("submit 任务开始执行");
                throw new IllegalStateException("submit 任务异常");
            });

            try {
                String result = future.get();
                log.info("submit 任务执行结果：{}", result);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待 submit 任务结果时线程被中断", e);
            } catch (ExecutionException e) {
                log.error("submit 任务执行异常，需要通过 Future.get() 获取", e);
            }
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 创建线程池
     *
     * @return 线程池
     */
    private static ThreadPoolExecutor buildExecutor() {
        AtomicInteger threadIndex = new AtomicInteger(1);

        return new ThreadPoolExecutor(
                2,
                2,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                runnable -> {
                    Thread thread = new Thread(runnable, "execute-submit-worker-" + threadIndex.getAndIncrement());
                    thread.setUncaughtExceptionHandler((t, e) ->
                            log.error("线程出现未捕获异常，线程名称：{}", t.getName(), e)
                    );
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

实际开发中，如果只是提交无需返回值的任务，可以使用 `execute()`。如果需要获取任务结果、判断任务是否完成或捕获任务内部异常，可以使用 `submit()`。

### 线程池为什么不建议使用 Executors

`Executors` 提供了多个快捷方法创建线程池，例如 `newFixedThreadPool()`、`newCachedThreadPool()`、`newSingleThreadExecutor()` 和 `newScheduledThreadPool()`。这些方法使用方便，但容易隐藏关键参数，导致生产环境风险不可控。

常见问题如下：

| 方法                                  | 潜在风险                                         |
| ------------------------------------- | ------------------------------------------------ |
| `Executors.newFixedThreadPool()`      | 使用近似无界队列，任务堆积可能导致 OOM           |
| `Executors.newSingleThreadExecutor()` | 使用近似无界队列，单线程处理慢时任务无限堆积     |
| `Executors.newCachedThreadPool()`     | 最大线程数接近无限，高并发下可能创建大量线程     |
| `Executors.newScheduledThreadPool()`  | 最大线程数接近无限，任务堆积或异常调度时风险较高 |

例如 `newFixedThreadPool()` 底层使用的是 `LinkedBlockingQueue`，默认容量非常大，任务持续堆积时可能占满内存。

生产环境更推荐直接使用 `ThreadPoolExecutor`，显式指定：

| 参数       | 目的                       |
| ---------- | -------------------------- |
| 核心线程数 | 控制基础并发能力           |
| 最大线程数 | 控制最大并发上限           |
| 有界队列   | 防止任务无限堆积           |
| 线程工厂   | 设置业务线程名称，方便排查 |
| 拒绝策略   | 过载时明确处理，不静默失败 |

推荐写法如下：

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
        4,
        8,
        60,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(500),
        runnable -> new Thread(runnable, "business-worker"),
        new ThreadPoolExecutor.CallerRunsPolicy()
);
```

更完整的项目中，应将线程池封装为配置类或工厂类，统一命名、统一拒绝策略、统一监控指标，并在应用关闭时优雅停机。

线程池使用规范可以概括为：

| 规范                 | 说明                               |
| -------------------- | ---------------------------------- |
| 不使用无界队列       | 防止任务无限堆积                   |
| 不使用无限线程数     | 防止线程数量失控                   |
| 必须设置线程名称     | 便于日志和线程栈排查               |
| 必须设置拒绝策略     | 过载时要有明确反馈                 |
| 不同业务隔离线程池   | 避免慢任务拖垮核心业务             |
| 增加监控指标         | 关注活跃线程数、队列长度、拒绝次数 |
| 应用停止时关闭线程池 | 避免线程泄漏                       |

### volatile 能否保证原子性

`volatile` 不能保证原子性。

`volatile` 主要保证两个能力：

| 能力   | 说明                                   |
| ------ | -------------------------------------- |
| 可见性 | 一个线程修改变量后，其他线程能及时看到 |
| 有序性 | 禁止与该变量相关的特定指令重排         |

但 `volatile` 不能保证复合操作的原子性。例如：

```java
private static volatile int count = 0;

count++;
```

`count++` 看起来是一行代码，实际包含三个步骤：

```java
int temp = count;
temp = temp + 1;
count = temp;
```

多个线程同时执行时，可能都读取到同一个旧值，然后分别写回相同的新值，导致更新丢失。

下面示例演示 `volatile` 修饰的变量执行自增仍然不是线程安全的。

文件位置：`src/main/java/io/github/atengk/concurrent/question/VolatileAtomicQuestionDemo.java`

这段代码使用 `volatile int` 执行多线程自增，最终结果通常小于理论值，用于说明 `volatile` 不能保证原子性。

```java
package io.github.atengk.concurrent.question;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * volatile 不能保证原子性示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class VolatileAtomicQuestionDemo {

    private static volatile int count = 0;

    public static void main(String[] args) {
        List<Thread> threadList = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 1; j <= 10000; j++) {
                    count++;
                }
            }, "volatile-atomic-worker-" + i);

            threadList.add(thread);
            thread.start();
        }

        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待线程执行完成时被中断", e);
            }
        }

        log.info("理论结果：{}，实际结果：{}", 10 * 10000, count);
    }
}
```

如果需要保证自增的原子性，可以使用 `AtomicInteger`。

```java
private static final AtomicInteger COUNT = new AtomicInteger(0);

COUNT.incrementAndGet();
```

也可以使用 `synchronized` 或 `ReentrantLock` 包裹临界区。

选择建议如下：

| 需求             | 推荐方式                        |
| ---------------- | ------------------------------- |
| 状态开关         | `volatile boolean`              |
| 初始化完成标记   | `volatile boolean`              |
| 简单计数         | `AtomicInteger`、`AtomicLong`   |
| 高并发统计       | `LongAdder`                     |
| 复合业务逻辑     | `synchronized`、`ReentrantLock` |
| 多字段一致性更新 | 锁、事务或不可变对象替换        |

