# 状态模式

状态模式用于把对象在不同状态下的行为拆分到独立状态类中，使对象在状态变化时表现出不同的业务行为。
在 Spring Boot 项目中，状态模式常用于订单状态流转、审批状态流转、工单状态流转、支付状态流转、任务状态流转、库存状态流转等场景。

本文以“订单生命周期流转”为例。订单从待支付、已支付、已发货、已完成、已取消、已退款等状态之间流转。不同状态下允许执行的动作不同，例如待支付订单可以支付和取消，已支付订单可以发货和退款，已取消订单不能再支付。

## 适用场景

状态模式适合处理“同一个业务对象在不同状态下行为不同”的场景。

订单生命周期中常见状态和动作如下：

| 当前状态 | 允许动作 | 下一个状态 |
| -------- | -------- | ---------- |
| 待支付   | 支付     | 已支付     |
| 待支付   | 取消     | 已取消     |
| 已支付   | 发货     | 已发货     |
| 已支付   | 退款     | 已退款     |
| 已发货   | 确认收货 | 已完成     |
| 已完成   | 退款     | 已退款     |
| 已取消   | 无       | 终态       |
| 已退款   | 无       | 终态       |

如果这些判断全部写在 `OrderServiceImpl` 中，代码通常会变成大量 `if-else` 或 `switch`：

```java
if ("CREATED".equals(order.getStatus())) {
    // 支付、取消
} else if ("PAID".equals(order.getStatus())) {
    // 发货、退款
} else if ("SHIPPED".equals(order.getStatus())) {
    // 确认收货
}
```

这种写法在状态和动作较多时会难以维护。状态模式可以把每个状态下的行为封装到对应状态类中，新增状态或调整状态行为时，只修改对应状态类。

## 基础配置

本示例基于 Spring Boot 3，使用 Hutool、Lombok 和 Validation。Hutool 用于字符串判断、对象判断和 ID 生成，Validation 用于接口参数基础校验。

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

    <!-- Hutool：提供字符串、对象、集合、ID生成等常用工具 -->
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
src/main/java/io/github/atengk/pattern/state
├── StateApplication.java
├── common
│   ├── ApiResult.java
│   ├── BizException.java
│   └── GlobalExceptionHandler.java
└── order
    ├── controller
    │   └── OrderStateController.java
    ├── dto
    │   ├── OrderActionRequest.java
    │   └── OrderCreateRequest.java
    ├── entity
    │   └── OrderInfo.java
    ├── repository
    │   └── OrderRepository.java
    ├── service
    │   ├── OrderStateService.java
    │   └── impl
    │       └── OrderStateServiceImpl.java
    ├── state
    │   ├── OrderState.java
    │   ├── OrderStateContext.java
    │   ├── OrderStateRegistry.java
    │   ├── OrderStatus.java
    │   └── impl
    │       ├── CanceledOrderState.java
    │       ├── CompletedOrderState.java
    │       ├── CreatedOrderState.java
    │       ├── PaidOrderState.java
    │       ├── RefundedOrderState.java
    │       └── ShippedOrderState.java
    └── vo
        └── OrderInfoVO.java
```

## 核心设计

本示例把状态模式拆成四个核心角色：

| 角色           | 项目中的类                               | 说明                                     |
| -------------- | ---------------------------------------- | ---------------------------------------- |
| Context        | `OrderStateContext`                      | 状态上下文，保存当前订单和操作原因       |
| State          | `OrderState`                             | 抽象状态接口，定义支付、发货、取消等动作 |
| ConcreteState  | `CreatedOrderState`、`PaidOrderState` 等 | 具体状态类，定义当前状态允许执行的动作   |
| State Registry | `OrderStateRegistry`                     | 状态注册器，根据订单状态找到对应状态类   |

执行流程如下：

```text
Controller
  -> OrderStateService
    -> OrderRepository 查询订单
    -> OrderStateRegistry 获取当前状态对象
    -> 当前状态对象执行动作
    -> OrderRepository 保存订单状态
```

状态模式的重点是：动作能不能执行，由当前状态类决定，而不是由 Service 集中判断。

## 公共代码

公共响应对象、业务异常和全局异常处理用于统一接口返回。实际项目中可以复用已有基础包。

文件位置：`src/main/java/io/github/atengk/pattern/state/common/ApiResult.java`

```java
package io.github.atengk.pattern.state.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/state/common/BizException.java`

```java
package io.github.atengk.pattern.state.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/state/common/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.state.common;

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

下面给出状态模式的核心代码。示例使用内存仓储模拟订单表，实际项目中可以替换为 MyBatis-Plus、JPA 或分布式状态机。

文件位置：`src/main/java/io/github/atengk/pattern/state/StateApplication.java`

```java
package io.github.atengk.pattern.state;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 状态模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class StateApplication {

    public static void main(String[] args) {
        SpringApplication.run(StateApplication.class, args);
    }

}
```

## 订单模型

订单实体保存订单基础信息和当前状态。状态字段是状态模式的核心，后续所有动作都基于该字段选择对应状态类。

文件位置：`src/main/java/io/github/atengk/pattern/state/order/entity/OrderInfo.java`

```java
package io.github.atengk.pattern.state.order.entity;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单信息实体
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderInfo {

    private String orderNo;

    private Long userId;

    private Long productId;

    private Integer quantity;

    private BigDecimal amount;

    private String status;

    private String lastReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
```

订单状态常量集中维护，避免业务代码中散落状态字符串。

文件位置：`src/main/java/io/github/atengk/pattern/state/order/state/OrderStatus.java`

```java
package io.github.atengk.pattern.state.order.state;

/**
 * 订单状态常量
 *
 * @author Ateng
 * @since 2026-05-13
 */
public final class OrderStatus {

    public static final String CREATED = "CREATED";

    public static final String PAID = "PAID";

    public static final String SHIPPED = "SHIPPED";

    public static final String COMPLETED = "COMPLETED";

    public static final String CANCELED = "CANCELED";

    public static final String REFUNDED = "REFUNDED";

    private OrderStatus() {
    }

}
```

请求对象和响应对象如下。

文件位置：`src/main/java/io/github/atengk/pattern/state/order/dto/OrderCreateRequest.java`

```java
package io.github.atengk.pattern.state.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单创建请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderCreateRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;

    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于0")
    private BigDecimal amount;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/state/order/dto/OrderActionRequest.java`

```java
package io.github.atengk.pattern.state.order.dto;

import lombok.Data;

/**
 * 订单动作请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderActionRequest {

    private String reason;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/state/order/vo/OrderInfoVO.java`

```java
package io.github.atengk.pattern.state.order.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单信息展示对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderInfoVO {

    private String orderNo;

    private Long userId;

    private Long productId;

    private Integer quantity;

    private BigDecimal amount;

    private String status;

    private String lastReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
```

## 状态上下文

状态上下文保存当前订单和操作原因，并提供统一状态切换方法。具体状态类通过上下文修改订单状态。

文件位置：`src/main/java/io/github/atengk/pattern/state/order/state/OrderStateContext.java`

```java
package io.github.atengk.pattern.state.order.state;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.state.order.entity.OrderInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 订单状态上下文
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Getter
public class OrderStateContext {

    private final OrderInfo orderInfo;

    private final String reason;

    /**
     * 创建订单状态上下文
     *
     * @param orderInfo 订单信息
     * @param reason 操作原因
     */
    public OrderStateContext(OrderInfo orderInfo, String reason) {
        this.orderInfo = orderInfo;
        this.reason = StrUtil.blankToDefault(reason, "未填写原因");
    }

    /**
     * 修改订单状态
     *
     * @param targetStatus 目标状态
     */
    public void changeStatus(String targetStatus) {
        String sourceStatus = orderInfo.getStatus();

        orderInfo.setStatus(targetStatus);
        orderInfo.setLastReason(reason);
        orderInfo.setUpdatedAt(LocalDateTime.now());

        log.info("订单状态流转完成，orderNo：{}，sourceStatus：{}，targetStatus：{}，reason：{}",
                orderInfo.getOrderNo(), sourceStatus, targetStatus, reason);
    }

}
```

## 抽象状态接口

`OrderState` 定义订单可能执行的动作。默认实现直接抛出业务异常，表示当前状态不支持该动作。具体状态类只需要重写自己允许的动作。

文件位置：`src/main/java/io/github/atengk/pattern/state/order/state/OrderState.java`

```java
package io.github.atengk.pattern.state.order.state;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.state.common.BizException;

/**
 * 订单状态接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderState {

    /**
     * 返回当前状态编码
     *
     * @return 状态编码
     */
    String status();

    /**
     * 支付订单
     *
     * @param context 状态上下文
     */
    default void pay(OrderStateContext context) {
        throwUnsupported(context, "支付");
    }

    /**
     * 发货订单
     *
     * @param context 状态上下文
     */
    default void ship(OrderStateContext context) {
        throwUnsupported(context, "发货");
    }

    /**
     * 确认收货
     *
     * @param context 状态上下文
     */
    default void confirmReceive(OrderStateContext context) {
        throwUnsupported(context, "确认收货");
    }

    /**
     * 取消订单
     *
     * @param context 状态上下文
     */
    default void cancel(OrderStateContext context) {
        throwUnsupported(context, "取消");
    }

    /**
     * 退款订单
     *
     * @param context 状态上下文
     */
    default void refund(OrderStateContext context) {
        throwUnsupported(context, "退款");
    }

    /**
     * 抛出不支持动作异常
     *
     * @param context 状态上下文
     * @param actionName 动作名称
     */
    default void throwUnsupported(OrderStateContext context, String actionName) {
        throw new BizException(StrUtil.format("当前订单状态[{}]不允许{}",
                context.getOrderInfo().getStatus(), actionName));
    }

}
```

## 具体状态类

每个具体状态类只处理当前状态允许执行的动作。非法动作由接口默认方法统一拦截。

待支付状态允许支付和取消。

文件位置：`src/main/java/io/github/atengk/pattern/state/order/state/impl/CreatedOrderState.java`

```java
package io.github.atengk.pattern.state.order.state.impl;

import io.github.atengk.pattern.state.order.state.OrderState;
import io.github.atengk.pattern.state.order.state.OrderStateContext;
import io.github.atengk.pattern.state.order.state.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 待支付订单状态
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class CreatedOrderState implements OrderState {

    /**
     * 返回当前状态编码
     *
     * @return 状态编码
     */
    @Override
    public String status() {
        return OrderStatus.CREATED;
    }

    /**
     * 支付订单
     *
     * @param context 状态上下文
     */
    @Override
    public void pay(OrderStateContext context) {
        log.info("待支付订单开始支付，orderNo：{}", context.getOrderInfo().getOrderNo());
        context.changeStatus(OrderStatus.PAID);
    }

    /**
     * 取消订单
     *
     * @param context 状态上下文
     */
    @Override
    public void cancel(OrderStateContext context) {
        log.info("待支付订单开始取消，orderNo：{}", context.getOrderInfo().getOrderNo());
        context.changeStatus(OrderStatus.CANCELED);
    }

}
```

已支付状态允许发货和退款。

文件位置：`src/main/java/io/github/atengk/pattern/state/order/state/impl/PaidOrderState.java`

```java
package io.github.atengk.pattern.state.order.state.impl;

import io.github.atengk.pattern.state.order.state.OrderState;
import io.github.atengk.pattern.state.order.state.OrderStateContext;
import io.github.atengk.pattern.state.order.state.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 已支付订单状态
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class PaidOrderState implements OrderState {

    /**
     * 返回当前状态编码
     *
     * @return 状态编码
     */
    @Override
    public String status() {
        return OrderStatus.PAID;
    }

    /**
     * 订单发货
     *
     * @param context 状态上下文
     */
    @Override
    public void ship(OrderStateContext context) {
        log.info("已支付订单开始发货，orderNo：{}", context.getOrderInfo().getOrderNo());
        context.changeStatus(OrderStatus.SHIPPED);
    }

    /**
     * 订单退款
     *
     * @param context 状态上下文
     */
    @Override
    public void refund(OrderStateContext context) {
        log.info("已支付订单开始退款，orderNo：{}", context.getOrderInfo().getOrderNo());
        context.changeStatus(OrderStatus.REFUNDED);
    }

}
```

已发货状态允许确认收货。

文件位置：`src/main/java/io/github/atengk/pattern/state/order/state/impl/ShippedOrderState.java`

```java
package io.github.atengk.pattern.state.order.state.impl;

import io.github.atengk.pattern.state.order.state.OrderState;
import io.github.atengk.pattern.state.order.state.OrderStateContext;
import io.github.atengk.pattern.state.order.state.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 已发货订单状态
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class ShippedOrderState implements OrderState {

    /**
     * 返回当前状态编码
     *
     * @return 状态编码
     */
    @Override
    public String status() {
        return OrderStatus.SHIPPED;
    }

    /**
     * 确认收货
     *
     * @param context 状态上下文
     */
    @Override
    public void confirmReceive(OrderStateContext context) {
        log.info("已发货订单开始确认收货，orderNo：{}", context.getOrderInfo().getOrderNo());
        context.changeStatus(OrderStatus.COMPLETED);
    }

}
```

已完成状态允许退款。实际项目中是否允许已完成订单退款，需要结合售后规则决定。

文件位置：`src/main/java/io/github/atengk/pattern/state/order/state/impl/CompletedOrderState.java`

```java
package io.github.atengk.pattern.state.order.state.impl;

import io.github.atengk.pattern.state.order.state.OrderState;
import io.github.atengk.pattern.state.order.state.OrderStateContext;
import io.github.atengk.pattern.state.order.state.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 已完成订单状态
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class CompletedOrderState implements OrderState {

    /**
     * 返回当前状态编码
     *
     * @return 状态编码
     */
    @Override
    public String status() {
        return OrderStatus.COMPLETED;
    }

    /**
     * 订单退款
     *
     * @param context 状态上下文
     */
    @Override
    public void refund(OrderStateContext context) {
        log.info("已完成订单开始退款，orderNo：{}", context.getOrderInfo().getOrderNo());
        context.changeStatus(OrderStatus.REFUNDED);
    }

}
```

已取消状态是终态，不允许继续操作。

文件位置：`src/main/java/io/github/atengk/pattern/state/order/state/impl/CanceledOrderState.java`

```java
package io.github.atengk.pattern.state.order.state.impl;

import io.github.atengk.pattern.state.order.state.OrderState;
import io.github.atengk.pattern.state.order.state.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * 已取消订单状态
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Component
public class CanceledOrderState implements OrderState {

    /**
     * 返回当前状态编码
     *
     * @return 状态编码
     */
    @Override
    public String status() {
        return OrderStatus.CANCELED;
    }

}
```

已退款状态是终态，不允许继续操作。

文件位置：`src/main/java/io/github/atengk/pattern/state/order/state/impl/RefundedOrderState.java`

```java
package io.github.atengk.pattern.state.order.state.impl;

import io.github.atengk.pattern.state.order.state.OrderState;
import io.github.atengk.pattern.state.order.state.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * 已退款订单状态
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Component
public class RefundedOrderState implements OrderState {

    /**
     * 返回当前状态编码
     *
     * @return 状态编码
     */
    @Override
    public String status() {
        return OrderStatus.REFUNDED;
    }

}
```

## 状态注册器

状态注册器负责把 Spring 容器中的所有状态类注册到 Map 中。业务服务根据订单当前状态获取对应状态对象。

文件位置：`src/main/java/io/github/atengk/pattern/state/order/state/OrderStateRegistry.java`

```java
package io.github.atengk.pattern.state.order.state;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.state.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单状态注册器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderStateRegistry {

    private final Map<String, OrderState> stateMap;

    /**
     * 初始化订单状态注册器
     *
     * @param orderStates Spring 容器中的订单状态集合
     */
    public OrderStateRegistry(List<OrderState> orderStates) {
        if (CollUtil.isEmpty(orderStates)) {
            this.stateMap = Collections.emptyMap();
            log.warn("订单状态注册器未加载到任何状态类");
            return;
        }

        Map<String, OrderState> registerMap = new HashMap<>(orderStates.size());
        for (OrderState orderState : orderStates) {
            String status = orderState.status();
            if (registerMap.containsKey(status)) {
                throw new IllegalStateException("订单状态重复注册：" + status);
            }

            registerMap.put(status, orderState);
            log.info("注册订单状态，status：{}，stateClass：{}", status, orderState.getClass().getSimpleName());
        }

        this.stateMap = Collections.unmodifiableMap(registerMap);
    }

    /**
     * 根据状态获取订单状态对象
     *
     * @param status 状态编码
     * @return 订单状态对象
     */
    public OrderState getRequired(String status) {
        if (StrUtil.isBlank(status)) {
            throw new BizException("订单状态不能为空");
        }

        OrderState orderState = stateMap.get(StrUtil.upperCase(status));
        if (orderState == null) {
            throw new BizException("不支持的订单状态：" + status);
        }

        return orderState;
    }

}
```

## 订单仓储

订单仓储用于模拟订单表。实际项目中可以替换为 Mapper 或 Repository。

文件位置：`src/main/java/io/github/atengk/pattern/state/order/repository/OrderRepository.java`

```java
package io.github.atengk.pattern.state.order.repository;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.state.common.BizException;
import io.github.atengk.pattern.state.order.entity.OrderInfo;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单仓储
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Repository
public class OrderRepository {

    private final Map<String, OrderInfo> orderStorage = new ConcurrentHashMap<>();

    /**
     * 保存订单
     *
     * @param orderInfo 订单信息
     */
    public void save(OrderInfo orderInfo) {
        orderStorage.put(orderInfo.getOrderNo(), orderInfo);
    }

    /**
     * 查询必须存在的订单
     *
     * @param orderNo 订单编号
     * @return 订单信息
     */
    public OrderInfo getRequired(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            throw new BizException("订单编号不能为空");
        }

        OrderInfo orderInfo = orderStorage.get(orderNo);
        if (orderInfo == null) {
            throw new BizException("订单不存在");
        }

        return orderInfo;
    }

}
```

## 业务服务

业务服务负责查询订单、获取当前状态对象、调用状态动作、保存结果。
可以看到，Service 不需要判断“当前状态是否允许支付、是否允许发货”，这些规则已经下沉到各个状态类中。

文件位置：`src/main/java/io/github/atengk/pattern/state/order/service/OrderStateService.java`

```java
package io.github.atengk.pattern.state.order.service;

import io.github.atengk.pattern.state.order.dto.OrderActionRequest;
import io.github.atengk.pattern.state.order.dto.OrderCreateRequest;
import io.github.atengk.pattern.state.order.vo.OrderInfoVO;

/**
 * 订单状态服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderStateService {

    /**
     * 创建订单
     *
     * @param request 创建请求
     * @return 订单信息
     */
    OrderInfoVO createOrder(OrderCreateRequest request);

    /**
     * 支付订单
     *
     * @param orderNo 订单编号
     * @param request 动作请求
     * @return 订单信息
     */
    OrderInfoVO payOrder(String orderNo, OrderActionRequest request);

    /**
     * 发货订单
     *
     * @param orderNo 订单编号
     * @param request 动作请求
     * @return 订单信息
     */
    OrderInfoVO shipOrder(String orderNo, OrderActionRequest request);

    /**
     * 确认收货
     *
     * @param orderNo 订单编号
     * @param request 动作请求
     * @return 订单信息
     */
    OrderInfoVO confirmReceive(String orderNo, OrderActionRequest request);

    /**
     * 取消订单
     *
     * @param orderNo 订单编号
     * @param request 动作请求
     * @return 订单信息
     */
    OrderInfoVO cancelOrder(String orderNo, OrderActionRequest request);

    /**
     * 退款订单
     *
     * @param orderNo 订单编号
     * @param request 动作请求
     * @return 订单信息
     */
    OrderInfoVO refundOrder(String orderNo, OrderActionRequest request);

    /**
     * 查询订单
     *
     * @param orderNo 订单编号
     * @return 订单信息
     */
    OrderInfoVO getOrder(String orderNo);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/state/order/service/impl/OrderStateServiceImpl.java`

```java
package io.github.atengk.pattern.state.order.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.state.order.dto.OrderActionRequest;
import io.github.atengk.pattern.state.order.dto.OrderCreateRequest;
import io.github.atengk.pattern.state.order.entity.OrderInfo;
import io.github.atengk.pattern.state.order.repository.OrderRepository;
import io.github.atengk.pattern.state.order.service.OrderStateService;
import io.github.atengk.pattern.state.order.state.OrderState;
import io.github.atengk.pattern.state.order.state.OrderStateContext;
import io.github.atengk.pattern.state.order.state.OrderStateRegistry;
import io.github.atengk.pattern.state.order.state.OrderStatus;
import io.github.atengk.pattern.state.order.vo.OrderInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * 订单状态服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStateServiceImpl implements OrderStateService {

    private final OrderRepository orderRepository;

    private final OrderStateRegistry orderStateRegistry;

    /**
     * 创建订单
     *
     * @param request 创建请求
     * @return 订单信息
     */
    @Override
    public OrderInfoVO createOrder(OrderCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();

        OrderInfo orderInfo = OrderInfo.builder()
                .orderNo("OD" + IdUtil.getSnowflakeNextIdStr())
                .userId(request.getUserId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .amount(request.getAmount())
                .status(OrderStatus.CREATED)
                .lastReason("订单创建")
                .createdAt(now)
                .updatedAt(now)
                .build();

        orderRepository.save(orderInfo);
        log.info("订单创建成功，orderNo：{}，status：{}", orderInfo.getOrderNo(), orderInfo.getStatus());

        return toVO(orderInfo);
    }

    /**
     * 支付订单
     *
     * @param orderNo 订单编号
     * @param request 动作请求
     * @return 订单信息
     */
    @Override
    public OrderInfoVO payOrder(String orderNo, OrderActionRequest request) {
        return executeAction(orderNo, request, OrderState::pay);
    }

    /**
     * 发货订单
     *
     * @param orderNo 订单编号
     * @param request 动作请求
     * @return 订单信息
     */
    @Override
    public OrderInfoVO shipOrder(String orderNo, OrderActionRequest request) {
        return executeAction(orderNo, request, OrderState::ship);
    }

    /**
     * 确认收货
     *
     * @param orderNo 订单编号
     * @param request 动作请求
     * @return 订单信息
     */
    @Override
    public OrderInfoVO confirmReceive(String orderNo, OrderActionRequest request) {
        return executeAction(orderNo, request, OrderState::confirmReceive);
    }

    /**
     * 取消订单
     *
     * @param orderNo 订单编号
     * @param request 动作请求
     * @return 订单信息
     */
    @Override
    public OrderInfoVO cancelOrder(String orderNo, OrderActionRequest request) {
        return executeAction(orderNo, request, OrderState::cancel);
    }

    /**
     * 退款订单
     *
     * @param orderNo 订单编号
     * @param request 动作请求
     * @return 订单信息
     */
    @Override
    public OrderInfoVO refundOrder(String orderNo, OrderActionRequest request) {
        return executeAction(orderNo, request, OrderState::refund);
    }

    /**
     * 查询订单
     *
     * @param orderNo 订单编号
     * @return 订单信息
     */
    @Override
    public OrderInfoVO getOrder(String orderNo) {
        return toVO(orderRepository.getRequired(orderNo));
    }

    /**
     * 执行订单状态动作
     *
     * @param orderNo 订单编号
     * @param request 动作请求
     * @param action 状态动作
     * @return 订单信息
     */
    private OrderInfoVO executeAction(String orderNo,
                                      OrderActionRequest request,
                                      ConsumerWithContext<OrderState, OrderStateContext> action) {
        OrderInfo orderInfo = orderRepository.getRequired(orderNo);
        OrderState orderState = orderStateRegistry.getRequired(orderInfo.getStatus());

        String reason = request == null ? "" : request.getReason();
        OrderStateContext context = new OrderStateContext(orderInfo, StrUtil.blankToDefault(reason, "状态流转"));

        log.info("开始执行订单状态动作，orderNo：{}，currentStatus：{}",
                orderInfo.getOrderNo(), orderInfo.getStatus());

        action.accept(orderState, context);

        orderRepository.save(orderInfo);

        log.info("订单状态动作执行完成，orderNo：{}，newStatus：{}",
                orderInfo.getOrderNo(), orderInfo.getStatus());

        return toVO(orderInfo);
    }

    /**
     * 转换订单展示对象
     *
     * @param orderInfo 订单信息
     * @return 订单展示对象
     */
    private OrderInfoVO toVO(OrderInfo orderInfo) {
        return OrderInfoVO.builder()
                .orderNo(orderInfo.getOrderNo())
                .userId(orderInfo.getUserId())
                .productId(orderInfo.getProductId())
                .quantity(orderInfo.getQuantity())
                .amount(orderInfo.getAmount())
                .status(orderInfo.getStatus())
                .lastReason(orderInfo.getLastReason())
                .createdAt(orderInfo.getCreatedAt())
                .updatedAt(orderInfo.getUpdatedAt())
                .build();
    }

    /**
     * 带上下文的状态动作函数
     *
     * @author Ateng
     * @since 2026-05-13
     */
    @FunctionalInterface
    private interface ConsumerWithContext<T, U> {

        /**
         * 执行动作
         *
         * @param target 目标对象
         * @param context 上下文对象
         */
        void accept(T target, U context);

    }

}
```

上面代码中定义了一个私有函数式接口 `ConsumerWithContext`，用于把 `pay`、`ship`、`cancel` 等状态动作抽象成统一执行逻辑，避免在 Service 中重复查询订单、创建上下文、保存订单等代码。

## 控制器接口

控制器提供订单创建、支付、发货、确认收货、取消、退款和查询接口。Controller 不关心状态流转规则，只调用对应业务动作。

文件位置：`src/main/java/io/github/atengk/pattern/state/order/controller/OrderStateController.java`

```java
package io.github.atengk.pattern.state.order.controller;

import io.github.atengk.pattern.state.common.ApiResult;
import io.github.atengk.pattern.state.order.dto.OrderActionRequest;
import io.github.atengk.pattern.state.order.dto.OrderCreateRequest;
import io.github.atengk.pattern.state.order.service.OrderStateService;
import io.github.atengk.pattern.state.order.vo.OrderInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单状态接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderStateController {

    private final OrderStateService orderStateService;

    /**
     * 创建订单
     *
     * @param request 创建请求
     * @return 订单信息
     */
    @PostMapping
    public ApiResult<OrderInfoVO> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return ApiResult.success(orderStateService.createOrder(request));
    }

    /**
     * 支付订单
     *
     * @param orderNo 订单编号
     * @param request 动作请求
     * @return 订单信息
     */
    @PostMapping("/{orderNo}/pay")
    public ApiResult<OrderInfoVO> payOrder(@PathVariable String orderNo,
                                           @RequestBody(required = false) OrderActionRequest request) {
        return ApiResult.success(orderStateService.payOrder(orderNo, request));
    }

    /**
     * 发货订单
     *
     * @param orderNo 订单编号
     * @param request 动作请求
     * @return 订单信息
     */
    @PostMapping("/{orderNo}/ship")
    public ApiResult<OrderInfoVO> shipOrder(@PathVariable String orderNo,
                                            @RequestBody(required = false) OrderActionRequest request) {
        return ApiResult.success(orderStateService.shipOrder(orderNo, request));
    }

    /**
     * 确认收货
     *
     * @param orderNo 订单编号
     * @param request 动作请求
     * @return 订单信息
     */
    @PostMapping("/{orderNo}/receive")
    public ApiResult<OrderInfoVO> confirmReceive(@PathVariable String orderNo,
                                                 @RequestBody(required = false) OrderActionRequest request) {
        return ApiResult.success(orderStateService.confirmReceive(orderNo, request));
    }

    /**
     * 取消订单
     *
     * @param orderNo 订单编号
     * @param request 动作请求
     * @return 订单信息
     */
    @PostMapping("/{orderNo}/cancel")
    public ApiResult<OrderInfoVO> cancelOrder(@PathVariable String orderNo,
                                              @RequestBody(required = false) OrderActionRequest request) {
        return ApiResult.success(orderStateService.cancelOrder(orderNo, request));
    }

    /**
     * 退款订单
     *
     * @param orderNo 订单编号
     * @param request 动作请求
     * @return 订单信息
     */
    @PostMapping("/{orderNo}/refund")
    public ApiResult<OrderInfoVO> refundOrder(@PathVariable String orderNo,
                                              @RequestBody(required = false) OrderActionRequest request) {
        return ApiResult.success(orderStateService.refundOrder(orderNo, request));
    }

    /**
     * 查询订单
     *
     * @param orderNo 订单编号
     * @return 订单信息
     */
    @GetMapping("/{orderNo}")
    public ApiResult<OrderInfoVO> getOrder(@PathVariable String orderNo) {
        return ApiResult.success(orderStateService.getOrder(orderNo));
    }

}
```

## 使用方式

启动项目后，先创建订单，然后按订单生命周期调用不同动作接口。

创建订单：

```bash
curl -X POST "http://localhost:8080/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "productId": 20001,
    "quantity": 2,
    "amount": 199.90
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "OD1998948715737427968",
    "userId": 10001,
    "productId": 20001,
    "quantity": 2,
    "amount": 199.90,
    "status": "CREATED",
    "lastReason": "订单创建",
    "createdAt": "2026-05-13T10:00:00",
    "updatedAt": "2026-05-13T10:00:00"
  }
}
```

支付订单：

```bash
curl -X POST "http://localhost:8080/orders/OD1998948715737427968/pay" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "用户完成支付"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "OD1998948715737427968",
    "userId": 10001,
    "productId": 20001,
    "quantity": 2,
    "amount": 199.90,
    "status": "PAID",
    "lastReason": "用户完成支付",
    "createdAt": "2026-05-13T10:00:00",
    "updatedAt": "2026-05-13T10:01:00"
  }
}
```

发货订单：

```bash
curl -X POST "http://localhost:8080/orders/OD1998948715737427968/ship" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "仓库已发货"
  }'
```

确认收货：

```bash
curl -X POST "http://localhost:8080/orders/OD1998948715737427968/receive" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "用户确认收货"
  }'
```

退款订单：

```bash
curl -X POST "http://localhost:8080/orders/OD1998948715737427968/refund" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "用户申请售后退款"
  }'
```

非法状态流转示例：已发货订单不能取消。

```bash
curl -X POST "http://localhost:8080/orders/OD1998948715737427968/cancel" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "用户取消订单"
  }'
```

返回示例：

```json
{
  "code": 500,
  "message": "当前订单状态[SHIPPED]不允许取消",
  "data": null
}
```

## 验证方式

项目启动后，可以看到状态注册日志：

```text
注册订单状态，status：CREATED，stateClass：CreatedOrderState
注册订单状态，status：PAID，stateClass：PaidOrderState
注册订单状态，status：SHIPPED，stateClass：ShippedOrderState
注册订单状态，status：COMPLETED，stateClass：CompletedOrderState
注册订单状态，status：CANCELED，stateClass：CanceledOrderState
注册订单状态，status：REFUNDED，stateClass：RefundedOrderState
```

正常支付订单时，可以看到状态流转日志：

```text
订单创建成功，orderNo：OD1998948715737427968，status：CREATED
开始执行订单状态动作，orderNo：OD1998948715737427968，currentStatus：CREATED
待支付订单开始支付，orderNo：OD1998948715737427968
订单状态流转完成，orderNo：OD1998948715737427968，sourceStatus：CREATED，targetStatus：PAID，reason：用户完成支付
订单状态动作执行完成，orderNo：OD1998948715737427968，newStatus：PAID
```

非法状态流转时，例如已取消订单继续支付，可以看到业务异常日志：

```text
业务处理失败：当前订单状态[CANCELED]不允许支付
```

## 替换为数据库表

实际项目中，订单状态一般保存在订单表中。核心字段如下：

```sql
-- 订单表，status 字段用于状态模式中的当前状态判断
CREATE TABLE t_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL COMMENT '购买数量',
    amount DECIMAL(18, 2) NOT NULL COMMENT '订单金额',
    status VARCHAR(32) NOT NULL COMMENT '订单状态',
    last_reason VARCHAR(255) DEFAULT '' COMMENT '最近一次状态变更原因',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id_status (user_id, status)
) COMMENT='订单表';
```

如果需要记录完整状态流转历史，建议新增订单状态流水表：

```sql
-- 订单状态流转流水表
CREATE TABLE t_order_status_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    source_status VARCHAR(32) NOT NULL COMMENT '原状态',
    target_status VARCHAR(32) NOT NULL COMMENT '目标状态',
    reason VARCHAR(255) DEFAULT '' COMMENT '状态变更原因',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    KEY idx_order_no_created_at (order_no, created_at)
) COMMENT='订单状态流转流水表';
```

状态模式解决的是“当前状态下允许执行什么行为”。状态流水表解决的是“状态变化过程如何审计”。这两个能力可以结合使用。

## 扩展方式

如果后续新增“售后中”状态，只需要三步。

第一，新增状态常量：

```java
public static final String AFTER_SALE = "AFTER_SALE";
```

第二，新增状态类：

```java
package io.github.atengk.pattern.state.order.state.impl;

import io.github.atengk.pattern.state.order.state.OrderState;
import io.github.atengk.pattern.state.order.state.OrderStateContext;
import io.github.atengk.pattern.state.order.state.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 售后中订单状态
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class AfterSaleOrderState implements OrderState {

    /**
     * 返回当前状态编码
     *
     * @return 状态编码
     */
    @Override
    public String status() {
        return OrderStatus.AFTER_SALE;
    }

    /**
     * 订单退款
     *
     * @param context 状态上下文
     */
    @Override
    public void refund(OrderStateContext context) {
        log.info("售后中订单退款通过，orderNo：{}", context.getOrderInfo().getOrderNo());
        context.changeStatus(OrderStatus.REFUNDED);
    }

}
```

第三，在允许进入售后状态的状态类中增加动作。例如在 `CompletedOrderState` 中把退款前置为进入售后：

```java
@Override
public void refund(OrderStateContext context) {
    log.info("已完成订单进入售后流程，orderNo：{}", context.getOrderInfo().getOrderNo());
    context.changeStatus(OrderStatus.AFTER_SALE);
}
```

由于 `OrderStateRegistry` 会自动注册 Spring 容器中的所有 `OrderState`，所以新增状态类后不需要修改注册器。

## 优点和注意事项

状态模式的核心价值是把状态相关行为封装到具体状态类中，避免主业务服务被大量状态判断污染。

| 注意事项                   | 说明                                               |
| -------------------------- | -------------------------------------------------- |
| 状态数量适中时最适合       | 状态和动作较少时，直接判断可能更简单               |
| 状态类职责要清晰           | 每个状态类只处理当前状态允许的行为                 |
| 状态流转要可追踪           | 生产环境建议记录状态变更流水                       |
| 注意并发更新               | 支付、取消、退款等动作需要考虑并发状态竞争         |
| 注意事务边界               | 状态变更和关联业务操作应在明确事务中完成           |
| 不要把所有业务都塞进状态类 | 状态类负责状态行为，复杂领域逻辑仍应放在领域服务中 |

## 并发状态变更注意事项

订单状态流转通常存在并发问题。例如用户正在支付订单，同时又触发取消订单。如果没有并发控制，可能出现状态覆盖。

实际项目中建议使用乐观锁：

```sql
-- 增加 version 字段用于乐观锁控制
ALTER TABLE t_order ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';
```

更新状态时带上原状态和版本号：

```sql
-- 只有当前状态和版本号都匹配时才允许更新
UPDATE t_order
SET status = #{targetStatus},
    version = version + 1,
    updated_at = NOW()
WHERE order_no = #{orderNo}
  AND status = #{sourceStatus}
  AND version = #{version};
```

如果影响行数为 0，说明订单状态已被其他请求修改，需要返回“订单状态已变化，请刷新后重试”。

## 和策略模式的区别

状态模式和策略模式结构上很像，都是把行为拆到多个类中，但意图不同。

| 模式     | 关注点                             | 典型场景                               |
| -------- | ---------------------------------- | -------------------------------------- |
| 状态模式 | 对象内部状态变化后，行为随状态改变 | 订单状态、审批状态、工单状态、任务状态 |
| 策略模式 | 外部根据类型选择一种算法或处理方式 | 支付渠道、计价策略、导出策略、登录方式 |

状态模式的关键是“对象有状态，状态会变化，行为随状态变化”。
策略模式的关键是“算法可替换，通常不强调对象内部状态流转”。

## 和责任链模式的区别

状态模式和责任链模式都能减少 `if-else`，但处理的问题不同。

| 模式       | 关注点                       | 典型场景                           |
| ---------- | ---------------------------- | ---------------------------------- |
| 状态模式   | 当前状态决定当前行为         | 订单状态流转、审批状态流转         |
| 责任链模式 | 一个请求依次经过多个处理节点 | 参数校验、风控过滤、审批链、处理链 |

如果核心问题是“当前状态能做什么”，优先考虑状态模式。
如果核心问题是“这个请求要经过哪些处理节点”，优先考虑责任链模式。

## 小结

状态模式在 Spring Boot 项目中的常见落地方式是：定义统一状态接口，为每个业务状态创建独立状态类，再通过状态注册器根据当前状态分发动作。
在订单、审批、工单、支付、任务等状态流转复杂的场景中，状态模式可以明显降低 Service 中的条件判断复杂度，使状态行为更清晰、更容易扩展。
