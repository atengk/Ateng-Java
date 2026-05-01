# 设计模式：观察者模式

观察者模式用于在一个对象状态发生变化时，自动通知多个依赖对象执行后续处理。在 JDK21 和 Spring Boot 3 项目中，观察者模式常用于订单创建后通知、支付成功后异步处理、缓存刷新、消息推送、积分发放、优惠券发放、审计日志、领域事件、Spring 事件监听等场景。

需要注意：观察者模式关注的是“一处变化，多方响应”。如果只是调用一个固定后续流程，普通方法调用即可；如果一个业务事件发生后，需要多个模块各自独立响应，观察者模式更合适。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证观察者模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、ID、日期等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- Lombok，简化日志对象、构造方法等样板代码 -->
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

如果项目使用 Spring Boot 3，建议使用 JDK17 及以上版本。当前文档以 JDK21 为基准，示例代码可以直接用于 Spring Boot 3 项目。

## 核心概念

观察者模式的核心目标是解耦事件发布方和事件订阅方。发布方只负责发布事件，不关心有多少观察者，也不关心观察者具体做什么。

常见角色如下：

| 角色             | 说明                                               |
| ---------------- | -------------------------------------------------- |
| Subject          | 被观察者，也叫主题，负责维护观察者列表并发布通知   |
| Observer         | 观察者，负责响应主题发布的事件                     |
| Event            | 事件对象，携带本次变化的上下文数据                 |
| ConcreteSubject  | 具体主题，业务状态发生变化后通知观察者             |
| ConcreteObserver | 具体观察者，执行通知、日志、积分、优惠券等后续逻辑 |

常见实现方式如下：

| 实现方式                      | 是否推荐         | 适用场景                           |
| ----------------------------- | ---------------- | ---------------------------------- |
| 普通 Java 观察者              | 推荐用于理解原理 | 无 Spring 依赖的事件通知           |
| Spring ApplicationEvent       | 强烈推荐         | Spring Boot 项目内事件发布和监听   |
| `@EventListener`              | 强烈推荐         | 注解式监听事件，代码简洁           |
| 异步事件监听                  | 推荐             | 事件响应耗时较长，不希望阻塞主流程 |
| JDK `Observable` / `Observer` | 不推荐           | 已过时，不建议在新项目使用         |

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring ApplicationEvent + @EventListener > 普通 Java 观察者 > JDK Observable
```

Spring Boot 项目中更推荐使用 Spring 内置事件机制，而不是自己维护观察者列表。

## 普通 Java 观察者

普通 Java 观察者适合不依赖 Spring 容器的事件通知场景。下面以用户注册事件为例，用户注册成功后，需要通知多个观察者处理后续逻辑。

整体流程如下：

```text
用户注册成功 -> 发布用户注册事件 -> 发送欢迎消息 -> 发放新人优惠券 -> 记录审计日志
```

### 文件结构

```text
src/main/java/io/github/atengk/design/observer/simple/
├── UserRegisterEvent.java
├── UserRegisterObserver.java
├── UserRegisterSubject.java
├── WelcomeMessageObserver.java
├── NewUserCouponObserver.java
└── RegisterAuditObserver.java
```

文件位置：`src/main/java/io/github/atengk/design/observer/simple/UserRegisterEvent.java`

下面是用户注册事件对象，用于携带用户注册后的上下文数据。

```java
package io.github.atengk.design.observer.simple;

import java.time.LocalDateTime;

/**
 * 用户注册事件
 *
 * @param eventId      事件ID
 * @param userId       用户ID
 * @param username     用户名
 * @param registerTime 注册时间
 * @author Ateng
 * @since 2026-04-30
 */
public record UserRegisterEvent(
        String eventId,
        Long userId,
        String username,
        LocalDateTime registerTime
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/observer/simple/UserRegisterObserver.java`

下面是用户注册观察者接口，所有注册后续处理器都实现该接口。

```java
package io.github.atengk.design.observer.simple;

/**
 * 用户注册观察者
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface UserRegisterObserver {

    /**
     * 处理用户注册事件
     *
     * @param event 用户注册事件
     */
    void onRegister(UserRegisterEvent event);
}
```

文件位置：`src/main/java/io/github/atengk/design/observer/simple/UserRegisterSubject.java`

下面是用户注册主题对象，负责维护观察者列表，并在用户注册成功后通知所有观察者。

```java
package io.github.atengk.design.observer.simple;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 用户注册主题
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class UserRegisterSubject {

    private final List<UserRegisterObserver> observers = new CopyOnWriteArrayList<>();

    /**
     * 添加观察者
     *
     * @param observer 用户注册观察者
     */
    public void addObserver(UserRegisterObserver observer) {
        if (observer == null) {
            log.warn("添加用户注册观察者失败，观察者为空");
            throw new IllegalArgumentException("观察者不能为空");
        }

        observers.add(observer);
        log.info("添加用户注册观察者成功，当前观察者数量：{}", observers.size());
    }

    /**
     * 移除观察者
     *
     * @param observer 用户注册观察者
     */
    public void removeObserver(UserRegisterObserver observer) {
        if (observer == null) {
            return;
        }

        observers.remove(observer);
        log.info("移除用户注册观察者成功，当前观察者数量：{}", observers.size());
    }

    /**
     * 注册用户并通知观察者
     *
     * @param userId   用户ID
     * @param username 用户名
     */
    public void register(Long userId, String username) {
        validateRegisterParam(userId, username);

        UserRegisterEvent event = new UserRegisterEvent(
                IdUtil.fastSimpleUUID(),
                userId,
                username,
                DateUtil.date().toLocalDateTime()
        );

        log.info("用户注册成功，用户ID：{}，用户名：{}，事件ID：{}", userId, username, event.eventId());
        notifyObservers(event);
    }

    /**
     * 通知所有观察者
     *
     * @param event 用户注册事件
     */
    private void notifyObservers(UserRegisterEvent event) {
        if (CollUtil.isEmpty(observers)) {
            log.warn("用户注册事件无观察者处理，事件ID：{}", event.eventId());
            return;
        }

        for (UserRegisterObserver observer : observers) {
            try {
                observer.onRegister(event);
            } catch (Exception exception) {
                log.warn("用户注册观察者处理失败，事件ID：{}，观察者：{}，异常：{}",
                        event.eventId(), observer.getClass().getSimpleName(), exception.getMessage());
            }
        }
    }

    /**
     * 校验注册参数
     *
     * @param userId   用户ID
     * @param username 用户名
     */
    private void validateRegisterParam(Long userId, String username) {
        if (userId == null || userId <= 0) {
            log.warn("用户注册失败，用户ID不合法，用户ID：{}", userId);
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        if (StrUtil.isBlank(username)) {
            log.warn("用户注册失败，用户名为空");
            throw new IllegalArgumentException("用户名不能为空");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/observer/simple/WelcomeMessageObserver.java`

下面是欢迎消息观察者，用于在用户注册后发送欢迎消息。

```java
package io.github.atengk.design.observer.simple;

import lombok.extern.slf4j.Slf4j;

/**
 * 欢迎消息观察者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class WelcomeMessageObserver implements UserRegisterObserver {

    /**
     * 处理用户注册事件
     *
     * @param event 用户注册事件
     */
    @Override
    public void onRegister(UserRegisterEvent event) {
        log.info("发送欢迎消息，用户ID：{}，用户名：{}", event.userId(), event.username());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/observer/simple/NewUserCouponObserver.java`

下面是新人优惠券观察者，用于在用户注册后发放新人优惠券。

```java
package io.github.atengk.design.observer.simple;

import lombok.extern.slf4j.Slf4j;

/**
 * 新人优惠券观察者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class NewUserCouponObserver implements UserRegisterObserver {

    /**
     * 处理用户注册事件
     *
     * @param event 用户注册事件
     */
    @Override
    public void onRegister(UserRegisterEvent event) {
        log.info("发放新人优惠券，用户ID：{}，事件ID：{}", event.userId(), event.eventId());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/observer/simple/RegisterAuditObserver.java`

下面是注册审计观察者，用于记录用户注册审计日志。

```java
package io.github.atengk.design.observer.simple;

import lombok.extern.slf4j.Slf4j;

/**
 * 注册审计观察者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class RegisterAuditObserver implements UserRegisterObserver {

    /**
     * 处理用户注册事件
     *
     * @param event 用户注册事件
     */
    @Override
    public void onRegister(UserRegisterEvent event) {
        log.info("记录用户注册审计日志，用户ID：{}，注册时间：{}，事件ID：{}",
                event.userId(), event.registerTime(), event.eventId());
    }
}
```

使用方式：

```java
UserRegisterSubject subject = new UserRegisterSubject();

subject.addObserver(new WelcomeMessageObserver());
subject.addObserver(new NewUserCouponObserver());
subject.addObserver(new RegisterAuditObserver());

subject.register(10001L, "Ateng");
```

普通 Java 观察者的优点是结构直观，不依赖框架。缺点是观察者注册、移除、异常隔离、异步处理、事务边界都需要自己管理。

## Spring Boot 观察者

Spring Boot 项目中更推荐使用 Spring 事件机制实现观察者模式。业务服务通过 `ApplicationEventPublisher` 发布事件，监听器通过 `@EventListener` 订阅事件。

下面以订单创建事件为例，订单创建成功后通知多个监听器处理后续逻辑。

整体流程如下：

```text
创建订单 -> 发布订单创建事件 -> 发放积分 -> 发送通知 -> 记录审计日志
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── ObserverApplication.java
├── controller/
│   └── OrderCreateController.java
├── dto/
│   ├── OrderCreateRequest.java
│   └── OrderCreateResponse.java
├── event/
│   └── OrderCreatedEvent.java
├── listener/
│   ├── OrderPointListener.java
│   ├── OrderNoticeListener.java
│   └── OrderAuditListener.java
└── service/
    ├── OrderCreateService.java
    └── impl/
        └── OrderCreateServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/ObserverApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 观察者模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class ObserverApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ObserverApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderCreateRequest.java`

下面是订单创建请求参数对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 订单创建请求
 *
 * @param userId      用户ID
 * @param productId   商品ID
 * @param productName 商品名称
 * @param quantity    购买数量
 * @param unitPrice   商品单价
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderCreateRequest(
        Long userId,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderCreateResponse.java`

下面是订单创建响应结果。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 订单创建响应
 *
 * @param orderNo     订单号
 * @param userId      用户ID
 * @param productName 商品名称
 * @param quantity    购买数量
 * @param totalAmount 订单总金额
 * @param message     结果消息
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderCreateResponse(
        String orderNo,
        Long userId,
        String productName,
        Integer quantity,
        BigDecimal totalAmount,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/event/OrderCreatedEvent.java`

下面是订单创建事件。事件对象只携带后续监听器需要的数据，不建议直接把复杂实体对象暴露给所有监听器。

```java
package io.github.atengk.design.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建事件
 *
 * @param eventId     事件ID
 * @param orderNo     订单号
 * @param userId      用户ID
 * @param productId   商品ID
 * @param productName 商品名称
 * @param quantity    购买数量
 * @param totalAmount 订单总金额
 * @param createTime  创建时间
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderCreatedEvent(
        String eventId,
        String orderNo,
        Long userId,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal totalAmount,
        LocalDateTime createTime
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/service/OrderCreateService.java`

下面是订单创建服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.OrderCreateRequest;
import io.github.atengk.design.dto.OrderCreateResponse;

/**
 * 订单创建服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderCreateService {

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单创建响应
     */
    OrderCreateResponse createOrder(OrderCreateRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/OrderCreateServiceImpl.java`

下面是订单创建服务实现。订单创建成功后，通过 `ApplicationEventPublisher` 发布订单创建事件。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.OrderCreateRequest;
import io.github.atengk.design.dto.OrderCreateResponse;
import io.github.atengk.design.event.OrderCreatedEvent;
import io.github.atengk.design.service.OrderCreateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 订单创建服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreateServiceImpl implements OrderCreateService {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单创建响应
     */
    @Override
    public OrderCreateResponse createOrder(OrderCreateRequest request) {
        validateRequest(request);

        String orderNo = "ORDER" + IdUtil.getSnowflakeNextId();
        BigDecimal totalAmount = NumberUtil.mul(request.unitPrice(), BigDecimal.valueOf(request.quantity()))
                .setScale(2, RoundingMode.HALF_UP);

        log.info("创建订单成功，订单号：{}，用户ID：{}，商品ID：{}，金额：{}",
                orderNo, request.userId(), request.productId(), totalAmount);

        OrderCreatedEvent event = new OrderCreatedEvent(
                IdUtil.fastSimpleUUID(),
                orderNo,
                request.userId(),
                request.productId(),
                request.productName(),
                request.quantity(),
                totalAmount,
                DateUtil.date().toLocalDateTime()
        );

        eventPublisher.publishEvent(event);
        log.info("发布订单创建事件，事件ID：{}，订单号：{}", event.eventId(), orderNo);

        return new OrderCreateResponse(
                orderNo,
                request.userId(),
                request.productName(),
                request.quantity(),
                totalAmount,
                "创建成功"
        );
    }

    /**
     * 校验订单创建请求
     *
     * @param request 订单创建请求
     */
    private void validateRequest(OrderCreateRequest request) {
        if (request == null) {
            log.warn("创建订单失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (request.userId() == null || request.userId() <= 0) {
            log.warn("创建订单失败，用户ID不合法，用户ID：{}", request.userId());
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        if (request.productId() == null || request.productId() <= 0) {
            log.warn("创建订单失败，商品ID不合法，商品ID：{}", request.productId());
            throw new IllegalArgumentException("商品ID必须大于0");
        }

        if (StrUtil.isBlank(request.productName())) {
            log.warn("创建订单失败，商品名称为空");
            throw new IllegalArgumentException("商品名称不能为空");
        }

        if (request.quantity() == null || request.quantity() <= 0) {
            log.warn("创建订单失败，购买数量不合法，购买数量：{}", request.quantity());
            throw new IllegalArgumentException("购买数量必须大于0");
        }

        if (request.unitPrice() == null || request.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("创建订单失败，商品单价不合法，商品单价：{}", request.unitPrice());
            throw new IllegalArgumentException("商品单价必须大于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/listener/OrderPointListener.java`

下面是订单积分监听器，用于在订单创建后发放积分。

```java
package io.github.atengk.design.listener;

import io.github.atengk.design.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 订单积分监听器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderPointListener {

    /**
     * 处理订单创建事件
     *
     * @param event 订单创建事件
     */
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        int point = event.totalAmount().intValue();
        log.info("发放订单积分，事件ID：{}，订单号：{}，用户ID：{}，积分：{}",
                event.eventId(), event.orderNo(), event.userId(), point);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/listener/OrderNoticeListener.java`

下面是订单通知监听器，用于在订单创建后发送通知。

```java
package io.github.atengk.design.listener;

import io.github.atengk.design.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 订单通知监听器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderNoticeListener {

    /**
     * 处理订单创建事件
     *
     * @param event 订单创建事件
     */
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("发送订单创建通知，事件ID：{}，订单号：{}，用户ID：{}，商品：{}",
                event.eventId(), event.orderNo(), event.userId(), event.productName());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/listener/OrderAuditListener.java`

下面是订单审计监听器，用于记录订单创建审计日志。

```java
package io.github.atengk.design.listener;

import io.github.atengk.design.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 订单审计监听器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderAuditListener {

    /**
     * 处理订单创建事件
     *
     * @param event 订单创建事件
     */
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("记录订单创建审计日志，事件ID：{}，订单号：{}，创建时间：{}",
                event.eventId(), event.orderNo(), event.createTime());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/OrderCreateController.java`

下面是订单创建接口，用于验证 Spring 事件监听效果。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.OrderCreateRequest;
import io.github.atengk.design.dto.OrderCreateResponse;
import io.github.atengk.design.service.OrderCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 订单创建控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/observer/order")
public class OrderCreateController {

    private final OrderCreateService orderCreateService;

    /**
     * 创建订单
     *
     * @param userId      用户ID
     * @param productId   商品ID
     * @param productName 商品名称
     * @param quantity    购买数量
     * @param unitPrice   商品单价
     * @return 订单创建响应
     */
    @PostMapping("/create")
    public OrderCreateResponse createOrder(@RequestParam Long userId,
                                           @RequestParam Long productId,
                                           @RequestParam String productName,
                                           @RequestParam Integer quantity,
                                           @RequestParam BigDecimal unitPrice) {
        OrderCreateRequest request = new OrderCreateRequest(
                userId,
                productId,
                productName,
                quantity,
                unitPrice
        );
        return orderCreateService.createOrder(request);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/observer/order/create?userId=10001&productId=20001&productName=键盘&quantity=2&unitPrice=199.00"
```

可能返回：

```json
{
  "orderNo": "ORDER2019776866538487808",
  "userId": 10001,
  "productName": "键盘",
  "quantity": 2,
  "totalAmount": 398.00,
  "message": "创建成功"
}
```

如果事件监听正常，可以看到多个监听器分别处理同一个订单创建事件。

```text
创建订单成功，订单号：ORDER2019776866538487808，用户ID：10001，商品ID：20001，金额：398.00
发放订单积分，事件ID：f17c7a2a81a44e25a06f7de799f6dd7c，订单号：ORDER2019776866538487808，用户ID：10001，积分：398
发送订单创建通知，事件ID：f17c7a2a81a44e25a06f7de799f6dd7c，订单号：ORDER2019776866538487808，用户ID：10001，商品：键盘
记录订单创建审计日志，事件ID：f17c7a2a81a44e25a06f7de799f6dd7c，订单号：ORDER2019776866538487808，创建时间：2026-04-30T10:15:30
发布订单创建事件，事件ID：f17c7a2a81a44e25a06f7de799f6dd7c，订单号：ORDER2019776866538487808
```

需要注意：Spring 事件默认是同步执行。也就是说，`publishEvent` 会等待监听器执行完成后才继续往下执行。

## 异步事件监听

如果事件监听器执行耗时较长，例如发送短信、调用第三方接口、生成报表，不建议阻塞主流程。此时可以开启异步事件监听。

异步事件适合“主流程成功后，后续处理可以延迟完成”的场景。下面在订单通知监听器上添加异步执行。

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── config/
│   └── AsyncConfig.java
└── listener/
    └── AsyncOrderNoticeListener.java
```

文件位置：`src/main/java/io/github/atengk/design/config/AsyncConfig.java`

下面是异步线程池配置。`@EnableAsync` 用于开启 Spring 异步方法能力。

```java
package io.github.atengk.design.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig {

    /**
     * 创建订单事件线程池
     *
     * @return 订单事件线程池
     */
    @Bean("orderEventExecutor")
    public Executor orderEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("order-event-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("初始化订单事件线程池完成");
        return executor;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/listener/AsyncOrderNoticeListener.java`

下面是异步订单通知监听器。它会在指定线程池中执行，不阻塞订单创建主流程。

```java
package io.github.atengk.design.listener;

import io.github.atengk.design.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 异步订单通知监听器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class AsyncOrderNoticeListener {

    /**
     * 异步处理订单创建事件
     *
     * @param event 订单创建事件
     */
    @Async("orderEventExecutor")
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("异步发送订单通知，线程：{}，事件ID：{}，订单号：{}，用户ID：{}",
                Thread.currentThread().getName(), event.eventId(), event.orderNo(), event.userId());
    }
}
```

开启异步后，订单创建接口可以更快返回，但需要注意：异步监听器的异常不会直接抛回主流程。生产环境中应记录异常日志，并根据业务重要性引入重试、补偿或消息队列。

## 扩展一个新观察者

在 Spring Boot 观察者模式中，新增事件响应逻辑通常只需要新增一个监听器类。下面以库存日志监听器为例，订单创建后记录库存变更流水。

文件位置：`src/main/java/io/github/atengk/design/listener/StockLogListener.java`

下面的监听器会被 Spring 自动扫描，并自动订阅 `OrderCreatedEvent`。

```java
package io.github.atengk.design.listener;

import io.github.atengk.design.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 库存日志监听器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class StockLogListener {

    /**
     * 处理订单创建事件
     *
     * @param event 订单创建事件
     */
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("记录库存变更流水，事件ID：{}，订单号：{}，商品ID：{}，扣减数量：{}",
                event.eventId(), event.orderNo(), event.productId(), event.quantity());
    }
}
```

新增该监听器后，`OrderCreateServiceImpl` 不需要修改。订单创建服务仍然只负责创建订单和发布事件，库存日志由新监听器独立完成。

这就是观察者模式的核心价值：发布方稳定，订阅方可扩展。

## 观察者模式和发布订阅的区别

观察者模式和发布订阅模式都能实现“一处发布，多处响应”，但二者耦合程度不同。

| 对比项   | 观察者模式                        | 发布订阅模式                             |
| -------- | --------------------------------- | ---------------------------------------- |
| 通信方式 | 主题直接通知观察者                | 发布方通过中间消息通道通知订阅方         |
| 中间层   | 通常没有独立消息中间件            | 通常有消息队列、事件总线或 Broker        |
| 耦合程度 | 发布方和观察者存在一定关系        | 发布方和订阅方解耦更彻底                 |
| 事务边界 | 多数在同一进程内                  | 通常跨进程、跨服务                       |
| 典型实现 | Spring ApplicationEvent、GUI 事件 | RabbitMQ、Kafka、Redis Pub/Sub、RocketMQ |

简单理解：

```text
观察者模式：对象状态变了，通知当前进程内的观察者。
发布订阅模式：事件发到中间通道，由订阅者自行消费。
```

Spring `ApplicationEvent` 更接近进程内观察者模式。Kafka、RabbitMQ 这类消息队列更接近发布订阅模式。

## 观察者模式和策略模式的区别

观察者模式和策略模式的关注点不同。

| 对比项   | 观察者模式                     | 策略模式                 |
| -------- | ------------------------------ | ------------------------ |
| 核心目的 | 一个事件触发多个响应           | 从多个算法中选择一个执行 |
| 调用关系 | 一对多                         | 一对一                   |
| 关注点   | 通知、扩展、解耦后续处理       | 替换算法、规则分发       |
| 典型场景 | 订单创建后发积分、发通知、审计 | 满减、折扣、新人优惠     |
| 扩展方式 | 新增监听器                     | 新增策略实现             |

简单理解：

```text
观察者模式：发生一件事后，多个模块都要响应。
策略模式：要处理一件事，但只能选择一种算法处理。
```

订单创建后同时发积分、发通知、写审计，适合观察者模式。订单优惠从满减、折扣、新人优惠中选择一种，适合策略模式。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行订单创建接口：

```bash
curl -X POST "http://localhost:8080/observer/order/create?userId=10001&productId=20001&productName=键盘&quantity=2&unitPrice=199.00"
```

如果观察者模式生效，可以看到订单创建后多个监听器依次响应。

正常日志示例：

```text
创建订单成功，订单号：ORDER2019776866538487808，用户ID：10001，商品ID：20001，金额：398.00
发放订单积分，事件ID：f17c7a2a81a44e25a06f7de799f6dd7c，订单号：ORDER2019776866538487808，用户ID：10001，积分：398
发送订单创建通知，事件ID：f17c7a2a81a44e25a06f7de799f6dd7c，订单号：ORDER2019776866538487808，用户ID：10001，商品：键盘
记录订单创建审计日志，事件ID：f17c7a2a81a44e25a06f7de799f6dd7c，订单号：ORDER2019776866538487808，创建时间：2026-04-30T10:15:30
记录库存变更流水，事件ID：f17c7a2a81a44e25a06f7de799f6dd7c，订单号：ORDER2019776866538487808，商品ID：20001，扣减数量：2
发布订单创建事件，事件ID：f17c7a2a81a44e25a06f7de799f6dd7c，订单号：ORDER2019776866538487808
```

执行异常请求：

```bash
curl -X POST "http://localhost:8080/observer/order/create?userId=10001&productId=20001&productName=键盘&quantity=0&unitPrice=199.00"
```

异常日志示例：

```text
创建订单失败，购买数量不合法，购买数量：0
```

此时不会发布 `OrderCreatedEvent`，因此监听器不会执行。

## 注意事项

观察者模式适合解耦后续响应逻辑，但不要把主流程必须强一致完成的逻辑随意丢到观察者中。

例如订单创建时，扣减库存如果必须和订单创建保持强一致，通常不建议简单放到异步监听器中处理。否则订单已经创建成功，但库存扣减失败，会产生业务不一致。

更适合放在观察者中的逻辑：

```text
发送通知
记录审计日志
发放非核心奖励
刷新缓存
同步搜索索引
发送埋点事件
```

不适合随意放在异步观察者中的强一致逻辑：

```text
核心库存扣减
核心资金入账
订单主状态变更
必须同事务提交的数据写入
```

Spring 事件默认同步执行。如果监听器抛出异常，可能影响事件发布方后续流程。

示例：

```java
@EventListener
public void handleOrderCreated(OrderCreatedEvent event) {
    throw new IllegalStateException("监听器处理失败");
}
```

如果不希望某个监听器异常影响主流程，建议在监听器内部捕获异常并记录日志。

```java
@EventListener
public void handleOrderCreated(OrderCreatedEvent event) {
    try {
        log.info("处理订单创建事件，订单号：{}", event.orderNo());
    } catch (Exception exception) {
        log.warn("处理订单创建事件失败，订单号：{}，异常：{}", event.orderNo(), exception.getMessage());
    }
}
```

异步监听器适合耗时但非强一致的后续处理。生产环境中，如果事件不能丢失，不建议只依赖 Spring 本地异步事件。应使用消息队列或事件表。

推荐生产方案：

```text
订单事务提交 -> 写入本地事件表 -> 定时任务或消息中间件投递 -> 消费者处理 -> 失败重试或人工补偿
```

如果使用 `@TransactionalEventListener`，可以让事件监听器在事务提交后执行，避免事务回滚后仍然处理事件。

示例：

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleOrderCreatedAfterCommit(OrderCreatedEvent event) {
    log.info("事务提交后处理订单创建事件，订单号：{}", event.orderNo());
}
```

该方式适合“只有主事务提交成功后，才允许处理后续逻辑”的场景。

## 总结

在 JDK21 和 Spring Boot 3 项目中，观察者模式的实践重点是通过事件解耦业务发布方和多个响应方。

普通 Java 观察者适合理解原理或无框架场景。Spring Boot 项目中更推荐使用 `ApplicationEventPublisher` 发布事件，并使用 `@EventListener` 或 `@TransactionalEventListener` 监听事件。对于订单创建、支付成功、用户注册、文件导入完成、审批通过等场景，观察者模式可以有效减少主流程对后续模块的直接依赖。

观察者模式不是为了替代所有方法调用，而是为了处理“一个业务事件发生后，多个模块需要独立响应”的场景。对于强一致、必须同步完成的核心链路，需要谨慎使用异步观察者，并结合事务、消息队列、重试和补偿机制保证业务可靠性。
