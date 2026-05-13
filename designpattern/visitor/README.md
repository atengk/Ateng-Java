# 访问者模式

访问者模式用于把“数据结构”和“作用在数据结构上的操作”分离。
当对象结构相对稳定，但需要频繁新增不同处理逻辑时，可以使用访问者模式。

在 Spring Boot 项目中，访问者模式常用于订单费用处理、报表统计、审计日志、规则检查、表达式节点处理、文件结构扫描、复杂对象导出等场景。

本文以“订单费用明细多维处理”为例。订单由商品明细、优惠券抵扣、运费、积分抵扣等多个明细元素组成。系统需要对这些元素执行不同操作，例如金额计算、审计文本生成、风控检查。使用访问者模式后，订单明细对象只负责接收访问，具体处理逻辑由不同访问者实现。

## 适用场景

访问者模式适合处理“对象结构稳定，但操作经常变化”的场景。

订单结算明细中通常包含多类元素：

| 元素       | 说明             |
| ---------- | ---------------- |
| 商品明细   | 计算商品金额     |
| 优惠券抵扣 | 计算优惠金额     |
| 运费明细   | 计算配送费用     |
| 积分抵扣   | 计算积分抵扣金额 |

同一批订单明细可能需要多种处理动作：

| 访问者         | 职责                             |
| -------------- | -------------------------------- |
| 金额计算访问者 | 计算商品金额、优惠金额、应付金额 |
| 审计文本访问者 | 生成订单费用审计说明             |
| 风控检查访问者 | 检查异常金额、异常数量、异常抵扣 |

如果把这些逻辑都写在订单明细类中，明细类会不断膨胀。
如果全部写在 Service 中，又会出现大量 `instanceof` 判断。访问者模式可以让每类处理逻辑独立扩展。

## 基础配置

本示例基于 Spring Boot 3，使用 Hutool、Lombok 和 Validation。Hutool 用于集合判断、对象判断、金额计算和字符串处理，Validation 用于接口参数基础校验。

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

    <!-- Hutool：提供集合、对象、金额计算、字符串处理等常用工具 -->
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
src/main/java/io/github/atengk/pattern/visitor
├── VisitorApplication.java
├── common
│   ├── ApiResult.java
│   ├── BizException.java
│   └── GlobalExceptionHandler.java
└── order
    ├── controller
    │   └── OrderPreviewController.java
    ├── dto
    │   ├── OrderPreviewRequest.java
    │   └── ProductItemRequest.java
    ├── model
    │   ├── CouponDiscountItem.java
    │   ├── FreightLineItem.java
    │   ├── OrderElement.java
    │   ├── PointDeductionItem.java
    │   └── ProductLineItem.java
    ├── service
    │   ├── OrderPreviewService.java
    │   └── impl
    │       └── OrderPreviewServiceImpl.java
    ├── visitor
    │   ├── AmountCalculateVisitor.java
    │   ├── AuditTextVisitor.java
    │   ├── OrderElementVisitor.java
    │   ├── OrderVisitorFactory.java
    │   └── RiskCheckVisitor.java
    └── vo
        └── OrderPreviewResultVO.java
```

## 核心设计

本示例把访问者模式拆成四个核心角色：

| 角色            | 项目中的类                                                   | 说明                                 |
| --------------- | ------------------------------------------------------------ | ------------------------------------ |
| Element         | `OrderElement`                                               | 订单明细元素接口，定义 `accept` 方法 |
| ConcreteElement | `ProductLineItem`、`CouponDiscountItem` 等                   | 具体订单明细元素                     |
| Visitor         | `OrderElementVisitor`                                        | 访问者接口，定义访问不同元素的方法   |
| ConcreteVisitor | `AmountCalculateVisitor`、`AuditTextVisitor`、`RiskCheckVisitor` | 具体访问者，实现不同处理逻辑         |

执行流程如下：

```text
Controller
  -> OrderPreviewService
    -> 构建订单明细元素集合
      -> ProductLineItem
      -> CouponDiscountItem
      -> FreightLineItem
      -> PointDeductionItem
    -> 创建多个访问者
      -> AmountCalculateVisitor
      -> AuditTextVisitor
      -> RiskCheckVisitor
    -> 每个元素依次接受访问者访问
    -> 汇总预览结果
```

访问者模式的重点是：订单明细元素结构相对稳定，新增处理动作时新增访问者即可；不需要修改订单预览主流程。

## 公共代码

公共响应对象、业务异常和全局异常处理用于统一接口返回。实际项目中可以复用已有基础包。

文件位置：`src/main/java/io/github/atengk/pattern/visitor/common/ApiResult.java`

```java
package io.github.atengk.pattern.visitor.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/visitor/common/BizException.java`

```java
package io.github.atengk.pattern.visitor.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/visitor/common/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.visitor.common;

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

下面给出访问者模式的核心实现。示例中访问者对象是有状态对象，例如金额计算访问者会累计金额，所以每次请求都应该创建新的访问者实例，不能直接把有状态访问者注册成单例 Bean 复用。

文件位置：`src/main/java/io/github/atengk/pattern/visitor/VisitorApplication.java`

```java
package io.github.atengk.pattern.visitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 访问者模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class VisitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(VisitorApplication.class, args);
    }

}
```

## 请求对象和响应对象

请求对象用于接收订单预览参数，包括商品明细、优惠券金额、运费和积分抵扣金额。

文件位置：`src/main/java/io/github/atengk/pattern/visitor/order/dto/ProductItemRequest.java`

```java
package io.github.atengk.pattern.visitor.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品明细请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class ProductItemRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotBlank(message = "商品名称不能为空")
    private String productName;

    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;

    @NotNull(message = "商品单价不能为空")
    @DecimalMin(value = "0.01", message = "商品单价必须大于0")
    private BigDecimal unitPrice;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/visitor/order/dto/OrderPreviewRequest.java`

```java
package io.github.atengk.pattern.visitor.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单预览请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderPreviewRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    private String userLevel;

    @Valid
    @NotEmpty(message = "商品明细不能为空")
    private List<ProductItemRequest> productItems;

    private String couponCode;

    @DecimalMin(value = "0.00", message = "优惠券金额不能小于0")
    private BigDecimal couponAmount;

    @DecimalMin(value = "0.00", message = "运费不能小于0")
    private BigDecimal freightAmount;

    @DecimalMin(value = "0.00", message = "积分抵扣金额不能小于0")
    private BigDecimal pointDeductionAmount;

    private Integer pointCount;

    private String region;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/visitor/order/vo/OrderPreviewResultVO.java`

```java
package io.github.atengk.pattern.visitor.order.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单预览结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderPreviewResultVO {

    private Long userId;

    private BigDecimal productAmount;

    private BigDecimal discountAmount;

    private BigDecimal freightAmount;

    private BigDecimal pointDeductionAmount;

    private BigDecimal payableAmount;

    private Boolean riskPassed;

    private List<String> auditMessages;

    private List<String> riskMessages;

}
```

## 元素接口和具体元素

订单元素接口定义 `accept` 方法，表示当前元素可以接受访问者访问。
每个具体元素只负责把自己交给访问者，不直接处理金额计算、审计、风控等逻辑。

文件位置：`src/main/java/io/github/atengk/pattern/visitor/order/model/OrderElement.java`

```java
package io.github.atengk.pattern.visitor.order.model;

import io.github.atengk.pattern.visitor.order.visitor.OrderElementVisitor;

/**
 * 订单明细元素接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderElement {

    /**
     * 接受访问者访问
     *
     * @param visitor 订单元素访问者
     */
    void accept(OrderElementVisitor visitor);

}
```

商品明细元素保存商品 ID、商品名称、数量和单价。

文件位置：`src/main/java/io/github/atengk/pattern/visitor/order/model/ProductLineItem.java`

```java
package io.github.atengk.pattern.visitor.order.model;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.visitor.order.visitor.OrderElementVisitor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品明细元素
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class ProductLineItem implements OrderElement {

    private Long productId;

    private String productName;

    private Integer quantity;

    private BigDecimal unitPrice;

    /**
     * 接受访问者访问
     *
     * @param visitor 订单元素访问者
     */
    @Override
    public void accept(OrderElementVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * 计算商品行金额
     *
     * @return 商品行金额
     */
    public BigDecimal getLineAmount() {
        return NumberUtil.mul(unitPrice, quantity);
    }

}
```

优惠券抵扣元素保存优惠券编码和优惠金额。

文件位置：`src/main/java/io/github/atengk/pattern/visitor/order/model/CouponDiscountItem.java`

```java
package io.github.atengk.pattern.visitor.order.model;

import io.github.atengk.pattern.visitor.order.visitor.OrderElementVisitor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 优惠券抵扣元素
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class CouponDiscountItem implements OrderElement {

    private String couponCode;

    private BigDecimal discountAmount;

    /**
     * 接受访问者访问
     *
     * @param visitor 订单元素访问者
     */
    @Override
    public void accept(OrderElementVisitor visitor) {
        visitor.visit(this);
    }

}
```

运费元素保存配送地区和运费金额。

文件位置：`src/main/java/io/github/atengk/pattern/visitor/order/model/FreightLineItem.java`

```java
package io.github.atengk.pattern.visitor.order.model;

import io.github.atengk.pattern.visitor.order.visitor.OrderElementVisitor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 运费明细元素
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class FreightLineItem implements OrderElement {

    private String region;

    private BigDecimal freightAmount;

    /**
     * 接受访问者访问
     *
     * @param visitor 订单元素访问者
     */
    @Override
    public void accept(OrderElementVisitor visitor) {
        visitor.visit(this);
    }

}
```

积分抵扣元素保存积分数量和抵扣金额。

文件位置：`src/main/java/io/github/atengk/pattern/visitor/order/model/PointDeductionItem.java`

```java
package io.github.atengk.pattern.visitor.order.model;

import io.github.atengk.pattern.visitor.order.visitor.OrderElementVisitor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 积分抵扣元素
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class PointDeductionItem implements OrderElement {

    private Integer pointCount;

    private BigDecimal deductionAmount;

    /**
     * 接受访问者访问
     *
     * @param visitor 订单元素访问者
     */
    @Override
    public void accept(OrderElementVisitor visitor) {
        visitor.visit(this);
    }

}
```

## 访问者接口

访问者接口为每一种订单元素定义一个访问方法。
新增一种操作时，只需要新增访问者实现类；新增一种元素时，需要扩展访问者接口和所有访问者实现，这是访问者模式的典型取舍。

文件位置：`src/main/java/io/github/atengk/pattern/visitor/order/visitor/OrderElementVisitor.java`

```java
package io.github.atengk.pattern.visitor.order.visitor;

import io.github.atengk.pattern.visitor.order.model.CouponDiscountItem;
import io.github.atengk.pattern.visitor.order.model.FreightLineItem;
import io.github.atengk.pattern.visitor.order.model.PointDeductionItem;
import io.github.atengk.pattern.visitor.order.model.ProductLineItem;

/**
 * 订单元素访问者接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderElementVisitor {

    /**
     * 访问商品明细元素
     *
     * @param item 商品明细元素
     */
    void visit(ProductLineItem item);

    /**
     * 访问优惠券抵扣元素
     *
     * @param item 优惠券抵扣元素
     */
    void visit(CouponDiscountItem item);

    /**
     * 访问运费明细元素
     *
     * @param item 运费明细元素
     */
    void visit(FreightLineItem item);

    /**
     * 访问积分抵扣元素
     *
     * @param item 积分抵扣元素
     */
    void visit(PointDeductionItem item);

}
```

## 金额计算访问者

金额计算访问者负责遍历订单元素并计算商品金额、优惠金额、运费、积分抵扣和应付金额。

文件位置：`src/main/java/io/github/atengk/pattern/visitor/order/visitor/AmountCalculateVisitor.java`

```java
package io.github.atengk.pattern.visitor.order.visitor;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.visitor.order.model.CouponDiscountItem;
import io.github.atengk.pattern.visitor.order.model.FreightLineItem;
import io.github.atengk.pattern.visitor.order.model.PointDeductionItem;
import io.github.atengk.pattern.visitor.order.model.ProductLineItem;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 订单金额计算访问者
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Getter
public class AmountCalculateVisitor implements OrderElementVisitor {

    private static final BigDecimal MIN_PAYABLE_AMOUNT = BigDecimal.valueOf(0.01);

    private BigDecimal productAmount = BigDecimal.ZERO;

    private BigDecimal discountAmount = BigDecimal.ZERO;

    private BigDecimal freightAmount = BigDecimal.ZERO;

    private BigDecimal pointDeductionAmount = BigDecimal.ZERO;

    /**
     * 访问商品明细元素并累计商品金额
     *
     * @param item 商品明细元素
     */
    @Override
    public void visit(ProductLineItem item) {
        BigDecimal lineAmount = item.getLineAmount();
        productAmount = NumberUtil.add(productAmount, lineAmount);

        log.info("访问商品明细计算金额，productId：{}，lineAmount：{}，productAmount：{}",
                item.getProductId(), lineAmount, productAmount);
    }

    /**
     * 访问优惠券抵扣元素并累计优惠金额
     *
     * @param item 优惠券抵扣元素
     */
    @Override
    public void visit(CouponDiscountItem item) {
        discountAmount = NumberUtil.add(discountAmount, item.getDiscountAmount());

        log.info("访问优惠券抵扣计算金额，couponCode：{}，discountAmount：{}",
                item.getCouponCode(), discountAmount);
    }

    /**
     * 访问运费明细元素并累计运费
     *
     * @param item 运费明细元素
     */
    @Override
    public void visit(FreightLineItem item) {
        freightAmount = NumberUtil.add(freightAmount, item.getFreightAmount());

        log.info("访问运费明细计算金额，region：{}，freightAmount：{}",
                item.getRegion(), freightAmount);
    }

    /**
     * 访问积分抵扣元素并累计抵扣金额
     *
     * @param item 积分抵扣元素
     */
    @Override
    public void visit(PointDeductionItem item) {
        pointDeductionAmount = NumberUtil.add(pointDeductionAmount, item.getDeductionAmount());

        log.info("访问积分抵扣计算金额，pointCount：{}，pointDeductionAmount：{}",
                item.getPointCount(), pointDeductionAmount);
    }

    /**
     * 获取应付金额
     *
     * @return 应付金额
     */
    public BigDecimal getPayableAmount() {
        BigDecimal amount = NumberUtil.add(productAmount, freightAmount);
        amount = NumberUtil.sub(amount, discountAmount);
        amount = NumberUtil.sub(amount, pointDeductionAmount);

        if (NumberUtil.isLessOrEqual(amount, BigDecimal.ZERO)) {
            return MIN_PAYABLE_AMOUNT;
        }

        return scaleAmount(amount);
    }

    /**
     * 获取格式化后的商品金额
     *
     * @return 商品金额
     */
    public BigDecimal getProductAmount() {
        return scaleAmount(productAmount);
    }

    /**
     * 获取格式化后的优惠金额
     *
     * @return 优惠金额
     */
    public BigDecimal getDiscountAmount() {
        return scaleAmount(discountAmount);
    }

    /**
     * 获取格式化后的运费金额
     *
     * @return 运费金额
     */
    public BigDecimal getFreightAmount() {
        return scaleAmount(freightAmount);
    }

    /**
     * 获取格式化后的积分抵扣金额
     *
     * @return 积分抵扣金额
     */
    public BigDecimal getPointDeductionAmount() {
        return scaleAmount(pointDeductionAmount);
    }

    /**
     * 金额保留两位小数
     *
     * @param amount 金额
     * @return 格式化金额
     */
    private BigDecimal scaleAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

}
```

## 审计文本访问者

审计文本访问者负责把订单明细转换成人可读的审计说明。它不参与金额计算，只生成文本。

文件位置：`src/main/java/io/github/atengk/pattern/visitor/order/visitor/AuditTextVisitor.java`

```java
package io.github.atengk.pattern.visitor.order.visitor;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.visitor.order.model.CouponDiscountItem;
import io.github.atengk.pattern.visitor.order.model.FreightLineItem;
import io.github.atengk.pattern.visitor.order.model.PointDeductionItem;
import io.github.atengk.pattern.visitor.order.model.ProductLineItem;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单审计文本访问者
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Getter
public class AuditTextVisitor implements OrderElementVisitor {

    private final List<String> auditMessages = new ArrayList<>();

    /**
     * 访问商品明细元素并生成审计文本
     *
     * @param item 商品明细元素
     */
    @Override
    public void visit(ProductLineItem item) {
        String message = StrUtil.format("商品[{}]，数量{}，单价{}，小计{}",
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineAmount());

        auditMessages.add(message);
        log.info("生成商品审计文本：{}", message);
    }

    /**
     * 访问优惠券抵扣元素并生成审计文本
     *
     * @param item 优惠券抵扣元素
     */
    @Override
    public void visit(CouponDiscountItem item) {
        String message = StrUtil.format("使用优惠券[{}]，优惠金额{}",
                item.getCouponCode(),
                item.getDiscountAmount());

        auditMessages.add(message);
        log.info("生成优惠券审计文本：{}", message);
    }

    /**
     * 访问运费明细元素并生成审计文本
     *
     * @param item 运费明细元素
     */
    @Override
    public void visit(FreightLineItem item) {
        String message = StrUtil.format("配送地区[{}]，运费{}",
                item.getRegion(),
                item.getFreightAmount());

        auditMessages.add(message);
        log.info("生成运费审计文本：{}", message);
    }

    /**
     * 访问积分抵扣元素并生成审计文本
     *
     * @param item 积分抵扣元素
     */
    @Override
    public void visit(PointDeductionItem item) {
        String message = StrUtil.format("使用积分{}，抵扣金额{}",
                item.getPointCount(),
                item.getDeductionAmount());

        auditMessages.add(message);
        log.info("生成积分审计文本：{}", message);
    }

}
```

## 风控检查访问者

风控检查访问者负责扫描订单元素，识别异常数量、异常金额和异常抵扣。它和金额计算、审计文本互不影响。

文件位置：`src/main/java/io/github/atengk/pattern/visitor/order/visitor/RiskCheckVisitor.java`

```java
package io.github.atengk.pattern.visitor.order.visitor;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.visitor.order.model.CouponDiscountItem;
import io.github.atengk.pattern.visitor.order.model.FreightLineItem;
import io.github.atengk.pattern.visitor.order.model.PointDeductionItem;
import io.github.atengk.pattern.visitor.order.model.ProductLineItem;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单风控检查访问者
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Getter
public class RiskCheckVisitor implements OrderElementVisitor {

    private final List<String> riskMessages = new ArrayList<>();

    /**
     * 访问商品明细元素并执行风控检查
     *
     * @param item 商品明细元素
     */
    @Override
    public void visit(ProductLineItem item) {
        if (item.getQuantity() > 100) {
            addRiskMessage(StrUtil.format("商品[{}]购买数量异常：{}", item.getProductId(), item.getQuantity()));
        }

        if (NumberUtil.isGreater(item.getLineAmount(), BigDecimal.valueOf(10000))) {
            addRiskMessage(StrUtil.format("商品[{}]单行金额过高：{}", item.getProductId(), item.getLineAmount()));
        }
    }

    /**
     * 访问优惠券抵扣元素并执行风控检查
     *
     * @param item 优惠券抵扣元素
     */
    @Override
    public void visit(CouponDiscountItem item) {
        if (NumberUtil.isGreater(item.getDiscountAmount(), BigDecimal.valueOf(500))) {
            addRiskMessage(StrUtil.format("优惠券[{}]抵扣金额过高：{}",
                    item.getCouponCode(), item.getDiscountAmount()));
        }
    }

    /**
     * 访问运费明细元素并执行风控检查
     *
     * @param item 运费明细元素
     */
    @Override
    public void visit(FreightLineItem item) {
        if (NumberUtil.isGreater(item.getFreightAmount(), BigDecimal.valueOf(200))) {
            addRiskMessage(StrUtil.format("地区[{}]运费异常：{}",
                    item.getRegion(), item.getFreightAmount()));
        }
    }

    /**
     * 访问积分抵扣元素并执行风控检查
     *
     * @param item 积分抵扣元素
     */
    @Override
    public void visit(PointDeductionItem item) {
        if (NumberUtil.isGreater(item.getDeductionAmount(), BigDecimal.valueOf(300))) {
            addRiskMessage(StrUtil.format("积分抵扣金额过高：{}，pointCount：{}",
                    item.getDeductionAmount(), item.getPointCount()));
        }
    }

    /**
     * 判断风控是否通过
     *
     * @return 是否通过
     */
    public boolean isPassed() {
        return riskMessages.isEmpty();
    }

    /**
     * 添加风控提示
     *
     * @param message 风控提示
     */
    private void addRiskMessage(String message) {
        riskMessages.add(message);
        log.warn("订单风控命中：{}", message);
    }

}
```

## 访问者工厂

访问者对象通常会保存本次访问的累计结果，因此不建议直接注册为 Spring 单例 Bean。这里使用工厂按请求创建新的访问者实例。

文件位置：`src/main/java/io/github/atengk/pattern/visitor/order/visitor/OrderVisitorFactory.java`

```java
package io.github.atengk.pattern.visitor.order.visitor;

import org.springframework.stereotype.Component;

/**
 * 订单访问者工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Component
public class OrderVisitorFactory {

    /**
     * 创建金额计算访问者
     *
     * @return 金额计算访问者
     */
    public AmountCalculateVisitor createAmountCalculateVisitor() {
        return new AmountCalculateVisitor();
    }

    /**
     * 创建审计文本访问者
     *
     * @return 审计文本访问者
     */
    public AuditTextVisitor createAuditTextVisitor() {
        return new AuditTextVisitor();
    }

    /**
     * 创建风控检查访问者
     *
     * @return 风控检查访问者
     */
    public RiskCheckVisitor createRiskCheckVisitor() {
        return new RiskCheckVisitor();
    }

}
```

## 业务服务

业务服务负责构建订单元素集合，并让每个元素依次接受多个访问者访问。
可以看到，金额计算、审计文本、风控检查都没有写在订单元素类中，也没有堆在 Service 的 `if-else` 中。

文件位置：`src/main/java/io/github/atengk/pattern/visitor/order/service/OrderPreviewService.java`

```java
package io.github.atengk.pattern.visitor.order.service;

import io.github.atengk.pattern.visitor.order.dto.OrderPreviewRequest;
import io.github.atengk.pattern.visitor.order.vo.OrderPreviewResultVO;

/**
 * 订单预览服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderPreviewService {

    /**
     * 预览订单费用
     *
     * @param request 订单预览请求
     * @return 订单预览结果
     */
    OrderPreviewResultVO preview(OrderPreviewRequest request);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/visitor/order/service/impl/OrderPreviewServiceImpl.java`

```java
package io.github.atengk.pattern.visitor.order.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.visitor.common.BizException;
import io.github.atengk.pattern.visitor.order.dto.OrderPreviewRequest;
import io.github.atengk.pattern.visitor.order.dto.ProductItemRequest;
import io.github.atengk.pattern.visitor.order.model.CouponDiscountItem;
import io.github.atengk.pattern.visitor.order.model.FreightLineItem;
import io.github.atengk.pattern.visitor.order.model.OrderElement;
import io.github.atengk.pattern.visitor.order.model.PointDeductionItem;
import io.github.atengk.pattern.visitor.order.model.ProductLineItem;
import io.github.atengk.pattern.visitor.order.service.OrderPreviewService;
import io.github.atengk.pattern.visitor.order.visitor.AmountCalculateVisitor;
import io.github.atengk.pattern.visitor.order.visitor.AuditTextVisitor;
import io.github.atengk.pattern.visitor.order.visitor.OrderVisitorFactory;
import io.github.atengk.pattern.visitor.order.visitor.RiskCheckVisitor;
import io.github.atengk.pattern.visitor.order.vo.OrderPreviewResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单预览服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPreviewServiceImpl implements OrderPreviewService {

    private final OrderVisitorFactory orderVisitorFactory;

    /**
     * 预览订单费用
     *
     * @param request 订单预览请求
     * @return 订单预览结果
     */
    @Override
    public OrderPreviewResultVO preview(OrderPreviewRequest request) {
        checkRequest(request);

        List<OrderElement> elements = buildOrderElements(request);

        AmountCalculateVisitor amountVisitor = orderVisitorFactory.createAmountCalculateVisitor();
        AuditTextVisitor auditTextVisitor = orderVisitorFactory.createAuditTextVisitor();
        RiskCheckVisitor riskCheckVisitor = orderVisitorFactory.createRiskCheckVisitor();

        for (OrderElement element : elements) {
            element.accept(amountVisitor);
            element.accept(auditTextVisitor);
            element.accept(riskCheckVisitor);
        }

        log.info("订单预览完成，userId：{}，elementCount：{}，payableAmount：{}，riskPassed：{}",
                request.getUserId(), elements.size(), amountVisitor.getPayableAmount(), riskCheckVisitor.isPassed());

        return OrderPreviewResultVO.builder()
                .userId(request.getUserId())
                .productAmount(amountVisitor.getProductAmount())
                .discountAmount(amountVisitor.getDiscountAmount())
                .freightAmount(amountVisitor.getFreightAmount())
                .pointDeductionAmount(amountVisitor.getPointDeductionAmount())
                .payableAmount(amountVisitor.getPayableAmount())
                .riskPassed(riskCheckVisitor.isPassed())
                .auditMessages(auditTextVisitor.getAuditMessages())
                .riskMessages(riskCheckVisitor.getRiskMessages())
                .build();
    }

    /**
     * 校验订单预览请求
     *
     * @param request 订单预览请求
     */
    private void checkRequest(OrderPreviewRequest request) {
        if (request == null) {
            throw new BizException("订单预览请求不能为空");
        }

        if (ObjectUtil.isNull(request.getUserId()) || request.getUserId() <= 0) {
            throw new BizException("用户ID不合法");
        }

        if (CollUtil.isEmpty(request.getProductItems())) {
            throw new BizException("商品明细不能为空");
        }
    }

    /**
     * 构建订单元素集合
     *
     * @param request 订单预览请求
     * @return 订单元素集合
     */
    private List<OrderElement> buildOrderElements(OrderPreviewRequest request) {
        List<OrderElement> elements = new ArrayList<>();

        for (ProductItemRequest productItem : request.getProductItems()) {
            elements.add(ProductLineItem.builder()
                    .productId(productItem.getProductId())
                    .productName(productItem.getProductName())
                    .quantity(productItem.getQuantity())
                    .unitPrice(productItem.getUnitPrice())
                    .build());
        }

        if (NumberUtil.isGreater(defaultAmount(request.getCouponAmount()), BigDecimal.ZERO)) {
            elements.add(CouponDiscountItem.builder()
                    .couponCode(StrUtil.blankToDefault(request.getCouponCode(), "UNKNOWN_COUPON"))
                    .discountAmount(request.getCouponAmount())
                    .build());
        }

        if (NumberUtil.isGreater(defaultAmount(request.getFreightAmount()), BigDecimal.ZERO)) {
            elements.add(FreightLineItem.builder()
                    .region(StrUtil.blankToDefault(request.getRegion(), "UNKNOWN_REGION"))
                    .freightAmount(request.getFreightAmount())
                    .build());
        }

        if (NumberUtil.isGreater(defaultAmount(request.getPointDeductionAmount()), BigDecimal.ZERO)) {
            elements.add(PointDeductionItem.builder()
                    .pointCount(ObjectUtil.defaultIfNull(request.getPointCount(), 0))
                    .deductionAmount(request.getPointDeductionAmount())
                    .build());
        }

        log.info("订单元素构建完成，userId：{}，elementCount：{}", request.getUserId(), elements.size());
        return elements;
    }

    /**
     * 获取默认金额
     *
     * @param amount 金额
     * @return 非空金额
     */
    private BigDecimal defaultAmount(BigDecimal amount) {
        return ObjectUtil.defaultIfNull(amount, BigDecimal.ZERO);
    }

}
```

## 控制器接口

控制器提供订单预览入口。调用方传入订单明细后，服务层通过访问者模式生成金额、审计和风控结果。

文件位置：`src/main/java/io/github/atengk/pattern/visitor/order/controller/OrderPreviewController.java`

```java
package io.github.atengk.pattern.visitor.order.controller;

import io.github.atengk.pattern.visitor.common.ApiResult;
import io.github.atengk.pattern.visitor.order.dto.OrderPreviewRequest;
import io.github.atengk.pattern.visitor.order.service.OrderPreviewService;
import io.github.atengk.pattern.visitor.order.vo.OrderPreviewResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单预览接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/order-previews")
@RequiredArgsConstructor
public class OrderPreviewController {

    private final OrderPreviewService orderPreviewService;

    /**
     * 预览订单费用
     *
     * @param request 订单预览请求
     * @return 订单预览结果
     */
    @PostMapping
    public ApiResult<OrderPreviewResultVO> preview(@Valid @RequestBody OrderPreviewRequest request) {
        return ApiResult.success(orderPreviewService.preview(request));
    }

}
```

## 使用方式

启动项目后，调用订单预览接口即可触发访问者模式。

接口信息：

| 项目         | 内容                                                         |
| ------------ | ------------------------------------------------------------ |
| 请求路径     | `/order-previews`                                            |
| 请求方法     | `POST`                                                       |
| Content-Type | `application/json`                                           |
| 主要流程     | 构建订单元素 → 多个访问者访问元素 → 汇总金额、审计、风控结果 |

正常订单预览请求：

```bash
curl -X POST "http://localhost:8080/order-previews" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "userLevel": "VIP",
    "productItems": [
      {
        "productId": 20001,
        "productName": "机械键盘",
        "quantity": 2,
        "unitPrice": 299.90
      },
      {
        "productId": 20002,
        "productName": "无线鼠标",
        "quantity": 1,
        "unitPrice": 129.90
      }
    ],
    "couponCode": "FULL_500_50",
    "couponAmount": 50.00,
    "freightAmount": 12.00,
    "pointDeductionAmount": 20.00,
    "pointCount": 2000,
    "region": "CN"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 10001,
    "productAmount": 729.70,
    "discountAmount": 50.00,
    "freightAmount": 12.00,
    "pointDeductionAmount": 20.00,
    "payableAmount": 671.70,
    "riskPassed": true,
    "auditMessages": [
      "商品[机械键盘]，数量2，单价299.90，小计599.80",
      "商品[无线鼠标]，数量1，单价129.90，小计129.90",
      "使用优惠券[FULL_500_50]，优惠金额50.00",
      "配送地区[CN]，运费12.00",
      "使用积分2000，抵扣金额20.00"
    ],
    "riskMessages": []
  }
}
```

风控命中请求：

```bash
curl -X POST "http://localhost:8080/order-previews" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10002,
    "productItems": [
      {
        "productId": 30001,
        "productName": "企业服务器",
        "quantity": 2,
        "unitPrice": 8999.00
      }
    ],
    "couponCode": "BIG_COUPON",
    "couponAmount": 800.00,
    "freightAmount": 260.00,
    "pointDeductionAmount": 350.00,
    "pointCount": 35000,
    "region": "REMOTE"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 10002,
    "productAmount": 17998.00,
    "discountAmount": 800.00,
    "freightAmount": 260.00,
    "pointDeductionAmount": 350.00,
    "payableAmount": 17108.00,
    "riskPassed": false,
    "auditMessages": [
      "商品[企业服务器]，数量2，单价8999.00，小计17998.00",
      "使用优惠券[BIG_COUPON]，优惠金额800.00",
      "配送地区[REMOTE]，运费260.00",
      "使用积分35000，抵扣金额350.00"
    ],
    "riskMessages": [
      "商品[30001]单行金额过高：17998.00",
      "优惠券[BIG_COUPON]抵扣金额过高：800.00",
      "地区[REMOTE]运费异常：260.00",
      "积分抵扣金额过高：350.00，pointCount：35000"
    ]
  }
}
```

## 验证方式

正常请求后，可以通过日志观察元素被多个访问者访问的过程：

```text
订单元素构建完成，userId：10001，elementCount：5
访问商品明细计算金额，productId：20001，lineAmount：599.80，productAmount：599.80
生成商品审计文本：商品[机械键盘]，数量2，单价299.90，小计599.80
访问商品明细计算金额，productId：20002，lineAmount：129.90，productAmount：729.70
生成商品审计文本：商品[无线鼠标]，数量1，单价129.90，小计129.90
访问优惠券抵扣计算金额，couponCode：FULL_500_50，discountAmount：50.00
生成优惠券审计文本：使用优惠券[FULL_500_50]，优惠金额50.00
访问运费明细计算金额，region：CN，freightAmount：12.00
生成运费审计文本：配送地区[CN]，运费12.00
访问积分抵扣计算金额，pointCount：2000，pointDeductionAmount：20.00
生成积分审计文本：使用积分2000，抵扣金额20.00
订单预览完成，userId：10001，elementCount：5，payableAmount：671.70，riskPassed：true
```

风控命中时，可以看到风险日志：

```text
订单风控命中：商品[30001]单行金额过高：17998.00
订单风控命中：优惠券[BIG_COUPON]抵扣金额过高：800.00
订单风控命中：地区[REMOTE]运费异常：260.00
订单风控命中：积分抵扣金额过高：350.00，pointCount：35000
```

## 扩展方式

如果后续需要新增一种处理动作，例如“订单明细导出访问者”，只需要新增一个访问者实现类，不需要修改订单元素类。

文件位置：`src/main/java/io/github/atengk/pattern/visitor/order/visitor/ExportTextVisitor.java`

```java
package io.github.atengk.pattern.visitor.order.visitor;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.visitor.order.model.CouponDiscountItem;
import io.github.atengk.pattern.visitor.order.model.FreightLineItem;
import io.github.atengk.pattern.visitor.order.model.PointDeductionItem;
import io.github.atengk.pattern.visitor.order.model.ProductLineItem;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单导出文本访问者
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
public class ExportTextVisitor implements OrderElementVisitor {

    private final List<String> exportRows = new ArrayList<>();

    /**
     * 访问商品明细元素并生成导出行
     *
     * @param item 商品明细元素
     */
    @Override
    public void visit(ProductLineItem item) {
        exportRows.add(StrUtil.format("PRODUCT,{},{},{},{}",
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getLineAmount()));
    }

    /**
     * 访问优惠券抵扣元素并生成导出行
     *
     * @param item 优惠券抵扣元素
     */
    @Override
    public void visit(CouponDiscountItem item) {
        exportRows.add(StrUtil.format("COUPON,{},{}", item.getCouponCode(), item.getDiscountAmount()));
    }

    /**
     * 访问运费明细元素并生成导出行
     *
     * @param item 运费明细元素
     */
    @Override
    public void visit(FreightLineItem item) {
        exportRows.add(StrUtil.format("FREIGHT,{},{}", item.getRegion(), item.getFreightAmount()));
    }

    /**
     * 访问积分抵扣元素并生成导出行
     *
     * @param item 积分抵扣元素
     */
    @Override
    public void visit(PointDeductionItem item) {
        exportRows.add(StrUtil.format("POINT,{},{}", item.getPointCount(), item.getDeductionAmount()));
    }

}
```

然后在业务服务中创建该访问者，并让元素接受访问：

```java
ExportTextVisitor exportTextVisitor = new ExportTextVisitor();

for (OrderElement element : elements) {
    element.accept(exportTextVisitor);
}

List<String> exportRows = exportTextVisitor.getExportRows();
```

这样新增“导出文本”能力时，不需要修改 `ProductLineItem`、`CouponDiscountItem`、`FreightLineItem` 和 `PointDeductionItem`。

## 新增元素的影响

访问者模式适合“元素类型稳定，操作经常新增”的场景。
如果需要新增一个元素，例如“包装费元素”，就需要修改访问者接口和所有访问者实现类。

新增元素示例：

```java
package io.github.atengk.pattern.visitor.order.model;

import io.github.atengk.pattern.visitor.order.visitor.OrderElementVisitor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 包装费元素
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class PackageFeeItem implements OrderElement {

    private String packageType;

    private BigDecimal packageFee;

    /**
     * 接受访问者访问
     *
     * @param visitor 订单元素访问者
     */
    @Override
    public void accept(OrderElementVisitor visitor) {
        visitor.visit(this);
    }

}
```

此时 `OrderElementVisitor` 需要新增方法：

```java
void visit(PackageFeeItem item);
```

同时 `AmountCalculateVisitor`、`AuditTextVisitor`、`RiskCheckVisitor` 都要实现该方法。
这也是访问者模式的主要限制：新增操作容易，新增元素成本较高。

## 优点和注意事项

访问者模式的核心价值是把作用在对象结构上的操作拆出来，避免对象类不断堆积各种不相关逻辑。

| 注意事项           | 说明                                               |
| ------------------ | -------------------------------------------------- |
| 适合元素稳定场景   | 元素类型经常变化时，访问者模式维护成本较高         |
| 适合操作频繁新增   | 新增金额计算、审计、导出、风控等操作时比较方便     |
| 访问者不要复用状态 | 有累计结果的访问者应每次请求新建实例               |
| 元素类保持简单     | 元素类只保存数据和 `accept`，不要塞入大量处理逻辑  |
| 访问者职责要单一   | 一个访问者只做一类处理，例如金额、审计、风控       |
| 注意双分派理解成本 | 访问者模式对团队成员有一定理解门槛，需配合文档说明 |

## 和策略模式的区别

访问者模式和策略模式都能把处理逻辑拆分成独立类，但关注点不同。

| 模式       | 关注点                         | 典型场景                               |
| ---------- | ------------------------------ | -------------------------------------- |
| 访问者模式 | 对一组不同类型元素执行某类操作 | 订单明细统计、AST 节点处理、文件树扫描 |
| 策略模式   | 从多种算法中选择一种执行       | 优惠计算、支付渠道、导出格式、登录方式 |

如果核心问题是“一组元素需要被多种操作处理”，优先考虑访问者模式。
如果核心问题是“同一个输入选择一种算法处理”，优先考虑策略模式。

## 和迭代器模式的区别

访问者模式和迭代器模式经常一起出现，但解决的问题不同。

| 模式       | 关注点                   | 典型场景                     |
| ---------- | ------------------------ | ---------------------------- |
| 访问者模式 | 元素被访问后执行什么操作 | 金额计算、审计、导出、风控   |
| 迭代器模式 | 如何顺序访问一组元素     | 分页遍历、集合遍历、批量处理 |

在本示例中，`for (OrderElement element : elements)` 是普通遍历；如果元素来源很大，也可以结合迭代器模式分批读取元素，再让访问者处理。

## 和模板方法模式的区别

访问者模式和模板方法模式都能组织复杂处理逻辑，但结构不同。

| 模式         | 关注点                             | 典型场景                         |
| ------------ | ---------------------------------- | -------------------------------- |
| 访问者模式   | 不同访问者对同一批元素执行不同操作 | 统计、审计、导出、风控           |
| 模板方法模式 | 父类固定流程，子类实现部分步骤     | 导入流程、导出流程、任务执行流程 |

模板方法模式强调“流程骨架固定”。
访问者模式强调“对象结构稳定，操作可扩展”。

## 小结

访问者模式在 Spring Boot 项目中的常见落地方式是：定义元素接口和访问者接口，让具体元素通过 `accept` 接收访问者，再由不同访问者实现不同业务操作。
在订单费用明细、报表统计、审计文本、风控检查、表达式树处理、文件结构扫描等场景中，访问者模式可以把数据结构和处理逻辑解耦，使新增处理动作更清晰、更容易维护。
