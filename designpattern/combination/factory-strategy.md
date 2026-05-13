# 简单工厂模式 + 策略模式

简单工厂模式和策略模式经常一起出现在 Spring Boot 后端项目中，尤其适合处理“根据业务类型选择不同处理逻辑”的场景。简单工厂负责根据类型找到对应的策略对象，策略模式负责把具体业务算法封装到独立实现类中。

在真实项目中，这种组合常用于支付渠道处理、订单计价、优惠计算、消息发送、文件导入、导出任务、审批处理、通知渠道分发等场景。

```text
简单工厂模式：负责根据类型获取对象
策略模式：负责封装不同业务处理逻辑
```

它们组合后的核心价值是：业务入口不再写大量 `if else` 或 `switch`，新增业务类型时只需要新增一个策略实现类，并在工厂中完成注册或由 Spring 自动收集。

## 适用场景

本示例以“订单计价”为业务场景。不同订单类型有不同的计价规则：

| 订单类型   | 说明     | 计价规则                   |
| ---------- | -------- | -------------------------- |
| NORMAL     | 普通订单 | 原价支付                   |
| VIP        | 会员订单 | 享受 9 折                  |
| FLASH_SALE | 秒杀订单 | 享受 7 折，但最低支付 1 元 |

如果使用传统写法，通常会在 Service 中写大量分支：

```java
if ("NORMAL".equals(orderType)) {
    // 普通订单计价
} else if ("VIP".equals(orderType)) {
    // 会员订单计价
} else if ("FLASH_SALE".equals(orderType)) {
    // 秒杀订单计价
}
```

这种写法的问题是：订单类型越多，Service 分支越复杂；每次新增类型都要修改原有代码；不同计价逻辑混在一个类中，可读性和可测试性都会下降。

使用“简单工厂模式 + 策略模式”后，结构变成：

```text
Controller
  -> Service
      -> 简单工厂：根据订单类型获取策略
          -> 策略接口
              -> 普通订单计价策略
              -> 会员订单计价策略
              -> 秒杀订单计价策略
```

## 基础配置

这里使用 JDK 21、Spring Boot 3、Maven、Lombok、Hutool 和 Spring Validation。Hutool 用于字符串、对象和金额计算等常见工具处理。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Validation：用于请求参数校验，Spring Boot 3 使用 jakarta.validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：常用工具类，简化字符串、对象、数字等处理 -->
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

建议将本示例代码放在如下目录中：

```text
src/main/java/io/github/atengk/pattern/combination/factorystrategy
├── controller
│   └── OrderPriceController.java
├── dto
│   ├── OrderPriceRequest.java
│   └── OrderPriceResponse.java
├── enums
│   └── OrderTypeEnum.java
├── factory
│   └── OrderPriceStrategyFactory.java
├── handler
│   └── GlobalExceptionHandler.java
├── result
│   └── Result.java
├── service
│   ├── OrderPriceService.java
│   └── impl
│       └── OrderPriceServiceImpl.java
├── strategy
│   ├── OrderPriceStrategy.java
│   └── impl
│       ├── FlashSaleOrderPriceStrategy.java
│       ├── NormalOrderPriceStrategy.java
│       └── VipOrderPriceStrategy.java
└── util
    └── MoneyUtils.java
```

## 核心代码

这一部分给出完整核心代码。示例中简单工厂不是直接 `new` 策略对象，而是通过 Spring 自动注入所有策略实现类，再组装成一个策略映射表。这是 Spring Boot 项目中更推荐的落地方式。

### 订单类型枚举

订单类型枚举用于统一维护业务类型编码，避免在代码中散落字符串常量。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorystrategy/enums/OrderTypeEnum.java`

```java
package io.github.atengk.pattern.combination.factorystrategy.enums;

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

    FLASH_SALE("FLASH_SALE", "秒杀订单");

    private final String code;

    private final String description;

    /**
     * 根据订单类型编码获取枚举
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

### 请求对象

请求对象用于接收订单计价参数，这里只保留计价示例需要的核心字段。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorystrategy/dto/OrderPriceRequest.java`

```java
package io.github.atengk.pattern.combination.factorystrategy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单计价请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderPriceRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 订单类型：NORMAL、VIP、FLASH_SALE
     */
    @NotBlank(message = "订单类型不能为空")
    private String orderType;

    /**
     * 订单原始金额
     */
    @NotNull(message = "订单原始金额不能为空")
    @DecimalMin(value = "0.01", message = "订单原始金额必须大于0")
    private BigDecimal originAmount;
}
```

### 响应对象

响应对象用于返回计价结果，包括原始金额、优惠金额、应付金额和命中的策略名称。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorystrategy/dto/OrderPriceResponse.java`

```java
package io.github.atengk.pattern.combination.factorystrategy.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单计价响应
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderPriceResponse {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单类型
     */
    private String orderType;

    /**
     * 策略名称
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
}
```

### 金额工具类

金额工具类用于统一处理金额乘法、减法和保留两位小数，避免不同策略中重复金额处理逻辑。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorystrategy/util/MoneyUtils.java`

```java
package io.github.atengk.pattern.combination.factorystrategy.util;

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

### 策略接口

策略接口定义所有订单计价策略必须实现的行为。每个策略都需要声明自己支持的订单类型，并实现自己的计价逻辑。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorystrategy/strategy/OrderPriceStrategy.java`

```java
package io.github.atengk.pattern.combination.factorystrategy.strategy;

import io.github.atengk.pattern.combination.factorystrategy.dto.OrderPriceRequest;
import io.github.atengk.pattern.combination.factorystrategy.dto.OrderPriceResponse;
import io.github.atengk.pattern.combination.factorystrategy.enums.OrderTypeEnum;

/**
 * 订单计价策略接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderPriceStrategy {

    /**
     * 获取当前策略支持的订单类型
     *
     * @return 订单类型
     */
    OrderTypeEnum orderType();

    /**
     * 计算订单价格
     *
     * @param request 订单计价请求
     * @return 订单计价响应
     */
    OrderPriceResponse calculate(OrderPriceRequest request);
}
```

### 普通订单计价策略

普通订单不参与优惠，直接按照原始金额支付。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorystrategy/strategy/impl/NormalOrderPriceStrategy.java`

```java
package io.github.atengk.pattern.combination.factorystrategy.strategy.impl;

import io.github.atengk.pattern.combination.factorystrategy.dto.OrderPriceRequest;
import io.github.atengk.pattern.combination.factorystrategy.dto.OrderPriceResponse;
import io.github.atengk.pattern.combination.factorystrategy.enums.OrderTypeEnum;
import io.github.atengk.pattern.combination.factorystrategy.strategy.OrderPriceStrategy;
import io.github.atengk.pattern.combination.factorystrategy.util.MoneyUtils;
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
     * 获取当前策略支持的订单类型
     *
     * @return 订单类型
     */
    @Override
    public OrderTypeEnum orderType() {
        return OrderTypeEnum.NORMAL;
    }

    /**
     * 计算普通订单价格
     *
     * @param request 订单计价请求
     * @return 订单计价响应
     */
    @Override
    public OrderPriceResponse calculate(OrderPriceRequest request) {
        BigDecimal originAmount = MoneyUtils.round(request.getOriginAmount());
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal payableAmount = originAmount;

        log.info("普通订单计价完成，用户ID：{}，原始金额：{}，应付金额：{}",
                request.getUserId(), originAmount, payableAmount);

        return OrderPriceResponse.builder()
                .userId(request.getUserId())
                .orderType(orderType().getCode())
                .strategyName("普通订单计价策略")
                .originAmount(originAmount)
                .discountAmount(discountAmount)
                .payableAmount(payableAmount)
                .build();
    }
}
```

### 会员订单计价策略

会员订单享受 9 折优惠，优惠金额为原始金额的 10%。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorystrategy/strategy/impl/VipOrderPriceStrategy.java`

```java
package io.github.atengk.pattern.combination.factorystrategy.strategy.impl;

import io.github.atengk.pattern.combination.factorystrategy.dto.OrderPriceRequest;
import io.github.atengk.pattern.combination.factorystrategy.dto.OrderPriceResponse;
import io.github.atengk.pattern.combination.factorystrategy.enums.OrderTypeEnum;
import io.github.atengk.pattern.combination.factorystrategy.strategy.OrderPriceStrategy;
import io.github.atengk.pattern.combination.factorystrategy.util.MoneyUtils;
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
     * 获取当前策略支持的订单类型
     *
     * @return 订单类型
     */
    @Override
    public OrderTypeEnum orderType() {
        return OrderTypeEnum.VIP;
    }

    /**
     * 计算会员订单价格
     *
     * @param request 订单计价请求
     * @return 订单计价响应
     */
    @Override
    public OrderPriceResponse calculate(OrderPriceRequest request) {
        BigDecimal originAmount = MoneyUtils.round(request.getOriginAmount());
        BigDecimal discountAmount = MoneyUtils.multiply(originAmount, DISCOUNT_RATE);
        BigDecimal payableAmount = MoneyUtils.subtract(originAmount, discountAmount);

        log.info("会员订单计价完成，用户ID：{}，原始金额：{}，优惠金额：{}，应付金额：{}",
                request.getUserId(), originAmount, discountAmount, payableAmount);

        return OrderPriceResponse.builder()
                .userId(request.getUserId())
                .orderType(orderType().getCode())
                .strategyName("会员订单计价策略")
                .originAmount(originAmount)
                .discountAmount(discountAmount)
                .payableAmount(payableAmount)
                .build();
    }
}
```

### 秒杀订单计价策略

秒杀订单享受 7 折优惠，但为了避免出现 0 元订单，增加最低支付金额限制。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorystrategy/strategy/impl/FlashSaleOrderPriceStrategy.java`

```java
package io.github.atengk.pattern.combination.factorystrategy.strategy.impl;

import io.github.atengk.pattern.combination.factorystrategy.dto.OrderPriceRequest;
import io.github.atengk.pattern.combination.factorystrategy.dto.OrderPriceResponse;
import io.github.atengk.pattern.combination.factorystrategy.enums.OrderTypeEnum;
import io.github.atengk.pattern.combination.factorystrategy.strategy.OrderPriceStrategy;
import io.github.atengk.pattern.combination.factorystrategy.util.MoneyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 秒杀订单计价策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class FlashSaleOrderPriceStrategy implements OrderPriceStrategy {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.30");

    private static final BigDecimal MIN_PAYABLE_AMOUNT = new BigDecimal("1.00");

    /**
     * 获取当前策略支持的订单类型
     *
     * @return 订单类型
     */
    @Override
    public OrderTypeEnum orderType() {
        return OrderTypeEnum.FLASH_SALE;
    }

    /**
     * 计算秒杀订单价格
     *
     * @param request 订单计价请求
     * @return 订单计价响应
     */
    @Override
    public OrderPriceResponse calculate(OrderPriceRequest request) {
        BigDecimal originAmount = MoneyUtils.round(request.getOriginAmount());
        BigDecimal discountAmount = MoneyUtils.multiply(originAmount, DISCOUNT_RATE);
        BigDecimal payableAmount = MoneyUtils.subtract(originAmount, discountAmount);

        if (payableAmount.compareTo(MIN_PAYABLE_AMOUNT) < 0) {
            log.info("秒杀订单触发最低支付金额限制，用户ID：{}，原应付金额：{}，最低支付金额：{}",
                    request.getUserId(), payableAmount, MIN_PAYABLE_AMOUNT);
            payableAmount = MIN_PAYABLE_AMOUNT;
            discountAmount = MoneyUtils.subtract(originAmount, payableAmount);
        }

        log.info("秒杀订单计价完成，用户ID：{}，原始金额：{}，优惠金额：{}，应付金额：{}",
                request.getUserId(), originAmount, discountAmount, payableAmount);

        return OrderPriceResponse.builder()
                .userId(request.getUserId())
                .orderType(orderType().getCode())
                .strategyName("秒杀订单计价策略")
                .originAmount(originAmount)
                .discountAmount(discountAmount)
                .payableAmount(payableAmount)
                .build();
    }
}
```

### 简单工厂

简单工厂负责根据订单类型获取对应策略对象。这里通过构造方法注入 `List<OrderPriceStrategy>`，Spring 会自动收集所有策略实现类。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorystrategy/factory/OrderPriceStrategyFactory.java`

```java
package io.github.atengk.pattern.combination.factorystrategy.factory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.combination.factorystrategy.enums.OrderTypeEnum;
import io.github.atengk.pattern.combination.factorystrategy.strategy.OrderPriceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 订单计价策略简单工厂
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

### Service 接口

Service 接口定义订单计价能力，Controller 不直接感知具体策略和工厂细节。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorystrategy/service/OrderPriceService.java`

```java
package io.github.atengk.pattern.combination.factorystrategy.service;

import io.github.atengk.pattern.combination.factorystrategy.dto.OrderPriceRequest;
import io.github.atengk.pattern.combination.factorystrategy.dto.OrderPriceResponse;

/**
 * 订单计价服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderPriceService {

    /**
     * 计算订单价格
     *
     * @param request 订单计价请求
     * @return 订单计价响应
     */
    OrderPriceResponse calculate(OrderPriceRequest request);
}
```

### Service 实现类

Service 实现类只负责编排流程：接收请求、通过简单工厂获取策略、调用策略完成计价。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorystrategy/service/impl/OrderPriceServiceImpl.java`

```java
package io.github.atengk.pattern.combination.factorystrategy.service.impl;

import io.github.atengk.pattern.combination.factorystrategy.dto.OrderPriceRequest;
import io.github.atengk.pattern.combination.factorystrategy.dto.OrderPriceResponse;
import io.github.atengk.pattern.combination.factorystrategy.factory.OrderPriceStrategyFactory;
import io.github.atengk.pattern.combination.factorystrategy.service.OrderPriceService;
import io.github.atengk.pattern.combination.factorystrategy.strategy.OrderPriceStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单计价服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPriceServiceImpl implements OrderPriceService {

    private final OrderPriceStrategyFactory orderPriceStrategyFactory;

    /**
     * 计算订单价格
     *
     * @param request 订单计价请求
     * @return 订单计价响应
     */
    @Override
    public OrderPriceResponse calculate(OrderPriceRequest request) {
        OrderPriceStrategy strategy = orderPriceStrategyFactory.getStrategy(request.getOrderType());
        OrderPriceResponse response = strategy.calculate(request);

        log.info("订单计价流程完成，用户ID：{}，订单类型：{}，应付金额：{}",
                response.getUserId(), response.getOrderType(), response.getPayableAmount());

        return response;
    }
}
```

### 统一响应对象

统一响应对象用于包装接口返回结果，便于前端或调用方统一处理成功和失败响应。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorystrategy/result/Result.java`

```java
package io.github.atengk.pattern.combination.factorystrategy.result;

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

全局异常处理器用于统一处理参数异常和业务异常，避免接口直接返回默认异常堆栈。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorystrategy/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.combination.factorystrategy.handler;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.factorystrategy.result.Result;
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
        log.warn("业务参数异常：{}", exception.getMessage());
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

Controller 提供订单计价接口，外部调用方只需要传入订单类型和原始金额即可。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorystrategy/controller/OrderPriceController.java`

```java
package io.github.atengk.pattern.combination.factorystrategy.controller;

import io.github.atengk.pattern.combination.factorystrategy.dto.OrderPriceRequest;
import io.github.atengk.pattern.combination.factorystrategy.dto.OrderPriceResponse;
import io.github.atengk.pattern.combination.factorystrategy.result.Result;
import io.github.atengk.pattern.combination.factorystrategy.service.OrderPriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单计价控制器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order-prices")
public class OrderPriceController {

    private final OrderPriceService orderPriceService;

    /**
     * 计算订单价格
     *
     * @param request 订单计价请求
     * @return 订单计价响应
     */
    @PostMapping("/calculate")
    public Result<OrderPriceResponse> calculate(@Valid @RequestBody OrderPriceRequest request) {
        return Result.success(orderPriceService.calculate(request));
    }
}
```

## 使用方式

启动 Spring Boot 项目后，可以通过接口验证不同订单类型是否命中不同策略。

接口信息如下：

```text
请求地址：POST /api/order-prices/calculate
Content-Type：application/json
```

普通订单请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-prices/calculate" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
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
    "userId": 1001,
    "orderType": "NORMAL",
    "strategyName": "普通订单计价策略",
    "originAmount": 100.00,
    "discountAmount": 0,
    "payableAmount": 100.00
  }
}
```

会员订单请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-prices/calculate" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1002,
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
    "userId": 1002,
    "orderType": "VIP",
    "strategyName": "会员订单计价策略",
    "originAmount": 100.00,
    "discountAmount": 10.00,
    "payableAmount": 90.00
  }
}
```

秒杀订单请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-prices/calculate" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1003,
    "orderType": "FLASH_SALE",
    "originAmount": 100.00
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1003,
    "orderType": "FLASH_SALE",
    "strategyName": "秒杀订单计价策略",
    "originAmount": 100.00,
    "discountAmount": 30.00,
    "payableAmount": 70.00
  }
}
```

不支持的订单类型请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-prices/calculate" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1004,
    "orderType": "GROUP_BUY",
    "originAmount": 100.00
  }'
```

响应示例：

```json
{
  "code": 400,
  "message": "不支持的订单类型：GROUP_BUY",
  "data": null
}
```

## 新增策略方式

当业务新增一种订单类型时，例如新增“新人订单”，不需要修改 Controller 和 Service，只需要新增枚举值和策略实现类。

第一步，在 `OrderTypeEnum` 中新增枚举：

```java
NEW_USER("NEW_USER", "新人订单");
```

第二步，新增策略实现类。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorystrategy/strategy/impl/NewUserOrderPriceStrategy.java`

```java
package io.github.atengk.pattern.combination.factorystrategy.strategy.impl;

import io.github.atengk.pattern.combination.factorystrategy.dto.OrderPriceRequest;
import io.github.atengk.pattern.combination.factorystrategy.dto.OrderPriceResponse;
import io.github.atengk.pattern.combination.factorystrategy.enums.OrderTypeEnum;
import io.github.atengk.pattern.combination.factorystrategy.strategy.OrderPriceStrategy;
import io.github.atengk.pattern.combination.factorystrategy.util.MoneyUtils;
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

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.20");

    /**
     * 获取当前策略支持的订单类型
     *
     * @return 订单类型
     */
    @Override
    public OrderTypeEnum orderType() {
        return OrderTypeEnum.NEW_USER;
    }

    /**
     * 计算新人订单价格
     *
     * @param request 订单计价请求
     * @return 订单计价响应
     */
    @Override
    public OrderPriceResponse calculate(OrderPriceRequest request) {
        BigDecimal originAmount = MoneyUtils.round(request.getOriginAmount());
        BigDecimal discountAmount = MoneyUtils.multiply(originAmount, DISCOUNT_RATE);
        BigDecimal payableAmount = MoneyUtils.subtract(originAmount, discountAmount);

        log.info("新人订单计价完成，用户ID：{}，原始金额：{}，优惠金额：{}，应付金额：{}",
                request.getUserId(), originAmount, discountAmount, payableAmount);

        return OrderPriceResponse.builder()
                .userId(request.getUserId())
                .orderType(orderType().getCode())
                .strategyName("新人订单计价策略")
                .originAmount(originAmount)
                .discountAmount(discountAmount)
                .payableAmount(payableAmount)
                .build();
    }
}
```

新增完成后，Spring 会自动把 `NewUserOrderPriceStrategy` 注入到 `OrderPriceStrategyFactory` 中。只要 `orderType()` 返回的枚举不重复，工厂就可以正常识别新策略。

## 验证方式

可以从以下几个方面验证组合模式是否生效：

```text
1. 启动项目时，日志应输出策略工厂初始化完成，并显示策略数量
2. 请求 NORMAL 类型时，应命中 NormalOrderPriceStrategy
3. 请求 VIP 类型时，应命中 VipOrderPriceStrategy
4. 请求 FLASH_SALE 类型时，应命中 FlashSaleOrderPriceStrategy
5. 请求不存在的订单类型时，应返回 400 错误
6. 新增策略实现类后，不需要修改 Service 主流程
```

启动日志示例：

```text
订单计价策略工厂初始化完成，策略数量：3
```

请求接口后的业务日志示例：

```text
命中订单计价策略，订单类型：VIP，策略类：VipOrderPriceStrategy
会员订单计价完成，用户ID：1002，原始金额：100.00，优惠金额：10.00，应付金额：90.00
订单计价流程完成，用户ID：1002，订单类型：VIP，应付金额：90.00
```

## 组合效果

简单工厂模式和策略模式组合后，各自负责不同变化点：

| 模式         | 职责             | 在示例中的体现                                               |
| ------------ | ---------------- | ------------------------------------------------------------ |
| 简单工厂模式 | 根据类型获取对象 | `OrderPriceStrategyFactory` 根据订单类型获取计价策略         |
| 策略模式     | 封装不同算法     | `NormalOrderPriceStrategy`、`VipOrderPriceStrategy`、`FlashSaleOrderPriceStrategy` 分别处理不同计价规则 |

这种结构的核心收益是：

```text
Controller 不关心具体计价规则
Service 不写复杂 if else
工厂统一管理策略选择
策略类独立封装业务算法
新增订单类型时对主流程影响很小
每个策略可以单独测试
```

## 注意事项

简单工厂模式和策略模式组合使用时，不建议把所有逻辑都塞进工厂。工厂只应该负责“根据类型找到策略”，不应该负责“执行具体业务规则”。具体业务规则应该放在策略实现类中。

在 Spring Boot 项目中，简单工厂通常不需要手动 `new` 策略对象。更推荐通过 Spring 注入所有策略 Bean，再由工厂组装成 `Map`。这样既保留了简单工厂的分发能力，又能利用 Spring 的依赖注入能力。

如果策略数量较少，并且变化不频繁，直接使用 `if else` 也可以接受。只有当业务类型不断增加、算法差异明显、主流程开始膨胀时，才更适合引入这种组合。
