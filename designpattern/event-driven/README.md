# 设计模式：事件驱动模式

事件驱动模式用于把业务动作和后续扩展动作解耦。某个业务完成后发布事件，订阅者根据事件执行自己的逻辑，发布方不需要直接依赖具体订阅者。在 JDK21 和 Spring Boot 3 项目中，事件驱动模式常用于用户注册后发送通知、订单支付后发放权益、库存变更后刷新缓存、配置发布后通知节点、业务操作后写审计日志、领域事件、异步任务、消息队列解耦等场景。

需要注意：事件驱动模式不是 GoF 23 种设计模式之一，属于这次设计模式文档里的“遗漏补充”。它和观察者模式关系很近，但在 Spring Boot 项目中，事件驱动通常会进一步结合 Spring Event、事务事件、异步线程池、MQ、最终一致性和失败重试。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证事件驱动行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot AMQP，可选：用于 RabbitMQ 消息事件示例，暂时不接 MQ 可不引入 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、ID、时间等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- Lombok，简化日志对象、构造方法、Getter 等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖，用于单元测试验证 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果只使用 Spring 内置事件机制，不需要引入 `spring-boot-starter-amqp`。如果要把事件发布到 RabbitMQ、Kafka、RocketMQ 等消息中间件，再引入对应依赖。

## 核心概念

事件驱动模式的核心目标是让业务主流程只发布“发生了什么”，后续处理由监听者自己订阅并执行。

常见角色如下：

| 角色           | 说明                                   |
| -------------- | -------------------------------------- |
| Event          | 事件对象，描述发生了什么               |
| Publisher      | 事件发布者，负责发布事件               |
| Listener       | 事件监听者，订阅并处理事件             |
| Event Bus      | 事件总线，负责分发事件                 |
| Event Store    | 事件存储，可选，用于可靠事件、事件溯源 |
| Message Broker | 消息中间件，可选，用于跨服务事件传递   |

用户注册场景示例：

```text
用户注册成功
    -> 发布 UserRegisteredEvent
        -> 发送欢迎短信
        -> 发放新人优惠券
        -> 写注册审计日志
        -> 同步用户画像
```

没有事件驱动时，用户服务可能直接调用多个服务：

```text
UserService
    -> NoticeService
    -> CouponService
    -> AuditService
    -> ProfileService
```

使用事件驱动后，用户服务只关心注册本身：

```text
UserService
    -> publish(UserRegisteredEvent)

NoticeListener
CouponListener
AuditListener
ProfileListener
```

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring Event 本地事件 > @Async 异步事件 > TransactionalEventListener 事务事件 > MQ 跨服务事件
```

选择方式可以简单理解为：

```text
同一个应用内解耦：Spring Event
同一个应用内异步解耦：Spring Event + @Async
需要事务提交后再处理：@TransactionalEventListener
跨服务解耦和可靠投递：MQ
```

## Spring 本地事件

Spring 内置事件适合单体应用或同一个 Spring Boot 应用内部的模块解耦。下面以用户注册为例，注册成功后发布用户注册事件，多个监听器分别处理欢迎通知、新人优惠券和审计日志。

整体流程如下：

```text
Controller
    -> UserRegisterService
        -> 保存用户
        -> ApplicationEventPublisher.publishEvent(UserRegisteredEvent)
            -> WelcomeNoticeListener
            -> NewUserCouponListener
            -> RegisterAuditListener
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── EventDrivenApplication.java
├── controller/
│   └── UserRegisterController.java
├── dto/
│   ├── UserRegisterRequest.java
│   └── UserRegisterResponse.java
├── event/
│   └── UserRegisteredEvent.java
├── listener/
│   ├── WelcomeNoticeListener.java
│   ├── NewUserCouponListener.java
│   └── RegisterAuditListener.java
└── service/
    ├── UserRegisterService.java
    └── impl/
        └── UserRegisterServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/EventDrivenApplication.java`

下面是 Spring Boot 启动类，同时启用异步能力，后续异步监听器会用到。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 事件驱动模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-01
 */
@EnableAsync
@SpringBootApplication
public class EventDrivenApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(EventDrivenApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/UserRegisterRequest.java`

下面是用户注册请求对象。

```java
package io.github.atengk.design.dto;

/**
 * 用户注册请求
 *
 * @param username 用户名
 * @param mobile   手机号
 * @param channel  注册渠道
 * @author Ateng
 * @since 2026-05-01
 */
public record UserRegisterRequest(
        String username,
        String mobile,
        String channel
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/UserRegisterResponse.java`

下面是用户注册响应对象。

```java
package io.github.atengk.design.dto;

/**
 * 用户注册响应
 *
 * @param userId   用户ID
 * @param username 用户名
 * @param mobile   手机号
 * @param message  响应消息
 * @author Ateng
 * @since 2026-05-01
 */
public record UserRegisterResponse(
        Long userId,
        String username,
        String mobile,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/event/UserRegisteredEvent.java`

下面是用户注册事件对象。事件对象只描述“用户注册成功”这个事实，不应该承载复杂业务处理逻辑。

```java
package io.github.atengk.design.event;

import java.time.LocalDateTime;

/**
 * 用户注册事件
 *
 * @param eventId   事件ID
 * @param userId    用户ID
 * @param username  用户名
 * @param mobile    手机号
 * @param channel   注册渠道
 * @param eventTime 事件时间
 * @author Ateng
 * @since 2026-05-01
 */
public record UserRegisteredEvent(
        String eventId,
        Long userId,
        String username,
        String mobile,
        String channel,
        LocalDateTime eventTime
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/service/UserRegisterService.java`

下面是用户注册服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.UserRegisterRequest;
import io.github.atengk.design.dto.UserRegisterResponse;

/**
 * 用户注册服务
 *
 * @author Ateng
 * @since 2026-05-01
 */
public interface UserRegisterService {

    /**
     * 注册用户
     *
     * @param request 用户注册请求
     * @return 用户注册响应
     */
    UserRegisterResponse register(UserRegisterRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/UserRegisterServiceImpl.java`

下面是用户注册服务实现。它只负责注册主流程，注册成功后发布事件，不直接调用通知、优惠券和审计服务。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.UserRegisterRequest;
import io.github.atengk.design.dto.UserRegisterResponse;
import io.github.atengk.design.event.UserRegisteredEvent;
import io.github.atengk.design.service.UserRegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户注册服务实现
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegisterServiceImpl implements UserRegisterService {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 注册用户
     *
     * @param request 用户注册请求
     * @return 用户注册响应
     */
    @Override
    public UserRegisterResponse register(UserRegisterRequest request) {
        validateRequest(request);

        Long userId = IdUtil.getSnowflakeNextId();
        log.info("用户注册成功，用户ID：{}，用户名：{}，手机号：{}，渠道：{}",
                userId, request.username(), request.mobile(), request.channel());

        UserRegisteredEvent event = new UserRegisteredEvent(
                "EVT" + IdUtil.getSnowflakeNextId(),
                userId,
                request.username(),
                request.mobile(),
                StrUtil.blankToDefault(request.channel(), "unknown"),
                LocalDateTime.now()
        );

        applicationEventPublisher.publishEvent(event);
        log.info("发布用户注册事件成功，事件ID：{}，用户ID：{}", event.eventId(), event.userId());

        return new UserRegisterResponse(
                userId,
                request.username(),
                request.mobile(),
                "注册成功"
        );
    }

    /**
     * 校验注册请求
     *
     * @param request 用户注册请求
     */
    private void validateRequest(UserRegisterRequest request) {
        if (request == null) {
            log.warn("用户注册失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.hasBlank(request.username(), request.mobile())) {
            log.warn("用户注册失败，用户名或手机号为空");
            throw new IllegalArgumentException("用户名和手机号不能为空");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/listener/WelcomeNoticeListener.java`

下面是欢迎通知监听器。它只关心用户注册事件，不需要被用户注册服务直接调用。

```java
package io.github.atengk.design.listener;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.event.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 欢迎通知监听器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class WelcomeNoticeListener {

    /**
     * 监听用户注册事件并发送欢迎通知
     *
     * @param event 用户注册事件
     */
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        if (event == null) {
            log.warn("处理欢迎通知失败，用户注册事件为空");
            return;
        }

        String content = StrUtil.format("欢迎 {} 注册成功", event.username());
        log.info("发送欢迎通知成功，事件ID：{}，用户ID：{}，手机号：{}，内容：{}",
                event.eventId(), event.userId(), event.mobile(), content);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/listener/NewUserCouponListener.java`

下面是新人优惠券监听器，用于处理注册后的权益发放。

```java
package io.github.atengk.design.listener;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.design.event.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 新人优惠券监听器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class NewUserCouponListener {

    /**
     * 监听用户注册事件并发放新人优惠券
     *
     * @param event 用户注册事件
     */
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        if (event == null) {
            log.warn("发放新人优惠券失败，用户注册事件为空");
            return;
        }

        String couponNo = "COUPON" + IdUtil.getSnowflakeNextId();
        log.info("发放新人优惠券成功，事件ID：{}，用户ID：{}，优惠券编号：{}",
                event.eventId(), event.userId(), couponNo);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/listener/RegisterAuditListener.java`

下面是注册审计监听器，用于记录用户注册后的审计日志。

```java
package io.github.atengk.design.listener;

import io.github.atengk.design.event.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 注册审计监听器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class RegisterAuditListener {

    /**
     * 监听用户注册事件并记录审计日志
     *
     * @param event 用户注册事件
     */
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        if (event == null) {
            log.warn("记录注册审计失败，用户注册事件为空");
            return;
        }

        log.info("记录用户注册审计成功，事件ID：{}，用户ID：{}，渠道：{}，事件时间：{}",
                event.eventId(), event.userId(), event.channel(), event.eventTime());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/UserRegisterController.java`

下面是用户注册接口，用于触发用户注册事件。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.UserRegisterRequest;
import io.github.atengk.design.dto.UserRegisterResponse;
import io.github.atengk.design.service.UserRegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户注册控制器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/event-driven/user")
public class UserRegisterController {

    private final UserRegisterService userRegisterService;

    /**
     * 注册用户
     *
     * @param username 用户名
     * @param mobile   手机号
     * @param channel  注册渠道
     * @return 用户注册响应
     */
    @PostMapping("/register")
    public UserRegisterResponse register(@RequestParam String username,
                                         @RequestParam String mobile,
                                         @RequestParam(required = false) String channel) {
        UserRegisterRequest request = new UserRegisterRequest(username, mobile, channel);
        return userRegisterService.register(request);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/event-driven/user/register?username=Ateng&mobile=13800138000&channel=APP"
```

可能返回：

```json
{
  "userId": 2020123456789017600,
  "username": "Ateng",
  "mobile": "13800138000",
  "message": "注册成功"
}
```

如果事件驱动正常，可以看到类似日志：

```text
用户注册成功，用户ID：2020123456789017600，用户名：Ateng，手机号：13800138000，渠道：APP
发布用户注册事件成功，事件ID：EVT2020123456789017601，用户ID：2020123456789017600
发送欢迎通知成功，事件ID：EVT2020123456789017601，用户ID：2020123456789017600，手机号：13800138000，内容：欢迎 Ateng 注册成功
发放新人优惠券成功，事件ID：EVT2020123456789017601，用户ID：2020123456789017600，优惠券编号：COUPON2020123456789017602
记录用户注册审计成功，事件ID：EVT2020123456789017601，用户ID：2020123456789017600，渠道：APP，事件时间：2026-05-01T10:20:30
```

默认情况下，Spring 本地事件是同步执行的。也就是说，发布事件后，监听器会在当前线程中执行完成，然后主流程才继续返回。

## 异步事件

异步事件适合后续处理不应该阻塞主流程的场景。例如发送通知、写审计、同步画像、刷新缓存等。如果监听器执行较慢，建议使用 `@Async`。

需要注意：异步事件仍然是当前应用内部事件，不等于可靠消息队列。应用重启、线程池满、监听器异常，都需要额外处理。

### 异步线程池配置

文件位置：`src/main/java/io/github/atengk/design/config/EventAsyncConfig.java`

下面是事件异步线程池配置，专门用于事件监听器异步执行。

```java
package io.github.atengk.design.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 事件异步线程池配置
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Configuration
public class EventAsyncConfig {

    /**
     * 创建事件异步线程池
     *
     * @return 事件异步线程池
     */
    @Bean("eventTaskExecutor")
    public Executor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：常驻处理事件的线程数量
        executor.setCorePoolSize(4);

        // 最大线程数：事件突增时允许扩展的最大线程数量
        executor.setMaxPoolSize(8);

        // 队列容量：线程都忙时，事件任务进入队列等待
        executor.setQueueCapacity(200);

        // 线程名前缀：方便日志和问题排查
        executor.setThreadNamePrefix("event-task-");

        // 等待任务结束再关闭线程池，避免应用关闭时任务直接丢失
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("初始化事件异步线程池完成，核心线程数：{}，最大线程数：{}，队列容量：{}",
                4, 8, 200);
        return executor;
    }
}
```

### 异步监听器

文件位置：`src/main/java/io/github/atengk/design/listener/AsyncUserProfileListener.java`

下面是异步用户画像监听器。它模拟注册后异步同步用户画像，不阻塞注册接口返回。

```java
package io.github.atengk.design.listener;

import io.github.atengk.design.event.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 异步用户画像监听器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class AsyncUserProfileListener {

    /**
     * 监听用户注册事件并异步同步用户画像
     *
     * @param event 用户注册事件
     */
    @Async("eventTaskExecutor")
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        if (event == null) {
            log.warn("同步用户画像失败，用户注册事件为空");
            return;
        }

        log.info("开始异步同步用户画像，事件ID：{}，用户ID：{}，线程：{}",
                event.eventId(), event.userId(), Thread.currentThread().getName());

        try {
            Thread.sleep(300);
            log.info("异步同步用户画像成功，事件ID：{}，用户ID：{}",
                    event.eventId(), event.userId());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("异步同步用户画像被中断，事件ID：{}，用户ID：{}",
                    event.eventId(), event.userId());
        } catch (Exception exception) {
            log.warn("异步同步用户画像失败，事件ID：{}，用户ID：{}，异常：{}",
                    event.eventId(), event.userId(), exception.getMessage());
        }
    }
}
```

异步事件适合非核心、可补偿、允许延迟执行的动作。例如：

```text
发送欢迎通知
刷新用户画像
写扩展审计
同步搜索索引
清理临时缓存
```

不建议直接用异步本地事件处理强一致核心业务。例如：

```text
扣减库存
创建支付单
更新账户余额
确认订单状态
发放不可重复权益
```

这些动作如果必须可靠执行，建议使用数据库事务、可靠事件表或 MQ。

## 事务事件

在真实业务中，发布事件经常和数据库事务有关。例如“用户注册成功”必须等用户数据提交成功后再发送通知。如果事务回滚了，事件监听器不应该继续发优惠券或发送通知。

Spring 提供 `@TransactionalEventListener` 支持事务阶段监听。常用阶段如下：

| 阶段               | 说明                             |
| ------------------ | -------------------------------- |
| `BEFORE_COMMIT`    | 事务提交前触发                   |
| `AFTER_COMMIT`     | 事务提交后触发，最常用           |
| `AFTER_ROLLBACK`   | 事务回滚后触发                   |
| `AFTER_COMPLETION` | 事务完成后触发，不区分提交或回滚 |

示例写法如下：

```java
package io.github.atengk.design.listener;

import io.github.atengk.design.event.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 事务提交后用户注册监听器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class AfterCommitUserRegisteredListener {

    /**
     * 事务提交后处理用户注册事件
     *
     * @param event 用户注册事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegisteredAfterCommit(UserRegisteredEvent event) {
        log.info("事务提交后处理用户注册事件，事件ID：{}，用户ID：{}",
                event.eventId(), event.userId());
    }
}
```

使用 `@TransactionalEventListener` 时，需要主流程处于事务中。比如 Service 方法上使用 `@Transactional`，并且发布事件发生在事务内部。否则监听器可能不会按预期执行。

典型流程如下：

```text
@Transactional
注册用户
保存用户数据
发布 UserRegisteredEvent
提交事务
触发 AFTER_COMMIT 监听器
```

如果业务使用 MyBatis-Plus、JPA、JDBC 等数据库访问，建议核心事件都优先考虑事务提交后触发，避免“业务数据回滚，但事件副作用已经发生”。

## MQ 事件驱动

Spring 本地事件只适合同一个应用内部解耦。如果事件需要跨服务传递，或者要求更高可靠性，就应该使用 MQ。下面以 RabbitMQ 为例，用户注册成功后发送用户注册消息，另一个消费者异步处理。

MQ 事件驱动适合：

```text
跨服务通知
削峰填谷
失败重试
解耦核心链路
最终一致性
服务间事件广播
```

### RabbitMQ 配置

文件位置：`src/main/resources/application.yml`

下面是 RabbitMQ 连接配置。实际项目中请将地址、账号和密码放到配置中心或环境变量中。

```yaml
spring:
  rabbitmq:
    # RabbitMQ 服务地址
    host: localhost
    # RabbitMQ 服务端口
    port: 5672
    # RabbitMQ 登录用户
    username: guest
    # RabbitMQ 登录密码
    password: guest
    # 虚拟主机
    virtual-host: /
```

文件位置：`src/main/java/io/github/atengk/design/config/UserEventRabbitConfig.java`

下面是用户事件 RabbitMQ 交换机、队列和绑定配置。

```java
package io.github.atengk.design.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 用户事件RabbitMQ配置
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Configuration
public class UserEventRabbitConfig {

    public static final String USER_EVENT_EXCHANGE = "user.event.exchange";
    public static final String USER_REGISTERED_QUEUE = "user.registered.queue";
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";

    /**
     * 创建用户事件交换机
     *
     * @return Topic交换机
     */
    @Bean
    public TopicExchange userEventExchange() {
        return ExchangeBuilder.topicExchange(USER_EVENT_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 创建用户注册事件队列
     *
     * @return 用户注册事件队列
     */
    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable(USER_REGISTERED_QUEUE)
                .build();
    }

    /**
     * 绑定用户注册事件队列到交换机
     *
     * @param userRegisteredQueue 用户注册事件队列
     * @param userEventExchange   用户事件交换机
     * @return 绑定关系
     */
    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange userEventExchange) {
        return BindingBuilder.bind(userRegisteredQueue)
                .to(userEventExchange)
                .with(USER_REGISTERED_ROUTING_KEY);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/mq/UserRegisteredMessage.java`

下面是用户注册 MQ 消息对象。MQ 消息建议使用稳定字段，不要直接发送复杂实体对象。

```java
package io.github.atengk.design.mq;

import java.time.LocalDateTime;

/**
 * 用户注册消息
 *
 * @param eventId   事件ID
 * @param userId    用户ID
 * @param username  用户名
 * @param mobile    手机号
 * @param channel   注册渠道
 * @param eventTime 事件时间
 * @author Ateng
 * @since 2026-05-01
 */
public record UserRegisteredMessage(
        String eventId,
        Long userId,
        String username,
        String mobile,
        String channel,
        LocalDateTime eventTime
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/listener/UserRegisteredMqPublisher.java`

下面是本地事件到 MQ 的发布器。它监听本地用户注册事件，然后发送 MQ 消息。

```java
package io.github.atengk.design.listener;

import io.github.atengk.design.config.UserEventRabbitConfig;
import io.github.atengk.design.event.UserRegisteredEvent;
import io.github.atengk.design.mq.UserRegisteredMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 用户注册MQ发布器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredMqPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 监听用户注册事件并发送MQ消息
     *
     * @param event 用户注册事件
     */
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        if (event == null) {
            log.warn("发送用户注册MQ消息失败，事件为空");
            return;
        }

        UserRegisteredMessage message = new UserRegisteredMessage(
                event.eventId(),
                event.userId(),
                event.username(),
                event.mobile(),
                event.channel(),
                event.eventTime()
        );

        rabbitTemplate.convertAndSend(
                UserEventRabbitConfig.USER_EVENT_EXCHANGE,
                UserEventRabbitConfig.USER_REGISTERED_ROUTING_KEY,
                message
        );

        log.info("发送用户注册MQ消息成功，事件ID：{}，用户ID：{}",
                message.eventId(), message.userId());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/mq/UserRegisteredMessageListener.java`

下面是用户注册 MQ 消费者，用于处理跨服务或异步可靠消息。

```java
package io.github.atengk.design.mq;

import io.github.atengk.design.config.UserEventRabbitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 用户注册MQ消息监听器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class UserRegisteredMessageListener {

    /**
     * 监听用户注册MQ消息
     *
     * @param message 用户注册消息
     */
    @RabbitListener(queues = UserEventRabbitConfig.USER_REGISTERED_QUEUE)
    public void onMessage(UserRegisteredMessage message) {
        if (message == null) {
            log.warn("消费用户注册MQ消息失败，消息为空");
            return;
        }

        log.info("消费用户注册MQ消息成功，事件ID：{}，用户ID：{}，用户名：{}，渠道：{}",
                message.eventId(), message.userId(), message.username(), message.channel());
    }
}
```

MQ 方式比本地事件更适合生产环境跨服务场景，但也带来新的复杂度：

```text
消息重复消费
消息顺序
消息积压
死信队列
失败重试
幂等处理
消息投递确认
消息版本兼容
```

因此，MQ 事件驱动一定要配合幂等设计。最常见方式是使用 `eventId` 作为唯一业务键，消费前检查是否已处理。

## 事件驱动和观察者模式的关系

事件驱动模式和观察者模式非常接近。观察者模式更偏对象级设计模式，事件驱动更偏架构级或工程级实践。

| 对比项     | 观察者模式         | 事件驱动模式                 |
| ---------- | ------------------ | ---------------------------- |
| 关注层级   | 对象之间的订阅通知 | 应用内或服务间事件流转       |
| 通信方式   | 主题通知观察者     | 发布事件，监听器或消费者处理 |
| 常见实现   | Subject + Observer | Spring Event、MQ、EventBus   |
| 是否跨进程 | 通常不跨进程       | 可以跨服务、跨进程           |
| 典型场景   | 对象状态变化通知   | 领域事件、异步任务、消息解耦 |

简单理解：

```text
观察者模式：一个对象变化，通知观察者。
事件驱动模式：业务事实发生，发布事件，订阅者自行处理。
```

在 Spring Boot 项目里，`@EventListener` 可以看作观察者模式在 Spring 事件机制中的工程化落地。

## 事件驱动和命令模式的区别

事件驱动模式和命令模式都常用于解耦，但语义不同。

| 对比项               | 事件驱动模式          | 命令模式                     |
| -------------------- | --------------------- | ---------------------------- |
| 核心语义             | 已经发生了什么        | 要求系统做什么               |
| 对象名称             | `UserRegisteredEvent` | `CreateOrderCommand`         |
| 时间含义             | 过去式                | 将来式或祈使式               |
| 发布方是否关心处理者 | 不关心                | 通常关心命令是否执行成功     |
| 典型场景             | 注册成功、订单已支付  | 创建订单、执行退款、分配工单 |

简单理解：

```text
事件：用户已经注册成功。
命令：请创建一个用户。
```

命令通常要求有明确执行结果。事件通常表示事实已经发生，订阅者各自响应。

## 事件驱动和中介者模式的区别

事件驱动模式和中介者模式都能减少对象之间直接依赖，但控制方式不同。

| 对比项   | 事件驱动模式               | 中介者模式                     |
| -------- | -------------------------- | ------------------------------ |
| 核心目的 | 通过事件解耦发布者和订阅者 | 通过中心对象协调多个对象       |
| 控制方式 | 发布者不控制监听器执行细节 | 中介者明确控制协作流程         |
| 适用关系 | 一对多广播、异步响应       | 多对象协作、有明确流程         |
| 典型场景 | 用户注册后通知多个模块     | 工单创建时协调客服、通知、审计 |
| 耦合程度 | 更松散                     | 中介者知道参与者               |

简单理解：

```text
事件驱动：我只声明发生了什么，谁关心谁处理。
中介者模式：我知道谁该做什么，并协调它们怎么做。
```

如果业务流程需要明确编排顺序，使用中介者或外观模式更合适。如果只是事件发生后的扩展响应，事件驱动更合适。

## 事件命名规范

事件命名非常重要。事件应该描述已经发生的业务事实，建议使用过去式。

推荐命名：

```text
UserRegisteredEvent
OrderPaidEvent
OrderCanceledEvent
PaymentSucceededEvent
InventoryDeductedEvent
CouponGrantedEvent
ConfigPublishedEvent
```

不推荐命名：

```text
RegisterUserEvent
PayOrderEvent
SendSmsEvent
CreateCouponEvent
DoSomethingEvent
```

原因是事件表示事实，不是命令。`OrderPaidEvent` 表示订单已经支付，`PayOrderCommand` 才表示要求系统去支付订单。

事件字段建议稳定、明确、可追踪：

```text
eventId
eventType
aggregateId
occurredTime
traceId
operatorId
payloadVersion
```

业务事件中不要放过大的对象，不建议直接塞完整实体：

```java
public record OrderPaidEvent(
        String eventId,
        String orderNo,
        Long userId,
        BigDecimal paidAmount,
        LocalDateTime paidTime
) {
}
```

不推荐：

```java
public record OrderPaidEvent(
        OrderEntity orderEntity,
        UserEntity userEntity,
        PaymentEntity paymentEntity
) {
}
```

事件对象越稳定，跨模块、跨服务演进成本越低。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行用户注册接口：

```bash
curl -X POST "http://localhost:8080/event-driven/user/register?username=Ateng&mobile=13800138000&channel=APP"
```

如果使用本地 Spring Event，可以观察注册服务和多个监听器日志：

```text
用户注册成功，用户ID：2020123456789017600，用户名：Ateng，手机号：13800138000，渠道：APP
发布用户注册事件成功，事件ID：EVT2020123456789017601，用户ID：2020123456789017600
发送欢迎通知成功，事件ID：EVT2020123456789017601，用户ID：2020123456789017600，手机号：13800138000，内容：欢迎 Ateng 注册成功
发放新人优惠券成功，事件ID：EVT2020123456789017601，用户ID：2020123456789017600，优惠券编号：COUPON2020123456789017602
记录用户注册审计成功，事件ID：EVT2020123456789017601，用户ID：2020123456789017600，渠道：APP，事件时间：2026-05-01T10:20:30
```

如果启用了异步监听器，可以观察线程名：

```text
开始异步同步用户画像，事件ID：EVT2020123456789017601，用户ID：2020123456789017600，线程：event-task-1
异步同步用户画像成功，事件ID：EVT2020123456789017601，用户ID：2020123456789017600
```

如果启用了 RabbitMQ 配置，需要先启动 RabbitMQ：

```bash
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

这里 `5672` 是应用连接 RabbitMQ 的端口，`15672` 是 RabbitMQ 管理后台端口。默认账号密码是 `guest/guest`，只适合本地开发环境。

启动应用后再次调用注册接口，如果 MQ 正常，可以看到发送和消费日志：

```text
发送用户注册MQ消息成功，事件ID：EVT2020123456789017601，用户ID：2020123456789017600
消费用户注册MQ消息成功，事件ID：EVT2020123456789017601，用户ID：2020123456789017600，用户名：Ateng，渠道：APP
```

## 注意事项

事件驱动模式适合解耦扩展动作，但不适合把所有业务流程都拆成事件。拆得太碎会导致调用链难追踪、故障定位困难、最终一致性成本升高。

适合使用事件驱动模式的场景：

```text
用户注册后发送通知
订单支付后发放积分
订单完成后更新统计
配置发布后刷新缓存
文件上传后异步解析
商品变更后同步搜索索引
业务操作后记录审计日志
跨服务异步解耦
```

不太适合使用事件驱动模式的场景：

```text
强一致核心链路
必须立即返回处理结果
调用顺序严格且复杂
失败后必须同步回滚
业务链路已经很难追踪
只是为了减少一行 Service 调用
```

本地 Spring Event 默认同步执行。监听器异常可能影响发布方主流程。因此监听器中要谨慎处理异常。

推荐监听器内部兜底：

```java
try {
    // 处理事件
} catch (Exception exception) {
    log.warn("处理事件失败，事件ID：{}，异常：{}", event.eventId(), exception.getMessage());
}
```

不要让非核心监听器异常影响核心业务。例如欢迎短信失败，不应该导致用户注册失败。是否影响主流程，需要根据业务语义判断。

事件监听器要保持单一职责。不要把多个无关动作塞进一个监听器。

不推荐：

```java
public class UserRegisteredListener {
    // 发送短信
    // 发优惠券
    // 写审计
    // 同步画像
    // 刷新缓存
}
```

推荐拆成多个监听器：

```text
WelcomeNoticeListener
NewUserCouponListener
RegisterAuditListener
AsyncUserProfileListener
```

如果使用异步事件，需要配置专用线程池，不建议使用默认线程池处理所有异步任务。事件突增时要关注：

```text
线程池大小
队列长度
拒绝策略
任务执行时间
异常日志
应用关闭等待时间
```

如果使用 MQ，消费者必须做幂等。因为消息可能重复投递。

常见幂等方式：

```text
eventId 唯一索引
业务单号唯一索引
消费记录表
Redis SETNX
状态条件更新
去重表
```

对于核心业务事件，建议使用可靠事件表。典型流程如下：

```text
业务事务中保存业务数据
业务事务中保存事件记录
事务提交后异步扫描事件表
投递 MQ
投递成功后标记事件状态
投递失败则重试
```

这种方式比直接在事务中发 MQ 更可靠，可以避免“数据库提交成功但 MQ 发送失败”的不一致问题。

事件内容要考虑版本兼容。跨服务事件不要随意删除字段或修改字段语义。

推荐增加版本字段：

```text
payloadVersion = 1
```

当事件结构变更时，可以兼容旧版本消费者，避免服务升级时出现消息解析失败。

## 总结

在 JDK21 和 Spring Boot 3 项目中，事件驱动模式的实践重点是用事件表达已经发生的业务事实，让订阅者围绕事件进行扩展处理，从而降低发布方和处理方的直接耦合。

Spring 本地事件适合应用内模块解耦，`@Async` 适合非核心逻辑异步处理，`@TransactionalEventListener` 适合事务提交后再触发副作用，MQ 适合跨服务、削峰填谷和最终一致性场景。对于用户注册、订单支付、配置发布、缓存刷新、审计记录、搜索同步等场景，事件驱动模式可以显著提升扩展性。

事件驱动模式不是简单的“把 Service 调用换成事件”。它最适合处理“业务事实已经发生，后续有多个扩展动作，并且发布方不应该关心谁来处理”的场景。实际落地时，需要重点关注事务边界、异常隔离、异步线程池、消息幂等、可靠投递、事件版本和链路追踪。
