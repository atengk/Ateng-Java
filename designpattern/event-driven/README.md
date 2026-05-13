# 事件驱动模式

事件驱动模式是 Spring Boot 项目中常用的工程实践模式，适合把业务主流程和后续扩展动作解耦。当前设计模式文档体系中，`event-driven` 属于 **Spring Boot 实战补充模式**，其核心作用是“用事件解耦业务主流程和扩展动作”。

在实际项目中，事件驱动模式常用于订单创建后发送通知、用户注册后初始化数据、支付成功后生成账单、文件上传后触发解析、业务状态变更后记录审计日志等场景。

## 基础配置

本示例基于 **JDK 21 + Spring Boot 3**，使用 Spring 内置事件机制实现事件发布、事件监听、异步消费和事务提交后触发。示例业务以“订单创建后发送通知和记录日志”为场景。

### 项目依赖

文件位置：`pom.xml`

下面配置 Spring Web、Validation、Lombok 和 Hutool，满足接口、参数校验、日志对象和工具类使用需求。

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Validation：用于请求参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Spring AOP：@Async 等异步能力常和 AOP 代理配合使用 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>

    <!-- Hutool：常用工具类，简化字符串、集合、对象、日期等处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Lombok：减少 Getter、Setter、构造器等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 应用配置

文件位置：`src/main/resources/application.yml`

这里配置应用名称和事件异步线程池参数。线程池参数用于控制异步监听器的并发执行能力。

```yaml
spring:
  application:
    name: design-pattern-event-driven

server:
  port: 8080

event:
  async:
    core-pool-size: 4      # 核心线程数，适合轻量级事件监听任务
    max-pool-size: 8       # 最大线程数，避免高峰期任务堆积
    queue-capacity: 200    # 队列容量，控制事件堆积上限
    thread-name-prefix: event-task-
```

### 文件结构

本示例按 Controller、Service、Event、Listener、Config、DTO、VO 分层组织，便于在真实 Spring Boot 项目中直接迁移。

```text
src/main/java/io/github/atengk/eventdriven
├── EventDrivenApplication.java
├── config
│   └── EventAsyncConfig.java
├── controller
│   └── OrderController.java
├── dto
│   └── OrderCreateRequest.java
├── event
│   └── OrderCreatedEvent.java
├── listener
│   ├── OrderAuditLogListener.java
│   └── OrderNotifyListener.java
├── service
│   ├── OrderService.java
│   └── impl
│       └── OrderServiceImpl.java
└── vo
    └── OrderCreateResponse.java
```

## 模式说明

事件驱动模式的核心是：业务对象完成关键动作后，不直接调用所有后续处理逻辑，而是发布一个事件，由多个监听器分别处理自己的职责。

在订单创建场景中，主流程只负责创建订单并发布 `OrderCreatedEvent`。发送短信、发送站内信、记录审计日志、同步搜索索引等动作由监听器完成。

这种结构可以把代码从下面这种强耦合形式：

```text
创建订单
 -> 发送短信
 -> 发送站内信
 -> 写审计日志
 -> 同步搜索索引
 -> 推送统计数据
```

调整为：

```text
创建订单
 -> 发布订单创建事件

监听器 A：发送通知
监听器 B：记录日志
监听器 C：同步索引
监听器 D：推送统计
```

主流程不再关心后续扩展动作有多少个，也不需要在新增动作时频繁修改核心业务代码。

## 核心代码

这一节给出事件驱动模式的完整关键代码。示例使用 Spring 内置的 `ApplicationEventPublisher` 发布事件，使用 `@EventListener` 消费事件，使用 `@Async` 支持异步监听。

### 启动类

文件位置：`src/main/java/io/github/atengk/eventdriven/EventDrivenApplication.java`

启动类用于启动 Spring Boot 应用，并开启异步方法支持。

```java
package io.github.atengk.eventdriven;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 事件驱动模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@EnableAsync
@SpringBootApplication
public class EventDrivenApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventDrivenApplication.class, args);
    }

}
```

### 异步线程池配置

文件位置：`src/main/java/io/github/atengk/eventdriven/config/EventAsyncConfig.java`

该配置类用于声明事件监听器使用的异步线程池，避免监听器任务直接占用 Web 请求线程。

```java
package io.github.atengk.eventdriven.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 事件异步线程池配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "event.async")
@Data
public class EventAsyncConfig {

    private Integer corePoolSize = 4;

    private Integer maxPoolSize = 8;

    private Integer queueCapacity = 200;

    private String threadNamePrefix = "event-task-";

    /**
     * 创建事件监听器异步线程池
     *
     * @return 异步执行器
     */
    @Bean("eventTaskExecutor")
    public Executor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("事件异步线程池初始化完成，corePoolSize={}，maxPoolSize={}，queueCapacity={}",
                corePoolSize, maxPoolSize, queueCapacity);
        return executor;
    }

}
```

### 请求 DTO

文件位置：`src/main/java/io/github/atengk/eventdriven/dto/OrderCreateRequest.java`

该 DTO 用于接收创建订单请求，并通过注解完成基础参数校验。

```java
package io.github.atengk.eventdriven.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 创建订单请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record OrderCreateRequest(

        @NotBlank(message = "用户ID不能为空")
        String userId,

        @NotBlank(message = "商品名称不能为空")
        String productName,

        @NotNull(message = "订单金额不能为空")
        @Min(value = 1, message = "订单金额必须大于0")
        BigDecimal amount

) {
}
```

### 响应 VO

文件位置：`src/main/java/io/github/atengk/eventdriven/vo/OrderCreateResponse.java`

该 VO 用于返回订单创建结果，实际项目中可以扩展支付状态、订单状态、创建时间等字段。

```java
package io.github.atengk.eventdriven.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建订单响应结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record OrderCreateResponse(

        String orderId,

        String userId,

        String productName,

        BigDecimal amount,

        LocalDateTime createTime

) {
}
```

### 订单创建事件

文件位置：`src/main/java/io/github/atengk/eventdriven/event/OrderCreatedEvent.java`

该事件对象用于承载订单创建后的核心业务数据，监听器只依赖事件，不直接依赖订单服务实现。

```java
package io.github.atengk.eventdriven.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record OrderCreatedEvent(

        String orderId,

        String userId,

        String productName,

        BigDecimal amount,

        LocalDateTime createTime

) {
}
```

### Service 接口

文件位置：`src/main/java/io/github/atengk/eventdriven/service/OrderService.java`

该接口定义订单创建能力，Controller 只依赖接口，不依赖具体实现。

```java
package io.github.atengk.eventdriven.service;

import io.github.atengk.eventdriven.dto.OrderCreateRequest;
import io.github.atengk.eventdriven.vo.OrderCreateResponse;

/**
 * 订单业务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderService {

    /**
     * 创建订单
     *
     * @param request 创建订单请求
     * @return 创建订单响应
     */
    OrderCreateResponse createOrder(OrderCreateRequest request);

}
```

### Service 实现

文件位置：`src/main/java/io/github/atengk/eventdriven/service/impl/OrderServiceImpl.java`

该实现类完成订单主流程，并在主流程完成后发布订单创建事件。

```java
package io.github.atengk.eventdriven.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.eventdriven.dto.OrderCreateRequest;
import io.github.atengk.eventdriven.event.OrderCreatedEvent;
import io.github.atengk.eventdriven.service.OrderService;
import io.github.atengk.eventdriven.vo.OrderCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 订单业务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 创建订单
     *
     * @param request 创建订单请求
     * @return 创建订单响应
     */
    @Override
    public OrderCreateResponse createOrder(OrderCreateRequest request) {
        String orderId = StrUtil.format("ORD-{}", UUID.fastUUID().toString(true));
        LocalDateTime createTime = LocalDateTime.now();

        log.info("开始创建订单，userId={}，productName={}，amount={}",
                request.userId(), request.productName(), request.amount());

        // 示例中省略数据库保存逻辑，真实项目应在这里保存订单主表、订单明细等数据。
        OrderCreateResponse response = new OrderCreateResponse(
                orderId,
                request.userId(),
                request.productName(),
                request.amount(),
                createTime
        );

        OrderCreatedEvent event = new OrderCreatedEvent(
                response.orderId(),
                response.userId(),
                response.productName(),
                response.amount(),
                response.createTime()
        );

        applicationEventPublisher.publishEvent(event);
        log.info("订单创建事件已发布，orderId={}", orderId);

        return response;
    }

}
```

### 通知监听器

文件位置：`src/main/java/io/github/atengk/eventdriven/listener/OrderNotifyListener.java`

该监听器负责处理订单创建后的通知逻辑，例如短信、站内信、邮件或 App 推送。

```java
package io.github.atengk.eventdriven.listener;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.eventdriven.event.OrderCreatedEvent;
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
public class OrderNotifyListener {

    /**
     * 处理订单创建通知
     *
     * @param event 订单创建事件
     */
    @Async("eventTaskExecutor")
    @EventListener
    public void handleOrderCreatedNotify(OrderCreatedEvent event) {
        String notifyContent = StrUtil.format(
                "用户 {} 的订单 {} 已创建，商品：{}，金额：{}",
                event.userId(),
                event.orderId(),
                event.productName(),
                event.amount()
        );

        // 示例中使用日志模拟通知发送，真实项目可对接短信、邮件、站内信或 MQ。
        log.info("发送订单创建通知，content={}", notifyContent);
    }

}
```

### 审计日志监听器

文件位置：`src/main/java/io/github/atengk/eventdriven/listener/OrderAuditLogListener.java`

该监听器负责记录订单创建后的审计日志，和通知逻辑相互独立。

```java
package io.github.atengk.eventdriven.listener;

import io.github.atengk.eventdriven.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 订单审计日志事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderAuditLogListener {

    /**
     * 记录订单创建审计日志
     *
     * @param event 订单创建事件
     */
    @Order(1)
    @EventListener
    public void recordOrderCreatedLog(OrderCreatedEvent event) {
        log.info("记录订单创建审计日志，orderId={}，userId={}，productName={}，amount={}，createTime={}",
                event.orderId(),
                event.userId(),
                event.productName(),
                event.amount(),
                event.createTime());
    }

}
```

### Controller 接口

文件位置：`src/main/java/io/github/atengk/eventdriven/controller/OrderController.java`

该 Controller 对外提供创建订单接口，调用 Service 后返回创建结果。

```java
package io.github.atengk.eventdriven.controller;

import io.github.atengk.eventdriven.dto.OrderCreateRequest;
import io.github.atengk.eventdriven.service.OrderService;
import io.github.atengk.eventdriven.vo.OrderCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     *
     * @param request 创建订单请求
     * @return 创建订单响应
     */
    @PostMapping
    public OrderCreateResponse createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return orderService.createOrder(request);
    }

}
```

## 事务提交后发布事件

如果事件监听器依赖数据库中的最终数据，推荐使用 `@TransactionalEventListener`，并设置为事务提交后执行。这样可以避免监听器在事务未提交时读取不到数据，或者事务回滚后仍然执行扩展动作。

适合事务提交后触发的动作包括：

| 场景                   | 推荐原因                   |
| ---------------------- | -------------------------- |
| 支付成功后发送通知     | 避免支付事务回滚后误发通知 |
| 订单创建后同步搜索索引 | 确保数据库中已有可查询订单 |
| 用户注册后发放优惠券   | 避免注册失败但优惠券已发放 |
| 审批通过后推送消息     | 避免审批事务回滚后误推送   |

文件位置：`src/main/java/io/github/atengk/eventdriven/listener/OrderAfterCommitListener.java`

该监听器只会在事务成功提交后处理事件，更适合真实业务中的外部通知、索引同步和数据派生任务。

```java
package io.github.atengk.eventdriven.listener;

import io.github.atengk.eventdriven.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

/**
 * 订单事务提交后事件监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderAfterCommitListener {

    /**
     * 事务提交后同步订单扩展数据
     *
     * @param event 订单创建事件
     */
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void syncAfterCommit(OrderCreatedEvent event) {
        log.info("事务提交后处理订单创建事件，orderId={}，userId={}",
                event.orderId(), event.userId());

        // 真实项目中可在这里同步搜索索引、发送 MQ、调用外部系统。
    }

}
```

使用 `@TransactionalEventListener` 时，发布事件的方法通常需要运行在事务中。例如：

```java
@Transactional(rollbackFor = Exception.class)
@Override
public OrderCreateResponse createOrder(OrderCreateRequest request) {
    // 1. 保存订单
    // 2. 发布事件
    // 3. 方法正常结束后提交事务
    // 4. AFTER_COMMIT 监听器开始执行
}
```

如果没有事务上下文，`@TransactionalEventListener` 默认不会执行。需要无事务也执行时，可以设置：

```java
@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)
```

但真实项目中不建议随意开启 `fallbackExecution`，否则监听器的执行语义会变得不清晰。

## 使用方式

本示例提供一个创建订单接口。调用接口后，订单主流程会立即返回创建结果，通知监听器会异步执行，审计日志监听器会同步执行。

接口信息如下：

| 项目         | 内容                       |
| ------------ | -------------------------- |
| 请求路径     | `/orders`                  |
| 请求方法     | `POST`                     |
| Content-Type | `application/json`         |
| 主要作用     | 创建订单并发布订单创建事件 |

请求示例：

```bash
curl -X POST 'http://localhost:8080/orders' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "10001",
    "productName": "Spring Boot 设计模式课程",
    "amount": 199.00
  }'
```

响应示例：

```json
{
  "orderId": "ORD-7f4f6d8d6f6d4a8b8f0c7d2e4c9a1234",
  "userId": "10001",
  "productName": "Spring Boot 设计模式课程",
  "amount": 199.00,
  "createTime": "2026-05-13T10:30:00"
}
```

控制台日志示例：

```text
开始创建订单，userId=10001，productName=Spring Boot 设计模式课程，amount=199.00
记录订单创建审计日志，orderId=ORD-xxx，userId=10001，productName=Spring Boot 设计模式课程，amount=199.00，createTime=2026-05-13T10:30:00
订单创建事件已发布，orderId=ORD-xxx
发送订单创建通知，content=用户 10001 的订单 ORD-xxx 已创建，商品：Spring Boot 设计模式课程，金额：199.00
```

## 验证方式

可以通过接口响应和日志顺序验证事件驱动模式是否生效。

启动项目：

```bash
mvn spring-boot:run
```

调用创建订单接口：

```bash
curl -X POST 'http://localhost:8080/orders' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "10001",
    "productName": "Spring Boot 设计模式课程",
    "amount": 199.00
  }'
```

验证点：

```text
1. 接口能够正常返回 orderId。
2. OrderServiceImpl 中出现“订单创建事件已发布”日志。
3. OrderAuditLogListener 中出现“记录订单创建审计日志”日志。
4. OrderNotifyListener 中出现“发送订单创建通知”日志。
5. 异步监听器日志线程名应包含 event-task- 前缀。
```

如果异步监听器没有执行，重点检查：

```text
1. 启动类是否添加 @EnableAsync。
2. 监听方法是否添加 @Async("eventTaskExecutor")。
3. 线程池 Bean 名称是否为 eventTaskExecutor。
4. 监听器类是否交给 Spring 管理，例如是否添加 @Component。
5. 事件对象类型是否和监听方法参数一致。
```

## 适用场景

事件驱动模式适合主流程稳定、扩展动作较多、后续动作之间相互独立的业务场景。

常见适用场景：

```text
用户注册成功后：
- 发送欢迎短信
- 初始化用户配置
- 发放新人优惠券
- 记录注册审计日志

订单创建成功后：
- 发送订单通知
- 记录操作日志
- 同步搜索索引
- 推送数据统计

支付成功后：
- 修改订单状态
- 生成账单流水
- 发放权益
- 发送支付成功通知

文件上传完成后：
- 解析文件内容
- 生成缩略图
- 扫描安全风险
- 发送处理结果通知
```

## 不适用场景

事件驱动模式不是所有业务都需要使用。对于简单、强一致、步骤之间有明确返回值依赖的业务，直接调用会更清晰。

不建议使用的场景：

```text
1. 后续步骤必须立即返回结果给主流程。
2. 多个步骤之间存在严格的输入输出依赖。
3. 业务非常简单，引入事件只会增加理解成本。
4. 需要强事务一致性，但没有补偿、重试、幂等等配套设计。
5. 调用链较短，直接调用已经足够清晰。
```

## 和观察者模式的区别

事件驱动模式和观察者模式非常接近，但二者关注点不同。观察者模式偏对象设计，事件驱动模式偏工程实践和架构解耦；这也是当前设计模式说明中强调的区别之一。

| 对比项           | 观察者模式                              | 事件驱动模式                                      |
| ---------------- | --------------------------------------- | ------------------------------------------------- |
| 关注层面         | 对象之间的通知关系                      | 应用内或系统间的事件流转                          |
| 常见形式         | Subject / Observer                      | Event / Publisher / Listener                      |
| Spring Boot 落地 | `ApplicationListener`、`@EventListener` | `ApplicationEventPublisher`、`@EventListener`、MQ |
| 适用范围         | 对象状态变化通知                        | 业务解耦、异步处理、跨模块协作                    |
| 扩展方式         | 新增观察者                              | 新增监听器或消费者                                |

简单理解：

```text
观察者模式是事件驱动模式的设计基础之一。
事件驱动模式是观察者思想在 Spring Boot 项目中的工程化落地方式。
```

## 项目落地建议

在 Spring Boot 项目中使用事件驱动模式时，应优先保证事件语义清晰、监听器职责单一、异常处理明确。

建议：

```text
1. 事件命名使用过去式，例如 OrderCreatedEvent、UserRegisteredEvent、PaymentSucceededEvent。
2. 事件对象只放监听器需要的关键数据，避免直接暴露复杂领域对象。
3. 主流程不要依赖监听器返回值。
4. 监听器之间不要互相调用。
5. 异步监听器要配置独立线程池。
6. 事务相关事件优先使用 @TransactionalEventListener。
7. 重要外部调用要设计重试、幂等和失败补偿。
8. 高频核心事件可以考虑升级为 MQ，例如 Kafka、RabbitMQ、RocketMQ。
```

## 常见问题

### 监听器异常会影响主流程吗

如果是同步监听器，监听器异常会向上传播，可能影响主流程。

如果是异步监听器，异常通常不会直接影响接口返回，但需要做好日志记录、告警、重试或补偿，否则容易出现“主流程成功，扩展动作静默失败”的问题。

### 什么时候用 Spring Event，什么时候用 MQ

Spring Event 更适合单体应用或单个服务内部的模块解耦。MQ 更适合跨服务、跨系统、削峰填谷、可靠投递和消费重试。

| 场景             | 推荐方式                  |
| ---------------- | ------------------------- |
| 单体项目内部解耦 | Spring Event              |
| 服务内部轻量异步 | Spring Event + `@Async`   |
| 跨微服务通信     | MQ                        |
| 高可靠事件投递   | MQ + 事务消息或本地消息表 |
| 需要消费重试     | MQ                        |
| 需要削峰填谷     | MQ                        |

### 事件对象应该放实体对象吗

一般不建议直接把数据库实体对象放到事件中。更推荐放业务标识和必要快照字段，例如 `orderId`、`userId`、`amount`、`createTime`。

原因：

```text
1. 降低监听器对实体结构的依赖。
2. 避免实体字段过多导致事件过重。
3. 避免懒加载、脏数据、事务边界等问题。
4. 事件语义更稳定，后续扩展更安全。
```

## 总结

事件驱动模式适合在 Spring Boot 项目中解耦主流程和扩展动作。它通过事件发布和事件监听，把“核心业务必须做的事情”和“业务完成后可以扩展的事情”拆开。

在真实项目中，可以按复杂度分层选择：

```text
简单模块解耦：ApplicationEventPublisher + @EventListener
需要异步处理：@EventListener + @Async + 独立线程池
依赖事务提交：@TransactionalEventListener(phase = AFTER_COMMIT)
跨服务事件通信：MQ + 幂等 + 重试 + 补偿
```

对于订单创建、用户注册、支付成功、审批通过、文件上传完成等业务，事件驱动模式可以明显降低代码耦合，让后续扩展动作更加独立、清晰、可测试。
