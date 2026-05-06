# 虚拟线程

虚拟线程是 JDK 21 正式引入的轻量级线程能力，适合用于提升大量并发阻塞任务的处理能力。它的核心目标不是替代业务代码中的所有线程模型，而是在保持同步阻塞编程风格的前提下，降低传统平台线程在高并发场景下的创建、调度和阻塞成本。JEP 444 将虚拟线程定位为 Java 平台能力，用于简化高吞吐并发应用的编写、维护和观测。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

## 技术概述

本节主要说明虚拟线程的技术定位、与传统平台线程的区别，以及在 Spring Boot 3 后端项目中的适用边界。虚拟线程不是响应式框架，也不是异步回调模型，而是 JDK 层面对线程实现方式的增强。

### 虚拟线程的定位

虚拟线程是 `java.lang.Thread` 的一种轻量级实现，由 JVM 负责调度，而不是和操作系统线程一一绑定。传统平台线程在执行期间通常会长期占用一个底层操作系统线程，而虚拟线程在执行阻塞 IO 操作时，可以释放其承载的平台线程，使该平台线程继续执行其他虚拟线程任务。JDK 文档中将这种承载虚拟线程运行的平台线程称为 carrier thread。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

在 Spring Boot 3 项目中，虚拟线程的主要价值是保留传统的同步代码编写方式。例如 Controller 调用 Service，Service 调用数据库、Redis、HTTP 客户端、文件系统或第三方 SDK 时，仍然可以按照直观的阻塞式代码编写，而不必为了提升并发能力强制改造成复杂的异步回调、`CompletableFuture` 链式编排或响应式 `Mono` / `Flux` 模型。

虚拟线程更适合解决“并发任务多、但大部分时间在等待外部资源”的问题。例如接口等待数据库返回、等待远程 HTTP 响应、等待 Redis 查询、等待对象存储上传下载等。它不会提升单个 CPU 计算任务的执行速度，也不会突破数据库连接池、HTTP 连接池、下游接口限流等外部资源瓶颈。

### 平台线程与虚拟线程的区别

平台线程是传统 Java 线程模型中的线程。每个 Java 平台线程通常对应一个操作系统线程，创建成本和调度成本较高，因此在实际开发中通常需要使用线程池复用线程。线程池大小一般需要根据 CPU 核数、任务阻塞比例、队列长度和业务吞吐目标进行配置。

虚拟线程则采用不同的使用方式。虚拟线程创建成本较低，推荐的使用方式是“每个任务创建一个虚拟线程”，而不是像平台线程一样通过固定线程池长期复用。JEP 444 明确说明，虚拟线程数量可以远大于操作系统线程数量，适合传统的 thread-per-request 服务端编程模型。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

| 对比项                | 平台线程                   | 虚拟线程                         |
| --------------------- | -------------------------- | -------------------------------- |
| 管理方                | 操作系统和 JVM             | JVM                              |
| 底层关系              | 通常与操作系统线程一一对应 | 多个虚拟线程可共享较少的平台线程 |
| 创建成本              | 较高                       | 较低                             |
| 使用方式              | 通常通过线程池复用         | 通常每个任务创建一个虚拟线程     |
| 阻塞 IO 行为          | 阻塞时通常占用操作系统线程 | 多数 JDK 阻塞操作可释放承载线程  |
| 适合场景              | CPU 密集任务、少量长期任务 | 大量并发 IO 阻塞任务             |
| 是否提升 CPU 计算速度 | 否                         | 否                               |
| ThreadLocal 支持      | 支持                       | 支持，但需要谨慎使用             |
| 生命周期              | 通常由线程池长期维护       | 通常随任务创建和结束             |

需要特别注意的是，虚拟线程并不是“更快的 CPU 线程”。如果任务主要消耗 CPU，例如大批量加密、图片处理、复杂报表计算、压缩解压、规则引擎计算等，创建大量虚拟线程并不能提升吞吐，反而可能增加调度压力。JEP 444 明确指出，对于 CPU-bound 工作负载，线程数量远大于处理器核心数并不能提升吞吐。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

### 适用场景与不适用场景

虚拟线程更适合 IO 密集型业务。典型场景包括 Web 接口请求处理、远程 HTTP 调用、数据库查询、Redis 访问、消息投递、文件上传下载、对象存储访问、第三方 SDK 同步调用等。这些场景的共同特点是业务线程经常处于等待状态，虚拟线程可以减少平台线程被长时间占用的问题。

| 场景                      | 是否推荐 | 说明                                                         |
| ------------------------- | -------- | ------------------------------------------------------------ |
| Spring MVC 同步接口       | 推荐     | 保持传统 Controller / Service 写法，适合高并发请求处理       |
| 阻塞式数据库访问          | 推荐     | 适合 JDBC、MyBatis、MyBatis-Plus 等同步调用，但仍受连接池限制 |
| 阻塞式 HTTP 客户端        | 推荐     | 适合 `RestClient`、`RestTemplate`、OpenFeign 等同步调用      |
| 文件 IO / 网络 IO         | 推荐     | 适合大量等待型任务                                           |
| `@Async` 异步任务         | 可以使用 | Spring Boot 启用虚拟线程后，自动配置的执行器可使用虚拟线程   |
| `@Scheduled` 定时任务     | 可以使用 | 需要关注虚拟线程是 daemon thread 的特性                      |
| CPU 密集型计算            | 不推荐   | 不会提升 CPU 计算能力，建议使用固定大小平台线程池            |
| 长时间持有锁的代码        | 谨慎使用 | `synchronized` 等场景可能导致虚拟线程 pinning，影响吞吐      |
| 强依赖 ThreadLocal 的代码 | 谨慎使用 | 虚拟线程数量可能很大，ThreadLocal 滥用会增加内存压力         |
| 连接池很小的外部资源      | 谨慎使用 | 虚拟线程不能突破数据库连接池、HTTP 连接池和下游限流          |

Spring Boot 官方文档也提示，启用虚拟线程前需要关注 pinned virtual threads 问题。部分场景下虚拟线程可能被固定在承载线程上，导致吞吐下降，可通过 JDK Flight Recorder 或 `jcmd` 进行分析。([Home](https://docs.spring.io/spring-boot/docs/3.2.9/reference/html/features.html?utm_source=chatgpt.com))

## 环境准备

本节用于说明在 JDK 21 和 Spring Boot 3 项目中使用虚拟线程需要准备的基础环境。虚拟线程属于 JDK 标准能力，不需要额外引入独立依赖，但项目必须使用满足要求的 JDK、Spring Boot 版本和构建配置。

### JDK 21 环境要求

虚拟线程在 JDK 21 中正式发布，因此项目运行环境必须使用 JDK 21 或更高版本。开发环境、编译环境、CI/CD 构建环境和容器运行环境应保持一致，避免本地可以运行但服务器或流水线构建失败。

检查本机 JDK 版本：

```bash
java -version
javac -version
```

期望输出中应包含 `21` 或更高版本，例如：

```bash
java version "21.0.x"
javac 21.0.x
```

检查 Maven 当前使用的 JDK 版本：

```bash
mvn -version
```

重点查看输出中的 `Java version`。如果这里显示的是 JDK 8、JDK 11 或 JDK 17，即使系统中已经安装 JDK 21，Maven 构建时仍然不会使用 JDK 21，需要调整 `JAVA_HOME`。

Linux 环境可参考如下配置：

```bash
# 配置 JDK 21 安装目录，请根据实际路径调整
export JAVA_HOME=/usr/local/jdk-21

# 将 JDK 21 的 bin 目录放到 PATH 前面
export PATH=$JAVA_HOME/bin:$PATH

# 验证 Java 版本
java -version

# 验证 Maven 使用的 Java 版本
mvn -version
```

虚拟线程支持 `ThreadLocal` 和 `InheritableThreadLocal`，因此大部分已有 Java 代码可以较低成本迁移。但由于虚拟线程数量可能非常多，不建议使用 ThreadLocal 缓存大对象、连接对象或其他昂贵资源。JEP 444 明确说明，虚拟线程支持 ThreadLocal，但不应将 ThreadLocal 作为线程池场景下的资源复用手段。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

### Spring Boot 3 版本要求

Spring Boot 3 项目使用虚拟线程时，建议使用 Spring Boot 3.2 或更高版本。Spring Boot 官方文档说明，在 Java 21 或更高版本上，可以通过 `spring.threads.virtual.enabled=true` 启用虚拟线程。([Home](https://docs.spring.io/spring-boot/docs/3.2.9/reference/html/features.html?utm_source=chatgpt.com))

| 项目环境              | 建议                                                        |
| --------------------- | ----------------------------------------------------------- |
| Spring Boot 2.x       | 不建议直接规划虚拟线程改造，应先升级到 Spring Boot 3        |
| Spring Boot 3.0 / 3.1 | 可以直接使用 JDK 虚拟线程 API，但自动配置支持不如 3.2+ 完整 |
| Spring Boot 3.2+      | 推荐使用，可通过配置项启用虚拟线程                          |
| JDK 17 及以下         | 不满足虚拟线程正式版要求                                    |
| JDK 21+               | 满足虚拟线程正式版要求                                      |

启用虚拟线程后，部分传统线程池配置项不再按原有方式生效。Spring Boot 官方文档说明，虚拟线程由 JVM 级别的平台线程池调度，而不是由应用自定义线程池直接承载，因此配置线程池的属性可能不再产生原来的效果。([Home](https://docs.spring.io/spring-boot/docs/3.2.9/reference/html/features.html?utm_source=chatgpt.com))

如果项目中存在 `@Scheduled` 定时任务，还需要关注虚拟线程的 daemon thread 特性。虚拟线程是守护线程，当 JVM 中只剩守护线程时，应用可能退出。Spring Boot 官方建议在这种情况下配置 `spring.main.keep-alive=true`，确保 JVM 持续运行。([Home](https://docs.spring.io/spring-boot/docs/3.2.9/reference/html/features.html?utm_source=chatgpt.com))

### Maven 依赖配置

虚拟线程本身不需要单独的 Maven 依赖，它属于 JDK 21 标准能力。Maven 配置的重点是确保项目使用 Spring Boot 3.2+，并将 Java 编译版本设置为 21。

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 3.2+ 对虚拟线程提供更完整的自动配置支持 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.13</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-boot-virtual-thread-demo</artifactId>
    <version>1.0.0</version>
    <name>spring-boot-virtual-thread-demo</name>
    <description>Spring Boot 3 with JDK 21 virtual thread demo</description>

    <properties>
        <!-- 虚拟线程正式版要求 JDK 21 或更高版本 -->
        <java.version>21</java.version>

        <!-- 编译输出版本固定为 Java 21，避免本地和 CI/CD 环境不一致 -->
        <maven.compiler.release>21</maven.compiler.release>

        <!-- Hutool 用于后续示例中的字符串、集合、时间等工具处理 -->
        <hutool.version>5.8.36</hutool.version>
    </properties>

    <dependencies>
        <!-- Spring MVC Web 项目基础依赖，适合同步阻塞风格接口 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Actuator 用于健康检查、运行状态观测和后续性能验证 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Hutool 工具包，用于处理字符串、集合、日期、JSON 等常见逻辑 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok 用于简化日志对象、DTO、VO 等样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 测试依赖，用于后续接口并发测试和线程验证 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot 打包插件，用于生成可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

            <!-- 显式指定 Java 21 编译版本，避免构建环境版本不一致 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <release>21</release>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

虚拟线程启用配置通常放在 `application.yml` 中。该配置也可以在后续“Spring Boot 3 虚拟线程支持”和“配置示例”章节中继续展开。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    # 应用名称，用于日志、监控和链路标识
    name: spring-boot-virtual-thread-demo

  threads:
    virtual:
      # 启用 Spring Boot 虚拟线程自动配置，要求 Java 21+
      enabled: true

  main:
    # 虚拟线程是守护线程；存在定时任务或纯后台任务时建议开启
    keep-alive: true

server:
  # 示例服务端口
  port: 8080

logging:
  level:
    # 示例项目根包日志级别
    io.github.atengk: info
```

完成依赖和配置后，可以执行以下命令验证项目是否能在 JDK 21 下正常构建和启动：

```bash
# 清理并编译项目
mvn clean compile

# 执行测试
mvn test

# 打包项目
mvn clean package

# 启动项目
java -jar target/spring-boot-virtual-thread-demo-1.0.0.jar
```

以上命令分别用于编译、测试、打包和启动项目。执行 `mvn -version` 时应确认 Maven 使用的是 JDK 21；执行 `java -jar` 时应确认运行环境同样是 JDK 21 或更高版本。项目正常启动后，说明基础环境已经满足虚拟线程开发要求。



## JDK 21 虚拟线程基础

本节用于说明 JDK 21 中虚拟线程的基础 API、任务提交方式、阻塞行为和 `ThreadLocal` 使用边界。虚拟线程属于 JDK 标准能力，不依赖 Spring，也不要求必须在 Web 项目中使用。

### 虚拟线程创建方式

JDK 21 中虚拟线程已经正式发布，可以通过 `Thread.startVirtualThread`、`Thread.ofVirtual()` 和 `ThreadFactory` 创建。虚拟线程仍然是 `java.lang.Thread` 的实例，因此可以使用 `join()`、`interrupt()`、`isVirtual()` 等常规线程 API。JEP 444 明确说明，`Thread.startVirtualThread(Runnable)` 是创建并启动虚拟线程的便捷方式，`Thread.Builder` 可用于创建未启动线程、命名线程或构建线程工厂。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

常见创建方式如下：

| 创建方式                                      | 说明                            | 适用场景               |
| --------------------------------------------- | ------------------------------- | ---------------------- |
| `Thread.startVirtualThread(...)`              | 创建并立即启动虚拟线程          | 简单任务、快速验证     |
| `Thread.ofVirtual().start(...)`               | 使用 Builder 创建并启动虚拟线程 | 需要设置线程名称等属性 |
| `Thread.ofVirtual().unstarted(...)`           | 创建未启动的虚拟线程            | 需要延迟启动           |
| `Thread.ofVirtual().factory()`                | 创建虚拟线程工厂                | 需要批量创建虚拟线程   |
| `Executors.newVirtualThreadPerTaskExecutor()` | 每个任务创建一个虚拟线程        | 批量任务、并发 IO 任务 |

文件位置：`src/test/java/io/github/atengk/virtualthread/VirtualThreadCreateDemo.java`

下面的代码演示 JDK 21 中几种常见的虚拟线程创建方式。

```java
package io.github.atengk.virtualthread;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadFactory;

/**
 * 虚拟线程创建方式示例。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class VirtualThreadCreateDemo {

    public static void main(String[] args) throws InterruptedException {
        Thread quickThread = Thread.startVirtualThread(() -> logThread("Thread.startVirtualThread"));

        Thread namedThread = Thread.ofVirtual()
                .name("vt-named-", 1)
                .start(() -> logThread("Thread.ofVirtual().start"));

        Thread unstartedThread = Thread.ofVirtual()
                .name("vt-unstarted")
                .unstarted(() -> logThread("Thread.ofVirtual().unstarted"));
        unstartedThread.start();

        ThreadFactory threadFactory = Thread.ofVirtual()
                .name("vt-factory-", 1)
                .factory();
        Thread factoryThread = threadFactory.newThread(() -> logThread("ThreadFactory"));
        factoryThread.start();

        quickThread.join();
        namedThread.join();
        unstartedThread.join();
        factoryThread.join();
    }

    /**
     * 打印当前线程信息。
     *
     * @param source 创建来源
     */
    private static void logThread(String source) {
        Thread currentThread = Thread.currentThread();
        log.info("创建方式：{}，线程名称：{}，是否虚拟线程：{}",
                source,
                currentThread.getName(),
                currentThread.isVirtual());

        ThreadUtil.sleep(300);
    }

}
```

运行后重点观察 `是否虚拟线程` 字段。如果输出为 `true`，说明当前任务确实运行在虚拟线程中。

需要注意，`new Thread(...)` 创建的是平台线程，不是虚拟线程。虚拟线程必须通过 `Thread.ofVirtual()`、`Thread.startVirtualThread(...)` 或基于虚拟线程的 `ExecutorService` 创建。

### ExecutorService 使用方式

在实际业务开发中，更推荐使用 `Executors.newVirtualThreadPerTaskExecutor()` 提交批量任务。该执行器的特点是每提交一个任务，就创建一个新的虚拟线程执行任务；任务完成后，虚拟线程生命周期结束。JEP 444 的示例也使用该 API 提交大量并发任务，并通过 `try-with-resources` 在执行器关闭时等待任务完成。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

文件位置：`src/test/java/io/github/atengk/virtualthread/VirtualThreadExecutorDemo.java`

下面的代码演示通过 `ExecutorService` 执行多个模拟远程调用任务。

```java
package io.github.atengk.virtualthread;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

/**
 * 虚拟线程 ExecutorService 使用示例。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class VirtualThreadExecutorDemo {

    public static void main(String[] args) throws Exception {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = IntStream.rangeClosed(1, 5)
                    .mapToObj(index -> executorService.submit(() -> queryRemoteData(index)))
                    .toList();

            for (Future<String> future : futures) {
                log.info("任务执行结果：{}", future.get());
            }
        }
    }

    /**
     * 模拟远程 IO 调用。
     *
     * @param index 任务编号
     * @return 执行结果
     */
    private static String queryRemoteData(Integer index) {
        Thread currentThread = Thread.currentThread();
        log.info("开始执行任务：{}，线程名称：{}，是否虚拟线程：{}",
                index,
                currentThread.getName(),
                currentThread.isVirtual());

        ThreadUtil.sleep(500);

        return "remote-data-" + index;
    }

}
```

这种方式适合在一个请求内并发执行多个互不依赖的阻塞 IO 任务，例如同时调用多个远程接口、读取多个文件、查询多个外部系统等。

不建议把虚拟线程执行器再包装成固定大小线程池。虚拟线程的推荐模型是“每个任务一个虚拟线程”，而不是“少量线程反复复用”。如果需要限制并发数量，应优先限制外部资源，例如数据库连接池、HTTP 连接池、信号量、限流器或业务队列，而不是限制虚拟线程池大小。

### 线程休眠与阻塞行为

虚拟线程的优势主要体现在阻塞场景。虚拟线程执行阻塞操作时，JDK 可以将虚拟线程从承载它的平台线程上卸载，使平台线程继续执行其他虚拟线程任务。JEP 444 说明，虚拟线程由 JDK 调度器调度到平台线程上执行，多个虚拟线程可以共享较少的平台线程；同时，`LockSupport` 已支持虚拟线程，使基于它的锁、信号量、阻塞队列等工具在虚拟线程中可以更好地挂起和恢复。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

文件位置：`src/test/java/io/github/atengk/virtualthread/VirtualThreadBlockingDemo.java`

下面的代码通过大量虚拟线程模拟阻塞等待场景。

```java
package io.github.atengk.virtualthread;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

/**
 * 虚拟线程阻塞行为示例。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class VirtualThreadBlockingDemo {

    public static void main(String[] args) throws InterruptedException {
        int taskCount = 1000;
        CountDownLatch countDownLatch = new CountDownLatch(taskCount);
        long startTime = System.currentTimeMillis();

        IntStream.rangeClosed(1, taskCount).forEach(index ->
                Thread.startVirtualThread(() -> {
                    try {
                        mockBlockingTask(index);
                    } finally {
                        countDownLatch.countDown();
                    }
                })
        );

        countDownLatch.await();

        long cost = System.currentTimeMillis() - startTime;
        log.info("任务数量：{}，总耗时：{}ms", taskCount, cost);
    }

    /**
     * 模拟阻塞任务。
     *
     * @param index 任务编号
     */
    private static void mockBlockingTask(Integer index) {
        Thread currentThread = Thread.currentThread();

        if (index <= 5) {
            log.info("任务：{}，线程名称：{}，是否虚拟线程：{}",
                    index,
                    currentThread.getName(),
                    currentThread.isVirtual());
        }

        ThreadUtil.sleep(1000);
    }

}
```

该示例中每个任务都会休眠 1 秒。如果使用传统平台线程直接创建大量线程，线程创建和调度成本会明显增加；使用虚拟线程时，大量等待型任务可以更轻量地挂起和恢复。

需要注意，虚拟线程并不适合 CPU 密集型计算。如果任务一直占用 CPU，例如加密、压缩、图片处理、复杂计算等，虚拟线程不会让计算变快。此类任务仍应结合 CPU 核数使用有限数量的平台线程池。

另外，JDK 21 中仍需关注虚拟线程 pinning 问题。典型风险是虚拟线程在持有 `synchronized` 锁或执行某些本地方法时发生阻塞，可能导致承载线程无法及时释放。出现吞吐异常时，可以结合 JDK Flight Recorder 或 `jcmd` 进行排查。Spring Boot 官方文档也提示，启用虚拟线程前应关注 pinned virtual threads 对吞吐的影响。([Home](https://docs.spring.io/spring-boot/reference/features/spring-application.html?utm_source=chatgpt.com))

### ThreadLocal 使用注意事项

虚拟线程支持 `ThreadLocal` 和 `InheritableThreadLocal`，这有利于兼容现有框架和业务代码。但虚拟线程数量可能远大于平台线程数量，如果在 `ThreadLocal` 中存放大对象、连接对象、缓存对象或没有及时清理的数据，就可能造成明显的内存压力。JEP 444 明确说明，虚拟线程支持线程本地变量，但应谨慎使用，不应使用 ThreadLocal 在线程池语义下缓存昂贵资源。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

推荐使用原则如下：

| 原则               | 说明                                             |
| ------------------ | ------------------------------------------------ |
| 可以存放轻量上下文 | 例如请求 ID、租户 ID、用户 ID、链路追踪 ID       |
| 不要存放大对象     | 例如大集合、文件内容、大 JSON、图片字节          |
| 不要缓存连接资源   | 例如数据库连接、HTTP 连接、Redis 连接            |
| 必须及时清理       | 使用 `try-finally` 调用 `remove()`               |
| 避免跨任务复用假设 | 虚拟线程通常一个任务一个线程，任务结束后线程结束 |

文件位置：`src/test/java/io/github/atengk/virtualthread/VirtualThreadLocalDemo.java`

下面的代码演示在虚拟线程中安全使用 `ThreadLocal` 的方式。

```java
package io.github.atengk.virtualthread;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 虚拟线程 ThreadLocal 使用示例。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class VirtualThreadLocalDemo {

    private static final ThreadLocal<String> REQUEST_ID_CONTEXT = new ThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
        String requestId = IdUtil.fastSimpleUUID();

        Thread virtualThread = Thread.startVirtualThread(() -> handleRequest(requestId));

        virtualThread.join();
    }

    /**
     * 模拟请求处理。
     *
     * @param requestId 请求 ID
     */
    private static void handleRequest(String requestId) {
        try {
            REQUEST_ID_CONTEXT.set(requestId);

            Thread currentThread = Thread.currentThread();
            log.info("请求ID：{}，线程名称：{}，是否虚拟线程：{}",
                    REQUEST_ID_CONTEXT.get(),
                    currentThread.getName(),
                    currentThread.isVirtual());
        } finally {
            REQUEST_ID_CONTEXT.remove();
        }
    }

}
```

如果需要排查项目中是否大量使用了 `ThreadLocal`，可以在启动参数中加入：

```bash
java -Djdk.traceVirtualThreadLocals=true -jar target/spring-boot-virtual-thread-demo-1.0.0.jar
```

该参数用于在虚拟线程设置 `ThreadLocal` 值时输出诊断信息，便于定位哪些代码路径使用了线程本地变量。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

## Spring Boot 3 虚拟线程支持

本节用于说明 Spring Boot 3 项目如何启用虚拟线程，以及 Web 请求、异步任务、定时任务在虚拟线程环境下的配置方式。Spring Boot 3.2 开始提供虚拟线程支持，运行环境需要 Java 21 或更高版本，并通过 `spring.threads.virtual.enabled=true` 启用。([Home](https://docs.spring.io/spring-boot/reference/features/spring-application.html?utm_source=chatgpt.com))

### 全局启用虚拟线程

Spring Boot 3 中启用虚拟线程的核心配置是 `spring.threads.virtual.enabled=true`。启用后，Spring Boot 会根据运行环境自动配置相关执行器。官方文档同时提示，虚拟线程启用后，传统线程池相关配置不再按原有方式影响虚拟线程，因为虚拟线程调度依赖 JVM 级别的平台线程池，而不是应用内专用线程池。([Home](https://docs.spring.io/spring-boot/reference/features/spring-application.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

下面的配置用于在 Spring Boot 3 项目中全局启用虚拟线程。

```yaml
spring:
  application:
    # 应用名称，用于日志、监控和链路标识
    name: spring-boot-virtual-thread-demo

  threads:
    virtual:
      # 启用虚拟线程，要求 Java 21 或更高版本
      enabled: true

  main:
    # 虚拟线程是守护线程；存在定时任务或纯后台任务时建议开启
    keep-alive: true

server:
  # 示例服务端口
  port: 8080

logging:
  level:
    # 示例项目根包日志级别
    io.github.atengk: info
```

`spring.main.keep-alive=true` 在使用 `@Scheduled`、后台消费任务或其他非 Web 常驻任务时尤其重要。虚拟线程是守护线程，当 JVM 中只剩守护线程时，应用可能退出；Spring Boot 官方建议通过该配置保持 JVM 运行。([Home](https://docs.spring.io/spring-boot/reference/features/spring-application.html?utm_source=chatgpt.com))

启用后可以通过接口、异步任务或定时任务打印：

```java
Thread.currentThread().isVirtual()
```

如果返回 `true`，说明当前代码运行在虚拟线程中。

### Web 请求处理线程

在 Spring Boot 使用嵌入式 Servlet Web Server 的场景中，启用虚拟线程后，Tomcat 和 Jetty 可使用虚拟线程处理请求，Controller 方法中的应用代码会运行在虚拟线程上。Spring Boot 3.2 发布说明明确说明，启用虚拟线程后，Servlet Web Server 中的 Tomcat 和 Jetty 会使用虚拟线程进行请求处理。([GitHub](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.2.0-M1-Release-Notes?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/virtualthread/controller/VirtualThreadController.java`

下面的 Controller 用于验证当前 Web 请求是否运行在虚拟线程中。

```java
package io.github.atengk.virtualthread.controller;

import cn.hutool.core.lang.Dict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 虚拟线程请求验证接口。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/virtual-thread")
public class VirtualThreadController {

    /**
     * 查看当前请求线程信息。
     *
     * @return 当前线程信息
     */
    @GetMapping("/current")
    public Dict currentThread() {
        Thread currentThread = Thread.currentThread();

        log.info("当前请求线程：{}，是否虚拟线程：{}",
                currentThread.getName(),
                currentThread.isVirtual());

        return Dict.create()
                .set("threadName", currentThread.getName())
                .set("virtual", currentThread.isVirtual())
                .set("thread", currentThread.toString());
    }

}
```

启动项目后执行：

```bash
curl http://localhost:8080/virtual-thread/current
```

返回示例：

```json
{
  "threadName": "tomcat-handler-0",
  "virtual": true,
  "thread": "VirtualThread[#42,tomcat-handler-0]/runnable@ForkJoinPool-1-worker-1"
}
```

如果 `virtual` 为 `true`，说明当前请求处理逻辑运行在虚拟线程中。

需要注意，如果项目不是以 Spring Boot 内嵌容器方式运行，而是打成 WAR 部署到外部 Tomcat，则请求线程由外部 Tomcat 管理，是否使用虚拟线程取决于外部容器自身配置。Tomcat 官方 API 中的 `StandardVirtualThreadExecutor` 是“每个任务使用一个新的虚拟线程”的执行器，外部容器场景需要单独关注 Tomcat 版本和 Connector / Executor 配置。([tomcat.apache.org](https://tomcat.apache.org/tomcat-10.1-doc/api/org/apache/catalina/core/StandardVirtualThreadExecutor.html?utm_source=chatgpt.com))

### 异步任务线程配置

Spring Boot 在没有自定义 `Executor` Bean 的情况下，会自动配置 `AsyncTaskExecutor`。当 Java 版本满足要求且 `spring.threads.virtual.enabled=true` 时，该执行器会使用支持虚拟线程的 `SimpleAsyncTaskExecutor`；它会被用于 `@EnableAsync`、Spring MVC 异步请求处理、WebFlux 阻塞执行支持等场景。([Home](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html?utm_source=chatgpt.com))

如果项目只需要默认虚拟线程执行器，通常只需要启用虚拟线程并添加 `@EnableAsync`。

文件位置：`src/main/java/io/github/atengk/virtualthread/config/AsyncConfig.java`

下面的配置启用异步任务，并显式声明基于虚拟线程的 `applicationTaskExecutor`，便于统一线程名前缀和验证日志。

```java
package io.github.atengk.virtualthread.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步任务配置。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@EnableAsync
@Configuration
public class AsyncConfig {

    /**
     * 配置应用异步任务执行器。
     *
     * @return 虚拟线程任务执行器
     */
    @Bean("applicationTaskExecutor")
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new VirtualThreadTaskExecutor("async-vt-");
    }

}
```

`VirtualThreadTaskExecutor` 是 Spring Framework 提供的基于 JDK 21 虚拟线程的 `TaskExecutor` 实现，主要配置项是线程名前缀。如果需要并发限制或任务装饰能力，Spring 官方建议考虑使用 `SimpleAsyncTaskExecutor#setVirtualThreads(boolean)`。([Home](https://docs.spring.io/spring-framework/docs/6.2.6/javadoc-api/org/springframework/core/task/VirtualThreadTaskExecutor.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/virtualthread/service/VirtualAsyncService.java`

下面的 Service 演示 `@Async` 方法运行在线程名前缀为 `async-vt-` 的虚拟线程中。

```java
package io.github.atengk.virtualthread.service;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 虚拟线程异步任务服务。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class VirtualAsyncService {

    /**
     * 执行异步任务。
     *
     * @param taskName 任务名称
     * @return 异步执行结果
     */
    @Async("applicationTaskExecutor")
    public CompletableFuture<Dict> executeAsyncTask(String taskName) {
        Thread currentThread = Thread.currentThread();

        log.info("开始执行异步任务：{}，线程名称：{}，是否虚拟线程：{}",
                taskName,
                currentThread.getName(),
                currentThread.isVirtual());

        ThreadUtil.sleep(1000);

        Dict result = Dict.create()
                .set("taskName", taskName)
                .set("threadName", currentThread.getName())
                .set("virtual", currentThread.isVirtual());

        return CompletableFuture.completedFuture(result);
    }

}
```

文件位置：`src/main/java/io/github/atengk/virtualthread/controller/VirtualAsyncController.java`

下面的 Controller 用于触发异步任务并返回执行结果。

```java
package io.github.atengk.virtualthread.controller;

import cn.hutool.core.lang.Dict;
import io.github.atengk.virtualthread.service.VirtualAsyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * 虚拟线程异步任务接口。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/virtual-thread/async")
public class VirtualAsyncController {

    private final VirtualAsyncService virtualAsyncService;

    /**
     * 执行异步任务。
     *
     * @param taskName 任务名称
     * @return 异步任务结果
     */
    @GetMapping("/execute")
    public CompletableFuture<Dict> execute(@RequestParam(defaultValue = "demo-task") String taskName) {
        return virtualAsyncService.executeAsyncTask(taskName);
    }

}
```

执行验证：

```bash
curl "http://localhost:8080/virtual-thread/async/execute?taskName=order-sync"
```

返回示例：

```json
{
  "taskName": "order-sync",
  "threadName": "async-vt-0",
  "virtual": true
}
```

如果项目中已经存在自定义 `Executor` Bean，需要重点检查 Bean 名称和类型。Spring Boot 官方文档说明，如果定义了自定义 `Executor`，常规 `@EnableAsync` 会使用该执行器；Spring MVC 和 WebFlux 相关支持则要求它是名为 `applicationTaskExecutor` 的 `AsyncTaskExecutor` 实现。([Home](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html?utm_source=chatgpt.com))

### 定时任务线程配置

Spring Boot 启用虚拟线程后，任务调度器会使用支持虚拟线程的 `SimpleAsyncTaskScheduler`；与传统 `ThreadPoolTaskScheduler` 不同，虚拟线程启用后，调度线程池相关的 pool 配置会被忽略。Spring Boot 官方文档说明，启用虚拟线程时，`SimpleAsyncTaskScheduler` 会使用虚拟线程，并忽略池化相关属性。([Home](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/virtualthread/config/SchedulingConfig.java`

下面的配置启用定时任务。

```java
package io.github.atengk.virtualthread.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@EnableScheduling
@Configuration
public class SchedulingConfig {
}
```

文件位置：`src/main/java/io/github/atengk/virtualthread/task/VirtualScheduledTask.java`

下面的定时任务用于周期性打印当前线程信息。

```java
package io.github.atengk.virtualthread.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 虚拟线程定时任务示例。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class VirtualScheduledTask {

    /**
     * 每 5 秒执行一次定时任务。
     */
    @Scheduled(fixedRate = 5000)
    public void printThreadInfo() {
        Thread currentThread = Thread.currentThread();

        log.info("定时任务执行时间：{}，线程名称：{}，是否虚拟线程：{}",
                DateUtil.now(),
                currentThread.getName(),
                currentThread.isVirtual());

        ThreadUtil.sleep(500);
    }

}
```

启动项目后观察控制台日志：

```text
定时任务执行时间：2026-05-06 10:00:00，线程名称：scheduling-1，是否虚拟线程：true
```

定时任务使用虚拟线程时，需要注意以下几点：

| 注意事项                                     | 说明                                    |
| -------------------------------------------- | --------------------------------------- |
| 建议开启 `spring.main.keep-alive=true`       | 避免应用只剩守护线程时 JVM 退出         |
| 不要依赖线程池大小限制定时任务并发           | 虚拟线程启用后，池化配置可能不再生效    |
| 长耗时任务要控制外部资源                     | 例如数据库连接池、HTTP 连接池、分布式锁 |
| 避免在定时任务中长时间持有 `synchronized` 锁 | 降低虚拟线程 pinning 风险               |
| 任务失败要捕获日志                           | 避免异常导致后续调度行为不符合预期      |

如果定时任务是项目的核心后台能力，例如订单补偿、数据同步、消息重试、报表生成等，建议上线前重点验证 `spring.main.keep-alive=true`、任务异常处理、任务幂等性、分布式部署互斥策略和外部资源连接池容量。



## 核心开发实践

本节用于说明虚拟线程在 Spring Boot 3 项目中的常见开发方式。虚拟线程适合保留同步阻塞代码风格，重点用于 Web 请求、Service 阻塞调用、HTTP 调用、数据库访问、文件 IO 等场景。Spring Boot 在 Java 21+ 且启用 `spring.threads.virtual.enabled=true` 后，会自动配置使用虚拟线程的任务执行能力。([Home](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html?utm_source=chatgpt.com))

### Controller 接口开发

Controller 层不需要为了虚拟线程额外修改接口写法。启用虚拟线程后，Spring MVC 请求处理可以继续使用同步返回方式，业务代码仍然按照 Controller 调用 Service 的方式开发。虚拟线程的价值体现在请求阻塞等待时减少平台线程占用，而不是改变接口编写模型。

文件位置：`src/main/java/io/github/atengk/virtualthread/controller/VirtualThreadPracticeController.java`

下面的接口用于验证当前请求线程、模拟阻塞接口和触发并发业务处理。

```java
package io.github.atengk.virtualthread.controller;

import cn.hutool.core.lang.Dict;
import io.github.atengk.virtualthread.service.VirtualThreadPracticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 虚拟线程核心开发实践接口。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/practice/virtual-thread")
public class VirtualThreadPracticeController {

    private final VirtualThreadPracticeService virtualThreadPracticeService;

    /**
     * 查看当前请求线程信息。
     *
     * @return 当前线程信息
     */
    @GetMapping("/thread")
    public Dict currentThread() {
        Thread currentThread = Thread.currentThread();

        log.info("查看当前请求线程，线程名称：{}，是否虚拟线程：{}",
                currentThread.getName(),
                currentThread.isVirtual());

        return Dict.create()
                .set("threadName", currentThread.getName())
                .set("virtual", currentThread.isVirtual())
                .set("threadInfo", currentThread.toString());
    }

    /**
     * 模拟阻塞接口。
     *
     * @param seconds 阻塞秒数
     * @return 执行结果
     */
    @GetMapping("/blocking/{seconds}")
    public Dict blocking(@PathVariable Integer seconds) {
        return virtualThreadPracticeService.mockBlocking(seconds);
    }

    /**
     * 模拟并发查询多个外部资源。
     *
     * @return 执行结果
     */
    @GetMapping("/parallel")
    public Dict parallelQuery() {
        List<String> resultList = virtualThreadPracticeService.parallelQuery();

        return Dict.create()
                .set("size", resultList.size())
                .set("data", resultList);
    }

}
```

接口验证命令如下：

```bash
# 查看当前请求是否运行在虚拟线程中
curl http://localhost:8080/practice/virtual-thread/thread

# 模拟阻塞 3 秒的接口
curl http://localhost:8080/practice/virtual-thread/blocking/3

# 模拟一个请求内并发查询多个外部资源
curl http://localhost:8080/practice/virtual-thread/parallel
```

返回结果中如果 `virtual` 为 `true`，表示当前请求处理逻辑运行在虚拟线程中。

### Service 阻塞调用处理

Service 层可以继续使用同步阻塞代码。虚拟线程适合处理数据库查询、HTTP 调用、Redis 操作、文件 IO、第三方 SDK 调用等等待型逻辑。需要注意，虚拟线程不会突破外部资源限制，例如数据库连接池数量、HTTP 连接池数量、Redis 最大连接数和下游接口限流。

文件位置：`src/main/java/io/github/atengk/virtualthread/service/VirtualThreadPracticeService.java`

下面的 Service 演示阻塞调用、并发调用和异常处理方式。

```java
package io.github.atengk.virtualthread.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

/**
 * 虚拟线程核心开发实践服务。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class VirtualThreadPracticeService {

    /**
     * 模拟阻塞业务处理。
     *
     * @param seconds 阻塞秒数
     * @return 执行结果
     */
    public Dict mockBlocking(Integer seconds) {
        int safeSeconds = seconds == null || seconds <= 0 ? 1 : Math.min(seconds, 10);
        Thread currentThread = Thread.currentThread();

        log.info("开始执行阻塞业务，阻塞秒数：{}，线程名称：{}，是否虚拟线程：{}",
                safeSeconds,
                currentThread.getName(),
                currentThread.isVirtual());

        ThreadUtil.sleep(safeSeconds * 1000L);

        return Dict.create()
                .set("seconds", safeSeconds)
                .set("threadName", currentThread.getName())
                .set("virtual", currentThread.isVirtual())
                .set("message", "阻塞业务执行完成");
    }

    /**
     * 在一个业务请求中并发查询多个外部资源。
     *
     * @return 查询结果
     */
    public List<String> parallelQuery() {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futureList = IntStream.rangeClosed(1, 5)
                    .mapToObj(index -> executorService.submit(() -> queryExternalResource(index)))
                    .toList();

            List<String> resultList = futureList.stream()
                    .map(this::getFutureResult)
                    .filter(CollUtil::isNotEmpty)
                    .toList();

            log.info("并发查询完成，结果数量：{}", resultList.size());
            return resultList;
        }
    }

    /**
     * 模拟查询外部资源。
     *
     * @param index 资源编号
     * @return 查询结果
     */
    private String queryExternalResource(Integer index) {
        Thread currentThread = Thread.currentThread();

        log.info("查询外部资源，资源编号：{}，线程名称：{}，是否虚拟线程：{}",
                index,
                currentThread.getName(),
                currentThread.isVirtual());

        ThreadUtil.sleep(500);

        return "resource-" + index;
    }

    /**
     * 获取 Future 执行结果。
     *
     * @param future 异步结果
     * @return 任务结果
     */
    private String getFutureResult(Future<String> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            log.error("获取并发任务结果失败", exception);
            return "";
        }
    }

}
```

这里使用 `Executors.newVirtualThreadPerTaskExecutor()` 处理一个请求内的多个并发阻塞任务。该执行器的语义是每个任务使用一个新的虚拟线程，适合并发等待多个独立 IO 结果。JDK 官方文档也提供了该 API，用于创建“每个任务一个虚拟线程”的执行器。([Home](https://docs.spring.io/spring-boot/3.3/reference/features/task-execution-and-scheduling.html?utm_source=chatgpt.com))

使用虚拟线程处理 Service 阻塞调用时，建议遵守以下原则：

| 原则                 | 说明                                                    |
| -------------------- | ------------------------------------------------------- |
| 保持同步代码清晰     | 不需要为了虚拟线程强制改成响应式或回调风格              |
| 并发调用要有边界     | 虚拟线程多不等于外部资源无限                            |
| 外部连接池要合理配置 | 数据库、HTTP、Redis 等连接池仍然是吞吐上限              |
| 异常必须收敛         | 并发任务中不要吞掉异常，应记录日志或返回降级结果        |
| 不要长期持有锁       | 避免 `synchronized` 长时间阻塞导致虚拟线程 pinning 风险 |

### HTTP 客户端调用

HTTP 调用是虚拟线程非常典型的适用场景。Spring Framework 的 `RestClient` 是同步 HTTP 客户端，提供流式 API，底层可以基于 JDK HttpClient、Apache HttpComponents 等实现。它适合和虚拟线程组合使用：业务代码保持同步写法，线程阻塞等待远程响应时由虚拟线程降低平台线程占用。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/RestClient.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/virtualthread/config/RestClientConfig.java`

下面的配置声明统一的 `RestClient` Bean，用于后续 Service 注入调用。

```java
package io.github.atengk.virtualthread.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * HTTP 客户端配置。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class RestClientConfig {

    /**
     * 配置 RestClient。
     *
     * @return RestClient 实例
     */
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .defaultHeader("User-Agent", "spring-boot-virtual-thread-demo")
                .build();
    }

}
```

文件位置：`src/main/java/io/github/atengk/virtualthread/service/RemoteHttpService.java`

下面的 Service 演示同步 HTTP 调用、异常日志和降级返回。

```java
package io.github.atengk.virtualthread.service;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 远程 HTTP 调用服务。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteHttpService {

    private final RestClient restClient;

    /**
     * 查询远程接口数据。
     *
     * @param url 远程接口地址
     * @return 查询结果
     */
    public Dict queryRemoteData(String url) {
        Thread currentThread = Thread.currentThread();

        if (StrUtil.isBlank(url)) {
            log.warn("远程接口地址为空，跳过 HTTP 调用");
            return Dict.create()
                    .set("success", false)
                    .set("message", "远程接口地址不能为空");
        }

        try {
            log.info("开始调用远程接口，url：{}，线程名称：{}，是否虚拟线程：{}",
                    url,
                    currentThread.getName(),
                    currentThread.isVirtual());

            String responseBody = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            return Dict.create()
                    .set("success", true)
                    .set("url", url)
                    .set("threadName", currentThread.getName())
                    .set("virtual", currentThread.isVirtual())
                    .set("body", responseBody);
        } catch (Exception exception) {
            log.error("远程接口调用失败，url：{}", url, exception);

            return Dict.create()
                    .set("success", false)
                    .set("url", url)
                    .set("message", "远程接口调用失败");
        }
    }

}
```

文件位置：`src/main/java/io/github/atengk/virtualthread/controller/RemoteHttpController.java`

下面的 Controller 用于测试虚拟线程中的同步 HTTP 调用。

```java
package io.github.atengk.virtualthread.controller;

import cn.hutool.core.lang.Dict;
import io.github.atengk.virtualthread.service.RemoteHttpService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 远程 HTTP 调用接口。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/practice/http")
public class RemoteHttpController {

    private final RemoteHttpService remoteHttpService;

    /**
     * 调用远程接口。
     *
     * @param url 远程接口地址
     * @return 调用结果
     */
    @GetMapping("/query")
    public Dict query(@RequestParam String url) {
        return remoteHttpService.queryRemoteData(url);
    }

}
```

验证命令如下：

```bash
curl "http://localhost:8080/practice/http/query?url=https://httpbin.org/get"
```

生产环境中不建议对任意 URL 开放代理式 HTTP 调用接口。上面的接口只用于验证虚拟线程下的同步 HTTP 调用方式。实际项目中应将远程地址封装为配置项，并结合超时时间、重试策略、熔断降级、连接池和白名单控制。

### 数据库访问场景

数据库访问也是虚拟线程的典型使用场景。Controller 和 Service 可以继续使用同步方式调用 MyBatis、MyBatis-Plus、JDBC 或 JPA。虚拟线程能降低请求线程等待数据库返回时的平台线程占用，但不能增加数据库连接池容量，也不能让数据库本身查询更快。

如果项目使用 MyBatis-Plus，可以补充以下依赖。

文件位置：`pom.xml`

```xml
<!-- MyBatis-Plus Spring Boot 3 启动器，用于简化数据库 CRUD 开发 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.12</version>
</dependency>

<!-- MySQL 驱动，根据实际数据库类型替换 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

示例表结构如下：

```sql
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) NOT NULL COMMENT '昵称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    create_time DATETIME NOT NULL COMMENT '创建时间'
) COMMENT '系统用户表';
```

文件位置：`src/main/java/io/github/atengk/virtualthread/entity/SysUser.java`

下面的实体类对应 `sys_user` 表。

```java
package io.github.atengk.virtualthread.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@TableName("sys_user")
public class SysUser {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 昵称。
     */
    private String nickname;

    /**
     * 状态：0禁用，1启用。
     */
    private Integer status;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

}
```

文件位置：`src/main/java/io/github/atengk/virtualthread/mapper/SysUserMapper.java`

下面的 Mapper 用于数据库访问。

```java
package io.github.atengk.virtualthread.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.virtualthread.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 Mapper。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
```

文件位置：`src/main/java/io/github/atengk/virtualthread/service/SysUserService.java`

下面的 Service 演示同步数据库查询。虚拟线程会在数据库 IO 等待期间减少平台线程占用，但实际并发能力仍受 HikariCP 连接池和数据库承载能力限制。

```java
package io.github.atengk.virtualthread.service;

import cn.hutool.core.lang.Dict;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.virtualthread.entity.SysUser;
import io.github.atengk.virtualthread.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统用户服务。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper sysUserMapper;

    /**
     * 根据状态查询用户列表。
     *
     * @param status 用户状态
     * @return 用户列表
     */
    public Dict listByStatus(Integer status) {
        Thread currentThread = Thread.currentThread();

        log.info("开始查询用户列表，状态：{}，线程名称：{}，是否虚拟线程：{}",
                status,
                currentThread.getName(),
                currentThread.isVirtual());

        List<SysUser> userList = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(status != null, SysUser::getStatus, status)
                        .orderByDesc(SysUser::getCreateTime)
        );

        return Dict.create()
                .set("threadName", currentThread.getName())
                .set("virtual", currentThread.isVirtual())
                .set("size", userList.size())
                .set("data", userList);
    }

}
```

文件位置：`src/main/java/io/github/atengk/virtualthread/controller/SysUserController.java`

下面的 Controller 用于验证虚拟线程下的同步数据库访问。

```java
package io.github.atengk.virtualthread.controller;

import cn.hutool.core.lang.Dict;
import io.github.atengk.virtualthread.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统用户接口。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/practice/users")
public class SysUserController {

    private final SysUserService sysUserService;

    /**
     * 根据状态查询用户列表。
     *
     * @param status 用户状态
     * @return 用户列表
     */
    @GetMapping
    public Dict list(@RequestParam(required = false) Integer status) {
        return sysUserService.listByStatus(status);
    }

}
```

验证命令如下：

```bash
curl "http://localhost:8080/practice/users?status=1"
```

数据库场景中需要重点关注连接池配置。虚拟线程可以承载大量请求，但如果 `maximum-pool-size` 只有 10，那么同时执行 SQL 的连接最多仍然只有 10 个。虚拟线程不能绕过连接池限制，也不能替代 SQL 优化、索引优化和数据库扩容。

### 文件与 IO 操作场景

文件读取、文件写入、对象存储上传下载、本地磁盘扫描等 IO 操作也适合使用虚拟线程。代码可以继续使用同步方式编写，避免为了 IO 等待引入复杂的异步回调。

文件位置：`src/main/java/io/github/atengk/virtualthread/service/FileIoService.java`

下面的 Service 演示使用 Hutool 处理文件读写，并打印当前线程信息。

```java
package io.github.atengk.virtualthread.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 文件 IO 服务。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class FileIoService {

    /**
     * 写入文本文件。
     *
     * @param filePath 文件路径
     * @param content 文件内容
     * @return 写入结果
     */
    public Dict writeText(String filePath, String content) {
        Thread currentThread = Thread.currentThread();

        if (StrUtil.isBlank(filePath)) {
            log.warn("文件路径为空，跳过写入");
            return Dict.create()
                    .set("success", false)
                    .set("message", "文件路径不能为空");
        }

        File file = FileUtil.file(filePath);
        FileUtil.mkParentDirs(file);
        FileUtil.writeString(StrUtil.blankToDefault(content, ""), file, CharsetUtil.CHARSET_UTF_8);

        log.info("文件写入完成，路径：{}，线程名称：{}，是否虚拟线程：{}",
                filePath,
                currentThread.getName(),
                currentThread.isVirtual());

        return Dict.create()
                .set("success", true)
                .set("filePath", filePath)
                .set("threadName", currentThread.getName())
                .set("virtual", currentThread.isVirtual());
    }

    /**
     * 读取文本文件。
     *
     * @param filePath 文件路径
     * @return 读取结果
     */
    public Dict readText(String filePath) {
        Thread currentThread = Thread.currentThread();

        if (StrUtil.isBlank(filePath)) {
            log.warn("文件路径为空，跳过读取");
            return Dict.create()
                    .set("success", false)
                    .set("message", "文件路径不能为空");
        }

        File file = FileUtil.file(filePath);
        if (!FileUtil.exist(file)) {
            log.warn("文件不存在，路径：{}", filePath);
            return Dict.create()
                    .set("success", false)
                    .set("message", "文件不存在");
        }

        String content = FileUtil.readString(file, CharsetUtil.CHARSET_UTF_8);

        log.info("文件读取完成，路径：{}，线程名称：{}，是否虚拟线程：{}",
                filePath,
                currentThread.getName(),
                currentThread.isVirtual());

        return Dict.create()
                .set("success", true)
                .set("filePath", filePath)
                .set("threadName", currentThread.getName())
                .set("virtual", currentThread.isVirtual())
                .set("content", content);
    }

}
```

文件位置：`src/main/java/io/github/atengk/virtualthread/controller/FileIoController.java`

下面的 Controller 用于验证虚拟线程下的同步文件读写。

```java
package io.github.atengk.virtualthread.controller;

import cn.hutool.core.lang.Dict;
import io.github.atengk.virtualthread.service.FileIoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件 IO 接口。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/practice/file")
public class FileIoController {

    private final FileIoService fileIoService;

    /**
     * 写入文本文件。
     *
     * @param filePath 文件路径
     * @param content 文件内容
     * @return 写入结果
     */
    @GetMapping("/write")
    public Dict write(@RequestParam String filePath,
                      @RequestParam String content) {
        return fileIoService.writeText(filePath, content);
    }

    /**
     * 读取文本文件。
     *
     * @param filePath 文件路径
     * @return 读取结果
     */
    @GetMapping("/read")
    public Dict read(@RequestParam String filePath) {
        return fileIoService.readText(filePath);
    }

}
```

验证命令如下：

```bash
# 写入文件
curl "http://localhost:8080/practice/file/write?filePath=/tmp/virtual-thread/demo.txt&content=hello"

# 读取文件
curl "http://localhost:8080/practice/file/read?filePath=/tmp/virtual-thread/demo.txt"
```

文件 IO 场景中需要关注磁盘吞吐、文件大小、目录权限和安全边界。生产环境不建议直接暴露任意路径读写接口，建议限定业务目录，并对文件名、路径穿越、文件大小和访问权限进行校验。

## 配置示例

本节给出虚拟线程项目的常用配置，包括 `application.yml`、自定义虚拟线程执行器和异步任务配置。启用虚拟线程后，部分传统线程池配置不再按原方式影响执行行为，因为虚拟线程调度基于 JVM 范围内的平台线程池，而不是应用内专用线程池。Spring Boot 官方文档对此有明确说明。([Home](https://docs.spring.io/spring-boot/4.1-SNAPSHOT/reference/features/spring-application.html?utm_source=chatgpt.com))

### application.yml 配置

`application.yml` 中主要配置虚拟线程开关、应用保活、数据库连接池、日志级别和业务参数。虚拟线程是守护线程，如果项目依赖 `@Scheduled` 或后台任务保持运行，建议配置 `spring.main.keep-alive=true`。([Home](https://docs.spring.io/spring-boot/4.1-SNAPSHOT/reference/features/spring-application.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    # 应用名称，用于日志、监控和链路标识
    name: spring-boot-virtual-thread-demo

  threads:
    virtual:
      # 启用虚拟线程，要求 Java 21 或更高版本
      enabled: true

  main:
    # 虚拟线程是守护线程；存在定时任务或后台任务时建议开启
    keep-alive: true

  datasource:
    # MySQL 连接地址，请根据实际环境修改
    url: jdbc:mysql://127.0.0.1:3306/demo?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    # 数据库用户名
    username: root
    # 数据库密码
    password: root
    # MySQL 驱动类
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      # 最大连接池数量；虚拟线程不能突破该限制
      maximum-pool-size: 20
      # 最小空闲连接数量
      minimum-idle: 5
      # 获取连接最大等待时间，单位毫秒
      connection-timeout: 30000
      # 空闲连接最大存活时间，单位毫秒
      idle-timeout: 600000
      # 连接最大生命周期，单位毫秒
      max-lifetime: 1800000

mybatis-plus:
  configuration:
    # 开发环境可开启 SQL 日志，生产环境建议关闭或调整为日志框架输出
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 主键策略，根据实际项目规范调整
      id-type: assign_id

server:
  # 服务端口
  port: 8080
  tomcat:
    # 虚拟线程启用后，不建议继续依赖 Tomcat 工作线程数作为核心并发控制手段
    threads:
      max: 200

logging:
  level:
    # 项目根包日志级别
    io.github.atengk: info
    # Spring Web 日志级别
    org.springframework.web: info

virtual-thread-demo:
  http:
    # 远程 HTTP 默认连接超时时间，单位毫秒
    connect-timeout: 3000
    # 远程 HTTP 默认读取超时时间，单位毫秒
    read-timeout: 5000
  file:
    # 文件 IO 示例允许访问的根目录，生产环境应限制访问范围
    base-path: /tmp/virtual-thread
```

需要注意，`server.tomcat.threads.max` 在传统平台线程模型下常用于控制 Tomcat 工作线程数。启用虚拟线程后，请求处理线程模型发生变化，不应继续把该参数作为虚拟线程并发能力的核心控制项。并发控制应更多放在数据库连接池、HTTP 连接池、业务限流和下游承载能力上。

### 自定义线程池配置

虚拟线程场景下，不建议再创建固定大小的虚拟线程池。虚拟线程推荐“每个任务一个虚拟线程”，如果需要限制并发，应通过连接池、信号量、限流器或业务队列控制。Spring Framework 提供了 `VirtualThreadTaskExecutor`，它是基于 JDK 21 虚拟线程的 `TaskExecutor` 实现，主要配置项是线程名前缀；如果需要并发限制或任务装饰能力，可以考虑 `SimpleAsyncTaskExecutor#setVirtualThreads(boolean)`。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/task/VirtualThreadTaskExecutor.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/virtualthread/config/VirtualThreadExecutorConfig.java`

下面的配置声明常用的虚拟线程执行器 Bean。

```java
package io.github.atengk.virtualthread.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 虚拟线程执行器配置。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class VirtualThreadExecutorConfig {

    /**
     * 配置 Spring 应用异步任务执行器。
     *
     * @return 虚拟线程任务执行器
     */
    @Bean("applicationTaskExecutor")
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new VirtualThreadTaskExecutor("app-vt-");
    }

    /**
     * 配置 JDK 虚拟线程执行器。
     *
     * @return 每个任务一个虚拟线程的执行器
     */
    @Bean(destroyMethod = "close")
    public ExecutorService virtualThreadExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 配置带并发限制的虚拟线程任务执行器。
     *
     * @return 带并发限制的异步任务执行器
     */
    @Bean("limitedVirtualTaskExecutor")
    public AsyncTaskExecutor limitedVirtualTaskExecutor() {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("limited-vt-");
        taskExecutor.setVirtualThreads(true);
        taskExecutor.setConcurrencyLimit(100);
        taskExecutor.setTaskTerminationTimeout(30000);
        return taskExecutor;
    }

}
```

该配置包含三种常见用途：

| Bean                           | 用途                                        |
| ------------------------------ | ------------------------------------------- |
| `applicationTaskExecutor`      | 供 Spring MVC 异步处理、`@Async` 等场景使用 |
| `virtualThreadExecutorService` | 供业务代码手动提交并发任务使用              |
| `limitedVirtualTaskExecutor`   | 在需要限制并发数时使用                      |

Spring Boot 自动配置的 `AsyncTaskExecutor` 会在未声明自定义 `Executor` Bean 时生效；如果项目已经声明了自定义 `Executor`，则 `@EnableAsync`、Spring MVC 异步处理等场景的执行器选择规则需要额外关注。Spring Boot 官方文档说明，Spring MVC 和 WebFlux 相关支持要求自定义执行器是名为 `applicationTaskExecutor` 的 `AsyncTaskExecutor` 实现。([Home](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html?utm_source=chatgpt.com))

### 异步任务配置

异步任务可以使用 `@EnableAsync` 和 `@Async`。启用虚拟线程后，异步任务适合处理发送通知、调用外部接口、文件处理、日志落库、数据同步等 IO 密集型任务。对于 CPU 密集型任务，仍建议使用固定大小的平台线程池，并根据 CPU 核数控制并发。

文件位置：`src/main/java/io/github/atengk/virtualthread/config/AsyncConfig.java`

下面的配置启用异步任务，并使用前面声明的 `applicationTaskExecutor`。

```java
package io.github.atengk.virtualthread.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步任务启用配置。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@EnableAsync
@Configuration
public class AsyncConfig {
}
```

文件位置：`src/main/java/io/github/atengk/virtualthread/service/AsyncNoticeService.java`

下面的 Service 演示使用虚拟线程执行异步通知任务。

```java
package io.github.atengk.virtualthread.service;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 异步通知服务。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class AsyncNoticeService {

    /**
     * 异步发送通知。
     *
     * @param receiver 接收人
     * @param content 通知内容
     * @return 发送结果
     */
    @Async("applicationTaskExecutor")
    public CompletableFuture<Dict> sendNotice(String receiver, String content) {
        Thread currentThread = Thread.currentThread();

        if (StrUtil.hasBlank(receiver, content)) {
            log.warn("异步通知参数不完整，receiver：{}", receiver);
            return CompletableFuture.completedFuture(
                    Dict.create()
                            .set("success", false)
                            .set("message", "接收人和通知内容不能为空")
            );
        }

        log.info("开始发送异步通知，receiver：{}，线程名称：{}，是否虚拟线程：{}",
                receiver,
                currentThread.getName(),
                currentThread.isVirtual());

        ThreadUtil.sleep(800);

        return CompletableFuture.completedFuture(
                Dict.create()
                        .set("success", true)
                        .set("receiver", receiver)
                        .set("threadName", currentThread.getName())
                        .set("virtual", currentThread.isVirtual())
        );
    }

}
```

文件位置：`src/main/java/io/github/atengk/virtualthread/controller/AsyncNoticeController.java`

下面的 Controller 用于触发异步通知任务。

```java
package io.github.atengk.virtualthread.controller;

import cn.hutool.core.lang.Dict;
import io.github.atengk.virtualthread.service.AsyncNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * 异步通知接口。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/practice/async-notice")
public class AsyncNoticeController {

    private final AsyncNoticeService asyncNoticeService;

    /**
     * 发送异步通知。
     *
     * @param receiver 接收人
     * @param content 通知内容
     * @return 发送结果
     */
    @GetMapping("/send")
    public CompletableFuture<Dict> send(@RequestParam String receiver,
                                        @RequestParam String content) {
        return asyncNoticeService.sendNotice(receiver, content);
    }

}
```

验证命令如下：

```bash
curl "http://localhost:8080/practice/async-notice/send?receiver=ateng&content=hello"
```

返回示例：

```json
{
  "success": true,
  "receiver": "ateng",
  "threadName": "app-vt-0",
  "virtual": true
}
```

异步任务配置完成后，需要重点验证三点：第一，日志中的线程名前缀是否符合预期；第二，`Thread.currentThread().isVirtual()` 是否为 `true`；第三，高并发下数据库连接池、HTTP 连接池和下游服务是否出现等待、拒绝或超时。虚拟线程可以提升等待型任务的承载能力，但资源容量仍必须通过连接池、限流和监控指标进行约束。



## 测试与验证

本节用于说明虚拟线程启用后的验证方式，包括接口并发测试、线程名称验证、性能对比验证和阻塞场景验证。虚拟线程测试不应只看接口是否能访问，还需要确认当前请求是否运行在虚拟线程中、阻塞场景下吞吐是否改善、外部资源是否成为瓶颈，以及是否存在 `synchronized` pinning 等问题。

### 接口并发测试

接口并发测试用于验证虚拟线程在高并发阻塞请求下的表现。虚拟线程更适合 IO 密集型和阻塞等待型接口，因此压测时应优先选择包含 `sleep`、HTTP 调用、数据库查询、文件 IO 等等待行为的接口，而不是纯 CPU 计算接口。

文件位置：`src/main/java/io/github/atengk/virtualthread/controller/VirtualThreadTestController.java`

下面的 Controller 提供线程信息、阻塞等待、CPU 计算和锁阻塞四类测试接口。

```java
package io.github.atengk.virtualthread.controller;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 虚拟线程测试验证接口。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/test/virtual-thread")
public class VirtualThreadTestController {

    private static final Object LOCK = new Object();

    /**
     * 查看当前请求线程信息。
     *
     * @return 当前线程信息
     */
    @GetMapping("/thread")
    public Dict thread() {
        Thread currentThread = Thread.currentThread();

        log.info("查看当前线程，线程名称：{}，是否虚拟线程：{}",
                currentThread.getName(),
                currentThread.isVirtual());

        return Dict.create()
                .set("threadName", currentThread.getName())
                .set("virtual", currentThread.isVirtual())
                .set("threadInfo", currentThread.toString());
    }

    /**
     * 模拟阻塞等待接口。
     *
     * @param millis 阻塞毫秒数
     * @return 执行结果
     */
    @GetMapping("/sleep")
    public Dict sleep(@RequestParam(defaultValue = "1000") Long millis) {
        long safeMillis = Math.max(100L, Math.min(millis, 10000L));
        Thread currentThread = Thread.currentThread();

        log.info("开始阻塞等待，millis：{}，线程名称：{}，是否虚拟线程：{}",
                safeMillis,
                currentThread.getName(),
                currentThread.isVirtual());

        ThreadUtil.sleep(safeMillis);

        return Dict.create()
                .set("millis", safeMillis)
                .set("threadName", currentThread.getName())
                .set("virtual", currentThread.isVirtual())
                .set("message", "阻塞等待完成");
    }

    /**
     * 模拟 CPU 密集型计算接口。
     *
     * @param count 循环次数
     * @return 执行结果
     */
    @GetMapping("/cpu")
    public Dict cpu(@RequestParam(defaultValue = "2000000") Long count) {
        long safeCount = Math.max(10000L, Math.min(count, 100000000L));
        Thread currentThread = Thread.currentThread();

        log.info("开始 CPU 计算，count：{}，线程名称：{}，是否虚拟线程：{}",
                safeCount,
                currentThread.getName(),
                currentThread.isVirtual());

        long sum = 0;
        for (long index = 0; index < safeCount; index++) {
            sum += index;
        }

        return Dict.create()
                .set("count", safeCount)
                .set("sum", sum)
                .set("threadName", currentThread.getName())
                .set("virtual", currentThread.isVirtual());
    }

    /**
     * 模拟 synchronized 锁内阻塞接口。
     *
     * @param millis 阻塞毫秒数
     * @return 执行结果
     */
    @GetMapping("/lock")
    public Dict lock(@RequestParam(defaultValue = "1000") Long millis) {
        long safeMillis = Math.max(100L, Math.min(millis, 10000L));
        Thread currentThread = Thread.currentThread();

        synchronized (LOCK) {
            log.info("进入 synchronized 锁，millis：{}，线程名称：{}，是否虚拟线程：{}",
                    safeMillis,
                    currentThread.getName(),
                    currentThread.isVirtual());

            ThreadUtil.sleep(safeMillis);
        }

        return Dict.create()
                .set("millis", safeMillis)
                .set("threadName", currentThread.getName())
                .set("virtual", currentThread.isVirtual())
                .set("message", "锁阻塞接口执行完成");
    }

}
```

启动项目后，先单次验证接口是否正常：

```bash
# 查看当前请求线程
curl http://localhost:8080/test/virtual-thread/thread

# 模拟阻塞 1 秒
curl "http://localhost:8080/test/virtual-thread/sleep?millis=1000"

# 模拟 CPU 计算
curl "http://localhost:8080/test/virtual-thread/cpu?count=2000000"

# 模拟 synchronized 锁内阻塞
curl "http://localhost:8080/test/virtual-thread/lock?millis=1000"
```

使用 `wrk` 进行并发压测：

```bash
# 200 并发，持续 30 秒，测试阻塞等待接口
wrk -t8 -c200 -d30s "http://localhost:8080/test/virtual-thread/sleep?millis=1000"

# 200 并发，持续 30 秒，测试 CPU 计算接口
wrk -t8 -c200 -d30s "http://localhost:8080/test/virtual-thread/cpu?count=2000000"

# 200 并发，持续 30 秒，测试 synchronized 锁阻塞接口
wrk -t8 -c200 -d30s "http://localhost:8080/test/virtual-thread/lock?millis=1000"
```

以上命令中，`-t8` 表示使用 8 个压测线程，`-c200` 表示保持 200 个并发连接，`-d30s` 表示持续压测 30 秒。阻塞等待接口更能体现虚拟线程优势；CPU 计算接口通常不会因为启用虚拟线程而明显提升吞吐；锁阻塞接口用于验证不合理的锁使用对虚拟线程扩展性的影响。

### 线程名称验证

线程名称验证用于确认请求、异步任务、定时任务是否真正运行在虚拟线程中。最直接的方式是在业务代码中打印 `Thread.currentThread().getName()` 和 `Thread.currentThread().isVirtual()`。

接口验证：

```bash
curl http://localhost:8080/test/virtual-thread/thread
```

返回示例：

```json
{
  "threadName": "tomcat-handler-1",
  "virtual": true,
  "threadInfo": "VirtualThread[#45,tomcat-handler-1]/runnable@ForkJoinPool-1-worker-3"
}
```

如果 `virtual` 为 `true`，表示当前请求运行在虚拟线程中。Spring Boot 在 Java 21+ 且配置 `spring.threads.virtual.enabled=true` 时，会将自动配置的 `AsyncTaskExecutor` 切换为使用虚拟线程的 `SimpleAsyncTaskExecutor`；该执行器也会用于 `@EnableAsync`、Spring MVC 异步请求处理、WebFlux 阻塞执行支持等场景。([Home](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html?utm_source=chatgpt.com))

也可以通过 `jcmd` 导出包含虚拟线程的线程转储：

```bash
# 查看 Java 进程 PID
jcmd -l

# 导出文本格式线程转储
jcmd <PID> Thread.dump_to_file -format=text /tmp/virtual-thread-dump.txt

# 导出 JSON 格式线程转储
jcmd <PID> Thread.dump_to_file -format=json /tmp/virtual-thread-dump.json
```

`<PID>` 替换为实际 Java 进程 ID。Oracle JDK 21 文档说明，`jcmd <PID> Thread.dump_to_file` 可以生成包含平台线程和虚拟线程的线程转储，并支持 plain text 与 JSON 格式。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html?utm_source=chatgpt.com))

### 性能对比验证

性能对比验证用于判断启用虚拟线程后是否真正改善了目标业务接口。测试时应控制变量，保证接口逻辑、JDK 版本、机器资源、JVM 参数、数据库连接池、HTTP 连接池和压测参数保持一致，只切换虚拟线程开关。

建议准备两组配置。

配置一：关闭虚拟线程。

文件位置：`src/main/resources/application-platform.yml`

```yaml
spring:
  threads:
    virtual:
      # 关闭虚拟线程，使用传统平台线程模型
      enabled: false
```

配置二：启用虚拟线程。

文件位置：`src/main/resources/application-virtual.yml`

```yaml
spring:
  threads:
    virtual:
      # 启用虚拟线程，要求 Java 21+
      enabled: true

  main:
    # 存在定时任务或后台任务时建议开启
    keep-alive: true
```

分别启动两组应用：

```bash
# 启动传统平台线程版本
java -jar target/spring-boot-virtual-thread-demo-1.0.0.jar --spring.profiles.active=platform

# 启动虚拟线程版本
java -jar target/spring-boot-virtual-thread-demo-1.0.0.jar --spring.profiles.active=virtual
```

对同一接口执行相同压测：

```bash
# 阻塞接口压测
wrk -t8 -c200 -d30s "http://localhost:8080/test/virtual-thread/sleep?millis=1000"

# CPU 接口压测
wrk -t8 -c200 -d30s "http://localhost:8080/test/virtual-thread/cpu?count=2000000"
```

对比时建议记录以下指标：

| 指标             | 说明                                         |
| ---------------- | -------------------------------------------- |
| Requests/sec     | 每秒请求数，观察吞吐变化                     |
| Latency          | 响应延迟，重点关注 P95、P99                  |
| Non-2xx or 5xx   | 非正常响应数量，判断是否出现错误             |
| CPU 使用率       | 判断是否已经 CPU 打满                        |
| 内存使用         | 判断大量虚拟线程和上下文对象是否造成内存压力 |
| 数据库连接池等待 | 判断连接池是否成为瓶颈                       |
| 下游接口耗时     | 判断远程服务是否成为瓶颈                     |
| GC 次数和耗时    | 判断对象创建是否造成额外 GC 压力             |

预期结果应按接口类型判断。阻塞等待型接口通常更容易受益于虚拟线程；CPU 密集型接口不应期待明显吞吐提升。JEP 444 的目标是让 thread-per-request 风格的服务端应用在高并发阻塞场景下获得更好的扩展性，而不是提升单个 CPU-bound 任务的执行速度。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

### 阻塞场景验证

阻塞场景验证用于确认项目中的常见等待操作是否适合迁移到虚拟线程。重点验证数据库查询、远程 HTTP 调用、Redis 访问、文件 IO、消息投递、对象存储上传下载等场景。

可按以下方式执行阻塞验证：

| 验证项              | 验证方式                          | 关注点                          |
| ------------------- | --------------------------------- | ------------------------------- |
| `Thread.sleep`      | 访问 `/test/virtual-thread/sleep` | 虚拟线程是否能承载大量等待请求  |
| 数据库查询          | 压测用户列表、订单查询等接口      | HikariCP 是否出现连接等待       |
| HTTP 调用           | 压测远程接口聚合场景              | HTTP 连接池、下游限流、超时重试 |
| 文件 IO             | 压测上传、下载、读取接口          | 磁盘 IO、文件大小、目录权限     |
| `synchronized` 阻塞 | 访问 `/test/virtual-thread/lock`  | 是否出现 pinning 和吞吐下降     |

针对 pinning 问题，可以添加 JVM 参数：

```bash
java -Djdk.tracePinnedThreads=full \
     -jar target/spring-boot-virtual-thread-demo-1.0.0.jar
```

`jdk.tracePinnedThreads=full` 会在虚拟线程阻塞时被固定到承载线程的场景输出完整堆栈，便于定位持有 monitor 的代码位置。JEP 444 说明，虚拟线程在 `synchronized` 块或方法中执行阻塞操作、执行 native 方法或 foreign function 时，可能无法从承载线程卸载，从而发生 pinning；频繁且长时间的 pinning 会影响应用扩展性。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

也可以使用 JFR 记录虚拟线程事件：

```bash
# 查看 Java 进程 PID
jcmd -l

# 启动 60 秒 JFR 采样
jcmd <PID> JFR.start name=virtual-thread-check settings=profile duration=60s filename=/tmp/virtual-thread-check.jfr
```

JFR 中可以关注 `jdk.VirtualThreadPinned`、`jdk.VirtualThreadStart`、`jdk.VirtualThreadEnd`、`jdk.VirtualThreadSubmitFailed` 等事件。JEP 444 说明，JFR 支持虚拟线程相关事件，其中 `jdk.VirtualThreadPinned` 默认启用并带有阈值，用于定位虚拟线程被固定的阻塞场景。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

## 开发注意事项

本节用于总结虚拟线程在实际开发中的风险点。虚拟线程可以降低大量阻塞任务对平台线程的占用，但不能替代容量规划、连接池治理、锁治理、SQL 优化和下游服务保护。

### 避免 CPU 密集型滥用

虚拟线程不适合用于提升 CPU 密集型任务性能。CPU 密集型任务的瓶颈是处理器核心数和计算逻辑本身，例如加密解密、图片处理、视频转码、大文件压缩、复杂规则计算、批量报表计算等。这类任务即使创建大量虚拟线程，也只是让更多任务竞争有限 CPU，通常不会提升吞吐。

不推荐的写法：

```java
try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int index = 0; index < 100000; index++) {
        executorService.submit(() -> {
            // 不推荐：大量 CPU 密集型任务直接提交到虚拟线程
            calculateLargeReport();
        });
    }
}
```

更合理的方式是使用固定大小的平台线程池，并根据 CPU 核数控制并发：

文件位置：`src/main/java/io/github/atengk/virtualthread/config/CpuTaskExecutorConfig.java`

下面的配置用于 CPU 密集型任务，不建议替换为虚拟线程执行器。

```java
package io.github.atengk.virtualthread.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CPU 密集型任务执行器配置。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class CpuTaskExecutorConfig {

    /**
     * 配置 CPU 密集型任务线程池。
     *
     * @return 固定大小平台线程池
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService cpuTaskExecutorService() {
        int processors = Runtime.getRuntime().availableProcessors();
        return Executors.newFixedThreadPool(processors);
    }

}
```

判断标准较简单：如果任务大部分时间在等待 IO，优先考虑虚拟线程；如果任务大部分时间在占用 CPU，优先考虑固定大小平台线程池。

### 避免线程池重复封装

虚拟线程不建议再按传统方式封装成固定大小线程池。虚拟线程的推荐使用方式是“每个任务一个虚拟线程”，任务完成后线程结束。如果将虚拟线程再放入固定大小线程池，会削弱虚拟线程的设计优势，还可能造成排队模型混乱。

不推荐的思路：

```java
// 不推荐：试图把虚拟线程当作传统线程池复用
ExecutorService executorService = Executors.newFixedThreadPool(100, Thread.ofVirtual().factory());
```

推荐使用：

```java
try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
    executorService.submit(() -> {
        // 每个任务一个虚拟线程
        queryRemoteData();
    });
}
```

如果业务确实需要限制并发，不建议通过“固定虚拟线程池大小”实现，而应按资源类型控制：

| 资源类型   | 推荐控制方式                                     |
| ---------- | ------------------------------------------------ |
| 数据库     | HikariCP `maximum-pool-size`、SQL 优化、索引优化 |
| HTTP 调用  | HTTP 连接池、超时时间、限流、熔断                |
| Redis      | Redis 连接池、命令耗时监控、慢查询治理           |
| 文件 IO    | 目录隔离、文件大小限制、磁盘吞吐监控             |
| 第三方接口 | 限流器、信号量、重试退避、降级策略               |

Spring Boot 在启用虚拟线程后，自动配置的任务执行器和任务调度器会使用虚拟线程；调度器场景下池化相关属性会被忽略，因此不应继续依赖传统 pool size 思维控制所有并发。([Home](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html?utm_source=chatgpt.com))

### 注意 synchronized 阻塞影响

虚拟线程遇到 `synchronized` 本身并不一定有问题，问题在于虚拟线程进入 `synchronized` 块或方法后又执行阻塞 IO、长时间等待、远程调用、数据库查询等操作。此时虚拟线程可能无法从承载线程卸载，导致承载线程被一并阻塞。

不推荐的写法：

```java
public synchronized void syncOrder() {
    // 不推荐：在 synchronized 方法中执行远程调用、数据库查询或长时间等待
    callRemoteOrderApi();
    updateOrderStatus();
}
```

更推荐缩小锁范围，或者使用 `ReentrantLock` 控制关键区：

文件位置：`src/main/java/io/github/atengk/virtualthread/service/OrderSyncService.java`

下面的示例将锁内逻辑控制在内存状态变更范围内，避免在锁内执行阻塞调用。

```java
package io.github.atengk.virtualthread.service;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 订单同步服务。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class OrderSyncService {

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 同步订单。
     *
     * @param orderId 订单 ID
     * @return 同步结果
     */
    public Dict syncOrder(Long orderId) {
        Thread currentThread = Thread.currentThread();

        log.info("开始同步订单，orderId：{}，线程名称：{}，是否虚拟线程：{}",
                orderId,
                currentThread.getName(),
                currentThread.isVirtual());

        // 阻塞 IO 放在锁外执行
        Dict remoteResult = callRemoteOrderApi(orderId);

        lock.lock();
        try {
            // 锁内只处理必要的内存状态或短事务逻辑
            log.info("更新订单同步状态，orderId：{}", orderId);
            return remoteResult.set("message", "订单同步完成");
        } finally {
            lock.unlock();
        }
    }

    /**
     * 模拟远程订单接口调用。
     *
     * @param orderId 订单 ID
     * @return 远程调用结果
     */
    private Dict callRemoteOrderApi(Long orderId) {
        ThreadUtil.sleep(500);
        return Dict.create()
                .set("orderId", orderId)
                .set("success", true);
    }

}
```

JEP 444 建议，对于频繁执行且保护潜在长 IO 操作的 `synchronized` 块或方法，可以考虑改用 `java.util.concurrent.locks.ReentrantLock`；但不需要机械替换所有 `synchronized`，启动阶段、低频操作或纯内存短操作通常不需要改造。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

### 注意连接池资源限制

虚拟线程能承载更多阻塞等待任务，但不能突破数据库、Redis、HTTP 客户端、消息中间件和下游服务的资源上限。最常见的问题是：接口线程不再成为瓶颈后，数据库连接池、HTTP 连接池或下游服务限流变成新的瓶颈。

数据库连接池示例：

```yaml
spring:
  datasource:
    hikari:
      # 最大数据库连接数；同时执行 SQL 的数量不会超过该值
      maximum-pool-size: 20
      # 获取连接最大等待时间，单位毫秒
      connection-timeout: 30000
```

如果 500 个虚拟线程同时进入数据库查询，但 HikariCP 最大连接数为 20，那么最多只有 20 个请求能同时持有连接，其余请求仍然会等待连接。此时继续增加虚拟线程数量没有意义，应检查 SQL 耗时、索引、事务范围、连接池大小和数据库负载。

HTTP 调用也类似。建议统一配置超时时间、连接池、重试和限流，避免大量虚拟线程长时间等待下游接口：

```yaml
virtual-thread-demo:
  http:
    # 连接超时时间，单位毫秒
    connect-timeout: 3000
    # 读取超时时间，单位毫秒
    read-timeout: 5000
    # 单实例最大并发请求数，根据下游承载能力设置
    max-concurrency: 100
```

上线前应重点观察以下指标：

| 资源        | 关键指标                                    |
| ----------- | ------------------------------------------- |
| HikariCP    | active、idle、pending、timeout              |
| 数据库      | 慢 SQL、锁等待、CPU、IO、连接数             |
| HTTP 客户端 | 连接池等待、超时率、错误率、P95/P99         |
| Redis       | 慢命令、连接数、阻塞命令、网络延迟          |
| JVM         | CPU、堆内存、GC、线程转储、JFR pinning 事件 |
| 应用接口    | QPS、RT、错误率、限流次数、降级次数         |

## 项目落地建议

本节用于说明虚拟线程在实际项目中的推荐使用场景、已有项目改造方式和上线前检查项。虚拟线程改造不应只做配置开关变更，而应结合业务链路、外部资源、监控指标和回滚方案一起落地。

### 推荐使用场景

虚拟线程推荐优先用于同步阻塞风格的后端服务，尤其是 Spring MVC、MyBatis、JDBC、阻塞 HTTP 客户端、文件 IO 和第三方 SDK 调用较多的项目。

推荐场景如下：

| 场景                      | 推荐程度 | 说明                                       |
| ------------------------- | -------- | ------------------------------------------ |
| Spring MVC 同步接口       | 高       | 请求处理保留同步代码，迁移成本低           |
| 数据库查询较多的接口      | 高       | 适合等待数据库返回的 IO 密集型场景         |
| 远程 HTTP 聚合接口        | 高       | 一个请求内调用多个下游接口时较适合         |
| 文件上传下载              | 中高     | 适合等待磁盘或网络 IO 的场景               |
| 消息投递、通知发送        | 中高     | 适合异步 IO 型任务                         |
| 定时数据同步              | 中       | 适合等待外部系统返回的任务                 |
| CPU 密集型任务            | 低       | 不建议依赖虚拟线程提升性能                 |
| 响应式 WebFlux 纯异步链路 | 低       | 原本不是 thread-per-request 模型，收益有限 |
| 强锁竞争老系统            | 谨慎     | 需要先排查 `synchronized` 和阻塞锁问题     |

Oracle 的虚拟线程采用指南也强调，同步阻塞风格、每个任务对应线程的应用更容易从虚拟线程中受益；异步风格框架或不为每个任务分配线程的框架，不应期待获得同等收益。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html?utm_source=chatgpt.com))

### 改造已有项目的方式

已有项目改造建议采用“先验证、再灰度、后推广”的方式。不要在全站一次性开启虚拟线程后直接上线，尤其是存在大量数据库访问、远程调用、定时任务和老旧同步锁代码的系统。

建议改造步骤如下：

| 阶段     | 操作                                            | 目标                     |
| -------- | ----------------------------------------------- | ------------------------ |
| 评估阶段 | 梳理接口类型、阻塞点、连接池、锁使用            | 判断是否适合虚拟线程     |
| 本地验证 | 启用 `spring.threads.virtual.enabled=true`      | 验证启动、接口、线程状态 |
| 压测验证 | 对核心接口做平台线程与虚拟线程对比              | 判断吞吐、延迟和资源瓶颈 |
| 风险治理 | 处理长事务、慢 SQL、锁内 IO、ThreadLocal 大对象 | 降低上线风险             |
| 灰度发布 | 小流量启用虚拟线程实例                          | 观察真实流量表现         |
| 全量推广 | 分批扩大实例范围                                | 保留回滚配置             |

推荐先改造以下类型接口：

| 接口类型     | 改造建议                             |
| ------------ | ------------------------------------ |
| 查询型接口   | 优先改造，通常风险较低               |
| 下游聚合接口 | 优先改造，但必须控制 HTTP 超时和并发 |
| 文件 IO 接口 | 可以改造，但要限制文件大小和路径权限 |
| 写事务接口   | 谨慎改造，重点关注事务时间和连接占用 |
| 高 CPU 接口  | 不建议作为虚拟线程收益验证对象       |

已有项目中如果已经自定义了大量线程池，需要重点梳理：

```text
1. 是否存在自定义 Executor Bean
2. 是否存在名为 applicationTaskExecutor 的 Bean
3. @Async 是否指定了 executor 名称
4. @Scheduled 是否依赖线程池大小控制并发
5. 是否存在固定线程池中再提交虚拟线程的重复封装
6. 是否存在 ThreadLocal 缓存大对象或连接资源
7. 是否存在 synchronized 方法中执行远程调用或数据库查询
```

Spring Boot 文档说明，如果应用上下文中没有自定义 `Executor` Bean，Boot 会自动配置 `AsyncTaskExecutor`；启用虚拟线程后，该执行器会使用虚拟线程。如果已经定义自定义执行器，则 `@EnableAsync`、Spring MVC 异步处理等场景的执行器选择规则需要额外确认。([Home](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html?utm_source=chatgpt.com))

### 上线前检查项

上线前检查项用于确保虚拟线程改造具备可观测、可回滚和可解释的运行状态。检查重点不是“是否启用成功”，而是启用后外部资源、锁、线程上下文、异常处理和监控体系是否都已准备好。

上线前建议按以下清单检查：

| 检查项                                | 是否必须         | 检查说明                           |
| ------------------------------------- | ---------------- | ---------------------------------- |
| JDK 版本为 21+                        | 必须             | 开发、构建、运行、容器镜像保持一致 |
| Spring Boot 版本为 3.2+               | 建议必须         | 便于使用官方虚拟线程自动配置       |
| `spring.threads.virtual.enabled=true` | 必须             | 确认虚拟线程开关已启用             |
| `spring.main.keep-alive=true`         | 有后台任务时必须 | 避免只剩守护线程导致 JVM 退出      |
| 核心接口已压测                        | 必须             | 对比平台线程和虚拟线程表现         |
| 数据库连接池已评估                    | 必须             | 避免大量请求等待连接池             |
| HTTP 超时已配置                       | 必须             | 避免大量虚拟线程长时间等待下游     |
| 慢 SQL 已治理                         | 必须             | 虚拟线程不能解决 SQL 慢的问题      |
| `synchronized` 阻塞已排查             | 建议必须         | 避免频繁 pinning                   |
| ThreadLocal 已排查                    | 建议必须         | 避免大对象和资源缓存               |
| JFR 或 jcmd 排查方式已准备            | 建议必须         | 便于定位线上问题                   |
| 监控面板已补充                        | 必须             | 关注 QPS、RT、错误率、连接池、GC   |
| 回滚配置已准备                        | 必须             | 可快速关闭虚拟线程                 |

推荐保留以下回滚配置：

文件位置：`src/main/resources/application-platform.yml`

```yaml
spring:
  threads:
    virtual:
      # 回滚到平台线程模型
      enabled: false
```

启动时可通过 profile 快速回滚：

```bash
java -jar target/spring-boot-virtual-thread-demo-1.0.0.jar \
  --spring.profiles.active=platform
```

上线后建议观察以下日志和指标：

```text
1. 接口 P95 / P99 响应时间是否下降或恶化
2. 错误率、超时率、拒绝率是否上升
3. HikariCP pending 连接等待是否增加
4. 下游 HTTP 调用是否出现更多超时
5. JVM CPU 是否接近打满
6. 堆内存和 GC 是否出现异常增长
7. JFR 是否出现频繁 VirtualThreadPinned 事件
8. jcmd 线程转储中虚拟线程数量是否异常
```

虚拟线程适合作为同步阻塞系统的并发能力增强手段，但不应被当作容量规划的替代品。最终上线标准应以真实业务链路压测结果、连接池指标、下游承载能力和可回滚方案为准。