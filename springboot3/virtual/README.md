# SpringBoot3虚拟线程相关的模块

虚拟线程是轻量级的线程，它由 JVM 管理，而不是直接由操作系统管理。与传统的内核线程（操作系统线程）相比，虚拟线程具有以下特点：

- **轻量级**：一个虚拟线程只占用极少的资源，可以创建数百万个虚拟线程而不会显著增加内存消耗。
- **调度灵活**：虚拟线程的调度由 JVM 完成，脱离了操作系统调度器。
- **阻塞操作非阻塞化**：虚拟线程在执行阻塞操作（如 I/O）时，JVM 会挂起线程而非占用操作系统资源。

启用虚拟线程后，Spring Boot 会自动将一些常用的线程池替换为基于虚拟线程的实现，例如：

1. **异步任务 (@Async)**：
    - 默认会使用虚拟线程来执行异步任务。
2. **Web 请求处理**：
    - Spring MVC 的请求处理线程由虚拟线程提供支持。
3. **任务执行器 (TaskExecutor)**：
    - Spring 默认会自动提供一个基于虚拟线程的 `SimpleAsyncTaskExecutor`。

在 Spring Boot 3 中启用虚拟线程（通过 spring.threads.virtual.enabled=true）后，通常不需要再手动配置传统的线程池，因为虚拟线程的轻量特性使得它本质上可以看作一个“无限大小”的线程池。但是，根据具体的应用场景，可能需要一些额外的配置或调整。



## 前提条件

- JDK 21（长期支持版，LTS）



## 编辑配置

**使用undertow容器**

```xml
        <!-- Spring Boot Web Starter: 包含用于构建Web应用程序的Spring Boot依赖项 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <exclusions>
                <exclusion>
                    <artifactId>spring-boot-starter-tomcat</artifactId>
                    <groupId>org.springframework.boot</groupId>
                </exclusion>
            </exclusions>
        </dependency>
        <!-- Web 容器使用 undertow 性能更强 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-undertow</artifactId>
        </dependency>
```

**开启虚拟线程**

```yaml
---
# 开启虚拟线程
spring:
  threads:
    virtual:
      enabled: true
```

**开启异步**

```java
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class SpringBoot3VirtualApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBoot3VirtualApplication.class, args);
    }

}
```



## 验证虚拟线程

### 异步任务

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DemoTask {

    @Scheduled(fixedRate = 5000)
    @Async
    public void demoTask() {
        String threadInfo = Thread.currentThread().toString();
        log.info("Current thread: " + threadInfo);
    }

}
```

打印如下日志可以看到VirtualThread（虚拟线程）字样

```
2025-01-07T22:29:44.178+08:00  INFO 16332 --- [virtual] [        task-19] local.ateng.java.virtual.test.DemoTask   : Current thread: VirtualThread[#99,task-19]/runnable@ForkJoinPool-1-worker-5
```



### Web请求

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class DemoController {

    @GetMapping("/test")
    public String test() {
        String threadInfo = Thread.currentThread().toString();
        log.info("Current thread: " + threadInfo);
        return "Thread info: " + threadInfo;
    }
}
```

打印如下日志可以看到VirtualThread（虚拟线程）字样

```
2025-01-07T22:24:56.757+08:00  INFO 49264 --- [virtual] [omcat-handler-0] l.a.java.virtual.test.DemoController     : Current thread: VirtualThread[#66,tomcat-handler-0]/runnable@ForkJoinPool-1-worker-3
```

