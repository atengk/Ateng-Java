# 观察者模式

观察者模式用于在一个对象状态发生变化时，自动通知多个依赖对象执行各自逻辑。
在 Spring Boot 项目中，观察者模式常用于业务事件通知、订单状态变更、支付成功后续处理、缓存刷新、消息通知、日志审计、数据统计、异步任务触发等场景。

本文以“订单支付成功后的多业务订阅处理”为例。订单支付成功后，系统需要扣减库存、发放积分、核销优惠券、发送通知、记录统计。使用观察者模式后，支付主流程只负责发布“订单已支付事件”，后续业务由多个监听器独立处理。

## 适用场景

观察者模式适合处理“一个业务动作发生后，需要触发多个后续动作”的场景。

订单支付成功后，通常会触发以下业务：

| 观察者       | 职责             |
| ------------ | ---------------- |
| 库存监听器   | 扣减商品库存     |
| 积分监听器   | 给用户发放积分   |
| 优惠券监听器 | 核销已使用优惠券 |
| 通知监听器   | 发送支付成功通知 |
| 统计监听器   | 记录订单支付统计 |

如果这些逻辑全部写在支付 Service 中，支付方法会变得臃肿，并且每新增一个后续动作都要修改支付主流程。观察者模式可以让主流程只发布事件，具体后续动作由监听器独立完成。

## 基础配置

本示例基于 Spring Boot 3，使用 Spring 内置事件机制实现观察者模式。
`ApplicationEventPublisher` 作为事件发布器，`@EventListener` 作为事件监听器。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Validation：用于接口参数基础校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：提供字符串、对象、金额、ID 等常用工具能力 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.29</version>
    </dependency>

    <!-- Lombok：简化 Getter、Setter、构造器、日志对象等代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

建议目录结构如下：

```text
src/main/java/io/github/atengk/pattern/observer
├── ObserverApplication.java
├── common
│   ├── ApiResult.java
│   ├── BizException.java
│   └── GlobalExceptionHandler.java
└── order
    ├── controller
    │   └── OrderPayController.java
    ├── dto
    │   └── OrderPayRequest.java
    ├── event
    │   └── OrderPaidEvent.java
    ├── listener
    │   ├── CouponWriteOffListener.java
    │   ├── OrderStatisticListener.java
    │   ├── PointRewardListener.java
    │   ├── StockDeductListener.java
    │   └── UserNotifyListener.java
    ├── publisher
    │   └── OrderEventPublisher.java
    ├── service
    │   ├── OrderPaymentService.java
    │   └── impl
    │       └── OrderPaymentServiceImpl.java
    └── vo
        └── OrderPayResultVO.java
```

## 核心设计

本示例把观察者模式拆成四个核心角色：

| 角色                | 项目中的类                | 说明                                     |
| ------------------- | ------------------------- | ---------------------------------------- |
| Subject / Publisher | `OrderEventPublisher`     | 事件发布者，负责发布订单支付成功事件     |
| Event               | `OrderPaidEvent`          | 事件对象，保存订单支付成功后的上下文数据 |
| Observer            | `StockDeductListener` 等  | 观察者，监听事件并执行各自业务           |
| Client              | `OrderPaymentServiceImpl` | 支付业务，完成主流程后发布事件           |

执行流程如下：

```text
Controller
  -> OrderPaymentService
    -> 完成订单支付主流程
    -> OrderEventPublisher.publishOrderPaidEvent()
      -> StockDeductListener
      -> PointRewardListener
      -> CouponWriteOffListener
      -> UserNotifyListener
      -> OrderStatisticListener
```

支付服务不直接依赖库存、积分、优惠券、通知和统计服务，而是通过事件发布机制解耦后续动作。

## 公共代码

公共响应、业务异常和全局异常处理用于统一接口返回。实际项目中可以复用已有基础包。

文件位置：`src/main/java/io/github/atengk/pattern/observer/common/ApiResult.java`

```java
package io.github.atengk.pattern.observer.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    private Integer code;

    private String message;

    private T data;

    /**
     * 返回成功结果
     *
     * @param data 响应数据
     * @return 统一响应对象
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 返回失败结果
     *
     * @param message 错误信息
     * @return 统一响应对象
     */
    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(500, message, null);
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/observer/common/BizException.java`

```java
package io.github.atengk.pattern.observer.common;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class BizException extends RuntimeException {

    /**
     * 创建业务异常
     *
     * @param message 异常信息
     */
    public BizException(String message) {
        super(message);
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/observer/common/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.observer.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param exception 业务异常
     * @return 统一响应对象
     */
    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBizException(BizException exception) {
        log.warn("业务处理失败：{}", exception.getMessage());
        return ApiResult.fail(exception.getMessage());
    }

    /**
     * 处理参数校验异常
     *
     * @param exception 参数校验异常
     * @return 统一响应对象
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResult<Void> handleValidException(Exception exception) {
        log.warn("接口参数校验失败：{}", exception.getMessage());
        return ApiResult.fail("请求参数不合法");
    }

    /**
     * 处理请求体解析异常
     *
     * @param exception 请求体解析异常
     * @return 统一响应对象
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Void> handleMessageNotReadableException(HttpMessageNotReadableException exception) {
        log.warn("请求体解析失败：{}", exception.getMessage());
        return ApiResult.fail("请求体格式不正确");
    }

    /**
     * 处理系统异常
     *
     * @param exception 系统异常
     * @return 统一响应对象
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return ApiResult.fail("系统繁忙，请稍后重试");
    }

}
```

## 完整代码

下面给出观察者模式的核心实现。示例使用 Spring 事件机制完成事件发布与监听，实际项目中可以进一步替换为 MQ、领域事件表或事务消息。

文件位置：`src/main/java/io/github/atengk/pattern/observer/ObserverApplication.java`

```java
package io.github.atengk.pattern.observer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 观察者模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class ObserverApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObserverApplication.class, args);
    }

}
```

## 请求对象和响应对象

请求对象用于接收订单支付参数，响应对象用于返回支付结果和事件 ID。

文件位置：`src/main/java/io/github/atengk/pattern/observer/order/dto/OrderPayRequest.java`

```java
package io.github.atengk.pattern.observer.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单支付请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderPayRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;

    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0")
    private BigDecimal payAmount;

    private String couponCode;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/observer/order/vo/OrderPayResultVO.java`

```java
package io.github.atengk.pattern.observer.order.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单支付结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderPayResultVO {

    private String orderNo;

    private String payNo;

    private String eventId;

    private BigDecimal payAmount;

    private String status;

    private String message;

}
```

## 事件对象

事件对象用于承载订单支付成功后的业务上下文。多个观察者会基于这个事件对象执行各自逻辑。

文件位置：`src/main/java/io/github/atengk/pattern/observer/order/event/OrderPaidEvent.java`

```java
package io.github.atengk.pattern.observer.order.event;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单支付成功事件
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
@Builder
public class OrderPaidEvent {

    private final String eventId;

    private final String orderNo;

    private final String payNo;

    private final Long userId;

    private final Long productId;

    private final Integer quantity;

    private final BigDecimal payAmount;

    private final String couponCode;

    private final LocalDateTime paidAt;

}
```

## 事件发布者

事件发布者封装 Spring 的 `ApplicationEventPublisher`。业务服务只调用业务语义明确的方法，不直接散落 `publishEvent` 代码。

文件位置：`src/main/java/io/github/atengk/pattern/observer/order/publisher/OrderEventPublisher.java`

```java
package io.github.atengk.pattern.observer.order.publisher;

import io.github.atengk.pattern.observer.order.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 订单事件发布者
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 发布订单支付成功事件
     *
     * @param event 订单支付成功事件
     */
    public void publishOrderPaidEvent(OrderPaidEvent event) {
        log.info("发布订单支付成功事件，eventId：{}，orderNo：{}，payNo：{}",
                event.getEventId(), event.getOrderNo(), event.getPayNo());

        applicationEventPublisher.publishEvent(event);
    }

}
```

## 支付服务

支付服务完成订单支付主流程后，只发布“订单支付成功事件”。它不直接调用库存、积分、优惠券、通知和统计逻辑。

文件位置：`src/main/java/io/github/atengk/pattern/observer/order/service/OrderPaymentService.java`

```java
package io.github.atengk.pattern.observer.order.service;

import io.github.atengk.pattern.observer.order.dto.OrderPayRequest;
import io.github.atengk.pattern.observer.order.vo.OrderPayResultVO;

/**
 * 订单支付服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderPaymentService {

    /**
     * 支付订单
     *
     * @param request 订单支付请求
     * @return 订单支付结果
     */
    OrderPayResultVO payOrder(OrderPayRequest request);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/observer/order/service/impl/OrderPaymentServiceImpl.java`

```java
package io.github.atengk.pattern.observer.order.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.observer.common.BizException;
import io.github.atengk.pattern.observer.order.dto.OrderPayRequest;
import io.github.atengk.pattern.observer.order.event.OrderPaidEvent;
import io.github.atengk.pattern.observer.order.publisher.OrderEventPublisher;
import io.github.atengk.pattern.observer.order.service.OrderPaymentService;
import io.github.atengk.pattern.observer.order.vo.OrderPayResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单支付服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentServiceImpl implements OrderPaymentService {

    private final OrderEventPublisher orderEventPublisher;

    /**
     * 支付订单
     *
     * @param request 订单支付请求
     * @return 订单支付结果
     */
    @Override
    public OrderPayResultVO payOrder(OrderPayRequest request) {
        checkPayRequest(request);

        String orderNo = "OD" + IdUtil.getSnowflakeNextIdStr();
        String payNo = "PAY" + IdUtil.getSnowflakeNextIdStr();
        String eventId = "EVT" + IdUtil.getSnowflakeNextIdStr();

        log.info("订单支付成功，orderNo：{}，payNo：{}，userId：{}，payAmount：{}",
                orderNo, payNo, request.getUserId(), request.getPayAmount());

        OrderPaidEvent event = OrderPaidEvent.builder()
                .eventId(eventId)
                .orderNo(orderNo)
                .payNo(payNo)
                .userId(request.getUserId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .payAmount(request.getPayAmount())
                .couponCode(request.getCouponCode())
                .paidAt(LocalDateTime.now())
                .build();

        orderEventPublisher.publishOrderPaidEvent(event);

        return OrderPayResultVO.builder()
                .orderNo(orderNo)
                .payNo(payNo)
                .eventId(eventId)
                .payAmount(request.getPayAmount())
                .status("PAID")
                .message("订单支付成功")
                .build();
    }

    /**
     * 校验支付请求
     *
     * @param request 支付请求
     */
    private void checkPayRequest(OrderPayRequest request) {
        if (ObjectUtil.isNull(request.getUserId()) || request.getUserId() <= 0) {
            throw new BizException("用户ID不合法");
        }

        if (ObjectUtil.isNull(request.getProductId()) || request.getProductId() <= 0) {
            throw new BizException("商品ID不合法");
        }

        if (ObjectUtil.isNull(request.getQuantity()) || request.getQuantity() <= 0) {
            throw new BizException("购买数量必须大于0");
        }

        if (ObjectUtil.isNull(request.getPayAmount())
                || NumberUtil.isLessOrEqual(request.getPayAmount(), BigDecimal.ZERO)) {
            throw new BizException("支付金额必须大于0");
        }
    }

}
```

## 观察者监听器

下面的多个监听器都是观察者。它们监听同一个 `OrderPaidEvent`，但各自执行不同业务。

文件位置：`src/main/java/io/github/atengk/pattern/observer/order/listener/StockDeductListener.java`

```java
package io.github.atengk.pattern.observer.order.listener;

import io.github.atengk.pattern.observer.order.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 库存扣减监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class StockDeductListener {

    /**
     * 监听订单支付成功事件并扣减库存
     *
     * @param event 订单支付成功事件
     */
    @Order(10)
    @EventListener
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("开始处理库存扣减，eventId：{}，productId：{}，quantity：{}",
                event.getEventId(), event.getProductId(), event.getQuantity());

        log.info("库存扣减完成，orderNo：{}，productId：{}，quantity：{}",
                event.getOrderNo(), event.getProductId(), event.getQuantity());
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/observer/order/listener/PointRewardListener.java`

```java
package io.github.atengk.pattern.observer.order.listener;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.observer.order.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 积分发放监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class PointRewardListener {

    /**
     * 监听订单支付成功事件并发放积分
     *
     * @param event 订单支付成功事件
     */
    @Order(20)
    @EventListener
    public void onOrderPaid(OrderPaidEvent event) {
        Integer rewardPoints = calculateRewardPoints(event.getPayAmount());

        log.info("积分发放完成，eventId：{}，userId：{}，orderNo：{}，rewardPoints：{}",
                event.getEventId(), event.getUserId(), event.getOrderNo(), rewardPoints);
    }

    /**
     * 计算奖励积分
     *
     * @param payAmount 支付金额
     * @return 奖励积分
     */
    private Integer calculateRewardPoints(BigDecimal payAmount) {
        return NumberUtil.div(payAmount, BigDecimal.TEN).intValue();
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/observer/order/listener/CouponWriteOffListener.java`

```java
package io.github.atengk.pattern.observer.order.listener;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.observer.order.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 优惠券核销监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class CouponWriteOffListener {

    /**
     * 监听订单支付成功事件并核销优惠券
     *
     * @param event 订单支付成功事件
     */
    @Order(30)
    @EventListener
    public void onOrderPaid(OrderPaidEvent event) {
        if (StrUtil.isBlank(event.getCouponCode())) {
            log.info("订单未使用优惠券，无需核销，eventId：{}，orderNo：{}",
                    event.getEventId(), event.getOrderNo());
            return;
        }

        log.info("优惠券核销完成，eventId：{}，userId：{}，orderNo：{}，couponCode：{}",
                event.getEventId(), event.getUserId(), event.getOrderNo(), event.getCouponCode());
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/observer/order/listener/UserNotifyListener.java`

```java
package io.github.atengk.pattern.observer.order.listener;

import io.github.atengk.pattern.observer.order.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 用户通知监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UserNotifyListener {

    /**
     * 监听订单支付成功事件并发送用户通知
     *
     * @param event 订单支付成功事件
     */
    @Order(40)
    @EventListener
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("用户支付成功通知发送完成，eventId：{}，userId：{}，orderNo：{}，payAmount：{}",
                event.getEventId(), event.getUserId(), event.getOrderNo(), event.getPayAmount());
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/observer/order/listener/OrderStatisticListener.java`

```java
package io.github.atengk.pattern.observer.order.listener;

import io.github.atengk.pattern.observer.order.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 订单统计监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderStatisticListener {

    /**
     * 监听订单支付成功事件并记录统计数据
     *
     * @param event 订单支付成功事件
     */
    @Order(50)
    @EventListener
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("订单支付统计记录完成，eventId：{}，orderNo：{}，productId：{}，payAmount：{}，paidAt：{}",
                event.getEventId(),
                event.getOrderNo(),
                event.getProductId(),
                event.getPayAmount(),
                event.getPaidAt());
    }

}
```

## 控制器接口

控制器只负责接收订单支付请求，然后调用支付服务。它不需要知道支付成功后有哪些观察者会被触发。

文件位置：`src/main/java/io/github/atengk/pattern/observer/order/controller/OrderPayController.java`

```java
package io.github.atengk.pattern.observer.order.controller;

import io.github.atengk.pattern.observer.common.ApiResult;
import io.github.atengk.pattern.observer.order.dto.OrderPayRequest;
import io.github.atengk.pattern.observer.order.service.OrderPaymentService;
import io.github.atengk.pattern.observer.order.vo.OrderPayResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单支付接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderPayController {

    private final OrderPaymentService orderPaymentService;

    /**
     * 支付订单
     *
     * @param request 订单支付请求
     * @return 订单支付结果
     */
    @PostMapping("/pay")
    public ApiResult<OrderPayResultVO> payOrder(@Valid @RequestBody OrderPayRequest request) {
        return ApiResult.success(orderPaymentService.payOrder(request));
    }

}
```

## 使用方式

启动项目后，调用订单支付接口即可触发观察者模式。

接口信息：

| 项目         | 内容                                                     |
| ------------ | -------------------------------------------------------- |
| 请求路径     | `/orders/pay`                                            |
| 请求方法     | `POST`                                                   |
| Content-Type | `application/json`                                       |
| 主流程       | 支付订单 → 发布订单支付成功事件 → 多个监听器处理后续业务 |

正常支付订单：

```bash
curl -X POST "http://localhost:8080/orders/pay" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "productId": 20001,
    "quantity": 2,
    "payAmount": 199.90,
    "couponCode": "FULL_100_20"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "OD1998948715737427968",
    "payNo": "PAY1998948715737427969",
    "eventId": "EVT1998948715737427970",
    "payAmount": 199.90,
    "status": "PAID",
    "message": "订单支付成功"
  }
}
```

未使用优惠券支付：

```bash
curl -X POST "http://localhost:8080/orders/pay" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10002,
    "productId": 20002,
    "quantity": 1,
    "payAmount": 59.90
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "OD1998948991737427971",
    "payNo": "PAY1998948991737427972",
    "eventId": "EVT1998948991737427973",
    "payAmount": 59.90,
    "status": "PAID",
    "message": "订单支付成功"
  }
}
```

非法支付金额请求：

```bash
curl -X POST "http://localhost:8080/orders/pay" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "productId": 20001,
    "quantity": 1,
    "payAmount": 0
  }'
```

返回示例：

```json
{
  "code": 500,
  "message": "请求参数不合法",
  "data": null
}
```

## 验证方式

正常支付后，可以通过日志观察事件发布和监听器执行过程：

```text
订单支付成功，orderNo：OD1998948715737427968，payNo：PAY1998948715737427969，userId：10001，payAmount：199.90
发布订单支付成功事件，eventId：EVT1998948715737427970，orderNo：OD1998948715737427968，payNo：PAY1998948715737427969
开始处理库存扣减，eventId：EVT1998948715737427970，productId：20001，quantity：2
库存扣减完成，orderNo：OD1998948715737427968，productId：20001，quantity：2
积分发放完成，eventId：EVT1998948715737427970，userId：10001，orderNo：OD1998948715737427968，rewardPoints：19
优惠券核销完成，eventId：EVT1998948715737427970，userId：10001，orderNo：OD1998948715737427968，couponCode：FULL_100_20
用户支付成功通知发送完成，eventId：EVT1998948715737427970，userId：10001，orderNo：OD1998948715737427968，payAmount：199.90
订单支付统计记录完成，eventId：EVT1998948715737427970，orderNo：OD1998948715737427968，productId：20001，payAmount：199.90，paidAt：2026-05-13T10:00:00
```

从日志可以看到，支付服务只发布了一次事件，但多个监听器都收到了该事件并执行自己的业务逻辑。

## 异步事件扩展

默认情况下，Spring 的 `@EventListener` 是同步执行的。也就是说，监听器抛出异常可能影响发布事件的主流程。
如果订单支付接口不希望等待积分、通知、统计等后续动作完成，可以启用异步事件监听。

异步事件配置如下。

文件位置：`src/main/java/io/github/atengk/pattern/observer/config/AsyncEventConfig.java`

```java
package io.github.atengk.pattern.observer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步事件配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncEventConfig implements AsyncConfigurer {

    /**
     * 创建事件处理线程池
     *
     * @return 事件处理线程池
     */
    @Bean("eventTaskExecutor")
    public Executor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("event-task-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("异步事件线程池初始化完成");
        return executor;
    }

}
```

然后在监听器方法上增加 `@Async("eventTaskExecutor")`：

```java
@Async("eventTaskExecutor")
@EventListener
public void onOrderPaid(OrderPaidEvent event) {
    log.info("异步处理订单支付成功事件，eventId：{}", event.getEventId());
}
```

异步事件适合通知、统计、日志、非核心后续处理。
如果是库存扣减、优惠券核销这类强一致业务，需要结合事务、补偿机制或 MQ 事务消息谨慎处理。

## 事务事件扩展

如果订单支付涉及数据库事务，普通 `@EventListener` 可能在事务提交前就执行。
如果监听器必须等主事务提交成功后再执行，可以使用 `@TransactionalEventListener`。

示例：

```java
package io.github.atengk.pattern.observer.order.listener;

import io.github.atengk.pattern.observer.order.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 事务提交后通知监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class TransactionalUserNotifyListener {

    /**
     * 在事务提交后发送通知
     *
     * @param event 订单支付成功事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaidAfterCommit(OrderPaidEvent event) {
        log.info("事务提交后发送支付成功通知，eventId：{}，orderNo：{}",
                event.getEventId(), event.getOrderNo());
    }

}
```

注意：`@TransactionalEventListener` 需要事件发布发生在 Spring 事务上下文中。如果当前没有事务，默认不会执行监听器；可以根据业务需要设置 `fallbackExecution = true`。

## 扩展方式

如果后续需要新增“会员成长值监听器”，只需要新增一个监听器类即可，不需要修改支付主流程。

文件位置：`src/main/java/io/github/atengk/pattern/observer/order/listener/MemberGrowthListener.java`

```java
package io.github.atengk.pattern.observer.order.listener;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.observer.order.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 会员成长值监听器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class MemberGrowthListener {

    /**
     * 监听订单支付成功事件并增加会员成长值
     *
     * @param event 订单支付成功事件
     */
    @EventListener
    public void onOrderPaid(OrderPaidEvent event) {
        Integer growthValue = NumberUtil.div(event.getPayAmount(), 5).intValue();

        log.info("会员成长值增加完成，eventId：{}，userId：{}，growthValue：{}",
                event.getEventId(), event.getUserId(), growthValue);
    }

}
```

新增该类后，Spring 会自动注册监听器。支付服务仍然只发布 `OrderPaidEvent`，不需要修改任何支付代码。

## 优点和注意事项

观察者模式的核心价值是解耦事件发布方和事件处理方。发布方只关心“发生了什么”，观察者各自决定“收到事件后做什么”。

| 注意事项         | 说明                                             |
| ---------------- | ------------------------------------------------ |
| 事件对象要稳定   | 事件字段会被多个监听器依赖，不建议频繁破坏性修改 |
| 监听器职责要单一 | 一个监听器只处理一类后续动作                     |
| 核心链路谨慎异步 | 库存、优惠券等强一致逻辑不要随意异步化           |
| 监听器异常要隔离 | 异步监听器应做好异常捕获、日志记录和重试补偿     |
| 高频事件注意性能 | 事件过多时应考虑线程池、队列、削峰和批处理       |
| 跨服务事件用 MQ  | Spring 本地事件只适合单体应用或同 JVM 内部解耦   |

## 和中介者模式的区别

观察者模式和中介者模式都能减少对象之间的直接依赖，但关注点不同。

| 模式       | 关注点                             | 典型场景                                 |
| ---------- | ---------------------------------- | ---------------------------------------- |
| 观察者模式 | 一个事件发生后，多个观察者自动响应 | 支付成功后通知、统计、积分、缓存刷新     |
| 中介者模式 | 一个协调者主动编排多个对象协作     | 订单提交、支付编排、审批流程、工作流调度 |

观察者模式更适合“一对多通知”。
中介者模式更适合“流程编排和协作控制”。

## 和责任链模式的区别

观察者模式和责任链模式都可能触发多个处理器，但处理关系不同。

| 模式       | 关注点                                             | 典型场景                       |
| ---------- | -------------------------------------------------- | ------------------------------ |
| 观察者模式 | 多个监听器响应同一个事件，监听器之间通常互不依赖   | 事件通知、统计、日志、缓存刷新 |
| 责任链模式 | 一个请求按顺序经过多个节点，节点之间有明确链路关系 | 校验链、过滤链、风控链、审批链 |

观察者模式强调广播通知。
责任链模式强调顺序处理。

## 小结

观察者模式在 Spring Boot 项目中的常见落地方式是：定义业务事件对象，通过事件发布器发布事件，再由多个监听器独立处理后续动作。
在订单支付、用户注册、配置变更、缓存刷新、数据统计、消息通知等场景中，观察者模式可以有效降低主流程复杂度，使业务扩展更灵活。
