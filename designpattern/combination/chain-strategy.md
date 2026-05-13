# 责任链模式 + 策略模式

责任链模式和策略模式组合使用时，通常用于处理 **多个处理节点需要按顺序执行，并且某些节点内部还存在多种可替换算法** 的业务场景。

责任链模式负责控制多个处理节点的执行顺序；策略模式负责封装某个节点内部的可变算法。这个组合在 Spring Boot 后端项目中常见于风控校验、支付校验、审批处理、优惠计算、导入校验、消息发送前置处理等场景。

```text
责任链模式：控制多个处理节点的执行顺序
策略模式：封装某个节点内部的可变算法
```

## 适用场景

本示例以“订单风控校验”为业务场景。订单提交前需要经过多个风控节点：

```text
用户风控校验
  -> 订单金额风控校验
      -> 支付方式风控校验
          -> 商品风控校验
```

其中，“支付方式风控校验”节点内部又会根据支付方式选择不同的风控算法：

| 支付方式 | 风控策略         |
| -------- | ---------------- |
| ALIPAY   | 支付宝风控策略   |
| WECHAT   | 微信支付风控策略 |
| BALANCE  | 余额支付风控策略 |

如果只使用责任链，支付方式风控节点内部可能会出现大量 `if else`。如果只使用策略模式，又无法很好地表达多个风控节点之间的顺序关系。

使用“责任链模式 + 策略模式”后，结构变成：

```text
Controller
  -> Service
      -> 订单风控校验链
          -> 用户风控校验节点
          -> 订单金额风控校验节点
          -> 支付方式风控校验节点
              -> 支付宝风控策略
              -> 微信支付风控策略
              -> 余额支付风控策略
          -> 商品风控校验节点
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

    <!-- Hutool：提供字符串、对象、集合、数字、ID 等常用工具 -->
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
src/main/java/io/github/atengk/pattern/combination/chainstrategy
├── chain
│   ├── OrderRiskCheckChain.java
│   ├── OrderRiskCheckHandler.java
│   └── impl
│       ├── AmountRiskCheckHandler.java
│       ├── PaymentRiskCheckHandler.java
│       ├── ProductRiskCheckHandler.java
│       └── UserRiskCheckHandler.java
├── context
│   └── OrderRiskContext.java
├── controller
│   └── OrderRiskCheckController.java
├── dto
│   ├── OrderRiskCheckRequest.java
│   └── OrderRiskCheckResponse.java
├── enums
│   └── PaymentTypeEnum.java
├── factory
│   └── PaymentRiskStrategyFactory.java
├── handler
│   └── GlobalExceptionHandler.java
├── result
│   └── Result.java
├── service
│   ├── OrderRiskCheckService.java
│   └── impl
│       └── OrderRiskCheckServiceImpl.java
└── strategy
    ├── PaymentRiskStrategy.java
    └── impl
        ├── AlipayRiskStrategy.java
        ├── BalanceRiskStrategy.java
        └── WechatRiskStrategy.java
```

## 核心代码

这一部分给出核心实现。重点看两个位置：

```text
OrderRiskCheckChain：责任链，控制多个风控节点的执行顺序
PaymentRiskStrategy：策略接口，封装支付方式风控算法
```

责任链不关心支付方式风控的具体算法，只负责执行 `PaymentRiskCheckHandler` 这个节点。支付方式风控节点内部再通过策略模式选择具体算法。

### 支付方式枚举

支付方式枚举用于统一维护支付方式编码，避免业务代码中散落字符串。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/enums/PaymentTypeEnum.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.enums;

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
     * @return 支付方式枚举
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

### 风控校验请求对象

请求对象用于接收订单风控校验参数。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/dto/OrderRiskCheckRequest.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单风控校验请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderRiskCheckRequest {

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
     * 订单编号
     */
    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    /**
     * 支付方式：ALIPAY、WECHAT、BALANCE
     */
    @NotBlank(message = "支付方式不能为空")
    private String paymentType;

    /**
     * 订单金额
     */
    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于0")
    private BigDecimal orderAmount;

    /**
     * 用户等级
     */
    @NotBlank(message = "用户等级不能为空")
    private String userLevel;

    /**
     * 订单备注
     */
    private String remark;
}
```

### 风控校验响应对象

响应对象用于返回风控校验结果。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/dto/OrderRiskCheckResponse.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 订单风控校验响应
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderRiskCheckResponse {

    /**
     * 风控流水号
     */
    private String riskNo;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 是否通过
     */
    private Boolean passed;

    /**
     * 风控结果
     */
    private String result;

    /**
     * 命中的支付风控策略
     */
    private String paymentRiskStrategy;

    /**
     * 已执行的风控节点
     */
    private List<String> executedHandlers;
}
```

### 风控上下文

上下文对象用于在责任链节点和策略对象之间传递数据。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/context/OrderRiskContext.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.context;

import io.github.atengk.pattern.combination.chainstrategy.dto.OrderRiskCheckRequest;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单风控上下文
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderRiskContext {

    /**
     * 风控流水号
     */
    private String riskNo;

    /**
     * 风控校验请求
     */
    private OrderRiskCheckRequest request;

    /**
     * 命中的支付风控策略
     */
    private String paymentRiskStrategy;

    /**
     * 已执行的风控节点
     */
    @Builder.Default
    private List<String> executedHandlers = new ArrayList<>();

    /**
     * 扩展属性
     */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
}
```

### 责任链处理器接口

责任链处理器接口定义所有风控节点必须实现的行为。`order()` 用于控制节点执行顺序。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/chain/OrderRiskCheckHandler.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.chain;

import io.github.atengk.pattern.combination.chainstrategy.context.OrderRiskContext;

/**
 * 订单风控校验处理器
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderRiskCheckHandler {

    /**
     * 执行顺序，数值越小越先执行
     *
     * @return 执行顺序
     */
    int order();

    /**
     * 处理器名称
     *
     * @return 处理器名称
     */
    String name();

    /**
     * 执行风控校验
     *
     * @param context 订单风控上下文
     */
    void check(OrderRiskContext context);
}
```

### 订单风控校验链

校验链负责收集所有风控处理器，并按照顺序依次执行。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/chain/OrderRiskCheckChain.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.chain;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.pattern.combination.chainstrategy.context.OrderRiskContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 订单风控校验链
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderRiskCheckChain {

    private final List<OrderRiskCheckHandler> handlerList;

    /**
     * 初始化订单风控校验链
     *
     * @param handlerList 风控校验处理器集合
     */
    public OrderRiskCheckChain(List<OrderRiskCheckHandler> handlerList) {
        if (CollUtil.isEmpty(handlerList)) {
            throw new IllegalStateException("订单风控校验处理器不能为空");
        }

        this.handlerList = handlerList.stream()
                .sorted(Comparator.comparing(OrderRiskCheckHandler::order))
                .toList();

        log.info("订单风控校验链初始化完成，处理器数量：{}", this.handlerList.size());
    }

    /**
     * 执行订单风控校验链
     *
     * @param context 订单风控上下文
     */
    public void check(OrderRiskContext context) {
        for (OrderRiskCheckHandler handler : handlerList) {
            log.info("开始执行订单风控节点：{}", handler.name());
            handler.check(context);
            context.getExecutedHandlers().add(handler.name());
            log.info("订单风控节点执行通过：{}", handler.name());
        }
    }
}
```

### 用户风控校验节点

用户风控校验节点用于判断用户是否允许继续下单。示例中用简单规则模拟异常用户。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/chain/impl/UserRiskCheckHandler.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.chain.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.chainstrategy.chain.OrderRiskCheckHandler;
import io.github.atengk.pattern.combination.chainstrategy.context.OrderRiskContext;
import io.github.atengk.pattern.combination.chainstrategy.dto.OrderRiskCheckRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户风控校验节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UserRiskCheckHandler implements OrderRiskCheckHandler {

    /**
     * 执行顺序
     *
     * @return 执行顺序
     */
    @Override
    public int order() {
        return 10;
    }

    /**
     * 处理器名称
     *
     * @return 处理器名称
     */
    @Override
    public String name() {
        return "用户风控校验";
    }

    /**
     * 执行用户风控校验
     *
     * @param context 订单风控上下文
     */
    @Override
    public void check(OrderRiskContext context) {
        OrderRiskCheckRequest request = context.getRequest();
        if (ObjectUtil.isNull(request.getUserId())) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        if (request.getUserId() % 10 == 0) {
            throw new IllegalArgumentException("用户命中风控黑名单");
        }

        if (StrUtil.equalsIgnoreCase(request.getUserLevel(), "DISABLED")) {
            throw new IllegalArgumentException("用户等级异常，不允许下单");
        }

        log.info("用户风控校验通过，用户ID：{}，用户等级：{}",
                request.getUserId(), request.getUserLevel());
    }
}
```

### 订单金额风控校验节点

订单金额风控校验节点用于判断订单金额是否超过系统限制。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/chain/impl/AmountRiskCheckHandler.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.chain.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.combination.chainstrategy.chain.OrderRiskCheckHandler;
import io.github.atengk.pattern.combination.chainstrategy.context.OrderRiskContext;
import io.github.atengk.pattern.combination.chainstrategy.dto.OrderRiskCheckRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 订单金额风控校验节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class AmountRiskCheckHandler implements OrderRiskCheckHandler {

    private static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("50000.00");

    /**
     * 执行顺序
     *
     * @return 执行顺序
     */
    @Override
    public int order() {
        return 20;
    }

    /**
     * 处理器名称
     *
     * @return 处理器名称
     */
    @Override
    public String name() {
        return "订单金额风控校验";
    }

    /**
     * 执行订单金额风控校验
     *
     * @param context 订单风控上下文
     */
    @Override
    public void check(OrderRiskContext context) {
        OrderRiskCheckRequest request = context.getRequest();
        if (ObjectUtil.isNull(request.getOrderAmount())) {
            throw new IllegalArgumentException("订单金额不能为空");
        }

        BigDecimal actualAmount = NumberUtil.round(request.getOrderAmount(), 2);
        if (actualAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("订单金额必须大于0");
        }
        if (actualAmount.compareTo(MAX_ORDER_AMOUNT) > 0) {
            throw new IllegalArgumentException("订单金额超过风控限制");
        }

        context.getAttributes().put("actualAmount", actualAmount);

        log.info("订单金额风控校验通过，订单编号：{}，订单金额：{}",
                request.getOrderNo(), actualAmount);
    }
}
```

### 支付风控策略接口

策略接口定义不同支付方式的风控算法。支付方式风控节点只依赖这个接口，不依赖具体策略实现类。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/strategy/PaymentRiskStrategy.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.strategy;

import io.github.atengk.pattern.combination.chainstrategy.context.OrderRiskContext;
import io.github.atengk.pattern.combination.chainstrategy.enums.PaymentTypeEnum;

/**
 * 支付方式风控策略接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface PaymentRiskStrategy {

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
     * 执行支付方式风控校验
     *
     * @param context 订单风控上下文
     */
    void check(OrderRiskContext context);
}
```

### 支付宝风控策略

支付宝风控策略用于处理支付宝支付的风控规则。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/strategy/impl/AlipayRiskStrategy.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.strategy.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.chainstrategy.context.OrderRiskContext;
import io.github.atengk.pattern.combination.chainstrategy.dto.OrderRiskCheckRequest;
import io.github.atengk.pattern.combination.chainstrategy.enums.PaymentTypeEnum;
import io.github.atengk.pattern.combination.chainstrategy.strategy.PaymentRiskStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付宝支付风控策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class AlipayRiskStrategy implements PaymentRiskStrategy {

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
        return "支付宝支付风控策略";
    }

    /**
     * 执行支付宝支付风控校验
     *
     * @param context 订单风控上下文
     */
    @Override
    public void check(OrderRiskContext context) {
        OrderRiskCheckRequest request = context.getRequest();
        String remark = StrUtil.lowerCase(StrUtil.nullToEmpty(request.getRemark()));

        if (StrUtil.contains(remark, "alipay_risk")) {
            throw new IllegalArgumentException("支付宝支付命中风控规则");
        }

        log.info("支付宝支付风控校验通过，订单编号：{}", request.getOrderNo());
    }
}
```

### 微信支付风控策略

微信支付风控策略用于处理微信支付的风控规则。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/strategy/impl/WechatRiskStrategy.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.strategy.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.chainstrategy.context.OrderRiskContext;
import io.github.atengk.pattern.combination.chainstrategy.dto.OrderRiskCheckRequest;
import io.github.atengk.pattern.combination.chainstrategy.enums.PaymentTypeEnum;
import io.github.atengk.pattern.combination.chainstrategy.strategy.PaymentRiskStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 微信支付风控策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class WechatRiskStrategy implements PaymentRiskStrategy {

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
        return "微信支付风控策略";
    }

    /**
     * 执行微信支付风控校验
     *
     * @param context 订单风控上下文
     */
    @Override
    public void check(OrderRiskContext context) {
        OrderRiskCheckRequest request = context.getRequest();
        String remark = StrUtil.lowerCase(StrUtil.nullToEmpty(request.getRemark()));

        if (StrUtil.contains(remark, "wechat_risk")) {
            throw new IllegalArgumentException("微信支付命中风控规则");
        }

        log.info("微信支付风控校验通过，订单编号：{}", request.getOrderNo());
    }
}
```

### 余额支付风控策略

余额支付风控策略用于处理余额支付的风控规则。示例中限制普通用户余额支付金额。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/strategy/impl/BalanceRiskStrategy.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.strategy.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.chainstrategy.context.OrderRiskContext;
import io.github.atengk.pattern.combination.chainstrategy.dto.OrderRiskCheckRequest;
import io.github.atengk.pattern.combination.chainstrategy.enums.PaymentTypeEnum;
import io.github.atengk.pattern.combination.chainstrategy.strategy.PaymentRiskStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 余额支付风控策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class BalanceRiskStrategy implements PaymentRiskStrategy {

    private static final BigDecimal NORMAL_USER_BALANCE_LIMIT = new BigDecimal("3000.00");

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
        return "余额支付风控策略";
    }

    /**
     * 执行余额支付风控校验
     *
     * @param context 订单风控上下文
     */
    @Override
    public void check(OrderRiskContext context) {
        OrderRiskCheckRequest request = context.getRequest();
        BigDecimal actualAmount = NumberUtil.round(request.getOrderAmount(), 2);

        if (StrUtil.equalsIgnoreCase(request.getUserLevel(), "NORMAL")
                && actualAmount.compareTo(NORMAL_USER_BALANCE_LIMIT) > 0) {
            throw new IllegalArgumentException("普通用户余额支付金额超过限制");
        }

        log.info("余额支付风控校验通过，订单编号：{}，用户等级：{}，订单金额：{}",
                request.getOrderNo(), request.getUserLevel(), actualAmount);
    }
}
```

### 支付风控策略工厂

策略工厂负责根据支付方式获取对应的支付风控策略。这里使用 Spring 自动收集所有策略 Bean。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/factory/PaymentRiskStrategyFactory.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.factory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.combination.chainstrategy.enums.PaymentTypeEnum;
import io.github.atengk.pattern.combination.chainstrategy.strategy.PaymentRiskStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 支付风控策略工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class PaymentRiskStrategyFactory {

    private final Map<PaymentTypeEnum, PaymentRiskStrategy> strategyMap;

    /**
     * 初始化支付风控策略工厂
     *
     * @param strategyList 支付风控策略集合
     */
    public PaymentRiskStrategyFactory(List<PaymentRiskStrategy> strategyList) {
        if (CollUtil.isEmpty(strategyList)) {
            throw new IllegalStateException("支付风控策略不能为空");
        }

        Map<PaymentTypeEnum, PaymentRiskStrategy> tempStrategyMap = new EnumMap<>(PaymentTypeEnum.class);
        for (PaymentRiskStrategy strategy : strategyList) {
            PaymentRiskStrategy oldStrategy = tempStrategyMap.put(strategy.paymentType(), strategy);
            if (ObjectUtil.isNotNull(oldStrategy)) {
                throw new IllegalStateException("支付风控策略重复注册：" + strategy.paymentType().getCode());
            }
        }

        this.strategyMap = Collections.unmodifiableMap(tempStrategyMap);
        log.info("支付风控策略工厂初始化完成，策略数量：{}", this.strategyMap.size());
    }

    /**
     * 根据支付方式获取风控策略
     *
     * @param paymentTypeCode 支付方式编码
     * @return 支付风控策略
     */
    public PaymentRiskStrategy getStrategy(String paymentTypeCode) {
        PaymentTypeEnum paymentType = PaymentTypeEnum.of(paymentTypeCode);
        PaymentRiskStrategy strategy = strategyMap.get(paymentType);
        if (ObjectUtil.isNull(strategy)) {
            throw new IllegalArgumentException("未找到支付风控策略：" + paymentTypeCode);
        }

        log.info("命中支付风控策略，支付方式：{}，策略类：{}",
                paymentType.getCode(), strategy.getClass().getSimpleName());
        return strategy;
    }
}
```

### 支付方式风控校验节点

支付方式风控校验节点是责任链中的一个节点，但它内部使用策略模式选择具体支付风控算法。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/chain/impl/PaymentRiskCheckHandler.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.chain.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.chainstrategy.chain.OrderRiskCheckHandler;
import io.github.atengk.pattern.combination.chainstrategy.context.OrderRiskContext;
import io.github.atengk.pattern.combination.chainstrategy.dto.OrderRiskCheckRequest;
import io.github.atengk.pattern.combination.chainstrategy.factory.PaymentRiskStrategyFactory;
import io.github.atengk.pattern.combination.chainstrategy.strategy.PaymentRiskStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付方式风控校验节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRiskCheckHandler implements OrderRiskCheckHandler {

    private final PaymentRiskStrategyFactory paymentRiskStrategyFactory;

    /**
     * 执行顺序
     *
     * @return 执行顺序
     */
    @Override
    public int order() {
        return 30;
    }

    /**
     * 处理器名称
     *
     * @return 处理器名称
     */
    @Override
    public String name() {
        return "支付方式风控校验";
    }

    /**
     * 执行支付方式风控校验
     *
     * @param context 订单风控上下文
     */
    @Override
    public void check(OrderRiskContext context) {
        OrderRiskCheckRequest request = context.getRequest();
        if (StrUtil.isBlank(request.getPaymentType())) {
            throw new IllegalArgumentException("支付方式不能为空");
        }

        PaymentRiskStrategy strategy = paymentRiskStrategyFactory.getStrategy(request.getPaymentType());
        strategy.check(context);

        context.setPaymentRiskStrategy(strategy.strategyName());

        log.info("支付方式风控校验通过，订单编号：{}，支付方式：{}，策略：{}",
                request.getOrderNo(), request.getPaymentType(), strategy.strategyName());
    }
}
```

### 商品风控校验节点

商品风控校验节点用于判断商品是否命中风控规则。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/chain/impl/ProductRiskCheckHandler.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.chain.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.chainstrategy.chain.OrderRiskCheckHandler;
import io.github.atengk.pattern.combination.chainstrategy.context.OrderRiskContext;
import io.github.atengk.pattern.combination.chainstrategy.dto.OrderRiskCheckRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 商品风控校验节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class ProductRiskCheckHandler implements OrderRiskCheckHandler {

    /**
     * 执行顺序
     *
     * @return 执行顺序
     */
    @Override
    public int order() {
        return 40;
    }

    /**
     * 处理器名称
     *
     * @return 处理器名称
     */
    @Override
    public String name() {
        return "商品风控校验";
    }

    /**
     * 执行商品风控校验
     *
     * @param context 订单风控上下文
     */
    @Override
    public void check(OrderRiskContext context) {
        OrderRiskCheckRequest request = context.getRequest();
        if (ObjectUtil.isNull(request.getProductId())) {
            throw new IllegalArgumentException("商品ID不能为空");
        }

        String remark = StrUtil.lowerCase(StrUtil.nullToEmpty(request.getRemark()));
        if (request.getProductId() == 999999L || StrUtil.contains(remark, "product_risk")) {
            throw new IllegalArgumentException("商品命中风控规则");
        }

        log.info("商品风控校验通过，商品ID：{}", request.getProductId());
    }
}
```

### Service 接口

Service 接口对外暴露订单风控校验能力。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/service/OrderRiskCheckService.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.service;

import io.github.atengk.pattern.combination.chainstrategy.dto.OrderRiskCheckRequest;
import io.github.atengk.pattern.combination.chainstrategy.dto.OrderRiskCheckResponse;

/**
 * 订单风控校验服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderRiskCheckService {

    /**
     * 执行订单风控校验
     *
     * @param request 订单风控校验请求
     * @return 订单风控校验响应
     */
    OrderRiskCheckResponse check(OrderRiskCheckRequest request);
}
```

### Service 实现类

Service 实现类负责构建上下文、调用责任链，并组装响应结果。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/service/impl/OrderRiskCheckServiceImpl.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.service.impl;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.pattern.combination.chainstrategy.chain.OrderRiskCheckChain;
import io.github.atengk.pattern.combination.chainstrategy.context.OrderRiskContext;
import io.github.atengk.pattern.combination.chainstrategy.dto.OrderRiskCheckRequest;
import io.github.atengk.pattern.combination.chainstrategy.dto.OrderRiskCheckResponse;
import io.github.atengk.pattern.combination.chainstrategy.service.OrderRiskCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单风控校验服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRiskCheckServiceImpl implements OrderRiskCheckService {

    private final OrderRiskCheckChain orderRiskCheckChain;

    /**
     * 执行订单风控校验
     *
     * @param request 订单风控校验请求
     * @return 订单风控校验响应
     */
    @Override
    public OrderRiskCheckResponse check(OrderRiskCheckRequest request) {
        OrderRiskContext context = OrderRiskContext.builder()
                .riskNo(IdUtil.fastSimpleUUID())
                .request(request)
                .build();

        orderRiskCheckChain.check(context);

        log.info("订单风控校验全部通过，风控流水号：{}，订单编号：{}",
                context.getRiskNo(), request.getOrderNo());

        return OrderRiskCheckResponse.builder()
                .riskNo(context.getRiskNo())
                .orderNo(request.getOrderNo())
                .passed(true)
                .result("PASS")
                .paymentRiskStrategy(context.getPaymentRiskStrategy())
                .executedHandlers(context.getExecutedHandlers())
                .build();
    }
}
```

### 统一响应对象

统一响应对象用于包装接口返回结果。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/result/Result.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.result;

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

全局异常处理器用于把风控拒绝、参数异常统一转换为接口响应。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.handler;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.chainstrategy.result.Result;
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
        log.warn("风控校验未通过：{}", exception.getMessage());
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

Controller 提供订单风控校验接口。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/controller/OrderRiskCheckController.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.controller;

import io.github.atengk.pattern.combination.chainstrategy.dto.OrderRiskCheckRequest;
import io.github.atengk.pattern.combination.chainstrategy.dto.OrderRiskCheckResponse;
import io.github.atengk.pattern.combination.chainstrategy.result.Result;
import io.github.atengk.pattern.combination.chainstrategy.service.OrderRiskCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单风控校验控制器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order-risk")
public class OrderRiskCheckController {

    private final OrderRiskCheckService orderRiskCheckService;

    /**
     * 执行订单风控校验
     *
     * @param request 订单风控校验请求
     * @return 订单风控校验响应
     */
    @PostMapping("/check")
    public Result<OrderRiskCheckResponse> check(@Valid @RequestBody OrderRiskCheckRequest request) {
        return Result.success(orderRiskCheckService.check(request));
    }
}
```

## 使用方式

启动 Spring Boot 项目后，可以通过订单风控校验接口验证责任链和策略模式是否同时生效。

接口信息如下：

```text
请求地址：POST /api/order-risk/check
Content-Type：application/json
```

支付宝支付风控校验请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-risk/check" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "productId": 2001,
    "orderNo": "OD202605130001",
    "paymentType": "ALIPAY",
    "orderAmount": 199.99,
    "userLevel": "NORMAL",
    "remark": "正常订单"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "riskNo": "7b4f4d72f7f74e9f94e31f4b5f1b0b68",
    "orderNo": "OD202605130001",
    "passed": true,
    "result": "PASS",
    "paymentRiskStrategy": "支付宝支付风控策略",
    "executedHandlers": [
      "用户风控校验",
      "订单金额风控校验",
      "支付方式风控校验",
      "商品风控校验"
    ]
  }
}
```

余额支付风控校验请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-risk/check" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1002,
    "productId": 2002,
    "orderNo": "OD202605130002",
    "paymentType": "BALANCE",
    "orderAmount": 2999.99,
    "userLevel": "NORMAL",
    "remark": "余额支付订单"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "riskNo": "e0df86c936694b338580957b20d307cb",
    "orderNo": "OD202605130002",
    "passed": true,
    "result": "PASS",
    "paymentRiskStrategy": "余额支付风控策略",
    "executedHandlers": [
      "用户风控校验",
      "订单金额风控校验",
      "支付方式风控校验",
      "商品风控校验"
    ]
  }
}
```

余额支付金额超过限制请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-risk/check" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1002,
    "productId": 2002,
    "orderNo": "OD202605130003",
    "paymentType": "BALANCE",
    "orderAmount": 5000.00,
    "userLevel": "NORMAL",
    "remark": "余额支付订单"
  }'
```

响应示例：

```json
{
  "code": 400,
  "message": "普通用户余额支付金额超过限制",
  "data": null
}
```

微信支付命中风控请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-risk/check" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1003,
    "productId": 2003,
    "orderNo": "OD202605130004",
    "paymentType": "WECHAT",
    "orderAmount": 199.99,
    "userLevel": "VIP",
    "remark": "wechat_risk"
  }'
```

响应示例：

```json
{
  "code": 400,
  "message": "微信支付命中风控规则",
  "data": null
}
```

## 新增风控节点

当业务新增一个风控节点时，例如新增“地区风控校验”，只需要新增一个责任链处理器，不需要修改已有节点，也不需要修改 Service。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/chain/impl/RegionRiskCheckHandler.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.chain.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.chainstrategy.chain.OrderRiskCheckHandler;
import io.github.atengk.pattern.combination.chainstrategy.context.OrderRiskContext;
import io.github.atengk.pattern.combination.chainstrategy.dto.OrderRiskCheckRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 地区风控校验节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class RegionRiskCheckHandler implements OrderRiskCheckHandler {

    /**
     * 执行顺序
     *
     * @return 执行顺序
     */
    @Override
    public int order() {
        return 25;
    }

    /**
     * 处理器名称
     *
     * @return 处理器名称
     */
    @Override
    public String name() {
        return "地区风控校验";
    }

    /**
     * 执行地区风控校验
     *
     * @param context 订单风控上下文
     */
    @Override
    public void check(OrderRiskContext context) {
        OrderRiskCheckRequest request = context.getRequest();
        String remark = StrUtil.lowerCase(StrUtil.nullToEmpty(request.getRemark()));

        if (StrUtil.contains(remark, "forbidden_region")) {
            throw new IllegalArgumentException("订单命中地区风控规则");
        }

        log.info("地区风控校验通过，订单编号：{}", request.getOrderNo());
    }
}
```

新增后，执行顺序变为：

```text
用户风控校验：10
订单金额风控校验：20
地区风控校验：25
支付方式风控校验：30
商品风控校验：40
```

## 新增支付策略

当业务新增一种支付方式时，例如新增“银行卡支付”，只需要新增支付方式枚举和策略实现类，不需要修改支付风控节点。

第一步，在 `PaymentTypeEnum` 中新增枚举：

```java
BANK_CARD("BANK_CARD", "银行卡支付");
```

第二步，新增银行卡支付风控策略。

文件位置：`src/main/java/io/github/atengk/pattern/combination/chainstrategy/strategy/impl/BankCardRiskStrategy.java`

```java
package io.github.atengk.pattern.combination.chainstrategy.strategy.impl;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.combination.chainstrategy.context.OrderRiskContext;
import io.github.atengk.pattern.combination.chainstrategy.dto.OrderRiskCheckRequest;
import io.github.atengk.pattern.combination.chainstrategy.enums.PaymentTypeEnum;
import io.github.atengk.pattern.combination.chainstrategy.strategy.PaymentRiskStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 银行卡支付风控策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class BankCardRiskStrategy implements PaymentRiskStrategy {

    private static final BigDecimal BANK_CARD_LIMIT = new BigDecimal("20000.00");

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
        return "银行卡支付风控策略";
    }

    /**
     * 执行银行卡支付风控校验
     *
     * @param context 订单风控上下文
     */
    @Override
    public void check(OrderRiskContext context) {
        OrderRiskCheckRequest request = context.getRequest();
        BigDecimal actualAmount = NumberUtil.round(request.getOrderAmount(), 2);

        if (actualAmount.compareTo(BANK_CARD_LIMIT) > 0) {
            throw new IllegalArgumentException("银行卡支付金额超过单笔限制");
        }

        log.info("银行卡支付风控校验通过，订单编号：{}，订单金额：{}",
                request.getOrderNo(), actualAmount);
    }
}
```

新增完成后，`PaymentRiskStrategyFactory` 会自动收集新的 `BankCardRiskStrategy`。`PaymentRiskCheckHandler` 不需要修改。

## 验证方式

可以从以下几个方面验证组合模式是否生效：

```text
1. 启动项目时，日志应输出订单风控校验链初始化完成
2. 启动项目时，日志应输出支付风控策略工厂初始化完成
3. 请求 ALIPAY 时，应命中 AlipayRiskStrategy
4. 请求 WECHAT 时，应命中 WechatRiskStrategy
5. 请求 BALANCE 时，应命中 BalanceRiskStrategy
6. 某个责任链节点抛出异常后，后续节点不会继续执行
7. 新增风控节点后，不需要修改已有节点
8. 新增支付策略后，不需要修改支付风控校验节点
```

正常请求日志示例：

```text
订单风控校验链初始化完成，处理器数量：4
支付风控策略工厂初始化完成，策略数量：3
开始执行订单风控节点：用户风控校验
用户风控校验通过，用户ID：1001，用户等级：NORMAL
订单风控节点执行通过：用户风控校验
开始执行订单风控节点：订单金额风控校验
订单金额风控校验通过，订单编号：OD202605130001，订单金额：199.99
订单风控节点执行通过：订单金额风控校验
开始执行订单风控节点：支付方式风控校验
命中支付风控策略，支付方式：ALIPAY，策略类：AlipayRiskStrategy
支付宝支付风控校验通过，订单编号：OD202605130001
支付方式风控校验通过，订单编号：OD202605130001，支付方式：ALIPAY，策略：支付宝支付风控策略
订单风控节点执行通过：支付方式风控校验
开始执行订单风控节点：商品风控校验
商品风控校验通过，商品ID：2001
订单风控节点执行通过：商品风控校验
订单风控校验全部通过，风控流水号：7b4f4d72f7f74e9f94e31f4b5f1b0b68，订单编号：OD202605130001
```

## 组合效果

责任链模式和策略模式组合后，各自负责不同变化点：

| 模式       | 职责                       | 在示例中的体现                                               |
| ---------- | -------------------------- | ------------------------------------------------------------ |
| 责任链模式 | 控制多个处理节点的执行顺序 | `OrderRiskCheckChain` 顺序执行用户、金额、支付、商品风控节点 |
| 策略模式   | 替换某个节点内部的算法     | `PaymentRiskStrategy` 根据支付方式执行不同风控规则           |

这种组合适合处理下面两类变化：

```text
第一类变化：处理节点会增减或调整顺序
例如新增地区风控、优惠券风控、设备风控

第二类变化：某个节点内部算法会变化
例如支付方式风控节点内部支持支付宝、微信、余额、银行卡等不同算法
```

相比只使用责任链模式，这个组合可以避免某个节点内部出现大量 `if else`。相比只使用策略模式，这个组合可以清晰表达多个处理节点之间的顺序关系。

## 注意事项

责任链模式和策略模式组合使用时，要明确两者边界：

```text
责任链负责节点顺序
策略模式负责节点内部算法
```

不要把所有策略都拆成责任链节点。责任链节点之间应该存在明确的处理顺序，例如先用户风控、再金额风控、再支付风控、再商品风控。策略之间通常是互斥选择关系，例如一次支付只会命中一种支付方式风控策略。

也不要让某个责任链节点内部继续堆积大量分支判断。如果一个节点内部出现多种算法，并且这些算法会持续扩展，就可以在这个节点内部引入策略模式。

这个组合最适合的判断标准是：**节点顺序归责任链，节点算法归策略**。