# 模板方法模式 + 策略模式

模板方法模式和策略模式组合使用时，通常用于处理 **主流程稳定，但流程中的某个步骤需要灵活替换** 的业务场景。

模板方法模式负责固定业务流程骨架，保证流程执行顺序一致；策略模式负责封装流程中某个可变步骤的具体算法或业务规则。这个组合在 Spring Boot 项目中很常见，例如订单提交、支付处理、文件导入、审批流转、消息发送、任务执行等场景。

```text
模板方法模式：固定主流程骨架
策略模式：替换主流程中的某个可变步骤
```

## 适用场景

本示例以“订单提交”为业务场景。订单提交的主流程通常比较稳定：

```text
参数校验
  -> 选择计价策略
      -> 计算订单应付金额
          -> 生成订单编号
              -> 构建订单提交结果
                  -> 执行提交后处理
```

但是订单计价规则会随着订单类型变化：

| 订单类型      | 说明   | 计价规则   |
| --------- | ---- | ------ |
| NORMAL    | 普通订单 | 原价支付   |
| VIP       | 会员订单 | 享受 9 折 |
| PROMOTION | 活动订单 | 享受 8 折 |

如果把流程控制和所有计价规则都写在一个 Service 中，代码会逐渐变成：

```text
Service 既控制订单提交流程
Service 又处理不同订单类型计价
Service 还要处理订单编号、日志、后置动作
```

这种写法会导致主流程和变化点混在一起。使用“模板方法模式 + 策略模式”后，结构变成：

```text
Controller
  -> Service
      -> 订单提交模板：固定订单提交主流程
          -> 计价策略工厂：根据订单类型获取计价策略
              -> 具体计价策略：执行不同计价算法
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

    <!-- Hutool：提供字符串、数字、ID、对象判断等常用工具 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok：减少 Getter、Setter、构造器、日志等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

建议目录结构如下：

```text
src/main/java/io/github/atengk/pattern/combination/templatestrategy
├── controller
│   └── OrderSubmitController.java
├── dto
│   ├── OrderSubmitRequest.java
│   └── OrderSubmitResponse.java
├── enums
│   └── OrderTypeEnum.java
├── factory
│   └── OrderPriceStrategyFactory.java
├── service
│   ├── OrderSubmitService.java
│   └── impl
│       └── OrderSubmitServiceImpl.java
├── strategy
│   ├── OrderPriceStrategy.java
│   └── impl
│       ├── NormalOrderPriceStrategy.java
│       ├── PromotionOrderPriceStrategy.java
│       └── VipOrderPriceStrategy.java
├── template
│   ├── AbstractOrderSubmitTemplate.java
│   └── DefaultOrderSubmitTemplate.java
└── util
    └── MoneyUtils.java
```

## 核心代码

这一部分给出核心实现。重点看两个位置：

```text
AbstractOrderSubmitTemplate：模板方法，固定订单提交主流程
OrderPriceStrategy：策略接口，封装不同订单计价算法
```

模板方法不关心具体订单类型如何计价，只负责在固定流程中调用计价步骤。计价步骤内部再通过策略模式完成扩展。

### 订单类型枚举

订单类型枚举用于统一维护订单类型编码，避免在代码中散落字符串常量。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatestrategy/enums/OrderTypeEnum.java`

```java
package io.github.atengk.pattern.combination.templatestrategy.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 订单类型枚举
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
@RequiredArgsConstructor
public enum OrderTypeEnum {

    NORMAL("NORMAL", "普通订单"),

    VIP("VIP", "会员订单"),

    PROMOTION("PROMOTION", "活动订单");

    private final String code;

    private final String description;

    /**
     * 根据编码获取订单类型
     *
     * @param code 订单类型编码
     * @return 订单类型枚举
     */
    public static OrderTypeEnum of(String code) {
        String actualCode = StrUtil.trimToEmpty(code);
        for (OrderTypeEnum item : values()) {
            if (StrUtil.equalsIgnoreCase(item.getCode(), actualCode)) {
                return item;
            }
        }
        throw new IllegalArgumentException("不支持的订单类型：" + code);
    }
}
```

### 订单提交请求对象

订单提交请求对象用于接收订单提交参数。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatestrategy/dto/OrderSubmitRequest.java`

```java
package io.github.atengk.pattern.combination.templatestrategy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单提交请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderSubmitRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 订单类型：NORMAL、VIP、PROMOTION
     */
    @NotBlank(message = "订单类型不能为空")
    private String orderType;

    /**
     * 商品原始金额
     */
    @NotNull(message = "商品原始金额不能为空")
    @DecimalMin(value = "0.01", message = "商品原始金额必须大于0")
    private BigDecimal originAmount;
}
```

### 订单提交响应对象

订单提交响应对象用于返回订单编号、订单类型、原始金额、优惠金额和应付金额。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatestrategy/dto/OrderSubmitResponse.java`

```java
package io.github.atengk.pattern.combination.templatestrategy.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单提交响应
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderSubmitResponse {

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 订单类型
     */
    private String orderType;

    /**
     * 计价策略名称
     */
    private String strategyName;

    /**
     * 原始金额
     */
    private BigDecimal originAmount;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 应付金额
     */
    private BigDecimal payableAmount;

    /**
     * 订单状态
     */
    private String orderStatus;
}
```

### 金额工具类

金额工具类用于统一处理金额计算，避免每个策略重复处理精度和小数位。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatestrategy/util/MoneyUtils.java`

```java
package io.github.atengk.pattern.combination.templatestrategy.util;

import cn.hutool.core.util.NumberUtil;

import java.math.BigDecimal;

/**
 * 金额工具类
 *
 * @author Ateng
 * @since 2026-05-13
 */
public final class MoneyUtils {

    private MoneyUtils() {
    }

    /**
     * 金额乘法并保留两位小数
     *
     * @param amount 金额
     * @param rate   比例
     * @return 计算结果
     */
    public static BigDecimal multiply(BigDecimal amount, BigDecimal rate) {
        return NumberUtil.round(NumberUtil.mul(amount, rate), 2);
    }

    /**
     * 金额减法并保留两位小数
     *
     * @param amount   原始金额
     * @param discount 优惠金额
     * @return 计算结果
     */
    public static BigDecimal subtract(BigDecimal amount, BigDecimal discount) {
        return NumberUtil.round(NumberUtil.sub(amount, discount), 2);
    }

    /**
     * 金额保留两位小数
     *
     * @param amount 金额
     * @return 处理后的金额
     */
    public static BigDecimal round(BigDecimal amount) {
        return NumberUtil.round(amount, 2);
    }
}
```

### 计价策略接口

计价策略接口定义不同订单类型的计价行为。模板方法只依赖这个接口，不依赖具体策略实现类。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatestrategy/strategy/OrderPriceStrategy.java`

```java
package io.github.atengk.pattern.combination.templatestrategy.strategy;

import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitRequest;
import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitResponse;
import io.github.atengk.pattern.combination.templatestrategy.enums.OrderTypeEnum;

/**
 * 订单计价策略接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderPriceStrategy {

    /**
     * 当前策略支持的订单类型
     *
     * @return 订单类型
     */
    OrderTypeEnum orderType();

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    String strategyName();

    /**
     * 计算订单价格
     *
     * @param request 订单提交请求
     * @return 订单提交响应
     */
    OrderSubmitResponse calculate(OrderSubmitRequest request);
}
```

### 普通订单计价策略

普通订单按照原价支付，不产生优惠金额。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatestrategy/strategy/impl/NormalOrderPriceStrategy.java`

```java
package io.github.atengk.pattern.combination.templatestrategy.strategy.impl;

import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitRequest;
import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitResponse;
import io.github.atengk.pattern.combination.templatestrategy.enums.OrderTypeEnum;
import io.github.atengk.pattern.combination.templatestrategy.strategy.OrderPriceStrategy;
import io.github.atengk.pattern.combination.templatestrategy.util.MoneyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 普通订单计价策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class NormalOrderPriceStrategy implements OrderPriceStrategy {

    /**
     * 当前策略支持的订单类型
     *
     * @return 订单类型
     */
    @Override
    public OrderTypeEnum orderType() {
        return OrderTypeEnum.NORMAL;
    }

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    @Override
    public String strategyName() {
        return "普通订单计价策略";
    }

    /**
     * 计算普通订单价格
     *
     * @param request 订单提交请求
     * @return 订单提交响应
     */
    @Override
    public OrderSubmitResponse calculate(OrderSubmitRequest request) {
        BigDecimal originAmount = MoneyUtils.round(request.getOriginAmount());
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal payableAmount = originAmount;

        log.info("普通订单计价完成，用户ID：{}，商品ID：{}，应付金额：{}",
                request.getUserId(), request.getProductId(), payableAmount);

        return OrderSubmitResponse.builder()
                .userId(request.getUserId())
                .productId(request.getProductId())
                .orderType(orderType().getCode())
                .strategyName(strategyName())
                .originAmount(originAmount)
                .discountAmount(discountAmount)
                .payableAmount(payableAmount)
                .build();
    }
}
```

### 会员订单计价策略

会员订单享受 9 折优惠。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatestrategy/strategy/impl/VipOrderPriceStrategy.java`

```java
package io.github.atengk.pattern.combination.templatestrategy.strategy.impl;

import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitRequest;
import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitResponse;
import io.github.atengk.pattern.combination.templatestrategy.enums.OrderTypeEnum;
import io.github.atengk.pattern.combination.templatestrategy.strategy.OrderPriceStrategy;
import io.github.atengk.pattern.combination.templatestrategy.util.MoneyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 会员订单计价策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class VipOrderPriceStrategy implements OrderPriceStrategy {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");

    /**
     * 当前策略支持的订单类型
     *
     * @return 订单类型
     */
    @Override
    public OrderTypeEnum orderType() {
        return OrderTypeEnum.VIP;
    }

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    @Override
    public String strategyName() {
        return "会员订单计价策略";
    }

    /**
     * 计算会员订单价格
     *
     * @param request 订单提交请求
     * @return 订单提交响应
     */
    @Override
    public OrderSubmitResponse calculate(OrderSubmitRequest request) {
        BigDecimal originAmount = MoneyUtils.round(request.getOriginAmount());
        BigDecimal discountAmount = MoneyUtils.multiply(originAmount, DISCOUNT_RATE);
        BigDecimal payableAmount = MoneyUtils.subtract(originAmount, discountAmount);

        log.info("会员订单计价完成，用户ID：{}，商品ID：{}，优惠金额：{}，应付金额：{}",
                request.getUserId(), request.getProductId(), discountAmount, payableAmount);

        return OrderSubmitResponse.builder()
                .userId(request.getUserId())
                .productId(request.getProductId())
                .orderType(orderType().getCode())
                .strategyName(strategyName())
                .originAmount(originAmount)
                .discountAmount(discountAmount)
                .payableAmount(payableAmount)
                .build();
    }
}
```

### 活动订单计价策略

活动订单享受 8 折优惠。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatestrategy/strategy/impl/PromotionOrderPriceStrategy.java`

```java
package io.github.atengk.pattern.combination.templatestrategy.strategy.impl;

import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitRequest;
import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitResponse;
import io.github.atengk.pattern.combination.templatestrategy.enums.OrderTypeEnum;
import io.github.atengk.pattern.combination.templatestrategy.strategy.OrderPriceStrategy;
import io.github.atengk.pattern.combination.templatestrategy.util.MoneyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 活动订单计价策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class PromotionOrderPriceStrategy implements OrderPriceStrategy {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.20");

    /**
     * 当前策略支持的订单类型
     *
     * @return 订单类型
     */
    @Override
    public OrderTypeEnum orderType() {
        return OrderTypeEnum.PROMOTION;
    }

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    @Override
    public String strategyName() {
        return "活动订单计价策略";
    }

    /**
     * 计算活动订单价格
     *
     * @param request 订单提交请求
     * @return 订单提交响应
     */
    @Override
    public OrderSubmitResponse calculate(OrderSubmitRequest request) {
        BigDecimal originAmount = MoneyUtils.round(request.getOriginAmount());
        BigDecimal discountAmount = MoneyUtils.multiply(originAmount, DISCOUNT_RATE);
        BigDecimal payableAmount = MoneyUtils.subtract(originAmount, discountAmount);

        log.info("活动订单计价完成，用户ID：{}，商品ID：{}，优惠金额：{}，应付金额：{}",
                request.getUserId(), request.getProductId(), discountAmount, payableAmount);

        return OrderSubmitResponse.builder()
                .userId(request.getUserId())
                .productId(request.getProductId())
                .orderType(orderType().getCode())
                .strategyName(strategyName())
                .originAmount(originAmount)
                .discountAmount(discountAmount)
                .payableAmount(payableAmount)
                .build();
    }
}
```

### 计价策略工厂

策略工厂负责根据订单类型找到对应计价策略。它不是本节的组合主体，只是为了让模板方法更方便地选择策略。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatestrategy/factory/OrderPriceStrategyFactory.java`

```java
package io.github.atengk.pattern.combination.templatestrategy.factory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.combination.templatestrategy.enums.OrderTypeEnum;
import io.github.atengk.pattern.combination.templatestrategy.strategy.OrderPriceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 订单计价策略工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderPriceStrategyFactory {

    private final Map<OrderTypeEnum, OrderPriceStrategy> strategyMap;

    /**
     * 初始化订单计价策略工厂
     *
     * @param strategyList 订单计价策略集合
     */
    public OrderPriceStrategyFactory(List<OrderPriceStrategy> strategyList) {
        if (CollUtil.isEmpty(strategyList)) {
            throw new IllegalStateException("订单计价策略不能为空");
        }

        Map<OrderTypeEnum, OrderPriceStrategy> tempStrategyMap = new EnumMap<>(OrderTypeEnum.class);
        for (OrderPriceStrategy strategy : strategyList) {
            OrderPriceStrategy oldStrategy = tempStrategyMap.put(strategy.orderType(), strategy);
            if (ObjectUtil.isNotNull(oldStrategy)) {
                throw new IllegalStateException("订单计价策略重复注册：" + strategy.orderType().getCode());
            }
        }

        this.strategyMap = Collections.unmodifiableMap(tempStrategyMap);
        log.info("订单计价策略工厂初始化完成，策略数量：{}", this.strategyMap.size());
    }

    /**
     * 根据订单类型获取计价策略
     *
     * @param orderTypeCode 订单类型编码
     * @return 订单计价策略
     */
    public OrderPriceStrategy getStrategy(String orderTypeCode) {
        OrderTypeEnum orderType = OrderTypeEnum.of(orderTypeCode);
        OrderPriceStrategy strategy = strategyMap.get(orderType);
        if (ObjectUtil.isNull(strategy)) {
            throw new IllegalArgumentException("未找到订单计价策略：" + orderTypeCode);
        }

        log.info("命中订单计价策略，订单类型：{}，策略类：{}",
                orderType.getCode(), strategy.getClass().getSimpleName());
        return strategy;
    }
}
```

### 抽象订单提交模板

抽象模板类固定订单提交主流程。`submit` 方法就是模板方法，定义完整的订单提交步骤。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatestrategy/template/AbstractOrderSubmitTemplate.java`

```java
package io.github.atengk.pattern.combination.templatestrategy.template;

import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitRequest;
import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitResponse;
import io.github.atengk.pattern.combination.templatestrategy.factory.OrderPriceStrategyFactory;
import io.github.atengk.pattern.combination.templatestrategy.strategy.OrderPriceStrategy;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象订单提交模板
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public abstract class AbstractOrderSubmitTemplate {

    private final OrderPriceStrategyFactory orderPriceStrategyFactory;

    /**
     * 初始化订单提交模板
     *
     * @param orderPriceStrategyFactory 订单计价策略工厂
     */
    protected AbstractOrderSubmitTemplate(OrderPriceStrategyFactory orderPriceStrategyFactory) {
        this.orderPriceStrategyFactory = orderPriceStrategyFactory;
    }

    /**
     * 提交订单，固定订单提交主流程
     *
     * @param request 订单提交请求
     * @return 订单提交响应
     */
    public final OrderSubmitResponse submit(OrderSubmitRequest request) {
        beforeSubmit(request);

        validateRequest(request);

        OrderPriceStrategy strategy = selectPriceStrategy(request);

        OrderSubmitResponse priceResponse = calculatePrice(request, strategy);

        String orderNo = createOrderNo(request);

        OrderSubmitResponse response = buildSubmitResponse(orderNo, priceResponse);

        afterSubmit(request, response);

        return response;
    }

    /**
     * 提交前处理
     *
     * @param request 订单提交请求
     */
    protected void beforeSubmit(OrderSubmitRequest request) {
        log.info("开始提交订单，用户ID：{}，商品ID：{}，订单类型：{}",
                request.getUserId(), request.getProductId(), request.getOrderType());
    }

    /**
     * 校验订单提交请求
     *
     * @param request 订单提交请求
     */
    protected abstract void validateRequest(OrderSubmitRequest request);

    /**
     * 选择计价策略
     *
     * @param request 订单提交请求
     * @return 订单计价策略
     */
    protected OrderPriceStrategy selectPriceStrategy(OrderSubmitRequest request) {
        return orderPriceStrategyFactory.getStrategy(request.getOrderType());
    }

    /**
     * 计算订单价格
     *
     * @param request  订单提交请求
     * @param strategy 订单计价策略
     * @return 订单计价结果
     */
    protected OrderSubmitResponse calculatePrice(OrderSubmitRequest request, OrderPriceStrategy strategy) {
        return strategy.calculate(request);
    }

    /**
     * 创建订单编号
     *
     * @param request 订单提交请求
     * @return 订单编号
     */
    protected abstract String createOrderNo(OrderSubmitRequest request);

    /**
     * 构建订单提交响应
     *
     * @param orderNo       订单编号
     * @param priceResponse 计价响应
     * @return 订单提交响应
     */
    protected OrderSubmitResponse buildSubmitResponse(String orderNo, OrderSubmitResponse priceResponse) {
        return OrderSubmitResponse.builder()
                .orderNo(orderNo)
                .userId(priceResponse.getUserId())
                .productId(priceResponse.getProductId())
                .orderType(priceResponse.getOrderType())
                .strategyName(priceResponse.getStrategyName())
                .originAmount(priceResponse.getOriginAmount())
                .discountAmount(priceResponse.getDiscountAmount())
                .payableAmount(priceResponse.getPayableAmount())
                .orderStatus("CREATED")
                .build();
    }

    /**
     * 提交后处理
     *
     * @param request  订单提交请求
     * @param response 订单提交响应
     */
    protected void afterSubmit(OrderSubmitRequest request, OrderSubmitResponse response) {
        log.info("订单提交完成，订单编号：{}，用户ID：{}，应付金额：{}",
                response.getOrderNo(), request.getUserId(), response.getPayableAmount());
    }
}
```

### 默认订单提交模板

默认订单提交模板实现具体的参数校验和订单编号生成逻辑。主流程仍然由父类控制。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatestrategy/template/DefaultOrderSubmitTemplate.java`

```java
package io.github.atengk.pattern.combination.templatestrategy.template;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitRequest;
import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitResponse;
import io.github.atengk.pattern.combination.templatestrategy.factory.OrderPriceStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 默认订单提交模板
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class DefaultOrderSubmitTemplate extends AbstractOrderSubmitTemplate {

    /**
     * 初始化默认订单提交模板
     *
     * @param orderPriceStrategyFactory 订单计价策略工厂
     */
    public DefaultOrderSubmitTemplate(OrderPriceStrategyFactory orderPriceStrategyFactory) {
        super(orderPriceStrategyFactory);
    }

    /**
     * 校验订单提交请求
     *
     * @param request 订单提交请求
     */
    @Override
    protected void validateRequest(OrderSubmitRequest request) {
        if (ObjectUtil.isNull(request.getUserId())) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (ObjectUtil.isNull(request.getProductId())) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        if (StrUtil.isBlank(request.getOrderType())) {
            throw new IllegalArgumentException("订单类型不能为空");
        }
        if (ObjectUtil.isNull(request.getOriginAmount()) || request.getOriginAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("商品原始金额必须大于0");
        }

        log.info("订单提交参数校验通过，用户ID：{}，商品ID：{}",
                request.getUserId(), request.getProductId());
    }

    /**
     * 创建订单编号
     *
     * @param request 订单提交请求
     * @return 订单编号
     */
    @Override
    protected String createOrderNo(OrderSubmitRequest request) {
        String orderNo = "OD" + IdUtil.getSnowflakeNextIdStr();
        log.info("订单编号生成完成，订单编号：{}", orderNo);
        return orderNo;
    }

    /**
     * 提交后处理
     *
     * @param request  订单提交请求
     * @param response 订单提交响应
     */
    @Override
    protected void afterSubmit(OrderSubmitRequest request, OrderSubmitResponse response) {
        super.afterSubmit(request, response);
        log.info("执行订单提交后置处理，订单编号：{}，订单状态：{}",
                response.getOrderNo(), response.getOrderStatus());
    }
}
```

### Service 接口

Service 接口对外暴露订单提交能力。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatestrategy/service/OrderSubmitService.java`

```java
package io.github.atengk.pattern.combination.templatestrategy.service;

import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitRequest;
import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitResponse;

/**
 * 订单提交服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderSubmitService {

    /**
     * 提交订单
     *
     * @param request 订单提交请求
     * @return 订单提交响应
     */
    OrderSubmitResponse submit(OrderSubmitRequest request);
}
```

### Service 实现类

Service 实现类只负责调用订单提交模板，不再关心主流程内部每个步骤的具体执行细节。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatestrategy/service/impl/OrderSubmitServiceImpl.java`

```java
package io.github.atengk.pattern.combination.templatestrategy.service.impl;

import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitRequest;
import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitResponse;
import io.github.atengk.pattern.combination.templatestrategy.service.OrderSubmitService;
import io.github.atengk.pattern.combination.templatestrategy.template.DefaultOrderSubmitTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单提交服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSubmitServiceImpl implements OrderSubmitService {

    private final DefaultOrderSubmitTemplate orderSubmitTemplate;

    /**
     * 提交订单
     *
     * @param request 订单提交请求
     * @return 订单提交响应
     */
    @Override
    public OrderSubmitResponse submit(OrderSubmitRequest request) {
        OrderSubmitResponse response = orderSubmitTemplate.submit(request);
        log.info("订单提交服务处理完成，订单编号：{}", response.getOrderNo());
        return response;
    }
}
```

### Controller

Controller 提供订单提交接口。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatestrategy/controller/OrderSubmitController.java`

```java
package io.github.atengk.pattern.combination.templatestrategy.controller;

import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitRequest;
import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitResponse;
import io.github.atengk.pattern.combination.templatestrategy.result.Result;
import io.github.atengk.pattern.combination.templatestrategy.service.OrderSubmitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单提交控制器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order-submit")
public class OrderSubmitController {

    private final OrderSubmitService orderSubmitService;

    /**
     * 提交订单
     *
     * @param request 订单提交请求
     * @return 订单提交响应
     */
    @PostMapping
    public Result<OrderSubmitResponse> submit(@Valid @RequestBody OrderSubmitRequest request) {
        return Result.success(orderSubmitService.submit(request));
    }
}
```

如果没有复用前面章节的 `Result`，可以在当前模块增加统一响应类。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatestrategy/result/Result.java`

```java
package io.github.atengk.pattern.combination.templatestrategy.result;

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

## 使用方式

启动 Spring Boot 项目后，可以通过订单提交接口验证模板方法和策略模式是否同时生效。

接口信息如下：

```text
请求地址：POST /api/order-submit
Content-Type：application/json
```

普通订单请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-submit" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "productId": 2001,
    "orderType": "NORMAL",
    "originAmount": 100.00
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "OD1988267712300011520",
    "userId": 1001,
    "productId": 2001,
    "orderType": "NORMAL",
    "strategyName": "普通订单计价策略",
    "originAmount": 100.00,
    "discountAmount": 0,
    "payableAmount": 100.00,
    "orderStatus": "CREATED"
  }
}
```

会员订单请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-submit" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1002,
    "productId": 2002,
    "orderType": "VIP",
    "originAmount": 100.00
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "OD1988267712300011521",
    "userId": 1002,
    "productId": 2002,
    "orderType": "VIP",
    "strategyName": "会员订单计价策略",
    "originAmount": 100.00,
    "discountAmount": 10.00,
    "payableAmount": 90.00,
    "orderStatus": "CREATED"
  }
}
```

活动订单请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-submit" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1003,
    "productId": 2003,
    "orderType": "PROMOTION",
    "originAmount": 100.00
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "OD1988267712300011522",
    "userId": 1003,
    "productId": 2003,
    "orderType": "PROMOTION",
    "strategyName": "活动订单计价策略",
    "originAmount": 100.00,
    "discountAmount": 20.00,
    "payableAmount": 80.00,
    "orderStatus": "CREATED"
  }
}
```

## 新增策略方式

当新增一种订单计价规则时，例如新增“新人订单”，只需要新增订单类型和策略实现类，不需要修改订单提交模板主流程。

第一步，在 `OrderTypeEnum` 中新增枚举：

```java
NEW_USER("NEW_USER", "新人订单");
```

第二步，新增新人订单计价策略。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatestrategy/strategy/impl/NewUserOrderPriceStrategy.java`

```java
package io.github.atengk.pattern.combination.templatestrategy.strategy.impl;

import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitRequest;
import io.github.atengk.pattern.combination.templatestrategy.dto.OrderSubmitResponse;
import io.github.atengk.pattern.combination.templatestrategy.enums.OrderTypeEnum;
import io.github.atengk.pattern.combination.templatestrategy.strategy.OrderPriceStrategy;
import io.github.atengk.pattern.combination.templatestrategy.util.MoneyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 新人订单计价策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class NewUserOrderPriceStrategy implements OrderPriceStrategy {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

    /**
     * 当前策略支持的订单类型
     *
     * @return 订单类型
     */
    @Override
    public OrderTypeEnum orderType() {
        return OrderTypeEnum.NEW_USER;
    }

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    @Override
    public String strategyName() {
        return "新人订单计价策略";
    }

    /**
     * 计算新人订单价格
     *
     * @param request 订单提交请求
     * @return 订单提交响应
     */
    @Override
    public OrderSubmitResponse calculate(OrderSubmitRequest request) {
        BigDecimal originAmount = MoneyUtils.round(request.getOriginAmount());
        BigDecimal discountAmount = MoneyUtils.multiply(originAmount, DISCOUNT_RATE);
        BigDecimal payableAmount = MoneyUtils.subtract(originAmount, discountAmount);

        log.info("新人订单计价完成，用户ID：{}，商品ID：{}，优惠金额：{}，应付金额：{}",
                request.getUserId(), request.getProductId(), discountAmount, payableAmount);

        return OrderSubmitResponse.builder()
                .userId(request.getUserId())
                .productId(request.getProductId())
                .orderType(orderType().getCode())
                .strategyName(strategyName())
                .originAmount(originAmount)
                .discountAmount(discountAmount)
                .payableAmount(payableAmount)
                .build();
    }
}
```

新增完成后，`OrderPriceStrategyFactory` 会自动收集新的策略 Bean。模板方法中的订单提交主流程不需要修改。

## 验证方式

可以从以下几个方面验证组合模式是否生效：

```text
1. 启动项目时，日志应输出订单计价策略工厂初始化完成
2. 请求 NORMAL 类型时，应命中 NormalOrderPriceStrategy
3. 请求 VIP 类型时，应命中 VipOrderPriceStrategy
4. 请求 PROMOTION 类型时，应命中 PromotionOrderPriceStrategy
5. 每次请求都应该执行同一套订单提交主流程
6. 新增 NEW_USER 策略后，不需要修改 AbstractOrderSubmitTemplate
7. 请求不支持的订单类型时，应返回业务异常
```

正常日志示例：

```text
开始提交订单，用户ID：1002，商品ID：2002，订单类型：VIP
订单提交参数校验通过，用户ID：1002，商品ID：2002
命中订单计价策略，订单类型：VIP，策略类：VipOrderPriceStrategy
会员订单计价完成，用户ID：1002，商品ID：2002，优惠金额：10.00，应付金额：90.00
订单编号生成完成，订单编号：OD1988267712300011521
订单提交完成，订单编号：OD1988267712300011521，用户ID：1002，应付金额：90.00
执行订单提交后置处理，订单编号：OD1988267712300011521，订单状态：CREATED
订单提交服务处理完成，订单编号：OD1988267712300011521
```

## 组合效果

模板方法模式和策略模式组合后，各自负责不同变化点：

| 模式     | 职责       | 在示例中的体现                                                                                   |
| ------ | -------- | ----------------------------------------------------------------------------------------- |
| 模板方法模式 | 固定业务流程骨架 | `AbstractOrderSubmitTemplate` 固定订单提交步骤                                                    |
| 策略模式   | 替换局部业务算法 | `NormalOrderPriceStrategy`、`VipOrderPriceStrategy`、`PromotionOrderPriceStrategy` 处理不同计价规则 |

这种组合适合处理下面两类变化：

```text
第一类变化：主流程基本稳定
例如订单提交都需要参数校验、计价、生成订单编号、构建响应、后置处理

第二类变化：流程中的局部步骤经常变化
例如不同订单类型的计价规则不同
```

相比只使用策略模式，这个组合能更好地约束主流程，避免每个策略自己控制完整业务流程。相比只使用模板方法模式，这个组合又能避免把所有差异逻辑都写进子类重写方法中。

## 注意事项

模板方法模式和策略模式组合使用时，模板方法应该只固定稳定流程，不要把所有业务细节都写死在抽象类中。否则抽象模板会越来越重，后续扩展会变得困难。

策略模式应该只处理流程中的可变算法，不应该反过来控制主流程。例如本示例中，计价策略只负责计算价格，不负责生成订单编号、修改订单状态或执行后置动作。

如果业务流程整体经常变化，可以考虑多套模板实现；如果只是某个步骤经常变化，可以优先使用策略模式替换该步骤。模板方法和策略模式的组合重点是：**流程稳定归模板，算法变化归策略**。
