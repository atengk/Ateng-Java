# 应用启动

## Runner

### CommandLineRunner

CommandLineRunner：这个接口的run方法会在Spring Boot应用启动时（所有Spring上下文和Bean都初始化完毕之后）执行。它接收一个String[] args数组，表示从命令行传递的参数。

```java
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class MyCommandLineRunner implements CommandLineRunner {
    @Override
    public void run(String... args) {
         /*
        在程序后面添加：run --name=ateng --age=24
         */
        List<String> argList = CollUtil.newArrayList(args);
        log.info("获取到程序所有参数: {}", argList);
    }
}
```

### ApplicationRunner

ApplicationRunner：与CommandLineRunner非常类似，它的run方法也在Spring Boot应用启动完成后执行。不同的是，它接受的是一个ApplicationArguments对象，它比String[] args提供了更多的功能，可以获取到选项参数和非选项参数。

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class MyApplicationRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        /*
        在程序后面添加：run --name=ateng --age=24
         */

        // 选项参数
        List<String> name = args.getOptionValues("name");
        List<String> age = args.getOptionValues("age");
        log.info("获取到参数：--name {} --age {}", name, age); // 获取到参数：--name [ateng] --age [24]

        // 非选项参数
        List<String> nonOptionArgs = args.getNonOptionArgs();
        log.info("获取到非参数：{}", nonOptionArgs); // 获取到非参数：[run]

    }
}
```

## Event事件

1. 在应用完全启动后执行的方法

在应用启动并且ApplicationContext准备好后执行。

```java
    @EventListener(ApplicationReadyEvent.class)
    public void event1() {
        log.info("由{}启动...", "@EventListener(ApplicationReadyEvent.class)");
    }
```

2. 监听自定义事件

用于监听自定义事件，当事件被发布时执行。

创建事件类

```java
import org.springframework.context.ApplicationEvent;

public class MyCustomEvent extends ApplicationEvent {
    private final String message;

    public MyCustomEvent(Object source, String message) {
        super(source);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
```

创建自定义事件

```java
    @EventListener
    public void event2(MyCustomEvent myCustomEvent) {
        log.info("myCustomEvent={}", myCustomEvent.getMessage());
    }
```

3. 发布自定义事件

`source` 参数 Spring 强制要求非空，必须传入一个非 `null` 的对象。建议规范写法为：this

```java
MyCustomEvent event = new MyCustomEvent(this, message);
ApplicationContext context = SpringUtil.getApplicationContext();
context.publishEvent(event);
```



## Bean

1. 定义Bean

初始化一个Bean

```java
@Configuration
@Slf4j
public class MyConfig {

    /**
     * @Bean方法：定义并初始化Spring容器中的Bean。
     * @return String
     */
    @Bean
    public String myBean() {
        return "This is a bean";
    }

}
```

2. 配置@PostConstruct

@PostConstruct注解的方法会在Spring Bean完全初始化之后执行，但在Spring容器中所有的Bean都完全加载之前。这个方法通常用于初始化逻辑，确保所有依赖注入的组件都已准备好。

```java
    @PostConstruct
    public void myBean() {
        String myBean = SpringUtil.getApplicationContext().getBean("myBean", String.class);
        log.info("myBean={}", myBean);
    }
```

3. 配置@PreDestroy

@PreDestroy 是一个生命周期回调注解，它用于标注在 Spring Bean 销毁之前执行的方法。具体来说，当 Spring 容器销毁一个 Bean 时，Spring 会自动调用该 Bean 上标记了 @PreDestroy 注解的方法。

```java
    @PreDestroy
    public void cleanup() {
        log.info("在Bean销毁前执行的方法");
    }
```



## InitializingBean / DisposableBean

```java
package local.ateng.java.netty.netty;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NettyServerStarter implements InitializingBean, DisposableBean {

    private final NettyServer nettyServer;

    @Override
    public void afterPropertiesSet() {
        nettyServer.start();
    }

    @Override
    public void destroy() {
        nettyServer.stop();
    }
}
```

**Spring 容器启动时（Bean 初始化阶段）**

**完整顺序大致是：**

1. 实例化 `NettyServerStarter`
2. 构造函数注入 `NettyServer`
3. 属性注入完成
4. 👉 **调用 `afterPropertiesSet()`**
5. Bean 就绪

所以：

> **Netty Server 会在 Spring 容器“完全准备好 Bean 之后”启动**

这是非常关键的一点。

------

**Spring 容器关闭时（优雅停机阶段）**

当发生下面任意情况：

- 应用正常退出
- Spring Boot 收到 `SIGTERM`（docker / k8s / kill）
- `ApplicationContext.close()`

Spring 会：

1. 触发 `ContextClosedEvent`
2. 👉 **调用 `DisposableBean.destroy()`**
3. 销毁 Bean

于是：

> **Netty Server 会在 Spring 关闭时被主动 stop**



## SmartLifecycle

`SmartLifecycle` 是 Spring 提供的 **高级生命周期管理接口**，适用于需要在 **Spring 容器启动和停止时执行特定逻辑** 的场景。

和 `Lifecycle` 相比，它具备三大增强点：

| 功能                 | Lifecycle  | SmartLifecycle |
| -------------------- | ---------- | -------------- |
| 是否自动启动         | ❌ 默认不会 | ✔ 默认自动启动 |
| 是否支持优先级 phase | ❌ 不支持   | ✔ 支持         |
| 是否支持异步启动     | ❌ 不支持   | ✔ 支持         |

> 适用于：**消息队列消费者、线程池、定时器、Socket 服务、资源管理器等**。

### 最小可运行示例

下面示例展示：

- bean 启动时开启消费者线程
- 容器关闭时优雅停止

```java
package local.ateng.java.config.runner;

import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class QueueConsumer implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            worker = new Thread(() -> {
                while (running.get()) {
                    try {
                        // 模拟消费
                        System.out.println("Consuming message...");
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }, "queue-consumer-thread");

            worker.start();
            System.out.println("Consumer started!");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            System.out.println("Stopping consumer...");
            worker.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        // 自动启动
        return true;
    }

    @Override
    public int getPhase() {
        // 越小越早启动
        return 0;
    }
}

```

### 生命周期表

✅ SmartLifecycle 与 Spring Boot 生命周期重点对照表（专业精简版）

| 生命周期分类                       | 触发方式                                | 相关方法 / 注解                            | 触发时机（Spring Boot）                   | 用途说明                                            |
| ---------------------------------- | --------------------------------------- | ------------------------------------------ | ----------------------------------------- | --------------------------------------------------- |
| **Bean 初始化阶段**                | Spring 创建 Bean                        | `@PostConstruct`                           | Bean 完成依赖注入后                       | 初始化资源（如缓存预热、校验配置）                  |
| **容器刷新完成**                   | Spring Boot 完成所有 Bean 创建          | SmartLifecycle → `getPhase()`              | Bean 初始化完成之后，按 phase 排序        | 控制多个 SmartLifecycle 的启动顺序                  |
| **生命周期自动启动**               | Spring 自动启动 SmartLifecycle          | SmartLifecycle → `isAutoStartup()`         | 容器准备启动所有生命周期 Bean 时          | 是否自动调用 `start()`                              |
| **组件启动**                       | Spring Boot 自动调用                    | SmartLifecycle → `start()`                 | Spring Boot 完全启动（Bean 初始化完毕后） | 启动消费者、线程池任务、Socket 服务等               |
| **组件运行状态查询**               | Spring 检查是否已启动                   | SmartLifecycle → `isRunning()`             | start() 执行后 / stop() 前                | 返回组件是否正在运行                                |
| **应用准备就绪（可对外提供服务）** | Spring Boot 事件                        | `ApplicationReadyEvent`（可选）            | 内嵌容器（如 Tomcat）启动完成后           | 初始化依赖外部服务的任务（如注册心跳、MQ 动态订阅） |
| **应用关闭（优雅）**               | Spring 收到关闭信号（Ctrl+C / SIGTERM） | SmartLifecycle → `stop(Runnable callback)` | Spring 优雅停止阶段第一波执行             | 优雅停止消费者、通知 callback                       |
| **组件停止（非 Callback 版）**     | Spring 停止 Lifecycle                   | SmartLifecycle → `stop()`                  | stop(callback) 之后（作为 fallback）      | 停止线程、释放资源                                  |
| **停止状态检查**                   | Spring 检查是否停止成功                 | SmartLifecycle → `isRunning()`             | stop() 调用后                             | 若为 true → 重复停止流程                            |
| **Bean 销毁阶段**                  | Spring 销毁 Bean                        | `@PreDestroy`                              | 所有生命周期组件停止之后                  | 最终资源释放（线程池、连接、文件句柄等）            |
| **容器关闭完成**                   | JVM 退出                                | （无方法）                                 | @PreDestroy 之后                          | 程序完全退出                                        |

------

🎯 最关键的 6 个生命周期节点（必须记住）

| 排序 | 生命周期节点                     | 方法 / 注解               | 说明                                |
| ---- | -------------------------------- | ------------------------- | ----------------------------------- |
| 1    | Bean 初始化完成                  | `@PostConstruct`          | Bean 已准备好，但系统尚未启动       |
| 2    | SmartLifecycle 组件按 phase 排序 | `getPhase()`              | 决定启动顺序                        |
| 3    | 自动启动组件                     | `start()`                 | MQ 消费者、线程池、后台任务启动     |
| 4    | 应用完全启动（对外可用）         | `ApplicationReadyEvent`   | 适用于需要等系统全启动后执行的逻辑  |
| 5    | 优雅停机开始                     | `stop(Runnable callback)` | 主动调用 callback，允许组件优雅关闭 |
| 6    | Bean 销毁                        | `@PreDestroy`             | 最终释放资源（连接、线程池）        |

------

🔥 SmartLifecycle 的 5 个核心点（总结版）

| 方法                      | 必要性          | 说明                              |
| ------------------------- | --------------- | --------------------------------- |
| `isAutoStartup()`         | ⭐ 必须理解      | 是否自动调用 `start()`            |
| `getPhase()`              | ⭐ 必须理解      | 控制启动/停止顺序                 |
| `start()`                 | ⭐ 必须实现      | 启动组件（消费者/线程等）         |
| `stop(Runnable callback)` | ⭐⭐ 强烈建议实现 | 优雅关停组件（必须调用 callback） |
| `isRunning()`             | ⭐ 必须实现      | 指示组件是否处于运行状态          |

