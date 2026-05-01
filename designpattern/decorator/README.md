# 设计模式：装饰器模式

装饰器模式用于在不修改原有类代码的前提下，动态增强对象能力。在 JDK21 和 Spring Boot 3 项目中，装饰器模式常用于接口调用增强、日志审计、幂等控制、权限校验、缓存增强、参数脱敏、加解密、限流、重试、消息发送增强、文件处理增强等场景。

需要注意：装饰器模式关注的是“增强原有对象能力”，不是替换原有算法。如果是不同算法之间切换，更适合策略模式；如果是固定流程复用，更适合模板方法模式；如果是给已有功能叠加前置或后置能力，装饰器模式更合适。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证装饰器模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、集合、ID、金额、摘要等通用处理 -->
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

装饰器模式的核心目标是让增强逻辑和核心业务逻辑解耦，通过包装原始对象来扩展能力。

常见角色如下：

| 角色       | 说明                                       |
| ---------- | ------------------------------------------ |
| 抽象组件   | 定义统一接口，原始对象和装饰器都实现它     |
| 具体组件   | 原始业务实现，负责核心功能                 |
| 抽象装饰器 | 持有抽象组件引用，并把调用转发给被装饰对象 |
| 具体装饰器 | 在调用前后增加增强逻辑                     |
| 客户端     | 面向抽象组件调用，不关心具体增强链路       |

典型结构如下：

```text
调用方 -> 装饰器B -> 装饰器A -> 原始对象
```

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring Bean 装饰器 > 普通 Java 装饰器 > 直接修改原有业务类
```

装饰器模式和代理模式结构很像。简单区分是：装饰器模式更强调“增强能力、可叠加”，代理模式更强调“控制访问、隐藏目标对象”。

## 普通 Java 装饰器

普通 Java 装饰器适合不依赖 Spring 容器的增强场景。下面以消息发送为例，基础发送器只负责发送消息，装饰器负责增加追踪 ID、内容签名等增强能力。

### 文件结构

```text
src/main/java/io/github/atengk/design/decorator/simple/
├── MessageRequest.java
├── MessageSender.java
├── DefaultMessageSender.java
├── AbstractMessageSenderDecorator.java
├── TraceMessageSenderDecorator.java
└── SignMessageSenderDecorator.java
```

文件位置：`src/main/java/io/github/atengk/design/decorator/simple/MessageRequest.java`

下面是消息发送请求参数对象。

```java
package io.github.atengk.design.decorator.simple;

/**
 * 消息发送请求
 *
 * @param receiver 接收人
 * @param content  消息内容
 * @author Ateng
 * @since 2026-04-30
 */
public record MessageRequest(String receiver, String content) {
}
```

文件位置：`src/main/java/io/github/atengk/design/decorator/simple/MessageSender.java`

下面是消息发送接口，原始发送器和所有装饰器都实现该接口。

```java
package io.github.atengk.design.decorator.simple;

/**
 * 消息发送器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface MessageSender {

    /**
     * 发送消息
     *
     * @param request 消息发送请求
     * @return 发送结果
     */
    String send(MessageRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/decorator/simple/DefaultMessageSender.java`

下面是原始消息发送器，只负责核心发送逻辑。

```java
package io.github.atengk.design.decorator.simple;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认消息发送器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class DefaultMessageSender implements MessageSender {

    /**
     * 发送消息
     *
     * @param request 消息发送请求
     * @return 发送结果
     */
    @Override
    public String send(MessageRequest request) {
        if (request == null || StrUtil.hasBlank(request.receiver(), request.content())) {
            log.warn("消息发送失败，请求参数不完整");
            throw new IllegalArgumentException("接收人和消息内容不能为空");
        }

        log.info("发送消息，接收人：{}，内容：{}", request.receiver(), request.content());
        return "消息发送成功";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/decorator/simple/AbstractMessageSenderDecorator.java`

下面是抽象装饰器，负责持有被装饰对象，并把调用转发给它。

```java
package io.github.atengk.design.decorator.simple;

/**
 * 消息发送器抽象装饰器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public abstract class AbstractMessageSenderDecorator implements MessageSender {

    protected final MessageSender delegate;

    /**
     * 创建消息发送器装饰器
     *
     * @param delegate 被装饰的消息发送器
     */
    protected AbstractMessageSenderDecorator(MessageSender delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("被装饰的消息发送器不能为空");
        }
        this.delegate = delegate;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/decorator/simple/TraceMessageSenderDecorator.java`

下面是追踪装饰器，在发送消息前后增加 traceId 日志。

```java
package io.github.atengk.design.decorator.simple;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 追踪消息发送器装饰器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class TraceMessageSenderDecorator extends AbstractMessageSenderDecorator {

    /**
     * 创建追踪消息发送器装饰器
     *
     * @param delegate 被装饰的消息发送器
     */
    public TraceMessageSenderDecorator(MessageSender delegate) {
        super(delegate);
    }

    /**
     * 发送消息
     *
     * @param request 消息发送请求
     * @return 发送结果
     */
    @Override
    public String send(MessageRequest request) {
        String traceId = IdUtil.fastSimpleUUID();
        log.info("开始发送消息，traceId：{}，接收人：{}", traceId, request.receiver());

        String result = delegate.send(request);

        log.info("消息发送结束，traceId：{}，结果：{}", traceId, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/decorator/simple/SignMessageSenderDecorator.java`

下面是签名装饰器，在发送前对消息内容生成摘要签名。

```java
package io.github.atengk.design.decorator.simple;

import cn.hutool.crypto.SecureUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 签名消息发送器装饰器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class SignMessageSenderDecorator extends AbstractMessageSenderDecorator {

    /**
     * 创建签名消息发送器装饰器
     *
     * @param delegate 被装饰的消息发送器
     */
    public SignMessageSenderDecorator(MessageSender delegate) {
        super(delegate);
    }

    /**
     * 发送消息
     *
     * @param request 消息发送请求
     * @return 发送结果
     */
    @Override
    public String send(MessageRequest request) {
        String sign = SecureUtil.md5(request.content());
        log.info("生成消息签名，接收人：{}，签名：{}", request.receiver(), sign);

        return delegate.send(request);
    }
}
```

使用方式：

```java
MessageSender sender = new TraceMessageSenderDecorator(
        new SignMessageSenderDecorator(
                new DefaultMessageSender()
        )
);

String result = sender.send(new MessageRequest("ateng@example.com", "订单已支付"));
```

调用链路如下：

```text
TraceMessageSenderDecorator
    -> SignMessageSenderDecorator
        -> DefaultMessageSender
```

普通 Java 装饰器的优点是结构清晰、增强能力可自由组合。缺点是对象链路需要手动创建，在复杂业务系统中容易散落在各处。

## Spring Boot 装饰器

Spring Boot 项目中更常见的写法，是将原始实现和装饰器都注册为 Spring Bean，然后通过 `@Qualifier` 和 `@Primary` 组织装饰链。

下面以订单创建为例，原始服务只负责创建订单，装饰器分别增强审计日志和幂等控制。

整体调用链路如下：

```text
Controller
    -> IdempotentOrderCreateServiceDecorator
        -> AuditOrderCreateServiceDecorator
            -> BasicOrderCreateService
```

示例能力如下：

```text
BasicOrderCreateService                 创建订单
AuditOrderCreateServiceDecorator        增加审计日志
IdempotentOrderCreateServiceDecorator   增加请求幂等控制
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── DecoratorApplication.java
├── controller/
│   └── OrderCreateController.java
├── dto/
│   ├── OrderCreateRequest.java
│   └── OrderCreateResponse.java
└── service/
    ├── OrderCreateService.java
    └── impl/
        ├── BasicOrderCreateService.java
        ├── AuditOrderCreateServiceDecorator.java
        └── IdempotentOrderCreateServiceDecorator.java
```

文件位置：`src/main/java/io/github/atengk/design/DecoratorApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 装饰器模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class DecoratorApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DecoratorApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderCreateRequest.java`

下面是订单创建请求参数对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 订单创建请求
 *
 * @param requestId   请求ID，用于幂等控制
 * @param userId      用户ID
 * @param productId   商品ID
 * @param productName 商品名称
 * @param quantity    购买数量
 * @param unitPrice   商品单价
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderCreateRequest(
        String requestId,
        Long userId,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderCreateResponse.java`

下面是订单创建响应结果。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 订单创建响应
 *
 * @param orderNo    订单号
 * @param userId     用户ID
 * @param totalAmount 订单总金额
 * @param message    结果消息
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderCreateResponse(
        String orderNo,
        Long userId,
        BigDecimal totalAmount,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/service/OrderCreateService.java`

下面是订单创建服务接口，原始服务和装饰器都实现该接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.OrderCreateRequest;
import io.github.atengk.design.dto.OrderCreateResponse;

/**
 * 订单创建服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderCreateService {

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单创建响应
     */
    OrderCreateResponse createOrder(OrderCreateRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/BasicOrderCreateService.java`

下面是订单创建原始实现，只负责核心下单逻辑。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.OrderCreateRequest;
import io.github.atengk.design.dto.OrderCreateResponse;
import io.github.atengk.design.service.OrderCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 基础订单创建服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service("basicOrderCreateService")
public class BasicOrderCreateService implements OrderCreateService {

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单创建响应
     */
    @Override
    public OrderCreateResponse createOrder(OrderCreateRequest request) {
        validateRequest(request);

        String orderNo = "ORDER" + IdUtil.getSnowflakeNextId();
        BigDecimal totalAmount = NumberUtil.mul(request.unitPrice(), BigDecimal.valueOf(request.quantity()))
                .setScale(2, RoundingMode.HALF_UP);

        log.info("执行基础下单逻辑，订单号：{}，用户ID：{}，商品ID：{}，金额：{}",
                orderNo, request.userId(), request.productId(), totalAmount);

        return new OrderCreateResponse(orderNo, request.userId(), totalAmount, "创建成功");
    }

    /**
     * 校验订单创建请求
     *
     * @param request 订单创建请求
     */
    private void validateRequest(OrderCreateRequest request) {
        if (request == null) {
            log.warn("创建订单失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.isBlank(request.requestId())) {
            log.warn("创建订单失败，请求ID为空");
            throw new IllegalArgumentException("请求ID不能为空");
        }

        if (request.userId() == null || request.userId() <= 0) {
            log.warn("创建订单失败，用户ID不合法，用户ID：{}", request.userId());
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        if (request.productId() == null || request.productId() <= 0) {
            log.warn("创建订单失败，商品ID不合法，商品ID：{}", request.productId());
            throw new IllegalArgumentException("商品ID必须大于0");
        }

        if (StrUtil.isBlank(request.productName())) {
            log.warn("创建订单失败，商品名称为空");
            throw new IllegalArgumentException("商品名称不能为空");
        }

        if (request.quantity() == null || request.quantity() <= 0) {
            log.warn("创建订单失败，购买数量不合法，购买数量：{}", request.quantity());
            throw new IllegalArgumentException("购买数量必须大于0");
        }

        if (request.unitPrice() == null || request.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("创建订单失败，商品单价不合法，商品单价：{}", request.unitPrice());
            throw new IllegalArgumentException("商品单价必须大于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/AuditOrderCreateServiceDecorator.java`

下面是审计装饰器，用于在订单创建前后记录审计日志。

```java
package io.github.atengk.design.service.impl;

import io.github.atengk.design.dto.OrderCreateRequest;
import io.github.atengk.design.dto.OrderCreateResponse;
import io.github.atengk.design.service.OrderCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 订单创建审计装饰器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service("auditOrderCreateService")
public class AuditOrderCreateServiceDecorator implements OrderCreateService {

    private final OrderCreateService delegate;

    /**
     * 创建订单审计装饰器
     *
     * @param delegate 被装饰的订单创建服务
     */
    public AuditOrderCreateServiceDecorator(@Qualifier("basicOrderCreateService") OrderCreateService delegate) {
        this.delegate = delegate;
    }

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单创建响应
     */
    @Override
    public OrderCreateResponse createOrder(OrderCreateRequest request) {
        log.info("记录下单审计开始，requestId：{}，用户ID：{}，商品ID：{}",
                request.requestId(), request.userId(), request.productId());

        OrderCreateResponse response = delegate.createOrder(request);

        log.info("记录下单审计完成，requestId：{}，订单号：{}，结果：{}",
                request.requestId(), response.orderNo(), response.message());
        return response;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/IdempotentOrderCreateServiceDecorator.java`

下面是幂等装饰器，用于处理同一个 `requestId` 的重复下单请求。示例使用本地 `ConcurrentHashMap` 演示，生产环境建议改成 Redis 或数据库唯一约束。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.OrderCreateRequest;
import io.github.atengk.design.dto.OrderCreateResponse;
import io.github.atengk.design.service.OrderCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单创建幂等装饰器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Primary
@Service("idempotentOrderCreateService")
public class IdempotentOrderCreateServiceDecorator implements OrderCreateService {

    private final OrderCreateService delegate;

    private final ConcurrentHashMap<String, OrderCreateResponse> responseCache = new ConcurrentHashMap<>();

    /**
     * 创建订单幂等装饰器
     *
     * @param delegate 被装饰的订单创建服务
     */
    public IdempotentOrderCreateServiceDecorator(@Qualifier("auditOrderCreateService") OrderCreateService delegate) {
        this.delegate = delegate;
    }

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单创建响应
     */
    @Override
    public OrderCreateResponse createOrder(OrderCreateRequest request) {
        if (request == null || StrUtil.isBlank(request.requestId())) {
            log.warn("幂等校验失败，请求参数或请求ID为空");
            throw new IllegalArgumentException("请求ID不能为空");
        }

        OrderCreateResponse cachedResponse = responseCache.get(request.requestId());
        if (cachedResponse != null) {
            log.info("命中幂等缓存，requestId：{}，订单号：{}", request.requestId(), cachedResponse.orderNo());
            return cachedResponse;
        }

        OrderCreateResponse response = delegate.createOrder(request);
        responseCache.put(request.requestId(), response);

        log.info("写入幂等缓存，requestId：{}，订单号：{}", request.requestId(), response.orderNo());
        return response;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/OrderCreateController.java`

下面是订单创建接口，用于验证装饰器模式效果。由于 `IdempotentOrderCreateServiceDecorator` 使用了 `@Primary`，这里注入的是最终增强后的装饰器链。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.OrderCreateRequest;
import io.github.atengk.design.dto.OrderCreateResponse;
import io.github.atengk.design.service.OrderCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 订单创建控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/decorator/order")
public class OrderCreateController {

    private final OrderCreateService orderCreateService;

    /**
     * 创建订单
     *
     * @param requestId   请求ID
     * @param userId      用户ID
     * @param productId   商品ID
     * @param productName 商品名称
     * @param quantity    购买数量
     * @param unitPrice   商品单价
     * @return 订单创建响应
     */
    @PostMapping("/create")
    public OrderCreateResponse createOrder(@RequestParam String requestId,
                                           @RequestParam Long userId,
                                           @RequestParam Long productId,
                                           @RequestParam String productName,
                                           @RequestParam Integer quantity,
                                           @RequestParam BigDecimal unitPrice) {
        OrderCreateRequest request = new OrderCreateRequest(
                requestId,
                userId,
                productId,
                productName,
                quantity,
                unitPrice
        );
        return orderCreateService.createOrder(request);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/decorator/order/create?requestId=REQ10001&userId=10001&productId=20001&productName=键盘&quantity=2&unitPrice=199.00"

curl -X POST "http://localhost:8080/decorator/order/create?requestId=REQ10001&userId=10001&productId=20001&productName=键盘&quantity=2&unitPrice=199.00"
```

第一次请求会执行完整下单流程，第二次请求会命中幂等缓存，直接返回第一次的订单结果。

可能返回：

```json
{
  "orderNo": "ORDER2019776866538487808",
  "userId": 10001,
  "totalAmount": 398.00,
  "message": "创建成功"
}
```

这种方式的优点是核心下单逻辑、审计逻辑、幂等逻辑相互独立。后续如果要新增限流、权限校验、风控检查，只需要继续增加装饰器，而不是修改 `BasicOrderCreateService`。

## 扩展一个新装饰器

在 Spring Boot 装饰器模式中，新增增强能力通常只需要新增一个装饰器类，然后调整装饰链。下面以限流装饰器为例，限制同一个 JVM 内最多处理 100 次订单创建请求。

文件位置：`src/main/java/io/github/atengk/design/service/impl/RateLimitOrderCreateServiceDecorator.java`

下面的限流装饰器通过 `AtomicInteger` 演示请求计数，生产环境建议使用 Redis、Sentinel、Bucket4j 或网关限流。

```java
package io.github.atengk.design.service.impl;

import io.github.atengk.design.dto.OrderCreateRequest;
import io.github.atengk.design.dto.OrderCreateResponse;
import io.github.atengk.design.service.OrderCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订单创建限流装饰器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Primary
@Service("rateLimitOrderCreateService")
public class RateLimitOrderCreateServiceDecorator implements OrderCreateService {

    private static final int MAX_REQUEST_COUNT = 100;

    private final AtomicInteger requestCounter = new AtomicInteger(0);

    private final OrderCreateService delegate;

    /**
     * 创建订单限流装饰器
     *
     * @param delegate 被装饰的订单创建服务
     */
    public RateLimitOrderCreateServiceDecorator(@Qualifier("idempotentOrderCreateService") OrderCreateService delegate) {
        this.delegate = delegate;
    }

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单创建响应
     */
    @Override
    public OrderCreateResponse createOrder(OrderCreateRequest request) {
        int currentCount = requestCounter.incrementAndGet();

        if (currentCount > MAX_REQUEST_COUNT) {
            log.warn("订单创建请求触发限流，当前请求次数：{}，最大请求次数：{}", currentCount, MAX_REQUEST_COUNT);
            throw new IllegalStateException("请求过于频繁，请稍后再试");
        }

        log.info("订单创建限流校验通过，当前请求次数：{}", currentCount);
        return delegate.createOrder(request);
    }
}
```

新增该装饰器后，调用链路会变成：

```text
Controller
    -> RateLimitOrderCreateServiceDecorator
        -> IdempotentOrderCreateServiceDecorator
            -> AuditOrderCreateServiceDecorator
                -> BasicOrderCreateService
```

这里需要注意：如果多个装饰器都标记了 `@Primary`，Spring 会因为主 Bean 不唯一而启动失败。因此新增 `RateLimitOrderCreateServiceDecorator` 后，需要移除 `IdempotentOrderCreateServiceDecorator` 上的 `@Primary`，只保留最外层装饰器的 `@Primary`。

调整后：

```text
BasicOrderCreateService                    不加 @Primary
AuditOrderCreateServiceDecorator           不加 @Primary
IdempotentOrderCreateServiceDecorator      不加 @Primary
RateLimitOrderCreateServiceDecorator       加 @Primary
```

## 装饰器模式和代理模式的区别

装饰器模式和代理模式在代码结构上都可能表现为“持有一个同接口对象，然后调用它”，但二者意图不同。

| 对比项     | 装饰器模式                         | 代理模式                               |
| ---------- | ---------------------------------- | -------------------------------------- |
| 核心目的   | 增强对象能力                       | 控制对象访问                           |
| 关注点     | 叠加功能                           | 访问控制、延迟加载、远程调用、权限控制 |
| 组合方式   | 可以多层叠加                       | 通常代理一个目标对象                   |
| 调用方感知 | 通常仍然面向同一接口               | 通常不关心目标对象是否真实存在         |
| 常见场景   | 日志、审计、缓存、重试、加密、限流 | AOP、RPC、权限代理、懒加载代理         |

简单理解：

```text
装饰器模式：原功能还在，我给它加点能力。
代理模式：你不能直接访问目标对象，需要通过我访问。
```

在 Spring Boot 中，AOP 更偏代理模式；而手写服务包装增强，更偏装饰器模式。

## 装饰器模式和 AOP 的选择

Spring Boot 项目中，很多增强能力也可以通过 AOP 实现。装饰器模式和 AOP 的选择可以按业务边界判断。

| 场景                                 | 推荐方式   |
| ------------------------------------ | ---------- |
| 针对某个接口或某类业务服务做明确增强 | 装饰器模式 |
| 针对大量方法统一做横切增强           | AOP        |
| 增强链路需要明确排序和组合           | 装饰器模式 |
| 日志、权限、事务、监控等通用横切能力 | AOP        |
| 需要按接口替换原始实现               | 装饰器模式 |
| 需要通过注解声明增强点               | AOP        |

例如订单创建中的幂等、审计、限流，如果只针对 `OrderCreateService`，使用装饰器模式会更直观。如果要对所有 Controller 统一记录访问日志，使用 AOP 更合适。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行第一次订单创建：

```bash
curl -X POST "http://localhost:8080/decorator/order/create?requestId=REQ10001&userId=10001&productId=20001&productName=键盘&quantity=2&unitPrice=199.00"
```

执行第二次重复订单创建：

```bash
curl -X POST "http://localhost:8080/decorator/order/create?requestId=REQ10001&userId=10001&productId=20001&productName=键盘&quantity=2&unitPrice=199.00"
```

如果装饰器链路正常，可以看到类似日志：

```text
记录下单审计开始，requestId：REQ10001，用户ID：10001，商品ID：20001
执行基础下单逻辑，订单号：ORDER2019776866538487808，用户ID：10001，商品ID：20001，金额：398.00
记录下单审计完成，requestId：REQ10001，订单号：ORDER2019776866538487808，结果：创建成功
写入幂等缓存，requestId：REQ10001，订单号：ORDER2019776866538487808
命中幂等缓存，requestId：REQ10001，订单号：ORDER2019776866538487808
```

如果第二次请求返回的 `orderNo` 和第一次一致，说明幂等装饰器已经生效。

也可以使用不同 `requestId` 发起新请求：

```bash
curl -X POST "http://localhost:8080/decorator/order/create?requestId=REQ10002&userId=10001&productId=20001&productName=键盘&quantity=2&unitPrice=199.00"
```

此时会重新创建一个新订单。

## 注意事项

装饰器模式适合叠加增强能力，但不要把核心业务逻辑全部分散到装饰器中。原始组件应该仍然表达清晰的核心职责，装饰器只做增强。

不推荐把装饰器写成这样：

```java
@Override
public OrderCreateResponse createOrder(OrderCreateRequest request) {
    // 校验参数
    // 计算金额
    // 扣减库存
    // 保存订单
    // 审计日志
    // 幂等控制
    // 限流控制
    // 消息通知
    return null;
}
```

推荐将核心逻辑和增强逻辑分开：

```java
OrderCreateService basicService = new BasicOrderCreateService();
OrderCreateService auditService = new AuditOrderCreateServiceDecorator(basicService);
OrderCreateService idempotentService = new IdempotentOrderCreateServiceDecorator(auditService);
```

在 Spring Boot 中使用装饰器时，要明确 Bean 的注入链路，避免循环依赖。

错误示例：

```java
@Service
public class AuditOrderCreateServiceDecorator implements OrderCreateService {

    public AuditOrderCreateServiceDecorator(OrderCreateService orderCreateService) {
        // 如果没有 @Qualifier，可能注入自己或注入错误的实现
    }
}
```

推荐使用明确的 Bean 名称：

```java
public AuditOrderCreateServiceDecorator(@Qualifier("basicOrderCreateService") OrderCreateService delegate) {
    this.delegate = delegate;
}
```

如果多个装饰器都实现同一个接口，通常只给最外层装饰器添加 `@Primary`。

```java
@Primary
@Service("idempotentOrderCreateService")
public class IdempotentOrderCreateServiceDecorator implements OrderCreateService {
}
```

Spring 单例 Bean 中不要保存请求级状态。装饰器也是 Spring 单例，成员变量会被多个请求共享。

错误示例：

```java
private String currentRequestId;
private String currentOrderNo;
private BigDecimal currentTotalAmount;
```

推荐使用局部变量、方法参数或线程安全组件。

```java
public OrderCreateResponse createOrder(OrderCreateRequest request) {
    String requestId = request.requestId();
    return delegate.createOrder(request);
}
```

如果幂等、限流、缓存等增强能力需要支持分布式部署，不要使用本地 `ConcurrentHashMap` 或 `AtomicInteger` 作为最终方案。生产环境应使用 Redis、数据库唯一索引、分布式锁、网关限流或专用中间件。

## 总结

在 JDK21 和 Spring Boot 3 项目中，装饰器模式的实践重点是增强已有对象能力，同时不污染核心业务实现。

普通 Java 装饰器适合无依赖的对象增强。Spring Boot 装饰器适合对某个服务接口做明确增强，例如审计、幂等、限流、缓存、重试、脱敏等。对于这些场景，推荐使用“接口 + 原始实现 + 多个装饰器 + 最外层 `@Primary`”的结构。

装饰器模式不是为了替代所有 AOP，也不是为了把业务拆得越碎越好。它适合在增强链路明确、增强对象明确、增强顺序重要的场景中使用。
