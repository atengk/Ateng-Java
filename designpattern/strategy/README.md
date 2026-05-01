# 设计模式：策略模式

策略模式用于把一组可替换的算法、规则或业务处理逻辑封装成独立策略类，让调用方通过统一接口调用不同实现。在 JDK21 和 Spring Boot 3 项目中，策略模式常用于优惠计算、支付分发、物流计费、文件解析、导入校验、风控规则、消息发送、订单状态处理等场景。

需要注意：策略模式解决的是“不同算法或业务规则的可替换问题”。在 Spring Boot 项目中，策略模式通常会结合 Spring 容器使用，通过接口多实现、`List<T>` 注入、`Map<String, T>` 缓存和工厂分发完成策略选择。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证策略模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、集合、金额计算、ID 等通用处理 -->
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

策略模式的核心目标是把变化的算法独立出来，使调用方只依赖策略接口，不直接依赖具体策略实现。

常见实现方式如下：

| 实现方式        | 是否推荐         | 适用场景                         |
| --------------- | ---------------- | -------------------------------- |
| 普通 Java 策略  | 推荐用于简单场景 | 无 Spring 依赖的算法封装         |
| Spring 策略分发 | 强烈推荐         | Spring Boot 项目中的业务策略选择 |
| 枚举策略        | 适合轻量规则     | 简单、固定、无依赖的规则         |
| 策略 + 工厂     | 推荐             | 根据业务类型选择不同策略         |
| 大量 `if else`  | 不推荐           | 策略增多后维护成本高             |

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring 策略分发 > 普通 Java 策略 > 枚举策略 > 大量 if else
```

策略模式和工厂模式经常一起使用。工厂模式负责“找到哪个策略”，策略模式负责“具体怎么执行”。

## 普通 Java 策略

普通 Java 策略适合不依赖 Spring 容器的算法场景。下面以运费计算为例，不同物流方式对应不同计费策略。

### 文件结构

```text
src/main/java/io/github/atengk/design/strategy/simple/
├── DeliveryType.java
├── DeliveryFeeStrategy.java
├── ExpressDeliveryFeeStrategy.java
├── ColdChainDeliveryFeeStrategy.java
├── DeliveryFeeContext.java
└── DeliveryFeeCalculator.java
```

文件位置：`src/main/java/io/github/atengk/design/strategy/simple/DeliveryType.java`

下面的枚举用于定义配送类型。

```java
package io.github.atengk.design.strategy.simple;

/**
 * 配送类型
 *
 * @author Ateng
 * @since 2026-04-30
 */
public enum DeliveryType {

    /**
     * 普通快递
     */
    EXPRESS,

    /**
     * 冷链配送
     */
    COLD_CHAIN
}
```

文件位置：`src/main/java/io/github/atengk/design/strategy/simple/DeliveryFeeContext.java`

下面是运费计算上下文，用于传递策略执行需要的参数。

```java
package io.github.atengk.design.strategy.simple;

import java.math.BigDecimal;

/**
 * 运费计算上下文
 *
 * @param deliveryType 配送类型
 * @param distance     配送距离，单位：公里
 * @param weight       包裹重量，单位：千克
 * @author Ateng
 * @since 2026-04-30
 */
public record DeliveryFeeContext(DeliveryType deliveryType, BigDecimal distance, BigDecimal weight) {
}
```

文件位置：`src/main/java/io/github/atengk/design/strategy/simple/DeliveryFeeStrategy.java`

下面是运费计算策略接口，不同配送方式实现该接口。

```java
package io.github.atengk.design.strategy.simple;

import java.math.BigDecimal;

/**
 * 运费计算策略
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface DeliveryFeeStrategy {

    /**
     * 获取支持的配送类型
     *
     * @return 配送类型
     */
    DeliveryType supportType();

    /**
     * 计算运费
     *
     * @param context 运费计算上下文
     * @return 运费
     */
    BigDecimal calculate(DeliveryFeeContext context);
}
```

文件位置：`src/main/java/io/github/atengk/design/strategy/simple/ExpressDeliveryFeeStrategy.java`

下面是普通快递运费策略，按基础费用、距离和重量计算。

```java
package io.github.atengk.design.strategy.simple;

import cn.hutool.core.util.NumberUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 普通快递运费策略
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class ExpressDeliveryFeeStrategy implements DeliveryFeeStrategy {

    private static final BigDecimal BASE_FEE = BigDecimal.valueOf(5);
    private static final BigDecimal DISTANCE_RATE = BigDecimal.valueOf(0.8);
    private static final BigDecimal WEIGHT_RATE = BigDecimal.valueOf(1.5);

    /**
     * 获取支持的配送类型
     *
     * @return 配送类型
     */
    @Override
    public DeliveryType supportType() {
        return DeliveryType.EXPRESS;
    }

    /**
     * 计算运费
     *
     * @param context 运费计算上下文
     * @return 运费
     */
    @Override
    public BigDecimal calculate(DeliveryFeeContext context) {
        BigDecimal distanceFee = NumberUtil.mul(context.distance(), DISTANCE_RATE);
        BigDecimal weightFee = NumberUtil.mul(context.weight(), WEIGHT_RATE);
        BigDecimal fee = NumberUtil.add(BASE_FEE, distanceFee, weightFee).setScale(2, RoundingMode.HALF_UP);

        log.info("普通快递运费计算完成，距离：{}，重量：{}，运费：{}", context.distance(), context.weight(), fee);
        return fee;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/strategy/simple/ColdChainDeliveryFeeStrategy.java`

下面是冷链配送运费策略，冷链配送通常基础费用和重量费用更高。

```java
package io.github.atengk.design.strategy.simple;

import cn.hutool.core.util.NumberUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 冷链配送运费策略
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class ColdChainDeliveryFeeStrategy implements DeliveryFeeStrategy {

    private static final BigDecimal BASE_FEE = BigDecimal.valueOf(12);
    private static final BigDecimal DISTANCE_RATE = BigDecimal.valueOf(1.2);
    private static final BigDecimal WEIGHT_RATE = BigDecimal.valueOf(3.5);

    /**
     * 获取支持的配送类型
     *
     * @return 配送类型
     */
    @Override
    public DeliveryType supportType() {
        return DeliveryType.COLD_CHAIN;
    }

    /**
     * 计算运费
     *
     * @param context 运费计算上下文
     * @return 运费
     */
    @Override
    public BigDecimal calculate(DeliveryFeeContext context) {
        BigDecimal distanceFee = NumberUtil.mul(context.distance(), DISTANCE_RATE);
        BigDecimal weightFee = NumberUtil.mul(context.weight(), WEIGHT_RATE);
        BigDecimal fee = NumberUtil.add(BASE_FEE, distanceFee, weightFee).setScale(2, RoundingMode.HALF_UP);

        log.info("冷链配送运费计算完成，距离：{}，重量：{}，运费：{}", context.distance(), context.weight(), fee);
        return fee;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/strategy/simple/DeliveryFeeCalculator.java`

下面是普通 Java 策略选择器，根据配送类型选择对应策略。

```java
package io.github.atengk.design.strategy.simple;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 运费计算器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class DeliveryFeeCalculator {

    private final Map<DeliveryType, DeliveryFeeStrategy> strategyMap = new EnumMap<>(DeliveryType.class);

    /**
     * 创建运费计算器
     *
     * @param strategies 运费计算策略列表
     */
    public DeliveryFeeCalculator(List<DeliveryFeeStrategy> strategies) {
        if (CollUtil.isEmpty(strategies)) {
            log.warn("运费计算策略列表为空");
            return;
        }

        for (DeliveryFeeStrategy strategy : strategies) {
            strategyMap.put(strategy.supportType(), strategy);
        }

        log.info("初始化运费计算器，支持配送类型：{}", strategyMap.keySet());
    }

    /**
     * 计算运费
     *
     * @param context 运费计算上下文
     * @return 运费
     */
    public BigDecimal calculate(DeliveryFeeContext context) {
        DeliveryFeeStrategy strategy = strategyMap.get(context.deliveryType());

        if (strategy == null) {
            log.warn("计算运费失败，不支持的配送类型：{}", context.deliveryType());
            throw new IllegalArgumentException("不支持的配送类型：" + context.deliveryType());
        }

        return strategy.calculate(context);
    }
}
```

使用方式：

```java
DeliveryFeeCalculator calculator = new DeliveryFeeCalculator(List.of(
        new ExpressDeliveryFeeStrategy(),
        new ColdChainDeliveryFeeStrategy()
));

BigDecimal expressFee = calculator.calculate(new DeliveryFeeContext(
        DeliveryType.EXPRESS,
        BigDecimal.valueOf(10),
        BigDecimal.valueOf(2)
));

BigDecimal coldChainFee = calculator.calculate(new DeliveryFeeContext(
        DeliveryType.COLD_CHAIN,
        BigDecimal.valueOf(10),
        BigDecimal.valueOf(2)
));
```

普通 Java 策略的优点是简单直接，不依赖 Spring。缺点是策略对象需要手动创建，不适合依赖数据库、Redis、配置中心、第三方客户端等 Spring Bean 的业务场景。

## Spring Boot 策略分发

Spring Boot 项目中最常用的策略模式，是将每个策略实现注册为 Spring Bean，再通过统一上下文或工厂类根据业务类型选择策略。

下面以订单优惠计算为例，实现三种优惠策略：

```text
none            无优惠
full_reduction  满减优惠
percentage      折扣优惠
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── StrategyApplication.java
├── controller/
│   └── OrderDiscountController.java
├── dto/
│   ├── OrderDiscountRequest.java
│   └── OrderDiscountResponse.java
├── strategy/
│   ├── OrderDiscountStrategy.java
│   ├── NoneDiscountStrategy.java
│   ├── FullReductionDiscountStrategy.java
│   └── PercentageDiscountStrategy.java
└── context/
    └── OrderDiscountContext.java
```

文件位置：`src/main/java/io/github/atengk/design/StrategyApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 策略模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class StrategyApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(StrategyApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderDiscountRequest.java`

下面是订单优惠计算请求参数。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 订单优惠计算请求
 *
 * @param strategyType 优惠策略类型
 * @param orderNo      订单号
 * @param orderAmount  订单原始金额
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderDiscountRequest(String strategyType, String orderNo, BigDecimal orderAmount) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderDiscountResponse.java`

下面是订单优惠计算响应结果。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 订单优惠计算响应
 *
 * @param orderNo        订单号
 * @param strategyType   优惠策略类型
 * @param originalAmount 原始金额
 * @param discountAmount 优惠金额
 * @param payableAmount  应付金额
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderDiscountResponse(
        String orderNo,
        String strategyType,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal payableAmount
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/strategy/OrderDiscountStrategy.java`

下面是订单优惠策略接口，所有优惠策略都实现该接口。

```java
package io.github.atengk.design.strategy;

import io.github.atengk.design.dto.OrderDiscountRequest;
import io.github.atengk.design.dto.OrderDiscountResponse;

/**
 * 订单优惠策略
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderDiscountStrategy {

    /**
     * 获取支持的优惠策略类型
     *
     * @return 优惠策略类型
     */
    String supportType();

    /**
     * 计算订单优惠
     *
     * @param request 订单优惠计算请求
     * @return 订单优惠计算响应
     */
    OrderDiscountResponse calculate(OrderDiscountRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/strategy/NoneDiscountStrategy.java`

下面是无优惠策略，订单应付金额等于原始金额。

```java
package io.github.atengk.design.strategy;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.OrderDiscountRequest;
import io.github.atengk.design.dto.OrderDiscountResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 无优惠策略
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class NoneDiscountStrategy implements OrderDiscountStrategy {

    /**
     * 获取支持的优惠策略类型
     *
     * @return 优惠策略类型
     */
    @Override
    public String supportType() {
        return "none";
    }

    /**
     * 计算订单优惠
     *
     * @param request 订单优惠计算请求
     * @return 订单优惠计算响应
     */
    @Override
    public OrderDiscountResponse calculate(OrderDiscountRequest request) {
        BigDecimal originalAmount = request.orderAmount();
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal payableAmount = originalAmount;

        log.info("执行无优惠策略，订单号：{}，原始金额：{}，应付金额：{}",
                request.orderNo(), originalAmount, payableAmount);

        return new OrderDiscountResponse(
                request.orderNo(),
                StrUtil.lowerFirst(supportType()),
                originalAmount,
                discountAmount,
                payableAmount
        );
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/strategy/FullReductionDiscountStrategy.java`

下面是满减优惠策略，示例规则为满 100 减 20。

```java
package io.github.atengk.design.strategy;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.design.dto.OrderDiscountRequest;
import io.github.atengk.design.dto.OrderDiscountResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 满减优惠策略
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class FullReductionDiscountStrategy implements OrderDiscountStrategy {

    private static final BigDecimal THRESHOLD_AMOUNT = BigDecimal.valueOf(100);
    private static final BigDecimal REDUCTION_AMOUNT = BigDecimal.valueOf(20);

    /**
     * 获取支持的优惠策略类型
     *
     * @return 优惠策略类型
     */
    @Override
    public String supportType() {
        return "full_reduction";
    }

    /**
     * 计算订单优惠
     *
     * @param request 订单优惠计算请求
     * @return 订单优惠计算响应
     */
    @Override
    public OrderDiscountResponse calculate(OrderDiscountRequest request) {
        BigDecimal originalAmount = request.orderAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (originalAmount.compareTo(THRESHOLD_AMOUNT) >= 0) {
            discountAmount = REDUCTION_AMOUNT;
        }

        BigDecimal payableAmount = NumberUtil.sub(originalAmount, discountAmount).setScale(2, RoundingMode.HALF_UP);

        log.info("执行满减优惠策略，订单号：{}，原始金额：{}，优惠金额：{}，应付金额：{}",
                request.orderNo(), originalAmount, discountAmount, payableAmount);

        return new OrderDiscountResponse(
                request.orderNo(),
                supportType(),
                originalAmount,
                discountAmount,
                payableAmount
        );
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/strategy/PercentageDiscountStrategy.java`

下面是折扣优惠策略，示例规则为 9 折。

```java
package io.github.atengk.design.strategy;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.design.dto.OrderDiscountRequest;
import io.github.atengk.design.dto.OrderDiscountResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 折扣优惠策略
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class PercentageDiscountStrategy implements OrderDiscountStrategy {

    private static final BigDecimal DISCOUNT_RATE = BigDecimal.valueOf(0.9);

    /**
     * 获取支持的优惠策略类型
     *
     * @return 优惠策略类型
     */
    @Override
    public String supportType() {
        return "percentage";
    }

    /**
     * 计算订单优惠
     *
     * @param request 订单优惠计算请求
     * @return 订单优惠计算响应
     */
    @Override
    public OrderDiscountResponse calculate(OrderDiscountRequest request) {
        BigDecimal originalAmount = request.orderAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal payableAmount = NumberUtil.mul(originalAmount, DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = NumberUtil.sub(originalAmount, payableAmount).setScale(2, RoundingMode.HALF_UP);

        log.info("执行折扣优惠策略，订单号：{}，原始金额：{}，优惠金额：{}，应付金额：{}",
                request.orderNo(), originalAmount, discountAmount, payableAmount);

        return new OrderDiscountResponse(
                request.orderNo(),
                supportType(),
                originalAmount,
                discountAmount,
                payableAmount
        );
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/context/OrderDiscountContext.java`

下面是订单优惠策略上下文。它接收所有策略实现，并根据策略类型选择对应策略执行。

```java
package io.github.atengk.design.context;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.OrderDiscountRequest;
import io.github.atengk.design.dto.OrderDiscountResponse;
import io.github.atengk.design.strategy.OrderDiscountStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单优惠策略上下文
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderDiscountContext {

    private final Map<String, OrderDiscountStrategy> strategyMap;

    /**
     * 创建订单优惠策略上下文
     *
     * @param strategies 订单优惠策略列表
     */
    public OrderDiscountContext(List<OrderDiscountStrategy> strategies) {
        if (CollUtil.isEmpty(strategies)) {
            log.warn("订单优惠策略列表为空");
            this.strategyMap = Map.of();
            return;
        }

        this.strategyMap = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        strategy -> StrUtil.trim(strategy.supportType()).toLowerCase(),
                        Function.identity()
                ));

        log.info("初始化订单优惠策略上下文，支持策略类型：{}", strategyMap.keySet());
    }

    /**
     * 计算订单优惠
     *
     * @param request 订单优惠计算请求
     * @return 订单优惠计算响应
     */
    public OrderDiscountResponse calculate(OrderDiscountRequest request) {
        validateRequest(request);

        String strategyType = StrUtil.trim(request.strategyType()).toLowerCase();
        OrderDiscountStrategy strategy = strategyMap.get(strategyType);

        if (strategy == null) {
            log.warn("计算订单优惠失败，不支持的策略类型：{}", request.strategyType());
            throw new IllegalArgumentException("不支持的策略类型：" + request.strategyType());
        }

        log.debug("匹配订单优惠策略成功，订单号：{}，策略类型：{}", request.orderNo(), strategyType);
        return strategy.calculate(request);
    }

    /**
     * 校验订单优惠请求
     *
     * @param request 订单优惠计算请求
     */
    private void validateRequest(OrderDiscountRequest request) {
        if (request == null) {
            log.warn("计算订单优惠失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.isBlank(request.strategyType())) {
            log.warn("计算订单优惠失败，策略类型为空");
            throw new IllegalArgumentException("策略类型不能为空");
        }

        if (StrUtil.isBlank(request.orderNo())) {
            log.warn("计算订单优惠失败，订单号为空");
            throw new IllegalArgumentException("订单号不能为空");
        }

        if (request.orderAmount() == null || request.orderAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("计算订单优惠失败，订单金额不合法，订单金额：{}", request.orderAmount());
            throw new IllegalArgumentException("订单金额必须大于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/OrderDiscountController.java`

下面是订单优惠计算接口，用于验证策略模式效果。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.context.OrderDiscountContext;
import io.github.atengk.design.dto.OrderDiscountRequest;
import io.github.atengk.design.dto.OrderDiscountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 订单优惠控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/strategy/order-discount")
public class OrderDiscountController {

    private final OrderDiscountContext orderDiscountContext;

    /**
     * 计算订单优惠
     *
     * @param strategyType 策略类型
     * @param orderNo      订单号
     * @param orderAmount  订单金额
     * @return 订单优惠计算响应
     */
    @PostMapping("/calculate")
    public OrderDiscountResponse calculate(@RequestParam String strategyType,
                                           @RequestParam String orderNo,
                                           @RequestParam BigDecimal orderAmount) {
        OrderDiscountRequest request = new OrderDiscountRequest(strategyType, orderNo, orderAmount);
        return orderDiscountContext.calculate(request);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/strategy/order-discount/calculate?strategyType=none&orderNo=ORDER10001&orderAmount=120.00"

curl -X POST "http://localhost:8080/strategy/order-discount/calculate?strategyType=full_reduction&orderNo=ORDER10002&orderAmount=120.00"

curl -X POST "http://localhost:8080/strategy/order-discount/calculate?strategyType=percentage&orderNo=ORDER10003&orderAmount=120.00"
```

可能返回：

```json
{
  "orderNo": "ORDER10002",
  "strategyType": "full_reduction",
  "originalAmount": 120.00,
  "discountAmount": 20,
  "payableAmount": 100.00
}
```

这种方式的优点是扩展成本低。后续新增优惠策略，只需要新增一个实现类并注册为 Spring Bean，不需要修改控制器，也不需要修改已有策略。

## 扩展一个新策略

在 Spring 策略分发模式中，新增策略通常只需要新增一个实现类。下面以新人立减策略为例，规则为立减 30 元，但应付金额最低不能小于 0.01 元。

文件位置：`src/main/java/io/github/atengk/design/strategy/NewUserDiscountStrategy.java`

下面的实现类会被 Spring 自动扫描，并自动加入 `OrderDiscountContext` 的策略列表。

```java
package io.github.atengk.design.strategy;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.design.dto.OrderDiscountRequest;
import io.github.atengk.design.dto.OrderDiscountResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 新人优惠策略
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class NewUserDiscountStrategy implements OrderDiscountStrategy {

    private static final BigDecimal DISCOUNT_AMOUNT = BigDecimal.valueOf(30);
    private static final BigDecimal MIN_PAYABLE_AMOUNT = BigDecimal.valueOf(0.01);

    /**
     * 获取支持的优惠策略类型
     *
     * @return 优惠策略类型
     */
    @Override
    public String supportType() {
        return "new_user";
    }

    /**
     * 计算订单优惠
     *
     * @param request 订单优惠计算请求
     * @return 订单优惠计算响应
     */
    @Override
    public OrderDiscountResponse calculate(OrderDiscountRequest request) {
        BigDecimal originalAmount = request.orderAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal payableAmount = NumberUtil.sub(originalAmount, DISCOUNT_AMOUNT).setScale(2, RoundingMode.HALF_UP);

        if (payableAmount.compareTo(MIN_PAYABLE_AMOUNT) < 0) {
            payableAmount = MIN_PAYABLE_AMOUNT;
        }

        BigDecimal discountAmount = NumberUtil.sub(originalAmount, payableAmount).setScale(2, RoundingMode.HALF_UP);

        log.info("执行新人优惠策略，订单号：{}，原始金额：{}，优惠金额：{}，应付金额：{}",
                request.orderNo(), originalAmount, discountAmount, payableAmount);

        return new OrderDiscountResponse(
                request.orderNo(),
                supportType(),
                originalAmount,
                discountAmount,
                payableAmount
        );
    }
}
```

调用示例：

```bash
curl -X POST "http://localhost:8080/strategy/order-discount/calculate?strategyType=new_user&orderNo=ORDER10004&orderAmount=88.00"
```

可能返回：

```json
{
  "orderNo": "ORDER10004",
  "strategyType": "new_user",
  "originalAmount": 88.00,
  "discountAmount": 30.00,
  "payableAmount": 58.00
}
```

新增策略时，原有的 `OrderDiscountController`、`OrderDiscountContext`、其他策略类都不需要修改。这就是策略模式对开闭原则的体现。

## 策略模式和工厂模式的关系

策略模式和工厂模式经常组合使用，但二者关注点不同。

策略模式关注“行为如何变化”。例如满减、折扣、新人优惠都属于不同的优惠算法。

工厂模式关注“对象如何创建或获取”。例如根据 `strategyType` 获取哪个 `OrderDiscountStrategy` 实现。

在上面的 Spring Boot 示例中：

```text
OrderDiscountStrategy      策略接口
NoneDiscountStrategy       具体策略
FullReductionDiscountStrategy 具体策略
PercentageDiscountStrategy 具体策略
OrderDiscountContext       策略上下文，也承担策略查找职责
```

如果项目规模较大，可以把策略查找职责单独拆成工厂类：

```text
OrderDiscountStrategyFactory 负责获取策略
OrderDiscountContext         负责组织执行流程
```

简单项目中，直接让 `OrderDiscountContext` 持有策略 Map 即可，结构更轻。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行无优惠策略：

```bash
curl -X POST "http://localhost:8080/strategy/order-discount/calculate?strategyType=none&orderNo=ORDER10001&orderAmount=120.00"
```

执行满减策略：

```bash
curl -X POST "http://localhost:8080/strategy/order-discount/calculate?strategyType=full_reduction&orderNo=ORDER10002&orderAmount=120.00"
```

执行折扣策略：

```bash
curl -X POST "http://localhost:8080/strategy/order-discount/calculate?strategyType=percentage&orderNo=ORDER10003&orderAmount=120.00"
```

执行新人优惠策略：

```bash
curl -X POST "http://localhost:8080/strategy/order-discount/calculate?strategyType=new_user&orderNo=ORDER10004&orderAmount=88.00"
```

如果策略分发正常，可以看到类似日志：

```text
初始化订单优惠策略上下文，支持策略类型：[none, full_reduction, percentage, new_user]
执行无优惠策略，订单号：ORDER10001，原始金额：120.00，应付金额：120.00
执行满减优惠策略，订单号：ORDER10002，原始金额：120.00，优惠金额：20，应付金额：100.00
执行折扣优惠策略，订单号：ORDER10003，原始金额：120.00，优惠金额：12.00，应付金额：108.00
执行新人优惠策略，订单号：ORDER10004，原始金额：88.00，优惠金额：30.00，应付金额：58.00
```

如果传入不支持的策略类型：

```bash
curl -X POST "http://localhost:8080/strategy/order-discount/calculate?strategyType=vip&orderNo=ORDER10005&orderAmount=120.00"
```

会抛出异常：

```text
不支持的策略类型：vip
```

实际项目中建议结合全局异常处理器，将该异常转换成统一响应结构。

## 注意事项

策略模式适合解决同一业务点下多种算法、多种规则、多种处理方式的问题。如果只是两三个非常简单且短期不会扩展的分支，强行使用策略模式可能会增加类数量和维护成本。

不推荐在业务代码中这样写：

```java
if ("none".equals(strategyType)) {
    return noneDiscount(request);
}
if ("full_reduction".equals(strategyType)) {
    return fullReductionDiscount(request);
}
if ("percentage".equals(strategyType)) {
    return percentageDiscount(request);
}
```

推荐使用策略分发：

```java
OrderDiscountResponse response = orderDiscountContext.calculate(request);
```

策略 Bean 默认是 Spring 单例，不要在策略类成员变量中保存请求级数据。

错误示例：

```java
private String currentOrderNo;
private BigDecimal currentOrderAmount;
private BigDecimal currentDiscountAmount;
```

这些字段在并发请求下会互相污染，导致线程安全问题。

推荐将请求数据放在方法参数、局部变量、DTO 或上下文对象中。

```java
public OrderDiscountResponse calculate(OrderDiscountRequest request) {
    BigDecimal originalAmount = request.orderAmount();
    BigDecimal payableAmount = originalAmount;
    return new OrderDiscountResponse(request.orderNo(), supportType(), originalAmount, BigDecimal.ZERO, payableAmount);
}
```

如果策略需要读取数据库、Redis、配置中心或远程服务，可以直接在具体策略类中注入对应的 Spring Bean。

示例：

```java
@RequiredArgsConstructor
@Component
public class MemberDiscountStrategy implements OrderDiscountStrategy {

    private final MemberService memberService;

    @Override
    public String supportType() {
        return "member";
    }

    @Override
    public OrderDiscountResponse calculate(OrderDiscountRequest request) {
        // 根据会员等级计算优惠
        return null;
    }
}
```

实际项目中不要返回 `null`，这里仅用于说明策略类可以注入其他业务 Bean。完整实现时应返回明确的业务响应对象。

## 总结

在 JDK21 和 Spring Boot 3 项目中，策略模式的实践重点是把变化的业务规则从主流程中拆出来，让主流程稳定，让策略实现可扩展。

普通 Java 策略适合无依赖的算法封装。Spring Boot 策略分发适合业务系统中的多实现选择。对于订单优惠、支付渠道、物流计费、文件解析、风控规则等场景，推荐使用“策略接口 + 多个策略实现 + 策略上下文”的结构。

策略模式不是为了消灭所有分支判断，而是为了让频繁变化的分支逻辑具备更低的扩展成本和更清晰的维护边界。
