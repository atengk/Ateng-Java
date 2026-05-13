# 模板方法模式 + 责任链模式

模板方法模式和责任链模式组合使用时，通常用于处理 **主流程固定，但流程中的某一段校验、过滤、审核或处理步骤需要灵活扩展** 的业务场景。

模板方法模式负责固定整体业务流程，保证流程执行顺序稳定；责任链模式负责把流程中的连续处理节点拆成多个独立处理器。这个组合在 Spring Boot 后端项目中非常常见，例如订单创建、支付前校验、审批提交、文件导入校验、风控校验、接口请求过滤等场景。

```text
模板方法模式：固定业务主流程
责任链模式：拆分主流程中的连续校验或处理节点
```

## 适用场景

本示例以“订单创建”为业务场景。订单创建的主流程通常比较稳定：

```text
创建前处理
  -> 构建上下文
      -> 执行订单创建校验链
          -> 生成订单编号
              -> 保存订单
                  -> 创建后处理
```

其中，“订单创建校验链”会随着业务变化不断扩展：

| 校验节点   | 说明         |
| ------ | ---------- |
| 用户状态校验 | 判断用户是否允许下单 |
| 商品库存校验 | 判断商品是否有库存  |
| 订单金额校验 | 判断订单金额是否合法 |
| 风控校验   | 判断订单是否存在风险 |

如果把所有校验逻辑都写在模板方法里，抽象模板会越来越重；如果把所有流程都交给责任链，又会丢失主流程的清晰边界。

使用“模板方法模式 + 责任链模式”后，结构变成：

```text
Controller
  -> Service
      -> 订单创建模板：固定订单创建主流程
          -> 订单创建校验链：执行多个校验节点
              -> 用户状态校验节点
              -> 商品库存校验节点
              -> 订单金额校验节点
              -> 风控校验节点
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

    <!-- Hutool：提供字符串、对象、数字、ID 等常用工具 -->
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
src/main/java/io/github/atengk/pattern/combination/templatechain
├── chain
│   ├── OrderCreateCheckChain.java
│   ├── OrderCreateCheckHandler.java
│   └── impl
│       ├── AmountCheckHandler.java
│       ├── ProductStockCheckHandler.java
│       ├── RiskCheckHandler.java
│       └── UserStatusCheckHandler.java
├── context
│   └── OrderCreateContext.java
├── controller
│   └── OrderCreateController.java
├── dto
│   ├── OrderCreateRequest.java
│   └── OrderCreateResponse.java
├── result
│   └── Result.java
├── service
│   ├── OrderCreateService.java
│   └── impl
│       └── OrderCreateServiceImpl.java
└── template
    ├── AbstractOrderCreateTemplate.java
    └── DefaultOrderCreateTemplate.java
```

## 核心代码

这一部分给出核心实现。重点看两个位置：

```text
AbstractOrderCreateTemplate：模板方法，固定订单创建主流程
OrderCreateCheckChain：责任链，按顺序执行多个校验节点
```

模板方法不关心具体有哪些校验节点，只关心在固定流程中调用校验链。责任链也不关心订单创建完整流程，只负责完成自己那一段连续校验逻辑。

### 订单创建请求对象

订单创建请求对象用于接收订单创建参数。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatechain/dto/OrderCreateRequest.java`

```java
package io.github.atengk.pattern.combination.templatechain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单创建请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderCreateRequest {

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
     * 购买数量
     */
    @NotNull(message = "购买数量不能为空")
    private Integer quantity;

    /**
     * 订单总金额
     */
    @NotNull(message = "订单总金额不能为空")
    @DecimalMin(value = "0.01", message = "订单总金额必须大于0")
    private BigDecimal totalAmount;

    /**
     * 订单备注
     */
    private String remark;
}
```

### 订单创建响应对象

订单创建响应对象用于返回订单编号、用户、商品、金额和订单状态。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatechain/dto/OrderCreateResponse.java`

```java
package io.github.atengk.pattern.combination.templatechain.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单创建响应
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderCreateResponse {

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 订单状态
     */
    private String orderStatus;

    /**
     * 响应消息
     */
    private String message;
}
```

### 订单创建上下文

上下文对象用于在模板方法和责任链之间传递数据。后续如果某个校验节点需要写入中间结果，也可以放到上下文中。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatechain/context/OrderCreateContext.java`

```java
package io.github.atengk.pattern.combination.templatechain.context;

import io.github.atengk.pattern.combination.templatechain.dto.OrderCreateRequest;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单创建上下文
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderCreateContext {

    /**
     * 订单创建请求
     */
    private OrderCreateRequest request;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 扩展属性
     */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
}
```

### 责任链处理器接口

责任链处理器接口定义每个校验节点必须实现的行为。`order()` 用于控制节点执行顺序。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatechain/chain/OrderCreateCheckHandler.java`

```java
package io.github.atengk.pattern.combination.templatechain.chain;

import io.github.atengk.pattern.combination.templatechain.context.OrderCreateContext;

/**
 * 订单创建校验处理器
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderCreateCheckHandler {

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
     * 执行校验
     *
     * @param context 订单创建上下文
     */
    void check(OrderCreateContext context);
}
```

### 订单创建校验链

校验链负责收集所有校验处理器，并按照 `order()` 顺序依次执行。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatechain/chain/OrderCreateCheckChain.java`

```java
package io.github.atengk.pattern.combination.templatechain.chain;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.pattern.combination.templatechain.context.OrderCreateContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 订单创建校验链
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderCreateCheckChain {

    private final List<OrderCreateCheckHandler> handlerList;

    /**
     * 初始化订单创建校验链
     *
     * @param handlerList 校验处理器集合
     */
    public OrderCreateCheckChain(List<OrderCreateCheckHandler> handlerList) {
        if (CollUtil.isEmpty(handlerList)) {
            throw new IllegalStateException("订单创建校验处理器不能为空");
        }

        this.handlerList = handlerList.stream()
                .sorted(Comparator.comparing(OrderCreateCheckHandler::order))
                .toList();

        log.info("订单创建校验链初始化完成，处理器数量：{}", this.handlerList.size());
    }

    /**
     * 执行订单创建校验链
     *
     * @param context 订单创建上下文
     */
    public void check(OrderCreateContext context) {
        for (OrderCreateCheckHandler handler : handlerList) {
            log.info("开始执行订单创建校验节点：{}", handler.name());
            handler.check(context);
            log.info("订单创建校验节点执行通过：{}", handler.name());
        }
    }
}
```

### 用户状态校验节点

用户状态校验节点用于判断用户是否允许下单。示例中用简单规则模拟用户状态，真实项目中可以查询用户服务或用户表。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatechain/chain/impl/UserStatusCheckHandler.java`

```java
package io.github.atengk.pattern.combination.templatechain.chain.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.combination.templatechain.chain.OrderCreateCheckHandler;
import io.github.atengk.pattern.combination.templatechain.context.OrderCreateContext;
import io.github.atengk.pattern.combination.templatechain.dto.OrderCreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户状态校验节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class UserStatusCheckHandler implements OrderCreateCheckHandler {

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
        return "用户状态校验";
    }

    /**
     * 执行用户状态校验
     *
     * @param context 订单创建上下文
     */
    @Override
    public void check(OrderCreateContext context) {
        OrderCreateRequest request = context.getRequest();
        if (ObjectUtil.isNull(request.getUserId())) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        if (request.getUserId() % 10 == 0) {
            throw new IllegalArgumentException("当前用户状态异常，不允许下单");
        }

        log.info("用户状态校验通过，用户ID：{}", request.getUserId());
    }
}
```

### 商品库存校验节点

商品库存校验节点用于判断商品是否有库存。示例中用简单规则模拟库存不足场景。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatechain/chain/impl/ProductStockCheckHandler.java`

```java
package io.github.atengk.pattern.combination.templatechain.chain.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.combination.templatechain.chain.OrderCreateCheckHandler;
import io.github.atengk.pattern.combination.templatechain.context.OrderCreateContext;
import io.github.atengk.pattern.combination.templatechain.dto.OrderCreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 商品库存校验节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class ProductStockCheckHandler implements OrderCreateCheckHandler {

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
        return "商品库存校验";
    }

    /**
     * 执行商品库存校验
     *
     * @param context 订单创建上下文
     */
    @Override
    public void check(OrderCreateContext context) {
        OrderCreateRequest request = context.getRequest();
        if (ObjectUtil.isNull(request.getProductId())) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        if (ObjectUtil.isNull(request.getQuantity()) || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("购买数量必须大于0");
        }

        if (request.getProductId() == 999999L) {
            throw new IllegalArgumentException("商品库存不足");
        }

        log.info("商品库存校验通过，商品ID：{}，购买数量：{}",
                request.getProductId(), request.getQuantity());
    }
}
```

### 订单金额校验节点

订单金额校验节点用于判断订单金额是否合法。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatechain/chain/impl/AmountCheckHandler.java`

```java
package io.github.atengk.pattern.combination.templatechain.chain.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.combination.templatechain.chain.OrderCreateCheckHandler;
import io.github.atengk.pattern.combination.templatechain.context.OrderCreateContext;
import io.github.atengk.pattern.combination.templatechain.dto.OrderCreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 订单金额校验节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class AmountCheckHandler implements OrderCreateCheckHandler {

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
        return "订单金额校验";
    }

    /**
     * 执行订单金额校验
     *
     * @param context 订单创建上下文
     */
    @Override
    public void check(OrderCreateContext context) {
        OrderCreateRequest request = context.getRequest();
        if (ObjectUtil.isNull(request.getTotalAmount())) {
            throw new IllegalArgumentException("订单总金额不能为空");
        }
        if (request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("订单总金额必须大于0");
        }

        BigDecimal actualAmount = NumberUtil.round(request.getTotalAmount(), 2);
        context.getAttributes().put("actualAmount", actualAmount);

        log.info("订单金额校验通过，原始金额：{}，处理后金额：{}",
                request.getTotalAmount(), actualAmount);
    }
}
```

### 风控校验节点

风控校验节点用于判断订单是否存在风险。示例中通过备注关键字模拟风险订单。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatechain/chain/impl/RiskCheckHandler.java`

```java
package io.github.atengk.pattern.combination.templatechain.chain.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.templatechain.chain.OrderCreateCheckHandler;
import io.github.atengk.pattern.combination.templatechain.context.OrderCreateContext;
import io.github.atengk.pattern.combination.templatechain.dto.OrderCreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单风控校验节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class RiskCheckHandler implements OrderCreateCheckHandler {

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
        return "订单风控校验";
    }

    /**
     * 执行订单风控校验
     *
     * @param context 订单创建上下文
     */
    @Override
    public void check(OrderCreateContext context) {
        OrderCreateRequest request = context.getRequest();
        String remark = StrUtil.lowerCase(StrUtil.nullToEmpty(request.getRemark()));

        if (StrUtil.contains(remark, "risk")) {
            throw new IllegalArgumentException("订单触发风控规则，不允许创建");
        }

        log.info("订单风控校验通过，用户ID：{}，商品ID：{}",
                request.getUserId(), request.getProductId());
    }
}
```

### 抽象订单创建模板

抽象模板类固定订单创建主流程。`create` 方法就是模板方法，定义完整的订单创建步骤。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatechain/template/AbstractOrderCreateTemplate.java`

```java
package io.github.atengk.pattern.combination.templatechain.template;

import io.github.atengk.pattern.combination.templatechain.chain.OrderCreateCheckChain;
import io.github.atengk.pattern.combination.templatechain.context.OrderCreateContext;
import io.github.atengk.pattern.combination.templatechain.dto.OrderCreateRequest;
import io.github.atengk.pattern.combination.templatechain.dto.OrderCreateResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象订单创建模板
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public abstract class AbstractOrderCreateTemplate {

    private final OrderCreateCheckChain orderCreateCheckChain;

    /**
     * 初始化订单创建模板
     *
     * @param orderCreateCheckChain 订单创建校验链
     */
    protected AbstractOrderCreateTemplate(OrderCreateCheckChain orderCreateCheckChain) {
        this.orderCreateCheckChain = orderCreateCheckChain;
    }

    /**
     * 创建订单，固定订单创建主流程
     *
     * @param request 订单创建请求
     * @return 订单创建响应
     */
    public final OrderCreateResponse create(OrderCreateRequest request) {
        beforeCreate(request);

        OrderCreateContext context = buildContext(request);

        executeCheckChain(context);

        String orderNo = generateOrderNo(context);
        context.setOrderNo(orderNo);

        OrderCreateResponse response = saveOrder(context);

        afterCreate(context, response);

        return response;
    }

    /**
     * 创建前处理
     *
     * @param request 订单创建请求
     */
    protected void beforeCreate(OrderCreateRequest request) {
        log.info("开始创建订单，用户ID：{}，商品ID：{}",
                request.getUserId(), request.getProductId());
    }

    /**
     * 构建订单创建上下文
     *
     * @param request 订单创建请求
     * @return 订单创建上下文
     */
    protected OrderCreateContext buildContext(OrderCreateRequest request) {
        return OrderCreateContext.builder()
                .request(request)
                .build();
    }

    /**
     * 执行订单创建校验链
     *
     * @param context 订单创建上下文
     */
    protected void executeCheckChain(OrderCreateContext context) {
        orderCreateCheckChain.check(context);
    }

    /**
     * 生成订单编号
     *
     * @param context 订单创建上下文
     * @return 订单编号
     */
    protected abstract String generateOrderNo(OrderCreateContext context);

    /**
     * 保存订单
     *
     * @param context 订单创建上下文
     * @return 订单创建响应
     */
    protected abstract OrderCreateResponse saveOrder(OrderCreateContext context);

    /**
     * 创建后处理
     *
     * @param context  订单创建上下文
     * @param response 订单创建响应
     */
    protected void afterCreate(OrderCreateContext context, OrderCreateResponse response) {
        log.info("订单创建完成，订单编号：{}，订单状态：{}",
                response.getOrderNo(), response.getOrderStatus());
    }
}
```

### 默认订单创建模板

默认订单创建模板实现订单编号生成和订单保存逻辑。示例中不连接数据库，只模拟保存订单。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatechain/template/DefaultOrderCreateTemplate.java`

```java
package io.github.atengk.pattern.combination.templatechain.template;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import io.github.atengk.pattern.combination.templatechain.chain.OrderCreateCheckChain;
import io.github.atengk.pattern.combination.templatechain.context.OrderCreateContext;
import io.github.atengk.pattern.combination.templatechain.dto.OrderCreateRequest;
import io.github.atengk.pattern.combination.templatechain.dto.OrderCreateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 默认订单创建模板
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class DefaultOrderCreateTemplate extends AbstractOrderCreateTemplate {

    /**
     * 初始化默认订单创建模板
     *
     * @param orderCreateCheckChain 订单创建校验链
     */
    public DefaultOrderCreateTemplate(OrderCreateCheckChain orderCreateCheckChain) {
        super(orderCreateCheckChain);
    }

    /**
     * 生成订单编号
     *
     * @param context 订单创建上下文
     * @return 订单编号
     */
    @Override
    protected String generateOrderNo(OrderCreateContext context) {
        String orderNo = "OD" + IdUtil.getSnowflakeNextIdStr();
        log.info("订单编号生成完成，订单编号：{}", orderNo);
        return orderNo;
    }

    /**
     * 保存订单
     *
     * @param context 订单创建上下文
     * @return 订单创建响应
     */
    @Override
    protected OrderCreateResponse saveOrder(OrderCreateContext context) {
        OrderCreateRequest request = context.getRequest();
        BigDecimal actualAmount = (BigDecimal) context.getAttributes()
                .getOrDefault("actualAmount", NumberUtil.round(request.getTotalAmount(), 2));

        log.info("模拟保存订单，订单编号：{}，用户ID：{}，商品ID：{}，订单金额：{}",
                context.getOrderNo(), request.getUserId(), request.getProductId(), actualAmount);

        return OrderCreateResponse.builder()
                .orderNo(context.getOrderNo())
                .userId(request.getUserId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalAmount(actualAmount)
                .orderStatus("CREATED")
                .message("订单创建成功")
                .build();
    }

    /**
     * 创建后处理
     *
     * @param context  订单创建上下文
     * @param response 订单创建响应
     */
    @Override
    protected void afterCreate(OrderCreateContext context, OrderCreateResponse response) {
        super.afterCreate(context, response);
        log.info("执行订单创建后置处理，订单编号：{}，用户ID：{}",
                response.getOrderNo(), response.getUserId());
    }
}
```

### Service 接口

Service 接口对外暴露订单创建能力。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatechain/service/OrderCreateService.java`

```java
package io.github.atengk.pattern.combination.templatechain.service;

import io.github.atengk.pattern.combination.templatechain.dto.OrderCreateRequest;
import io.github.atengk.pattern.combination.templatechain.dto.OrderCreateResponse;

/**
 * 订单创建服务接口
 *
 * @author Ateng
 * @since 2026-05-13
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

### Service 实现类

Service 实现类只负责调用订单创建模板，不直接编排校验节点。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatechain/service/impl/OrderCreateServiceImpl.java`

```java
package io.github.atengk.pattern.combination.templatechain.service.impl;

import io.github.atengk.pattern.combination.templatechain.dto.OrderCreateRequest;
import io.github.atengk.pattern.combination.templatechain.dto.OrderCreateResponse;
import io.github.atengk.pattern.combination.templatechain.service.OrderCreateService;
import io.github.atengk.pattern.combination.templatechain.template.DefaultOrderCreateTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单创建服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreateServiceImpl implements OrderCreateService {

    private final DefaultOrderCreateTemplate orderCreateTemplate;

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单创建响应
     */
    @Override
    public OrderCreateResponse create(OrderCreateRequest request) {
        OrderCreateResponse response = orderCreateTemplate.create(request);
        log.info("订单创建服务处理完成，订单编号：{}", response.getOrderNo());
        return response;
    }
}
```

### 统一响应对象

统一响应对象用于包装接口返回数据。实际项目中可以复用公共模块中的统一响应类。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatechain/result/Result.java`

```java
package io.github.atengk.pattern.combination.templatechain.result;

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

### Controller

Controller 提供订单创建接口。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatechain/controller/OrderCreateController.java`

```java
package io.github.atengk.pattern.combination.templatechain.controller;

import io.github.atengk.pattern.combination.templatechain.dto.OrderCreateRequest;
import io.github.atengk.pattern.combination.templatechain.dto.OrderCreateResponse;
import io.github.atengk.pattern.combination.templatechain.result.Result;
import io.github.atengk.pattern.combination.templatechain.service.OrderCreateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单创建控制器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order-create")
public class OrderCreateController {

    private final OrderCreateService orderCreateService;

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单创建响应
     */
    @PostMapping
    public Result<OrderCreateResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        return Result.success(orderCreateService.create(request));
    }
}
```

## 使用方式

启动 Spring Boot 项目后，可以通过订单创建接口验证模板方法和责任链是否同时生效。

接口信息如下：

```text
请求地址：POST /api/order-create
Content-Type：application/json
```

正常订单创建请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-create" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "productId": 2001,
    "quantity": 2,
    "totalAmount": 199.99,
    "remark": "正常订单"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "OD1988267712300011520",
    "userId": 1001,
    "productId": 2001,
    "quantity": 2,
    "totalAmount": 199.99,
    "orderStatus": "CREATED",
    "message": "订单创建成功"
  }
}
```

库存不足请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-create" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "productId": 999999,
    "quantity": 1,
    "totalAmount": 99.99,
    "remark": "库存不足测试"
  }'
```

预期结果：

```json
{
  "code": 400,
  "message": "商品库存不足",
  "data": null
}
```

风控拦截请求示例：

```bash
curl -X POST "http://localhost:8080/api/order-create" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "productId": 2001,
    "quantity": 1,
    "totalAmount": 99.99,
    "remark": "risk order"
  }'
```

预期结果：

```json
{
  "code": 400,
  "message": "订单触发风控规则，不允许创建",
  "data": null
}
```

如果当前项目还没有全局异常处理器，可以复用前面章节的 `GlobalExceptionHandler`，把 `IllegalArgumentException` 统一转换成 `Result.fail(400, message)`。

## 新增校验节点

当业务新增一个校验规则时，例如“优惠券校验”，不需要修改订单创建模板，也不需要修改已有校验节点，只需要新增一个责任链处理器。

文件位置：`src/main/java/io/github/atengk/pattern/combination/templatechain/chain/impl/CouponCheckHandler.java`

```java
package io.github.atengk.pattern.combination.templatechain.chain.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.templatechain.chain.OrderCreateCheckHandler;
import io.github.atengk.pattern.combination.templatechain.context.OrderCreateContext;
import io.github.atengk.pattern.combination.templatechain.dto.OrderCreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 优惠券校验节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class CouponCheckHandler implements OrderCreateCheckHandler {

    /**
     * 执行顺序
     *
     * @return 执行顺序
     */
    @Override
    public int order() {
        return 35;
    }

    /**
     * 处理器名称
     *
     * @return 处理器名称
     */
    @Override
    public String name() {
        return "优惠券校验";
    }

    /**
     * 执行优惠券校验
     *
     * @param context 订单创建上下文
     */
    @Override
    public void check(OrderCreateContext context) {
        OrderCreateRequest request = context.getRequest();
        String remark = StrUtil.nullToEmpty(request.getRemark());

        if (StrUtil.contains(remark, "invalid_coupon")) {
            throw new IllegalArgumentException("优惠券无效，不允许创建订单");
        }

        log.info("优惠券校验通过，用户ID：{}，商品ID：{}",
                request.getUserId(), request.getProductId());
    }
}
```

新增完成后，Spring 会自动把 `CouponCheckHandler` 注入到 `OrderCreateCheckChain` 中。由于它的 `order()` 返回 `35`，所以执行顺序会位于金额校验之后、风控校验之前。

```text
用户状态校验：10
商品库存校验：20
订单金额校验：30
优惠券校验：35
订单风控校验：40
```

## 验证方式

可以从以下几个方面验证组合模式是否生效：

```text
1. 启动项目时，日志应输出订单创建校验链初始化完成
2. 正常请求会依次执行所有校验节点
3. 某个校验节点抛出异常后，后续节点不会继续执行
4. 订单编号生成和订单保存仍然由模板方法控制
5. 新增校验节点后，不需要修改 AbstractOrderCreateTemplate
6. 调整校验节点 order() 后，可以改变责任链执行顺序
```

正常请求日志示例：

```text
开始创建订单，用户ID：1001，商品ID：2001
开始执行订单创建校验节点：用户状态校验
用户状态校验通过，用户ID：1001
订单创建校验节点执行通过：用户状态校验
开始执行订单创建校验节点：商品库存校验
商品库存校验通过，商品ID：2001，购买数量：2
订单创建校验节点执行通过：商品库存校验
开始执行订单创建校验节点：订单金额校验
订单金额校验通过，原始金额：199.99，处理后金额：199.99
订单创建校验节点执行通过：订单金额校验
开始执行订单创建校验节点：订单风控校验
订单风控校验通过，用户ID：1001，商品ID：2001
订单创建校验节点执行通过：订单风控校验
订单编号生成完成，订单编号：OD1988267712300011520
模拟保存订单，订单编号：OD1988267712300011520，用户ID：1001，商品ID：2001，订单金额：199.99
订单创建完成，订单编号：OD1988267712300011520，订单状态：CREATED
执行订单创建后置处理，订单编号：OD1988267712300011520，用户ID：1001
订单创建服务处理完成，订单编号：OD1988267712300011520
```

## 组合效果

模板方法模式和责任链模式组合后，各自负责不同变化点：

| 模式     | 职责       | 在示例中的体现                                                                                              |
| ------ | -------- | ---------------------------------------------------------------------------------------------------- |
| 模板方法模式 | 固定业务主流程  | `AbstractOrderCreateTemplate` 固定订单创建步骤                                                               |
| 责任链模式  | 扩展连续处理节点 | `UserStatusCheckHandler`、`ProductStockCheckHandler`、`AmountCheckHandler`、`RiskCheckHandler` 负责订单创建校验 |

这种组合适合处理下面两类变化：

```text
第一类变化：主流程相对稳定
例如订单创建都需要创建前处理、校验、生成订单编号、保存订单、创建后处理

第二类变化：流程中的一段处理节点经常增减
例如校验规则、风控规则、过滤规则、审批规则不断变化
```

相比只使用模板方法模式，这个组合可以避免抽象模板类中堆积大量校验逻辑。相比只使用责任链模式，这个组合可以保留完整业务流程的稳定骨架，避免责任链承担过多主流程编排职责。

## 注意事项

模板方法模式和责任链模式组合使用时，要明确边界：

```text
模板方法负责流程骨架
责任链负责连续节点
```

不要让责任链控制完整业务流程。责任链只应该处理流程中的一段逻辑，例如校验链、过滤链、审批链、风控链。订单编号生成、订单保存、事务边界、后置处理等主流程步骤仍然应该由模板方法控制。

也不要把所有校验都写进模板方法。模板方法一旦承载过多细节，就会变成难以维护的“上帝类”。如果某一段步骤具有明显的顺序性、可插拔性和扩展性，就适合拆成责任链。

这个组合最适合的判断标准是：**流程稳定归模板，节点扩展归责任链**。
