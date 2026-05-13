# 外观模式

外观模式属于结构型模式，核心作用是为复杂子系统提供统一入口，隐藏内部多个服务、组件或接口的调用细节。在当前设计模式文档体系中，外观模式位于结构型模式分类下，适合服务聚合、接口编排、复杂流程封装、第三方系统统一调用等 Spring Boot 项目场景。

本文以 **JDK21 + Spring Boot 3** 后端项目为背景，通过“订单下单结算流程”的示例，说明外观模式在真实项目中的落地方式。

## 基础配置

本示例模拟一个电商下单接口。一次下单看起来只是一个 API 调用，但内部通常会涉及多个子系统：

```text
用户校验
库存校验
优惠券计算
订单创建
支付流水创建
物流单预创建
```

如果 Controller 或上层业务直接依赖这些子系统，代码会变成这样：

```java
userAccountService.checkUser(userId);
inventoryService.lockStock(skuId, quantity);
couponService.calculate(...);
orderService.createOrder(...);
paymentService.createPayment(...);
logisticsService.createDelivery(...);
```

这种写法会让调用方直接知道太多内部细节，后续流程调整、子系统替换、异常补偿都会影响调用方。

外观模式的处理方式是：提供一个统一的 `OrderCheckoutFacade`，调用方只调用“提交订单”这一个入口，复杂流程由外观类内部完成编排。

本示例需要以下依赖。

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于校验下单请求参数 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：用于字符串、金额、ID、集合等常用工具处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok：减少 DTO、VO、构造器等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 测试依赖：用于单元测试和接口测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

示例项目配置如下。

```yaml
server:
  port: 8080 # 示例服务端口

checkout:
  payment-timeout-minutes: 30 # 支付超时时间，单位分钟
```

本示例的核心文件结构如下。

```text
src/main/java/io/github/atengk/designpattern/facade
├── FacadeApplication.java
├── config
│   └── CheckoutProperties.java
├── controller
│   └── OrderCheckoutController.java
├── dto
│   └── OrderCheckoutRequest.java
├── facade
│   ├── OrderCheckoutFacade.java
│   └── OrderCheckoutFacadeImpl.java
├── service
│   ├── UserAccountService.java
│   ├── InventoryService.java
│   ├── CouponService.java
│   ├── OrderService.java
│   ├── PaymentService.java
│   └── LogisticsService.java
├── vo
│   ├── ApiResult.java
│   ├── CouponCalculateResultVO.java
│   ├── DeliveryPrepareResultVO.java
│   ├── OrderCheckoutResultVO.java
│   ├── OrderCreateResultVO.java
│   ├── PaymentCreateResultVO.java
│   └── ProductStockVO.java
└── web
    └── GlobalExceptionHandler.java
```

## 模式设计

外观模式不是把所有业务逻辑都塞进一个大类，而是为复杂子系统提供一个稳定、简洁的调用入口。

在本示例中，Controller 不再直接编排用户、库存、优惠券、订单、支付、物流等服务，而是只依赖 `OrderCheckoutFacade`。

| 角色     | 示例类                    | 说明                           |
| -------- | ------------------------- | ------------------------------ |
| 外观接口 | `OrderCheckoutFacade`     | 对外提供统一下单入口           |
| 外观实现 | `OrderCheckoutFacadeImpl` | 负责编排多个子系统             |
| 子系统   | `UserAccountService`      | 校验用户状态                   |
| 子系统   | `InventoryService`        | 查询商品和锁定库存             |
| 子系统   | `CouponService`           | 计算优惠金额                   |
| 子系统   | `OrderService`            | 创建订单                       |
| 子系统   | `PaymentService`          | 创建支付流水                   |
| 子系统   | `LogisticsService`        | 预创建物流单                   |
| 调用方   | `OrderCheckoutController` | 只调用外观接口，不关心内部流程 |

核心流程如下。

```text
Controller
    ↓
OrderCheckoutFacade.submitOrder()
    ↓
校验用户
    ↓
查询商品与库存
    ↓
锁定库存
    ↓
计算优惠
    ↓
创建订单
    ↓
创建支付流水
    ↓
预创建物流单
    ↓
返回统一下单结果
```

外观模式的关键价值是降低调用方复杂度，但不应该破坏子系统自身边界。用户、库存、优惠券、订单、支付、物流仍然保持独立服务。

## 核心代码

下面给出外观模式在 Spring Boot 项目中的关键实现。示例使用内存数据和模拟逻辑，真实项目中可以把子系统替换为数据库、RPC、MQ 或第三方 API。

项目启动类负责启动 Spring Boot 应用，并开启配置属性扫描。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/FacadeApplication.java`

```java
package io.github.atengk.designpattern.facade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 外观模式示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@ConfigurationPropertiesScan
@SpringBootApplication
public class FacadeApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FacadeApplication.class, args);
    }
}
```

下单配置类用于控制下单流程中的通用参数。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/config/CheckoutProperties.java`

```java
package io.github.atengk.designpattern.facade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 下单结算配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@ConfigurationProperties(prefix = "checkout")
public class CheckoutProperties {

    /**
     * 支付超时时间，单位分钟
     */
    private Integer paymentTimeoutMinutes = 30;
}
```

下单请求 DTO 用于承接外部提交订单参数。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/dto/OrderCheckoutRequest.java`

```java
package io.github.atengk.designpattern.facade.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单下单请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderCheckoutRequest {

    /**
     * 用户 ID
     */
    @NotBlank(message = "用户 ID 不能为空")
    private String userId;

    /**
     * 商品 SKU ID
     */
    @NotBlank(message = "商品 SKU ID 不能为空")
    private String skuId;

    /**
     * 购买数量
     */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于 0")
    private Integer quantity;

    /**
     * 优惠券 ID，可为空
     */
    private String couponId;

    /**
     * 收货地址 ID
     */
    @NotBlank(message = "收货地址 ID 不能为空")
    private String addressId;
}
```

商品库存 VO 用于承接库存子系统查询结果。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/vo/ProductStockVO.java`

```java
package io.github.atengk.designpattern.facade.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品库存返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class ProductStockVO {

    /**
     * 商品 SKU ID
     */
    private String skuId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品单价
     */
    private BigDecimal price;

    /**
     * 可用库存
     */
    private Integer availableStock;
}
```

优惠券计算结果 VO 用于返回优惠金额和应付金额。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/vo/CouponCalculateResultVO.java`

```java
package io.github.atengk.designpattern.facade.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 优惠券计算结果返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class CouponCalculateResultVO {

    /**
     * 订单原始金额
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
     * 优惠说明
     */
    private String description;
}
```

订单创建结果 VO 表示订单子系统创建订单后的结果。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/vo/OrderCreateResultVO.java`

```java
package io.github.atengk.designpattern.facade.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单创建结果返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderCreateResultVO {

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 商品 SKU ID
     */
    private String skuId;

    /**
     * 购买数量
     */
    private Integer quantity;

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

支付创建结果 VO 表示支付子系统创建支付流水后的结果。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/vo/PaymentCreateResultVO.java`

```java
package io.github.atengk.designpattern.facade.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付创建结果返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class PaymentCreateResultVO {

    /**
     * 支付流水号
     */
    private String paymentNo;

    /**
     * 支付状态
     */
    private String paymentStatus;

    /**
     * 支付过期时间
     */
    private LocalDateTime expireTime;
}
```

物流预创建结果 VO 表示物流子系统预创建配送单后的结果。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/vo/DeliveryPrepareResultVO.java`

```java
package io.github.atengk.designpattern.facade.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 物流预创建结果返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class DeliveryPrepareResultVO {

    /**
     * 配送单号
     */
    private String deliveryNo;

    /**
     * 配送状态
     */
    private String deliveryStatus;

    /**
     * 配送说明
     */
    private String message;
}
```

最终下单结果 VO 聚合多个子系统的返回结果，是外观接口对调用方暴露的统一结果。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/vo/OrderCheckoutResultVO.java`

```java
package io.github.atengk.designpattern.facade.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单下单结果返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderCheckoutResultVO {

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 支付流水号
     */
    private String paymentNo;

    /**
     * 配送单号
     */
    private String deliveryNo;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 订单原始金额
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

    /**
     * 支付状态
     */
    private String paymentStatus;

    /**
     * 支付过期时间
     */
    private LocalDateTime paymentExpireTime;
}
```

统一 API 返回对象用于包装接口响应。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/vo/ApiResult.java`

```java
package io.github.atengk.designpattern.facade.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 统一返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 业务状态码
     */
    private Integer code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 成功返回
     *
     * @param data 返回数据
     * @return API 返回对象
     */
    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 失败返回
     *
     * @param message 失败消息
     * @return API 返回对象
     */
    public static ApiResult<Void> fail(String message) {
        return ApiResult.<Void>builder()
                .code(500)
                .message(message)
                .build();
    }
}
```

用户账户服务模拟用户状态校验子系统。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/service/UserAccountService.java`

```java
package io.github.atengk.designpattern.facade.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户账户服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class UserAccountService {

    /**
     * 校验用户是否允许下单
     *
     * @param userId 用户 ID
     */
    public void checkUserCanCheckout(String userId) {
        if (StrUtil.equals("blocked-user", userId)) {
            throw new IllegalStateException("当前用户已被冻结，不能下单");
        }

        log.info("用户下单资格校验通过，userId={}", userId);
    }
}
```

库存服务模拟商品查询和库存锁定子系统。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/service/InventoryService.java`

```java
package io.github.atengk.designpattern.facade.service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.facade.vo.ProductStockVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 库存服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class InventoryService {

    private final Map<String, ProductStockVO> stockMap = MapUtil.<String, ProductStockVO>builder(new HashMap<>())
            .put("sku-1001", ProductStockVO.builder()
                    .skuId("sku-1001")
                    .productName("JDK21 实战课程")
                    .price(new BigDecimal("99.90"))
                    .availableStock(100)
                    .build())
            .put("sku-1002", ProductStockVO.builder()
                    .skuId("sku-1002")
                    .productName("Spring Boot 3 项目课程")
                    .price(new BigDecimal("199.90"))
                    .availableStock(50)
                    .build())
            .build();

    /**
     * 查询商品库存
     *
     * @param skuId 商品 SKU ID
     * @return 商品库存信息
     */
    public ProductStockVO getProductStock(String skuId) {
        ProductStockVO productStock = stockMap.get(skuId);
        if (productStock == null) {
            throw new IllegalArgumentException("商品不存在：" + skuId);
        }

        log.info("查询商品库存成功，skuId={}，stock={}", skuId, productStock.getAvailableStock());
        return productStock;
    }

    /**
     * 锁定库存
     *
     * @param skuId    商品 SKU ID
     * @param quantity 购买数量
     */
    public void lockStock(String skuId, Integer quantity) {
        ProductStockVO productStock = getProductStock(skuId);
        if (productStock.getAvailableStock() < quantity) {
            throw new IllegalStateException(StrUtil.format("商品库存不足，skuId={}，availableStock={}", skuId, productStock.getAvailableStock()));
        }

        log.info("库存锁定成功，skuId={}，quantity={}", skuId, quantity);
    }
}
```

优惠券服务模拟优惠计算子系统。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/service/CouponService.java`

```java
package io.github.atengk.designpattern.facade.service;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.facade.vo.CouponCalculateResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 优惠券服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class CouponService {

    /**
     * 计算优惠金额
     *
     * @param couponId     优惠券 ID
     * @param originAmount 订单原始金额
     * @return 优惠计算结果
     */
    public CouponCalculateResultVO calculate(String couponId, BigDecimal originAmount) {
        BigDecimal discountAmount = BigDecimal.ZERO;
        String description = "未使用优惠券";

        if (StrUtil.isNotBlank(couponId)) {
            if (StrUtil.equals("coupon-10", couponId)) {
                discountAmount = NumberUtil.min(new BigDecimal("10.00"), originAmount);
                description = "使用满减优惠券，优惠 10 元";
            } else if (StrUtil.equals("coupon-30", couponId)) {
                discountAmount = NumberUtil.min(new BigDecimal("30.00"), originAmount);
                description = "使用满减优惠券，优惠 30 元";
            } else {
                throw new IllegalArgumentException("优惠券不存在或不可用：" + couponId);
            }
        }

        BigDecimal payableAmount = originAmount.subtract(discountAmount);
        log.info("优惠券计算完成，couponId={}，originAmount={}，discountAmount={}，payableAmount={}",
                couponId, originAmount, discountAmount, payableAmount);

        return CouponCalculateResultVO.builder()
                .originAmount(originAmount)
                .discountAmount(discountAmount)
                .payableAmount(payableAmount)
                .description(description)
                .build();
    }
}
```

订单服务模拟订单创建子系统。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/service/OrderService.java`

```java
package io.github.atengk.designpattern.facade.service;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.designpattern.facade.dto.OrderCheckoutRequest;
import io.github.atengk.designpattern.facade.vo.CouponCalculateResultVO;
import io.github.atengk.designpattern.facade.vo.OrderCreateResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class OrderService {

    /**
     * 创建订单
     *
     * @param request         下单请求
     * @param calculateResult 优惠计算结果
     * @return 订单创建结果
     */
    public OrderCreateResultVO createOrder(OrderCheckoutRequest request, CouponCalculateResultVO calculateResult) {
        String orderNo = "ORDER_" + IdUtil.fastSimpleUUID();

        log.info("订单创建成功，orderNo={}，userId={}，skuId={}，quantity={}，payableAmount={}",
                orderNo, request.getUserId(), request.getSkuId(), request.getQuantity(), calculateResult.getPayableAmount());

        return OrderCreateResultVO.builder()
                .orderNo(orderNo)
                .userId(request.getUserId())
                .skuId(request.getSkuId())
                .quantity(request.getQuantity())
                .payableAmount(calculateResult.getPayableAmount())
                .orderStatus("WAIT_PAY")
                .build();
    }
}
```

支付服务模拟支付流水创建子系统。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/service/PaymentService.java`

```java
package io.github.atengk.designpattern.facade.service;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.designpattern.facade.config.CheckoutProperties;
import io.github.atengk.designpattern.facade.vo.OrderCreateResultVO;
import io.github.atengk.designpattern.facade.vo.PaymentCreateResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 支付服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final CheckoutProperties checkoutProperties;

    /**
     * 创建支付流水
     *
     * @param order 订单创建结果
     * @return 支付创建结果
     */
    public PaymentCreateResultVO createPayment(OrderCreateResultVO order) {
        String paymentNo = "PAY_" + IdUtil.fastSimpleUUID();
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(checkoutProperties.getPaymentTimeoutMinutes());

        log.info("支付流水创建成功，paymentNo={}，orderNo={}，amount={}，expireTime={}",
                paymentNo, order.getOrderNo(), order.getPayableAmount(), expireTime);

        return PaymentCreateResultVO.builder()
                .paymentNo(paymentNo)
                .paymentStatus("WAIT_PAY")
                .expireTime(expireTime)
                .build();
    }
}
```

物流服务模拟配送单预创建子系统。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/service/LogisticsService.java`

```java
package io.github.atengk.designpattern.facade.service;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.designpattern.facade.dto.OrderCheckoutRequest;
import io.github.atengk.designpattern.facade.vo.DeliveryPrepareResultVO;
import io.github.atengk.designpattern.facade.vo.OrderCreateResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 物流服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class LogisticsService {

    /**
     * 预创建配送单
     *
     * @param request 下单请求
     * @param order   订单创建结果
     * @return 物流预创建结果
     */
    public DeliveryPrepareResultVO prepareDelivery(OrderCheckoutRequest request, OrderCreateResultVO order) {
        String deliveryNo = "DELIVERY_" + IdUtil.fastSimpleUUID();

        log.info("物流配送单预创建成功，deliveryNo={}，orderNo={}，addressId={}",
                deliveryNo, order.getOrderNo(), request.getAddressId());

        return DeliveryPrepareResultVO.builder()
                .deliveryNo(deliveryNo)
                .deliveryStatus("WAIT_PAY")
                .message("支付成功后自动推送仓库发货")
                .build();
    }
}
```

外观接口定义对外暴露的统一下单能力。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/facade/OrderCheckoutFacade.java`

```java
package io.github.atengk.designpattern.facade.facade;

import io.github.atengk.designpattern.facade.dto.OrderCheckoutRequest;
import io.github.atengk.designpattern.facade.vo.OrderCheckoutResultVO;

/**
 * 订单下单外观接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderCheckoutFacade {

    /**
     * 提交订单
     *
     * @param request 下单请求
     * @return 下单结果
     */
    OrderCheckoutResultVO submitOrder(OrderCheckoutRequest request);
}
```

外观实现类负责编排多个子系统调用，并把结果组装成统一返回对象。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/facade/OrderCheckoutFacadeImpl.java`

```java
package io.github.atengk.designpattern.facade.facade;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.designpattern.facade.dto.OrderCheckoutRequest;
import io.github.atengk.designpattern.facade.service.CouponService;
import io.github.atengk.designpattern.facade.service.InventoryService;
import io.github.atengk.designpattern.facade.service.LogisticsService;
import io.github.atengk.designpattern.facade.service.OrderService;
import io.github.atengk.designpattern.facade.service.PaymentService;
import io.github.atengk.designpattern.facade.service.UserAccountService;
import io.github.atengk.designpattern.facade.vo.CouponCalculateResultVO;
import io.github.atengk.designpattern.facade.vo.DeliveryPrepareResultVO;
import io.github.atengk.designpattern.facade.vo.OrderCheckoutResultVO;
import io.github.atengk.designpattern.facade.vo.OrderCreateResultVO;
import io.github.atengk.designpattern.facade.vo.PaymentCreateResultVO;
import io.github.atengk.designpattern.facade.vo.ProductStockVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 订单下单外观实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCheckoutFacadeImpl implements OrderCheckoutFacade {

    private final UserAccountService userAccountService;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final LogisticsService logisticsService;

    /**
     * 提交订单
     *
     * @param request 下单请求
     * @return 下单结果
     */
    @Override
    public OrderCheckoutResultVO submitOrder(OrderCheckoutRequest request) {
        log.info("开始执行下单流程，userId={}，skuId={}，quantity={}，couponId={}",
                request.getUserId(), request.getSkuId(), request.getQuantity(), request.getCouponId());

        userAccountService.checkUserCanCheckout(request.getUserId());

        ProductStockVO productStock = inventoryService.getProductStock(request.getSkuId());
        inventoryService.lockStock(request.getSkuId(), request.getQuantity());

        BigDecimal originAmount = NumberUtil.mul(productStock.getPrice(), new BigDecimal(request.getQuantity().toString()));
        CouponCalculateResultVO calculateResult = couponService.calculate(request.getCouponId(), originAmount);

        OrderCreateResultVO order = orderService.createOrder(request, calculateResult);
        PaymentCreateResultVO payment = paymentService.createPayment(order);
        DeliveryPrepareResultVO delivery = logisticsService.prepareDelivery(request, order);

        log.info("下单流程执行完成，orderNo={}，paymentNo={}，deliveryNo={}",
                order.getOrderNo(), payment.getPaymentNo(), delivery.getDeliveryNo());

        return OrderCheckoutResultVO.builder()
                .orderNo(order.getOrderNo())
                .paymentNo(payment.getPaymentNo())
                .deliveryNo(delivery.getDeliveryNo())
                .productName(productStock.getProductName())
                .quantity(order.getQuantity())
                .originAmount(calculateResult.getOriginAmount())
                .discountAmount(calculateResult.getDiscountAmount())
                .payableAmount(calculateResult.getPayableAmount())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(payment.getPaymentStatus())
                .paymentExpireTime(payment.getExpireTime())
                .build();
    }
}
```

Controller 只依赖外观接口，不直接依赖用户、库存、优惠券、订单、支付、物流等子系统。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/controller/OrderCheckoutController.java`

```java
package io.github.atengk.designpattern.facade.controller;

import io.github.atengk.designpattern.facade.dto.OrderCheckoutRequest;
import io.github.atengk.designpattern.facade.facade.OrderCheckoutFacade;
import io.github.atengk.designpattern.facade.vo.ApiResult;
import io.github.atengk.designpattern.facade.vo.OrderCheckoutResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单下单接口控制器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders/checkout")
public class OrderCheckoutController {

    private final OrderCheckoutFacade orderCheckoutFacade;

    /**
     * 提交订单
     *
     * @param request 下单请求
     * @return 下单结果
     */
    @PostMapping
    public ApiResult<OrderCheckoutResultVO> submitOrder(@Valid @RequestBody OrderCheckoutRequest request) {
        return ApiResult.success(orderCheckoutFacade.submitOrder(request));
    }
}
```

全局异常处理器用于统一处理参数校验异常和业务异常。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/web/GlobalExceptionHandler.java`

```java
package io.github.atengk.designpattern.facade.web;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.facade.vo.ApiResult;
import lombok.extern.slf4j.Slf4j;
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
     * @return API 返回对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> StrUtil.format("{} {}", error.getField(), error.getDefaultMessage()))
                .orElse("请求参数不合法");

        log.warn("请求参数校验失败，message={}", message);
        return ApiResult.fail(message);
    }

    /**
     * 处理非法参数异常
     *
     * @param exception 非法参数异常
     * @return API 返回对象
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("请求参数错误，message={}", exception.getMessage());
        return ApiResult.fail(exception.getMessage());
    }

    /**
     * 处理业务状态异常
     *
     * @param exception 业务状态异常
     * @return API 返回对象
     */
    @ExceptionHandler(IllegalStateException.class)
    public ApiResult<Void> handleIllegalStateException(IllegalStateException exception) {
        log.warn("业务处理失败，message={}", exception.getMessage());
        return ApiResult.fail(exception.getMessage());
    }

    /**
     * 处理系统异常
     *
     * @param exception 系统异常
     * @return API 返回对象
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception exception) {
        log.error("系统处理异常", exception);
        return ApiResult.fail("系统处理异常");
    }
}
```

## 使用方式

启动项目后，可以通过统一下单接口完成完整下单流程。调用方只需要提交下单参数，不需要分别调用用户、库存、优惠券、订单、支付和物流接口。

正常下单请求：

```bash
curl -X POST 'http://localhost:8080/api/orders/checkout' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "user-1001",
    "skuId": "sku-1001",
    "quantity": 2,
    "couponId": "coupon-10",
    "addressId": "address-1001"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "ORDER_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "paymentNo": "PAY_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "deliveryNo": "DELIVERY_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "productName": "JDK21 实战课程",
    "quantity": 2,
    "originAmount": 199.80,
    "discountAmount": 10.00,
    "payableAmount": 189.80,
    "orderStatus": "WAIT_PAY",
    "paymentStatus": "WAIT_PAY",
    "paymentExpireTime": "2026-05-13T10:30:00"
  }
}
```

不使用优惠券下单：

```bash
curl -X POST 'http://localhost:8080/api/orders/checkout' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "user-1002",
    "skuId": "sku-1002",
    "quantity": 1,
    "addressId": "address-1002"
  }'
```

库存不足请求示例：

```bash
curl -X POST 'http://localhost:8080/api/orders/checkout' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "user-1001",
    "skuId": "sku-1001",
    "quantity": 999,
    "couponId": "coupon-10",
    "addressId": "address-1001"
  }'
```

返回示例：

```json
{
  "code": 500,
  "message": "商品库存不足，skuId=sku-1001，availableStock=100",
  "data": null
}
```

用户被冻结请求示例：

```bash
curl -X POST 'http://localhost:8080/api/orders/checkout' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "blocked-user",
    "skuId": "sku-1001",
    "quantity": 1,
    "couponId": "coupon-10",
    "addressId": "address-1001"
  }'
```

返回示例：

```json
{
  "code": 500,
  "message": "当前用户已被冻结，不能下单",
  "data": null
}
```

## 验证方式

可以从下面几个角度验证外观模式是否落地成功。

第一，Controller 没有直接依赖多个子系统。`OrderCheckoutController` 只依赖 `OrderCheckoutFacade`，不依赖 `UserAccountService`、`InventoryService`、`CouponService`、`OrderService`、`PaymentService`、`LogisticsService`。

第二，复杂流程被收敛到外观类。下单流程的调用顺序、结果组装、日志记录都在 `OrderCheckoutFacadeImpl` 中完成，调用方不需要知道内部细节。

第三，子系统仍然保持独立。库存服务只负责库存，优惠券服务只负责优惠计算，支付服务只负责支付流水创建，外观类只是编排它们，不替代它们的职责。

第四，调整内部流程不影响外部接口。比如后续要在创建订单前增加风控校验，只需要在外观实现中增加 `RiskControlService` 调用，Controller 和前端请求格式可以保持不变。

可以重点查看日志：

```text
开始执行下单流程，userId=user-1001，skuId=sku-1001，quantity=2，couponId=coupon-10
用户下单资格校验通过，userId=user-1001
查询商品库存成功，skuId=sku-1001，stock=100
库存锁定成功，skuId=sku-1001，quantity=2
优惠券计算完成，couponId=coupon-10，originAmount=199.80，discountAmount=10.00，payableAmount=189.80
订单创建成功，orderNo=ORDER_xxx，userId=user-1001，skuId=sku-1001，quantity=2，payableAmount=189.80
支付流水创建成功，paymentNo=PAY_xxx，orderNo=ORDER_xxx，amount=189.80
物流配送单预创建成功，deliveryNo=DELIVERY_xxx，orderNo=ORDER_xxx，addressId=address-1001
下单流程执行完成，orderNo=ORDER_xxx，paymentNo=PAY_xxx，deliveryNo=DELIVERY_xxx
```

## 扩展风控校验

如果后续需要在下单流程中增加风控校验，可以新增一个子系统服务。

文件位置：`src/main/java/io/github/atengk/designpattern/facade/service/RiskControlService.java`

```java
package io.github.atengk.designpattern.facade.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.facade.dto.OrderCheckoutRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 风控服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class RiskControlService {

    /**
     * 校验下单风险
     *
     * @param request 下单请求
     */
    public void checkRisk(OrderCheckoutRequest request) {
        if (StrUtil.equals("risk-address", request.getAddressId())) {
            throw new IllegalStateException("当前收货地址存在风险，暂不允许下单");
        }

        log.info("下单风控校验通过，userId={}，addressId={}", request.getUserId(), request.getAddressId());
    }
}
```

然后在 `OrderCheckoutFacadeImpl` 中注入并调用：

```java
private final RiskControlService riskControlService;
```

在用户校验后增加：

```java
riskControlService.checkRisk(request);
```

这样 Controller 不需要修改，前端也不需要知道内部新增了风控子系统。外观模式的价值就在于把内部复杂度隔离在统一入口之后。

## 适用场景

外观模式适合“调用方需要完成一个业务目标，但内部涉及多个子系统”的场景。

常见 Spring Boot 项目场景如下。

| 场景       | 外观入口                | 内部子系统                             |
| ---------- | ----------------------- | -------------------------------------- |
| 电商下单   | `OrderCheckoutFacade`   | 用户、库存、优惠券、订单、支付、物流   |
| 用户注册   | `UserRegisterFacade`    | 账号、短信、风控、积分、欢迎消息       |
| 文件上传   | `FileUploadFacade`      | 文件校验、存储、元数据、病毒扫描、权限 |
| 报表导出   | `ReportExportFacade`    | 查询、转换、模板、文件生成、对象存储   |
| 支付回调   | `PaymentCallbackFacade` | 验签、支付单、订单、库存、通知、日志   |
| 工作流审批 | `WorkflowApproveFacade` | 流程、任务、权限、消息、审计           |
| 统一通知   | `NotificationFacade`    | 短信、邮件、站内信、企业微信、钉钉     |

外观模式尤其适合以下特征明显的模块：

```text
调用方不应该关心内部流程
内部依赖多个服务或第三方系统
流程顺序比较稳定
外部接口需要保持简洁稳定
内部子系统可能持续调整
```

## 和其他模式的区别

外观模式容易和中介者模式、适配器模式、代理模式、服务层编排混淆。区分时重点看模式解决的问题。

| 模式       | 关注点                 | 和外观模式的区别                            |
| ---------- | ---------------------- | ------------------------------------------- |
| 外观模式   | 简化复杂子系统调用     | 重点是给调用方提供统一入口                  |
| 中介者模式 | 协调多个对象之间的交互 | 重点是减少对象之间网状依赖                  |
| 适配器模式 | 转换不兼容接口         | 重点是让旧接口或第三方接口适配当前系统      |
| 代理模式   | 控制对象访问           | 重点是权限、缓存、远程调用、事务等访问控制  |
| 装饰器模式 | 动态增强对象能力       | 重点是给对象叠加额外功能                    |
| 应用服务   | 业务用例编排           | 在 DDD 中常见，很多应用服务天然具有外观特征 |

在 Spring Boot 项目中，很多 `ApplicationService` 或 `FacadeService` 本质上都承担了外观模式的职责。但如果它只是普通 CRUD 服务，就不一定需要刻意称为外观模式。

## 注意事项

外观类不应该变成上帝类。它可以编排流程，但不应该吞掉所有子系统职责。比如库存扣减规则应该在库存服务里，优惠计算规则应该在优惠券服务里，支付流水创建规则应该在支付服务里。

外观接口要稳定。比如 `submitOrder()` 应该表达业务目标，而不是暴露过多内部步骤，例如 `checkUserAndLockStockAndCreatePayment()`。外观方法名应该面向调用方语义。

外观模式不等于事务万能封装。真实下单流程中，库存、优惠券、订单、支付可能涉及本地事务、分布式事务、最终一致性、MQ 补偿等问题。外观类可以负责编排，但事务边界需要根据业务一致性要求单独设计。

外观类要做好异常边界。某个子系统失败时，应该明确是直接失败、重试、补偿，还是记录待处理任务。示例中为了简洁直接抛出异常，生产项目中需要结合事务和补偿机制。

不要为了套模式而增加无意义外观。如果一个接口只调用一个服务，而且没有隐藏复杂性，额外加一层 Facade 可能只是增加样板代码。

## 总结

外观模式的核心价值是为复杂子系统提供统一入口，让调用方不需要了解内部复杂流程。

在本示例中：

```text
OrderCheckoutFacade 对外提供统一下单能力
OrderCheckoutFacadeImpl 编排用户、库存、优惠券、订单、支付、物流子系统
Controller 只依赖外观接口
各子系统仍然保持自己的职责边界
```

最终效果是：

```text
调用方代码更简单
复杂流程集中编排
内部子系统变化不容易影响外部接口
接口语义更贴近业务目标
代码结构更清晰，也更容易维护
```
