# 状态模式 + 策略模式

状态模式和策略模式组合使用时，通常用于处理 **对象状态会变化，并且某些状态行为内部还存在多种可替换算法** 的业务场景。

状态模式负责把不同状态下的行为封装到不同状态类中，避免业务代码中出现大量状态判断；策略模式负责封装某个状态行为内部的算法变化，例如不同支付方式退款、不同配送方式计费、不同会员等级权益计算等。


```text
状态模式：管理对象在不同状态下的行为
策略模式：封装某个状态行为内部的可变算法
```

## 适用场景

本示例以“订单状态流转”为业务场景。订单会经历待支付、已支付、已发货、已完成、已取消等状态：

| 状态              | 可执行行为 | 说明                 |
| --------------- | ----- | ------------------ |
| PENDING_PAYMENT | 支付、取消 | 待支付订单可以支付，也可以取消    |
| PAID            | 发货、取消 | 已支付订单可以发货，也可以取消并退款 |
| SHIPPED         | 确认收货  | 已发货订单可以确认收货        |
| COMPLETED       | 无     | 已完成订单不允许继续流转       |
| CANCELED        | 无     | 已取消订单不允许继续流转       |

其中，“已支付订单取消”时需要退款，而不同支付方式的退款规则不同：

| 支付方式    | 退款策略     |
| ------- | -------- |
| ALIPAY  | 支付宝原路退款  |
| WECHAT  | 微信支付原路退款 |
| BALANCE | 余额直接退回账户 |

如果只使用状态模式，`PaidOrderState` 内部可能会出现大量支付方式判断；如果只使用策略模式，又无法清晰表达订单在不同状态下允许执行哪些行为。

使用“状态模式 + 策略模式”后，结构变成：

```text
Controller
  -> Service
      -> OrderStateFactory：根据当前订单状态获取状态对象
          -> PendingPaymentOrderState
          -> PaidOrderState
              -> RefundStrategyFactory
                  -> AlipayRefundStrategy
                  -> WechatRefundStrategy
                  -> BalanceRefundStrategy
          -> ShippedOrderState
          -> CompletedOrderState
          -> CanceledOrderState
```

## 基础配置

这里使用 JDK 21、Spring Boot 3、Maven、Lombok、Hutool 和 Spring Validation。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Validation：用于请求参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：提供字符串、对象、数字、ID、集合等常用工具 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok：减少 Getter、Setter、Builder、构造器、日志等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

建议目录结构如下：

```text
src/main/java/io/github/atengk/pattern/combination/statestrategy
├── context
│   └── OrderStateContext.java
├── controller
│   └── OrderStateController.java
├── dto
│   ├── OrderActionRequest.java
│   └── OrderActionResponse.java
├── enums
│   ├── OrderActionEnum.java
│   ├── OrderStatusEnum.java
│   └── PaymentTypeEnum.java
├── factory
│   ├── OrderStateFactory.java
│   └── RefundStrategyFactory.java
├── handler
│   └── GlobalExceptionHandler.java
├── result
│   └── Result.java
├── service
│   ├── OrderStateService.java
│   └── impl
│       └── OrderStateServiceImpl.java
├── state
│   ├── AbstractOrderState.java
│   ├── OrderState.java
│   └── impl
│       ├── CanceledOrderState.java
│       ├── CompletedOrderState.java
│       ├── PaidOrderState.java
│       ├── PendingPaymentOrderState.java
│       └── ShippedOrderState.java
└── strategy
    ├── RefundStrategy.java
    └── impl
        ├── AlipayRefundStrategy.java
        ├── BalanceRefundStrategy.java
        └── WechatRefundStrategy.java
```

## 核心代码

这一部分给出核心实现。重点看两个位置：

```text
OrderState：状态接口，封装不同订单状态下的行为
RefundStrategy：退款策略接口，封装已支付订单取消时的退款算法
```

状态类负责判断当前状态允许执行哪些动作；策略类负责处理某个状态行为中的算法差异。

### 订单状态枚举

订单状态枚举用于统一维护订单状态编码。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/enums/OrderStatusEnum.java`

```java
package io.github.atengk.pattern.combination.statestrategy.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 订单状态枚举
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
@RequiredArgsConstructor
public enum OrderStatusEnum {

    PENDING_PAYMENT("PENDING_PAYMENT", "待支付"),

    PAID("PAID", "已支付"),

    SHIPPED("SHIPPED", "已发货"),

    COMPLETED("COMPLETED", "已完成"),

    CANCELED("CANCELED", "已取消");

    private final String code;

    private final String description;

    /**
     * 根据编码获取订单状态
     *
     * @param code 订单状态编码
     * @return 订单状态
     */
    public static OrderStatusEnum of(String code) {
        String actualCode = StrUtil.trimToEmpty(code);
        for (OrderStatusEnum item : values()) {
            if (StrUtil.equalsIgnoreCase(item.getCode(), actualCode)) {
                return item;
            }
        }
        throw new IllegalArgumentException("不支持的订单状态：" + code);
    }
}
```

### 订单动作枚举

订单动作枚举用于统一维护订单状态流转动作。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/enums/OrderActionEnum.java`

```java
package io.github.atengk.pattern.combination.statestrategy.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 订单动作枚举
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
@RequiredArgsConstructor
public enum OrderActionEnum {

    PAY("PAY", "支付"),

    SHIP("SHIP", "发货"),

    CONFIRM_RECEIVE("CONFIRM_RECEIVE", "确认收货"),

    CANCEL("CANCEL", "取消");

    private final String code;

    private final String description;

    /**
     * 根据编码获取订单动作
     *
     * @param code 订单动作编码
     * @return 订单动作
     */
    public static OrderActionEnum of(String code) {
        String actualCode = StrUtil.trimToEmpty(code);
        for (OrderActionEnum item : values()) {
            if (StrUtil.equalsIgnoreCase(item.getCode(), actualCode)) {
                return item;
            }
        }
        throw new IllegalArgumentException("不支持的订单动作：" + code);
    }
}
```

### 支付方式枚举

支付方式枚举用于选择不同退款策略。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/enums/PaymentTypeEnum.java`

```java
package io.github.atengk.pattern.combination.statestrategy.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 支付方式枚举
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
@RequiredArgsConstructor
public enum PaymentTypeEnum {

    ALIPAY("ALIPAY", "支付宝支付"),

    WECHAT("WECHAT", "微信支付"),

    BALANCE("BALANCE", "余额支付");

    private final String code;

    private final String description;

    /**
     * 根据编码获取支付方式
     *
     * @param code 支付方式编码
     * @return 支付方式
     */
    public static PaymentTypeEnum of(String code) {
        String actualCode = StrUtil.trimToEmpty(code);
        for (PaymentTypeEnum item : values()) {
            if (StrUtil.equalsIgnoreCase(item.getCode(), actualCode)) {
                return item;
            }
        }
        throw new IllegalArgumentException("不支持的支付方式：" + code);
    }
}
```

### 订单动作请求对象

订单动作请求对象用于接收订单状态流转参数。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/dto/OrderActionRequest.java`

```java
package io.github.atengk.pattern.combination.statestrategy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单动作请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderActionRequest {

    /**
     * 订单编号
     */
    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 当前订单状态：PENDING_PAYMENT、PAID、SHIPPED、COMPLETED、CANCELED
     */
    @NotBlank(message = "当前订单状态不能为空")
    private String currentStatus;

    /**
     * 订单动作：PAY、SHIP、CONFIRM_RECEIVE、CANCEL
     */
    @NotBlank(message = "订单动作不能为空")
    private String action;

    /**
     * 支付方式：ALIPAY、WECHAT、BALANCE
     */
    private String paymentType;

    /**
     * 订单金额
     */
    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于0")
    private BigDecimal orderAmount;

    /**
     * 操作原因
     */
    private String reason;
}
```

### 订单动作响应对象

订单动作响应对象用于返回状态流转结果和策略执行结果。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/dto/OrderActionResponse.java`

```java
package io.github.atengk.pattern.combination.statestrategy.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单动作响应
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderActionResponse {

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 原订单状态
     */
    private String oldStatus;

    /**
     * 新订单状态
     */
    private String newStatus;

    /**
     * 执行动作
     */
    private String action;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 退款流水号
     */
    private String refundNo;

    /**
     * 退款策略名称
     */
    private String refundStrategyName;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 响应消息
     */
    private String message;
}
```

### 订单状态上下文

上下文对象用于在状态对象和策略对象之间传递订单数据。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/context/OrderStateContext.java`

```java
package io.github.atengk.pattern.combination.statestrategy.context;

import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionRequest;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单状态上下文
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderStateContext {

    /**
     * 订单动作请求
     */
    private OrderActionRequest request;

    /**
     * 退款流水号
     */
    private String refundNo;

    /**
     * 退款策略名称
     */
    private String refundStrategyName;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 扩展属性
     */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
}
```

### 退款策略接口

退款策略接口定义不同支付方式的退款算法。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/strategy/RefundStrategy.java`

```java
package io.github.atengk.pattern.combination.statestrategy.strategy;

import io.github.atengk.pattern.combination.statestrategy.context.OrderStateContext;
import io.github.atengk.pattern.combination.statestrategy.enums.PaymentTypeEnum;

/**
 * 退款策略接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface RefundStrategy {

    /**
     * 当前策略支持的支付方式
     *
     * @return 支付方式
     */
    PaymentTypeEnum paymentType();

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    String strategyName();

    /**
     * 执行退款
     *
     * @param context 订单状态上下文
     */
    void refund(OrderStateContext context);
}
```

### 支付宝退款策略

支付宝退款策略用于模拟支付宝原路退款。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/strategy/impl/AlipayRefundStrategy.java`

```java
package io.github.atengk.pattern.combination.statestrategy.strategy.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.combination.statestrategy.context.OrderStateContext;
import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionRequest;
import io.github.atengk.pattern.combination.statestrategy.enums.PaymentTypeEnum;
import io.github.atengk.pattern.combination.statestrategy.strategy.RefundStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 支付宝退款策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class AlipayRefundStrategy implements RefundStrategy {

    /**
     * 当前策略支持的支付方式
     *
     * @return 支付方式
     */
    @Override
    public PaymentTypeEnum paymentType() {
        return PaymentTypeEnum.ALIPAY;
    }

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    @Override
    public String strategyName() {
        return "支付宝原路退款策略";
    }

    /**
     * 执行支付宝退款
     *
     * @param context 订单状态上下文
     */
    @Override
    public void refund(OrderStateContext context) {
        OrderActionRequest request = context.getRequest();
        BigDecimal refundAmount = NumberUtil.round(request.getOrderAmount(), 2);
        String refundNo = "ALI_REFUND_" + IdUtil.fastSimpleUUID();

        context.setRefundNo(refundNo);
        context.setRefundAmount(refundAmount);
        context.setRefundStrategyName(strategyName());

        log.info("执行支付宝退款，订单编号：{}，退款流水号：{}，退款金额：{}",
                request.getOrderNo(), refundNo, refundAmount);
    }
}
```

### 微信退款策略

微信退款策略用于模拟微信支付原路退款。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/strategy/impl/WechatRefundStrategy.java`

```java
package io.github.atengk.pattern.combination.statestrategy.strategy.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.combination.statestrategy.context.OrderStateContext;
import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionRequest;
import io.github.atengk.pattern.combination.statestrategy.enums.PaymentTypeEnum;
import io.github.atengk.pattern.combination.statestrategy.strategy.RefundStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 微信退款策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class WechatRefundStrategy implements RefundStrategy {

    /**
     * 当前策略支持的支付方式
     *
     * @return 支付方式
     */
    @Override
    public PaymentTypeEnum paymentType() {
        return PaymentTypeEnum.WECHAT;
    }

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    @Override
    public String strategyName() {
        return "微信支付原路退款策略";
    }

    /**
     * 执行微信退款
     *
     * @param context 订单状态上下文
     */
    @Override
    public void refund(OrderStateContext context) {
        OrderActionRequest request = context.getRequest();
        BigDecimal refundAmount = NumberUtil.round(request.getOrderAmount(), 2);
        String refundNo = "WX_REFUND_" + IdUtil.fastSimpleUUID();

        context.setRefundNo(refundNo);
        context.setRefundAmount(refundAmount);
        context.setRefundStrategyName(strategyName());

        log.info("执行微信支付退款，订单编号：{}，退款流水号：{}，退款金额：{}",
                request.getOrderNo(), refundNo, refundAmount);
    }
}
```

### 余额退款策略

余额退款策略用于模拟直接退回用户余额账户。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/strategy/impl/BalanceRefundStrategy.java`

```java
package io.github.atengk.pattern.combination.statestrategy.strategy.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.combination.statestrategy.context.OrderStateContext;
import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionRequest;
import io.github.atengk.pattern.combination.statestrategy.enums.PaymentTypeEnum;
import io.github.atengk.pattern.combination.statestrategy.strategy.RefundStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 余额退款策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class BalanceRefundStrategy implements RefundStrategy {

    /**
     * 当前策略支持的支付方式
     *
     * @return 支付方式
     */
    @Override
    public PaymentTypeEnum paymentType() {
        return PaymentTypeEnum.BALANCE;
    }

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    @Override
    public String strategyName() {
        return "余额账户退款策略";
    }

    /**
     * 执行余额退款
     *
     * @param context 订单状态上下文
     */
    @Override
    public void refund(OrderStateContext context) {
        OrderActionRequest request = context.getRequest();
        BigDecimal refundAmount = NumberUtil.round(request.getOrderAmount(), 2);
        String refundNo = "BAL_REFUND_" + IdUtil.fastSimpleUUID();

        context.setRefundNo(refundNo);
        context.setRefundAmount(refundAmount);
        context.setRefundStrategyName(strategyName());

        log.info("执行余额退款，订单编号：{}，用户ID：{}，退款流水号：{}，退款金额：{}",
                request.getOrderNo(), request.getUserId(), refundNo, refundAmount);
    }
}
```

### 退款策略工厂

退款策略工厂负责根据支付方式获取对应退款策略。它让状态类不用直接依赖具体退款策略实现。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/factory/RefundStrategyFactory.java`

```java
package io.github.atengk.pattern.combination.statestrategy.factory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.combination.statestrategy.enums.PaymentTypeEnum;
import io.github.atengk.pattern.combination.statestrategy.strategy.RefundStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 退款策略工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class RefundStrategyFactory {

    private final Map<PaymentTypeEnum, RefundStrategy> strategyMap;

    /**
     * 初始化退款策略工厂
     *
     * @param strategyList 退款策略集合
     */
    public RefundStrategyFactory(List<RefundStrategy> strategyList) {
        if (CollUtil.isEmpty(strategyList)) {
            throw new IllegalStateException("退款策略不能为空");
        }

        Map<PaymentTypeEnum, RefundStrategy> tempStrategyMap = new EnumMap<>(PaymentTypeEnum.class);
        for (RefundStrategy strategy : strategyList) {
            RefundStrategy oldStrategy = tempStrategyMap.put(strategy.paymentType(), strategy);
            if (ObjectUtil.isNotNull(oldStrategy)) {
                throw new IllegalStateException("退款策略重复注册：" + strategy.paymentType().getCode());
            }
        }

        this.strategyMap = Collections.unmodifiableMap(tempStrategyMap);
        log.info("退款策略工厂初始化完成，策略数量：{}", this.strategyMap.size());
    }

    /**
     * 根据支付方式获取退款策略
     *
     * @param paymentTypeCode 支付方式编码
     * @return 退款策略
     */
    public RefundStrategy getStrategy(String paymentTypeCode) {
        PaymentTypeEnum paymentType = PaymentTypeEnum.of(paymentTypeCode);
        RefundStrategy strategy = strategyMap.get(paymentType);
        if (ObjectUtil.isNull(strategy)) {
            throw new IllegalArgumentException("未找到退款策略：" + paymentTypeCode);
        }

        log.info("命中退款策略，支付方式：{}，策略类：{}",
                paymentType.getCode(), strategy.getClass().getSimpleName());
        return strategy;
    }
}
```

### 订单状态接口

订单状态接口定义不同状态下允许执行的行为。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/state/OrderState.java`

```java
package io.github.atengk.pattern.combination.statestrategy.state;

import io.github.atengk.pattern.combination.statestrategy.context.OrderStateContext;
import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionResponse;
import io.github.atengk.pattern.combination.statestrategy.enums.OrderStatusEnum;

/**
 * 订单状态接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderState {

    /**
     * 当前状态
     *
     * @return 订单状态
     */
    OrderStatusEnum status();

    /**
     * 支付订单
     *
     * @param context 订单状态上下文
     * @return 订单动作响应
     */
    OrderActionResponse pay(OrderStateContext context);

    /**
     * 发货订单
     *
     * @param context 订单状态上下文
     * @return 订单动作响应
     */
    OrderActionResponse ship(OrderStateContext context);

    /**
     * 确认收货
     *
     * @param context 订单状态上下文
     * @return 订单动作响应
     */
    OrderActionResponse confirmReceive(OrderStateContext context);

    /**
     * 取消订单
     *
     * @param context 订单状态上下文
     * @return 订单动作响应
     */
    OrderActionResponse cancel(OrderStateContext context);
}
```

### 抽象订单状态

抽象状态类提供默认不支持的行为，具体状态类只需要重写自己支持的动作。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/state/AbstractOrderState.java`

```java
package io.github.atengk.pattern.combination.statestrategy.state;

import io.github.atengk.pattern.combination.statestrategy.context.OrderStateContext;
import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionRequest;
import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionResponse;
import io.github.atengk.pattern.combination.statestrategy.enums.OrderStatusEnum;

/**
 * 抽象订单状态
 *
 * @author Ateng
 * @since 2026-05-13
 */
public abstract class AbstractOrderState implements OrderState {

    /**
     * 支付订单
     *
     * @param context 订单状态上下文
     * @return 订单动作响应
     */
    @Override
    public OrderActionResponse pay(OrderStateContext context) {
        return unsupported(context, "支付");
    }

    /**
     * 发货订单
     *
     * @param context 订单状态上下文
     * @return 订单动作响应
     */
    @Override
    public OrderActionResponse ship(OrderStateContext context) {
        return unsupported(context, "发货");
    }

    /**
     * 确认收货
     *
     * @param context 订单状态上下文
     * @return 订单动作响应
     */
    @Override
    public OrderActionResponse confirmReceive(OrderStateContext context) {
        return unsupported(context, "确认收货");
    }

    /**
     * 取消订单
     *
     * @param context 订单状态上下文
     * @return 订单动作响应
     */
    @Override
    public OrderActionResponse cancel(OrderStateContext context) {
        return unsupported(context, "取消");
    }

    /**
     * 构建状态流转响应
     *
     * @param context   订单状态上下文
     * @param newStatus 新状态
     * @param message   响应消息
     * @return 订单动作响应
     */
    protected OrderActionResponse success(OrderStateContext context, OrderStatusEnum newStatus, String message) {
        OrderActionRequest request = context.getRequest();

        return OrderActionResponse.builder()
                .orderNo(request.getOrderNo())
                .oldStatus(status().getCode())
                .newStatus(newStatus.getCode())
                .action(request.getAction())
                .success(true)
                .refundNo(context.getRefundNo())
                .refundStrategyName(context.getRefundStrategyName())
                .refundAmount(context.getRefundAmount())
                .message(message)
                .build();
    }

    /**
     * 构建不支持动作响应
     *
     * @param context    订单状态上下文
     * @param actionName 动作名称
     * @return 订单动作响应
     */
    private OrderActionResponse unsupported(OrderStateContext context, String actionName) {
        throw new IllegalArgumentException(status().getDescription() + "状态不支持" + actionName + "操作");
    }
}
```

### 待支付订单状态

待支付状态支持支付和取消。待支付订单取消时不需要退款策略。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/state/impl/PendingPaymentOrderState.java`

```java
package io.github.atengk.pattern.combination.statestrategy.state.impl;

import io.github.atengk.pattern.combination.statestrategy.context.OrderStateContext;
import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionResponse;
import io.github.atengk.pattern.combination.statestrategy.enums.OrderStatusEnum;
import io.github.atengk.pattern.combination.statestrategy.state.AbstractOrderState;
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
public class PendingPaymentOrderState extends AbstractOrderState {

    /**
     * 当前状态
     *
     * @return 订单状态
     */
    @Override
    public OrderStatusEnum status() {
        return OrderStatusEnum.PENDING_PAYMENT;
    }

    /**
     * 支付订单
     *
     * @param context 订单状态上下文
     * @return 订单动作响应
     */
    @Override
    public OrderActionResponse pay(OrderStateContext context) {
        log.info("待支付订单执行支付，订单编号：{}，用户ID：{}",
                context.getRequest().getOrderNo(), context.getRequest().getUserId());

        return success(context, OrderStatusEnum.PAID, "订单支付成功");
    }

    /**
     * 取消订单
     *
     * @param context 订单状态上下文
     * @return 订单动作响应
     */
    @Override
    public OrderActionResponse cancel(OrderStateContext context) {
        log.info("待支付订单直接取消，订单编号：{}，原因：{}",
                context.getRequest().getOrderNo(), context.getRequest().getReason());

        return success(context, OrderStatusEnum.CANCELED, "待支付订单取消成功");
    }
}
```

### 已支付订单状态

已支付状态支持发货和取消。取消时需要根据支付方式选择退款策略。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/state/impl/PaidOrderState.java`

```java
package io.github.atengk.pattern.combination.statestrategy.state.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.statestrategy.context.OrderStateContext;
import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionResponse;
import io.github.atengk.pattern.combination.statestrategy.enums.OrderStatusEnum;
import io.github.atengk.pattern.combination.statestrategy.factory.RefundStrategyFactory;
import io.github.atengk.pattern.combination.statestrategy.state.AbstractOrderState;
import io.github.atengk.pattern.combination.statestrategy.strategy.RefundStrategy;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class PaidOrderState extends AbstractOrderState {

    private final RefundStrategyFactory refundStrategyFactory;

    /**
     * 当前状态
     *
     * @return 订单状态
     */
    @Override
    public OrderStatusEnum status() {
        return OrderStatusEnum.PAID;
    }

    /**
     * 发货订单
     *
     * @param context 订单状态上下文
     * @return 订单动作响应
     */
    @Override
    public OrderActionResponse ship(OrderStateContext context) {
        log.info("已支付订单执行发货，订单编号：{}，用户ID：{}",
                context.getRequest().getOrderNo(), context.getRequest().getUserId());

        return success(context, OrderStatusEnum.SHIPPED, "订单发货成功");
    }

    /**
     * 取消订单并退款
     *
     * @param context 订单状态上下文
     * @return 订单动作响应
     */
    @Override
    public OrderActionResponse cancel(OrderStateContext context) {
        if (StrUtil.isBlank(context.getRequest().getPaymentType())) {
            throw new IllegalArgumentException("已支付订单取消时，支付方式不能为空");
        }

        RefundStrategy refundStrategy = refundStrategyFactory.getStrategy(context.getRequest().getPaymentType());
        refundStrategy.refund(context);

        log.info("已支付订单取消完成，订单编号：{}，退款策略：{}，退款流水号：{}",
                context.getRequest().getOrderNo(), context.getRefundStrategyName(), context.getRefundNo());

        return success(context, OrderStatusEnum.CANCELED, "已支付订单取消成功，退款已发起");
    }
}
```

### 已发货订单状态

已发货状态支持确认收货，不支持取消。真实项目中如果已发货也允许取消，可以引入退货退款流程。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/state/impl/ShippedOrderState.java`

```java
package io.github.atengk.pattern.combination.statestrategy.state.impl;

import io.github.atengk.pattern.combination.statestrategy.context.OrderStateContext;
import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionResponse;
import io.github.atengk.pattern.combination.statestrategy.enums.OrderStatusEnum;
import io.github.atengk.pattern.combination.statestrategy.state.AbstractOrderState;
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
public class ShippedOrderState extends AbstractOrderState {

    /**
     * 当前状态
     *
     * @return 订单状态
     */
    @Override
    public OrderStatusEnum status() {
        return OrderStatusEnum.SHIPPED;
    }

    /**
     * 确认收货
     *
     * @param context 订单状态上下文
     * @return 订单动作响应
     */
    @Override
    public OrderActionResponse confirmReceive(OrderStateContext context) {
        log.info("已发货订单确认收货，订单编号：{}，用户ID：{}",
                context.getRequest().getOrderNo(), context.getRequest().getUserId());

        return success(context, OrderStatusEnum.COMPLETED, "订单确认收货成功");
    }
}
```

### 已完成订单状态

已完成状态不支持继续流转。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/state/impl/CompletedOrderState.java`

```java
package io.github.atengk.pattern.combination.statestrategy.state.impl;

import io.github.atengk.pattern.combination.statestrategy.enums.OrderStatusEnum;
import io.github.atengk.pattern.combination.statestrategy.state.AbstractOrderState;
import org.springframework.stereotype.Component;

/**
 * 已完成订单状态
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Component
public class CompletedOrderState extends AbstractOrderState {

    /**
     * 当前状态
     *
     * @return 订单状态
     */
    @Override
    public OrderStatusEnum status() {
        return OrderStatusEnum.COMPLETED;
    }
}
```

### 已取消订单状态

已取消状态不支持继续流转。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/state/impl/CanceledOrderState.java`

```java
package io.github.atengk.pattern.combination.statestrategy.state.impl;

import io.github.atengk.pattern.combination.statestrategy.enums.OrderStatusEnum;
import io.github.atengk.pattern.combination.statestrategy.state.AbstractOrderState;
import org.springframework.stereotype.Component;

/**
 * 已取消订单状态
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Component
public class CanceledOrderState extends AbstractOrderState {

    /**
     * 当前状态
     *
     * @return 订单状态
     */
    @Override
    public OrderStatusEnum status() {
        return OrderStatusEnum.CANCELED;
    }
}
```

### 订单状态工厂

订单状态工厂负责根据当前状态获取对应状态对象。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/factory/OrderStateFactory.java`

```java
package io.github.atengk.pattern.combination.statestrategy.factory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.combination.statestrategy.enums.OrderStatusEnum;
import io.github.atengk.pattern.combination.statestrategy.state.OrderState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 订单状态工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderStateFactory {

    private final Map<OrderStatusEnum, OrderState> stateMap;

    /**
     * 初始化订单状态工厂
     *
     * @param stateList 订单状态集合
     */
    public OrderStateFactory(List<OrderState> stateList) {
        if (CollUtil.isEmpty(stateList)) {
            throw new IllegalStateException("订单状态处理器不能为空");
        }

        Map<OrderStatusEnum, OrderState> tempStateMap = new EnumMap<>(OrderStatusEnum.class);
        for (OrderState state : stateList) {
            OrderState oldState = tempStateMap.put(state.status(), state);
            if (ObjectUtil.isNotNull(oldState)) {
                throw new IllegalStateException("订单状态处理器重复注册：" + state.status().getCode());
            }
        }

        this.stateMap = Collections.unmodifiableMap(tempStateMap);
        log.info("订单状态工厂初始化完成，状态数量：{}", this.stateMap.size());
    }

    /**
     * 根据订单状态获取状态处理器
     *
     * @param statusCode 订单状态编码
     * @return 订单状态处理器
     */
    public OrderState getState(String statusCode) {
        OrderStatusEnum status = OrderStatusEnum.of(statusCode);
        OrderState state = stateMap.get(status);
        if (ObjectUtil.isNull(state)) {
            throw new IllegalArgumentException("未找到订单状态处理器：" + statusCode);
        }

        log.info("命中订单状态处理器，订单状态：{}，处理器类：{}",
                status.getCode(), state.getClass().getSimpleName());
        return state;
    }
}
```

### Service 接口

Service 接口对外暴露订单状态流转能力。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/service/OrderStateService.java`

```java
package io.github.atengk.pattern.combination.statestrategy.service;

import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionRequest;
import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionResponse;

/**
 * 订单状态服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderStateService {

    /**
     * 执行订单动作
     *
     * @param request 订单动作请求
     * @return 订单动作响应
     */
    OrderActionResponse execute(OrderActionRequest request);
}
```

### Service 实现类

Service 实现类负责编排：根据当前状态获取状态对象，再根据动作调用状态对象对应行为。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/service/impl/OrderStateServiceImpl.java`

```java
package io.github.atengk.pattern.combination.statestrategy.service.impl;

import io.github.atengk.pattern.combination.statestrategy.context.OrderStateContext;
import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionRequest;
import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionResponse;
import io.github.atengk.pattern.combination.statestrategy.enums.OrderActionEnum;
import io.github.atengk.pattern.combination.statestrategy.factory.OrderStateFactory;
import io.github.atengk.pattern.combination.statestrategy.service.OrderStateService;
import io.github.atengk.pattern.combination.statestrategy.state.OrderState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    private final OrderStateFactory orderStateFactory;

    /**
     * 执行订单动作
     *
     * @param request 订单动作请求
     * @return 订单动作响应
     */
    @Override
    public OrderActionResponse execute(OrderActionRequest request) {
        OrderState state = orderStateFactory.getState(request.getCurrentStatus());
        OrderStateContext context = OrderStateContext.builder()
                .request(request)
                .build();

        OrderActionEnum action = OrderActionEnum.of(request.getAction());
        OrderActionResponse response = switch (action) {
            case PAY -> state.pay(context);
            case SHIP -> state.ship(context);
            case CONFIRM_RECEIVE -> state.confirmReceive(context);
            case CANCEL -> state.cancel(context);
        };

        log.info("订单状态动作执行完成，订单编号：{}，动作：{}，原状态：{}，新状态：{}",
                response.getOrderNo(), response.getAction(), response.getOldStatus(), response.getNewStatus());

        return response;
    }
}
```

### 统一响应对象

统一响应对象用于包装接口返回结果。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/result/Result.java`

```java
package io.github.atengk.pattern.combination.statestrategy.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 响应编码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一响应结果
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 失败响应
     *
     * @param code    响应编码
     * @param message 响应消息
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }
}
```

### 全局异常处理器

全局异常处理器用于统一处理状态流转异常和参数异常。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.combination.statestrategy.handler;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.statestrategy.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
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
     * 处理参数校验异常
     *
     * @param exception 参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = ObjectUtil.isNotNull(fieldError)
                ? StrUtil.blankToDefault(fieldError.getDefaultMessage(), "请求参数不合法")
                : "请求参数不合法";

        log.warn("请求参数校验失败：{}", message);
        return Result.fail(400, message);
    }

    /**
     * 处理非法参数异常
     *
     * @param exception 非法参数异常
     * @return 统一响应结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("订单状态流转异常：{}", exception.getMessage());
        return Result.fail(400, exception.getMessage());
    }

    /**
     * 处理系统异常
     *
     * @param exception 系统异常
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return Result.fail(500, "系统繁忙，请稍后再试");
    }
}
```

### Controller

Controller 提供订单状态动作执行接口。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/controller/OrderStateController.java`

```java
package io.github.atengk.pattern.combination.statestrategy.controller;

import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionRequest;
import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionResponse;
import io.github.atengk.pattern.combination.statestrategy.result.Result;
import io.github.atengk.pattern.combination.statestrategy.service.OrderStateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单状态控制器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order-states")
public class OrderStateController {

    private final OrderStateService orderStateService;

    /**
     * 执行订单状态动作
     *
     * @param request 订单动作请求
     * @return 订单动作响应
     */
    @PostMapping("/execute")
    public Result<OrderActionResponse> execute(@Valid @RequestBody OrderActionRequest request) {
        return Result.success(orderStateService.execute(request));
    }
}
```

## 使用方式

启动 Spring Boot 项目后，可以通过订单状态动作接口验证状态模式和策略模式是否同时生效。

接口信息如下：

```text
请求地址：POST /api/order-states/execute
Content-Type：application/json
```

待支付订单执行支付请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-states/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "OD202605130001",
    "userId": 1001,
    "currentStatus": "PENDING_PAYMENT",
    "action": "PAY",
    "paymentType": "ALIPAY",
    "orderAmount": 199.99,
    "reason": "用户支付订单"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "OD202605130001",
    "oldStatus": "PENDING_PAYMENT",
    "newStatus": "PAID",
    "action": "PAY",
    "success": true,
    "refundNo": null,
    "refundStrategyName": null,
    "refundAmount": null,
    "message": "订单支付成功"
  }
}
```

已支付订单执行发货请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-states/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "OD202605130002",
    "userId": 1002,
    "currentStatus": "PAID",
    "action": "SHIP",
    "paymentType": "WECHAT",
    "orderAmount": 299.99,
    "reason": "仓库发货"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "OD202605130002",
    "oldStatus": "PAID",
    "newStatus": "SHIPPED",
    "action": "SHIP",
    "success": true,
    "refundNo": null,
    "refundStrategyName": null,
    "refundAmount": null,
    "message": "订单发货成功"
  }
}
```

已支付订单取消并走支付宝退款请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-states/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "OD202605130003",
    "userId": 1003,
    "currentStatus": "PAID",
    "action": "CANCEL",
    "paymentType": "ALIPAY",
    "orderAmount": 399.99,
    "reason": "用户取消订单"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "OD202605130003",
    "oldStatus": "PAID",
    "newStatus": "CANCELED",
    "action": "CANCEL",
    "success": true,
    "refundNo": "ALI_REFUND_7b4f4d72f7f74e9f94e31f4b5f1b0b68",
    "refundStrategyName": "支付宝原路退款策略",
    "refundAmount": 399.99,
    "message": "已支付订单取消成功，退款已发起"
  }
}
```

已支付订单取消并走余额退款请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-states/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "OD202605130004",
    "userId": 1004,
    "currentStatus": "PAID",
    "action": "CANCEL",
    "paymentType": "BALANCE",
    "orderAmount": 99.99,
    "reason": "用户取消订单"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "OD202605130004",
    "oldStatus": "PAID",
    "newStatus": "CANCELED",
    "action": "CANCEL",
    "success": true,
    "refundNo": "BAL_REFUND_e0df86c936694b338580957b20d307cb",
    "refundStrategyName": "余额账户退款策略",
    "refundAmount": 99.99,
    "message": "已支付订单取消成功，退款已发起"
  }
}
```

已完成订单尝试取消请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-states/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "OD202605130005",
    "userId": 1005,
    "currentStatus": "COMPLETED",
    "action": "CANCEL",
    "paymentType": "WECHAT",
    "orderAmount": 199.99,
    "reason": "用户取消订单"
  }'
```

响应示例：

```json
{
  "code": 400,
  "message": "已完成状态不支持取消操作",
  "data": null
}
```

## 新增订单状态

当业务新增一种状态时，例如新增“售后中”状态，可以新增枚举和状态类，不需要修改已有状态类。

第一步，在 `OrderStatusEnum` 中新增枚举：

```java
AFTER_SALE("AFTER_SALE", "售后中");
```

第二步，新增售后中状态类。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/state/impl/AfterSaleOrderState.java`

```java
package io.github.atengk.pattern.combination.statestrategy.state.impl;

import io.github.atengk.pattern.combination.statestrategy.context.OrderStateContext;
import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionResponse;
import io.github.atengk.pattern.combination.statestrategy.enums.OrderStatusEnum;
import io.github.atengk.pattern.combination.statestrategy.state.AbstractOrderState;
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
public class AfterSaleOrderState extends AbstractOrderState {

    /**
     * 当前状态
     *
     * @return 订单状态
     */
    @Override
    public OrderStatusEnum status() {
        return OrderStatusEnum.AFTER_SALE;
    }

    /**
     * 取消售后订单
     *
     * @param context 订单状态上下文
     * @return 订单动作响应
     */
    @Override
    public OrderActionResponse cancel(OrderStateContext context) {
        log.info("售后中订单取消售后流程，订单编号：{}，原因：{}",
                context.getRequest().getOrderNo(), context.getRequest().getReason());

        return success(context, OrderStatusEnum.COMPLETED, "售后取消成功，订单恢复已完成状态");
    }
}
```

新增完成后，`OrderStateFactory` 会自动收集 `AfterSaleOrderState`。只要 `status()` 不重复，系统就能识别新状态。

## 新增退款策略

当业务新增一种支付方式时，例如新增“银行卡支付”，只需要新增支付方式枚举和退款策略，不需要修改 `PaidOrderState`。

第一步，在 `PaymentTypeEnum` 中新增枚举：

```java
BANK_CARD("BANK_CARD", "银行卡支付");
```

第二步，新增银行卡退款策略。

文件位置：`src/main/java/io/github/atengk/pattern/combination/statestrategy/strategy/impl/BankCardRefundStrategy.java`

```java
package io.github.atengk.pattern.combination.statestrategy.strategy.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.combination.statestrategy.context.OrderStateContext;
import io.github.atengk.pattern.combination.statestrategy.dto.OrderActionRequest;
import io.github.atengk.pattern.combination.statestrategy.enums.PaymentTypeEnum;
import io.github.atengk.pattern.combination.statestrategy.strategy.RefundStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 银行卡退款策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class BankCardRefundStrategy implements RefundStrategy {

    /**
     * 当前策略支持的支付方式
     *
     * @return 支付方式
     */
    @Override
    public PaymentTypeEnum paymentType() {
        return PaymentTypeEnum.BANK_CARD;
    }

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    @Override
    public String strategyName() {
        return "银行卡退款策略";
    }

    /**
     * 执行银行卡退款
     *
     * @param context 订单状态上下文
     */
    @Override
    public void refund(OrderStateContext context) {
        OrderActionRequest request = context.getRequest();
        BigDecimal refundAmount = NumberUtil.round(request.getOrderAmount(), 2);
        String refundNo = "BANK_REFUND_" + IdUtil.fastSimpleUUID();

        context.setRefundNo(refundNo);
        context.setRefundAmount(refundAmount);
        context.setRefundStrategyName(strategyName());

        log.info("执行银行卡退款，订单编号：{}，退款流水号：{}，退款金额：{}",
                request.getOrderNo(), refundNo, refundAmount);
    }
}
```

新增完成后，`RefundStrategyFactory` 会自动收集 `BankCardRefundStrategy`。`PaidOrderState` 不需要新增 `if else`。

## 验证方式

可以从以下几个方面验证组合模式是否生效：

```text
1. 启动项目时，日志应输出订单状态工厂初始化完成
2. 启动项目时，日志应输出退款策略工厂初始化完成
3. PENDING_PAYMENT 状态执行 PAY 时，状态变为 PAID
4. PAID 状态执行 SHIP 时，状态变为 SHIPPED
5. SHIPPED 状态执行 CONFIRM_RECEIVE 时，状态变为 COMPLETED
6. PAID 状态执行 CANCEL 时，会根据 paymentType 命中不同退款策略
7. COMPLETED 和 CANCELED 状态执行流转动作时，应返回不支持操作
8. 新增状态时，不需要修改已有状态类
9. 新增退款策略时，不需要修改 PaidOrderState
```

正常支付日志示例：

```text
订单状态工厂初始化完成，状态数量：5
退款策略工厂初始化完成，策略数量：3
命中订单状态处理器，订单状态：PENDING_PAYMENT，处理器类：PendingPaymentOrderState
待支付订单执行支付，订单编号：OD202605130001，用户ID：1001
订单状态动作执行完成，订单编号：OD202605130001，动作：PAY，原状态：PENDING_PAYMENT，新状态：PAID
```

已支付订单取消退款日志示例：

```text
命中订单状态处理器，订单状态：PAID，处理器类：PaidOrderState
命中退款策略，支付方式：ALIPAY，策略类：AlipayRefundStrategy
执行支付宝退款，订单编号：OD202605130003，退款流水号：ALI_REFUND_7b4f4d72f7f74e9f94e31f4b5f1b0b68，退款金额：399.99
已支付订单取消完成，订单编号：OD202605130003，退款策略：支付宝原路退款策略，退款流水号：ALI_REFUND_7b4f4d72f7f74e9f94e31f4b5f1b0b68
订单状态动作执行完成，订单编号：OD202605130003，动作：CANCEL，原状态：PAID，新状态：CANCELED
```

非法流转日志示例：

```text
命中订单状态处理器，订单状态：COMPLETED，处理器类：CompletedOrderState
订单状态流转异常：已完成状态不支持取消操作
```

## 组合效果

状态模式和策略模式组合后，各自负责不同变化点：

| 模式   | 职责            | 在示例中的体现                                                                          |
| ---- | ------------- | -------------------------------------------------------------------------------- |
| 状态模式 | 封装不同状态下的行为    | `PendingPaymentOrderState`、`PaidOrderState`、`ShippedOrderState` 等分别处理不同状态行为      |
| 策略模式 | 封装某个状态行为内部的算法 | `AlipayRefundStrategy`、`WechatRefundStrategy`、`BalanceRefundStrategy` 分别处理不同退款规则 |

这种组合适合处理下面两类变化：

```text
第一类变化：对象状态会变化
例如订单从待支付、已支付、已发货、已完成、已取消之间流转

第二类变化：某个状态行为内部算法会变化
例如已支付订单取消时，不同支付方式需要不同退款算法
```

相比只使用状态模式，这个组合可以避免某个状态类内部堆积大量算法分支。相比只使用策略模式，这个组合可以清晰表达不同状态下允许执行的行为和禁止执行的行为。

## 注意事项

状态模式和策略模式组合使用时，要明确两者边界：

```text
状态模式负责状态行为
策略模式负责行为算法
```

不要把所有支付方式退款逻辑都写进 `PaidOrderState`。状态类应该表达“已支付状态允许取消，并且取消时需要退款”，具体怎么退款应该交给退款策略处理。

也不要让退款策略反过来控制订单状态流转。退款策略只负责退款算法，不应该决定订单是变成已取消、售后中还是已完成。状态流转应该由状态类或状态机统一控制。

如果业务只有简单状态字段，没有明显的状态行为差异，直接使用枚举和普通判断即可。只有当不同状态下允许的行为、流转规则、异常处理明显不同，并且某些行为内部还存在算法变化时，才适合使用“状态模式 + 策略模式”。

这个组合最适合的判断标准是：**状态行为归状态，行为算法归策略**。
