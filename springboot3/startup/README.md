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

