# 策略模式

策略模式用于定义一组可替换的算法或业务处理方式，并在运行时根据条件选择其中一种执行。
在 Spring Boot 项目中，策略模式常用于支付渠道、优惠计算、运费计算、登录方式、文件导出、消息发送、数据同步、风控规则、订单结算等场景。

本文以“订单结算优惠计算”为例。系统支持普通结算、会员折扣、满减优惠、秒杀价结算等多种计算方式。每种计算方式独立成一个策略类，业务服务只根据策略编码选择并执行对应策略。

## 适用场景

策略模式适合处理“同一类业务有多种处理算法，并且需要按类型动态选择”的场景。

订单结算中常见优惠策略如下：

| 策略编码         | 策略名称 | 说明                       |
| ---------------- | -------- | -------------------------- |
| `NORMAL`         | 普通结算 | 不使用优惠，原价支付       |
| `VIP_DISCOUNT`   | 会员折扣 | VIP 用户享受折扣           |
| `FULL_REDUCTION` | 满减优惠 | 满指定金额后减免           |
| `FLASH_SALE`     | 秒杀价   | 按秒杀单价重新计算应付金额 |

如果在 Service 中使用大量 `if-else` 判断策略类型，后续新增优惠策略时会不断修改主业务代码。策略模式可以把每种优惠计算逻辑拆成独立策略类，新增策略时只新增类，不修改原有结算主流程。

## 基础配置

本示例基于 Spring Boot 3，使用 Hutool、Lombok 和 Validation。Hutool 用于字符串判断、集合判断、金额计算和格式化，Validation 用于接口参数基础校验。

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

    <!-- Hutool：提供字符串、集合、金额计算等常用工具 -->
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
src/main/java/io/github/atengk/pattern/strategy
├── StrategyApplication.java
├── common
│   ├── ApiResult.java
│   ├── BizException.java
│   └── GlobalExceptionHandler.java
└── order
    ├── controller
    │   └── OrderSettlementController.java
    ├── dto
    │   └── OrderSettlementRequest.java
    ├── service
    │   ├── OrderSettlementService.java
    │   └── impl
    │       └── OrderSettlementServiceImpl.java
    ├── strategy
    │   ├── AbstractOrderDiscountStrategy.java
    │   ├── OrderDiscountStrategy.java
    │   ├── OrderDiscountStrategyExecutor.java
    │   ├── OrderDiscountStrategyCodes.java
    │   └── impl
    │       ├── FlashSaleDiscountStrategy.java
    │       ├── FullReductionDiscountStrategy.java
    │       ├── NormalDiscountStrategy.java
    │       └── VipDiscountStrategy.java
    └── vo
        └── OrderSettlementResultVO.java
```

## 核心设计

本示例把策略模式拆成四个核心角色：

| 角色               | 项目中的类                      | 说明                       |
| ------------------ | ------------------------------- | -------------------------- |
| Strategy           | `OrderDiscountStrategy`         | 统一策略接口               |
| ConcreteStrategy   | `NormalDiscountStrategy` 等     | 具体优惠计算策略           |
| Context / Executor | `OrderDiscountStrategyExecutor` | 根据策略编码选择并执行策略 |
| Client             | `OrderSettlementServiceImpl`    | 订单结算业务服务           |

执行流程如下：

```text
Controller
  -> OrderSettlementService
    -> OrderDiscountStrategyExecutor
      -> 根据 strategyCode 选择具体策略
        -> NormalDiscountStrategy
        -> VipDiscountStrategy
        -> FullReductionDiscountStrategy
        -> FlashSaleDiscountStrategy
    -> 返回结算结果
```

策略模式的重点是：结算服务不关心每种优惠如何计算，只关心使用哪个策略编码完成结算。

## 公共代码

公共响应对象、业务异常和全局异常处理用于统一接口返回。实际项目中可以复用已有基础包。

文件位置：`src/main/java/io/github/atengk/pattern/strategy/common/ApiResult.java`

```java
package io.github.atengk.pattern.strategy.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/strategy/common/BizException.java`

```java
package io.github.atengk.pattern.strategy.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/strategy/common/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.strategy.common;

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

下面给出策略模式的核心实现。示例使用接口请求中的策略编码选择优惠计算策略，实际项目中策略编码通常来自商品活动、优惠券规则、会员权益、运营配置或订单上下文。

文件位置：`src/main/java/io/github/atengk/pattern/strategy/StrategyApplication.java`

```java
package io.github.atengk.pattern.strategy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 策略模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class StrategyApplication {

    public static void main(String[] args) {
        SpringApplication.run(StrategyApplication.class, args);
    }

}
```

## 请求对象和响应对象

请求对象用于接收订单结算参数。不同策略可能使用不同字段，例如会员折扣使用 `userLevel`，秒杀价使用 `flashSaleUnitPrice`。

文件位置：`src/main/java/io/github/atengk/pattern/strategy/order/dto/OrderSettlementRequest.java`

```java
package io.github.atengk.pattern.strategy.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单结算请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderSettlementRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;

    @NotNull(message = "订单原始金额不能为空")
    @DecimalMin(value = "0.01", message = "订单原始金额必须大于0")
    private BigDecimal originalAmount;

    @NotBlank(message = "优惠策略编码不能为空")
    private String strategyCode;

    private String userLevel;

    private BigDecimal flashSaleUnitPrice;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/strategy/order/vo/OrderSettlementResultVO.java`

```java
package io.github.atengk.pattern.strategy.order.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单结算结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderSettlementResultVO {

    private Long userId;

    private Long productId;

    private Integer quantity;

    private String strategyCode;

    private String strategyName;

    private BigDecimal originalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payableAmount;

    private String message;

}
```

## 策略接口

策略接口定义所有优惠策略必须实现的行为。每个策略都需要提供策略编码、策略名称和计算方法。

文件位置：`src/main/java/io/github/atengk/pattern/strategy/order/strategy/OrderDiscountStrategy.java`

```java
package io.github.atengk.pattern.strategy.order.strategy;

import io.github.atengk.pattern.strategy.order.dto.OrderSettlementRequest;
import io.github.atengk.pattern.strategy.order.vo.OrderSettlementResultVO;

/**
 * 订单优惠策略接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderDiscountStrategy {

    /**
     * 返回策略编码
     *
     * @return 策略编码
     */
    String strategyCode();

    /**
     * 返回策略名称
     *
     * @return 策略名称
     */
    String strategyName();

    /**
     * 计算订单结算金额
     *
     * @param request 订单结算请求
     * @return 订单结算结果
     */
    OrderSettlementResultVO calculate(OrderSettlementRequest request);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/strategy/order/strategy/OrderDiscountStrategyCodes.java`

```java
package io.github.atengk.pattern.strategy.order.strategy;

/**
 * 订单优惠策略编码常量
 *
 * @author Ateng
 * @since 2026-05-13
 */
public final class OrderDiscountStrategyCodes {

    public static final String NORMAL = "NORMAL";

    public static final String VIP_DISCOUNT = "VIP_DISCOUNT";

    public static final String FULL_REDUCTION = "FULL_REDUCTION";

    public static final String FLASH_SALE = "FLASH_SALE";

    private OrderDiscountStrategyCodes() {
    }

}
```

## 抽象策略基类

抽象策略基类封装通用金额处理逻辑，例如金额保底、折扣金额计算和结果对象构建。具体策略只关注自己的算法。

文件位置：`src/main/java/io/github/atengk/pattern/strategy/order/strategy/AbstractOrderDiscountStrategy.java`

```java
package io.github.atengk.pattern.strategy.order.strategy;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.strategy.order.dto.OrderSettlementRequest;
import io.github.atengk.pattern.strategy.order.vo.OrderSettlementResultVO;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 订单优惠策略抽象基类
 *
 * @author Ateng
 * @since 2026-05-13
 */
public abstract class AbstractOrderDiscountStrategy implements OrderDiscountStrategy {

    private static final BigDecimal MIN_PAYABLE_AMOUNT = BigDecimal.valueOf(0.01);

    /**
     * 构建订单结算结果
     *
     * @param request 订单结算请求
     * @param payableAmount 应付金额
     * @param message 结算说明
     * @return 订单结算结果
     */
    protected OrderSettlementResultVO buildResult(OrderSettlementRequest request,
                                                  BigDecimal payableAmount,
                                                  String message) {
        BigDecimal safePayableAmount = ensurePayableAmount(payableAmount);
        BigDecimal discountAmount = NumberUtil.sub(request.getOriginalAmount(), safePayableAmount);

        if (NumberUtil.isLess(discountAmount, BigDecimal.ZERO)) {
            discountAmount = BigDecimal.ZERO;
        }

        return OrderSettlementResultVO.builder()
                .userId(request.getUserId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .strategyCode(strategyCode())
                .strategyName(strategyName())
                .originalAmount(scaleAmount(request.getOriginalAmount()))
                .discountAmount(scaleAmount(discountAmount))
                .payableAmount(scaleAmount(safePayableAmount))
                .message(message)
                .build();
    }

    /**
     * 保证应付金额合法
     *
     * @param payableAmount 应付金额
     * @return 合法应付金额
     */
    protected BigDecimal ensurePayableAmount(BigDecimal payableAmount) {
        if (NumberUtil.isLessOrEqual(payableAmount, BigDecimal.ZERO)) {
            return MIN_PAYABLE_AMOUNT;
        }
        return payableAmount;
    }

    /**
     * 金额保留两位小数
     *
     * @param amount 金额
     * @return 格式化后的金额
     */
    protected BigDecimal scaleAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

}
```

## 具体策略实现

每个具体策略类只实现自己的优惠计算逻辑。新增策略时，只需要新增一个实现类并交给 Spring 管理。

普通结算策略不做优惠，原价支付。

文件位置：`src/main/java/io/github/atengk/pattern/strategy/order/strategy/impl/NormalDiscountStrategy.java`

```java
package io.github.atengk.pattern.strategy.order.strategy.impl;

import io.github.atengk.pattern.strategy.order.dto.OrderSettlementRequest;
import io.github.atengk.pattern.strategy.order.strategy.AbstractOrderDiscountStrategy;
import io.github.atengk.pattern.strategy.order.strategy.OrderDiscountStrategyCodes;
import io.github.atengk.pattern.strategy.order.vo.OrderSettlementResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 普通结算策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class NormalDiscountStrategy extends AbstractOrderDiscountStrategy {

    /**
     * 返回策略编码
     *
     * @return 策略编码
     */
    @Override
    public String strategyCode() {
        return OrderDiscountStrategyCodes.NORMAL;
    }

    /**
     * 返回策略名称
     *
     * @return 策略名称
     */
    @Override
    public String strategyName() {
        return "普通结算";
    }

    /**
     * 计算普通结算金额
     *
     * @param request 订单结算请求
     * @return 订单结算结果
     */
    @Override
    public OrderSettlementResultVO calculate(OrderSettlementRequest request) {
        log.info("执行普通结算策略，userId：{}，productId：{}，originalAmount：{}",
                request.getUserId(), request.getProductId(), request.getOriginalAmount());

        return buildResult(request, request.getOriginalAmount(), "普通结算，无优惠");
    }

}
```

会员折扣策略根据用户等级计算折扣价。

文件位置：`src/main/java/io/github/atengk/pattern/strategy/order/strategy/impl/VipDiscountStrategy.java`

```java
package io.github.atengk.pattern.strategy.order.strategy.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.strategy.common.BizException;
import io.github.atengk.pattern.strategy.order.dto.OrderSettlementRequest;
import io.github.atengk.pattern.strategy.order.strategy.AbstractOrderDiscountStrategy;
import io.github.atengk.pattern.strategy.order.strategy.OrderDiscountStrategyCodes;
import io.github.atengk.pattern.strategy.order.vo.OrderSettlementResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 会员折扣策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class VipDiscountStrategy extends AbstractOrderDiscountStrategy {

    private static final String VIP_LEVEL = "VIP";

    private static final BigDecimal VIP_DISCOUNT_RATE = BigDecimal.valueOf(0.90);

    /**
     * 返回策略编码
     *
     * @return 策略编码
     */
    @Override
    public String strategyCode() {
        return OrderDiscountStrategyCodes.VIP_DISCOUNT;
    }

    /**
     * 返回策略名称
     *
     * @return 策略名称
     */
    @Override
    public String strategyName() {
        return "会员折扣";
    }

    /**
     * 计算会员折扣金额
     *
     * @param request 订单结算请求
     * @return 订单结算结果
     */
    @Override
    public OrderSettlementResultVO calculate(OrderSettlementRequest request) {
        if (!StrUtil.equalsIgnoreCase(request.getUserLevel(), VIP_LEVEL)) {
            throw new BizException("当前用户不是VIP，不能使用会员折扣策略");
        }

        BigDecimal payableAmount = NumberUtil.mul(request.getOriginalAmount(), VIP_DISCOUNT_RATE);

        log.info("执行会员折扣策略，userId：{}，userLevel：{}，originalAmount：{}，payableAmount：{}",
                request.getUserId(), request.getUserLevel(), request.getOriginalAmount(), payableAmount);

        return buildResult(request, payableAmount, "VIP会员享受9折优惠");
    }

}
```

满减策略在订单金额达到门槛后减免固定金额。

文件位置：`src/main/java/io/github/atengk/pattern/strategy/order/strategy/impl/FullReductionDiscountStrategy.java`

```java
package io.github.atengk.pattern.strategy.order.strategy.impl;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.strategy.common.BizException;
import io.github.atengk.pattern.strategy.order.dto.OrderSettlementRequest;
import io.github.atengk.pattern.strategy.order.strategy.AbstractOrderDiscountStrategy;
import io.github.atengk.pattern.strategy.order.strategy.OrderDiscountStrategyCodes;
import io.github.atengk.pattern.strategy.order.vo.OrderSettlementResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 满减优惠策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class FullReductionDiscountStrategy extends AbstractOrderDiscountStrategy {

    private static final BigDecimal THRESHOLD_AMOUNT = BigDecimal.valueOf(100);

    private static final BigDecimal REDUCTION_AMOUNT = BigDecimal.valueOf(20);

    /**
     * 返回策略编码
     *
     * @return 策略编码
     */
    @Override
    public String strategyCode() {
        return OrderDiscountStrategyCodes.FULL_REDUCTION;
    }

    /**
     * 返回策略名称
     *
     * @return 策略名称
     */
    @Override
    public String strategyName() {
        return "满减优惠";
    }

    /**
     * 计算满减优惠金额
     *
     * @param request 订单结算请求
     * @return 订单结算结果
     */
    @Override
    public OrderSettlementResultVO calculate(OrderSettlementRequest request) {
        if (NumberUtil.isLess(request.getOriginalAmount(), THRESHOLD_AMOUNT)) {
            throw new BizException("订单金额未达到满减门槛");
        }

        BigDecimal payableAmount = NumberUtil.sub(request.getOriginalAmount(), REDUCTION_AMOUNT);

        log.info("执行满减优惠策略，userId：{}，thresholdAmount：{}，reductionAmount：{}，payableAmount：{}",
                request.getUserId(), THRESHOLD_AMOUNT, REDUCTION_AMOUNT, payableAmount);

        return buildResult(request, payableAmount, "满100减20");
    }

}
```

秒杀价策略使用秒杀单价乘以购买数量重新计算应付金额。

文件位置：`src/main/java/io/github/atengk/pattern/strategy/order/strategy/impl/FlashSaleDiscountStrategy.java`

```java
package io.github.atengk.pattern.strategy.order.strategy.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.strategy.common.BizException;
import io.github.atengk.pattern.strategy.order.dto.OrderSettlementRequest;
import io.github.atengk.pattern.strategy.order.strategy.AbstractOrderDiscountStrategy;
import io.github.atengk.pattern.strategy.order.strategy.OrderDiscountStrategyCodes;
import io.github.atengk.pattern.strategy.order.vo.OrderSettlementResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 秒杀价结算策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class FlashSaleDiscountStrategy extends AbstractOrderDiscountStrategy {

    /**
     * 返回策略编码
     *
     * @return 策略编码
     */
    @Override
    public String strategyCode() {
        return OrderDiscountStrategyCodes.FLASH_SALE;
    }

    /**
     * 返回策略名称
     *
     * @return 策略名称
     */
    @Override
    public String strategyName() {
        return "秒杀价";
    }

    /**
     * 计算秒杀价金额
     *
     * @param request 订单结算请求
     * @return 订单结算结果
     */
    @Override
    public OrderSettlementResultVO calculate(OrderSettlementRequest request) {
        if (ObjectUtil.isNull(request.getFlashSaleUnitPrice())
                || NumberUtil.isLessOrEqual(request.getFlashSaleUnitPrice(), BigDecimal.ZERO)) {
            throw new BizException("秒杀单价必须大于0");
        }

        BigDecimal payableAmount = NumberUtil.mul(request.getFlashSaleUnitPrice(), request.getQuantity());

        if (NumberUtil.isGreater(payableAmount, request.getOriginalAmount())) {
            throw new BizException("秒杀价不能高于订单原始金额");
        }

        log.info("执行秒杀价策略，userId：{}，productId：{}，unitPrice：{}，quantity：{}，payableAmount：{}",
                request.getUserId(),
                request.getProductId(),
                request.getFlashSaleUnitPrice(),
                request.getQuantity(),
                payableAmount);

        return buildResult(request, payableAmount, "按秒杀价结算");
    }

}
```

## 策略执行器

策略执行器负责收集所有策略，并根据策略编码执行对应策略。
业务服务不直接依赖具体策略类，只依赖执行器。

文件位置：`src/main/java/io/github/atengk/pattern/strategy/order/strategy/OrderDiscountStrategyExecutor.java`

```java
package io.github.atengk.pattern.strategy.order.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.strategy.common.BizException;
import io.github.atengk.pattern.strategy.order.dto.OrderSettlementRequest;
import io.github.atengk.pattern.strategy.order.vo.OrderSettlementResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单优惠策略执行器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderDiscountStrategyExecutor {

    private final Map<String, OrderDiscountStrategy> strategyMap;

    /**
     * 初始化订单优惠策略执行器
     *
     * @param strategies Spring 容器中的订单优惠策略集合
     */
    public OrderDiscountStrategyExecutor(List<OrderDiscountStrategy> strategies) {
        if (CollUtil.isEmpty(strategies)) {
            this.strategyMap = Collections.emptyMap();
            log.warn("订单优惠策略执行器未加载到任何策略");
            return;
        }

        Map<String, OrderDiscountStrategy> registerMap = new HashMap<>(strategies.size());
        for (OrderDiscountStrategy strategy : strategies) {
            String strategyCode = StrUtil.upperCase(strategy.strategyCode());
            if (registerMap.containsKey(strategyCode)) {
                throw new IllegalStateException("订单优惠策略编码重复：" + strategyCode);
            }

            registerMap.put(strategyCode, strategy);
            log.info("注册订单优惠策略，strategyCode：{}，strategyName：{}",
                    strategyCode, strategy.strategyName());
        }

        this.strategyMap = Collections.unmodifiableMap(registerMap);
    }

    /**
     * 执行订单优惠策略
     *
     * @param request 订单结算请求
     * @return 订单结算结果
     */
    public OrderSettlementResultVO execute(OrderSettlementRequest request) {
        String strategyCode = StrUtil.upperCase(request.getStrategyCode());
        OrderDiscountStrategy strategy = strategyMap.get(strategyCode);

        if (strategy == null) {
            throw new BizException(StrUtil.format("不支持的订单优惠策略：{}", request.getStrategyCode()));
        }

        log.info("开始执行订单优惠策略，strategyCode：{}，strategyName：{}",
                strategy.strategyCode(), strategy.strategyName());

        OrderSettlementResultVO result = strategy.calculate(request);

        log.info("订单优惠策略执行完成，strategyCode：{}，originalAmount：{}，discountAmount：{}，payableAmount：{}",
                result.getStrategyCode(),
                result.getOriginalAmount(),
                result.getDiscountAmount(),
                result.getPayableAmount());

        return result;
    }

}
```

## 业务服务

业务服务负责订单结算主流程。可以看到，这里没有策略类型的 `if-else`，只是调用策略执行器完成计算。

文件位置：`src/main/java/io/github/atengk/pattern/strategy/order/service/OrderSettlementService.java`

```java
package io.github.atengk.pattern.strategy.order.service;

import io.github.atengk.pattern.strategy.order.dto.OrderSettlementRequest;
import io.github.atengk.pattern.strategy.order.vo.OrderSettlementResultVO;

/**
 * 订单结算服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderSettlementService {

    /**
     * 计算订单结算金额
     *
     * @param request 订单结算请求
     * @return 订单结算结果
     */
    OrderSettlementResultVO settle(OrderSettlementRequest request);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/strategy/order/service/impl/OrderSettlementServiceImpl.java`

```java
package io.github.atengk.pattern.strategy.order.service.impl;

import io.github.atengk.pattern.strategy.order.dto.OrderSettlementRequest;
import io.github.atengk.pattern.strategy.order.service.OrderSettlementService;
import io.github.atengk.pattern.strategy.order.strategy.OrderDiscountStrategyExecutor;
import io.github.atengk.pattern.strategy.order.vo.OrderSettlementResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单结算服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSettlementServiceImpl implements OrderSettlementService {

    private final OrderDiscountStrategyExecutor orderDiscountStrategyExecutor;

    /**
     * 计算订单结算金额
     *
     * @param request 订单结算请求
     * @return 订单结算结果
     */
    @Override
    public OrderSettlementResultVO settle(OrderSettlementRequest request) {
        log.info("开始订单结算，userId：{}，productId：{}，strategyCode：{}",
                request.getUserId(), request.getProductId(), request.getStrategyCode());

        OrderSettlementResultVO result = orderDiscountStrategyExecutor.execute(request);

        log.info("订单结算完成，userId：{}，productId：{}，payableAmount：{}",
                result.getUserId(), result.getProductId(), result.getPayableAmount());

        return result;
    }

}
```

## 控制器接口

控制器提供订单结算入口。调用方传入策略编码，系统自动选择对应策略执行。

文件位置：`src/main/java/io/github/atengk/pattern/strategy/order/controller/OrderSettlementController.java`

```java
package io.github.atengk.pattern.strategy.order.controller;

import io.github.atengk.pattern.strategy.common.ApiResult;
import io.github.atengk.pattern.strategy.order.dto.OrderSettlementRequest;
import io.github.atengk.pattern.strategy.order.service.OrderSettlementService;
import io.github.atengk.pattern.strategy.order.vo.OrderSettlementResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单结算接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/order-settlements")
@RequiredArgsConstructor
public class OrderSettlementController {

    private final OrderSettlementService orderSettlementService;

    /**
     * 计算订单结算金额
     *
     * @param request 订单结算请求
     * @return 订单结算结果
     */
    @PostMapping("/calculate")
    public ApiResult<OrderSettlementResultVO> calculate(@Valid @RequestBody OrderSettlementRequest request) {
        return ApiResult.success(orderSettlementService.settle(request));
    }

}
```

## 使用方式

启动项目后，调用订单结算接口即可触发策略模式。

接口信息：

| 项目         | 内容                           |
| ------------ | ------------------------------ |
| 请求路径     | `/order-settlements/calculate` |
| 请求方法     | `POST`                         |
| Content-Type | `application/json`             |
| 核心字段     | `strategyCode`                 |

普通结算请求：

```bash
curl -X POST "http://localhost:8080/order-settlements/calculate" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "productId": 20001,
    "quantity": 2,
    "originalAmount": 199.90,
    "strategyCode": "NORMAL"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 10001,
    "productId": 20001,
    "quantity": 2,
    "strategyCode": "NORMAL",
    "strategyName": "普通结算",
    "originalAmount": 199.90,
    "discountAmount": 0.00,
    "payableAmount": 199.90,
    "message": "普通结算，无优惠"
  }
}
```

会员折扣请求：

```bash
curl -X POST "http://localhost:8080/order-settlements/calculate" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "productId": 20001,
    "quantity": 2,
    "originalAmount": 199.90,
    "strategyCode": "VIP_DISCOUNT",
    "userLevel": "VIP"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 10001,
    "productId": 20001,
    "quantity": 2,
    "strategyCode": "VIP_DISCOUNT",
    "strategyName": "会员折扣",
    "originalAmount": 199.90,
    "discountAmount": 19.99,
    "payableAmount": 179.91,
    "message": "VIP会员享受9折优惠"
  }
}
```

满减优惠请求：

```bash
curl -X POST "http://localhost:8080/order-settlements/calculate" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10002,
    "productId": 20002,
    "quantity": 1,
    "originalAmount": 159.90,
    "strategyCode": "FULL_REDUCTION"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 10002,
    "productId": 20002,
    "quantity": 1,
    "strategyCode": "FULL_REDUCTION",
    "strategyName": "满减优惠",
    "originalAmount": 159.90,
    "discountAmount": 20.00,
    "payableAmount": 139.90,
    "message": "满100减20"
  }
}
```

秒杀价请求：

```bash
curl -X POST "http://localhost:8080/order-settlements/calculate" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10003,
    "productId": 20003,
    "quantity": 2,
    "originalAmount": 299.80,
    "strategyCode": "FLASH_SALE",
    "flashSaleUnitPrice": 99.90
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 10003,
    "productId": 20003,
    "quantity": 2,
    "strategyCode": "FLASH_SALE",
    "strategyName": "秒杀价",
    "originalAmount": 299.80,
    "discountAmount": 100.00,
    "payableAmount": 199.80,
    "message": "按秒杀价结算"
  }
}
```

不支持的策略请求：

```bash
curl -X POST "http://localhost:8080/order-settlements/calculate" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "productId": 20001,
    "quantity": 1,
    "originalAmount": 99.90,
    "strategyCode": "UNKNOWN"
  }'
```

返回示例：

```json
{
  "code": 500,
  "message": "不支持的订单优惠策略：UNKNOWN",
  "data": null
}
```

## 验证方式

项目启动后，可以看到策略注册日志：

```text
注册订单优惠策略，strategyCode：NORMAL，strategyName：普通结算
注册订单优惠策略，strategyCode：VIP_DISCOUNT，strategyName：会员折扣
注册订单优惠策略，strategyCode：FULL_REDUCTION，strategyName：满减优惠
注册订单优惠策略，strategyCode：FLASH_SALE，strategyName：秒杀价
```

执行会员折扣策略后，可以看到类似日志：

```text
开始订单结算，userId：10001，productId：20001，strategyCode：VIP_DISCOUNT
开始执行订单优惠策略，strategyCode：VIP_DISCOUNT，strategyName：会员折扣
执行会员折扣策略，userId：10001，userLevel：VIP，originalAmount：199.90，payableAmount：179.9100
订单优惠策略执行完成，strategyCode：VIP_DISCOUNT，originalAmount：199.90，discountAmount：19.99，payableAmount：179.91
订单结算完成，userId：10001，productId：20001，payableAmount：179.91
```

执行非法策略时，可以看到业务异常日志：

```text
业务处理失败：不支持的订单优惠策略：UNKNOWN
```

## 扩展方式

如果后续新增“新人优惠策略”，只需要三步。

第一，新增策略编码：

```java
public static final String NEW_USER_DISCOUNT = "NEW_USER_DISCOUNT";
```

第二，新增策略实现类。

文件位置：`src/main/java/io/github/atengk/pattern/strategy/order/strategy/impl/NewUserDiscountStrategy.java`

```java
package io.github.atengk.pattern.strategy.order.strategy.impl;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.strategy.order.dto.OrderSettlementRequest;
import io.github.atengk.pattern.strategy.order.strategy.AbstractOrderDiscountStrategy;
import io.github.atengk.pattern.strategy.order.strategy.OrderDiscountStrategyCodes;
import io.github.atengk.pattern.strategy.order.vo.OrderSettlementResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 新人优惠策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class NewUserDiscountStrategy extends AbstractOrderDiscountStrategy {

    private static final BigDecimal NEW_USER_DISCOUNT_AMOUNT = BigDecimal.valueOf(15);

    /**
     * 返回策略编码
     *
     * @return 策略编码
     */
    @Override
    public String strategyCode() {
        return OrderDiscountStrategyCodes.NEW_USER_DISCOUNT;
    }

    /**
     * 返回策略名称
     *
     * @return 策略名称
     */
    @Override
    public String strategyName() {
        return "新人优惠";
    }

    /**
     * 计算新人优惠金额
     *
     * @param request 订单结算请求
     * @return 订单结算结果
     */
    @Override
    public OrderSettlementResultVO calculate(OrderSettlementRequest request) {
        BigDecimal payableAmount = NumberUtil.sub(request.getOriginalAmount(), NEW_USER_DISCOUNT_AMOUNT);

        log.info("执行新人优惠策略，userId：{}，discountAmount：{}，payableAmount：{}",
                request.getUserId(), NEW_USER_DISCOUNT_AMOUNT, payableAmount);

        return buildResult(request, payableAmount, "新人立减15元");
    }

}
```

第三，使用新策略编码调用接口：

```bash
curl -X POST "http://localhost:8080/order-settlements/calculate" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10004,
    "productId": 20004,
    "quantity": 1,
    "originalAmount": 89.90,
    "strategyCode": "NEW_USER_DISCOUNT"
  }'
```

由于策略执行器会自动加载 Spring 容器中的 `OrderDiscountStrategy` 实现类，所以新增策略后不需要修改执行器和结算服务。

## 结合数据库配置

实际项目中，策略编码通常不会由前端随意传入，而是根据商品、活动、用户权益、优惠券规则等配置计算出来。

可以设计一张活动策略配置表：

```sql
-- 活动策略配置表
CREATE TABLE t_discount_strategy_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    activity_code VARCHAR(64) NOT NULL COMMENT '活动编码',
    strategy_code VARCHAR(64) NOT NULL COMMENT '策略编码',
    strategy_name VARCHAR(128) NOT NULL COMMENT '策略名称',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_activity_code (activity_code),
    KEY idx_strategy_code (strategy_code)
) COMMENT='活动策略配置表';
```

订单结算时可以先根据活动编码查出 `strategy_code`，再交给策略执行器执行。这样运营只需要配置活动和策略关系，代码侧只需要维护具体策略实现。

## 优点和注意事项

策略模式的核心价值是把多种算法或业务处理方式拆开，使它们可以独立扩展和替换。

| 注意事项         | 说明                                                        |
| ---------------- | ----------------------------------------------------------- |
| 策略编码要稳定   | 策略编码可能被数据库、前端、运营配置依赖，不建议随意修改    |
| 策略职责要单一   | 一个策略只处理一种明确算法，不要混入多个业务分支            |
| 公共逻辑放抽象类 | 金额格式化、结果构建、通用校验可以放到抽象基类              |
| 不要过度设计     | 如果只有两个很简单的分支，直接判断可能更清晰                |
| 策略选择要可信   | 不建议完全信任前端传入的策略编码，生产项目应由后端规则决定  |
| 注意金额精度     | 金额计算必须使用 `BigDecimal`，避免使用 `double` 或 `float` |

## 和状态模式的区别

策略模式和状态模式结构很像，但意图不同。

| 模式     | 关注点                       | 典型场景                               |
| -------- | ---------------------------- | -------------------------------------- |
| 策略模式 | 根据外部条件选择一种算法     | 优惠计算、支付渠道、导出格式、登录方式 |
| 状态模式 | 对象内部状态变化后行为也变化 | 订单状态、审批状态、工单状态、任务状态 |

策略模式通常由外部输入或配置决定使用哪种策略。
状态模式通常由对象当前状态决定能执行什么行为。

## 和工厂模式的区别

策略模式经常和工厂模式或注册器一起使用，但两者关注点不同。

| 模式          | 关注点               | 在本示例中的体现                                     |
| ------------- | -------------------- | ---------------------------------------------------- |
| 策略模式      | 定义多种可替换算法   | `OrderDiscountStrategy` 及其实现类                   |
| 工厂 / 注册器 | 根据标识获取具体对象 | `OrderDiscountStrategyExecutor` 内部的 `strategyMap` |

简单来说，策略模式解决“算法如何拆分”，工厂或注册器解决“对象如何获取”。

## 和命令模式的区别

策略模式和命令模式都可以通过编码选择处理类，但语义不同。

| 模式     | 关注点                     | 典型场景                               |
| -------- | -------------------------- | -------------------------------------- |
| 策略模式 | 选择一种算法并返回计算结果 | 优惠计算、运费计算、路由选择、评分算法 |
| 命令模式 | 封装一个业务动作并执行     | 创建订单、取消订单、退款、任务调度     |

如果核心是“同类算法可替换”，优先考虑策略模式。
如果核心是“一个动作被封装、执行、排队或记录”，优先考虑命令模式。

## 小结

策略模式在 Spring Boot 项目中的常见落地方式是：定义统一策略接口，为每种业务算法创建独立策略类，再通过执行器或注册器根据策略编码选择执行。
在订单结算、支付渠道、运费计算、导出格式、登录方式、消息发送等场景中，策略模式可以有效减少 `if-else` 分支，使业务算法更清晰、更容易扩展。
