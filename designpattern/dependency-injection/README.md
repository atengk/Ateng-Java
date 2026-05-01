# 设计模式：依赖注入模式

依赖注入模式用于把对象依赖的创建和装配交给外部容器或调用方处理，而不是在对象内部主动 `new` 依赖对象。在 JDK21 和 Spring Boot 3 项目中，依赖注入是 Spring IoC 的核心思想，常用于 Controller 注入 Service、Service 注入 Repository、业务服务注入策略实现、配置对象注入、外部客户端注入、测试替身注入等场景。

需要注意：依赖注入模式不是 GoF 23 种设计模式之一，属于这次设计模式文档里的“遗漏补充”。它是 Spring Boot 项目中最基础、最高频、最重要的工程模式之一，优先级通常高于大部分传统设计模式。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证依赖注入效果 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、ID、集合等通用处理 -->
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

    <!-- Spring Boot 测试依赖，用于单元测试和 Bean 注入验证 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用 Spring Boot 3，建议使用 JDK17 及以上版本。当前文档以 JDK21 为基准，示例代码可以直接用于 Spring Boot 3 项目。

## 核心概念

依赖注入的核心目标是让对象不再自己创建依赖，而是由外部把依赖传进来。这样可以降低类之间的耦合，提高可测试性、可替换性和扩展性。

没有依赖注入时，代码通常写成：

```java
public class OrderService {

    private final SmsNoticeSender sender = new SmsNoticeSender();
}
```

这种写法的问题是：`OrderService` 被固定绑定到 `SmsNoticeSender`，后续要换成邮件、站内信、Mock 对象、远程通知客户端都不方便。

使用依赖注入后，代码通常写成：

```java
public class OrderService {

    private final NoticeSender sender;

    public OrderService(NoticeSender sender) {
        this.sender = sender;
    }
}
```

依赖对象由外部传入，`OrderService` 只依赖抽象接口，不依赖具体实现。

常见角色如下：

| 角色                 | 说明                                         |
| -------------------- | -------------------------------------------- |
| Client               | 需要依赖其他对象的业务类                     |
| Dependency           | 被依赖对象，例如 Service、Repository、Sender |
| Injector / Container | 注入器或容器，负责创建和装配对象             |
| Interface            | 抽象接口，用于隔离具体实现                   |
| Implementation       | 具体实现类，由容器管理并注入                 |

在 Spring Boot 项目中，常见依赖注入方式如下：

| 注入方式              | 推荐程度 | 说明                                |
| --------------------- | -------- | ----------------------------------- |
| 构造方法注入          | 高       | 推荐默认使用，依赖不可变，便于测试  |
| Setter 注入           | 中       | 适合可选依赖或后置配置              |
| 字段注入              | 低       | 不推荐，测试困难，依赖不清晰        |
| `ObjectProvider` 注入 | 中       | 适合可选 Bean、延迟获取、多实现选择 |
| `List` / `Map` 注入   | 高       | 适合策略集合、处理器集合、插件集合  |

推荐优先级：

```text
构造方法注入 > List / Map 注入 > ObjectProvider 注入 > Setter 注入 > 字段注入
```

## 普通 Java 依赖注入

普通 Java 中不依赖 Spring 容器，也可以手动实现依赖注入。下面以订单创建后发送通知为例，订单服务依赖通知发送器，但不关心具体发送方式。

整体关系如下：

```text
OrderService
    -> NoticeSender

NoticeSender
    -> SmsNoticeSender
    -> EmailNoticeSender
```

### 文件结构

```text
src/main/java/io/github/atengk/design/di/simple/
├── NoticeSender.java
├── SmsNoticeSender.java
├── EmailNoticeSender.java
└── OrderService.java
```

文件位置：`src/main/java/io/github/atengk/design/di/simple/NoticeSender.java`

下面是通知发送器接口，订单服务只依赖该接口。

```java
package io.github.atengk.design.di.simple;

/**
 * 通知发送器
 *
 * @author Ateng
 * @since 2026-05-01
 */
public interface NoticeSender {

    /**
     * 发送通知
     *
     * @param receiver 接收人
     * @param content  通知内容
     * @return 发送结果
     */
    String send(String receiver, String content);
}
```

文件位置：`src/main/java/io/github/atengk/design/di/simple/SmsNoticeSender.java`

下面是短信通知发送器实现。

```java
package io.github.atengk.design.di.simple;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 短信通知发送器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
public class SmsNoticeSender implements NoticeSender {

    /**
     * 发送短信通知
     *
     * @param receiver 接收人
     * @param content  通知内容
     * @return 发送结果
     */
    @Override
    public String send(String receiver, String content) {
        if (StrUtil.hasBlank(receiver, content)) {
            log.warn("短信发送失败，接收人或内容为空");
            throw new IllegalArgumentException("接收人和内容不能为空");
        }

        String bizId = "SMS" + IdUtil.getSnowflakeNextId();
        log.info("短信发送成功，接收人：{}，业务ID：{}", receiver, bizId);
        return bizId;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/di/simple/EmailNoticeSender.java`

下面是邮件通知发送器实现。

```java
package io.github.atengk.design.di.simple;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 邮件通知发送器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
public class EmailNoticeSender implements NoticeSender {

    /**
     * 发送邮件通知
     *
     * @param receiver 接收人
     * @param content  通知内容
     * @return 发送结果
     */
    @Override
    public String send(String receiver, String content) {
        if (StrUtil.hasBlank(receiver, content)) {
            log.warn("邮件发送失败，接收人或内容为空");
            throw new IllegalArgumentException("接收人和内容不能为空");
        }

        String bizId = "EMAIL" + IdUtil.getSnowflakeNextId();
        log.info("邮件发送成功，接收人：{}，业务ID：{}", receiver, bizId);
        return bizId;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/di/simple/OrderService.java`

下面是订单服务。它通过构造方法接收 `NoticeSender`，这就是最基础的构造方法注入。

```java
package io.github.atengk.design.di.simple;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 订单服务
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
public class OrderService {

    private final NoticeSender noticeSender;

    /**
     * 创建订单服务
     *
     * @param noticeSender 通知发送器
     */
    public OrderService(NoticeSender noticeSender) {
        if (noticeSender == null) {
            throw new IllegalArgumentException("通知发送器不能为空");
        }

        this.noticeSender = noticeSender;
    }

    /**
     * 创建订单
     *
     * @param userId   用户ID
     * @param receiver 通知接收人
     * @return 订单号
     */
    public String createOrder(Long userId, String receiver) {
        if (userId == null || userId <= 0 || StrUtil.isBlank(receiver)) {
            log.warn("创建订单失败，用户ID或接收人不合法，用户ID：{}，接收人：{}", userId, receiver);
            throw new IllegalArgumentException("用户ID和接收人不能为空");
        }

        String orderNo = "ORDER" + IdUtil.getSnowflakeNextId();
        log.info("创建订单成功，订单号：{}，用户ID：{}", orderNo, userId);

        noticeSender.send(receiver, StrUtil.format("订单 {} 创建成功", orderNo));
        return orderNo;
    }
}
```

使用方式：

```java
NoticeSender smsSender = new SmsNoticeSender();
OrderService smsOrderService = new OrderService(smsSender);
smsOrderService.createOrder(10001L, "13800138000");

NoticeSender emailSender = new EmailNoticeSender();
OrderService emailOrderService = new OrderService(emailSender);
emailOrderService.createOrder(10002L, "ateng@example.com");
```

这里 `OrderService` 没有修改代码，就可以切换短信或邮件通知。这就是依赖注入的核心价值：依赖从内部创建变成外部传入。

## Spring Boot 构造方法注入

Spring Boot 项目中，依赖注入由 Spring 容器自动完成。推荐使用构造方法注入，让依赖关系在对象创建时就确定下来。

下面以订单创建接口为例，`OrderCreateServiceImpl` 依赖 `OrderNoticeSender`。由于存在多个通知发送器实现，所以使用 `@Qualifier` 明确指定默认注入短信发送器。

整体流程如下：

```text
Controller
    -> OrderCreateService
        -> OrderNoticeSender
            -> SmsOrderNoticeSender
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── DependencyInjectionApplication.java
├── controller/
│   └── OrderController.java
├── dto/
│   ├── OrderCreateRequest.java
│   └── OrderCreateResponse.java
├── sender/
│   ├── OrderNoticeSender.java
│   ├── SmsOrderNoticeSender.java
│   └── EmailOrderNoticeSender.java
└── service/
    ├── OrderCreateService.java
    └── impl/
        └── OrderCreateServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/DependencyInjectionApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 依赖注入模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-01
 */
@SpringBootApplication
public class DependencyInjectionApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DependencyInjectionApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderCreateRequest.java`

下面是订单创建请求对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 订单创建请求
 *
 * @param userId      用户ID
 * @param receiver    通知接收人
 * @param productName 商品名称
 * @param amount      订单金额
 * @author Ateng
 * @since 2026-05-01
 */
public record OrderCreateRequest(
        Long userId,
        String receiver,
        String productName,
        BigDecimal amount
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderCreateResponse.java`

下面是订单创建响应对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 订单创建响应
 *
 * @param orderNo     订单号
 * @param userId      用户ID
 * @param productName 商品名称
 * @param amount      订单金额
 * @param noticeBizId 通知业务ID
 * @param message     响应消息
 * @author Ateng
 * @since 2026-05-01
 */
public record OrderCreateResponse(
        String orderNo,
        Long userId,
        String productName,
        BigDecimal amount,
        String noticeBizId,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/sender/OrderNoticeSender.java`

下面是订单通知发送器接口。

```java
package io.github.atengk.design.sender;

/**
 * 订单通知发送器
 *
 * @author Ateng
 * @since 2026-05-01
 */
public interface OrderNoticeSender {

    /**
     * 获取通知渠道
     *
     * @return 通知渠道
     */
    String channel();

    /**
     * 发送订单通知
     *
     * @param receiver 接收人
     * @param content  通知内容
     * @return 通知业务ID
     */
    String send(String receiver, String content);
}
```

文件位置：`src/main/java/io/github/atengk/design/sender/SmsOrderNoticeSender.java`

下面是短信订单通知发送器。Bean 名默认是 `smsOrderNoticeSender`。

```java
package io.github.atengk.design.sender;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 短信订单通知发送器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class SmsOrderNoticeSender implements OrderNoticeSender {

    /**
     * 获取通知渠道
     *
     * @return 通知渠道
     */
    @Override
    public String channel() {
        return "sms";
    }

    /**
     * 发送短信订单通知
     *
     * @param receiver 接收人
     * @param content  通知内容
     * @return 通知业务ID
     */
    @Override
    public String send(String receiver, String content) {
        if (StrUtil.hasBlank(receiver, content)) {
            log.warn("短信订单通知发送失败，接收人或内容为空");
            throw new IllegalArgumentException("接收人和内容不能为空");
        }

        String bizId = "SMS" + IdUtil.getSnowflakeNextId();
        log.info("短信订单通知发送成功，接收人：{}，业务ID：{}", receiver, bizId);
        return bizId;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/sender/EmailOrderNoticeSender.java`

下面是邮件订单通知发送器。Bean 名默认是 `emailOrderNoticeSender`。

```java
package io.github.atengk.design.sender;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 邮件订单通知发送器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class EmailOrderNoticeSender implements OrderNoticeSender {

    /**
     * 获取通知渠道
     *
     * @return 通知渠道
     */
    @Override
    public String channel() {
        return "email";
    }

    /**
     * 发送邮件订单通知
     *
     * @param receiver 接收人
     * @param content  通知内容
     * @return 通知业务ID
     */
    @Override
    public String send(String receiver, String content) {
        if (StrUtil.hasBlank(receiver, content)) {
            log.warn("邮件订单通知发送失败，接收人或内容为空");
            throw new IllegalArgumentException("接收人和内容不能为空");
        }

        String bizId = "EMAIL" + IdUtil.getSnowflakeNextId();
        log.info("邮件订单通知发送成功，接收人：{}，业务ID：{}", receiver, bizId);
        return bizId;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/OrderCreateService.java`

下面是订单创建服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.OrderCreateRequest;
import io.github.atengk.design.dto.OrderCreateResponse;

/**
 * 订单创建服务
 *
 * @author Ateng
 * @since 2026-05-01
 */
public interface OrderCreateService {

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单创建响应
     */
    OrderCreateResponse create(OrderCreateRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/OrderCreateServiceImpl.java`

下面是订单创建服务实现。它通过构造方法注入指定的订单通知发送器。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.OrderCreateRequest;
import io.github.atengk.design.dto.OrderCreateResponse;
import io.github.atengk.design.sender.OrderNoticeSender;
import io.github.atengk.design.service.OrderCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 订单创建服务实现
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Service
public class OrderCreateServiceImpl implements OrderCreateService {

    private final OrderNoticeSender orderNoticeSender;

    /**
     * 创建订单创建服务
     *
     * @param orderNoticeSender 订单通知发送器
     */
    public OrderCreateServiceImpl(@Qualifier("smsOrderNoticeSender") OrderNoticeSender orderNoticeSender) {
        this.orderNoticeSender = orderNoticeSender;
    }

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单创建响应
     */
    @Override
    public OrderCreateResponse create(OrderCreateRequest request) {
        validateRequest(request);

        String orderNo = "ORDER" + IdUtil.getSnowflakeNextId();
        log.info("创建订单成功，订单号：{}，用户ID：{}，商品：{}，金额：{}",
                orderNo, request.userId(), request.productName(), request.amount());

        String noticeBizId = orderNoticeSender.send(
                request.receiver(),
                StrUtil.format("订单 {} 创建成功，商品：{}", orderNo, request.productName())
        );

        log.info("订单创建后通知发送完成，订单号：{}，通知渠道：{}，通知业务ID：{}",
                orderNo, orderNoticeSender.channel(), noticeBizId);

        return new OrderCreateResponse(
                orderNo,
                request.userId(),
                request.productName(),
                request.amount(),
                noticeBizId,
                "订单创建成功"
        );
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

        if (request.userId() == null || request.userId() <= 0) {
            log.warn("创建订单失败，用户ID不合法，用户ID：{}", request.userId());
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        if (StrUtil.hasBlank(request.receiver(), request.productName())) {
            log.warn("创建订单失败，接收人或商品名称为空");
            throw new IllegalArgumentException("接收人和商品名称不能为空");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("创建订单失败，订单金额不合法，金额：{}", request.amount());
            throw new IllegalArgumentException("订单金额必须大于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/OrderController.java`

下面是订单接口，用于验证构造方法注入效果。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.OrderCreateRequest;
import io.github.atengk.design.dto.OrderCreateResponse;
import io.github.atengk.design.service.OrderCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 订单控制器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/dependency-injection/order")
public class OrderController {

    private final OrderCreateService orderCreateService;

    /**
     * 创建订单
     *
     * @param userId      用户ID
     * @param receiver    通知接收人
     * @param productName 商品名称
     * @param amount      订单金额
     * @return 订单创建响应
     */
    @PostMapping("/create")
    public OrderCreateResponse create(@RequestParam Long userId,
                                      @RequestParam String receiver,
                                      @RequestParam String productName,
                                      @RequestParam BigDecimal amount) {
        OrderCreateRequest request = new OrderCreateRequest(userId, receiver, productName, amount);
        return orderCreateService.create(request);
    }
}
```

## 多实现注入

当一个接口有多个实现时，Spring Boot 项目中常见处理方式有三种：`@Qualifier` 指定 Bean、`@Primary` 指定默认 Bean、注入 `List` 或 `Map` 后按业务类型选择。

### `@Qualifier` 指定实现

当一个接口存在多个 Bean 时，直接按接口注入会报错。可以使用 `@Qualifier` 指定 Bean 名。

```java
public OrderCreateServiceImpl(@Qualifier("smsOrderNoticeSender") OrderNoticeSender orderNoticeSender) {
    this.orderNoticeSender = orderNoticeSender;
}
```

这种方式适合某个类明确只需要一个固定实现。

### `@Primary` 指定默认实现

如果某个实现是默认实现，可以在实现类上加 `@Primary`。

```java
@Primary
@Component
public class SmsOrderNoticeSender implements OrderNoticeSender {
}
```

这样按接口注入时，Spring 会优先选择 `SmsOrderNoticeSender`。但如果默认实现不够明确，建议使用 `@Qualifier`，避免后续维护人员误解。

### `List` 注入全部实现

当业务需要广播给所有实现时，可以直接注入 `List<OrderNoticeSender>`。

文件位置：`src/main/java/io/github/atengk/design/service/OrderNoticeBroadcastService.java`

下面是订单通知广播服务接口。

```java
package io.github.atengk.design.service;

/**
 * 订单通知广播服务
 *
 * @author Ateng
 * @since 2026-05-01
 */
public interface OrderNoticeBroadcastService {

    /**
     * 广播订单通知
     *
     * @param receiver 接收人
     * @param content  通知内容
     */
    void broadcast(String receiver, String content);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/OrderNoticeBroadcastServiceImpl.java`

下面是订单通知广播服务实现。它通过构造方法注入所有 `OrderNoticeSender` 实现。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.sender.OrderNoticeSender;
import io.github.atengk.design.service.OrderNoticeBroadcastService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单通知广播服务实现
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Service
public class OrderNoticeBroadcastServiceImpl implements OrderNoticeBroadcastService {

    private final List<OrderNoticeSender> orderNoticeSenders;

    /**
     * 创建订单通知广播服务
     *
     * @param orderNoticeSenders 订单通知发送器列表
     */
    public OrderNoticeBroadcastServiceImpl(List<OrderNoticeSender> orderNoticeSenders) {
        this.orderNoticeSenders = orderNoticeSenders;
    }

    /**
     * 广播订单通知
     *
     * @param receiver 接收人
     * @param content  通知内容
     */
    @Override
    public void broadcast(String receiver, String content) {
        if (StrUtil.hasBlank(receiver, content)) {
            log.warn("广播订单通知失败，接收人或内容为空");
            throw new IllegalArgumentException("接收人和内容不能为空");
        }

        if (CollUtil.isEmpty(orderNoticeSenders)) {
            log.warn("广播订单通知失败，通知发送器列表为空");
            return;
        }

        for (OrderNoticeSender sender : orderNoticeSenders) {
            String bizId = sender.send(receiver, content);
            log.info("广播订单通知成功，渠道：{}，业务ID：{}", sender.channel(), bizId);
        }
    }
}
```

这种方式适合插件集合、处理器集合、监听器集合、导出器集合等场景。

### `Map` 注入全部实现

如果需要按 Bean 名选择实现，可以注入 `Map<String, OrderNoticeSender>`。Map 的 key 默认是 Bean 名。

文件位置：`src/main/java/io/github/atengk/design/service/impl/OrderNoticeSelector.java`

下面是订单通知选择器。它通过 Map 注入所有通知发送器，并按 Bean 名选择。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.sender.OrderNoticeSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单通知选择器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class OrderNoticeSelector {

    private final Map<String, OrderNoticeSender> senderMap;

    /**
     * 创建订单通知选择器
     *
     * @param senderMap 通知发送器Map
     */
    public OrderNoticeSelector(Map<String, OrderNoticeSender> senderMap) {
        this.senderMap = senderMap;
        log.info("初始化订单通知选择器完成，Bean名称：{}", senderMap.keySet());
    }

    /**
     * 根据Bean名称选择通知发送器
     *
     * @param beanName Bean名称
     * @return 通知发送器
     */
    public OrderNoticeSender selectByBeanName(String beanName) {
        if (StrUtil.isBlank(beanName)) {
            log.warn("选择订单通知发送器失败，Bean名称为空");
            throw new IllegalArgumentException("Bean名称不能为空");
        }

        OrderNoticeSender sender = senderMap.get(beanName);
        if (sender == null) {
            log.warn("选择订单通知发送器失败，Bean不存在，Bean名称：{}", beanName);
            throw new IllegalArgumentException("通知发送器不存在：" + beanName);
        }

        return sender;
    }
}
```

如果希望按业务渠道选择，例如 `sms`、`email`，建议自己转换为 `channel -> sender`，不要依赖 Bean 名作为业务标识。

## 配置对象注入

依赖注入不只适用于 Service，也适用于配置对象。Spring Boot 推荐使用 `@ConfigurationProperties` 注入配置，而不是在业务代码中到处使用 `@Value`。

文件位置：`src/main/resources/application.yml`

下面是订单通知配置。

```yaml
order:
  notice:
    # 是否开启订单通知
    enabled: true
    # 默认通知渠道
    default-channel: sms
    # 通知标题前缀
    title-prefix: "订单通知"
```

文件位置：`src/main/java/io/github/atengk/design/config/OrderNoticeProperties.java`

下面是订单通知配置属性类。

```java
package io.github.atengk.design.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 订单通知配置属性
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Data
@Component
@ConfigurationProperties(prefix = "order.notice")
public class OrderNoticeProperties {

    /**
     * 是否开启订单通知
     */
    private Boolean enabled = true;

    /**
     * 默认通知渠道
     */
    private String defaultChannel = "sms";

    /**
     * 通知标题前缀
     */
    private String titlePrefix = "订单通知";
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/ConfigurableOrderNoticeService.java`

下面是使用配置对象的订单通知服务。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.config.OrderNoticeProperties;
import io.github.atengk.design.sender.OrderNoticeSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 可配置订单通知服务
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Service
public class ConfigurableOrderNoticeService {

    private final OrderNoticeProperties properties;
    private final Map<String, OrderNoticeSender> senderMap;

    /**
     * 创建可配置订单通知服务
     *
     * @param properties 订单通知配置
     * @param senders    订单通知发送器列表
     */
    public ConfigurableOrderNoticeService(OrderNoticeProperties properties, List<OrderNoticeSender> senders) {
        this.properties = properties;
        this.senderMap = senders.stream()
                .collect(Collectors.toUnmodifiableMap(OrderNoticeSender::channel, Function.identity()));

        log.info("初始化可配置订单通知服务完成，默认渠道：{}，可用渠道：{}",
                properties.getDefaultChannel(), senderMap.keySet());
    }

    /**
     * 发送默认渠道通知
     *
     * @param receiver 接收人
     * @param orderNo  订单号
     * @return 通知业务ID
     */
    public String sendDefaultNotice(String receiver, String orderNo) {
        if (Boolean.FALSE.equals(properties.getEnabled())) {
            log.info("订单通知未开启，跳过发送，订单号：{}", orderNo);
            return "";
        }

        if (StrUtil.hasBlank(receiver, orderNo)) {
            log.warn("发送默认订单通知失败，接收人或订单号为空");
            throw new IllegalArgumentException("接收人和订单号不能为空");
        }

        OrderNoticeSender sender = senderMap.get(properties.getDefaultChannel());
        if (sender == null) {
            log.warn("默认通知渠道不存在，渠道：{}", properties.getDefaultChannel());
            throw new IllegalArgumentException("默认通知渠道不存在：" + properties.getDefaultChannel());
        }

        String content = StrUtil.format("{}：订单 {} 创建成功", properties.getTitlePrefix(), orderNo);
        return sender.send(receiver, content);
    }
}
```

配置对象注入的好处是：配置集中、类型安全、便于测试，也便于后续接入配置中心。

## ObjectProvider 延迟注入

有些依赖不是每次都需要，或者 Bean 可能不存在，可以使用 `ObjectProvider` 做可选注入或延迟获取。

文件位置：`src/main/java/io/github/atengk/design/service/impl/OptionalAuditService.java`

下面是可选审计服务。只有存在审计处理器时才执行审计逻辑。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 可选审计服务
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Service
public class OptionalAuditService {

    private final ObjectProvider<AuditHandler> auditHandlerProvider;

    /**
     * 创建可选审计服务
     *
     * @param auditHandlerProvider 审计处理器提供者
     */
    public OptionalAuditService(ObjectProvider<AuditHandler> auditHandlerProvider) {
        this.auditHandlerProvider = auditHandlerProvider;
    }

    /**
     * 记录操作审计
     *
     * @param bizType 业务类型
     * @param bizNo   业务编号
     */
    public void record(String bizType, String bizNo) {
        if (StrUtil.hasBlank(bizType, bizNo)) {
            log.warn("记录审计失败，业务类型或业务编号为空");
            return;
        }

        AuditHandler auditHandler = auditHandlerProvider.getIfAvailable();
        if (auditHandler == null) {
            log.info("审计处理器不存在，跳过审计，业务类型：{}，业务编号：{}", bizType, bizNo);
            return;
        }

        auditHandler.record(bizType, bizNo);
    }

    /**
     * 审计处理器
     *
     * @author Ateng
     * @since 2026-05-01
     */
    public interface AuditHandler {

        /**
         * 记录审计
         *
         * @param bizType 业务类型
         * @param bizNo   业务编号
         */
        void record(String bizType, String bizNo);
    }
}
```

`ObjectProvider` 适合可选依赖、延迟加载、避免启动阶段过早创建对象等场景。但不要把它当成万能查找器滥用，否则会接近服务定位器模式，降低依赖清晰度。

## 使用方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

调用创建订单接口：

```bash
curl -X POST "http://localhost:8080/dependency-injection/order/create?userId=10001&receiver=13800138000&productName=机械键盘&amount=199.00"
```

可能返回：

```json
{
  "orderNo": "ORDER2020123456789017600",
  "userId": 10001,
  "productName": "机械键盘",
  "amount": 199.00,
  "noticeBizId": "SMS2020123456789017601",
  "message": "订单创建成功"
}
```

如果构造方法注入正常，可以看到类似日志：

```text
创建订单成功，订单号：ORDER2020123456789017600，用户ID：10001，商品：机械键盘，金额：199.00
短信订单通知发送成功，接收人：13800138000，业务ID：SMS2020123456789017601
订单创建后通知发送完成，订单号：ORDER2020123456789017600，通知渠道：sms，通知业务ID：SMS2020123456789017601
```

如果把 `OrderCreateServiceImpl` 构造方法中的 `@Qualifier("smsOrderNoticeSender")` 改成 `@Qualifier("emailOrderNoticeSender")`，通知渠道会从短信切换为邮件，业务服务主体逻辑不需要修改。

## 测试方式

依赖注入能显著提升测试便利性。因为业务类依赖接口，测试时可以注入一个假的实现，不需要真实发送短信或邮件。

文件位置：`src/test/java/io/github/atengk/design/service/OrderCreateServiceTest.java`

下面是订单创建服务的单元测试。测试中手动注入一个假的通知发送器。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.OrderCreateRequest;
import io.github.atengk.design.dto.OrderCreateResponse;
import io.github.atengk.design.sender.OrderNoticeSender;
import io.github.atengk.design.service.impl.OrderCreateServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;

/**
 * 订单创建服务测试
 *
 * @author Ateng
 * @since 2026-05-01
 */
class OrderCreateServiceTest {

    /**
     * 测试创建订单成功
     */
    @Test
    void shouldCreateOrderSuccess() {
        OrderNoticeSender fakeSender = new OrderNoticeSender() {

            /**
             * 获取通知渠道
             *
             * @return 通知渠道
             */
            @Override
            public String channel() {
                return "fake";
            }

            /**
             * 发送订单通知
             *
             * @param receiver 接收人
             * @param content  通知内容
             * @return 通知业务ID
             */
            @Override
            public String send(String receiver, String content) {
                return "FAKE_NOTICE_ID";
            }
        };

        OrderCreateService service = new TestOrderCreateServiceImpl(fakeSender);
        OrderCreateRequest request = new OrderCreateRequest(
                10001L,
                "13800138000",
                "机械键盘",
                BigDecimal.valueOf(199)
        );

        OrderCreateResponse response = service.create(request);

        Assertions.assertNotNull(response.orderNo());
        Assertions.assertEquals("FAKE_NOTICE_ID", response.noticeBizId());
        Assertions.assertEquals("订单创建成功", response.message());
    }

    /**
     * 测试订单创建服务实现，用于绕开Spring容器直接注入假对象
     *
     * @author Ateng
     * @since 2026-05-01
     */
    private static class TestOrderCreateServiceImpl extends OrderCreateServiceImpl {

        /**
         * 创建测试订单创建服务
         *
         * @param sender 通知发送器
         */
        public TestOrderCreateServiceImpl(OrderNoticeSender sender) {
            super(sender);
        }
    }
}
```

上面这个测试展示了依赖注入的直接收益：测试不需要启动短信服务，不需要连接外部系统，只要注入一个测试实现即可验证业务流程。

如果不想继承测试类，也可以把 `OrderCreateServiceImpl` 的构造方法保留为普通 public 构造方法，测试中直接 new。示例中 `@Qualifier` 只影响 Spring 注入，不影响普通 Java 调用。

## 依赖注入和工厂模式的区别

依赖注入和工厂模式都和对象创建有关，但关注点不同。

| 对比项      | 依赖注入模式            | 工厂模式                             |
| ----------- | ----------------------- | ------------------------------------ |
| 核心目的    | 把依赖交给外部注入      | 封装对象创建逻辑                     |
| 使用方式    | 容器或调用方传入依赖    | 调用工厂获取对象                     |
| 控制权      | 依赖由外部控制          | 调用方主动调用工厂                   |
| Spring 对应 | IoC 容器、Bean 注入     | BeanFactory、FactoryBean、自定义工厂 |
| 典型场景    | Service 注入 Repository | 根据类型创建客户端或处理器           |

简单理解：

```text
依赖注入：需要什么，外部给我。
工厂模式：我要什么，去工厂拿。
```

在 Spring 项目中，容器本身就可以看成大型对象工厂，但应用代码更推荐通过依赖注入使用对象，而不是到处主动从容器里取对象。

## 依赖注入和服务定位器的区别

服务定位器模式是调用方主动通过容器或注册表查找依赖。依赖注入是依赖被动传入。二者都能解耦具体实现，但依赖透明度不同。

服务定位器写法：

```java
OrderNoticeSender sender = applicationContext.getBean(OrderNoticeSender.class);
```

依赖注入写法：

```java
public OrderCreateServiceImpl(OrderNoticeSender sender) {
    this.sender = sender;
}
```

| 对比项           | 依赖注入 | 服务定位器 |
| ---------------- | -------- | ---------- |
| 依赖是否显式     | 显式     | 隐式       |
| 测试便利性       | 高       | 较低       |
| 代码可读性       | 高       | 较低       |
| 是否主动查找依赖 | 否       | 是         |
| 推荐程度         | 高       | 低         |

在 Spring Boot 项目中，不推荐在业务代码里频繁使用 `ApplicationContext.getBean()`。这会隐藏真实依赖关系，让测试和维护变困难。

不推荐：

```java
@Component
public class OrderService {

    @Autowired
    private ApplicationContext applicationContext;

    public void createOrder() {
        NoticeSender sender = applicationContext.getBean(NoticeSender.class);
    }
}
```

推荐：

```java
@Service
public class OrderService {

    private final NoticeSender noticeSender;

    public OrderService(NoticeSender noticeSender) {
        this.noticeSender = noticeSender;
    }
}
```

## 常见注入问题

依赖注入虽然基础，但项目中经常出现注入歧义、循环依赖、字段注入滥用、Bean 未注册等问题。

### 多 Bean 注入歧义

如果一个接口有多个实现，按接口注入时会报错：

```text
expected single matching bean but found 2
```

常见解决方式：

```text
使用 @Qualifier 指定 Bean
使用 @Primary 指定默认 Bean
注入 List 或 Map 后自行选择
按业务类型建立上下文选择器
```

### Bean 未注册

如果类没有加 `@Component`、`@Service`、`@Repository`，或者不在启动类扫描路径下，就不会被 Spring 管理。

常见检查点：

```text
类上是否有组件注解
包路径是否在启动类所在包或子包下
是否被条件注解排除
是否是接口而没有实现类
是否创建了多个 ApplicationContext
```

### 循环依赖

循环依赖通常说明模块边界不清晰。例如：

```text
OrderService -> PaymentService -> OrderService
```

推荐解决方式：

```text
重新拆分职责
抽取第三个协调服务
使用事件驱动解耦
抽取领域服务
避免互相调用
```

不建议优先使用 `@Lazy` 掩盖循环依赖。`@Lazy` 可以解决启动问题，但不解决设计问题。

### 字段注入

字段注入常见写法：

```java
@Autowired
private OrderRepository orderRepository;
```

不推荐这种方式。问题包括：

```text
依赖不透明
对象不能直接 new
单元测试不方便
final 不可用
容易隐藏过多依赖
```

推荐构造方法注入：

```java
private final OrderRepository orderRepository;

public OrderService(OrderRepository orderRepository) {
    this.orderRepository = orderRepository;
}
```

如果使用 Lombok，可以用 `@RequiredArgsConstructor` 简化构造方法：

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
}
```

但涉及 `@Qualifier`、复杂构造逻辑或需要明确构造参数时，建议手写构造方法。

## 注意事项

依赖注入是 Spring Boot 项目最基础的工程模式，但不要把它理解成简单的 `@Autowired`。它的重点是控制依赖方向、面向接口编程、提高可替换性和可测试性。

适合使用依赖注入的场景：

```text
Controller 注入 Service
Service 注入 Repository
业务类注入策略接口
配置类注入属性对象
客户端类注入配置和工具类
聚合服务注入多个子服务
测试中注入 Mock 或 Fake 对象
```

不太适合使用依赖注入的场景：

```text
简单值对象
DTO
Entity
临时局部对象
没有外部依赖的工具方法
需要频繁创建的有状态对象
```

不要把所有类都交给 Spring 管理。领域对象、请求对象、响应对象、临时计算对象通常不需要注册为 Bean。

不推荐：

```java
@Component
public class OrderEntity {
}
```

推荐：

```java
public class Order {
}
```

Spring Bean 默认是单例。被注入的 Service、Component、Repository 中不要保存请求级状态。

错误示例：

```java
private Long currentUserId;
private String currentOrderNo;
```

推荐使用方法参数和局部变量：

```java
public OrderCreateResponse create(OrderCreateRequest request) {
    String orderNo = "ORDER" + IdUtil.getSnowflakeNextId();
    return response;
}
```

依赖注入应该尽量依赖接口，而不是直接依赖实现类。

不推荐：

```java
private final SmsOrderNoticeSender smsOrderNoticeSender;
```

推荐：

```java
private final OrderNoticeSender orderNoticeSender;
```

但也不要为了形式强行抽接口。如果某个类没有替换实现、没有测试替身、没有扩展点，直接注入具体类也可以接受。接口应该服务于抽象和扩展，而不是机械分层。

构造方法参数过多通常说明类职责过重。如果一个 Service 构造方法注入了十几个依赖，需要考虑拆分服务或引入中介者、外观、领域服务等结构。

风险示例：

```text
OrderService(
    UserService,
    ProductService,
    InventoryService,
    PaymentService,
    CouponService,
    NoticeService,
    AuditService,
    RiskService,
    DeliveryService
)
```

这种类往往已经变成上帝服务，建议拆分业务用例或抽出协调层。

## 总结

在 JDK21 和 Spring Boot 3 项目中，依赖注入模式的实践重点是让对象不主动创建依赖，而是通过构造方法、配置对象、集合注入或容器注入获得依赖，从而降低耦合、提升可测试性和扩展性。

普通 Java 依赖注入适合理解构造方法注入的本质。Spring Boot 项目中更推荐使用“接口 + 实现类 Bean + 构造方法注入”的结构。对于多实现注入，可以使用 `@Qualifier`、`@Primary`、`List`、`Map` 或上下文选择器。对于配置注入，推荐使用 `@ConfigurationProperties` 绑定配置对象。

依赖注入不是简单使用 `@Autowired`，也不是把所有对象都交给 Spring 管理。它最适合处理“业务类依赖外部协作对象，并且依赖需要可替换、可测试、可组合”的场景。实际落地时，需要重点关注构造方法注入、多实现歧义、循环依赖、字段注入滥用、Bean 扫描范围和单例 Bean 的线程安全问题。
