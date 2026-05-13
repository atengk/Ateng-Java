# 中介者模式

中介者模式用于把多个对象之间复杂的直接调用关系，集中交给一个中介者对象协调。
在 Spring Boot 项目中，中介者模式常用于订单提交、支付流程、审批协同、消息路由、工作流编排、跨模块业务聚合等场景。

本文以“订单提交协同中心”为例。订单提交时需要校验用户、锁定库存、使用优惠券、创建订单、创建支付单、发送通知。如果这些服务互相调用，依赖关系会快速变复杂。使用中介者模式后，各个业务服务只负责自己的职责，由 `OrderSubmitMediator` 统一编排流程。

## 适用场景

中介者模式适合处理“多个业务对象之间存在复杂协作关系”的场景。

订单提交流程中通常涉及多个模块：

| 模块       | 职责             |
| ---------- | ---------------- |
| 用户服务   | 校验用户状态     |
| 库存服务   | 锁定商品库存     |
| 优惠券服务 | 校验并使用优惠券 |
| 订单服务   | 创建订单         |
| 支付服务   | 创建支付单       |
| 通知服务   | 发送订单创建通知 |

如果让这些服务直接互相调用，例如订单服务调用库存服务、库存服务再调用通知服务、优惠券服务又依赖订单服务，模块之间会形成网状依赖。中介者模式可以把这些交互集中到一个协调类中，使各个业务服务之间保持相对独立。

## 基础配置

本示例基于 Spring Boot 3，使用 Hutool、Lombok 和 Validation。Hutool 用于对象判断、字符串判断、金额计算和 ID 生成，Validation 用于接口参数基础校验。

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

    <!-- Hutool：提供字符串、对象、金额、ID 等常用工具能力 -->
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
src/main/java/io/github/atengk/pattern/mediator
├── MediatorApplication.java
├── common
│   ├── ApiResult.java
│   ├── BizException.java
│   └── GlobalExceptionHandler.java
└── order
    ├── controller
    │   └── OrderSubmitController.java
    ├── dto
    │   └── OrderSubmitRequest.java
    ├── mediator
    │   ├── OrderSubmitMediator.java
    │   └── impl
    │       └── OrderSubmitMediatorImpl.java
    ├── service
    │   ├── CouponService.java
    │   ├── MessageNotifyService.java
    │   ├── OrderDomainService.java
    │   ├── PaymentService.java
    │   ├── ProductStockService.java
    │   ├── UserAccountService.java
    │   └── impl
    │       ├── CouponServiceImpl.java
    │       ├── MessageNotifyServiceImpl.java
    │       ├── OrderDomainServiceImpl.java
    │       ├── PaymentServiceImpl.java
    │       ├── ProductStockServiceImpl.java
    │       └── UserAccountServiceImpl.java
    └── vo
        └── OrderSubmitResultVO.java
```

## 核心设计

本示例把中介者模式拆成三个核心角色：

| 角色             | 项目中的类                                     | 说明                                         |
| ---------------- | ---------------------------------------------- | -------------------------------------------- |
| Mediator         | `OrderSubmitMediator`                          | 定义订单提交协同接口                         |
| ConcreteMediator | `OrderSubmitMediatorImpl`                      | 统一编排用户、库存、优惠券、订单、支付、通知 |
| Colleague        | `UserAccountService`、`ProductStockService` 等 | 具体业务服务，只处理自身职责                 |

执行流程如下：

```text
Controller
  -> OrderSubmitMediator
    -> UserAccountService.checkUserAvailable()
    -> ProductStockService.lockStock()
    -> CouponService.useCoupon()
    -> OrderDomainService.createOrder()
    -> PaymentService.createPayment()
    -> MessageNotifyService.sendOrderCreatedMessage()
  -> 返回订单提交结果
```

中介者模式的重点不是减少代码量，而是减少模块之间的直接依赖。各个业务服务不需要互相注入，也不需要知道完整订单提交流程。

## 公共代码

公共响应、业务异常和全局异常处理用于统一接口返回。实际项目中可以复用已有基础包。

文件位置：`src/main/java/io/github/atengk/pattern/mediator/common/ApiResult.java`

```java
package io.github.atengk.pattern.mediator.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/mediator/common/BizException.java`

```java
package io.github.atengk.pattern.mediator.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/mediator/common/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.mediator.common;

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

下面给出中介者模式的核心代码。示例使用内存数据模拟用户、库存、优惠券、订单和支付。实际项目中可以替换为数据库、Redis、MQ 或远程 RPC 调用。

文件位置：`src/main/java/io/github/atengk/pattern/mediator/MediatorApplication.java`

```java
package io.github.atengk.pattern.mediator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 中介者模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class MediatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediatorApplication.class, args);
    }

}
```

## 请求对象和响应对象

请求对象用于接收订单提交参数，响应对象用于返回中介者协调后的整体结果。

文件位置：`src/main/java/io/github/atengk/pattern/mediator/order/dto/OrderSubmitRequest.java`

```java
package io.github.atengk.pattern.mediator.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单提交请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderSubmitRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;

    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于0")
    private BigDecimal amount;

    private String couponCode;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/mediator/order/vo/OrderSubmitResultVO.java`

```java
package io.github.atengk.pattern.mediator.order.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单提交结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderSubmitResultVO {

    private String orderNo;

    private String payNo;

    private Boolean stockLocked;

    private Boolean couponUsed;

    private BigDecimal discountAmount;

    private BigDecimal payableAmount;

    private String message;

}
```

## 同事对象接口

同事对象是中介者需要协调的业务服务。每个服务只关心自己的业务能力，不直接调用其他同事对象。

文件位置：`src/main/java/io/github/atengk/pattern/mediator/order/service/UserAccountService.java`

```java
package io.github.atengk.pattern.mediator.order.service;

/**
 * 用户账户服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface UserAccountService {

    /**
     * 校验用户是否可用
     *
     * @param userId 用户ID
     */
    void checkUserAvailable(Long userId);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/mediator/order/service/ProductStockService.java`

```java
package io.github.atengk.pattern.mediator.order.service;

/**
 * 商品库存服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ProductStockService {

    /**
     * 锁定商品库存
     *
     * @param productId 商品ID
     * @param quantity 购买数量
     */
    void lockStock(Long productId, Integer quantity);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/mediator/order/service/CouponService.java`

```java
package io.github.atengk.pattern.mediator.order.service;

import java.math.BigDecimal;

/**
 * 优惠券服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface CouponService {

    /**
     * 使用优惠券
     *
     * @param userId 用户ID
     * @param couponCode 优惠券编码
     * @param orderAmount 订单金额
     * @return 优惠金额
     */
    BigDecimal useCoupon(Long userId, String couponCode, BigDecimal orderAmount);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/mediator/order/service/OrderDomainService.java`

```java
package io.github.atengk.pattern.mediator.order.service;

import io.github.atengk.pattern.mediator.order.dto.OrderSubmitRequest;

import java.math.BigDecimal;

/**
 * 订单领域服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderDomainService {

    /**
     * 创建订单
     *
     * @param request 订单提交请求
     * @param discountAmount 优惠金额
     * @param payableAmount 应付金额
     * @return 订单编号
     */
    String createOrder(OrderSubmitRequest request, BigDecimal discountAmount, BigDecimal payableAmount);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/mediator/order/service/PaymentService.java`

```java
package io.github.atengk.pattern.mediator.order.service;

import java.math.BigDecimal;

/**
 * 支付服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface PaymentService {

    /**
     * 创建支付单
     *
     * @param orderNo 订单编号
     * @param payableAmount 应付金额
     * @return 支付单号
     */
    String createPayment(String orderNo, BigDecimal payableAmount);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/mediator/order/service/MessageNotifyService.java`

```java
package io.github.atengk.pattern.mediator.order.service;

/**
 * 消息通知服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface MessageNotifyService {

    /**
     * 发送订单创建通知
     *
     * @param userId 用户ID
     * @param orderNo 订单编号
     */
    void sendOrderCreatedMessage(Long userId, String orderNo);

}
```

## 同事对象实现

下面是各个同事对象的实现。它们之间不互相注入、不互相调用，只提供自身能力。

文件位置：`src/main/java/io/github/atengk/pattern/mediator/order/service/impl/UserAccountServiceImpl.java`

```java
package io.github.atengk.pattern.mediator.order.service.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.mediator.common.BizException;
import io.github.atengk.pattern.mediator.order.service.UserAccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户账户服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class UserAccountServiceImpl implements UserAccountService {

    /**
     * 校验用户是否可用
     *
     * @param userId 用户ID
     */
    @Override
    public void checkUserAvailable(Long userId) {
        if (ObjectUtil.isNull(userId) || userId <= 0) {
            throw new BizException("用户ID不合法");
        }

        if (userId.equals(999L)) {
            log.warn("用户状态异常，userId：{}", userId);
            throw new BizException("当前用户状态异常，不能提交订单");
        }

        log.info("用户校验通过，userId：{}", userId);
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/mediator/order/service/impl/ProductStockServiceImpl.java`

```java
package io.github.atengk.pattern.mediator.order.service.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.mediator.common.BizException;
import io.github.atengk.pattern.mediator.order.service.ProductStockService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 商品库存服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class ProductStockServiceImpl implements ProductStockService {

    private final Map<Long, Integer> stockStorage = new ConcurrentHashMap<>();

    /**
     * 初始化模拟库存数据
     */
    @PostConstruct
    public void initStock() {
        stockStorage.put(1001L, 10);
        stockStorage.put(1002L, 5);
        stockStorage.put(1003L, 0);
        log.info("模拟库存初始化完成，商品数量：{}", stockStorage.size());
    }

    /**
     * 锁定商品库存
     *
     * @param productId 商品ID
     * @param quantity 购买数量
     */
    @Override
    public synchronized void lockStock(Long productId, Integer quantity) {
        if (ObjectUtil.isNull(productId) || productId <= 0) {
            throw new BizException("商品ID不合法");
        }

        if (ObjectUtil.isNull(quantity) || quantity <= 0) {
            throw new BizException("购买数量必须大于0");
        }

        Integer stock = stockStorage.get(productId);
        if (ObjectUtil.isNull(stock)) {
            throw new BizException("商品不存在");
        }

        if (stock < quantity) {
            log.warn("商品库存不足，productId：{}，quantity：{}，stock：{}", productId, quantity, stock);
            throw new BizException("商品库存不足");
        }

        stockStorage.put(productId, stock - quantity);
        log.info("库存锁定成功，productId：{}，quantity：{}，remainingStock：{}",
                productId, quantity, stock - quantity);
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/mediator/order/service/impl/CouponServiceImpl.java`

```java
package io.github.atengk.pattern.mediator.order.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.mediator.common.BizException;
import io.github.atengk.pattern.mediator.order.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 优惠券服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class CouponServiceImpl implements CouponService {

    private static final String NEW_USER_10 = "NEW_USER_10";

    private static final String FULL_100_20 = "FULL_100_20";

    /**
     * 使用优惠券
     *
     * @param userId 用户ID
     * @param couponCode 优惠券编码
     * @param orderAmount 订单金额
     * @return 优惠金额
     */
    @Override
    public BigDecimal useCoupon(Long userId, String couponCode, BigDecimal orderAmount) {
        if (StrUtil.isBlank(couponCode)) {
            log.info("未使用优惠券，userId：{}", userId);
            return BigDecimal.ZERO;
        }

        if (StrUtil.equalsIgnoreCase(couponCode, NEW_USER_10)) {
            log.info("优惠券使用成功，userId：{}，couponCode：{}，discountAmount：10.00", userId, couponCode);
            return BigDecimal.valueOf(10);
        }

        if (StrUtil.equalsIgnoreCase(couponCode, FULL_100_20)) {
            if (NumberUtil.isLess(orderAmount, BigDecimal.valueOf(100))) {
                throw new BizException("订单金额未达到优惠券使用门槛");
            }

            log.info("优惠券使用成功，userId：{}，couponCode：{}，discountAmount：20.00", userId, couponCode);
            return BigDecimal.valueOf(20);
        }

        throw new BizException("优惠券不存在或已失效");
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/mediator/order/service/impl/OrderDomainServiceImpl.java`

```java
package io.github.atengk.pattern.mediator.order.service.impl;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.pattern.mediator.order.dto.OrderSubmitRequest;
import io.github.atengk.pattern.mediator.order.service.OrderDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 订单领域服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class OrderDomainServiceImpl implements OrderDomainService {

    /**
     * 创建订单
     *
     * @param request 订单提交请求
     * @param discountAmount 优惠金额
     * @param payableAmount 应付金额
     * @return 订单编号
     */
    @Override
    public String createOrder(OrderSubmitRequest request, BigDecimal discountAmount, BigDecimal payableAmount) {
        String orderNo = "OD" + IdUtil.getSnowflakeNextIdStr();

        log.info("订单创建成功，orderNo：{}，userId：{}，productId：{}，amount：{}，discountAmount：{}，payableAmount：{}",
                orderNo,
                request.getUserId(),
                request.getProductId(),
                request.getAmount(),
                discountAmount,
                payableAmount);

        return orderNo;
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/mediator/order/service/impl/PaymentServiceImpl.java`

```java
package io.github.atengk.pattern.mediator.order.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.mediator.common.BizException;
import io.github.atengk.pattern.mediator.order.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 支付服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    /**
     * 创建支付单
     *
     * @param orderNo 订单编号
     * @param payableAmount 应付金额
     * @return 支付单号
     */
    @Override
    public String createPayment(String orderNo, BigDecimal payableAmount) {
        if (NumberUtil.isLessOrEqual(payableAmount, BigDecimal.ZERO)) {
            throw new BizException("支付金额必须大于0");
        }

        if (NumberUtil.isGreater(payableAmount, BigDecimal.valueOf(100000))) {
            throw new BizException("支付金额超过单笔上限");
        }

        String payNo = "PAY" + IdUtil.getSnowflakeNextIdStr();
        log.info("支付单创建成功，orderNo：{}，payNo：{}，payableAmount：{}", orderNo, payNo, payableAmount);

        return payNo;
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/mediator/order/service/impl/MessageNotifyServiceImpl.java`

```java
package io.github.atengk.pattern.mediator.order.service.impl;

import io.github.atengk.pattern.mediator.order.service.MessageNotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 消息通知服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class MessageNotifyServiceImpl implements MessageNotifyService {

    /**
     * 发送订单创建通知
     *
     * @param userId 用户ID
     * @param orderNo 订单编号
     */
    @Override
    public void sendOrderCreatedMessage(Long userId, String orderNo) {
        log.info("订单创建通知发送成功，userId：{}，orderNo：{}", userId, orderNo);
    }

}
```

## 中介者接口

中介者接口定义统一的订单提交入口。Controller 只依赖中介者，不直接依赖库存、优惠券、支付等多个服务。

文件位置：`src/main/java/io/github/atengk/pattern/mediator/order/mediator/OrderSubmitMediator.java`

```java
package io.github.atengk.pattern.mediator.order.mediator;

import io.github.atengk.pattern.mediator.order.dto.OrderSubmitRequest;
import io.github.atengk.pattern.mediator.order.vo.OrderSubmitResultVO;

/**
 * 订单提交中介者
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderSubmitMediator {

    /**
     * 提交订单
     *
     * @param request 订单提交请求
     * @return 订单提交结果
     */
    OrderSubmitResultVO submitOrder(OrderSubmitRequest request);

}
```

## 中介者实现

中介者实现类负责集中编排订单提交流程。各个业务服务之间不发生直接调用，由中介者按流程协调。

文件位置：`src/main/java/io/github/atengk/pattern/mediator/order/mediator/impl/OrderSubmitMediatorImpl.java`

```java
package io.github.atengk.pattern.mediator.order.mediator.impl;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.mediator.order.dto.OrderSubmitRequest;
import io.github.atengk.pattern.mediator.order.mediator.OrderSubmitMediator;
import io.github.atengk.pattern.mediator.order.service.CouponService;
import io.github.atengk.pattern.mediator.order.service.MessageNotifyService;
import io.github.atengk.pattern.mediator.order.service.OrderDomainService;
import io.github.atengk.pattern.mediator.order.service.PaymentService;
import io.github.atengk.pattern.mediator.order.service.ProductStockService;
import io.github.atengk.pattern.mediator.order.service.UserAccountService;
import io.github.atengk.pattern.mediator.order.vo.OrderSubmitResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 订单提交中介者实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSubmitMediatorImpl implements OrderSubmitMediator {

    private final UserAccountService userAccountService;

    private final ProductStockService productStockService;

    private final CouponService couponService;

    private final OrderDomainService orderDomainService;

    private final PaymentService paymentService;

    private final MessageNotifyService messageNotifyService;

    /**
     * 提交订单
     *
     * @param request 订单提交请求
     * @return 订单提交结果
     */
    @Override
    public OrderSubmitResultVO submitOrder(OrderSubmitRequest request) {
        log.info("开始提交订单，userId：{}，productId：{}，quantity：{}",
                request.getUserId(), request.getProductId(), request.getQuantity());

        userAccountService.checkUserAvailable(request.getUserId());

        productStockService.lockStock(request.getProductId(), request.getQuantity());

        BigDecimal discountAmount = couponService.useCoupon(
                request.getUserId(),
                request.getCouponCode(),
                request.getAmount()
        );

        BigDecimal payableAmount = calculatePayableAmount(request.getAmount(), discountAmount);

        String orderNo = orderDomainService.createOrder(request, discountAmount, payableAmount);

        String payNo = paymentService.createPayment(orderNo, payableAmount);

        messageNotifyService.sendOrderCreatedMessage(request.getUserId(), orderNo);

        log.info("订单提交完成，orderNo：{}，payNo：{}", orderNo, payNo);

        return OrderSubmitResultVO.builder()
                .orderNo(orderNo)
                .payNo(payNo)
                .stockLocked(true)
                .couponUsed(NumberUtil.isGreater(discountAmount, BigDecimal.ZERO))
                .discountAmount(discountAmount)
                .payableAmount(payableAmount)
                .message("订单提交成功")
                .build();
    }

    /**
     * 计算应付金额
     *
     * @param orderAmount 订单金额
     * @param discountAmount 优惠金额
     * @return 应付金额
     */
    private BigDecimal calculatePayableAmount(BigDecimal orderAmount, BigDecimal discountAmount) {
        BigDecimal payableAmount = NumberUtil.sub(orderAmount, discountAmount);

        if (NumberUtil.isLessOrEqual(payableAmount, BigDecimal.ZERO)) {
            log.warn("优惠后金额小于等于0，按最低支付金额处理，orderAmount：{}，discountAmount：{}",
                    orderAmount, discountAmount);
            return BigDecimal.valueOf(0.01);
        }

        return payableAmount;
    }

}
```

## 控制器接口

控制器只负责接收 HTTP 请求，然后调用中介者。它不直接编排用户、库存、优惠券、订单、支付和通知服务。

文件位置：`src/main/java/io/github/atengk/pattern/mediator/order/controller/OrderSubmitController.java`

```java
package io.github.atengk.pattern.mediator.order.controller;

import io.github.atengk.pattern.mediator.common.ApiResult;
import io.github.atengk.pattern.mediator.order.dto.OrderSubmitRequest;
import io.github.atengk.pattern.mediator.order.mediator.OrderSubmitMediator;
import io.github.atengk.pattern.mediator.order.vo.OrderSubmitResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单提交接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderSubmitController {

    private final OrderSubmitMediator orderSubmitMediator;

    /**
     * 提交订单
     *
     * @param request 订单提交请求
     * @return 订单提交结果
     */
    @PostMapping("/submit")
    public ApiResult<OrderSubmitResultVO> submitOrder(@Valid @RequestBody OrderSubmitRequest request) {
        return ApiResult.success(orderSubmitMediator.submitOrder(request));
    }

}
```

## 使用方式

启动项目后，调用订单提交接口即可触发中介者协调多个业务服务。

接口信息：

| 项目         | 内容                                                         |
| ------------ | ------------------------------------------------------------ |
| 请求路径     | `/orders/submit`                                             |
| 请求方法     | `POST`                                                       |
| Content-Type | `application/json`                                           |
| 主要流程     | 用户校验 → 锁定库存 → 使用优惠券 → 创建订单 → 创建支付单 → 发送通知 |

正常提交订单：

```bash
curl -X POST "http://localhost:8080/orders/submit" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "productId": 1001,
    "quantity": 2,
    "amount": 199.90,
    "couponCode": "FULL_100_20"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "OD1998948715737427968",
    "payNo": "PAY1998948715737427969",
    "stockLocked": true,
    "couponUsed": true,
    "discountAmount": 20,
    "payableAmount": 179.90,
    "message": "订单提交成功"
  }
}
```

不使用优惠券提交订单：

```bash
curl -X POST "http://localhost:8080/orders/submit" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "productId": 1002,
    "quantity": 1,
    "amount": 59.90
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "OD1998948715737427970",
    "payNo": "PAY1998948715737427971",
    "stockLocked": true,
    "couponUsed": false,
    "discountAmount": 0,
    "payableAmount": 59.90,
    "message": "订单提交成功"
  }
}
```

库存不足请求：

```bash
curl -X POST "http://localhost:8080/orders/submit" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "productId": 1003,
    "quantity": 1,
    "amount": 99.90,
    "couponCode": "NEW_USER_10"
  }'
```

返回示例：

```json
{
  "code": 500,
  "message": "商品库存不足",
  "data": null
}
```

优惠券门槛不足请求：

```bash
curl -X POST "http://localhost:8080/orders/submit" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "productId": 1001,
    "quantity": 1,
    "amount": 59.90,
    "couponCode": "FULL_100_20"
  }'
```

返回示例：

```json
{
  "code": 500,
  "message": "订单金额未达到优惠券使用门槛",
  "data": null
}
```

## 验证方式

正常请求后，可以通过日志观察中介者统一协调流程：

```text
开始提交订单，userId：10001，productId：1001，quantity：2
用户校验通过，userId：10001
库存锁定成功，productId：1001，quantity：2，remainingStock：8
优惠券使用成功，userId：10001，couponCode：FULL_100_20，discountAmount：20.00
订单创建成功，orderNo：OD1998948715737427968，userId：10001，productId：1001，amount：199.90，discountAmount：20，payableAmount：179.90
支付单创建成功，orderNo：OD1998948715737427968，payNo：PAY1998948715737427969，payableAmount：179.90
订单创建通知发送成功，userId：10001，orderNo：OD1998948715737427968
订单提交完成，orderNo：OD1998948715737427968，payNo：PAY1998948715737427969
```

从日志可以看到，所有模块的调用顺序都由 `OrderSubmitMediatorImpl` 管理，而不是由各个服务互相调用。

## 扩展方式

如果后续订单提交需要增加“积分抵扣”，可以新增一个同事对象 `PointService`，然后只在中介者中增加对应编排逻辑。

新增接口：

```java
package io.github.atengk.pattern.mediator.order.service;

import java.math.BigDecimal;

/**
 * 积分服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface PointService {

    /**
     * 使用积分抵扣
     *
     * @param userId 用户ID
     * @param orderAmount 订单金额
     * @return 抵扣金额
     */
    BigDecimal usePoint(Long userId, BigDecimal orderAmount);

}
```

新增实现：

```java
package io.github.atengk.pattern.mediator.order.service.impl;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.mediator.order.service.PointService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 积分服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class PointServiceImpl implements PointService {

    /**
     * 使用积分抵扣
     *
     * @param userId 用户ID
     * @param orderAmount 订单金额
     * @return 抵扣金额
     */
    @Override
    public BigDecimal usePoint(Long userId, BigDecimal orderAmount) {
        if (NumberUtil.isLess(orderAmount, BigDecimal.valueOf(50))) {
            return BigDecimal.ZERO;
        }

        BigDecimal pointDiscountAmount = BigDecimal.valueOf(5);
        log.info("积分抵扣成功，userId：{}，discountAmount：{}", userId, pointDiscountAmount);
        return pointDiscountAmount;
    }

}
```

然后在 `OrderSubmitMediatorImpl` 中注入 `PointService`，把积分抵扣纳入统一编排：

```java
private final PointService pointService;
BigDecimal pointDiscountAmount = pointService.usePoint(request.getUserId(), request.getAmount());
BigDecimal totalDiscountAmount = NumberUtil.add(discountAmount, pointDiscountAmount);
BigDecimal payableAmount = calculatePayableAmount(request.getAmount(), totalDiscountAmount);
```

这样新增积分服务时，不需要让优惠券服务调用积分服务，也不需要让订单服务直接依赖积分服务，协作关系仍然集中在中介者中。

## 优点和注意事项

中介者模式的核心价值是降低多个对象之间的耦合，把复杂协作逻辑集中管理。

| 注意事项               | 说明                                                         |
| ---------------------- | ------------------------------------------------------------ |
| 中介者不要过度膨胀     | 中介者只负责编排流程，不要承载过多领域规则                   |
| 同事对象保持职责单一   | 用户服务只处理用户，库存服务只处理库存，支付服务只处理支付   |
| 需要注意事务边界       | 涉及数据库时，订单创建、库存锁定、优惠券使用需要明确事务策略 |
| 远程调用要考虑失败补偿 | 如果库存、支付、优惠券是远程服务，需要设计补偿或最终一致性   |
| 不适合简单流程         | 如果只有两个对象简单调用，直接调用即可，不需要引入中介者     |
| 可以结合领域服务       | 中介者可以作为应用服务或流程编排服务，领域规则仍放在领域服务中 |

## 和外观模式的区别

中介者模式和外观模式都可能提供一个统一入口，但关注点不同。

| 模式       | 关注点                     | 典型场景                                   |
| ---------- | -------------------------- | ------------------------------------------ |
| 中介者模式 | 协调多个对象之间的交互关系 | 订单提交、审批协同、工作流编排、消息路由   |
| 外观模式   | 对外屏蔽子系统复杂接口     | 文件服务封装、支付网关封装、第三方接口聚合 |

外观模式偏向“简化调用入口”，中介者模式偏向“管理对象协作关系”。
如果核心问题是多个模块之间互相依赖、调用关系混乱，优先考虑中介者模式。

## 和责任链模式的区别

中介者模式和责任链模式都能组织复杂流程，但流程结构不同。

| 模式       | 关注点                         | 典型场景                       |
| ---------- | ------------------------------ | ------------------------------ |
| 中介者模式 | 一个协调者统一调度多个对象     | 订单提交、支付流程、审批协同   |
| 责任链模式 | 一个请求按顺序经过多个处理节点 | 校验链、风控链、过滤链、审批链 |

责任链更适合“节点顺序处理同一个请求”。
中介者更适合“多个服务之间存在复杂协作，需要一个中心对象统一调度”。

## 小结

中介者模式在 Spring Boot 项目中的常见落地方式是：定义一个流程协调接口，把多个业务服务作为同事对象，由具体中介者统一编排完整业务流程。
在订单提交、支付、审批、工作流、消息路由等需要多模块协同的场景中，中介者模式可以减少服务之间的直接依赖，使业务流程更清晰、更容易扩展。
