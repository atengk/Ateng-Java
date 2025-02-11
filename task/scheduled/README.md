# SpringBoot 定时任务

Spring Boot 定时任务是基于 Spring 的定时调度功能（通常是通过 @Scheduled 注解实现），让开发者能够方便地在应用中添加定时执行的任务。Spring Boot 提供了简单的方式来创建和配置定时任务，同时保证了自动化配置和灵活的调度方式。

## 基础配置

### 编辑 `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <!-- 项目模型版本 -->
    <modelVersion>4.0.0</modelVersion>

    <!-- 项目坐标 -->
    <groupId>local.ateng.java</groupId>
    <artifactId>scheduled</artifactId>
    <version>v1.0</version>
    <name>scheduled</name>
    <description>SpringBoot 定时任务</description>

    <!-- 项目属性 -->
    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <spring-boot.version>3.4.1</spring-boot.version>
        <maven-compiler.version>3.12.1</maven-compiler.version>
        <lombok.version>1.18.36</lombok.version>
    </properties>

    <!-- 项目依赖 -->
    <dependencies>
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

        <!-- Spring Boot Starter Test: 包含用于测试Spring Boot应用程序的依赖项 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Lombok: 简化Java代码编写的依赖项 -->
        <!-- https://mvnrepository.com/artifact/org.projectlombok/lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

    </dependencies>

    <!-- Spring Boot 依赖管理 -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- 插件仓库配置 -->
    <repositories>
        <!-- Central Repository -->
        <repository>
            <id>central</id>
            <name>阿里云中央仓库</name>
            <url>https://maven.aliyun.com/repository/central</url>
            <!--<name>Maven官方中央仓库</name>
            <url>https://repo.maven.apache.org/maven2/</url>-->
        </repository>
    </repositories>

    <!-- 构建配置 -->
    <build>
        <finalName>${project.name}-${project.version}</finalName>
        <plugins>
            <!-- Maven 编译插件 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven-compiler.version}</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <encoding>${project.build.sourceEncoding}</encoding>
                    <!-- 编译参数 -->
                    <compilerArgs>
                        <!-- 启用Java 8参数名称保留功能 -->
                        <arg>-parameters</arg>
                    </compilerArgs>
                </configuration>
            </plugin>

            <!-- Spring Boot Maven 插件 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
                <executions>
                    <execution>
                        <id>repackage</id>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
        <resources>
            <!-- 第一个资源配置块 -->
            <resource>
                <directory>src/main/resources</directory>
                <filtering>false</filtering>
            </resource>
            <!-- 第二个资源配置块 -->
            <resource>
                <directory>src/main/resources</directory>
                <includes>
                    <include>application*</include>
                    <include>bootstrap*.yml</include>
                    <include>common*</include>
                    <include>banner*</include>
                </includes>
                <filtering>true</filtering>
            </resource>
        </resources>
    </build>

</project>
```

### 编辑 `application.yml`

```yaml
server:
  port: 19001
  servlet:
    context-path: /
spring:
  main:
    web-application-type: servlet
  application:
    name: ${project.artifactId}
---
# 开启虚拟线程
spring:
  threads:
    virtual:
      enabled: true
```

### 启用定时任务

启用定时任务并开启异步

```java
package local.ateng.java.scheduled;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class TaskScheduledApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskScheduledApplication.class, args);
    }

}
```



## 使用定时任务

### 创建类

```java
package local.ateng.java.scheduled.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 定时任务示例
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-11
 */
@Component
@Slf4j
public class DemoTask {

}
```

### 固定间隔时间

固定间隔时间：任务会按照指定的固定时间间隔执行，并且程序启动立即运行一次

```java
    /**
     * 固定间隔时间：任务会按照指定的固定时间间隔执行，并且程序启动立即运行一次
     */
    @Scheduled(fixedRate = 5000) // 每隔 5 秒执行一次
    @Async
    public void fixedRateTask() {
        log.info("[固定间隔时间] 执行任务...");
    }
```

### 固定延迟时间

固定延迟时间：任务在执行完成后，等待指定的延迟时间再开始下一次执行，并且程序启动立即运行一次

```java
    /**
     * 固定延迟时间：任务在执行完成后，等待指定的延迟时间再开始下一次执行，并且程序启动立即运行一次
     */
    @Scheduled(fixedDelay = 5000) // 上一个任务执行完后 5 秒执行下一次
    @Async
    public void fixedDelayTask() {
        log.info("[固定延迟时间] 执行任务...");
    }
```

### 延迟执行

延迟执行：任务在启动时延迟指定时间后执行

```java
    /**
     * 延迟执行：任务在启动时延迟指定时间后执行
     */
    @Scheduled(initialDelay = 10000, fixedRate = 5000) // 10 秒后开始执行，之后每隔 5 秒执行一次
    @Async
    public void initialDelayTask() {
        log.info("[延迟执行][固定间隔时间] 执行任务...");
    }
```

### Cron 表达式

Cron 表达式：通过 cron 表达式来设置任务执行的频率，这是一种非常灵活的方式，适用于复杂的定时任务需求。

```java
    /**
     * Cron 表达式
     */
    @Scheduled(cron = "0 0/5 9-17 * * MON-FRI") // 每周一到周五，9:00 到 17:00 每隔 5 分钟执行一次
    @Async
    public void cronTask() {
        log.info("[Cron 表达式] 执行任务...");
    }
```

Cron 表达式格式：

```scss
秒（0-59）
分钟（0-59）
小时（0-23）
日（1-31）
月（1-12）
星期（0-6）(0 = Sunday)
```



## 动态定时任务

### 创建任务服务

```java
package local.ateng.java.scheduled.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DynamicTaskService {
    private final SimpleAsyncTaskScheduler taskScheduler;
    private ScheduledFuture<?> currentTask;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void init() {
        // 设置任务默认时间
        this.startCronTask("0/5 * * * * ?");
    }

    /**
     * 启动一个定时任务（固定间隔时间）
     */
    public void startFixedRateTask(long startTime, long period) {
        stopTask(); // 先停止当前任务
        Runnable task = () -> executeTask();
        currentTask = taskScheduler.scheduleAtFixedRate(task, Instant.now().plusSeconds(startTime), Duration.ofSeconds(period));
        log.info("任务已启动！执行周期：{} 秒", period);
    }

    /**
     * 启动一个 CRON 任务
     */
    public void startCronTask(String cronExpression) {
        stopTask(); // 先停止当前任务
        Runnable task = () -> executeTask();
        currentTask = taskScheduler.schedule(task, new CronTrigger(cronExpression));
        log.info("任务已启动！CRON 表达式：{}", cronExpression);
    }

    /**
     * 执行任务内容
     */
    public void executeTask() {
        log.info("任务开始执行，线程：{}", Thread.currentThread());
        try {
            // 模拟任务执行
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("执行完成！线程：{}", Thread.currentThread());
    }

    /**
     * 停止当前任务
     */
    public void stopTask() {
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel(false);
            log.info("当前任务已停止！");
        }
    }

    /**
     * 获取当前任务状态
     */
    public Boolean getTaskStatus() {
        if (currentTask == null || currentTask.isCancelled()) {
            return false;
        }
        return true;
    }
}
```

### 创建任务接口

```java
package local.ateng.java.scheduled.controller;

import local.ateng.java.scheduled.service.DynamicTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DynamicTaskController {
    private final DynamicTaskService taskService;

    @PostMapping("/startFixedRate")
    public String startFixedRateTask(long startTime, long period) {
        taskService.startFixedRateTask(startTime, period);
        return "固定间隔任务启动成功，初始时间：" + startTime + "秒，周期：" + period + " 秒";
    }

    @PostMapping("/startCron")
    public String startCronTask(String cron) {
        taskService.startCronTask(cron);
        return "CRON 任务启动成功，表达式：" + cron;
    }

    @DeleteMapping("/stop")
    public String stopTask() {
        taskService.stopTask();
        return "任务已停止";
    }

    @GetMapping("/status")
    public Boolean getTaskStatus() {
        return taskService.getTaskStatus();
    }

}
```

### 使用定时任务

好的，以下是简单的接口调用示例：

#### **1. 启动固定间隔任务**

**请求：**
```http
POST http://localhost:19001/schedule/startFixedRate?startTime=5&period=10
```

**响应：**
```json
"固定间隔任务启动成功，初始时间：5秒，周期：10 秒"
```

---

#### **2. 启动 CRON 任务**

**请求：**
```http
POST http://localhost:19001/schedule/startCron?cron=0/5 * * * * *
```

**响应：**
```json
"CRON 任务启动成功，表达式：0/5 * * * * *"
```

---

#### **3. 停止任务**

**请求：**
```http
DELETE http://localhost:19001/schedule/stop
```

**响应：**
```json
"任务已停止"
```

---

#### **4. 获取任务状态**

**请求：**
```http
GET http://localhost:19001/schedule/status
```

**响应：**
```json
"任务正在执行中"
```
