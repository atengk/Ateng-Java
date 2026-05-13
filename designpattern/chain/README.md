# 责任链模式

责任链模式用于把多个处理节点按顺序串联起来，让请求沿着链路依次传递。每个节点只处理自己关心的逻辑，处理完成后交给下一个节点。
在 Spring Boot 项目中，责任链模式常用于参数校验、业务规则校验、审批流、风控校验、导入数据校验、消息消费过滤、支付前置检查等场景。

本文以“订单创建前置校验”为例，把订单创建前需要执行的多项校验拆成多个独立处理器，通过 Spring 自动注入并排序形成责任链。

## 适用场景

责任链模式适合处理“一个请求需要经过多个规则节点”的业务。

在订单创建场景中，常见校验包括：

| 校验节点 | 职责                   |
| -------- | ---------------------- |
| 用户校验 | 校验用户 ID、用户状态  |
| 商品校验 | 校验商品 ID、购买数量  |
| 库存校验 | 校验库存是否充足       |
| 金额校验 | 校验订单金额是否合法   |
| 风控校验 | 校验是否命中高风险规则 |

如果这些逻辑全部写在 `OrderServiceImpl#createOrder` 中，代码会快速膨胀，后续新增规则也容易影响原有逻辑。责任链模式可以把这些规则拆开，使每个节点职责单一、顺序清晰、扩展方便。

## 基础配置

本示例基于 Spring Boot 3，使用 Hutool、Lombok 和 Validation。Hutool 用于字符串、对象和集合判断，Validation 用于 Controller 入参基础校验，责任链用于业务规则校验。

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

    <!-- Hutool：提供常用工具类，减少重复判断代码 -->
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
src/main/java/io/github/atengk/pattern/chain
├── ChainApplication.java
├── common
│   ├── ApiResult.java
│   ├── BizException.java
│   └── GlobalExceptionHandler.java
├── order
│   ├── controller
│   │   └── OrderController.java
│   ├── dto
│   │   └── OrderCreateRequest.java
│   ├── service
│   │   ├── OrderService.java
│   │   └── impl
│   │       └── OrderServiceImpl.java
│   └── chain
│       ├── OrderCreateContext.java
│       ├── OrderCreateChainHandler.java
│       ├── OrderCreateChainExecutor.java
│       └── handler
│           ├── UserCheckHandler.java
│           ├── ProductCheckHandler.java
│           ├── StockCheckHandler.java
│           ├── AmountCheckHandler.java
│           └── RiskCheckHandler.java
```

## 核心设计

本示例把责任链拆成三个核心角色：

| 角色       | 项目中的类                 | 说明                       |
| ---------- | -------------------------- | -------------------------- |
| 请求上下文 | `OrderCreateContext`       | 责任链中传递的数据对象     |
| 抽象处理器 | `OrderCreateChainHandler`  | 定义每个节点必须实现的方法 |
| 链路执行器 | `OrderCreateChainExecutor` | 负责按顺序执行所有处理器   |

执行流程如下：

```text
Controller
  -> OrderService
    -> OrderCreateChainExecutor
      -> UserCheckHandler
      -> ProductCheckHandler
      -> StockCheckHandler
      -> AmountCheckHandler
      -> RiskCheckHandler
    -> 创建订单主流程
```

责任链节点只负责校验，不直接创建订单。订单创建仍然由 `OrderServiceImpl` 统一完成，这样可以避免校验逻辑和主业务逻辑混杂。

## 完整代码

下面给出可直接集成到 Spring Boot 项目中的核心代码。示例没有接入真实数据库，商品、库存和用户状态使用模拟逻辑代替；实际项目中可替换为 Mapper、RPC、Redis 或缓存查询。

文件位置：`src/main/java/io/github/atengk/pattern/chain/ChainApplication.java`

```java
package io.github.atengk.pattern.chain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 责任链模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class ChainApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChainApplication.class, args);
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/chain/common/ApiResult.java`

```java
package io.github.atengk.pattern.chain.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/chain/common/BizException.java`

```java
package io.github.atengk.pattern.chain.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/chain/common/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.chain.common;

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
     * 处理请求体异常
     *
     * @param exception 请求体异常
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

文件位置：`src/main/java/io/github/atengk/pattern/chain/order/dto/OrderCreateRequest.java`

```java
package io.github.atengk.pattern.chain.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单创建请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderCreateRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;

    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "客户端IP不能为空")
    private String clientIp;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/chain/order/chain/OrderCreateContext.java`

```java
package io.github.atengk.pattern.chain.order.chain;

import io.github.atengk.pattern.chain.order.dto.OrderCreateRequest;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单创建责任链上下文
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderCreateContext {

    private Long userId;

    private Long productId;

    private Integer quantity;

    private BigDecimal amount;

    private String clientIp;

    /**
     * 从请求参数构建责任链上下文
     *
     * @param request 订单创建请求参数
     * @return 责任链上下文
     */
    public static OrderCreateContext fromRequest(OrderCreateRequest request) {
        return OrderCreateContext.builder()
                .userId(request.getUserId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .amount(request.getAmount())
                .clientIp(request.getClientIp())
                .build();
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/chain/order/chain/OrderCreateChainHandler.java`

```java
package io.github.atengk.pattern.chain.order.chain;

/**
 * 订单创建责任链处理器
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderCreateChainHandler {

    /**
     * 返回处理器执行顺序，数值越小越先执行
     *
     * @return 执行顺序
     */
    int order();

    /**
     * 执行责任链节点逻辑
     *
     * @param context 订单创建上下文
     */
    void handle(OrderCreateContext context);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/chain/order/chain/OrderCreateChainExecutor.java`

```java
package io.github.atengk.pattern.chain.order.chain;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 订单创建责任链执行器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderCreateChainExecutor {

    private final List<OrderCreateChainHandler> chainHandlers;

    /**
     * 初始化责任链执行器
     *
     * @param chainHandlers Spring 容器中的责任链处理器
     */
    public OrderCreateChainExecutor(List<OrderCreateChainHandler> chainHandlers) {
        this.chainHandlers = chainHandlers.stream()
                .sorted(Comparator.comparingInt(OrderCreateChainHandler::order))
                .toList();
    }

    /**
     * 执行订单创建责任链
     *
     * @param context 订单创建上下文
     */
    public void execute(OrderCreateContext context) {
        if (CollUtil.isEmpty(chainHandlers)) {
            log.warn("订单创建责任链未配置任何处理器");
            return;
        }

        log.info("开始执行订单创建责任链，处理器数量：{}", chainHandlers.size());
        for (OrderCreateChainHandler handler : chainHandlers) {
            log.info("执行订单创建责任链节点：{}", handler.getClass().getSimpleName());
            handler.handle(context);
        }
        log.info("订单创建责任链执行完成");
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/chain/order/chain/handler/UserCheckHandler.java`

```java
package io.github.atengk.pattern.chain.order.chain.handler;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.chain.common.BizException;
import io.github.atengk.pattern.chain.order.chain.OrderCreateChainHandler;
import io.github.atengk.pattern.chain.order.chain.OrderCreateContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户校验责任链节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UserCheckHandler implements OrderCreateChainHandler {

    /**
     * 返回处理器执行顺序
     *
     * @return 执行顺序
     */
    @Override
    public int order() {
        return 10;
    }

    /**
     * 校验用户状态
     *
     * @param context 订单创建上下文
     */
    @Override
    public void handle(OrderCreateContext context) {
        Long userId = context.getUserId();
        if (ObjectUtil.isNull(userId)) {
            throw new BizException("用户ID不能为空");
        }

        if (userId <= 0) {
            throw new BizException("用户ID不合法");
        }

        if (userId.equals(999L)) {
            log.warn("用户状态异常，userId：{}", userId);
            throw new BizException("当前用户状态异常，暂不能创建订单");
        }

        log.info("用户校验通过，userId：{}", userId);
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/chain/order/chain/handler/ProductCheckHandler.java`

```java
package io.github.atengk.pattern.chain.order.chain.handler;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.chain.common.BizException;
import io.github.atengk.pattern.chain.order.chain.OrderCreateChainHandler;
import io.github.atengk.pattern.chain.order.chain.OrderCreateContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 商品校验责任链节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class ProductCheckHandler implements OrderCreateChainHandler {

    /**
     * 返回处理器执行顺序
     *
     * @return 执行顺序
     */
    @Override
    public int order() {
        return 20;
    }

    /**
     * 校验商品和购买数量
     *
     * @param context 订单创建上下文
     */
    @Override
    public void handle(OrderCreateContext context) {
        if (ObjectUtil.isNull(context.getProductId())) {
            throw new BizException("商品ID不能为空");
        }

        if (context.getProductId() <= 0) {
            throw new BizException("商品ID不合法");
        }

        if (ObjectUtil.isNull(context.getQuantity()) || context.getQuantity() <= 0) {
            throw new BizException("购买数量必须大于0");
        }

        if (context.getProductId().equals(404L)) {
            log.warn("商品不存在，productId：{}", context.getProductId());
            throw new BizException("商品不存在或已下架");
        }

        log.info("商品校验通过，productId：{}，quantity：{}", context.getProductId(), context.getQuantity());
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/chain/order/chain/handler/StockCheckHandler.java`

```java
package io.github.atengk.pattern.chain.order.chain.handler;

import io.github.atengk.pattern.chain.common.BizException;
import io.github.atengk.pattern.chain.order.chain.OrderCreateChainHandler;
import io.github.atengk.pattern.chain.order.chain.OrderCreateContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 库存校验责任链节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class StockCheckHandler implements OrderCreateChainHandler {

    /**
     * 返回处理器执行顺序
     *
     * @return 执行顺序
     */
    @Override
    public int order() {
        return 30;
    }

    /**
     * 校验商品库存
     *
     * @param context 订单创建上下文
     */
    @Override
    public void handle(OrderCreateContext context) {
        Integer stock = getMockStock(context.getProductId());
        if (context.getQuantity() > stock) {
            log.warn("库存不足，productId：{}，quantity：{}，stock：{}",
                    context.getProductId(), context.getQuantity(), stock);
            throw new BizException("商品库存不足");
        }

        log.info("库存校验通过，productId：{}，stock：{}", context.getProductId(), stock);
    }

    /**
     * 获取模拟库存
     *
     * @param productId 商品ID
     * @return 库存数量
     */
    private Integer getMockStock(Long productId) {
        if (productId.equals(1001L)) {
            return 5;
        }
        return 100;
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/chain/order/chain/handler/AmountCheckHandler.java`

```java
package io.github.atengk.pattern.chain.order.chain.handler;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.chain.common.BizException;
import io.github.atengk.pattern.chain.order.chain.OrderCreateChainHandler;
import io.github.atengk.pattern.chain.order.chain.OrderCreateContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 金额校验责任链节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class AmountCheckHandler implements OrderCreateChainHandler {

    /**
     * 返回处理器执行顺序
     *
     * @return 执行顺序
     */
    @Override
    public int order() {
        return 40;
    }

    /**
     * 校验订单金额
     *
     * @param context 订单创建上下文
     */
    @Override
    public void handle(OrderCreateContext context) {
        BigDecimal amount = context.getAmount();
        if (ObjectUtil.isNull(amount)) {
            throw new BizException("订单金额不能为空");
        }

        if (NumberUtil.isLessOrEqual(amount, BigDecimal.ZERO)) {
            throw new BizException("订单金额必须大于0");
        }

        BigDecimal maxAmount = BigDecimal.valueOf(100000);
        if (NumberUtil.isGreater(amount, maxAmount)) {
            log.warn("订单金额超过单笔上限，userId：{}，amount：{}", context.getUserId(), amount);
            throw new BizException("订单金额超过单笔上限");
        }

        log.info("金额校验通过，amount：{}", amount);
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/chain/order/chain/handler/RiskCheckHandler.java`

```java
package io.github.atengk.pattern.chain.order.chain.handler;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.chain.common.BizException;
import io.github.atengk.pattern.chain.order.chain.OrderCreateChainHandler;
import io.github.atengk.pattern.chain.order.chain.OrderCreateContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 风控校验责任链节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class RiskCheckHandler implements OrderCreateChainHandler {

    /**
     * 返回处理器执行顺序
     *
     * @return 执行顺序
     */
    @Override
    public int order() {
        return 50;
    }

    /**
     * 校验订单风险
     *
     * @param context 订单创建上下文
     */
    @Override
    public void handle(OrderCreateContext context) {
        String clientIp = context.getClientIp();
        if (StrUtil.isBlank(clientIp)) {
            throw new BizException("客户端IP不能为空");
        }

        if (StrUtil.equals(clientIp, "127.0.0.2")) {
            log.warn("订单命中风控规则，userId：{}，clientIp：{}", context.getUserId(), clientIp);
            throw new BizException("当前请求存在风险，请稍后再试");
        }

        log.info("风控校验通过，clientIp：{}", clientIp);
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/chain/order/service/OrderService.java`

```java
package io.github.atengk.pattern.chain.order.service;

import io.github.atengk.pattern.chain.order.dto.OrderCreateRequest;

/**
 * 订单服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderService {

    /**
     * 创建订单
     *
     * @param request 订单创建请求参数
     * @return 订单编号
     */
    String createOrder(OrderCreateRequest request);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/chain/order/service/impl/OrderServiceImpl.java`

```java
package io.github.atengk.pattern.chain.order.service.impl;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.pattern.chain.order.chain.OrderCreateChainExecutor;
import io.github.atengk.pattern.chain.order.chain.OrderCreateContext;
import io.github.atengk.pattern.chain.order.dto.OrderCreateRequest;
import io.github.atengk.pattern.chain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderCreateChainExecutor orderCreateChainExecutor;

    /**
     * 创建订单
     *
     * @param request 订单创建请求参数
     * @return 订单编号
     */
    @Override
    public String createOrder(OrderCreateRequest request) {
        OrderCreateContext context = OrderCreateContext.fromRequest(request);

        orderCreateChainExecutor.execute(context);

        String orderNo = IdUtil.getSnowflakeNextIdStr();
        log.info("订单创建成功，userId：{}，productId：{}，orderNo：{}",
                request.getUserId(), request.getProductId(), orderNo);

        return orderNo;
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/chain/order/controller/OrderController.java`

```java
package io.github.atengk.pattern.chain.order.controller;

import io.github.atengk.pattern.chain.common.ApiResult;
import io.github.atengk.pattern.chain.order.dto.OrderCreateRequest;
import io.github.atengk.pattern.chain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     *
     * @param request 订单创建请求参数
     * @return 订单编号
     */
    @PostMapping
    public ApiResult<String> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return ApiResult.success(orderService.createOrder(request));
    }

}
```

## 使用方式

启动项目后，请求订单创建接口即可触发责任链。

接口信息：

| 项目         | 内容                                            |
| ------------ | ----------------------------------------------- |
| 请求路径     | `/orders`                                       |
| 请求方法     | `POST`                                          |
| Content-Type | `application/json`                              |
| 主要流程     | Controller 参数校验 → 责任链业务校验 → 创建订单 |

正常创建订单请求：

```bash
curl -X POST "http://localhost:8080/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "productId": 1001,
    "quantity": 2,
    "amount": 199.90,
    "clientIp": "192.168.1.10"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "1998948715737427968"
}
```

库存不足请求：

```bash
curl -X POST "http://localhost:8080/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "productId": 1001,
    "quantity": 10,
    "amount": 199.90,
    "clientIp": "192.168.1.10"
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

风控拦截请求：

```bash
curl -X POST "http://localhost:8080/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "productId": 1002,
    "quantity": 1,
    "amount": 99.90,
    "clientIp": "127.0.0.2"
  }'
```

返回示例：

```json
{
  "code": 500,
  "message": "当前请求存在风险，请稍后再试",
  "data": null
}
```

## 扩展方式

新增一个责任链节点时，只需要新增一个实现 `OrderCreateChainHandler` 的 Spring Bean，并设置合适的 `order()` 顺序即可，不需要修改原有执行器和已有节点。

例如新增“优惠券校验节点”。

文件位置：`src/main/java/io/github/atengk/pattern/chain/order/chain/handler/CouponCheckHandler.java`

```java
package io.github.atengk.pattern.chain.order.chain.handler;

import io.github.atengk.pattern.chain.order.chain.OrderCreateChainHandler;
import io.github.atengk.pattern.chain.order.chain.OrderCreateContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 优惠券校验责任链节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class CouponCheckHandler implements OrderCreateChainHandler {

    /**
     * 返回处理器执行顺序
     *
     * @return 执行顺序
     */
    @Override
    public int order() {
        return 35;
    }

    /**
     * 校验优惠券信息
     *
     * @param context 订单创建上下文
     */
    @Override
    public void handle(OrderCreateContext context) {
        log.info("优惠券校验通过，userId：{}，productId：{}",
                context.getUserId(), context.getProductId());
    }

}
```

因为 `CouponCheckHandler#order()` 返回 `35`，所以它会在库存校验之后、金额校验之前执行：

```text
UserCheckHandler       order = 10
ProductCheckHandler    order = 20
StockCheckHandler      order = 30
CouponCheckHandler     order = 35
AmountCheckHandler     order = 40
RiskCheckHandler       order = 50
```

这就是责任链模式在 Spring Boot 中最常见的扩展方式：新增节点，不改主流程。

## 验证方式

可以通过日志确认责任链执行顺序。正常请求时，控制台应看到类似日志：

```text
开始执行订单创建责任链，处理器数量：5
执行订单创建责任链节点：UserCheckHandler
用户校验通过，userId：10001
执行订单创建责任链节点：ProductCheckHandler
商品校验通过，productId：1001，quantity：2
执行订单创建责任链节点：StockCheckHandler
库存校验通过，productId：1001，stock：5
执行订单创建责任链节点：AmountCheckHandler
金额校验通过，amount：199.90
执行订单创建责任链节点：RiskCheckHandler
风控校验通过，clientIp：192.168.1.10
订单创建责任链执行完成
订单创建成功，userId：10001，productId：1001，orderNo：1998948715737427968
```

如果某个节点抛出 `BizException`，后续节点不会继续执行。例如库存不足时，执行到 `StockCheckHandler` 后流程直接终止。

## 优点和注意事项

责任链模式的主要优点是解耦业务规则。每个处理器只负责一个节点，新增规则时只需要新增处理器类，符合开闭原则。结合 Spring Boot 后，可以通过 `List<OrderCreateChainHandler>` 自动收集处理器，再通过 `order()` 控制执行顺序，项目落地成本较低。

使用时需要注意以下几点：

| 注意事项           | 说明                                                   |
| ------------------ | ------------------------------------------------------ |
| 节点顺序必须清晰   | 前置依赖强的节点要放在前面，例如商品校验应早于库存校验 |
| 节点职责要单一     | 不要在一个节点中处理多个不相关规则                     |
| 异常要统一处理     | 建议节点抛业务异常，由全局异常处理器统一返回           |
| 不要滥用责任链     | 如果只有一两个简单判断，直接写在 Service 中即可        |
| 链路上下文不要过重 | `Context` 只放链路需要的数据，避免变成万能对象         |

## 小结

责任链模式在 Spring Boot 项目中的典型落地方式是：定义统一处理器接口，使用 Spring 自动收集所有处理器，按顺序执行每个节点。
在订单创建、支付前校验、审批流程、导入校验、风控过滤等场景中，责任链模式可以有效降低主业务方法复杂度，提高规则扩展能力。
