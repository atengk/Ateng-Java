# Spring Boot 事件机制

## 事件机制概述

Spring Boot 事件机制是基于 Spring Framework 事件模型提供的一种应用内消息通知机制。它允许一个组件在完成某个业务动作后发布事件，由一个或多个监听器根据事件类型执行后续逻辑，从而降低主业务流程与扩展业务流程之间的直接依赖。Spring 通过 `ApplicationEventPublisher` 发布事件，事件会交由事件广播器分发给匹配的监听器；发布动作本身更接近一次“事件交付”，实际是同步执行还是异步执行，取决于事件广播器配置和监听器执行方式。([Home](https://docs.spring.io/spring-framework/docs/6.2.10/javadoc-api/org/springframework/context/ApplicationEventPublisher.html?utm_source=chatgpt.com))

### 事件驱动的核心思想

事件驱动的核心思想是：业务代码不直接调用后置处理逻辑，而是将“某件事已经发生”抽象成一个事件并发布出去，后续处理逻辑通过监听事件完成扩展。

例如用户注册成功后，主流程只负责完成用户数据落库、账号初始化等核心动作；发送欢迎短信、发放新人优惠券、记录操作日志、推送站内信等逻辑，可以拆分成多个事件监听器独立处理。主流程只需要发布一个“用户已注册”事件，不需要感知后续有多少个监听器，也不需要直接依赖短信、优惠券、日志或通知模块。

这种模型通常包含三个关键角色：

| 角色   | 说明                                                         |
| ------ | ------------------------------------------------------------ |
| 事件   | 描述已经发生的业务事实，例如 `UserRegisteredEvent`、`OrderCreatedEvent` |
| 发布者 | 在业务动作完成后发布事件，例如用户注册成功后发布用户注册事件 |
| 监听器 | 监听指定事件并执行扩展逻辑，例如发送短信、记录日志、通知第三方系统 |

在 Spring Boot 项目中，事件不是外部消息队列，也不是跨服务通信协议，而是应用进程内部的事件通知机制。它适合解决同一个应用内部的模块解耦问题，不适合作为分布式系统中的可靠消息最终一致性方案。对于跨服务通知、削峰填谷、失败重试、消息持久化等场景，应优先考虑 RabbitMQ、Kafka、RocketMQ 等消息中间件。

### Spring Event 的适用场景

Spring Event 适合用于“主流程已经完成，但还需要触发一批后置动作”的业务场景。它的价值不在于替代普通方法调用，而是在于让业务扩展点从主流程中拆分出去，使代码结构更清晰、职责边界更稳定。

常见适用场景包括：

| 场景             | 示例                                                       |
| ---------------- | ---------------------------------------------------------- |
| 用户行为后置处理 | 用户注册后发送欢迎消息、初始化用户配置、记录注册日志       |
| 订单业务扩展     | 订单创建后发送通知、记录订单轨迹、触发库存检查             |
| 操作审计         | 业务操作完成后异步写入操作日志或审计日志                   |
| 系统通知         | 某个业务状态变化后发送站内信、短信、邮件或 WebSocket 消息  |
| 本地模块解耦     | 订单模块发布事件，通知模块、日志模块、积分模块各自监听处理 |
| 事务提交后处理   | 业务数据提交成功后再执行通知、缓存刷新、外部回调等动作     |

如果事件处理逻辑依赖数据库事务结果，应避免在事务未提交前直接执行不可回滚的外部动作。例如订单创建事务还未提交时就发送短信或调用第三方系统，可能出现主事务回滚但外部通知已经发出的数据不一致问题。此类场景可以使用 `@TransactionalEventListener`，它默认在事务提交后处理事件，并且支持绑定到不同事务阶段。([Home](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html?utm_source=chatgpt.com))

不建议使用 Spring Event 的场景包括：

| 不适合场景       | 原因                                                       |
| ---------------- | ---------------------------------------------------------- |
| 跨服务可靠通信   | Spring Event 是应用内事件，不能保证跨进程投递              |
| 强一致主流程     | 如果后置逻辑失败必须直接导致主流程失败，普通方法调用更直观 |
| 大量耗时任务     | 应结合异步线程池，或者改用消息队列、任务调度系统           |
| 需要消息持久化   | Spring Event 默认不提供消息存储、确认、重试和死信机制      |
| 高复杂度事件编排 | 复杂流程应考虑工作流、状态机或消息中间件                   |

### 业务解耦的设计价值

在传统写法中，主业务方法容易不断堆积扩展逻辑。例如用户注册方法中同时处理用户保存、短信通知、积分初始化、优惠券发放、日志记录和第三方同步，随着业务增加，方法会变得越来越长，模块依赖也会越来越复杂。

使用事件机制后，主流程只保留核心业务动作和事件发布动作，扩展逻辑下沉到不同监听器中。这样可以带来以下设计价值：

| 设计价值     | 说明                                                     |
| ------------ | -------------------------------------------------------- |
| 降低模块耦合 | 发布者只关心事件，不关心具体有哪些监听器                 |
| 提升扩展能力 | 新增后置逻辑时新增监听器即可，通常不需要修改主流程       |
| 明确职责边界 | 主流程处理核心业务，监听器处理通知、日志、缓存、扩展动作 |
| 便于异步化   | 耗时但非核心的逻辑可以通过异步监听器处理                 |
| 便于测试     | 主流程和监听器可以分别测试，减少复杂依赖                 |
| 便于维护     | 后置逻辑按事件类型和业务职责拆分，代码结构更清晰         |

例如订单创建场景中，订单服务只负责创建订单并发布 `OrderCreatedEvent`。库存监听器可以处理库存预占，通知监听器可以处理消息推送，日志监听器可以记录订单轨迹，积分监听器可以处理营销积分。后续如果新增“订单创建后同步数据到 BI 系统”的需求，只需要新增一个监听器，不需要改动订单创建主流程。

需要注意的是，事件机制带来的解耦不是无成本的。事件过多、命名不清、事件字段设计不稳定、监听器职责过大，都会导致业务链路变得隐式，排查问题时不如直接方法调用直观。因此在项目落地时，应控制事件粒度，使用清晰的业务命名，并在关键监听器中记录必要日志，保证事件流转过程可追踪。



## Spring Event 基础模型

Spring Event 的基础模型由事件对象、事件发布器、事件监听器和事件分发机制组成。开发时通常不需要直接操作底层的 `ApplicationEventMulticaster`，只需要定义事件、发布事件、监听事件即可。Spring 的 `ApplicationEventPublisher` 负责发布事件，`ApplicationListener` 或 `@EventListener` 负责接收事件；如果发布的对象不是 `ApplicationEvent` 类型，Spring 会将其包装为 `PayloadApplicationEvent` 再进行分发。([Home](https://docs.spring.io/spring-framework/docs/6.0.0/javadoc-api/org/springframework/context/ApplicationEventPublisher.html?utm_source=chatgpt.com))

### ApplicationEvent

`ApplicationEvent` 是 Spring 提供的事件基类，用于描述应用中发生的某个事件。它继承自 Java 标准的 `EventObject`，内部包含事件源 `source` 和事件发生时间 `timestamp`。在 Spring Framework 6 中，`ApplicationEvent` 仍然是抽象类，业务事件可以继承它，也可以直接使用普通对象作为事件负载。([Home](https://docs.spring.io/spring/docs/6.2.10/javadoc-api/org/springframework/context/ApplicationEvent.html?utm_source=chatgpt.com))

在早期 Spring 项目中，业务事件通常会继承 `ApplicationEvent`。这种方式的优点是语义明确，能够直接表达“这是一个 Spring 应用事件”；缺点是事件对象会与 Spring 框架类型产生耦合。

`UserRegisterApplicationEvent` 用于描述“用户注册成功”这一业务事件。

文件位置：`src/main/java/io/github/atengk/event/model/UserRegisterApplicationEvent.java`

```java
package io.github.atengk.event.model;

import org.springframework.context.ApplicationEvent;

/**
 * 用户注册事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class UserRegisterApplicationEvent extends ApplicationEvent {

    private final Long userId;

    private final String username;

    /**
     * 创建用户注册事件
     *
     * @param source   事件源
     * @param userId   用户ID
     * @param username 用户名
     */
    public UserRegisterApplicationEvent(Object source, Long userId, String username) {
        super(source);
        this.userId = userId;
        this.username = username;
    }

    /**
     * 获取用户ID
     *
     * @return 用户ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 获取用户名
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }
}
```

在 Spring 4.2 以后，也可以直接发布任意普通对象作为事件。Spring 会在内部将普通对象包装为 `PayloadApplicationEvent`。在 Spring Boot 3 项目中，推荐优先使用普通 Java 对象、`record` 或 DTO 作为事件载体，这样事件模型更轻量，也更利于单元测试。([Home](https://docs.spring.io/spring-framework/docs/6.0.0/javadoc-api/org/springframework/context/ApplicationEventPublisher.html?utm_source=chatgpt.com))

`UserRegisteredEvent` 使用 `record` 定义普通事件对象，适合 Spring Boot 3 和 Java 17 以上项目。

文件位置：`src/main/java/io/github/atengk/event/model/UserRegisteredEvent.java`

```java
package io.github.atengk.event.model;

import java.time.LocalDateTime;

/**
 * 用户注册完成事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record UserRegisteredEvent(
        Long userId,
        String username,
        String mobile,
        LocalDateTime registeredTime
) {
}
```

实际业务开发中建议优先使用普通对象事件，只有在需要兼容旧代码、使用框架事件体系扩展能力，或者团队已有统一规范时，才继承 `ApplicationEvent`。

### ApplicationEventPublisher

`ApplicationEventPublisher` 是 Spring 提供的事件发布接口，用于将事件交给 Spring 容器进行分发。`ApplicationContext` 本身就是 `ApplicationEventPublisher` 的子接口，因此在 Spring Bean 中可以直接注入 `ApplicationEventPublisher` 来发布事件。官方文档也明确说明，`publishEvent` 的语义是将事件交给事件广播器处理，并不直接承诺一定是同步、异步或立即执行，具体执行方式取决于事件广播器和监听器配置。([Home](https://docs.spring.io/spring-framework/docs/6.2.10/javadoc-api/org/springframework/context/ApplicationEventPublisher.html?utm_source=chatgpt.com))

常用发布方式有两种：发布 `ApplicationEvent` 子类对象，或者发布普通对象事件。对于业务代码来说，推荐把事件发布动作封装到 Service 或专门的事件发布组件中，避免在 Controller 中直接发布事件。

`UserService` 在用户注册完成后发布用户注册事件。

文件位置：`src/main/java/io/github/atengk/event/service/UserService.java`

```java
package io.github.atengk.event.service;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.event.model.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户业务服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 注册用户
     *
     * @param username 用户名
     * @param mobile   手机号
     * @return 用户ID
     */
    public Long register(String username, String mobile) {
        // 示例中使用 Hutool 生成用户ID，真实项目通常由数据库或雪花算法服务生成
        Long userId = IdUtil.getSnowflakeNextId();

        log.info("用户注册成功，用户ID：{}，用户名：{}", userId, username);

        UserRegisteredEvent event = new UserRegisteredEvent(
                userId,
                username,
                mobile,
                LocalDateTime.now()
        );

        applicationEventPublisher.publishEvent(event);
        log.info("用户注册事件已发布，用户ID：{}", userId);

        return userId;
    }
}
```

事件发布时应注意两个原则。第一，事件对象中应包含监听器处理所需的关键业务数据，不要让监听器再依赖大量上下文变量。第二，事件发布的位置应放在核心业务动作完成之后，例如用户保存成功、订单创建成功、状态变更成功之后，避免发布一个与真实业务状态不一致的事件。

### ApplicationListener

`ApplicationListener` 是 Spring 提供的函数式监听器接口，用于监听指定类型的 `ApplicationEvent`。它通过泛型声明自己关心的事件类型，当 Spring 容器发布事件时，只会将匹配的事件对象交给对应监听器处理。`ApplicationListener` 的核心方法是 `onApplicationEvent(E event)`。([Home](https://docs.spring.io/spring/docs/6.2.10/javadoc-api/org/springframework/context/ApplicationListener.html?utm_source=chatgpt.com))

这种方式适合对监听器类型要求明确、需要实现接口规范、或者需要在框架层扩展监听行为的场景。缺点是只能直接监听 `ApplicationEvent` 类型，如果业务事件使用普通对象，通常使用 `@EventListener` 更方便。

`UserRegisterApplicationListener` 监听继承自 `ApplicationEvent` 的用户注册事件。

文件位置：`src/main/java/io/github/atengk/event/listener/UserRegisterApplicationListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.UserRegisterApplicationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 用户注册事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UserRegisterApplicationListener implements ApplicationListener<UserRegisterApplicationEvent> {

    /**
     * 处理用户注册事件
     *
     * @param event 用户注册事件
     */
    @Override
    public void onApplicationEvent(UserRegisterApplicationEvent event) {
        if (ObjectUtil.isNull(event.getUserId())) {
            log.warn("用户注册事件缺少用户ID，跳过处理");
            return;
        }

        log.info("监听到用户注册事件，用户ID：{}，用户名：{}", event.getUserId(), event.getUsername());

        // 这里可以执行发送欢迎短信、记录日志、初始化用户配置等后置逻辑
    }
}
```

`ApplicationListener` 更偏向传统接口式监听。对于业务开发，尤其是一个类中需要监听多个事件，或者希望监听普通对象事件时，`@EventListener` 通常更简洁。

### @EventListener

`@EventListener` 是 Spring 提供的注解式事件监听方式，可以直接标记在 Spring Bean 的方法上。监听方法可以通过方法参数推断事件类型，也可以通过 `classes` 或 `value` 指定监听的事件类型。官方文档说明，`@EventListener` 可以监听 `ApplicationEvent` 实例，也可以监听任意普通对象事件，并且支持通过 `condition` 使用 SpEL 条件表达式过滤事件。([Home](https://docs.spring.io/spring-framework/docs/6.1.21/javadoc-api/org/springframework/context/event/EventListener.html?utm_source=chatgpt.com))

在业务开发中，`@EventListener` 是最常用的监听方式。它代码更轻量，不需要实现接口，适合将不同业务事件的处理方法组织在同一个监听器类中。

`UserEventListener` 使用 `@EventListener` 监听普通对象事件。

文件位置：`src/main/java/io/github/atengk/event/listener/UserEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 用户事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UserEventListener {

    /**
     * 发送欢迎通知
     *
     * @param event 用户注册完成事件
     */
    @Order(1)
    @EventListener
    public void sendWelcomeMessage(UserRegisteredEvent event) {
        if (ObjectUtil.hasEmpty(event.userId(), event.mobile())) {
            log.warn("用户注册事件参数不完整，跳过欢迎通知，事件内容：{}", event);
            return;
        }

        log.info("发送用户欢迎通知，用户ID：{}，手机号：{}", event.userId(), event.mobile());

        // 这里可以调用短信、邮件、站内信等通知服务
    }

    /**
     * 初始化用户配置
     *
     * @param event 用户注册完成事件
     */
    @Order(2)
    @EventListener
    public void initUserConfig(UserRegisteredEvent event) {
        if (ObjectUtil.isNull(event.userId())) {
            log.warn("用户注册事件缺少用户ID，跳过用户配置初始化");
            return;
        }

        log.info("初始化用户配置，用户ID：{}", event.userId());

        // 这里可以初始化用户偏好设置、默认权限、默认资料等
    }
}
```

如果监听器只处理特定条件下的事件，可以使用 `condition` 进行过滤。下面示例只处理手机号不为空的用户注册事件。

```java
@EventListener(condition = "#event.mobile() != null && #event.mobile() != ''")
public void handleMobileUserRegistered(UserRegisteredEvent event) {
    log.info("处理带手机号的用户注册事件，用户ID：{}，手机号：{}", event.userId(), event.mobile());
}
```

`@Order` 可以控制同一事件下多个监听器的执行顺序。数值越小，优先级越高。需要注意的是，顺序只表示监听器调用顺序，不代表业务强依赖关系。如果监听器之间存在强依赖，应考虑把强依赖逻辑放回主流程，或者设计更明确的业务编排方式。

## 同步事件机制

同步事件机制是 Spring Event 的默认使用方式。在未配置异步事件广播器或未使用 `@Async` 的情况下，事件发布后，匹配的监听器通常会在发布事件的当前线程中执行。也就是说，监听器逻辑会影响发布方法的执行耗时，监听器抛出的运行时异常也可能向上传递并影响主流程。

### 同步事件执行流程

同步事件的执行过程可以理解为一次应用内的观察者模式调用。业务方法发布事件后，Spring 将事件交给事件广播器，事件广播器查找所有匹配的监听器，然后按顺序调用监听器方法。

典型执行流程如下：

```text
业务方法开始
  ↓
执行核心业务逻辑
  ↓
调用 ApplicationEventPublisher.publishEvent(event)
  ↓
Spring 事件广播器查找匹配的监听器
  ↓
按顺序执行监听器
  ↓
监听器全部执行完成
  ↓
publishEvent 方法返回
  ↓
业务方法继续执行或结束
```

在同步模式下，事件监听器与事件发布者处于同一个调用链。监听器执行时间会叠加到主流程耗时中，监听器异常也可能中断主流程。因此，同步监听器更适合处理轻量、快速、与主流程关系较强的后置逻辑，例如本地缓存刷新、内存状态同步、简单业务校验、必要的审计记录等。

不建议在同步监听器中执行长耗时操作，例如远程 HTTP 调用、大批量数据处理、复杂报表生成、邮件短信批量发送等。这些逻辑会拉长接口响应时间，也会增加主事务持有时间。

### 事件发布方式

事件发布通常放在 Service 层，而不是 Controller 层。Controller 负责接收请求和返回响应，Service 负责业务动作和事件发布。这样可以保证事件与业务状态变化绑定，而不是与某个接口入口绑定。

常见发布方式有三种。

第一种是注入 `ApplicationEventPublisher` 发布普通对象事件，这是 Spring Boot 3 项目中最常用的方式。

```java
applicationEventPublisher.publishEvent(new UserRegisteredEvent(
        userId,
        username,
        mobile,
        LocalDateTime.now()
));
```

第二种是发布继承 `ApplicationEvent` 的事件对象，适合已有项目或需要显式使用 Spring 事件基类的场景。

```java
applicationEventPublisher.publishEvent(new UserRegisterApplicationEvent(
        this,
        userId,
        username
));
```

第三种是封装统一事件发布组件，适合项目中事件较多、需要统一日志、异常保护或事件审计的场景。

`BusinessEventPublisher` 对事件发布进行统一封装，便于后续扩展日志、埋点或事件审计。

文件位置：`src/main/java/io/github/atengk/event/publisher/BusinessEventPublisher.java`

```java
package io.github.atengk.event.publisher;

import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 业务事件发布器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 发布业务事件
     *
     * @param event 业务事件
     */
    public void publish(Object event) {
        if (ObjectUtil.isNull(event)) {
            log.warn("业务事件为空，跳过发布");
            return;
        }

        applicationEventPublisher.publishEvent(event);
        log.info("业务事件发布完成，事件类型：{}", event.getClass().getName());
    }
}
```

业务服务中可以通过统一发布器发布事件。

```java
businessEventPublisher.publish(new UserRegisteredEvent(
        userId,
        username,
        mobile,
        LocalDateTime.now()
));
```

统一封装的好处是发布入口稳定。后续如果需要增加事件日志、事件开关、链路追踪、异常统计，不需要在每个业务 Service 中重复修改。

### 事件监听方式

同步事件监听主要有两种方式：实现 `ApplicationListener` 接口，或者使用 `@EventListener` 注解。实际业务开发中推荐优先使用 `@EventListener`，因为它支持普通对象事件，代码更简洁，也更容易在一个类中组织多个事件处理方法。

接口式监听适合框架扩展或监听 Spring 内置事件。

```java
@Component
public class CustomApplicationListener implements ApplicationListener<UserRegisterApplicationEvent> {

    @Override
    public void onApplicationEvent(UserRegisterApplicationEvent event) {
        // 处理事件
    }
}
```

注解式监听适合业务事件处理。

```java
@Component
public class CustomEventListener {

    @EventListener
    public void handle(UserRegisteredEvent event) {
        // 处理事件
    }
}
```

如果多个监听器处理同一个事件，可以使用 `@Order` 控制执行顺序。但需要注意，事件监听器本质上应保持职责独立，不建议让监听器 A 的执行结果成为监听器 B 的强依赖。否则事件机制会从“解耦工具”变成“隐式调用链”，后续维护和排查会变得困难。

监听器设计时建议遵循以下原则：

| 原则       | 说明                                             |
| ---------- | ------------------------------------------------ |
| 单一职责   | 一个监听器方法只处理一个明确的后置动作           |
| 参数完整   | 事件对象中包含监听器必要参数，避免过度反查数据库 |
| 日志明确   | 监听器开始、关键分支、失败场景应记录日志         |
| 异常可控   | 非核心监听器异常不应随意影响主流程               |
| 避免强依赖 | 多个监听器之间不应依赖执行顺序传递业务结果       |

### 同步事件的事务影响

同步事件与事务的关系需要重点关注。如果在 `@Transactional` 方法中发布同步事件，默认情况下监听器会在当前事务提交前执行，并且通常处于同一个线程调用链中。此时监听器读取数据库，可能读到当前事务内尚未提交的数据；监听器执行外部调用，则可能出现“外部调用成功，但主事务回滚”的不一致问题。

例如订单创建方法中发布订单创建事件：

```java
@Transactional(rollbackFor = Exception.class)
public Long createOrder(CreateOrderCommand command) {
    Long orderId = saveOrder(command);

    applicationEventPublisher.publishEvent(new OrderCreatedEvent(orderId));

    return orderId;
}
```

如果 `OrderCreatedEvent` 的监听器中发送短信、调用第三方系统或写入无法随主事务回滚的外部资源，就可能出现以下问题：

| 问题         | 说明                                         |
| ------------ | -------------------------------------------- |
| 主事务未提交 | 监听器执行时订单数据还没有真正提交           |
| 主事务回滚   | 外部通知已经发出，但订单创建最终失败         |
| 监听器异常   | 同步监听器异常可能导致主业务事务回滚         |
| 事务时间变长 | 监听器耗时会延长事务持有时间，增加锁竞争风险 |

因此，同步事件在事务方法中使用时，应明确监听器逻辑是否允许参与主事务。

如果监听器逻辑是主业务的一部分，失败时应让主流程失败，可以使用同步事件，但更推荐直接方法调用，因为直接调用更清晰。

如果监听器逻辑是后置扩展，例如短信、通知、日志、第三方回调，则不建议在事务提交前执行。此类场景应使用 `@TransactionalEventListener`，让监听器绑定到事务阶段。Spring 官方文档说明，`@TransactionalEventListener` 默认绑定到事务提交阶段，即 `AFTER_COMMIT`，适合在当前事务成功提交后再处理事件。([Home](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html?utm_source=chatgpt.com))

示例：

```java
@TransactionalEventListener
public void handleOrderCreatedAfterCommit(OrderCreatedEvent event) {
    log.info("事务提交后处理订单创建事件，订单ID：{}", event.orderId());

    // 适合执行通知、回调、缓存刷新等依赖事务提交结果的逻辑
}
```

同步事件不是不能放在事务中，而是必须明确事件监听器执行时机和失败影响。开发时可以按以下规则判断：

| 场景                                   | 推荐方式                               |
| -------------------------------------- | -------------------------------------- |
| 监听器失败必须导致主流程失败           | 直接方法调用或同步事件                 |
| 监听器只是扩展动作，失败不应影响主流程 | 异步事件或异常保护                     |
| 监听器必须在事务提交后执行             | `@TransactionalEventListener`          |
| 监听器包含外部系统调用                 | 优先事务提交后执行，必要时使用消息队列 |
| 监听器执行时间较长                     | 异步监听或消息队列                     |

在项目规范中，凡是涉及通知、消息、第三方接口、缓存刷新、搜索索引、统计分析等非主流程动作，都应优先考虑事务提交后处理，避免事务回滚与外部副作用不一致。



## 异步事件机制

异步事件机制用于将事件监听器的执行从主业务线程中拆分出去。主流程发布事件后，不再等待监听器全部执行完成，而是将监听器逻辑交给线程池处理，从而降低接口响应耗时，避免非核心后置逻辑阻塞主业务流程。

在 Spring Boot 3 中，异步事件常见实现方式有两种：一种是在监听器方法上使用 `@Async`，另一种是自定义事件广播器使用异步线程池。业务开发中更常用 `@Async + @EventListener`，因为配置简单，代码侵入小，适合大多数应用内事件异步处理场景。

### 异步事件的使用场景

异步事件适合处理不影响主流程结果、允许稍后完成、失败后可以补偿或记录的业务逻辑。它通常用于“主流程已经成功，后续动作可以延迟处理”的场景。

常见适用场景如下：

| 场景         | 示例                                | 是否推荐异步                   |
| ------------ | ----------------------------------- | ------------------------------ |
| 消息通知     | 用户注册后发送短信、邮件、站内信    | 推荐                           |
| 操作日志     | 记录用户操作日志、业务审计日志      | 推荐                           |
| 第三方回调   | 订单创建后通知外部系统              | 推荐，但建议增加重试或消息队列 |
| 缓存刷新     | 状态变更后刷新本地缓存或 Redis 缓存 | 视一致性要求决定               |
| 统计分析     | 记录访问行为、更新统计指标          | 推荐                           |
| 搜索索引     | 数据变更后更新 Elasticsearch 索引   | 推荐，但要考虑失败补偿         |
| 核心业务校验 | 库存校验、余额扣减、权限校验        | 不推荐                         |

异步事件的核心判断标准是：监听器执行失败是否应该影响主流程。如果失败不能影响主流程，例如短信发送失败不应该导致用户注册失败，就适合异步。如果失败必须让主业务失败，例如订单金额校验失败必须阻止下单，则不应该异步处理。

异步事件也不是消息队列的替代品。它没有消息持久化、消费确认、死信队列、跨服务投递等能力。如果业务要求高可靠投递、失败自动重试、跨服务消费，应优先使用 RabbitMQ、Kafka、RocketMQ 等消息中间件。

### @Async 与事件监听结合

`@Async` 可以让 Spring Bean 中的方法通过异步线程池执行。将 `@Async` 与 `@EventListener` 结合后，事件监听器会在异步线程中执行，事件发布方法不会等待监听器业务逻辑执行完成。

使用 `@Async` 前，需要先开启异步能力。

`AsyncConfig` 用于开启异步执行能力，并声明事件监听器使用的线程池。

文件位置：`src/main/java/io/github/atengk/event/config/AsyncConfig.java`

```java
package io.github.atengk.event.config;

import cn.hutool.core.thread.ThreadFactoryBuilder;
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
 * @since 2026-05-13
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 事件异步线程池
     *
     * @return 线程池执行器
     */
    @Bean("eventTaskExecutor")
    public ThreadPoolTaskExecutor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：常驻线程数量
        executor.setCorePoolSize(4);

        // 最大线程数：高峰期可扩展线程数量
        executor.setMaxPoolSize(8);

        // 队列容量：超过核心线程后进入队列等待
        executor.setQueueCapacity(200);

        // 空闲线程存活时间：超过核心线程的空闲线程回收时间
        executor.setKeepAliveSeconds(60);

        // 线程名前缀：便于日志排查
        executor.setThreadNamePrefix("event-task-");

        // 使用 Hutool 构建线程工厂，统一线程命名
        executor.setThreadFactory(ThreadFactoryBuilder.create()
                .setNamePrefix("event-task-")
                .build());

        // 拒绝策略：由调用线程执行，避免任务直接丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待队列任务执行完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    /**
     * 默认异步执行器
     *
     * @return 线程池执行器
     */
    @Override
    public Executor getAsyncExecutor() {
        return eventTaskExecutor();
    }

    /**
     * 异步异常处理器
     *
     * @return 异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return this::handleAsyncException;
    }

    /**
     * 处理异步方法异常
     *
     * @param throwable 异常信息
     * @param method    异步方法
     * @param objects   方法参数
     */
    private void handleAsyncException(Throwable throwable, Method method, Object... objects) {
        log.error("异步事件执行异常，方法：{}，参数：{}", method.getName(), objects, throwable);
    }
}
```

定义一个用户注册事件，作为异步监听器处理的事件对象。

文件位置：`src/main/java/io/github/atengk/event/model/UserRegisteredEvent.java`

```java
package io.github.atengk.event.model;

import java.time.LocalDateTime;

/**
 * 用户注册完成事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record UserRegisteredEvent(
        Long userId,
        String username,
        String mobile,
        LocalDateTime registeredTime
) {
}
```

异步监听器使用 `@Async("eventTaskExecutor")` 指定线程池，并使用 `@EventListener` 监听业务事件。

文件位置：`src/main/java/io/github/atengk/event/listener/UserAsyncEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户异步事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UserAsyncEventListener {

    /**
     * 异步发送欢迎短信
     *
     * @param event 用户注册完成事件
     */
    @Async("eventTaskExecutor")
    @EventListener
    public void sendWelcomeMessage(UserRegisteredEvent event) {
        if (ObjectUtil.hasEmpty(event.userId(), event.mobile())) {
            log.warn("用户注册事件参数不完整，跳过欢迎短信发送，事件内容：{}", event);
            return;
        }

        log.info("开始异步发送欢迎短信，用户ID：{}，手机号：{}", event.userId(), event.mobile());

        // 示例：这里可以调用短信服务、邮件服务或站内信服务
        // smsService.sendWelcomeMessage(event.mobile());

        log.info("欢迎短信发送完成，用户ID：{}", event.userId());
    }

    /**
     * 异步初始化用户扩展信息
     *
     * @param event 用户注册完成事件
     */
    @Async("eventTaskExecutor")
    @EventListener
    public void initUserProfile(UserRegisteredEvent event) {
        if (ObjectUtil.isNull(event.userId())) {
            log.warn("用户注册事件缺少用户ID，跳过用户扩展信息初始化");
            return;
        }

        log.info("开始异步初始化用户扩展信息，用户ID：{}", event.userId());

        // 示例：这里可以初始化用户偏好、默认配置、扩展资料等

        log.info("用户扩展信息初始化完成，用户ID：{}", event.userId());
    }
}
```

业务服务中仍然只负责发布事件，不需要关心监听器是同步执行还是异步执行。

文件位置：`src/main/java/io/github/atengk/event/service/UserService.java`

```java
package io.github.atengk.event.service;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.event.model.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户业务服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 注册用户
     *
     * @param username 用户名
     * @param mobile   手机号
     * @return 用户ID
     */
    public Long register(String username, String mobile) {
        Long userId = IdUtil.getSnowflakeNextId();

        log.info("用户注册成功，用户ID：{}，用户名：{}", userId, username);

        applicationEventPublisher.publishEvent(new UserRegisteredEvent(
                userId,
                username,
                mobile,
                LocalDateTime.now()
        ));

        log.info("用户注册事件发布完成，用户ID：{}", userId);
        return userId;
    }
}
```

使用 `@Async` 时需要注意几个细节。第一，监听器必须是 Spring 容器管理的 Bean。第二，异步方法不应通过同一个类内部的 `this` 调用触发，否则可能绕过 Spring 代理。第三，异步监听器执行失败不会像同步监听器一样直接抛回事件发布方法，因此必须做好日志、异常处理和补偿策略。

### 线程池配置

异步事件必须使用明确的业务线程池，不建议直接使用默认线程池。默认线程池参数不可控，业务增长后容易出现线程膨胀、任务堆积、排查困难等问题。事件线程池应根据业务类型、接口耗时、机器规格和任务量单独配置。

推荐为事件机制声明独立线程池，例如 `eventTaskExecutor`。如果项目中还有导出任务、报表任务、定时任务、消息消费任务，应分别设置不同线程池，避免互相影响。

线程池核心参数说明如下：

| 参数                       | 说明             | 建议                       |
| -------------------------- | ---------------- | -------------------------- |
| `corePoolSize`             | 核心线程数       | 根据常态事件处理量设置     |
| `maxPoolSize`              | 最大线程数       | 根据峰值流量和机器资源设置 |
| `queueCapacity`            | 任务队列容量     | 不能无限大，避免内存风险   |
| `keepAliveSeconds`         | 空闲线程存活时间 | 适合释放峰值线程           |
| `threadNamePrefix`         | 线程名前缀       | 必须设置，便于日志排查     |
| `RejectedExecutionHandler` | 拒绝策略         | 根据业务可接受程度选择     |

常见拒绝策略选择如下：

| 拒绝策略              | 行为               | 适用场景                         |
| --------------------- | ------------------ | -------------------------------- |
| `AbortPolicy`         | 直接抛出异常       | 需要快速暴露问题                 |
| `CallerRunsPolicy`    | 由调用线程执行     | 不希望任务丢失，但可能拖慢主流程 |
| `DiscardPolicy`       | 直接丢弃任务       | 一般不推荐                       |
| `DiscardOldestPolicy` | 丢弃队列中最老任务 | 一般不推荐                       |

对于业务事件监听器，通常优先选择 `CallerRunsPolicy` 或自定义拒绝策略。`CallerRunsPolicy` 的优点是不直接丢任务，缺点是线程池打满时会退化为调用线程执行，可能影响接口响应。因此高并发核心系统中，如果事件非常重要，应考虑使用消息队列承接异步任务。

如果希望线程池参数可配置，可以通过 `application.yml` 管理。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    # 应用名称，便于日志和监控识别
    name: spring-event-demo

event:
  executor:
    # 核心线程数
    core-pool-size: 4
    # 最大线程数
    max-pool-size: 8
    # 队列容量
    queue-capacity: 200
    # 线程空闲时间，单位：秒
    keep-alive-seconds: 60
    # 关闭线程池时等待任务完成时间，单位：秒
    await-termination-seconds: 30
```

对应的配置属性类如下。

文件位置：`src/main/java/io/github/atengk/event/config/EventExecutorProperties.java`

```java
package io.github.atengk.event.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 事件线程池配置属性
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Component
@ConfigurationProperties(prefix = "event.executor")
public class EventExecutorProperties {

    /**
     * 核心线程数
     */
    private int corePoolSize = 4;

    /**
     * 最大线程数
     */
    private int maxPoolSize = 8;

    /**
     * 队列容量
     */
    private int queueCapacity = 200;

    /**
     * 线程空闲时间，单位：秒
     */
    private int keepAliveSeconds = 60;

    /**
     * 关闭等待时间，单位：秒
     */
    private int awaitTerminationSeconds = 30;
}
```

使用配置属性创建线程池。

文件位置：`src/main/java/io/github/atengk/event/config/EventExecutorConfig.java`

```java
package io.github.atengk.event.config;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 事件线程池配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Configuration
@RequiredArgsConstructor
public class EventExecutorConfig {

    private final EventExecutorProperties eventExecutorProperties;

    /**
     * 创建事件线程池
     *
     * @return 事件线程池
     */
    @Bean("eventTaskExecutor")
    public ThreadPoolTaskExecutor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(eventExecutorProperties.getCorePoolSize());
        executor.setMaxPoolSize(eventExecutorProperties.getMaxPoolSize());
        executor.setQueueCapacity(eventExecutorProperties.getQueueCapacity());
        executor.setKeepAliveSeconds(eventExecutorProperties.getKeepAliveSeconds());
        executor.setThreadNamePrefix("event-task-");
        executor.setThreadFactory(ThreadFactoryBuilder.create()
                .setNamePrefix("event-task-")
                .build());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(eventExecutorProperties.getAwaitTerminationSeconds());

        executor.initialize();
        return executor;
    }
}
```

线程池配置应结合业务压测结果调整。对于 IO 密集型任务，例如短信、邮件、HTTP 回调，可以适当增加线程数；对于 CPU 密集型任务，例如复杂计算、报表生成，则线程数不宜过高，避免上下文切换过多。

### 异步事件异常处理

异步事件的异常处理比同步事件更重要。同步监听器抛出异常时，异常通常会沿调用链传回发布方；异步监听器在线程池中执行，发布方已经返回，异常不会直接影响发布方法。因此异步监听器必须主动记录异常，并根据业务重要程度设计补偿机制。

常见处理方式有三种。

第一种是在监听器内部使用 `try-catch` 捕获异常。这种方式最直接，适合在异常发生时记录事件数据、告警或写入失败表。

```java
@Async("eventTaskExecutor")
@EventListener
public void sendWelcomeMessage(UserRegisteredEvent event) {
    try {
        log.info("开始发送欢迎短信，用户ID：{}", event.userId());

        // smsService.sendWelcomeMessage(event.mobile());

        log.info("欢迎短信发送成功，用户ID：{}", event.userId());
    } catch (Exception e) {
        log.error("欢迎短信发送失败，用户ID：{}，手机号：{}", event.userId(), event.mobile(), e);

        // 这里可以写入失败记录表，后续由定时任务补偿
        // eventFailRecordService.save(event, e);
    }
}
```

第二种是实现 `AsyncConfigurer#getAsyncUncaughtExceptionHandler`，统一处理 `void` 异步方法抛出的未捕获异常。前面的 `AsyncConfig` 已经给出示例。需要注意的是，这种方式只能处理异步方法未捕获异常，不能替代业务级补偿。

第三种是让异步方法返回 `CompletableFuture`，由调用方或框架逻辑处理完成状态。但在事件监听场景中，监听器返回值通常不会被发布方直接消费，因此更推荐在监听器内部处理异常，并把失败事件落库或发送到可靠消息队列。

对于重要异步事件，建议建立失败记录模型。

| 字段          | 说明          |
| ------------- | ------------- |
| `eventId`     | 事件唯一标识  |
| `eventType`   | 事件类型      |
| `eventBody`   | 事件内容 JSON |
| `failReason`  | 失败原因      |
| `retryCount`  | 重试次数      |
| `status`      | 处理状态      |
| `createdTime` | 创建时间      |
| `updatedTime` | 更新时间      |

对于只是记录日志、发送非关键通知的事件，可以只记录错误日志和告警。对于涉及第三方回调、搜索索引、缓存一致性等重要事件，应配合失败表、定时重试、消息队列或人工补偿机制。

## 业务解耦设计

业务解耦设计的目标不是简单地把代码从一个方法拆到多个监听器中，而是重新划分主流程和扩展流程的职责边界。事件机制适合表达“某个业务事实已经发生”，监听器根据这个事实执行各自独立的后置动作。

良好的事件设计应具备三个特征：事件命名能表达业务事实，事件数据能支撑监听器处理，监听器之间没有强耦合依赖。

### 主流程与扩展流程拆分

主流程是完成业务目标所必须执行的逻辑，扩展流程是在主流程完成后可以独立执行的后置逻辑。拆分事件前，必须先判断哪些逻辑属于核心业务，哪些逻辑属于扩展业务。

以用户注册为例，核心主流程包括参数校验、账号唯一性校验、用户数据保存、默认状态初始化等。扩展流程包括发送欢迎短信、记录注册日志、发放新人优惠券、初始化用户偏好、同步第三方 CRM 等。

拆分前的代码通常如下：

```java
@Transactional(rollbackFor = Exception.class)
public Long register(String username, String mobile) {
    Long userId = saveUser(username, mobile);

    sendWelcomeMessage(mobile);
    saveRegisterLog(userId);
    grantNewUserCoupon(userId);
    initUserPreference(userId);

    return userId;
}
```

这种写法的问题是注册方法承担了过多职责。后续新增一个注册后动作，就需要修改注册主流程，主流程会越来越臃肿。

拆分后的代码应让主流程只负责核心业务和事件发布。

```java
@Transactional(rollbackFor = Exception.class)
public Long register(String username, String mobile) {
    Long userId = saveUser(username, mobile);

    applicationEventPublisher.publishEvent(new UserRegisteredEvent(
            userId,
            username,
            mobile,
            LocalDateTime.now()
    ));

    return userId;
}
```

后置扩展动作由监听器分别处理：

| 监听器                        | 职责                 |
| ----------------------------- | -------------------- |
| `UserMessageEventListener`    | 发送欢迎短信、站内信 |
| `UserLogEventListener`        | 记录用户注册日志     |
| `UserCouponEventListener`     | 发放新人优惠券       |
| `UserPreferenceEventListener` | 初始化用户偏好配置   |

这种拆分方式的价值在于，新增注册后置动作时，通常只需要新增监听器，不需要修改注册主流程。

### 事件对象设计

事件对象是发布者和监听器之间的契约。事件对象设计不清晰，会导致监听器频繁反查数据库、字段语义不明确、后续扩展困难。因此事件对象应围绕业务事实设计，而不是围绕某个监听器的临时参数设计。

事件对象命名应使用过去式或完成态，表达“已经发生”的事实。例如：

| 推荐命名                   | 不推荐命名          | 说明             |
| -------------------------- | ------------------- | ---------------- |
| `UserRegisteredEvent`      | `RegisterUserEvent` | 用户已经注册完成 |
| `OrderCreatedEvent`        | `CreateOrderEvent`  | 订单已经创建完成 |
| `PaymentSucceededEvent`    | `PayEvent`          | 支付已经成功     |
| `ProductStockChangedEvent` | `UpdateStockEvent`  | 商品库存已经变化 |

事件对象字段应尽量稳定、明确、必要。不要把整个 Entity 直接塞进事件对象中，避免监听器依赖数据库实体结构。推荐传递业务 ID、关键状态、必要快照字段和事件发生时间。

`OrderCreatedEvent` 用于描述订单创建成功这一业务事实。

文件位置：`src/main/java/io/github/atengk/event/model/OrderCreatedEvent.java`

```java
package io.github.atengk.event.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建完成事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        String orderNo,
        BigDecimal totalAmount,
        LocalDateTime createdTime
) {
}
```

如果事件需要唯一标识，便于日志追踪、失败重试或幂等处理，可以增加 `eventId` 字段。

文件位置：`src/main/java/io/github/atengk/event/model/BusinessEvent.java`

```java
package io.github.atengk.event.model;

import java.time.LocalDateTime;

/**
 * 业务事件基础接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface BusinessEvent {

    /**
     * 获取事件ID
     *
     * @return 事件ID
     */
    String eventId();

    /**
     * 获取事件发生时间
     *
     * @return 事件发生时间
     */
    LocalDateTime occurredTime();
}
```

`UserRegisteredEvent` 实现统一业务事件接口，便于后续做事件日志和链路追踪。

文件位置：`src/main/java/io/github/atengk/event/model/UserRegisteredEvent.java`

```java
package io.github.atengk.event.model;

import java.time.LocalDateTime;

/**
 * 用户注册完成事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record UserRegisteredEvent(
        String eventId,
        Long userId,
        String username,
        String mobile,
        LocalDateTime occurredTime
) implements BusinessEvent {
}
```

发布事件时生成事件 ID。

```java
applicationEventPublisher.publishEvent(new UserRegisteredEvent(
        IdUtil.fastSimpleUUID(),
        userId,
        username,
        mobile,
        LocalDateTime.now()
));
```

事件对象设计建议如下：

| 设计项 | 建议                                            |
| ------ | ----------------------------------------------- |
| 命名   | 使用业务名词 + 完成态，例如 `OrderCreatedEvent` |
| 字段   | 只放监听器必要数据，避免塞入完整 Entity         |
| 时间   | 建议包含事件发生时间                            |
| 标识   | 重要事件建议包含 `eventId`                      |
| 可变性 | 推荐使用不可变对象，例如 `record`               |
| 语义   | 表达业务事实，不表达操作命令                    |
| 扩展   | 字段变更应谨慎，避免影响多个监听器              |

### 监听器职责边界

监听器的职责边界决定了事件机制是否真正实现解耦。一个监听器方法只应该处理一个明确的后置动作，不应把多个无关动作堆在同一个监听器中。

例如用户注册事件监听器不建议写成这样：

```java
@EventListener
public void handle(UserRegisteredEvent event) {
    sendMessage(event);
    saveLog(event);
    grantCoupon(event);
    syncCrm(event);
}
```

这种写法只是把代码从 Service 挪到了 Listener，并没有实现职责拆分。更合理的方式是将不同后置动作拆成不同监听器类或不同监听器方法。

文件结构示例：

```text
src/main/java/io/github/atengk/event/listener
├── UserMessageEventListener.java
├── UserLogEventListener.java
├── UserCouponEventListener.java
└── UserCrmSyncEventListener.java
```

`UserMessageEventListener` 只负责用户消息通知。

文件位置：`src/main/java/io/github/atengk/event/listener/UserMessageEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户消息事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UserMessageEventListener {

    /**
     * 处理用户注册后的消息通知
     *
     * @param event 用户注册完成事件
     */
    @Async("eventTaskExecutor")
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        if (ObjectUtil.hasEmpty(event.userId(), event.mobile())) {
            log.warn("用户注册消息通知参数不完整，事件ID：{}", event.eventId());
            return;
        }

        log.info("开始发送用户注册消息，事件ID：{}，用户ID：{}", event.eventId(), event.userId());

        // messageService.sendWelcomeMessage(event.mobile());

        log.info("用户注册消息发送完成，事件ID：{}，用户ID：{}", event.eventId(), event.userId());
    }
}
```

`UserLogEventListener` 只负责注册日志记录。

文件位置：`src/main/java/io/github/atengk/event/listener/UserLogEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 用户日志事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UserLogEventListener {

    /**
     * 记录用户注册日志
     *
     * @param event 用户注册完成事件
     */
    @EventListener
    public void saveRegisterLog(UserRegisteredEvent event) {
        if (ObjectUtil.isNull(event.userId())) {
            log.warn("用户注册日志缺少用户ID，事件ID：{}", event.eventId());
            return;
        }

        log.info("记录用户注册日志，事件ID：{}，用户ID：{}，注册时间：{}",
                event.eventId(), event.userId(), event.occurredTime());

        // userLogService.saveRegisterLog(event.userId(), event.username(), event.occurredTime());
    }
}
```

监听器边界设计建议如下：

| 设计项         | 建议                                   |
| -------------- | -------------------------------------- |
| 一个监听器方法 | 只处理一个业务动作                     |
| 一个监听器类   | 可以按业务域或职责聚合                 |
| 日志           | 必须记录事件 ID、业务 ID、关键状态     |
| 异常           | 非核心扩展逻辑内部消化异常并记录       |
| 幂等           | 外部通知、优惠券、回调等要考虑重复执行 |
| 依赖           | 不依赖其他监听器执行结果               |
| 耗时           | 耗时逻辑优先异步或消息队列             |

### 多监听器扩展模式

多监听器扩展模式是事件机制最常见的落地方式。一个事件可以被多个监听器同时监听，每个监听器完成一个独立扩展点。发布者只发布一次事件，后续新增扩展逻辑时只增加监听器，不修改发布者代码。

以订单创建事件为例，订单服务只发布 `OrderCreatedEvent`。

文件位置：`src/main/java/io/github/atengk/event/service/OrderService.java`

```java
package io.github.atengk.event.service;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.event.model.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单业务服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 创建订单
     *
     * @param userId      用户ID
     * @param totalAmount 订单金额
     * @return 订单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(Long userId, BigDecimal totalAmount) {
        Long orderId = IdUtil.getSnowflakeNextId();
        String orderNo = IdUtil.fastSimpleUUID();

        log.info("订单创建成功，订单ID：{}，订单编号：{}，用户ID：{}", orderId, orderNo, userId);

        applicationEventPublisher.publishEvent(new OrderCreatedEvent(
                orderId,
                userId,
                orderNo,
                totalAmount,
                LocalDateTime.now()
        ));

        log.info("订单创建事件发布完成，订单ID：{}，订单编号：{}", orderId, orderNo);
        return orderId;
    }
}
```

库存监听器负责处理库存相关逻辑。

文件位置：`src/main/java/io/github/atengk/event/listener/OrderStockEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 订单库存事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderStockEventListener {

    /**
     * 处理订单创建后的库存逻辑
     *
     * @param event 订单创建完成事件
     */
    @EventListener
    public void handleStock(OrderCreatedEvent event) {
        if (ObjectUtil.isNull(event.orderId())) {
            log.warn("订单库存事件缺少订单ID，跳过处理");
            return;
        }

        log.info("处理订单库存逻辑，订单ID：{}，订单编号：{}", event.orderId(), event.orderNo());

        // stockService.lockStockByOrder(event.orderId());
    }
}
```

通知监听器负责发送订单通知，通常可以异步执行。

文件位置：`src/main/java/io/github/atengk/event/listener/OrderNotifyEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 订单通知事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderNotifyEventListener {

    /**
     * 发送订单创建通知
     *
     * @param event 订单创建完成事件
     */
    @Async("eventTaskExecutor")
    @EventListener
    public void sendOrderCreatedNotify(OrderCreatedEvent event) {
        if (ObjectUtil.hasEmpty(event.orderId(), event.userId())) {
            log.warn("订单通知事件参数不完整，事件内容：{}", event);
            return;
        }

        log.info("发送订单创建通知，订单ID：{}，用户ID：{}", event.orderId(), event.userId());

        // notifyService.sendOrderCreatedNotify(event.userId(), event.orderNo());

        log.info("订单创建通知发送完成，订单ID：{}，用户ID：{}", event.orderId(), event.userId());
    }
}
```

轨迹监听器负责记录订单操作轨迹。

文件位置：`src/main/java/io/github/atengk/event/listener/OrderTraceEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 订单轨迹事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderTraceEventListener {

    /**
     * 记录订单创建轨迹
     *
     * @param event 订单创建完成事件
     */
    @EventListener
    public void saveOrderTrace(OrderCreatedEvent event) {
        if (ObjectUtil.isNull(event.orderId())) {
            log.warn("订单轨迹事件缺少订单ID，跳过记录");
            return;
        }

        log.info("记录订单创建轨迹，订单ID：{}，订单编号：{}，创建时间：{}",
                event.orderId(), event.orderNo(), event.createdTime());

        // orderTraceService.saveCreatedTrace(event.orderId(), event.createdTime());
    }
}
```

这种模式下，订单服务和扩展逻辑之间的依赖关系变成了下面这样：

```text
OrderService
  └── 发布 OrderCreatedEvent
        ├── OrderStockEventListener
        ├── OrderNotifyEventListener
        └── OrderTraceEventListener
```

多监听器扩展模式的核心收益是新增扩展点时不修改主流程。例如后续需要在订单创建后同步 BI 系统，只需要新增 `OrderBiSyncEventListener`，继续监听 `OrderCreatedEvent` 即可。

使用多监听器时需要注意以下问题：

| 问题       | 说明                             | 建议                                |
| ---------- | -------------------------------- | ----------------------------------- |
| 执行顺序   | 多个监听器默认不应存在强顺序依赖 | 必须排序时使用 `@Order`，但不要滥用 |
| 异常影响   | 同步监听器异常可能影响发布方     | 非核心逻辑使用异步或内部捕获异常    |
| 重复执行   | 事件可能因补偿或重试重复处理     | 重要监听器必须做幂等                |
| 日志排查   | 多监听器会让链路分散             | 日志中统一记录事件 ID 和业务 ID     |
| 数据一致性 | 事务内发布事件可能早于事务提交   | 依赖提交结果的逻辑使用事务事件      |

在实际项目中，事件机制应作为业务扩展点使用，而不是替代所有方法调用。核心业务链路要保持显式，扩展业务链路可以事件化。这样既能获得解耦收益，也能避免事件链路过度隐式化。



## 典型业务场景

典型业务场景主要用于说明 Spring Event 在真实业务中的落地方式。事件机制适合处理主流程完成后的扩展动作，例如用户注册后的通知、订单创建后的库存处理、操作日志记录、消息通知和第三方回调等。事件发布本质上是将事件交给 Spring 事件广播机制处理，具体是否同步、异步或立即执行，取决于监听器和事件广播器配置。([Home](https://docs.spring.io/spring-framework/docs/6.2.6/javadoc-api/org/springframework/context/ApplicationEventPublisher.html?utm_source=chatgpt.com))

### 用户注册后置处理

用户注册后置处理是 Spring Event 最常见的使用场景。注册主流程只负责用户数据保存和注册结果返回，发送欢迎短信、初始化用户配置、发放新人优惠券、记录注册日志等扩展动作交给事件监听器处理。

推荐拆分方式如下：

| 模块                       | 职责                                 |
| -------------------------- | ------------------------------------ |
| `UserService`              | 执行用户注册主流程，发布用户注册事件 |
| `UserRegisteredEvent`      | 描述用户注册完成这一业务事实         |
| `UserMessageEventListener` | 发送欢迎短信、站内信、邮件           |
| `UserCouponEventListener`  | 发放新人优惠券                       |
| `UserLogEventListener`     | 记录注册日志                         |

`UserRegisteredEvent` 用于描述用户注册成功后的事件数据。

文件位置：`src/main/java/io/github/atengk/event/model/UserRegisteredEvent.java`

```java
package io.github.atengk.event.model;

import java.time.LocalDateTime;

/**
 * 用户注册完成事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record UserRegisteredEvent(
        String eventId,
        Long userId,
        String username,
        String mobile,
        LocalDateTime occurredTime
) {
}
```

`UserService` 完成用户注册主流程后发布事件，后置处理不直接写在注册方法中。

文件位置：`src/main/java/io/github/atengk/event/service/UserService.java`

```java
package io.github.atengk.event.service;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.event.model.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户业务服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 注册用户
     *
     * @param username 用户名
     * @param mobile   手机号
     * @return 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long register(String username, String mobile) {
        Long userId = IdUtil.getSnowflakeNextId();
        String eventId = IdUtil.fastSimpleUUID();

        // 示例中省略数据库保存逻辑
        log.info("用户注册成功，用户ID：{}，用户名：{}", userId, username);

        applicationEventPublisher.publishEvent(new UserRegisteredEvent(
                eventId,
                userId,
                username,
                mobile,
                LocalDateTime.now()
        ));

        log.info("用户注册事件发布完成，事件ID：{}，用户ID：{}", eventId, userId);
        return userId;
    }
}
```

用户消息监听器负责发送欢迎通知。该动作通常不是注册主流程的强依赖，适合异步执行。

文件位置：`src/main/java/io/github/atengk/event/listener/UserMessageEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户消息事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UserMessageEventListener {

    /**
     * 发送用户注册欢迎消息
     *
     * @param event 用户注册完成事件
     */
    @Async("eventTaskExecutor")
    @EventListener
    public void sendWelcomeMessage(UserRegisteredEvent event) {
        if (ObjectUtil.hasEmpty(event.userId(), event.mobile())) {
            log.warn("用户注册消息事件参数不完整，事件ID：{}", event.eventId());
            return;
        }

        try {
            log.info("开始发送用户注册欢迎消息，事件ID：{}，用户ID：{}，手机号：{}",
                    event.eventId(), event.userId(), event.mobile());

            // 示例：调用短信、邮件或站内信服务
            // messageService.sendWelcomeMessage(event.mobile());

            log.info("用户注册欢迎消息发送完成，事件ID：{}，用户ID：{}", event.eventId(), event.userId());
        } catch (Exception e) {
            log.error("用户注册欢迎消息发送失败，事件ID：{}，用户ID：{}",
                    event.eventId(), event.userId(), e);
        }
    }
}
```

优惠券监听器负责发放新人优惠券。该逻辑应具备幂等能力，避免事件重复执行时重复发券。

文件位置：`src/main/java/io/github/atengk/event/listener/UserCouponEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 用户优惠券事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UserCouponEventListener {

    /**
     * 发放新人优惠券
     *
     * @param event 用户注册完成事件
     */
    @EventListener
    public void grantNewUserCoupon(UserRegisteredEvent event) {
        if (ObjectUtil.isNull(event.userId())) {
            log.warn("发放新人优惠券失败，用户ID为空，事件ID：{}", event.eventId());
            return;
        }

        log.info("开始发放新人优惠券，事件ID：{}，用户ID：{}", event.eventId(), event.userId());

        // 示例：真实项目中需要根据 userId + couponType 做幂等判断
        // couponService.grantNewUserCoupon(event.userId());

        log.info("新人优惠券发放完成，事件ID：{}，用户ID：{}", event.eventId(), event.userId());
    }
}
```

这种方式可以让注册主流程保持稳定。后续如果新增“注册后同步 CRM 系统”或“注册后推送运营事件”，只需要新增监听器，不需要修改 `UserService#register` 主流程。

### 订单创建后库存与通知处理

订单创建后通常会触发库存处理、订单轨迹记录、用户通知、支付倒计时初始化等后置动作。是否适合使用事件机制，需要根据业务一致性要求判断。

如果库存扣减或库存锁定是订单创建的核心强依赖，则不建议完全放到普通异步事件中。因为异步监听器失败后，订单可能已经创建成功，但库存没有正确处理。如果库存处理允许补偿，或者只是创建后通知库存模块进行后续处理，则可以使用事件监听器。对于依赖事务提交结果的监听逻辑，建议使用 `@TransactionalEventListener` 绑定事务阶段。`@TransactionalEventListener` 默认在事务提交后执行，也可以通过 `phase` 指定 `BEFORE_COMMIT`、`AFTER_COMMIT`、`AFTER_ROLLBACK` 或 `AFTER_COMPLETION`。([Home](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html?utm_source=chatgpt.com))

`OrderCreatedEvent` 用于描述订单创建完成后的关键数据。

文件位置：`src/main/java/io/github/atengk/event/model/OrderCreatedEvent.java`

```java
package io.github.atengk.event.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建完成事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record OrderCreatedEvent(
        String eventId,
        Long orderId,
        Long userId,
        String orderNo,
        BigDecimal totalAmount,
        LocalDateTime occurredTime
) {
}
```

`OrderService` 在订单保存完成后发布订单创建事件。

文件位置：`src/main/java/io/github/atengk/event/service/OrderService.java`

```java
package io.github.atengk.event.service;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.event.model.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单业务服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 创建订单
     *
     * @param userId      用户ID
     * @param totalAmount 订单金额
     * @return 订单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(Long userId, BigDecimal totalAmount) {
        Long orderId = IdUtil.getSnowflakeNextId();
        String orderNo = IdUtil.fastSimpleUUID();
        String eventId = IdUtil.fastSimpleUUID();

        // 示例中省略订单入库、订单明细保存等逻辑
        log.info("订单创建成功，订单ID：{}，订单编号：{}，用户ID：{}", orderId, orderNo, userId);

        applicationEventPublisher.publishEvent(new OrderCreatedEvent(
                eventId,
                orderId,
                userId,
                orderNo,
                totalAmount,
                LocalDateTime.now()
        ));

        log.info("订单创建事件发布完成，事件ID：{}，订单ID：{}", eventId, orderId);
        return orderId;
    }
}
```

库存监听器如果要求在事务提交后执行，可以使用 `@TransactionalEventListener`。

文件位置：`src/main/java/io/github/atengk/event/listener/OrderStockEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单库存事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderStockEventListener {

    /**
     * 订单事务提交后处理库存逻辑
     *
     * @param event 订单创建完成事件
     */
    @TransactionalEventListener
    public void handleStockAfterCommit(OrderCreatedEvent event) {
        if (ObjectUtil.isNull(event.orderId())) {
            log.warn("订单库存处理失败，订单ID为空，事件ID：{}", event.eventId());
            return;
        }

        log.info("开始处理订单库存，事件ID：{}，订单ID：{}，订单编号：{}",
                event.eventId(), event.orderId(), event.orderNo());

        // 示例：真实项目中可以执行库存锁定、库存流水记录等逻辑
        // stockService.lockStockByOrder(event.orderId());

        log.info("订单库存处理完成，事件ID：{}，订单ID：{}", event.eventId(), event.orderId());
    }
}
```

订单通知监听器可以异步处理，因为通知失败一般不应该导致订单创建失败。

文件位置：`src/main/java/io/github/atengk/event/listener/OrderNotifyEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 订单通知事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderNotifyEventListener {

    /**
     * 异步发送订单创建通知
     *
     * @param event 订单创建完成事件
     */
    @Async("eventTaskExecutor")
    @EventListener
    public void sendOrderCreatedNotify(OrderCreatedEvent event) {
        if (ObjectUtil.hasEmpty(event.orderId(), event.userId())) {
            log.warn("订单通知事件参数不完整，事件ID：{}", event.eventId());
            return;
        }

        try {
            log.info("开始发送订单创建通知，事件ID：{}，订单ID：{}，用户ID：{}",
                    event.eventId(), event.orderId(), event.userId());

            // 示例：发送短信、站内信、WebSocket 推送等
            // notifyService.sendOrderCreatedNotify(event.userId(), event.orderNo());

            log.info("订单创建通知发送完成，事件ID：{}，订单ID：{}", event.eventId(), event.orderId());
        } catch (Exception e) {
            log.error("订单创建通知发送失败，事件ID：{}，订单ID：{}",
                    event.eventId(), event.orderId(), e);
        }
    }
}
```

订单场景的关键不是简单使用事件，而是明确哪些动作属于主流程强一致逻辑，哪些动作属于可异步、可补偿的扩展逻辑。库存扣减、余额扣减、优惠锁定这类核心数据变更通常要谨慎事件化；通知、日志、轨迹、第三方同步更适合事件化。

### 操作日志记录

操作日志记录适合通过事件机制解耦。业务方法只负责发布操作日志事件，日志监听器负责统一落库或发送到日志系统。这样可以避免每个业务方法中都重复编写日志保存逻辑。

`OperationLogEvent` 用于记录操作日志所需的信息。

文件位置：`src/main/java/io/github/atengk/event/model/OperationLogEvent.java`

```java
package io.github.atengk.event.model;

import java.time.LocalDateTime;

/**
 * 操作日志事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record OperationLogEvent(
        String eventId,
        Long operatorId,
        String module,
        String operation,
        String businessNo,
        String content,
        LocalDateTime occurredTime
) {
}
```

业务服务中发布操作日志事件。

文件位置：`src/main/java/io/github/atengk/event/service/ProductService.java`

```java
package io.github.atengk.event.service;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.event.model.OperationLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 商品业务服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 上架商品
     *
     * @param operatorId 操作人ID
     * @param productId  商品ID
     */
    public void publishProduct(Long operatorId, Long productId) {
        // 示例：这里执行商品上架逻辑
        log.info("商品上架成功，操作人ID：{}，商品ID：{}", operatorId, productId);

        applicationEventPublisher.publishEvent(new OperationLogEvent(
                IdUtil.fastSimpleUUID(),
                operatorId,
                "商品管理",
                "商品上架",
                String.valueOf(productId),
                "商品已上架",
                LocalDateTime.now()
        ));
    }
}
```

操作日志监听器异步保存日志，避免日志写入影响主接口响应。

文件位置：`src/main/java/io/github/atengk/event/listener/OperationLogEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.OperationLogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 操作日志事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OperationLogEventListener {

    /**
     * 保存操作日志
     *
     * @param event 操作日志事件
     */
    @Async("eventTaskExecutor")
    @EventListener
    public void saveOperationLog(OperationLogEvent event) {
        if (ObjectUtil.hasEmpty(event.eventId(), event.module(), event.operation())) {
            log.warn("操作日志事件参数不完整，事件内容：{}", event);
            return;
        }

        try {
            log.info("开始保存操作日志，事件ID：{}，模块：{}，操作：{}，业务编号：{}",
                    event.eventId(), event.module(), event.operation(), event.businessNo());

            // 示例：真实项目中保存到数据库、ES 或日志平台
            // operationLogService.save(event);

            log.info("操作日志保存完成，事件ID：{}", event.eventId());
        } catch (Exception e) {
            log.error("操作日志保存失败，事件ID：{}，模块：{}，操作：{}",
                    event.eventId(), event.module(), event.operation(), e);
        }
    }
}
```

操作日志事件适合做成统一基础能力。如果项目中已经有 AOP 日志注解，也可以在切面中发布 `OperationLogEvent`，由监听器统一保存。这样可以让日志采集、日志落库、日志异步化在架构上保持一致。

### 消息通知与第三方回调

消息通知和第三方回调是事件机制的典型扩展场景，但它们也最容易出现可靠性问题。短信、邮件、Webhook、HTTP 回调都属于外部副作用操作，不能随本地数据库事务一起回滚。因此这类逻辑应尽量在事务提交后执行，并增加异常记录、重试或失败补偿。

`ThirdPartyCallbackEvent` 用于描述需要回调外部系统的业务事件。

文件位置：`src/main/java/io/github/atengk/event/model/ThirdPartyCallbackEvent.java`

```java
package io.github.atengk.event.model;

import java.time.LocalDateTime;

/**
 * 第三方回调事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record ThirdPartyCallbackEvent(
        String eventId,
        String callbackUrl,
        String businessType,
        String businessNo,
        String payload,
        LocalDateTime occurredTime
) {
}
```

第三方回调监听器在事务提交后异步执行，并对异常进行记录。

文件位置：`src/main/java/io/github/atengk/event/listener/ThirdPartyCallbackEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import io.github.atengk.event.model.ThirdPartyCallbackEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 第三方回调事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class ThirdPartyCallbackEventListener {

    /**
     * 事务提交后执行第三方回调
     *
     * @param event 第三方回调事件
     */
    @Async("eventTaskExecutor")
    @TransactionalEventListener
    public void callbackAfterCommit(ThirdPartyCallbackEvent event) {
        if (ObjectUtil.hasEmpty(event.eventId(), event.callbackUrl(), event.payload())) {
            log.warn("第三方回调事件参数不完整，事件内容：{}", event);
            return;
        }

        try {
            log.info("开始执行第三方回调，事件ID：{}，业务类型：{}，业务编号：{}，回调地址：{}",
                    event.eventId(), event.businessType(), event.businessNo(), event.callbackUrl());

            HttpResponse response = HttpRequest.post(event.callbackUrl())
                    .body(event.payload())
                    .timeout(5000)
                    .execute();

            if (!response.isOk()) {
                log.warn("第三方回调响应异常，事件ID：{}，状态码：{}，响应内容：{}",
                        event.eventId(), response.getStatus(), response.body());
                return;
            }

            log.info("第三方回调执行成功，事件ID：{}，状态码：{}", event.eventId(), response.getStatus());
        } catch (Exception e) {
            log.error("第三方回调执行失败，事件ID：{}，业务编号：{}",
                    event.eventId(), event.businessNo(), e);

            // 重要回调建议写入失败记录表，由定时任务或消息队列补偿
            // callbackFailRecordService.save(event, e);
        }
    }
}
```

如果第三方回调要求高可靠，单纯依赖 Spring Event 不够。Spring Event 不提供消息持久化、消费确认、死信队列和跨进程投递能力。此时更推荐在事务提交后写入本地消息表，或者发送可靠消息到 RabbitMQ、Kafka、RocketMQ，再由消费者执行回调。

## 事务与事件一致性

事务与事件一致性是 Spring Event 落地时最容易出问题的部分。问题的根源在于：业务事件通常在事务方法中发布，但事件监听器可能在事务提交前就执行。如果监听器执行了外部调用、消息通知、缓存刷新等不可回滚操作，就可能与数据库最终提交结果不一致。

### 事务内发布事件的问题

在 `@Transactional` 方法中直接发布普通事件时，事件发布动作发生在当前事务方法执行过程中。默认同步监听器通常会在当前调用链中执行，可能早于事务提交。`ApplicationEventPublisher#publishEvent` 的官方语义是将事件交给事件广播器处理，不保证一定同步、异步或立即执行，因此业务代码不能只根据 `publishEvent` 方法调用位置推断最终执行语义。([Home](https://docs.spring.io/spring-framework/docs/6.2.6/javadoc-api/org/springframework/context/ApplicationEventPublisher.html?utm_source=chatgpt.com))

典型问题如下：

```java
@Transactional(rollbackFor = Exception.class)
public Long createOrder(Long userId) {
    Long orderId = saveOrder(userId);

    applicationEventPublisher.publishEvent(new OrderCreatedEvent(
            IdUtil.fastSimpleUUID(),
            orderId,
            userId,
            "ORDER-001",
            BigDecimal.TEN,
            LocalDateTime.now()
    ));

    // 如果后续代码抛出异常，事务会回滚
    checkRisk(userId);

    return orderId;
}
```

上面代码中，如果普通监听器在 `publishEvent` 时立即执行，并且监听器发送了短信或调用了第三方接口，后续 `checkRisk` 抛出异常导致事务回滚，就会出现“订单没有创建成功，但外部系统已经收到订单创建通知”的问题。

常见风险如下：

| 风险           | 说明                                                 |
| -------------- | ---------------------------------------------------- |
| 脏业务通知     | 主事务最终回滚，但通知、短信、回调已经发出           |
| 数据读取不一致 | 监听器查询数据库时，目标数据可能还没有提交           |
| 缓存污染       | 事务未提交前刷新缓存，后续回滚导致缓存与数据库不一致 |
| 锁持有时间变长 | 同步监听器耗时逻辑会延长事务执行时间                 |
| 异常影响主流程 | 同步监听器异常可能向上传播，导致主事务回滚           |
| 排查困难       | 事件链路分散，事务边界不清晰时问题难定位             |

因此，在事务方法中发布事件时，必须明确监听器是否依赖事务提交结果。如果依赖，就应使用事务事件监听器；如果不依赖，也要确认监听器异常是否允许影响主事务。

### @TransactionalEventListener

`@TransactionalEventListener` 是 Spring 提供的事务事件监听注解，用于将事件监听器绑定到事务生命周期的指定阶段。默认阶段是 `AFTER_COMMIT`，即事务提交成功后执行监听器。官方文档说明，它支持 `BEFORE_COMMIT`、`AFTER_COMMIT`、`AFTER_ROLLBACK` 和 `AFTER_COMPLETION` 四个阶段；如果没有正在运行的事务，监听器默认不会执行，除非显式设置 `fallbackExecution = true`。([Home](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html?utm_source=chatgpt.com))

事务阶段说明如下：

| 阶段               | 执行时机                     | 适用场景                           |
| ------------------ | ---------------------------- | ---------------------------------- |
| `BEFORE_COMMIT`    | 事务提交前                   | 提交前校验、提交前补充数据         |
| `AFTER_COMMIT`     | 事务提交后，默认值           | 通知、回调、缓存刷新、搜索索引更新 |
| `AFTER_ROLLBACK`   | 事务回滚后                   | 回滚补偿、失败日志、释放外部资源   |
| `AFTER_COMPLETION` | 事务完成后，不区分提交或回滚 | 清理上下文、统计事务完成结果       |

`OrderTransactionalEventListener` 展示不同事务阶段的监听方式。

文件位置：`src/main/java/io/github/atengk/event/listener/OrderTransactionalEventListener.java`

```java
package io.github.atengk.event.listener;

import io.github.atengk.event.model.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单事务事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderTransactionalEventListener {

    /**
     * 事务提交前处理
     *
     * @param event 订单创建完成事件
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void beforeCommit(OrderCreatedEvent event) {
        log.info("事务提交前处理订单事件，事件ID：{}，订单ID：{}", event.eventId(), event.orderId());

        // 示例：提交前校验、补充事务内数据等
    }

    /**
     * 事务提交后处理
     *
     * @param event 订单创建完成事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(OrderCreatedEvent event) {
        log.info("事务提交后处理订单事件，事件ID：{}，订单ID：{}", event.eventId(), event.orderId());

        // 示例：发送通知、刷新缓存、同步搜索索引、第三方回调等
    }

    /**
     * 事务回滚后处理
     *
     * @param event 订单创建完成事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void afterRollback(OrderCreatedEvent event) {
        log.warn("事务回滚后处理订单事件，事件ID：{}，订单ID：{}", event.eventId(), event.orderId());

        // 示例：记录失败日志、释放外部临时资源等
    }

    /**
     * 事务完成后处理
     *
     * @param event 订单创建完成事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    public void afterCompletion(OrderCreatedEvent event) {
        log.info("事务完成后处理订单事件，事件ID：{}，订单ID：{}", event.eventId(), event.orderId());

        // 示例：清理线程上下文、统计完成结果等
    }
}
```

如果事件可能在无事务环境下发布，而监听器仍然需要执行，可以设置 `fallbackExecution = true`。

```java
@TransactionalEventListener(fallbackExecution = true)
public void handleWithFallback(OrderCreatedEvent event) {
    log.info("处理订单事件，允许无事务时执行，事件ID：{}，订单ID：{}", event.eventId(), event.orderId());
}
```

`fallbackExecution = true` 要谨慎使用。它会让监听器在没有事务时也执行，适合兼容同时存在事务发布和非事务发布的事件，但也可能掩盖事务边界设计不清的问题。

### 事务提交后执行事件

事务提交后执行事件是业务开发中最常用的事务事件模式。它适合处理那些依赖数据库最终提交结果，但不应该影响主事务提交的后置动作，例如发送通知、刷新缓存、更新搜索索引、调用第三方接口、写入异步日志等。

推荐模式如下：

```text
@Transactional 主流程
  ↓
保存业务数据
  ↓
发布业务事件
  ↓
事务提交
  ↓
@TransactionalEventListener(AFTER_COMMIT)
  ↓
执行通知、缓存、回调、索引等后置动作
```

下面示例展示订单支付成功后，在事务提交后发送支付成功通知。

`PaymentSucceededEvent` 用于描述支付成功事件。

文件位置：`src/main/java/io/github/atengk/event/model/PaymentSucceededEvent.java`

```java
package io.github.atengk.event.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付成功事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record PaymentSucceededEvent(
        String eventId,
        Long paymentId,
        Long orderId,
        Long userId,
        BigDecimal payAmount,
        LocalDateTime occurredTime
) {
}
```

支付服务在事务内完成支付状态更新后发布事件。

文件位置：`src/main/java/io/github/atengk/event/service/PaymentService.java`

```java
package io.github.atengk.event.service;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.event.model.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付业务服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 确认支付成功
     *
     * @param paymentId 支付ID
     * @param orderId   订单ID
     * @param userId    用户ID
     * @param payAmount 支付金额
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmPaymentSuccess(Long paymentId, Long orderId, Long userId, BigDecimal payAmount) {
        // 示例：更新支付单状态、订单支付状态、写入支付流水
        log.info("支付状态更新成功，支付ID：{}，订单ID：{}，用户ID：{}", paymentId, orderId, userId);

        applicationEventPublisher.publishEvent(new PaymentSucceededEvent(
                IdUtil.fastSimpleUUID(),
                paymentId,
                orderId,
                userId,
                payAmount,
                LocalDateTime.now()
        ));

        log.info("支付成功事件发布完成，支付ID：{}，订单ID：{}", paymentId, orderId);
    }
}
```

支付成功监听器在事务提交后异步发送通知。

文件位置：`src/main/java/io/github/atengk/event/listener/PaymentNotifyEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.PaymentSucceededEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 支付通知事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class PaymentNotifyEventListener {

    /**
     * 事务提交后发送支付成功通知
     *
     * @param event 支付成功事件
     */
    @Async("eventTaskExecutor")
    @TransactionalEventListener
    public void sendPaymentSuccessNotify(PaymentSucceededEvent event) {
        if (ObjectUtil.hasEmpty(event.paymentId(), event.orderId(), event.userId())) {
            log.warn("支付成功通知事件参数不完整，事件ID：{}", event.eventId());
            return;
        }

        try {
            log.info("开始发送支付成功通知，事件ID：{}，支付ID：{}，订单ID：{}，用户ID：{}",
                    event.eventId(), event.paymentId(), event.orderId(), event.userId());

            // 示例：发送短信、站内信、WebSocket、App Push 等
            // paymentNotifyService.sendSuccessNotify(event.userId(), event.orderId(), event.payAmount());

            log.info("支付成功通知发送完成，事件ID：{}，支付ID：{}", event.eventId(), event.paymentId());
        } catch (Exception e) {
            log.error("支付成功通知发送失败，事件ID：{}，支付ID：{}",
                    event.eventId(), event.paymentId(), e);
        }
    }
}
```

在 Spring Framework 6.1 之后，事务事件监听同时支持基于 `PlatformTransactionManager` 的线程绑定事务，以及基于 `ReactiveTransactionManager` 的响应式事务；响应式事务场景需要将事务上下文包含在发布的事件源中，可参考 `TransactionalEventPublisher`。普通 Spring MVC + JDBC/MyBatis/JPA 项目通常属于线程绑定事务场景。([Home](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html?utm_source=chatgpt.com))

### 事件执行失败的处理策略

事件执行失败的处理策略应根据事件的重要程度决定。不是所有监听器失败都需要重试，也不是所有事件都适合简单记录日志。通知类、日志类、统计类事件通常可以记录失败并告警；第三方回调、搜索索引、缓存一致性、资金相关扩展动作则应具备补偿能力。

常见处理策略如下：

| 策略         | 说明                                     | 适用场景                       |
| ------------ | ---------------------------------------- | ------------------------------ |
| 记录错误日志 | 捕获异常并记录事件 ID、业务 ID、异常堆栈 | 非核心通知、普通日志           |
| 失败记录表   | 将失败事件落库，后续定时任务重试         | 第三方回调、索引同步、重要通知 |
| 幂等处理     | 根据事件 ID 或业务唯一键防止重复执行     | 发券、回调、库存流水           |
| 定时补偿     | 周期性扫描失败记录并重试                 | 外部接口不稳定场景             |
| 消息队列     | 将事件转换为可靠消息投递                 | 高可靠、跨服务、削峰场景       |
| 人工介入     | 超过重试次数后进入人工处理               | 资金、订单、合同等关键业务     |

失败记录对象可以这样设计。

文件位置：`src/main/java/io/github/atengk/event/model/EventFailRecord.java`

```java
package io.github.atengk.event.model;

import java.time.LocalDateTime;

/**
 * 事件失败记录
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record EventFailRecord(
        String eventId,
        String eventType,
        String businessNo,
        String eventBody,
        String failReason,
        Integer retryCount,
        String status,
        LocalDateTime createdTime,
        LocalDateTime updatedTime
) {
}
```

事件失败记录服务负责保存失败事件。这里给出接口定义，实际项目中可以使用 MyBatis-Plus、JPA 或其他持久层实现。

文件位置：`src/main/java/io/github/atengk/event/service/EventFailRecordService.java`

```java
package io.github.atengk.event.service;

/**
 * 事件失败记录服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface EventFailRecordService {

    /**
     * 保存失败事件
     *
     * @param eventId    事件ID
     * @param eventType  事件类型
     * @param businessNo 业务编号
     * @param eventBody  事件内容
     * @param exception  异常信息
     */
    void saveFailRecord(String eventId, String eventType, String businessNo, String eventBody, Exception exception);
}
```

在监听器中保存失败记录。

```java
try {
    // 执行业务监听逻辑
    // callbackService.callback(event);
} catch (Exception e) {
    log.error("事件处理失败，事件ID：{}，业务编号：{}", event.eventId(), event.businessNo(), e);

    eventFailRecordService.saveFailRecord(
            event.eventId(),
            event.getClass().getName(),
            event.businessNo(),
            JSONUtil.toJsonStr(event),
            e
    );
}
```

如果需要补偿重试，可以通过定时任务扫描失败记录表。下面示例只展示核心结构，真实项目需要配合数据库状态更新、并发锁、最大重试次数和告警机制。

文件位置：`src/main/java/io/github/atengk/event/job/EventFailRetryJob.java`

```java
package io.github.atengk.event.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 事件失败重试任务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventFailRetryJob {

    /**
     * 重试失败事件
     */
    @Scheduled(fixedDelay = 60_000)
    public void retryFailEvent() {
        log.info("开始扫描失败事件记录");

        // 示例：
        // 1. 查询状态为待重试的失败事件
        // 2. 根据 eventType 还原事件对象
        // 3. 调用对应补偿处理器
        // 4. 成功后更新为已完成
        // 5. 失败后增加 retryCount，超过阈值后标记为人工处理

        log.info("失败事件记录扫描完成");
    }
}
```

事件失败处理建议遵循以下规范：

| 规范                       | 说明                                          |
| -------------------------- | --------------------------------------------- |
| 日志必须包含事件 ID        | 便于串联发布日志、监听日志和失败记录          |
| 重要监听器必须幂等         | 防止重试导致重复发券、重复回调、重复写流水    |
| 外部调用必须设置超时       | 避免线程池被长时间阻塞                        |
| 不要无限重试               | 应设置最大重试次数和失败状态                  |
| 失败记录要保留原始事件内容 | 便于问题复现和人工补偿                        |
| 关键事件建议接入告警       | 超过重试次数或连续失败时及时通知              |
| 高可靠场景使用消息队列     | Spring Event 只适合应用内解耦，不负责可靠投递 |

在事务一致性要求较高的业务中，推荐组合使用以下模式：主事务保存业务数据和本地事件记录，事务提交后由事件监听器或定时任务投递消息，消费者执行外部通知或回调。这样可以比单纯依赖内存事件获得更好的可恢复性和可观测性。



## 项目落地实现

项目落地实现部分给出一套可以直接集成到 Spring Boot 3 项目中的事件机制代码结构。该实现覆盖依赖配置、事件对象定义、事件发布封装、监听器实现和异步线程池配置，适合作为普通单体应用或模块化后端服务的应用内事件方案。

推荐目录结构如下：

```text
src/main/java/io/github/atengk/event
├── config
│   ├── AsyncConfig.java
│   ├── EventExecutorConfig.java
│   └── EventExecutorProperties.java
├── listener
│   ├── OrderEventListener.java
│   └── UserEventListener.java
├── model
│   ├── BusinessEvent.java
│   ├── OrderCreatedEvent.java
│   └── UserRegisteredEvent.java
├── publisher
│   └── BusinessEventPublisher.java
└── service
    ├── OrderService.java
    └── UserService.java
```

### 依赖与基础配置

依赖与基础配置用于提供 Spring Boot 事件机制、异步执行、日志、Lombok、Hutool 工具类和配置属性绑定能力。Spring Event 本身属于 Spring Framework 基础能力，不需要单独引入事件专用依赖；实际项目中通常通过 `spring-boot-starter-web`、`spring-boot-starter` 或其他 starter 间接具备事件发布与监听能力。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Web 基础能力，包含 Spring 容器、Web MVC、JSON 等常用能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- AOP 支持，@Async 基于 Spring 代理机制执行异步方法 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>

    <!-- Hutool 工具类，示例中用于 ID、对象判断等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok 简化日志、构造方法、Getter/Setter 等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 配置属性提示，用于 @ConfigurationProperties 元数据生成 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 单元测试与 Spring Boot 测试支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目中还没有定义 Hutool 版本，可以在 `properties` 中统一维护。

```xml
<properties>
    <!-- Spring Boot 3 推荐使用 Java 17 或更高版本 -->
    <java.version>17</java.version>
    <!-- Hutool 版本按项目依赖规范统一管理 -->
    <hutool.version>5.8.36</hutool.version>
</properties>
```

基础配置主要包括应用名称、事件线程池参数和日志级别。事件线程池参数不建议硬编码在代码中，应放到配置文件中统一维护。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    # 应用名称，便于日志、链路追踪和监控识别
    name: spring-event-demo

event:
  executor:
    # 核心线程数，常态事件处理线程数量
    core-pool-size: 4
    # 最大线程数，事件高峰期允许扩展的线程数量
    max-pool-size: 8
    # 队列容量，超过核心线程后进入队列等待
    queue-capacity: 200
    # 空闲线程存活时间，单位：秒
    keep-alive-seconds: 60
    # 应用关闭时等待异步任务完成时间，单位：秒
    await-termination-seconds: 30
    # 线程名前缀，便于通过日志定位事件任务
    thread-name-prefix: event-task-

logging:
  level:
    # 示例项目包日志级别
    io.github.atengk.event: info
```

如果项目已经存在 `application.yml`，只需要合并 `event.executor` 配置即可。

### 事件类定义

事件类用于表达“某个业务事实已经发生”。事件对象建议使用不可变结构，例如 Java `record`，字段保持必要、稳定、明确。不要把完整 Entity 直接放进事件对象中，避免监听器依赖数据库实体结构。

首先定义统一业务事件接口。该接口不是必须的，但在需要统一记录事件日志、事件 ID、事件时间、失败补偿时非常有用。

文件位置：`src/main/java/io/github/atengk/event/model/BusinessEvent.java`

```java
package io.github.atengk.event.model;

import java.time.LocalDateTime;

/**
 * 业务事件基础接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface BusinessEvent {

    /**
     * 获取事件ID
     *
     * @return 事件ID
     */
    String eventId();

    /**
     * 获取事件发生时间
     *
     * @return 事件发生时间
     */
    LocalDateTime occurredTime();
}
```

用户注册事件用于表达用户注册已经完成。监听器可以基于该事件发送欢迎消息、初始化用户配置、记录注册日志等。

文件位置：`src/main/java/io/github/atengk/event/model/UserRegisteredEvent.java`

```java
package io.github.atengk.event.model;

import java.time.LocalDateTime;

/**
 * 用户注册完成事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record UserRegisteredEvent(
        String eventId,
        Long userId,
        String username,
        String mobile,
        LocalDateTime occurredTime
) implements BusinessEvent {
}
```

订单创建事件用于表达订单已经创建完成。监听器可以基于该事件处理订单轨迹、订单通知、库存扩展逻辑等。

文件位置：`src/main/java/io/github/atengk/event/model/OrderCreatedEvent.java`

```java
package io.github.atengk.event.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建完成事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record OrderCreatedEvent(
        String eventId,
        Long orderId,
        Long userId,
        String orderNo,
        BigDecimal totalAmount,
        LocalDateTime occurredTime
) implements BusinessEvent {
}
```

事件类定义建议遵循以下规则：

| 规则           | 说明                                            |
| -------------- | ----------------------------------------------- |
| 使用完成态命名 | 例如 `UserRegisteredEvent`、`OrderCreatedEvent` |
| 包含事件 ID    | 便于日志追踪、幂等处理、失败补偿                |
| 包含业务 ID    | 例如 `userId`、`orderId`、`orderNo`             |
| 包含发生时间   | 便于审计、排序、问题排查                        |
| 避免放 Entity  | 防止监听器依赖数据库表结构                      |
| 字段保持稳定   | 事件对象是发布者与监听器之间的契约              |

### 事件发布封装

事件发布封装用于统一事件发布入口。业务代码可以直接注入 `ApplicationEventPublisher` 发布事件，但在中大型项目中，更推荐封装一个 `BusinessEventPublisher`，用于统一日志、参数检查、事件类型识别和后续扩展。

`BusinessEventPublisher` 对 Spring 原生事件发布能力进行一层业务封装。

文件位置：`src/main/java/io/github/atengk/event/publisher/BusinessEventPublisher.java`

```java
package io.github.atengk.event.publisher;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.BusinessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 业务事件发布器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 发布业务事件
     *
     * @param event 业务事件
     */
    public void publish(Object event) {
        if (ObjectUtil.isNull(event)) {
            log.warn("业务事件为空，跳过发布");
            return;
        }

        if (event instanceof BusinessEvent businessEvent) {
            log.info("开始发布业务事件，事件ID：{}，事件类型：{}",
                    businessEvent.eventId(), event.getClass().getSimpleName());
        } else {
            log.info("开始发布普通事件，事件类型：{}", event.getClass().getSimpleName());
        }

        applicationEventPublisher.publishEvent(event);

        if (event instanceof BusinessEvent businessEvent) {
            log.info("业务事件发布完成，事件ID：{}，事件类型：{}",
                    businessEvent.eventId(), event.getClass().getSimpleName());
        } else {
            log.info("普通事件发布完成，事件类型：{}", event.getClass().getSimpleName());
        }
    }
}
```

用户业务服务通过统一发布器发布用户注册事件。主流程只关心用户注册成功，不直接调用短信、优惠券、日志等扩展逻辑。

文件位置：`src/main/java/io/github/atengk/event/service/UserService.java`

```java
package io.github.atengk.event.service;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.event.model.UserRegisteredEvent;
import io.github.atengk.event.publisher.BusinessEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户业务服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final BusinessEventPublisher businessEventPublisher;

    /**
     * 注册用户
     *
     * @param username 用户名
     * @param mobile   手机号
     * @return 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long register(String username, String mobile) {
        Long userId = IdUtil.getSnowflakeNextId();
        String eventId = IdUtil.fastSimpleUUID();

        // 示例中省略用户唯一性校验、密码加密、用户数据入库等主流程逻辑
        log.info("用户注册成功，用户ID：{}，用户名：{}", userId, username);

        businessEventPublisher.publish(new UserRegisteredEvent(
                eventId,
                userId,
                username,
                mobile,
                LocalDateTime.now()
        ));

        return userId;
    }
}
```

订单业务服务通过统一发布器发布订单创建事件。订单主流程只负责订单创建，通知、轨迹、扩展动作由监听器处理。

文件位置：`src/main/java/io/github/atengk/event/service/OrderService.java`

```java
package io.github.atengk.event.service;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.event.model.OrderCreatedEvent;
import io.github.atengk.event.publisher.BusinessEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单业务服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final BusinessEventPublisher businessEventPublisher;

    /**
     * 创建订单
     *
     * @param userId      用户ID
     * @param totalAmount 订单金额
     * @return 订单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(Long userId, BigDecimal totalAmount) {
        Long orderId = IdUtil.getSnowflakeNextId();
        String orderNo = IdUtil.fastSimpleUUID();
        String eventId = IdUtil.fastSimpleUUID();

        // 示例中省略订单主表、订单明细、金额计算等主流程逻辑
        log.info("订单创建成功，订单ID：{}，订单编号：{}，用户ID：{}", orderId, orderNo, userId);

        businessEventPublisher.publish(new OrderCreatedEvent(
                eventId,
                orderId,
                userId,
                orderNo,
                totalAmount,
                LocalDateTime.now()
        ));

        return orderId;
    }
}
```

事件发布封装的价值在于统一入口。后续如果需要接入链路追踪、事件审计、事件开关、灰度控制、失败记录或消息队列桥接，可以优先在 `BusinessEventPublisher` 中扩展，而不是修改每个业务 Service。

### 监听器实现

监听器用于处理业务事件对应的后置动作。监听器应保持单一职责，避免一个监听器方法中堆积多个无关逻辑。普通非耗时逻辑可以同步执行，耗时逻辑可以配合 `@Async` 异步执行，依赖事务提交结果的逻辑可以使用 `@TransactionalEventListener`。

用户事件监听器处理用户注册后的扩展逻辑。欢迎消息使用异步监听，用户日志使用同步监听。

文件位置：`src/main/java/io/github/atengk/event/listener/UserEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UserEventListener {

    /**
     * 异步发送用户欢迎消息
     *
     * @param event 用户注册完成事件
     */
    @Async("eventTaskExecutor")
    @EventListener
    public void sendWelcomeMessage(UserRegisteredEvent event) {
        if (ObjectUtil.hasEmpty(event.eventId(), event.userId(), event.mobile())) {
            log.warn("用户欢迎消息事件参数不完整，事件内容：{}", event);
            return;
        }

        try {
            log.info("开始发送用户欢迎消息，事件ID：{}，用户ID：{}，手机号：{}",
                    event.eventId(), event.userId(), event.mobile());

            // 示例：调用短信、邮件、站内信或 App Push 服务
            // messageService.sendWelcomeMessage(event.mobile());

            log.info("用户欢迎消息发送完成，事件ID：{}，用户ID：{}", event.eventId(), event.userId());
        } catch (Exception e) {
            log.error("用户欢迎消息发送失败，事件ID：{}，用户ID：{}",
                    event.eventId(), event.userId(), e);

            // 重要通知可在这里写入失败记录表，后续定时补偿
            // eventFailRecordService.save(event, e);
        }
    }

    /**
     * 记录用户注册日志
     *
     * @param event 用户注册完成事件
     */
    @EventListener
    public void saveRegisterLog(UserRegisteredEvent event) {
        if (ObjectUtil.hasEmpty(event.eventId(), event.userId())) {
            log.warn("用户注册日志事件参数不完整，事件内容：{}", event);
            return;
        }

        log.info("记录用户注册日志，事件ID：{}，用户ID：{}，用户名：{}，注册时间：{}",
                event.eventId(), event.userId(), event.username(), event.occurredTime());

        // 示例：保存注册日志、审计日志或行为日志
        // userLogService.saveRegisterLog(event);
    }
}
```

订单事件监听器处理订单创建后的扩展逻辑。订单轨迹记录适合同步处理，订单通知适合异步处理；如果监听器依赖订单事务提交结果，可以改用 `@TransactionalEventListener`。

文件位置：`src/main/java/io/github/atengk/event/listener/OrderEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderEventListener {

    /**
     * 记录订单创建轨迹
     *
     * @param event 订单创建完成事件
     */
    @EventListener
    public void saveOrderTrace(OrderCreatedEvent event) {
        if (ObjectUtil.hasEmpty(event.eventId(), event.orderId())) {
            log.warn("订单轨迹事件参数不完整，事件内容：{}", event);
            return;
        }

        log.info("记录订单创建轨迹，事件ID：{}，订单ID：{}，订单编号：{}",
                event.eventId(), event.orderId(), event.orderNo());

        // 示例：保存订单轨迹
        // orderTraceService.saveCreatedTrace(event.orderId(), event.orderNo(), event.occurredTime());
    }

    /**
     * 事务提交后处理订单库存扩展逻辑
     *
     * @param event 订单创建完成事件
     */
    @TransactionalEventListener
    public void handleStockAfterCommit(OrderCreatedEvent event) {
        if (ObjectUtil.hasEmpty(event.eventId(), event.orderId())) {
            log.warn("订单库存事件参数不完整，事件内容：{}", event);
            return;
        }

        log.info("事务提交后处理订单库存扩展逻辑，事件ID：{}，订单ID：{}",
                event.eventId(), event.orderId());

        // 示例：库存扩展处理。如果库存扣减属于下单强一致逻辑，不建议放在普通事件中
        // stockService.handleOrderCreated(event.orderId());
    }

    /**
     * 异步发送订单创建通知
     *
     * @param event 订单创建完成事件
     */
    @Async("eventTaskExecutor")
    @EventListener
    public void sendOrderCreatedNotify(OrderCreatedEvent event) {
        if (ObjectUtil.hasEmpty(event.eventId(), event.orderId(), event.userId())) {
            log.warn("订单通知事件参数不完整，事件内容：{}", event);
            return;
        }

        try {
            log.info("开始发送订单创建通知，事件ID：{}，订单ID：{}，用户ID：{}",
                    event.eventId(), event.orderId(), event.userId());

            // 示例：发送短信、站内信、WebSocket、App Push 等
            // notifyService.sendOrderCreatedNotify(event.userId(), event.orderNo());

            log.info("订单创建通知发送完成，事件ID：{}，订单ID：{}", event.eventId(), event.orderId());
        } catch (Exception e) {
            log.error("订单创建通知发送失败，事件ID：{}，订单ID：{}",
                    event.eventId(), event.orderId(), e);
        }
    }
}
```

监听器实现时建议遵循以下规范：

| 规范                   | 说明                                                   |
| ---------------------- | ------------------------------------------------------ |
| 监听器方法职责单一     | 一个方法只处理一个后置动作                             |
| 日志包含事件 ID        | 便于关联发布日志、监听日志和异常日志                   |
| 异步监听器捕获异常     | 防止异常丢失，便于失败补偿                             |
| 外部调用设置超时       | 避免事件线程池被长时间阻塞                             |
| 重要动作保证幂等       | 例如发券、回调、库存流水、消息投递                     |
| 事务后动作使用事务事件 | 依赖数据库提交结果时使用 `@TransactionalEventListener` |

### 异步线程池配置

异步线程池配置用于承接 `@Async` 标记的事件监听器。项目中应为事件处理单独配置线程池，不建议所有异步任务共用默认线程池。独立线程池可以避免报表导出、定时任务、事件监听、消息消费等任务互相影响。

首先定义事件线程池配置属性类。

文件位置：`src/main/java/io/github/atengk/event/config/EventExecutorProperties.java`

```java
package io.github.atengk.event.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 事件线程池配置属性
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Component
@ConfigurationProperties(prefix = "event.executor")
public class EventExecutorProperties {

    /**
     * 核心线程数
     */
    private int corePoolSize = 4;

    /**
     * 最大线程数
     */
    private int maxPoolSize = 8;

    /**
     * 队列容量
     */
    private int queueCapacity = 200;

    /**
     * 线程空闲存活时间，单位：秒
     */
    private int keepAliveSeconds = 60;

    /**
     * 关闭时等待任务完成时间，单位：秒
     */
    private int awaitTerminationSeconds = 30;

    /**
     * 线程名前缀
     */
    private String threadNamePrefix = "event-task-";
}
```

然后创建事件线程池 Bean。这里使用 Hutool 的 `ThreadFactoryBuilder` 设置线程名称，便于从日志中识别事件线程。

文件位置：`src/main/java/io/github/atengk/event/config/EventExecutorConfig.java`

```java
package io.github.atengk.event.config;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 事件线程池配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Configuration
@RequiredArgsConstructor
public class EventExecutorConfig {

    private final EventExecutorProperties eventExecutorProperties;

    /**
     * 创建事件异步线程池
     *
     * @return 事件线程池
     */
    @Bean("eventTaskExecutor")
    public ThreadPoolTaskExecutor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(eventExecutorProperties.getCorePoolSize());
        executor.setMaxPoolSize(eventExecutorProperties.getMaxPoolSize());
        executor.setQueueCapacity(eventExecutorProperties.getQueueCapacity());
        executor.setKeepAliveSeconds(eventExecutorProperties.getKeepAliveSeconds());
        executor.setThreadNamePrefix(eventExecutorProperties.getThreadNamePrefix());

        executor.setThreadFactory(ThreadFactoryBuilder.create()
                .setNamePrefix(eventExecutorProperties.getThreadNamePrefix())
                .build());

        // 队列满时由调用线程执行，避免任务直接丢失；高并发场景需结合压测评估
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 应用关闭时等待队列任务执行完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(eventExecutorProperties.getAwaitTerminationSeconds());

        executor.initialize();
        return executor;
    }
}
```

最后开启 Spring 异步能力，并配置异步异常处理器。`void` 异步方法未捕获的异常会进入 `AsyncUncaughtExceptionHandler`。

文件位置：`src/main/java/io/github/atengk/event/config/AsyncConfig.java`

```java
package io.github.atengk.event.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.reflect.Method;

/**
 * 异步执行配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 获取异步异常处理器
     *
     * @return 异步异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return this::handleAsyncException;
    }

    /**
     * 处理异步方法未捕获异常
     *
     * @param throwable 异常
     * @param method    方法
     * @param objects   参数
     */
    private void handleAsyncException(Throwable throwable, Method method, Object... objects) {
        log.error("异步事件执行异常，方法：{}，参数：{}", method.getName(), objects, throwable);
    }
}
```

配置完成后，监听器可以通过以下方式指定事件线程池：

```java
@Async("eventTaskExecutor")
@EventListener
public void handle(UserRegisteredEvent event) {
    log.info("异步处理用户注册事件，事件ID：{}", event.eventId());
}
```

线程池落地时需要注意几个问题。第一，线程池参数必须结合接口流量、事件耗时、机器资源和压测结果调整。第二，异步事件不是可靠消息队列，应用重启时内存中的未完成事件可能丢失。第三，重要事件必须配合失败记录、重试机制或消息队列，不能只依赖 `@Async`。



## 开发规范

开发规范用于统一项目中事件的命名、内容设计、监听器拆分、日志记录和异常处理方式。Spring Event 本身使用简单，但如果缺少规范，项目中很容易出现事件命名混乱、事件字段随意、监听器职责过大、异常影响主流程、日志链路不可追踪等问题。

### 事件命名规范

事件命名应表达“某个业务事实已经发生”，而不是表达“要执行某个动作”。事件不是命令，事件监听器才是根据事件执行后续动作的处理者。因此事件类名建议使用完成态命名。

推荐命名格式：

```text
业务对象 + 完成态动作 + Event
```

常见示例：

| 业务事实   | 推荐事件名                 | 不推荐事件名          |
| ---------- | -------------------------- | --------------------- |
| 用户已注册 | `UserRegisteredEvent`      | `RegisterUserEvent`   |
| 订单已创建 | `OrderCreatedEvent`        | `CreateOrderEvent`    |
| 支付已成功 | `PaymentSucceededEvent`    | `PaySuccessEvent`     |
| 商品已上架 | `ProductPublishedEvent`    | `PublishProductEvent` |
| 库存已变更 | `ProductStockChangedEvent` | `UpdateStockEvent`    |
| 退款已完成 | `RefundCompletedEvent`     | `DoRefundEvent`       |

事件命名应避免以下问题：

| 问题             | 示例               | 说明                             |
| ---------------- | ------------------ | -------------------------------- |
| 使用动词命令式   | `SendSmsEvent`     | 这是动作，不是业务事实           |
| 命名过于宽泛     | `UserEvent`        | 无法判断具体发生了什么           |
| 命名带技术实现   | `UserAsyncEvent`   | 是否异步不应该体现在事件名中     |
| 命名带监听器意图 | `GrantCouponEvent` | 发券只是监听器行为，不是注册事实 |
| 命名缩写过多     | `UsrRegEvt`        | 降低可读性和维护性               |

推荐做法如下：

```java
// 推荐：表达业务事实
UserRegisteredEvent
OrderCreatedEvent
PaymentSucceededEvent

// 不推荐：表达命令动作
SendWelcomeMessageEvent
CreateOrderEvent
DoPaymentNotifyEvent
```

事件类包路径建议统一放在 `model`、`event` 或业务模块下的 `event` 包中。例如：

```text
src/main/java/io/github/atengk/user/event/UserRegisteredEvent.java
src/main/java/io/github/atengk/order/event/OrderCreatedEvent.java
src/main/java/io/github/atengk/payment/event/PaymentSucceededEvent.java
```

如果项目规模较小，也可以统一放在公共事件包中：

```text
src/main/java/io/github/atengk/event/model/UserRegisteredEvent.java
src/main/java/io/github/atengk/event/model/OrderCreatedEvent.java
```

关键点是同一个项目内保持一致，不要一部分事件放在 `dto`，一部分放在 `listener`，一部分放在 `service`。

### 事件内容设计规范

事件内容是发布者与监听器之间的契约。事件字段应服务于监听器处理，但不能为了某个监听器的临时实现随意膨胀字段。事件对象应保持稳定、明确、轻量、不可变。

推荐事件对象包含以下基础字段：

| 字段           | 说明                                            | 是否推荐               |
| -------------- | ----------------------------------------------- | ---------------------- |
| `eventId`      | 事件唯一标识，用于日志追踪、幂等和失败补偿      | 推荐                   |
| `businessId`   | 业务主键，例如 `userId`、`orderId`、`paymentId` | 推荐                   |
| `businessNo`   | 业务编号，例如订单号、支付流水号                | 视场景使用             |
| `occurredTime` | 事件发生时间                                    | 推荐                   |
| `operatorId`   | 操作人 ID                                       | 管理后台、审计场景推荐 |
| `source`       | 事件来源，例如 `APP`、`ADMIN`、`JOB`            | 多入口场景推荐         |
| `snapshot`     | 必要业务快照字段                                | 视场景使用             |

推荐使用 `record` 定义不可变事件对象。

文件位置：`src/main/java/io/github/atengk/event/model/UserRegisteredEvent.java`

```java
package io.github.atengk.event.model;

import java.time.LocalDateTime;

/**
 * 用户注册完成事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record UserRegisteredEvent(
        String eventId,
        Long userId,
        String username,
        String mobile,
        Long operatorId,
        String source,
        LocalDateTime occurredTime
) implements BusinessEvent {
}
```

订单事件可以保留必要业务快照，避免监听器为了基础信息频繁反查数据库。

文件位置：`src/main/java/io/github/atengk/event/model/OrderCreatedEvent.java`

```java
package io.github.atengk.event.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建完成事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record OrderCreatedEvent(
        String eventId,
        Long orderId,
        String orderNo,
        Long userId,
        BigDecimal totalAmount,
        Integer itemCount,
        LocalDateTime occurredTime
) implements BusinessEvent {
}
```

事件内容设计建议如下：

| 规范               | 说明                                                  |
| ------------------ | ----------------------------------------------------- |
| 只放必要字段       | 不要把整个 Entity、Request、Response 直接放入事件     |
| 优先使用不可变对象 | 推荐 `record` 或只读 DTO                              |
| 保留业务 ID        | 监听器可以基于 ID 做幂等、查询、补偿                  |
| 保留事件 ID        | 日志追踪和失败记录必须依赖事件 ID                     |
| 保留发生时间       | 审计、排序、排查问题时有用                            |
| 避免携带敏感字段   | 不要在事件中传播明文密码、身份证、银行卡等敏感信息    |
| 避免字段语义模糊   | 不使用 `data`、`value`、`type` 这类无明确业务语义字段 |
| 避免过度定制       | 不要为了某一个监听器加入大量临时字段                  |

不推荐示例：

```java
/**
 * 不推荐：字段过于随意，且直接携带 Entity
 */
public record UserEvent(
        Object data,
        Object userEntity,
        String type
) {
}
```

推荐示例：

```java
/**
 * 推荐：表达明确业务事实，字段稳定且必要
 */
public record UserRegisteredEvent(
        String eventId,
        Long userId,
        String username,
        String mobile,
        LocalDateTime occurredTime
) {
}
```

如果监听器确实需要完整数据，应优先传递业务 ID，由监听器根据需要查询当前最新数据。只有在需要事件发生时的历史快照时，才把必要快照字段放入事件对象中。

### 监听器拆分规范

监听器拆分的核心原则是单一职责。一个监听器方法只处理一个明确的后置动作，不要把多个业务扩展动作写在同一个监听器方法中。否则只是把代码从 Service 挪到了 Listener，并没有真正实现解耦。

不推荐写法：

```java
@EventListener
public void handle(UserRegisteredEvent event) {
    sendWelcomeMessage(event);
    grantCoupon(event);
    saveRegisterLog(event);
    syncCrm(event);
}
```

推荐按照职责拆分：

```text
src/main/java/io/github/atengk/event/listener
├── UserMessageEventListener.java
├── UserCouponEventListener.java
├── UserLogEventListener.java
└── UserCrmSyncEventListener.java
```

用户消息监听器只负责消息通知。

文件位置：`src/main/java/io/github/atengk/event/listener/UserMessageEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户消息事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UserMessageEventListener {

    /**
     * 发送用户注册欢迎消息
     *
     * @param event 用户注册完成事件
     */
    @Async("eventTaskExecutor")
    @EventListener
    public void sendWelcomeMessage(UserRegisteredEvent event) {
        if (ObjectUtil.hasEmpty(event.eventId(), event.userId(), event.mobile())) {
            log.warn("用户欢迎消息事件参数不完整，事件内容：{}", event);
            return;
        }

        log.info("发送用户欢迎消息，事件ID：{}，用户ID：{}，手机号：{}",
                event.eventId(), event.userId(), event.mobile());

        // messageService.sendWelcomeMessage(event.mobile());
    }
}
```

用户优惠券监听器只负责新人优惠券发放。

文件位置：`src/main/java/io/github/atengk/event/listener/UserCouponEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 用户优惠券事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UserCouponEventListener {

    /**
     * 发放新人优惠券
     *
     * @param event 用户注册完成事件
     */
    @EventListener
    public void grantNewUserCoupon(UserRegisteredEvent event) {
        if (ObjectUtil.hasEmpty(event.eventId(), event.userId())) {
            log.warn("新人优惠券事件参数不完整，事件内容：{}", event);
            return;
        }

        log.info("发放新人优惠券，事件ID：{}，用户ID：{}", event.eventId(), event.userId());

        // couponService.grantNewUserCoupon(event.userId());
    }
}
```

监听器拆分建议如下：

| 规范               | 说明                                                   |
| ------------------ | ------------------------------------------------------ |
| 按职责拆分         | 消息、日志、优惠券、回调、缓存分别拆分                 |
| 避免大监听器       | 不要在一个监听器类中堆积过多不相关事件                 |
| 避免强顺序依赖     | 多监听器之间不应依赖执行顺序传递结果                   |
| 需要排序时显式声明 | 可使用 `@Order`，但不能滥用                            |
| 耗时逻辑异步化     | 外部调用、通知、回调、统计等建议异步                   |
| 事务后动作事务化   | 依赖数据库提交结果时使用 `@TransactionalEventListener` |
| 重要动作做幂等     | 发券、回调、流水、库存等必须考虑重复执行               |

如果监听器之间存在强依赖，例如必须先生成某个结果，后续监听器才能使用该结果，通常不建议使用多个普通监听器串联处理。此类场景更适合放回显式业务流程，或者使用工作流、状态机、消息队列编排。

### 日志与异常处理规范

事件机制会让业务链路从直接方法调用变成发布-监听模式。为了便于排查问题，日志中必须包含事件 ID、事件类型、业务 ID 和关键状态。异步事件尤其需要完整日志，因为异常不会直接返回给事件发布方。

事件发布日志建议包含：

| 日志项        | 说明               |
| ------------- | ------------------ |
| `eventId`     | 事件唯一标识       |
| `eventType`   | 事件类型           |
| `businessId`  | 业务 ID            |
| `publishTime` | 发布时间           |
| `publisher`   | 发布位置或发布组件 |

监听器日志建议包含：

| 日志项       | 说明               |
| ------------ | ------------------ |
| `eventId`    | 用于关联发布日志   |
| `eventType`  | 当前处理的事件类型 |
| `listener`   | 当前监听器         |
| `businessId` | 业务 ID            |
| `result`     | 执行结果           |
| `error`      | 异常堆栈和失败原因 |

统一发布器中记录事件发布日志。

```java
if (event instanceof BusinessEvent businessEvent) {
    log.info("发布业务事件，事件ID：{}，事件类型：{}，发生时间：{}",
            businessEvent.eventId(), event.getClass().getSimpleName(), businessEvent.occurredTime());
}
```

监听器中记录开始、成功和失败日志。

```java
try {
    log.info("开始处理用户注册事件，事件ID：{}，用户ID：{}", event.eventId(), event.userId());

    // 执行业务处理

    log.info("用户注册事件处理完成，事件ID：{}，用户ID：{}", event.eventId(), event.userId());
} catch (Exception e) {
    log.error("用户注册事件处理失败，事件ID：{}，用户ID：{}", event.eventId(), event.userId(), e);
}
```

异常处理规范如下：

| 场景                         | 处理方式                                 |
| ---------------------------- | ---------------------------------------- |
| 同步监听器异常必须影响主流程 | 允许抛出异常，但要明确业务含义           |
| 同步监听器异常不应影响主流程 | 在监听器内部捕获并记录                   |
| 异步监听器异常               | 必须捕获并记录，重要事件写入失败记录     |
| 第三方调用失败               | 记录请求参数、响应码、响应内容、异常信息 |
| 可重试失败                   | 写入失败记录表或消息队列                 |
| 不可重试失败                 | 记录明确原因并告警                       |
| 关键业务失败                 | 接入告警和人工处理机制                   |

不推荐在监听器中吞掉异常且不记录日志：

```java
try {
    // 处理事件
} catch (Exception ignored) {
}
```

推荐至少记录错误日志，重要事件写入失败记录。

```java
try {
    // 处理事件
} catch (Exception e) {
    log.error("事件处理失败，事件ID：{}，事件类型：{}",
            event.eventId(), event.getClass().getSimpleName(), e);

    // eventFailRecordService.saveFailRecord(event, e);
}
```

异步事件还应配置统一异常处理器，避免未捕获异常没有统一出口。

文件位置：`src/main/java/io/github/atengk/event/config/AsyncConfig.java`

```java
package io.github.atengk.event.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.reflect.Method;

/**
 * 异步执行配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 获取异步异常处理器
     *
     * @return 异步异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return this::handleAsyncException;
    }

    /**
     * 处理异步方法未捕获异常
     *
     * @param throwable 异常
     * @param method    异步方法
     * @param objects   方法参数
     */
    private void handleAsyncException(Throwable throwable, Method method, Object... objects) {
        log.error("异步事件执行异常，方法：{}，参数：{}", method.getName(), objects, throwable);
    }
}
```

日志与异常处理的目标不是把所有异常都隐藏起来，而是根据监听器职责判断异常是否应该影响主流程。核心强一致逻辑应显式失败，非核心扩展逻辑应隔离失败并保留可追踪记录。

## 测试与验证

测试与验证用于确认事件发布、监听器执行、异步处理和事务事件行为符合预期。事件机制的测试不能只验证方法返回值，还需要验证事件是否被发布、监听器是否执行、异步任务是否完成、事务提交或回滚时监听器是否按预期触发。

测试建议覆盖以下类型：

| 测试类型     | 验证目标                         |
| ------------ | -------------------------------- |
| 同步事件测试 | 验证事件发布后监听器立即执行     |
| 异步事件测试 | 验证事件发布后异步监听器最终执行 |
| 事务事件测试 | 验证提交后、回滚后监听器行为     |
| 解耦效果验证 | 验证主流程不依赖具体扩展逻辑     |

### 同步事件测试

同步事件测试主要验证发布事件后，监听器能够在当前流程中完成处理。可以通过内存计数器、测试专用监听器、Mock 服务或 Spring Boot 的事件记录能力进行验证。

下面给出一个简单的同步事件测试示例。测试监听器通过 `AtomicInteger` 记录执行次数，发布事件后立即断言监听器已经执行。

测试专用事件对象用于验证同步事件行为。

文件位置：`src/test/java/io/github/atengk/event/test/TestSyncEvent.java`

```java
package io.github.atengk.event.test;

import java.time.LocalDateTime;

/**
 * 测试同步事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record TestSyncEvent(
        String eventId,
        Long businessId,
        LocalDateTime occurredTime
) {
}
```

测试专用监听器用于记录同步事件处理次数。

文件位置：`src/test/java/io/github/atengk/event/test/TestSyncEventListener.java`

```java
package io.github.atengk.event.test;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试同步事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class TestSyncEventListener {

    private final AtomicInteger handleCount = new AtomicInteger();

    /**
     * 处理同步测试事件
     *
     * @param event 同步测试事件
     */
    @EventListener
    public void handle(TestSyncEvent event) {
        if (ObjectUtil.hasEmpty(event.eventId(), event.businessId())) {
            log.warn("同步测试事件参数不完整，事件内容：{}", event);
            return;
        }

        log.info("处理同步测试事件，事件ID：{}，业务ID：{}", event.eventId(), event.businessId());
        handleCount.incrementAndGet();
    }

    /**
     * 获取处理次数
     *
     * @return 处理次数
     */
    public int getHandleCount() {
        return handleCount.get();
    }

    /**
     * 重置处理次数
     */
    public void reset() {
        handleCount.set(0);
    }
}
```

同步事件测试类发布事件后立即断言执行次数。

文件位置：`src/test/java/io/github/atengk/event/EventSyncTest.java`

```java
package io.github.atengk.event;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.event.test.TestSyncEvent;
import io.github.atengk.event.test.TestSyncEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 同步事件测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootTest
class EventSyncTest {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private TestSyncEventListener testSyncEventListener;

    /**
     * 初始化测试数据
     */
    @BeforeEach
    void setUp() {
        testSyncEventListener.reset();
    }

    /**
     * 测试同步事件发布后监听器立即执行
     */
    @Test
    void shouldHandleSyncEventImmediately() {
        applicationEventPublisher.publishEvent(new TestSyncEvent(
                IdUtil.fastSimpleUUID(),
                1001L,
                LocalDateTime.now()
        ));

        assertThat(testSyncEventListener.getHandleCount()).isEqualTo(1);
    }
}
```

同步事件测试的重点是“发布后立即可见”。如果这个测试需要等待，说明监听器可能已经被异步化，或者事件处理链路中存在异步执行逻辑。

### 异步事件测试

异步事件测试不能在发布事件后立即断言结果，因为监听器在其他线程中执行。常见做法是使用 `CountDownLatch`、`Awaitility`、轮询等待或测试专用状态对象等待异步执行完成。

下面示例使用 `CountDownLatch` 验证异步监听器最终执行完成，不额外引入第三方测试依赖。

测试专用异步事件对象。

文件位置：`src/test/java/io/github/atengk/event/test/TestAsyncEvent.java`

```java
package io.github.atengk.event.test;

import java.time.LocalDateTime;

/**
 * 测试异步事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record TestAsyncEvent(
        String eventId,
        Long businessId,
        LocalDateTime occurredTime
) {
}
```

测试专用异步监听器使用 `CountDownLatch` 标记执行完成。

文件位置：`src/test/java/io/github/atengk/event/test/TestAsyncEventListener.java`

```java
package io.github.atengk.event.test;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试异步事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class TestAsyncEventListener {

    private volatile CountDownLatch countDownLatch = new CountDownLatch(1);

    private final AtomicInteger handleCount = new AtomicInteger();

    /**
     * 处理异步测试事件
     *
     * @param event 异步测试事件
     */
    @Async("eventTaskExecutor")
    @EventListener
    public void handle(TestAsyncEvent event) {
        if (ObjectUtil.hasEmpty(event.eventId(), event.businessId())) {
            log.warn("异步测试事件参数不完整，事件内容：{}", event);
            countDownLatch.countDown();
            return;
        }

        log.info("处理异步测试事件，事件ID：{}，业务ID：{}，线程：{}",
                event.eventId(), event.businessId(), Thread.currentThread().getName());

        handleCount.incrementAndGet();
        countDownLatch.countDown();
    }

    /**
     * 等待异步事件执行完成
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否执行完成
     * @throws InterruptedException 等待中断异常
     */
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return countDownLatch.await(timeout, unit);
    }

    /**
     * 获取处理次数
     *
     * @return 处理次数
     */
    public int getHandleCount() {
        return handleCount.get();
    }

    /**
     * 重置测试状态
     */
    public void reset() {
        countDownLatch = new CountDownLatch(1);
        handleCount.set(0);
    }
}
```

异步事件测试类发布事件后等待监听器完成。

文件位置：`src/test/java/io/github/atengk/event/EventAsyncTest.java`

```java
package io.github.atengk.event;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.event.test.TestAsyncEvent;
import io.github.atengk.event.test.TestAsyncEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 异步事件测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootTest
class EventAsyncTest {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private TestAsyncEventListener testAsyncEventListener;

    /**
     * 初始化测试数据
     */
    @BeforeEach
    void setUp() {
        testAsyncEventListener.reset();
    }

    /**
     * 测试异步事件最终被监听器处理
     *
     * @throws InterruptedException 等待中断异常
     */
    @Test
    void shouldHandleAsyncEventEventually() throws InterruptedException {
        applicationEventPublisher.publishEvent(new TestAsyncEvent(
                IdUtil.fastSimpleUUID(),
                2001L,
                LocalDateTime.now()
        ));

        boolean completed = testAsyncEventListener.await(3, TimeUnit.SECONDS);

        assertThat(completed).isTrue();
        assertThat(testAsyncEventListener.getHandleCount()).isEqualTo(1);
    }
}
```

异步事件测试的关键是设置合理超时时间。超时时间过短会导致测试不稳定，过长会拖慢测试反馈。通常本地单元测试可以设置 2 到 5 秒，集成测试可以根据实际任务耗时适当调整。

### 事务事件测试

事务事件测试用于验证 `@TransactionalEventListener` 在事务提交或回滚时的行为。最常见的验证目标是：事务提交后监听器执行，事务回滚后提交监听器不执行。

测试专用事务事件对象。

文件位置：`src/test/java/io/github/atengk/event/test/TestTransactionEvent.java`

```java
package io.github.atengk.event.test;

import java.time.LocalDateTime;

/**
 * 测试事务事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record TestTransactionEvent(
        String eventId,
        Long businessId,
        LocalDateTime occurredTime
) {
}
```

事务事件测试服务在事务中发布事件，并提供提交和回滚两个方法。

文件位置：`src/test/java/io/github/atengk/event/test/TestTransactionEventService.java`

```java
package io.github.atengk.event.test;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 测试事务事件服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestTransactionEventService {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 提交事务并发布事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishAndCommit() {
        TestTransactionEvent event = new TestTransactionEvent(
                IdUtil.fastSimpleUUID(),
                3001L,
                LocalDateTime.now()
        );

        applicationEventPublisher.publishEvent(event);
        log.info("事务提交测试事件已发布，事件ID：{}", event.eventId());
    }

    /**
     * 发布事件后回滚事务
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishAndRollback() {
        TestTransactionEvent event = new TestTransactionEvent(
                IdUtil.fastSimpleUUID(),
                3002L,
                LocalDateTime.now()
        );

        applicationEventPublisher.publishEvent(event);
        log.info("事务回滚测试事件已发布，事件ID：{}", event.eventId());

        throw new IllegalStateException("模拟事务回滚");
    }
}
```

事务事件监听器分别统计提交后和回滚后的执行次数。

文件位置：`src/test/java/io/github/atengk/event/test/TestTransactionEventListener.java`

```java
package io.github.atengk.event.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试事务事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class TestTransactionEventListener {

    private final AtomicInteger afterCommitCount = new AtomicInteger();

    private final AtomicInteger afterRollbackCount = new AtomicInteger();

    /**
     * 事务提交后处理事件
     *
     * @param event 测试事务事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAfterCommit(TestTransactionEvent event) {
        log.info("事务提交后处理测试事件，事件ID：{}，业务ID：{}", event.eventId(), event.businessId());
        afterCommitCount.incrementAndGet();
    }

    /**
     * 事务回滚后处理事件
     *
     * @param event 测试事务事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleAfterRollback(TestTransactionEvent event) {
        log.info("事务回滚后处理测试事件，事件ID：{}，业务ID：{}", event.eventId(), event.businessId());
        afterRollbackCount.incrementAndGet();
    }

    /**
     * 获取事务提交后处理次数
     *
     * @return 处理次数
     */
    public int getAfterCommitCount() {
        return afterCommitCount.get();
    }

    /**
     * 获取事务回滚后处理次数
     *
     * @return 处理次数
     */
    public int getAfterRollbackCount() {
        return afterRollbackCount.get();
    }

    /**
     * 重置统计次数
     */
    public void reset() {
        afterCommitCount.set(0);
        afterRollbackCount.set(0);
    }
}
```

事务事件测试类验证提交和回滚两种场景。

文件位置：`src/test/java/io/github/atengk/event/EventTransactionTest.java`

```java
package io.github.atengk.event;

import io.github.atengk.event.test.TestTransactionEventListener;
import io.github.atengk.event.test.TestTransactionEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * 事务事件测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootTest
class EventTransactionTest {

    @Autowired
    private TestTransactionEventService testTransactionEventService;

    @Autowired
    private TestTransactionEventListener testTransactionEventListener;

    /**
     * 初始化测试数据
     */
    @BeforeEach
    void setUp() {
        testTransactionEventListener.reset();
    }

    /**
     * 测试事务提交后执行 AFTER_COMMIT 监听器
     */
    @Test
    void shouldHandleEventAfterCommit() {
        testTransactionEventService.publishAndCommit();

        assertThat(testTransactionEventListener.getAfterCommitCount()).isEqualTo(1);
        assertThat(testTransactionEventListener.getAfterRollbackCount()).isEqualTo(0);
    }

    /**
     * 测试事务回滚后执行 AFTER_ROLLBACK 监听器
     */
    @Test
    void shouldHandleEventAfterRollback() {
        Throwable throwable = catchThrowable(() -> testTransactionEventService.publishAndRollback());

        assertThat(throwable).isInstanceOf(IllegalStateException.class);
        assertThat(testTransactionEventListener.getAfterCommitCount()).isEqualTo(0);
        assertThat(testTransactionEventListener.getAfterRollbackCount()).isEqualTo(1);
    }
}
```

事务事件测试需要注意，不建议直接在测试方法上添加 `@Transactional` 后再断言 `AFTER_COMMIT` 监听器，因为测试方法事务通常会在测试结束时回滚，容易导致监听器执行时机与预期不一致。更稳妥的方式是把事务边界放在测试 Service 方法中。

### 业务解耦效果验证

业务解耦效果验证不是单纯验证事件能否执行，而是验证主流程是否已经从扩展流程中解耦。验证重点包括主流程代码是否稳定、扩展逻辑是否可以独立新增、监听器失败是否按预期隔离、日志链路是否可追踪。

可以从以下几个维度检查：

| 验证维度           | 判断标准                           |
| ------------------ | ---------------------------------- |
| 主流程是否简洁     | Service 中只保留核心业务和事件发布 |
| 扩展逻辑是否独立   | 新增通知、日志、回调时不修改主流程 |
| 监听器职责是否单一 | 一个监听器只处理一个明确动作       |
| 失败是否隔离       | 非核心监听器失败不影响主流程       |
| 日志是否可追踪     | 发布日志和监听日志都包含事件 ID    |
| 事务边界是否清晰   | 依赖提交结果的逻辑使用事务事件     |
| 是否可测试         | 主流程和监听器可以分别测试         |

可以用下面的方式验证主流程是否保持解耦。

不推荐主流程：

```java
@Transactional(rollbackFor = Exception.class)
public Long register(String username, String mobile) {
    Long userId = saveUser(username, mobile);

    sendWelcomeMessage(mobile);
    grantNewUserCoupon(userId);
    saveRegisterLog(userId);
    syncCrm(userId);

    return userId;
}
```

推荐主流程：

```java
@Transactional(rollbackFor = Exception.class)
public Long register(String username, String mobile) {
    Long userId = saveUser(username, mobile);

    businessEventPublisher.publish(new UserRegisteredEvent(
            IdUtil.fastSimpleUUID(),
            userId,
            username,
            mobile,
            null,
            "APP",
            LocalDateTime.now()
    ));

    return userId;
}
```

解耦后的扩展逻辑应该通过新增监听器完成，而不是修改主流程。例如新增 CRM 同步逻辑时，只新增监听器。

文件位置：`src/main/java/io/github/atengk/event/listener/UserCrmSyncEventListener.java`

```java
package io.github.atengk.event.listener;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.event.model.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 用户 CRM 同步事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UserCrmSyncEventListener {

    /**
     * 事务提交后异步同步用户到 CRM
     *
     * @param event 用户注册完成事件
     */
    @Async("eventTaskExecutor")
    @TransactionalEventListener
    public void syncUserToCrm(UserRegisteredEvent event) {
        if (ObjectUtil.hasEmpty(event.eventId(), event.userId())) {
            log.warn("用户 CRM 同步事件参数不完整，事件内容：{}", event);
            return;
        }

        try {
            log.info("开始同步用户到 CRM，事件ID：{}，用户ID：{}", event.eventId(), event.userId());

            // crmService.syncUser(event.userId());

            log.info("用户同步 CRM 完成，事件ID：{}，用户ID：{}", event.eventId(), event.userId());
        } catch (Exception e) {
            log.error("用户同步 CRM 失败，事件ID：{}，用户ID：{}", event.eventId(), event.userId(), e);

            // 重要外部同步建议写入失败记录表，后续补偿
            // eventFailRecordService.saveFailRecord(event, e);
        }
    }
}
```

验证业务解耦效果时，可以使用以下检查清单：

```text
1. 新增一个后置业务动作时，是否只需要新增监听器？
2. 删除某个扩展动作时，是否不影响主流程？
3. 监听器失败时，是否符合业务预期？
4. 主流程中是否还直接依赖短信、优惠券、日志、CRM 等扩展服务？
5. 事件对象字段是否稳定、清晰、必要？
6. 事件日志是否能通过 eventId 串联发布和处理过程？
7. 依赖事务提交结果的监听器是否使用 @TransactionalEventListener？
8. 耗时监听器是否使用独立线程池异步执行？
9. 重要监听器是否具备幂等和失败补偿能力？
```

最终落地标准是：主流程只描述核心业务和业务事实，扩展流程通过监听器独立演进；事件链路有日志、有边界、有异常处理、有测试覆盖。这样才能让 Spring Event 真正成为业务解耦工具，而不是把复杂度从 Service 转移到 Listener。
