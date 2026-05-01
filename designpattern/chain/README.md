# 设计模式：责任链模式

责任链模式用于把一个请求沿着一组处理器依次传递，每个处理器只负责自己关心的处理逻辑，处理完成后再交给下一个处理器。在 JDK21 和 Spring Boot 3 项目中，责任链模式常用于参数校验、订单提交校验、风控规则、审批流、权限校验、接口过滤、文件导入校验、消息消费前置处理等场景。

需要注意：责任链模式关注的是“多个处理步骤按顺序执行”。如果是从多个算法中选择一个执行，更适合策略模式；如果是固定流程由父类控制，更适合模板方法模式；如果是简化多个子系统调用，更适合外观模式。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证责任链模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、集合、ID、金额等通用处理 -->
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

责任链模式的核心目标是把多个处理步骤拆成多个独立处理器，避免一个方法中堆积大量 `if else`、校验逻辑和流程控制逻辑。

常见角色如下：

| 角色            | 说明                                           |
| --------------- | ---------------------------------------------- |
| Handler         | 抽象处理器，定义统一处理方法                   |
| ConcreteHandler | 具体处理器，只负责一个明确职责                 |
| Chain           | 责任链，负责组织多个处理器的执行顺序           |
| Request         | 请求上下文，携带处理器需要的数据               |
| Client          | 调用方，只调用责任链入口，不直接调用多个处理器 |

常见实现方式如下：

| 实现方式             | 是否推荐         | 适用场景                                 |
| -------------------- | ---------------- | ---------------------------------------- |
| 链表式责任链         | 推荐用于理解原理 | 每个处理器持有下一个处理器               |
| List 顺序责任链      | 推荐             | 按顺序遍历多个处理器                     |
| Spring Bean 责任链   | 强烈推荐         | Spring Boot 项目中的规则校验、风控、审批 |
| Servlet Filter Chain | 框架内置         | Web 请求过滤链                           |
| 大量 `if else`       | 不推荐           | 处理步骤增多后维护成本高                 |

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring Bean 责任链 > List 顺序责任链 > 链表式责任链 > 大量 if else
```

责任链模式通常有两种处理方式：

```text
第一种：每个处理器处理完后继续向后传递。
第二种：某个处理器满足条件后中断链路，不再继续处理。
```

在业务校验场景中，通常通过抛出异常中断链路。例如订单金额不合法时，金额校验处理器直接抛出异常，后续处理器不再执行。

## 普通 Java 责任链

普通 Java 责任链适合不依赖 Spring 容器的本地流程处理。下面以账号登录校验为例，一个登录请求需要经过参数校验、账号状态校验、密码校验三个处理器。

整体流程如下：

```text
登录请求 -> 参数校验 -> 账号状态校验 -> 密码校验 -> 登录通过
```

### 文件结构

```text
src/main/java/io/github/atengk/design/chain/simple/
├── LoginRequest.java
├── AbstractLoginCheckHandler.java
├── LoginParamCheckHandler.java
├── AccountStatusCheckHandler.java
├── PasswordCheckHandler.java
└── LoginCheckChainBuilder.java
```

文件位置：`src/main/java/io/github/atengk/design/chain/simple/LoginRequest.java`

下面是登录请求对象。

```java
package io.github.atengk.design.chain.simple;

/**
 * 登录请求
 *
 * @param username 用户名
 * @param password 密码
 * @author Ateng
 * @since 2026-04-30
 */
public record LoginRequest(String username, String password) {
}
```

文件位置：`src/main/java/io/github/atengk/design/chain/simple/AbstractLoginCheckHandler.java`

下面是登录校验抽象处理器。它持有下一个处理器，并提供 `next` 方法把请求继续向后传递。

```java
package io.github.atengk.design.chain.simple;

/**
 * 登录校验抽象处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public abstract class AbstractLoginCheckHandler {

    private AbstractLoginCheckHandler nextHandler;

    /**
     * 设置下一个处理器
     *
     * @param nextHandler 下一个处理器
     * @return 下一个处理器
     */
    public AbstractLoginCheckHandler setNext(AbstractLoginCheckHandler nextHandler) {
        this.nextHandler = nextHandler;
        return nextHandler;
    }

    /**
     * 执行校验
     *
     * @param request 登录请求
     */
    public abstract void check(LoginRequest request);

    /**
     * 执行下一个处理器
     *
     * @param request 登录请求
     */
    protected void next(LoginRequest request) {
        if (nextHandler != null) {
            nextHandler.check(request);
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/chain/simple/LoginParamCheckHandler.java`

下面是登录参数校验处理器，负责校验用户名和密码是否为空。

```java
package io.github.atengk.design.chain.simple;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录参数校验处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class LoginParamCheckHandler extends AbstractLoginCheckHandler {

    /**
     * 执行校验
     *
     * @param request 登录请求
     */
    @Override
    public void check(LoginRequest request) {
        if (request == null) {
            log.warn("登录校验失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.hasBlank(request.username(), request.password())) {
            log.warn("登录校验失败，用户名或密码为空");
            throw new IllegalArgumentException("用户名和密码不能为空");
        }

        log.info("登录参数校验通过，用户名：{}", request.username());
        next(request);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/chain/simple/AccountStatusCheckHandler.java`

下面是账号状态校验处理器，负责校验账号是否可用。示例中 `locked` 用户表示账号被锁定。

```java
package io.github.atengk.design.chain.simple;

import lombok.extern.slf4j.Slf4j;

/**
 * 账号状态校验处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class AccountStatusCheckHandler extends AbstractLoginCheckHandler {

    /**
     * 执行校验
     *
     * @param request 登录请求
     */
    @Override
    public void check(LoginRequest request) {
        if ("locked".equalsIgnoreCase(request.username())) {
            log.warn("登录校验失败，账号已锁定，用户名：{}", request.username());
            throw new IllegalStateException("账号已锁定");
        }

        log.info("账号状态校验通过，用户名：{}", request.username());
        next(request);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/chain/simple/PasswordCheckHandler.java`

下面是密码校验处理器，负责校验密码是否正确。示例中固定正确密码为 `123456`。

```java
package io.github.atengk.design.chain.simple;

import lombok.extern.slf4j.Slf4j;

/**
 * 密码校验处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class PasswordCheckHandler extends AbstractLoginCheckHandler {

    private static final String CORRECT_PASSWORD = "123456";

    /**
     * 执行校验
     *
     * @param request 登录请求
     */
    @Override
    public void check(LoginRequest request) {
        if (!CORRECT_PASSWORD.equals(request.password())) {
            log.warn("登录校验失败，密码错误，用户名：{}", request.username());
            throw new IllegalArgumentException("用户名或密码错误");
        }

        log.info("密码校验通过，用户名：{}", request.username());
        next(request);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/chain/simple/LoginCheckChainBuilder.java`

下面是责任链构建器，用于组装登录校验链。

```java
package io.github.atengk.design.chain.simple;

/**
 * 登录校验责任链构建器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class LoginCheckChainBuilder {

    private LoginCheckChainBuilder() {
    }

    /**
     * 构建登录校验责任链
     *
     * @return 登录校验责任链头节点
     */
    public static AbstractLoginCheckHandler build() {
        AbstractLoginCheckHandler paramCheckHandler = new LoginParamCheckHandler();
        AbstractLoginCheckHandler accountStatusCheckHandler = new AccountStatusCheckHandler();
        AbstractLoginCheckHandler passwordCheckHandler = new PasswordCheckHandler();

        paramCheckHandler
                .setNext(accountStatusCheckHandler)
                .setNext(passwordCheckHandler);

        return paramCheckHandler;
    }
}
```

使用方式：

```java
AbstractLoginCheckHandler loginCheckChain = LoginCheckChainBuilder.build();

loginCheckChain.check(new LoginRequest("Ateng", "123456"));
```

如果参数、账号状态、密码都校验通过，责任链执行完成。如果中间任意处理器抛出异常，后续处理器不会继续执行。

## Spring Boot 责任链

Spring Boot 项目中更常见的写法，是把每个处理器注册为 Spring Bean，然后由上下文类按顺序组织处理器列表。下面以订单提交校验为例，提交订单前需要经过参数校验、用户校验、库存校验、金额校验等多个步骤。

整体流程如下：

```text
提交订单 -> 参数校验 -> 用户校验 -> 库存校验 -> 金额校验 -> 创建订单
```

示例中的处理器职责如下：

```text
OrderParamCheckHandler      校验请求参数
UserOrderCheckHandler       校验用户是否可下单
InventoryCheckHandler       校验库存是否充足
AmountCheckHandler          校验订单金额是否合理
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── ChainApplication.java
├── controller/
│   └── OrderSubmitController.java
├── context/
│   └── OrderSubmitCheckChain.java
├── dto/
│   ├── OrderSubmitRequest.java
│   └── OrderSubmitResponse.java
├── handler/
│   ├── OrderCheckHandler.java
│   ├── OrderParamCheckHandler.java
│   ├── UserOrderCheckHandler.java
│   ├── InventoryCheckHandler.java
│   └── AmountCheckHandler.java
└── service/
    ├── OrderSubmitService.java
    └── impl/
        └── OrderSubmitServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/ChainApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 责任链模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class ChainApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ChainApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderSubmitRequest.java`

下面是订单提交请求对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 订单提交请求
 *
 * @param userId      用户ID
 * @param productId   商品ID
 * @param productName 商品名称
 * @param quantity    购买数量
 * @param unitPrice   商品单价
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderSubmitRequest(
        Long userId,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderSubmitResponse.java`

下面是订单提交响应对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 订单提交响应
 *
 * @param orderNo     订单号
 * @param userId      用户ID
 * @param productName 商品名称
 * @param quantity    购买数量
 * @param totalAmount 订单总金额
 * @param message     结果消息
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderSubmitResponse(
        String orderNo,
        Long userId,
        String productName,
        Integer quantity,
        BigDecimal totalAmount,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/handler/OrderCheckHandler.java`

下面是订单提交校验处理器接口。每个处理器通过 `order` 方法声明执行顺序。

```java
package io.github.atengk.design.handler;

import io.github.atengk.design.dto.OrderSubmitRequest;

/**
 * 订单校验处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderCheckHandler {

    /**
     * 获取处理器顺序
     *
     * @return 顺序值，越小越先执行
     */
    int order();

    /**
     * 执行订单校验
     *
     * @param request 订单提交请求
     */
    void check(OrderSubmitRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/handler/OrderParamCheckHandler.java`

下面是订单参数校验处理器，负责基础参数合法性校验。

```java
package io.github.atengk.design.handler;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.OrderSubmitRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 订单参数校验处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderParamCheckHandler implements OrderCheckHandler {

    /**
     * 获取处理器顺序
     *
     * @return 顺序值
     */
    @Override
    public int order() {
        return 100;
    }

    /**
     * 执行订单校验
     *
     * @param request 订单提交请求
     */
    @Override
    public void check(OrderSubmitRequest request) {
        if (request == null) {
            log.warn("订单参数校验失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (request.userId() == null || request.userId() <= 0) {
            log.warn("订单参数校验失败，用户ID不合法，用户ID：{}", request.userId());
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        if (request.productId() == null || request.productId() <= 0) {
            log.warn("订单参数校验失败，商品ID不合法，商品ID：{}", request.productId());
            throw new IllegalArgumentException("商品ID必须大于0");
        }

        if (StrUtil.isBlank(request.productName())) {
            log.warn("订单参数校验失败，商品名称为空");
            throw new IllegalArgumentException("商品名称不能为空");
        }

        if (request.quantity() == null || request.quantity() <= 0) {
            log.warn("订单参数校验失败，购买数量不合法，购买数量：{}", request.quantity());
            throw new IllegalArgumentException("购买数量必须大于0");
        }

        if (request.unitPrice() == null || request.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("订单参数校验失败，商品单价不合法，商品单价：{}", request.unitPrice());
            throw new IllegalArgumentException("商品单价必须大于0");
        }

        log.info("订单参数校验通过，用户ID：{}，商品ID：{}", request.userId(), request.productId());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/handler/UserOrderCheckHandler.java`

下面是用户下单校验处理器，负责判断用户是否允许下单。示例中用户 ID 为 `99999` 表示黑名单用户。

```java
package io.github.atengk.design.handler;

import io.github.atengk.design.dto.OrderSubmitRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户下单校验处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class UserOrderCheckHandler implements OrderCheckHandler {

    private static final Long BLACK_USER_ID = 99999L;

    /**
     * 获取处理器顺序
     *
     * @return 顺序值
     */
    @Override
    public int order() {
        return 200;
    }

    /**
     * 执行订单校验
     *
     * @param request 订单提交请求
     */
    @Override
    public void check(OrderSubmitRequest request) {
        if (BLACK_USER_ID.equals(request.userId())) {
            log.warn("用户下单校验失败，命中黑名单，用户ID：{}", request.userId());
            throw new IllegalStateException("当前用户不允许下单");
        }

        log.info("用户下单校验通过，用户ID：{}", request.userId());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/handler/InventoryCheckHandler.java`

下面是库存校验处理器，负责判断商品库存是否充足。示例中模拟库存为 100。

```java
package io.github.atengk.design.handler;

import io.github.atengk.design.dto.OrderSubmitRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 库存校验处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class InventoryCheckHandler implements OrderCheckHandler {

    private static final int MOCK_STOCK = 100;

    /**
     * 获取处理器顺序
     *
     * @return 顺序值
     */
    @Override
    public int order() {
        return 300;
    }

    /**
     * 执行订单校验
     *
     * @param request 订单提交请求
     */
    @Override
    public void check(OrderSubmitRequest request) {
        if (request.quantity() > MOCK_STOCK) {
            log.warn("库存校验失败，商品ID：{}，购买数量：{}，当前库存：{}",
                    request.productId(), request.quantity(), MOCK_STOCK);
            throw new IllegalStateException("商品库存不足");
        }

        log.info("库存校验通过，商品ID：{}，购买数量：{}，当前库存：{}",
                request.productId(), request.quantity(), MOCK_STOCK);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/handler/AmountCheckHandler.java`

下面是金额校验处理器，负责判断订单金额是否在允许范围内。

```java
package io.github.atengk.design.handler;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.design.dto.OrderSubmitRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额校验处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class AmountCheckHandler implements OrderCheckHandler {

    private static final BigDecimal MAX_ORDER_AMOUNT = BigDecimal.valueOf(10000);

    /**
     * 获取处理器顺序
     *
     * @return 顺序值
     */
    @Override
    public int order() {
        return 400;
    }

    /**
     * 执行订单校验
     *
     * @param request 订单提交请求
     */
    @Override
    public void check(OrderSubmitRequest request) {
        BigDecimal totalAmount = NumberUtil.mul(request.unitPrice(), BigDecimal.valueOf(request.quantity()))
                .setScale(2, RoundingMode.HALF_UP);

        if (totalAmount.compareTo(MAX_ORDER_AMOUNT) > 0) {
            log.warn("金额校验失败，订单金额超过上限，用户ID：{}，金额：{}，上限：{}",
                    request.userId(), totalAmount, MAX_ORDER_AMOUNT);
            throw new IllegalStateException("订单金额超过上限");
        }

        log.info("金额校验通过，用户ID：{}，订单金额：{}", request.userId(), totalAmount);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/context/OrderSubmitCheckChain.java`

下面是订单提交校验责任链。它接收所有 `OrderCheckHandler` Bean，按顺序排序后依次执行。

```java
package io.github.atengk.design.context;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.design.dto.OrderSubmitRequest;
import io.github.atengk.design.handler.OrderCheckHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 订单提交校验责任链
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderSubmitCheckChain {

    private final List<OrderCheckHandler> handlers;

    /**
     * 创建订单提交校验责任链
     *
     * @param handlers 订单校验处理器列表
     */
    public OrderSubmitCheckChain(List<OrderCheckHandler> handlers) {
        if (CollUtil.isEmpty(handlers)) {
            log.warn("订单校验处理器列表为空");
            this.handlers = List.of();
            return;
        }

        this.handlers = handlers.stream()
                .sorted(Comparator.comparingInt(OrderCheckHandler::order))
                .toList();

        log.info("初始化订单提交校验责任链，处理器数量：{}", this.handlers.size());
    }

    /**
     * 执行订单提交校验
     *
     * @param request 订单提交请求
     */
    public void check(OrderSubmitRequest request) {
        for (OrderCheckHandler handler : handlers) {
            log.debug("开始执行订单校验处理器：{}", handler.getClass().getSimpleName());
            handler.check(request);
        }

        log.info("订单提交校验责任链执行完成，用户ID：{}，商品ID：{}", request.userId(), request.productId());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/OrderSubmitService.java`

下面是订单提交服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.OrderSubmitRequest;
import io.github.atengk.design.dto.OrderSubmitResponse;

/**
 * 订单提交服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderSubmitService {

    /**
     * 提交订单
     *
     * @param request 订单提交请求
     * @return 订单提交响应
     */
    OrderSubmitResponse submit(OrderSubmitRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/OrderSubmitServiceImpl.java`

下面是订单提交服务实现。核心逻辑执行前，先调用责任链完成所有校验。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import io.github.atengk.design.context.OrderSubmitCheckChain;
import io.github.atengk.design.dto.OrderSubmitRequest;
import io.github.atengk.design.dto.OrderSubmitResponse;
import io.github.atengk.design.service.OrderSubmitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 订单提交服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSubmitServiceImpl implements OrderSubmitService {

    private final OrderSubmitCheckChain orderSubmitCheckChain;

    /**
     * 提交订单
     *
     * @param request 订单提交请求
     * @return 订单提交响应
     */
    @Override
    public OrderSubmitResponse submit(OrderSubmitRequest request) {
        orderSubmitCheckChain.check(request);

        String orderNo = "ORDER" + IdUtil.getSnowflakeNextId();
        BigDecimal totalAmount = NumberUtil.mul(request.unitPrice(), BigDecimal.valueOf(request.quantity()))
                .setScale(2, RoundingMode.HALF_UP);

        log.info("提交订单成功，订单号：{}，用户ID：{}，商品ID：{}，金额：{}",
                orderNo, request.userId(), request.productId(), totalAmount);

        return new OrderSubmitResponse(
                orderNo,
                request.userId(),
                request.productName(),
                request.quantity(),
                totalAmount,
                "提交成功"
        );
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/OrderSubmitController.java`

下面是订单提交接口，用于验证责任链模式效果。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.OrderSubmitRequest;
import io.github.atengk.design.dto.OrderSubmitResponse;
import io.github.atengk.design.service.OrderSubmitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 订单提交控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/chain/order")
public class OrderSubmitController {

    private final OrderSubmitService orderSubmitService;

    /**
     * 提交订单
     *
     * @param userId      用户ID
     * @param productId   商品ID
     * @param productName 商品名称
     * @param quantity    购买数量
     * @param unitPrice   商品单价
     * @return 订单提交响应
     */
    @PostMapping("/submit")
    public OrderSubmitResponse submit(@RequestParam Long userId,
                                      @RequestParam Long productId,
                                      @RequestParam String productName,
                                      @RequestParam Integer quantity,
                                      @RequestParam BigDecimal unitPrice) {
        OrderSubmitRequest request = new OrderSubmitRequest(
                userId,
                productId,
                productName,
                quantity,
                unitPrice
        );
        return orderSubmitService.submit(request);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/chain/order/submit?userId=10001&productId=20001&productName=键盘&quantity=2&unitPrice=199.00"
```

可能返回：

```json
{
  "orderNo": "ORDER2019776866538487808",
  "userId": 10001,
  "productName": "键盘",
  "quantity": 2,
  "totalAmount": 398.00,
  "message": "提交成功"
}
```

这种方式的优点是每个校验处理器职责独立。后续新增风控校验、优惠校验、会员等级校验时，只需要新增处理器 Bean，不需要修改原有处理器逻辑。

## 扩展一个新处理器

在 Spring Boot 责任链模式中，新增处理器通常只需要新增一个实现类。下面以风控校验为例，订单数量超过 50 时认为需要人工审核，直接中断下单流程。

文件位置：`src/main/java/io/github/atengk/design/handler/RiskCheckHandler.java`

下面的风控处理器会被 Spring 自动扫描，并自动加入 `OrderSubmitCheckChain`。

```java
package io.github.atengk.design.handler;

import io.github.atengk.design.dto.OrderSubmitRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 风控校验处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class RiskCheckHandler implements OrderCheckHandler {

    private static final int RISK_QUANTITY_THRESHOLD = 50;

    /**
     * 获取处理器顺序
     *
     * @return 顺序值
     */
    @Override
    public int order() {
        return 350;
    }

    /**
     * 执行订单校验
     *
     * @param request 订单提交请求
     */
    @Override
    public void check(OrderSubmitRequest request) {
        if (request.quantity() > RISK_QUANTITY_THRESHOLD) {
            log.warn("风控校验失败，购买数量过大，用户ID：{}，商品ID：{}，购买数量：{}",
                    request.userId(), request.productId(), request.quantity());
            throw new IllegalStateException("订单触发风控，请人工审核");
        }

        log.info("风控校验通过，用户ID：{}，商品ID：{}，购买数量：{}",
                request.userId(), request.productId(), request.quantity());
    }
}
```

新增后，责任链顺序变为：

```text
100 OrderParamCheckHandler
200 UserOrderCheckHandler
300 InventoryCheckHandler
350 RiskCheckHandler
400 AmountCheckHandler
```

调用方和 `OrderSubmitServiceImpl` 不需要修改。只要新增的处理器注册为 Spring Bean，就会自动参与责任链执行。

## 中断链路和继续链路

责任链模式的一个重点是链路是否继续向后执行。常见方式有两种。

第一种是校验型责任链。处理器失败时抛出异常，中断链路；处理器成功时继续向后执行。

```java
public void check(OrderSubmitRequest request) {
    if (request.quantity() <= 0) {
        throw new IllegalArgumentException("购买数量必须大于0");
    }
}
```

这种方式适合参数校验、权限校验、风控校验、库存校验等场景。

第二种是命中型责任链。某个处理器能够处理请求时直接返回结果，不再继续向后传递。

```java
public boolean support(String type) {
    return "vip".equals(type);
}
```

这种方式适合审批流、规则匹配、异常处理、消息路由等场景。

在 Spring Boot 业务项目中，校验型责任链更常见。因为多个校验项通常都需要依次执行，只要有一个失败就应该中断主流程。

## 责任链模式和策略模式的区别

责任链模式和策略模式都可以拆分多个处理类，但关注点不同。

| 对比项   | 责任链模式                       | 策略模式                     |
| -------- | -------------------------------- | ---------------------------- |
| 核心目的 | 多个处理器按顺序处理同一个请求   | 从多个策略中选择一个执行     |
| 调用关系 | 一对多，通常多个处理器都会执行   | 一对一，通常只执行一个策略   |
| 关注点   | 处理顺序、链路中断、步骤拆分     | 算法替换、规则选择           |
| 典型场景 | 参数校验、风控链、审批链、过滤链 | 优惠计算、支付渠道、物流计费 |
| 扩展方式 | 新增处理器并设置顺序             | 新增策略并声明支持类型       |

简单理解：

```text
责任链模式：一个请求要经过多个关卡。
策略模式：一个请求从多个算法中选一个。
```

订单提交前依次执行参数校验、用户校验、库存校验、风控校验，适合责任链模式。订单优惠从满减、折扣、新人优惠中选择一种，适合策略模式。

## 责任链模式和模板方法模式的区别

责任链模式和模板方法模式都能表达流程，但结构不同。

| 对比项       | 责任链模式             | 模板方法模式               |
| ------------ | ---------------------- | -------------------------- |
| 流程组织方式 | 多个处理器组合成链     | 父类定义固定流程           |
| 扩展方式     | 新增处理器             | 新增子类或覆盖步骤         |
| 是否依赖继承 | 通常不依赖             | 依赖抽象类继承             |
| 步骤顺序     | 可通过配置或排序调整   | 通常由父类固定             |
| 典型场景     | 校验链、过滤链、风控链 | 文件导入模板、订单处理模板 |

简单理解：

```text
责任链模式：流程由多个独立处理器组成，可以灵活增减。
模板方法模式：流程由父类固定，子类只实现变化步骤。
```

如果流程步骤需要经常增减、排序、组合，责任链模式更灵活。如果流程稳定，只是部分步骤实现不同，模板方法模式更合适。

## 责任链模式和过滤器链的关系

Web 开发中的过滤器链是责任链模式的典型应用。比如 Servlet Filter、Spring Security Filter Chain 都体现了责任链思想。

典型过滤器链如下：

```text
HTTP 请求
    -> CorsFilter
    -> AuthenticationFilter
    -> AuthorizationFilter
    -> LogFilter
    -> Controller
```

每个过滤器只处理自己关心的逻辑，并决定是否继续放行。

业务责任链和 Web 过滤器链的区别在于：

| 对比项       | 业务责任链                     | Web 过滤器链           |
| ------------ | ------------------------------ | ---------------------- |
| 处理对象     | 业务请求对象                   | HTTP 请求和响应        |
| 使用位置     | Service、Facade、Domain 层     | Web 入口层             |
| 典型职责     | 参数校验、风控、审批、规则处理 | 鉴权、跨域、日志、限流 |
| 是否框架内置 | 通常手写                       | 通常框架提供           |

订单校验链属于业务责任链，不建议直接用 Servlet Filter 实现。Filter 更适合处理 HTTP 层通用逻辑。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行正常订单提交：

```bash
curl -X POST "http://localhost:8080/chain/order/submit?userId=10001&productId=20001&productName=键盘&quantity=2&unitPrice=199.00"
```

如果责任链正常执行，可以看到类似日志：

```text
初始化订单提交校验责任链，处理器数量：5
订单参数校验通过，用户ID：10001，商品ID：20001
用户下单校验通过，用户ID：10001
库存校验通过，商品ID：20001，购买数量：2，当前库存：100
风控校验通过，用户ID：10001，商品ID：20001，购买数量：2
金额校验通过，用户ID：10001，订单金额：398.00
订单提交校验责任链执行完成，用户ID：10001，商品ID：20001
提交订单成功，订单号：ORDER2019776866538487808，用户ID：10001，商品ID：20001，金额：398.00
```

执行黑名单用户请求：

```bash
curl -X POST "http://localhost:8080/chain/order/submit?userId=99999&productId=20001&productName=键盘&quantity=2&unitPrice=199.00"
```

异常日志示例：

```text
订单参数校验通过，用户ID：99999，商品ID：20001
用户下单校验失败，命中黑名单，用户ID：99999
```

此时后续库存校验、风控校验、金额校验不会继续执行。

执行库存不足请求：

```bash
curl -X POST "http://localhost:8080/chain/order/submit?userId=10001&productId=20001&productName=键盘&quantity=101&unitPrice=199.00"
```

异常日志示例：

```text
库存校验失败，商品ID：20001，购买数量：101，当前库存：100
```

实际项目中建议结合全局异常处理器，将业务异常转换成统一响应结构。

## 注意事项

责任链模式适合拆分多个顺序处理步骤，但不要为了拆分类而拆分类。处理器数量过多、职责边界不清晰时，责任链会变得难以排查。

推荐每个处理器只做一类明确事情：

```text
参数校验处理器
用户校验处理器
库存校验处理器
风控校验处理器
金额校验处理器
```

不推荐一个处理器同时处理多类职责：

```java
public class OrderCheckHandler implements OrderCheckHandler {

    public void check(OrderSubmitRequest request) {
        // 校验参数
        // 查用户
        // 查库存
        // 算金额
        // 做风控
        // 发通知
    }
}
```

责任链的顺序必须明确。不要依赖 Spring Bean 的默认加载顺序。推荐通过 `order()`、`@Order` 或配置文件明确声明顺序。

推荐写法：

```java
@Override
public int order() {
    return 300;
}
```

如果处理器之间存在强依赖，需要谨慎设计。例如金额校验依赖参数校验已经完成，那么参数校验必须排在金额校验之前。

责任链中不要把请求级状态保存到处理器成员变量中。Spring Bean 默认是单例，成员变量会被多个请求共享。

错误示例：

```java
private Long currentUserId;
private Long currentProductId;
private BigDecimal currentAmount;
```

推荐使用方法参数、局部变量或上下文对象：

```java
public void check(OrderSubmitRequest request) {
    BigDecimal totalAmount = NumberUtil.mul(request.unitPrice(), BigDecimal.valueOf(request.quantity()));
}
```

如果链路中需要多个处理器共享中间结果，可以设计一个上下文对象，而不是把中间状态放到处理器字段中。

示例：

```java
public record OrderCheckContext(
        OrderSubmitRequest request,
        Map<String, Object> attributes
) {
}
```

生产环境中，如果责任链处理的是下单、支付、审批等核心流程，需要考虑事务边界和异常补偿。责任链只能拆分步骤，不自动保证事务一致性。

常见生产关注点包括：

```text
链路顺序是否稳定
处理器异常是否中断主流程
是否需要记录每个处理器执行结果
是否需要支持灰度规则
是否需要支持动态开关
是否需要异步处理
是否需要事务回滚或补偿
```

如果责任链规则需要频繁由运营配置调整，可以考虑把处理器启停状态、顺序、规则参数放到数据库或配置中心，但处理器本身仍然保持单一职责。

## 总结

在 JDK21 和 Spring Boot 3 项目中，责任链模式的实践重点是把一个复杂请求拆成多个独立处理器，并按明确顺序执行。

普通 Java 链表式责任链适合理解原理。Spring Boot 项目中更推荐使用“处理器接口 + 多个处理器 Bean + 责任链上下文排序执行”的结构。对于订单提交校验、风控规则、审批流程、导入校验、权限校验等场景，责任链模式可以显著降低主流程复杂度。

责任链模式不是为了替代所有流程编排，而是为了处理“一个请求需要经过多个处理步骤，并且步骤可能扩展、排序或中断”的场景。设计时需要重点控制处理器职责、执行顺序、异常中断和上下文状态，避免责任链演变成难以追踪的隐式流程。
