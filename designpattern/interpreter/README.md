# 解释器模式

解释器模式用于定义一种简单语言的语法规则，并为该语言提供解释执行能力。
在 Spring Boot 项目中，解释器模式常用于规则引擎、优惠券规则、权限表达式、检索条件解析、动态过滤条件、告警规则、审批条件判断等场景。

本文以“优惠券使用规则解析”为例，实现一个轻量级业务规则表达式解释器。业务方可以配置规则表达式，系统在运行时解析表达式并判断当前订单是否满足优惠券使用条件。

## 适用场景

解释器模式适合处理“业务规则经常变化，但规则语法相对稳定”的场景。

例如优惠券系统中，常见规则包括：

| 规则         | 示例                   | 说明                     |
| ------------ | ---------------------- | ------------------------ |
| 用户等级规则 | `USER_LEVEL_EQ:VIP`    | 用户等级必须是 VIP       |
| 金额规则     | `AMOUNT_GE:100`        | 订单金额大于等于 100     |
| 商品规则     | `PRODUCT_IN:1001|1002` | 商品 ID 必须在指定范围内 |
| 地区规则     | `REGION_IN:CN|JP`      | 地区必须在指定范围内     |
| 组合规则     | `AND(...)`、`OR(...)`  | 多个规则组合判断         |

如果这些规则都写死在代码里，优惠券规则一变化就需要改代码、重新发布。使用解释器模式后，可以把规则配置成字符串，由系统动态解析并执行。

## 基础配置

本示例基于 Spring Boot 3，使用 Hutool、Lombok 和 Validation。Hutool 用于字符串处理、集合判断、类型转换，Validation 用于接口参数基础校验。

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

    <!-- Hutool：提供字符串、集合、类型转换等常用工具 -->
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
src/main/java/io/github/atengk/pattern/interpreter
├── InterpreterApplication.java
├── common
│   ├── ApiResult.java
│   ├── BizException.java
│   └── GlobalExceptionHandler.java
└── coupon
    ├── controller
    │   └── CouponRuleController.java
    ├── dto
    │   └── CouponCheckRequest.java
    ├── vo
    │   └── CouponCheckResultVO.java
    ├── service
    │   ├── CouponRuleService.java
    │   └── impl
    │       └── CouponRuleServiceImpl.java
    └── expression
        ├── CouponRuleContext.java
        ├── CouponRuleExpression.java
        ├── parser
        │   └── CouponRuleExpressionParser.java
        ├── factory
        │   └── TerminalExpressionFactory.java
        ├── composite
        │   ├── AndExpression.java
        │   └── OrExpression.java
        └── terminal
            ├── UserLevelEqExpression.java
            ├── AmountGeExpression.java
            ├── ProductInExpression.java
            └── RegionInExpression.java
```

## 规则语法设计

本示例定义一套非常轻量的优惠券规则 DSL，不引入第三方规则引擎。

规则表达式支持以下格式：

```text
USER_LEVEL_EQ:VIP
AMOUNT_GE:100
PRODUCT_IN:1001|1002|1003
REGION_IN:CN|JP
AND(USER_LEVEL_EQ:VIP,AMOUNT_GE:100)
OR(USER_LEVEL_EQ:VIP,AMOUNT_GE:500)
AND(USER_LEVEL_EQ:VIP,OR(PRODUCT_IN:1001|1002,AMOUNT_GE:500),REGION_IN:CN|JP)
```

语法说明如下：

| 表达式                 | 类型         | 说明                          |
| ---------------------- | ------------ | ----------------------------- |
| `USER_LEVEL_EQ:VIP`    | 终结表达式   | 判断用户等级是否等于 VIP      |
| `AMOUNT_GE:100`        | 终结表达式   | 判断订单金额是否大于等于 100  |
| `PRODUCT_IN:1001|1002` | 终结表达式   | 判断商品 ID 是否在列表内      |
| `REGION_IN:CN|JP`      | 终结表达式   | 判断地区是否在列表内          |
| `AND(...)`             | 非终结表达式 | 所有子表达式都满足才返回 true |
| `OR(...)`              | 非终结表达式 | 任意子表达式满足即返回 true   |

解释器模式中的角色对应关系如下：

| 角色                  | 项目中的类                      | 说明                     |
| --------------------- | ------------------------------- | ------------------------ |
| AbstractExpression    | `CouponRuleExpression`          | 规则表达式统一接口       |
| TerminalExpression    | `UserLevelEqExpression` 等      | 具体业务规则             |
| NonterminalExpression | `AndExpression`、`OrExpression` | 组合表达式               |
| Context               | `CouponRuleContext`             | 规则执行上下文           |
| Parser                | `CouponRuleExpressionParser`    | 把字符串解析成表达式对象 |

## 公共代码

公共响应对象、业务异常和全局异常处理用于统一接口返回结果。实际项目中可以复用项目已有的基础包。

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/common/ApiResult.java`

```java
package io.github.atengk.pattern.interpreter.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/common/BizException.java`

```java
package io.github.atengk.pattern.interpreter.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/common/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.interpreter.common;

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

下面给出解释器模式的核心代码。示例中的规则表达式来自接口请求，实际项目中通常来自优惠券规则表、配置中心、运营后台或缓存。

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/InterpreterApplication.java`

```java
package io.github.atengk.pattern.interpreter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 解释器模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class InterpreterApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterpreterApplication.class, args);
    }

}
```

## 请求对象和响应对象

请求对象用于承载当前订单信息和待解释的优惠券规则表达式。规则上下文会从该请求对象中构建。

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/coupon/dto/CouponCheckRequest.java`

```java
package io.github.atengk.pattern.interpreter.coupon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 优惠券规则校验请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class CouponCheckRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "用户等级不能为空")
    private String userLevel;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "地区编码不能为空")
    private String region;

    @NotBlank(message = "规则表达式不能为空")
    private String ruleExpression;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/coupon/vo/CouponCheckResultVO.java`

```java
package io.github.atengk.pattern.interpreter.coupon.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 优惠券规则校验结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class CouponCheckResultVO {

    private Boolean passed;

    private String message;

    private String ruleExpression;

}
```

## 解释器上下文

上下文对象保存解释器执行时需要使用的业务数据。所有表达式都只依赖上下文判断结果，不直接依赖 Controller 或 Service。

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/coupon/expression/CouponRuleContext.java`

```java
package io.github.atengk.pattern.interpreter.coupon.expression;

import io.github.atengk.pattern.interpreter.coupon.dto.CouponCheckRequest;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 优惠券规则解释器上下文
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class CouponRuleContext {

    private Long userId;

    private String userLevel;

    private Long productId;

    private BigDecimal amount;

    private String region;

    /**
     * 从请求参数构建规则上下文
     *
     * @param request 优惠券规则校验请求
     * @return 规则上下文
     */
    public static CouponRuleContext fromRequest(CouponCheckRequest request) {
        return CouponRuleContext.builder()
                .userId(request.getUserId())
                .userLevel(request.getUserLevel())
                .productId(request.getProductId())
                .amount(request.getAmount())
                .region(request.getRegion())
                .build();
    }

}
```

## 抽象表达式

抽象表达式是解释器模式的核心接口。每个具体表达式都通过 `interpret` 方法解释当前上下文并返回判断结果。

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/coupon/expression/CouponRuleExpression.java`

```java
package io.github.atengk.pattern.interpreter.coupon.expression;

/**
 * 优惠券规则表达式接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface CouponRuleExpression {

    /**
     * 解释并执行规则表达式
     *
     * @param context 优惠券规则上下文
     * @return 是否满足规则
     */
    boolean interpret(CouponRuleContext context);

}
```

## 终结表达式

终结表达式负责解释最小业务规则，例如用户等级、订单金额、商品范围、地区范围。每个终结表达式只处理一种业务判断。

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/coupon/expression/terminal/UserLevelEqExpression.java`

```java
package io.github.atengk.pattern.interpreter.coupon.expression.terminal;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleContext;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleExpression;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户等级等于表达式
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class UserLevelEqExpression implements CouponRuleExpression {

    private final String expectedUserLevel;

    /**
     * 创建用户等级表达式
     *
     * @param expectedUserLevel 期望用户等级
     */
    public UserLevelEqExpression(String expectedUserLevel) {
        this.expectedUserLevel = StrUtil.trim(expectedUserLevel);
    }

    /**
     * 判断用户等级是否匹配
     *
     * @param context 优惠券规则上下文
     * @return 是否满足规则
     */
    @Override
    public boolean interpret(CouponRuleContext context) {
        boolean passed = StrUtil.equalsIgnoreCase(context.getUserLevel(), expectedUserLevel);
        log.info("解释用户等级规则，actual：{}，expected：{}，passed：{}",
                context.getUserLevel(), expectedUserLevel, passed);
        return passed;
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/coupon/expression/terminal/AmountGeExpression.java`

```java
package io.github.atengk.pattern.interpreter.coupon.expression.terminal;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleContext;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleExpression;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 订单金额大于等于表达式
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class AmountGeExpression implements CouponRuleExpression {

    private final BigDecimal thresholdAmount;

    /**
     * 创建订单金额表达式
     *
     * @param thresholdAmount 门槛金额
     */
    public AmountGeExpression(BigDecimal thresholdAmount) {
        this.thresholdAmount = thresholdAmount;
    }

    /**
     * 判断订单金额是否满足门槛
     *
     * @param context 优惠券规则上下文
     * @return 是否满足规则
     */
    @Override
    public boolean interpret(CouponRuleContext context) {
        BigDecimal actualAmount = context.getAmount();
        boolean passed = ObjectUtil.isNotNull(actualAmount) && actualAmount.compareTo(thresholdAmount) >= 0;

        log.info("解释订单金额规则，actual：{}，threshold：{}，passed：{}",
                actualAmount, thresholdAmount, passed);
        return passed;
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/coupon/expression/terminal/ProductInExpression.java`

```java
package io.github.atengk.pattern.interpreter.coupon.expression.terminal;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleContext;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleExpression;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * 商品范围表达式
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class ProductInExpression implements CouponRuleExpression {

    private final Set<Long> productIds;

    /**
     * 创建商品范围表达式
     *
     * @param productIds 商品ID集合
     */
    public ProductInExpression(Set<Long> productIds) {
        this.productIds = productIds;
    }

    /**
     * 判断商品是否在指定范围内
     *
     * @param context 优惠券规则上下文
     * @return 是否满足规则
     */
    @Override
    public boolean interpret(CouponRuleContext context) {
        boolean passed = CollUtil.isNotEmpty(productIds) && productIds.contains(context.getProductId());

        log.info("解释商品范围规则，productId：{}，range：{}，passed：{}",
                context.getProductId(), productIds, passed);
        return passed;
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/coupon/expression/terminal/RegionInExpression.java`

```java
package io.github.atengk.pattern.interpreter.coupon.expression.terminal;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleContext;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleExpression;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * 地区范围表达式
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class RegionInExpression implements CouponRuleExpression {

    private final Set<String> regions;

    /**
     * 创建地区范围表达式
     *
     * @param regions 地区编码集合
     */
    public RegionInExpression(Set<String> regions) {
        this.regions = regions;
    }

    /**
     * 判断地区是否在指定范围内
     *
     * @param context 优惠券规则上下文
     * @return 是否满足规则
     */
    @Override
    public boolean interpret(CouponRuleContext context) {
        String actualRegion = StrUtil.upperCase(context.getRegion());
        boolean passed = CollUtil.isNotEmpty(regions) && regions.contains(actualRegion);

        log.info("解释地区范围规则，region：{}，range：{}，passed：{}",
                actualRegion, regions, passed);
        return passed;
    }

}
```

## 非终结表达式

非终结表达式用于组合多个子表达式。本示例实现 `AND` 和 `OR` 两种组合规则。

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/coupon/expression/composite/AndExpression.java`

```java
package io.github.atengk.pattern.interpreter.coupon.expression.composite;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleContext;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleExpression;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * AND 组合表达式
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class AndExpression implements CouponRuleExpression {

    private final List<CouponRuleExpression> expressions;

    /**
     * 创建 AND 表达式
     *
     * @param expressions 子表达式集合
     */
    public AndExpression(List<CouponRuleExpression> expressions) {
        this.expressions = expressions;
    }

    /**
     * 判断所有子表达式是否全部满足
     *
     * @param context 优惠券规则上下文
     * @return 是否满足规则
     */
    @Override
    public boolean interpret(CouponRuleContext context) {
        if (CollUtil.isEmpty(expressions)) {
            log.warn("AND 表达式没有子表达式");
            return false;
        }

        for (CouponRuleExpression expression : expressions) {
            if (!expression.interpret(context)) {
                log.info("AND 表达式解释结果：false");
                return false;
            }
        }

        log.info("AND 表达式解释结果：true");
        return true;
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/coupon/expression/composite/OrExpression.java`

```java
package io.github.atengk.pattern.interpreter.coupon.expression.composite;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleContext;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleExpression;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * OR 组合表达式
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class OrExpression implements CouponRuleExpression {

    private final List<CouponRuleExpression> expressions;

    /**
     * 创建 OR 表达式
     *
     * @param expressions 子表达式集合
     */
    public OrExpression(List<CouponRuleExpression> expressions) {
        this.expressions = expressions;
    }

    /**
     * 判断是否存在任意子表达式满足
     *
     * @param context 优惠券规则上下文
     * @return 是否满足规则
     */
    @Override
    public boolean interpret(CouponRuleContext context) {
        if (CollUtil.isEmpty(expressions)) {
            log.warn("OR 表达式没有子表达式");
            return false;
        }

        for (CouponRuleExpression expression : expressions) {
            if (expression.interpret(context)) {
                log.info("OR 表达式解释结果：true");
                return true;
            }
        }

        log.info("OR 表达式解释结果：false");
        return false;
    }

}
```

## 表达式工厂

终结表达式工厂负责把最小规则字符串转换为具体表达式对象。这里集中维护规则编码，避免解析器中堆积大量业务判断。

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/coupon/expression/factory/TerminalExpressionFactory.java`

```java
package io.github.atengk.pattern.interpreter.coupon.expression.factory;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.interpreter.common.BizException;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleExpression;
import io.github.atengk.pattern.interpreter.coupon.expression.terminal.AmountGeExpression;
import io.github.atengk.pattern.interpreter.coupon.expression.terminal.ProductInExpression;
import io.github.atengk.pattern.interpreter.coupon.expression.terminal.RegionInExpression;
import io.github.atengk.pattern.interpreter.coupon.expression.terminal.UserLevelEqExpression;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 终结表达式工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Component
public class TerminalExpressionFactory {

    private static final String USER_LEVEL_EQ = "USER_LEVEL_EQ";

    private static final String AMOUNT_GE = "AMOUNT_GE";

    private static final String PRODUCT_IN = "PRODUCT_IN";

    private static final String REGION_IN = "REGION_IN";

    /**
     * 创建终结表达式
     *
     * @param expressionText 表达式文本
     * @return 优惠券规则表达式
     */
    public CouponRuleExpression create(String expressionText) {
        int splitIndex = expressionText.indexOf(":");
        if (splitIndex <= 0 || splitIndex == expressionText.length() - 1) {
            throw new BizException("规则表达式格式错误：" + expressionText);
        }

        String ruleCode = StrUtil.upperCase(StrUtil.trim(expressionText.substring(0, splitIndex)));
        String ruleValue = StrUtil.trim(expressionText.substring(splitIndex + 1));

        return switch (ruleCode) {
            case USER_LEVEL_EQ -> new UserLevelEqExpression(ruleValue);
            case AMOUNT_GE -> new AmountGeExpression(parseAmount(ruleValue));
            case PRODUCT_IN -> new ProductInExpression(parseProductIds(ruleValue));
            case REGION_IN -> new RegionInExpression(parseRegions(ruleValue));
            default -> throw new BizException("不支持的规则表达式：" + ruleCode);
        };
    }

    /**
     * 解析金额
     *
     * @param ruleValue 规则值
     * @return 金额
     */
    private BigDecimal parseAmount(String ruleValue) {
        try {
            return new BigDecimal(ruleValue);
        } catch (NumberFormatException exception) {
            throw new BizException("金额规则格式错误：" + ruleValue);
        }
    }

    /**
     * 解析商品ID集合
     *
     * @param ruleValue 规则值
     * @return 商品ID集合
     */
    private Set<Long> parseProductIds(String ruleValue) {
        return StrUtil.split(ruleValue, "|")
                .stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .map(Convert::toLong)
                .collect(Collectors.toSet());
    }

    /**
     * 解析地区编码集合
     *
     * @param ruleValue 规则值
     * @return 地区编码集合
     */
    private Set<String> parseRegions(String ruleValue) {
        return StrUtil.split(ruleValue, "|")
                .stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .map(StrUtil::upperCase)
                .collect(Collectors.toSet());
    }

}
```

## 表达式解析器

解析器负责把完整规则字符串解析成表达式树。它识别 `AND(...)`、`OR(...)` 这类组合表达式，也会把普通规则交给终结表达式工厂处理。

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/coupon/expression/parser/CouponRuleExpressionParser.java`

```java
package io.github.atengk.pattern.interpreter.coupon.expression.parser;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.interpreter.common.BizException;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleExpression;
import io.github.atengk.pattern.interpreter.coupon.expression.composite.AndExpression;
import io.github.atengk.pattern.interpreter.coupon.expression.composite.OrExpression;
import io.github.atengk.pattern.interpreter.coupon.expression.factory.TerminalExpressionFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 优惠券规则表达式解析器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Component
@RequiredArgsConstructor
public class CouponRuleExpressionParser {

    private static final String AND_FUNCTION = "AND";

    private static final String OR_FUNCTION = "OR";

    private final TerminalExpressionFactory terminalExpressionFactory;

    /**
     * 解析规则表达式
     *
     * @param expressionText 表达式文本
     * @return 优惠券规则表达式
     */
    public CouponRuleExpression parse(String expressionText) {
        if (StrUtil.isBlank(expressionText)) {
            throw new BizException("规则表达式不能为空");
        }

        String expression = StrUtil.trim(expressionText);
        if (isFunctionExpression(expression, AND_FUNCTION)) {
            List<CouponRuleExpression> children = parseChildren(extractFunctionBody(expression));
            return new AndExpression(children);
        }

        if (isFunctionExpression(expression, OR_FUNCTION)) {
            List<CouponRuleExpression> children = parseChildren(extractFunctionBody(expression));
            return new OrExpression(children);
        }

        return terminalExpressionFactory.create(expression);
    }

    /**
     * 判断是否为指定函数表达式
     *
     * @param expression 表达式文本
     * @param functionName 函数名称
     * @return 是否匹配
     */
    private boolean isFunctionExpression(String expression, String functionName) {
        String prefix = functionName + "(";
        return StrUtil.startWithIgnoreCase(expression, prefix) && expression.endsWith(")");
    }

    /**
     * 提取函数体
     *
     * @param expression 表达式文本
     * @return 函数体内容
     */
    private String extractFunctionBody(String expression) {
        int startIndex = expression.indexOf("(");
        return expression.substring(startIndex + 1, expression.length() - 1);
    }

    /**
     * 解析子表达式集合
     *
     * @param body 函数体内容
     * @return 子表达式集合
     */
    private List<CouponRuleExpression> parseChildren(String body) {
        List<String> childTexts = splitTopLevel(body);
        if (CollUtil.isEmpty(childTexts)) {
            throw new BizException("组合表达式不能为空");
        }

        return childTexts.stream()
                .map(this::parse)
                .toList();
    }

    /**
     * 按顶层逗号拆分表达式
     *
     * @param body 函数体内容
     * @return 子表达式文本集合
     */
    private List<String> splitTopLevel(String body) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenthesisLevel = 0;

        for (int index = 0; index < body.length(); index++) {
            char currentChar = body.charAt(index);

            if (currentChar == '(') {
                parenthesisLevel++;
                current.append(currentChar);
                continue;
            }

            if (currentChar == ')') {
                parenthesisLevel--;
                if (parenthesisLevel < 0) {
                    throw new BizException("规则表达式括号不匹配");
                }
                current.append(currentChar);
                continue;
            }

            if (currentChar == ',' && parenthesisLevel == 0) {
                addExpressionText(result, current);
                current.setLength(0);
                continue;
            }

            current.append(currentChar);
        }

        if (parenthesisLevel != 0) {
            throw new BizException("规则表达式括号不匹配");
        }

        addExpressionText(result, current);
        return result;
    }

    /**
     * 添加表达式文本
     *
     * @param result 结果集合
     * @param current 当前表达式文本
     */
    private void addExpressionText(List<String> result, StringBuilder current) {
        String expressionText = StrUtil.trim(current.toString());
        if (StrUtil.isNotBlank(expressionText)) {
            result.add(expressionText);
        }
    }

}
```

## 业务服务

业务服务负责把请求转换为上下文，调用解析器得到表达式对象，然后执行解释逻辑。Controller 不直接接触解释器细节。

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/coupon/service/CouponRuleService.java`

```java
package io.github.atengk.pattern.interpreter.coupon.service;

import io.github.atengk.pattern.interpreter.coupon.dto.CouponCheckRequest;
import io.github.atengk.pattern.interpreter.coupon.vo.CouponCheckResultVO;

/**
 * 优惠券规则服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface CouponRuleService {

    /**
     * 校验优惠券规则
     *
     * @param request 优惠券规则校验请求
     * @return 校验结果
     */
    CouponCheckResultVO check(CouponCheckRequest request);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/coupon/service/impl/CouponRuleServiceImpl.java`

```java
package io.github.atengk.pattern.interpreter.coupon.service.impl;

import io.github.atengk.pattern.interpreter.coupon.dto.CouponCheckRequest;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleContext;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleExpression;
import io.github.atengk.pattern.interpreter.coupon.expression.parser.CouponRuleExpressionParser;
import io.github.atengk.pattern.interpreter.coupon.service.CouponRuleService;
import io.github.atengk.pattern.interpreter.coupon.vo.CouponCheckResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 优惠券规则服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRuleServiceImpl implements CouponRuleService {

    private final CouponRuleExpressionParser couponRuleExpressionParser;

    /**
     * 校验优惠券规则
     *
     * @param request 优惠券规则校验请求
     * @return 校验结果
     */
    @Override
    public CouponCheckResultVO check(CouponCheckRequest request) {
        CouponRuleContext context = CouponRuleContext.fromRequest(request);
        CouponRuleExpression expression = couponRuleExpressionParser.parse(request.getRuleExpression());

        boolean passed = expression.interpret(context);
        log.info("优惠券规则校验完成，userId：{}，productId：{}，passed：{}",
                request.getUserId(), request.getProductId(), passed);

        return CouponCheckResultVO.builder()
                .passed(passed)
                .message(passed ? "满足优惠券使用条件" : "不满足优惠券使用条件")
                .ruleExpression(request.getRuleExpression())
                .build();
    }

}
```

## 控制器接口

控制器提供优惠券规则校验入口。调用方提交订单信息和规则表达式，接口返回是否满足规则。

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/coupon/controller/CouponRuleController.java`

```java
package io.github.atengk.pattern.interpreter.coupon.controller;

import io.github.atengk.pattern.interpreter.common.ApiResult;
import io.github.atengk.pattern.interpreter.coupon.dto.CouponCheckRequest;
import io.github.atengk.pattern.interpreter.coupon.service.CouponRuleService;
import io.github.atengk.pattern.interpreter.coupon.vo.CouponCheckResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 优惠券规则接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/coupon-rules")
@RequiredArgsConstructor
public class CouponRuleController {

    private final CouponRuleService couponRuleService;

    /**
     * 校验优惠券规则
     *
     * @param request 优惠券规则校验请求
     * @return 校验结果
     */
    @PostMapping("/check")
    public ApiResult<CouponCheckResultVO> check(@Valid @RequestBody CouponCheckRequest request) {
        return ApiResult.success(couponRuleService.check(request));
    }

}
```

## 使用方式

启动项目后，通过 `/coupon-rules/check` 接口提交订单信息和规则表达式即可。

接口信息：

| 项目         | 内容                  |
| ------------ | --------------------- |
| 请求路径     | `/coupon-rules/check` |
| 请求方法     | `POST`                |
| Content-Type | `application/json`    |
| 核心字段     | `ruleExpression`      |

单个规则校验：

```bash
curl -X POST "http://localhost:8080/coupon-rules/check" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "userLevel": "VIP",
    "productId": 1001,
    "amount": 199.90,
    "region": "CN",
    "ruleExpression": "USER_LEVEL_EQ:VIP"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "passed": true,
    "message": "满足优惠券使用条件",
    "ruleExpression": "USER_LEVEL_EQ:VIP"
  }
}
```

组合规则校验：

```bash
curl -X POST "http://localhost:8080/coupon-rules/check" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "userLevel": "VIP",
    "productId": 1001,
    "amount": 199.90,
    "region": "CN",
    "ruleExpression": "AND(USER_LEVEL_EQ:VIP,AMOUNT_GE:100,PRODUCT_IN:1001|1002,REGION_IN:CN|JP)"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "passed": true,
    "message": "满足优惠券使用条件",
    "ruleExpression": "AND(USER_LEVEL_EQ:VIP,AMOUNT_GE:100,PRODUCT_IN:1001|1002,REGION_IN:CN|JP)"
  }
}
```

嵌套规则校验：

```bash
curl -X POST "http://localhost:8080/coupon-rules/check" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10002,
    "userLevel": "NORMAL",
    "productId": 2001,
    "amount": 699.00,
    "region": "JP",
    "ruleExpression": "AND(OR(USER_LEVEL_EQ:VIP,AMOUNT_GE:500),REGION_IN:CN|JP)"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "passed": true,
    "message": "满足优惠券使用条件",
    "ruleExpression": "AND(OR(USER_LEVEL_EQ:VIP,AMOUNT_GE:500),REGION_IN:CN|JP)"
  }
}
```

不满足规则示例：

```bash
curl -X POST "http://localhost:8080/coupon-rules/check" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10003,
    "userLevel": "NORMAL",
    "productId": 3001,
    "amount": 50.00,
    "region": "US",
    "ruleExpression": "AND(USER_LEVEL_EQ:VIP,AMOUNT_GE:100,REGION_IN:CN|JP)"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "passed": false,
    "message": "不满足优惠券使用条件",
    "ruleExpression": "AND(USER_LEVEL_EQ:VIP,AMOUNT_GE:100,REGION_IN:CN|JP)"
  }
}
```

## 扩展方式

新增规则时，只需要新增一个终结表达式，并在 `TerminalExpressionFactory` 中增加映射。例如新增“用户 ID 在白名单中”规则：

```text
USER_IN:10001|10002|10003
```

新增表达式类。

文件位置：`src/main/java/io/github/atengk/pattern/interpreter/coupon/expression/terminal/UserInExpression.java`

```java
package io.github.atengk.pattern.interpreter.coupon.expression.terminal;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleContext;
import io.github.atengk.pattern.interpreter.coupon.expression.CouponRuleExpression;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * 用户白名单表达式
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class UserInExpression implements CouponRuleExpression {

    private final Set<Long> userIds;

    /**
     * 创建用户白名单表达式
     *
     * @param userIds 用户ID集合
     */
    public UserInExpression(Set<Long> userIds) {
        this.userIds = userIds;
    }

    /**
     * 判断用户是否在白名单内
     *
     * @param context 优惠券规则上下文
     * @return 是否满足规则
     */
    @Override
    public boolean interpret(CouponRuleContext context) {
        boolean passed = CollUtil.isNotEmpty(userIds) && userIds.contains(context.getUserId());
        log.info("解释用户白名单规则，userId：{}，range：{}，passed：{}",
                context.getUserId(), userIds, passed);
        return passed;
    }

}
```

然后在 `TerminalExpressionFactory` 中增加规则编码和分支：

```java
private static final String USER_IN = "USER_IN";
case USER_IN -> new UserInExpression(parseUserIds(ruleValue));
private Set<Long> parseUserIds(String ruleValue) {
    return StrUtil.split(ruleValue, "|")
            .stream()
            .map(StrUtil::trim)
            .filter(StrUtil::isNotBlank)
            .map(Convert::toLong)
            .collect(Collectors.toSet());
}
```

扩展后即可使用：

```text
AND(USER_IN:10001|10002,AMOUNT_GE:100)
```

## 验证方式

正常请求时，可以通过日志看到表达式逐层解释过程：

```text
解释用户等级规则，actual：VIP，expected：VIP，passed：true
解释订单金额规则，actual：199.90，threshold：100，passed：true
解释商品范围规则，productId：1001，range：[1001, 1002]，passed：true
解释地区范围规则，region：CN，range：[CN, JP]，passed：true
AND 表达式解释结果：true
优惠券规则校验完成，userId：10001，productId：1001，passed：true
```

如果表达式格式错误，例如：

```text
AND(USER_LEVEL_EQ:VIP,AMOUNT_GE:100
```

接口会返回：

```json
{
  "code": 500,
  "message": "规则表达式括号不匹配",
  "data": null
}
```

如果传入不支持的规则：

```text
ORDER_COUNT_GE:3
```

接口会返回：

```json
{
  "code": 500,
  "message": "不支持的规则表达式：ORDER_COUNT_GE",
  "data": null
}
```

## 优点和注意事项

解释器模式的主要价值是把可变业务规则从代码中抽离出来，用一套稳定语法描述业务规则，再由解释器在运行时解析执行。

| 注意事项         | 说明                                                 |
| ---------------- | ---------------------------------------------------- |
| 语法不要过度复杂 | 如果 DSL 越来越复杂，应考虑成熟规则引擎或表达式引擎  |
| 表达式需要校验   | 运营后台保存规则前，应先调用解析器校验格式           |
| 注意执行性能     | 高频场景可以缓存解析后的表达式对象，避免每次重新解析 |
| 注意规则可读性   | 规则表达式要能被研发和运营理解，不要设计得过于隐晦   |
| 注意安全边界     | 不要直接执行用户输入的脚本语言，避免安全风险         |
| 适合规则稳定场景 | 解释器模式适合语法稳定、规则组合频繁变化的业务       |

## 和策略模式的区别

解释器模式和策略模式都可以处理业务规则，但关注点不同。

| 模式       | 关注点                             | 典型场景                                     |
| ---------- | ---------------------------------- | -------------------------------------------- |
| 解释器模式 | 解析并执行一套规则语言             | 优惠券规则、权限表达式、查询条件、告警表达式 |
| 策略模式   | 根据类型选择一种算法或业务处理方式 | 支付渠道、计价方式、发货方式、登录方式       |

如果业务规则需要通过字符串、配置、表达式动态组合，优先考虑解释器模式。
如果只是根据类型选择固定处理逻辑，优先考虑策略模式。

## 小结

解释器模式在 Spring Boot 项目中的常见落地方式是：定义规则表达式接口，提供终结表达式和组合表达式，再通过解析器把配置字符串转换成表达式对象执行。
在优惠券、权限、检索、告警和动态过滤等场景中，解释器模式可以把规则从代码中抽离出来，提高业务规则的可配置性和扩展性。
