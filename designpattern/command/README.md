# 命令模式

命令模式用于把“请求”封装成独立对象，使调用方不直接调用具体业务方法，而是通过统一的命令执行入口触发不同操作。
在 Spring Boot 项目中，命令模式常用于后台操作中心、订单操作、审批动作、任务调度、消息消费、业务补偿、撤销重做、操作审计等场景。

本文以“订单操作命令中心”为例，把创建订单、取消订单、订单退款封装成不同命令，由统一执行器根据命令编码分发执行。

## 适用场景

命令模式适合处理“同一个入口触发多种业务动作”的场景。

在订单系统中，后台或外部系统可能会发起多种操作：

| 命令           | 说明     |
| -------------- | -------- |
| `ORDER_CREATE` | 创建订单 |
| `ORDER_CANCEL` | 取消订单 |
| `ORDER_REFUND` | 订单退款 |

如果 Controller 或 Service 中大量使用 `if-else` 判断命令类型，后续新增命令时会不断修改原有代码，扩展性较差。命令模式可以把每个动作封装成独立命令类，让调用方只关心“执行命令”，不关心“命令内部怎么做”。

## 基础配置

本示例基于 Spring Boot 3，使用 Hutool、Lombok 和 Validation。Hutool 用于对象转换、字符串判断、金额比较和 ID 生成，Validation 用于接口参数基础校验。

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

    <!-- Hutool：提供对象转换、字符串处理、金额比较、ID生成等工具能力 -->
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
src/main/java/io/github/atengk/pattern/command
├── CommandApplication.java
├── common
│   ├── ApiResult.java
│   ├── BizException.java
│   └── GlobalExceptionHandler.java
└── order
    ├── controller
    │   └── OrderCommandController.java
    ├── command
    │   ├── OrderCommand.java
    │   ├── OrderCommandCodes.java
    │   ├── OrderCommandExecutor.java
    │   └── impl
    │       ├── CreateOrderCommand.java
    │       ├── CancelOrderCommand.java
    │       └── RefundOrderCommand.java
    ├── dto
    │   ├── CommandExecuteRequest.java
    │   ├── CommandExecuteResult.java
    │   ├── CreateOrderCommandParam.java
    │   ├── CancelOrderCommandParam.java
    │   └── RefundOrderCommandParam.java
    ├── service
    │   ├── OrderDomainService.java
    │   └── impl
    │       └── OrderDomainServiceImpl.java
    └── vo
        └── OrderInfoVO.java
```

## 核心设计

本示例把命令模式拆成四个核心角色：

| 角色            | 项目中的类                                                   | 说明                                 |
| --------------- | ------------------------------------------------------------ | ------------------------------------ |
| Command         | `OrderCommand`                                               | 统一命令接口，定义命令编码和执行方法 |
| ConcreteCommand | `CreateOrderCommand`、`CancelOrderCommand`、`RefundOrderCommand` | 具体命令实现                         |
| Invoker         | `OrderCommandExecutor`                                       | 命令调用者，根据命令编码执行对应命令 |
| Receiver        | `OrderDomainService`                                         | 真正执行业务逻辑的领域服务           |

执行流程如下：

```text
Controller
  -> OrderCommandExecutor
    -> 根据 commandCode 查找 OrderCommand
      -> CreateOrderCommand / CancelOrderCommand / RefundOrderCommand
        -> OrderDomainService
          -> 执行具体订单业务
```

命令类不直接承载复杂业务，而是负责参数转换、命令级校验和调用领域服务。真正的业务处理放在 `OrderDomainService` 中，这样命令对象和业务模型不会过度耦合。

## 公共代码

下面的公共响应、业务异常和全局异常处理可以和上一篇责任链模式中的公共类保持一致。如果你已经在项目中创建过，可以直接复用。

文件位置：`src/main/java/io/github/atengk/pattern/command/common/ApiResult.java`

```java
package io.github.atengk.pattern.command.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/command/common/BizException.java`

```java
package io.github.atengk.pattern.command.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/command/common/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.command.common;

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

下面给出命令模式在 Spring Boot 中的核心实现。示例为了聚焦设计模式，没有接入数据库，订单数据使用内存 `ConcurrentHashMap` 模拟。实际项目中可以替换为 MyBatis-Plus、Redis、MQ 或远程服务调用。

文件位置：`src/main/java/io/github/atengk/pattern/command/CommandApplication.java`

```java
package io.github.atengk.pattern.command;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 命令模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class CommandApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommandApplication.class, args);
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/command/order/dto/CommandExecuteRequest.java`

```java
package io.github.atengk.pattern.command.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

/**
 * 命令执行请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class CommandExecuteRequest {

    @NotBlank(message = "命令编码不能为空")
    private String commandCode;

    @NotEmpty(message = "命令参数不能为空")
    private Map<String, Object> params;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/command/order/dto/CommandExecuteResult.java`

```java
package io.github.atengk.pattern.command.order.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 命令执行结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class CommandExecuteResult {

    private String commandCode;

    private String businessNo;

    private String message;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/command/order/dto/CreateOrderCommandParam.java`

```java
package io.github.atengk.pattern.command.order.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单命令参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class CreateOrderCommandParam {

    private Long userId;

    private Long productId;

    private Integer quantity;

    private BigDecimal amount;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/command/order/dto/CancelOrderCommandParam.java`

```java
package io.github.atengk.pattern.command.order.dto;

import lombok.Data;

/**
 * 取消订单命令参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class CancelOrderCommandParam {

    private String orderNo;

    private String cancelReason;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/command/order/dto/RefundOrderCommandParam.java`

```java
package io.github.atengk.pattern.command.order.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单退款命令参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class RefundOrderCommandParam {

    private String orderNo;

    private BigDecimal refundAmount;

    private String refundReason;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/command/order/vo/OrderInfoVO.java`

```java
package io.github.atengk.pattern.command.order.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单信息展示对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderInfoVO {

    private String orderNo;

    private Long userId;

    private Long productId;

    private Integer quantity;

    private BigDecimal amount;

    private String status;

    private String refundNo;

}
```

## 命令接口

命令接口定义每个命令必须具备的基本能力，包括命令编码、命令名称和执行逻辑。所有具体命令都实现这个接口。

文件位置：`src/main/java/io/github/atengk/pattern/command/order/command/OrderCommand.java`

```java
package io.github.atengk.pattern.command.order.command;

import io.github.atengk.pattern.command.order.dto.CommandExecuteRequest;
import io.github.atengk.pattern.command.order.dto.CommandExecuteResult;

/**
 * 订单命令接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderCommand {

    /**
     * 返回命令编码
     *
     * @return 命令编码
     */
    String commandCode();

    /**
     * 返回命令名称
     *
     * @return 命令名称
     */
    String commandName();

    /**
     * 执行命令
     *
     * @param request 命令执行请求
     * @return 命令执行结果
     */
    CommandExecuteResult execute(CommandExecuteRequest request);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/command/order/command/OrderCommandCodes.java`

```java
package io.github.atengk.pattern.command.order.command;

/**
 * 订单命令编码常量
 *
 * @author Ateng
 * @since 2026-05-13
 */
public final class OrderCommandCodes {

    public static final String ORDER_CREATE = "ORDER_CREATE";

    public static final String ORDER_CANCEL = "ORDER_CANCEL";

    public static final String ORDER_REFUND = "ORDER_REFUND";

    private OrderCommandCodes() {
    }

}
```

## 命令执行器

命令执行器是命令模式中的调用者。它不处理具体业务，只负责根据 `commandCode` 找到对应命令并执行。

文件位置：`src/main/java/io/github/atengk/pattern/command/order/command/OrderCommandExecutor.java`

```java
package io.github.atengk.pattern.command.order.command;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.command.common.BizException;
import io.github.atengk.pattern.command.order.dto.CommandExecuteRequest;
import io.github.atengk.pattern.command.order.dto.CommandExecuteResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单命令执行器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderCommandExecutor {

    private final Map<String, OrderCommand> commandMap;

    /**
     * 初始化订单命令执行器
     *
     * @param orderCommands Spring 容器中的订单命令集合
     */
    public OrderCommandExecutor(List<OrderCommand> orderCommands) {
        if (CollUtil.isEmpty(orderCommands)) {
            this.commandMap = Collections.emptyMap();
            log.warn("订单命令执行器未加载到任何命令");
            return;
        }

        Map<String, OrderCommand> registerMap = new HashMap<>(orderCommands.size());
        for (OrderCommand orderCommand : orderCommands) {
            String commandCode = orderCommand.commandCode();
            if (registerMap.containsKey(commandCode)) {
                throw new IllegalStateException("订单命令编码重复：" + commandCode);
            }
            registerMap.put(commandCode, orderCommand);
            log.info("注册订单命令，commandCode：{}，commandName：{}", commandCode, orderCommand.commandName());
        }

        this.commandMap = Collections.unmodifiableMap(registerMap);
    }

    /**
     * 执行订单命令
     *
     * @param request 命令执行请求
     * @return 命令执行结果
     */
    public CommandExecuteResult execute(CommandExecuteRequest request) {
        String commandCode = request.getCommandCode();
        if (StrUtil.isBlank(commandCode)) {
            throw new BizException("命令编码不能为空");
        }

        OrderCommand orderCommand = commandMap.get(commandCode);
        if (orderCommand == null) {
            throw new BizException(StrUtil.format("不支持的订单命令：{}", commandCode));
        }

        log.info("开始执行订单命令，commandCode：{}，commandName：{}", commandCode, orderCommand.commandName());
        CommandExecuteResult result = orderCommand.execute(request);
        log.info("订单命令执行完成，commandCode：{}，businessNo：{}", commandCode, result.getBusinessNo());

        return result;
    }

}
```

## 领域服务

领域服务是命令模式中的接收者。具体命令只负责把请求转换成对应参数，然后调用领域服务完成业务动作。

文件位置：`src/main/java/io/github/atengk/pattern/command/order/service/OrderDomainService.java`

```java
package io.github.atengk.pattern.command.order.service;

import io.github.atengk.pattern.command.order.dto.CancelOrderCommandParam;
import io.github.atengk.pattern.command.order.dto.CreateOrderCommandParam;
import io.github.atengk.pattern.command.order.dto.RefundOrderCommandParam;
import io.github.atengk.pattern.command.order.vo.OrderInfoVO;

/**
 * 订单领域服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderDomainService {

    /**
     * 创建订单
     *
     * @param param 创建订单参数
     * @return 订单编号
     */
    String createOrder(CreateOrderCommandParam param);

    /**
     * 取消订单
     *
     * @param param 取消订单参数
     */
    void cancelOrder(CancelOrderCommandParam param);

    /**
     * 订单退款
     *
     * @param param 订单退款参数
     * @return 退款编号
     */
    String refundOrder(RefundOrderCommandParam param);

    /**
     * 查询订单信息
     *
     * @param orderNo 订单编号
     * @return 订单信息
     */
    OrderInfoVO getOrder(String orderNo);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/command/order/service/impl/OrderDomainServiceImpl.java`

```java
package io.github.atengk.pattern.command.order.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.command.common.BizException;
import io.github.atengk.pattern.command.order.dto.CancelOrderCommandParam;
import io.github.atengk.pattern.command.order.dto.CreateOrderCommandParam;
import io.github.atengk.pattern.command.order.dto.RefundOrderCommandParam;
import io.github.atengk.pattern.command.order.service.OrderDomainService;
import io.github.atengk.pattern.command.order.vo.OrderInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单领域服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class OrderDomainServiceImpl implements OrderDomainService {

    private static final String STATUS_CREATED = "CREATED";

    private static final String STATUS_CANCELED = "CANCELED";

    private static final String STATUS_REFUNDED = "REFUNDED";

    private final Map<String, OrderInfoVO> orderStorage = new ConcurrentHashMap<>();

    /**
     * 创建订单
     *
     * @param param 创建订单参数
     * @return 订单编号
     */
    @Override
    public String createOrder(CreateOrderCommandParam param) {
        String orderNo = "OD" + IdUtil.getSnowflakeNextIdStr();

        OrderInfoVO orderInfo = OrderInfoVO.builder()
                .orderNo(orderNo)
                .userId(param.getUserId())
                .productId(param.getProductId())
                .quantity(param.getQuantity())
                .amount(param.getAmount())
                .status(STATUS_CREATED)
                .build();

        orderStorage.put(orderNo, orderInfo);
        log.info("订单创建成功，orderNo：{}，userId：{}，productId：{}",
                orderNo, param.getUserId(), param.getProductId());

        return orderNo;
    }

    /**
     * 取消订单
     *
     * @param param 取消订单参数
     */
    @Override
    public void cancelOrder(CancelOrderCommandParam param) {
        OrderInfoVO orderInfo = getRequiredOrder(param.getOrderNo());

        if (StrUtil.equals(orderInfo.getStatus(), STATUS_CANCELED)) {
            throw new BizException("订单已取消，不能重复取消");
        }

        if (StrUtil.equals(orderInfo.getStatus(), STATUS_REFUNDED)) {
            throw new BizException("订单已退款，不能取消");
        }

        orderInfo.setStatus(STATUS_CANCELED);
        log.info("订单取消成功，orderNo：{}，cancelReason：{}",
                param.getOrderNo(), param.getCancelReason());
    }

    /**
     * 订单退款
     *
     * @param param 订单退款参数
     * @return 退款编号
     */
    @Override
    public String refundOrder(RefundOrderCommandParam param) {
        OrderInfoVO orderInfo = getRequiredOrder(param.getOrderNo());

        if (!StrUtil.equals(orderInfo.getStatus(), STATUS_CREATED)) {
            throw new BizException("当前订单状态不允许退款");
        }

        if (NumberUtil.isGreater(param.getRefundAmount(), orderInfo.getAmount())) {
            throw new BizException("退款金额不能大于订单金额");
        }

        String refundNo = "RF" + IdUtil.getSnowflakeNextIdStr();
        orderInfo.setStatus(STATUS_REFUNDED);
        orderInfo.setRefundNo(refundNo);

        log.info("订单退款成功，orderNo：{}，refundNo：{}，refundAmount：{}",
                param.getOrderNo(), refundNo, param.getRefundAmount());

        return refundNo;
    }

    /**
     * 查询订单信息
     *
     * @param orderNo 订单编号
     * @return 订单信息
     */
    @Override
    public OrderInfoVO getOrder(String orderNo) {
        return getRequiredOrder(orderNo);
    }

    /**
     * 获取必须存在的订单
     *
     * @param orderNo 订单编号
     * @return 订单信息
     */
    private OrderInfoVO getRequiredOrder(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            throw new BizException("订单编号不能为空");
        }

        OrderInfoVO orderInfo = orderStorage.get(orderNo);
        if (orderInfo == null) {
            throw new BizException("订单不存在");
        }

        return orderInfo;
    }

}
```

## 具体命令

每个具体命令都是一个独立的 Spring Bean。新增命令时，只需要新增一个实现 `OrderCommand` 的类，不需要修改执行器。

文件位置：`src/main/java/io/github/atengk/pattern/command/order/command/impl/CreateOrderCommand.java`

```java
package io.github.atengk.pattern.command.order.command.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.command.common.BizException;
import io.github.atengk.pattern.command.order.command.OrderCommand;
import io.github.atengk.pattern.command.order.command.OrderCommandCodes;
import io.github.atengk.pattern.command.order.dto.CommandExecuteRequest;
import io.github.atengk.pattern.command.order.dto.CommandExecuteResult;
import io.github.atengk.pattern.command.order.dto.CreateOrderCommandParam;
import io.github.atengk.pattern.command.order.service.OrderDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 创建订单命令
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateOrderCommand implements OrderCommand {

    private final OrderDomainService orderDomainService;

    /**
     * 返回命令编码
     *
     * @return 命令编码
     */
    @Override
    public String commandCode() {
        return OrderCommandCodes.ORDER_CREATE;
    }

    /**
     * 返回命令名称
     *
     * @return 命令名称
     */
    @Override
    public String commandName() {
        return "创建订单";
    }

    /**
     * 执行创建订单命令
     *
     * @param request 命令执行请求
     * @return 命令执行结果
     */
    @Override
    public CommandExecuteResult execute(CommandExecuteRequest request) {
        CreateOrderCommandParam param = BeanUtil.toBean(request.getParams(), CreateOrderCommandParam.class);
        checkParam(param);

        String orderNo = orderDomainService.createOrder(param);
        log.info("创建订单命令执行成功，orderNo：{}", orderNo);

        return CommandExecuteResult.builder()
                .commandCode(commandCode())
                .businessNo(orderNo)
                .message("订单创建成功")
                .build();
    }

    /**
     * 校验创建订单参数
     *
     * @param param 创建订单参数
     */
    private void checkParam(CreateOrderCommandParam param) {
        if (ObjectUtil.isNull(param.getUserId()) || param.getUserId() <= 0) {
            throw new BizException("用户ID不合法");
        }

        if (ObjectUtil.isNull(param.getProductId()) || param.getProductId() <= 0) {
            throw new BizException("商品ID不合法");
        }

        if (ObjectUtil.isNull(param.getQuantity()) || param.getQuantity() <= 0) {
            throw new BizException("购买数量必须大于0");
        }

        if (ObjectUtil.isNull(param.getAmount()) || NumberUtil.isLessOrEqual(param.getAmount(), BigDecimal.ZERO)) {
            throw new BizException("订单金额必须大于0");
        }
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/command/order/command/impl/CancelOrderCommand.java`

```java
package io.github.atengk.pattern.command.order.command.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.command.common.BizException;
import io.github.atengk.pattern.command.order.command.OrderCommand;
import io.github.atengk.pattern.command.order.command.OrderCommandCodes;
import io.github.atengk.pattern.command.order.dto.CancelOrderCommandParam;
import io.github.atengk.pattern.command.order.dto.CommandExecuteRequest;
import io.github.atengk.pattern.command.order.dto.CommandExecuteResult;
import io.github.atengk.pattern.command.order.service.OrderDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 取消订单命令
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CancelOrderCommand implements OrderCommand {

    private final OrderDomainService orderDomainService;

    /**
     * 返回命令编码
     *
     * @return 命令编码
     */
    @Override
    public String commandCode() {
        return OrderCommandCodes.ORDER_CANCEL;
    }

    /**
     * 返回命令名称
     *
     * @return 命令名称
     */
    @Override
    public String commandName() {
        return "取消订单";
    }

    /**
     * 执行取消订单命令
     *
     * @param request 命令执行请求
     * @return 命令执行结果
     */
    @Override
    public CommandExecuteResult execute(CommandExecuteRequest request) {
        CancelOrderCommandParam param = BeanUtil.toBean(request.getParams(), CancelOrderCommandParam.class);
        checkParam(param);

        orderDomainService.cancelOrder(param);
        log.info("取消订单命令执行成功，orderNo：{}", param.getOrderNo());

        return CommandExecuteResult.builder()
                .commandCode(commandCode())
                .businessNo(param.getOrderNo())
                .message("订单取消成功")
                .build();
    }

    /**
     * 校验取消订单参数
     *
     * @param param 取消订单参数
     */
    private void checkParam(CancelOrderCommandParam param) {
        if (StrUtil.isBlank(param.getOrderNo())) {
            throw new BizException("订单编号不能为空");
        }

        if (StrUtil.isBlank(param.getCancelReason())) {
            throw new BizException("取消原因不能为空");
        }
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/command/order/command/impl/RefundOrderCommand.java`

```java
package io.github.atengk.pattern.command.order.command.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.command.common.BizException;
import io.github.atengk.pattern.command.order.command.OrderCommand;
import io.github.atengk.pattern.command.order.command.OrderCommandCodes;
import io.github.atengk.pattern.command.order.dto.CommandExecuteRequest;
import io.github.atengk.pattern.command.order.dto.CommandExecuteResult;
import io.github.atengk.pattern.command.order.dto.RefundOrderCommandParam;
import io.github.atengk.pattern.command.order.service.OrderDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 订单退款命令
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundOrderCommand implements OrderCommand {

    private final OrderDomainService orderDomainService;

    /**
     * 返回命令编码
     *
     * @return 命令编码
     */
    @Override
    public String commandCode() {
        return OrderCommandCodes.ORDER_REFUND;
    }

    /**
     * 返回命令名称
     *
     * @return 命令名称
     */
    @Override
    public String commandName() {
        return "订单退款";
    }

    /**
     * 执行订单退款命令
     *
     * @param request 命令执行请求
     * @return 命令执行结果
     */
    @Override
    public CommandExecuteResult execute(CommandExecuteRequest request) {
        RefundOrderCommandParam param = BeanUtil.toBean(request.getParams(), RefundOrderCommandParam.class);
        checkParam(param);

        String refundNo = orderDomainService.refundOrder(param);
        log.info("订单退款命令执行成功，orderNo：{}，refundNo：{}", param.getOrderNo(), refundNo);

        return CommandExecuteResult.builder()
                .commandCode(commandCode())
                .businessNo(refundNo)
                .message("订单退款成功")
                .build();
    }

    /**
     * 校验订单退款参数
     *
     * @param param 订单退款参数
     */
    private void checkParam(RefundOrderCommandParam param) {
        if (StrUtil.isBlank(param.getOrderNo())) {
            throw new BizException("订单编号不能为空");
        }

        if (ObjectUtil.isNull(param.getRefundAmount())
                || NumberUtil.isLessOrEqual(param.getRefundAmount(), BigDecimal.ZERO)) {
            throw new BizException("退款金额必须大于0");
        }

        if (StrUtil.isBlank(param.getRefundReason())) {
            throw new BizException("退款原因不能为空");
        }
    }

}
```

## 控制器接口

Controller 只暴露统一命令执行入口，不直接判断具体命令类型。命令类型由 `commandCode` 决定。

文件位置：`src/main/java/io/github/atengk/pattern/command/order/controller/OrderCommandController.java`

```java
package io.github.atengk.pattern.command.order.controller;

import io.github.atengk.pattern.command.common.ApiResult;
import io.github.atengk.pattern.command.order.command.OrderCommandExecutor;
import io.github.atengk.pattern.command.order.dto.CommandExecuteRequest;
import io.github.atengk.pattern.command.order.dto.CommandExecuteResult;
import io.github.atengk.pattern.command.order.service.OrderDomainService;
import io.github.atengk.pattern.command.order.vo.OrderInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单命令接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/order-commands")
@RequiredArgsConstructor
public class OrderCommandController {

    private final OrderCommandExecutor orderCommandExecutor;

    private final OrderDomainService orderDomainService;

    /**
     * 执行订单命令
     *
     * @param request 命令执行请求
     * @return 命令执行结果
     */
    @PostMapping("/execute")
    public ApiResult<CommandExecuteResult> execute(@Valid @RequestBody CommandExecuteRequest request) {
        return ApiResult.success(orderCommandExecutor.execute(request));
    }

    /**
     * 查询订单信息
     *
     * @param orderNo 订单编号
     * @return 订单信息
     */
    @GetMapping("/orders/{orderNo}")
    public ApiResult<OrderInfoVO> getOrder(@PathVariable String orderNo) {
        return ApiResult.success(orderDomainService.getOrder(orderNo));
    }

}
```

## 使用方式

启动项目后，所有订单操作都通过统一命令执行接口完成。

接口信息：

| 项目         | 内容                      |
| ------------ | ------------------------- |
| 请求路径     | `/order-commands/execute` |
| 请求方法     | `POST`                    |
| Content-Type | `application/json`        |
| 命令字段     | `commandCode`             |
| 参数字段     | `params`                  |

创建订单请求：

```bash
curl -X POST "http://localhost:8080/order-commands/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "commandCode": "ORDER_CREATE",
    "params": {
      "userId": 10001,
      "productId": 20001,
      "quantity": 2,
      "amount": 199.90
    }
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "commandCode": "ORDER_CREATE",
    "businessNo": "OD1998948715737427968",
    "message": "订单创建成功"
  }
}
```

查询订单请求：

```bash
curl -X GET "http://localhost:8080/order-commands/orders/OD1998948715737427968"
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "OD1998948715737427968",
    "userId": 10001,
    "productId": 20001,
    "quantity": 2,
    "amount": 199.90,
    "status": "CREATED",
    "refundNo": null
  }
}
```

取消订单请求：

```bash
curl -X POST "http://localhost:8080/order-commands/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "commandCode": "ORDER_CANCEL",
    "params": {
      "orderNo": "OD1998948715737427968",
      "cancelReason": "用户主动取消"
    }
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "commandCode": "ORDER_CANCEL",
    "businessNo": "OD1998948715737427968",
    "message": "订单取消成功"
  }
}
```

订单退款请求：

```bash
curl -X POST "http://localhost:8080/order-commands/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "commandCode": "ORDER_REFUND",
    "params": {
      "orderNo": "OD1998948715737427968",
      "refundAmount": 99.90,
      "refundReason": "商品质量问题"
    }
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "commandCode": "ORDER_REFUND",
    "businessNo": "RF1998948991737427968",
    "message": "订单退款成功"
  }
}
```

## 扩展方式

新增命令时，只需要三步：

第一，新增命令编码。

文件位置：`src/main/java/io/github/atengk/pattern/command/order/command/OrderCommandCodes.java`

```java
public static final String ORDER_CLOSE = "ORDER_CLOSE";
```

第二，新增命令参数对象。

文件位置：`src/main/java/io/github/atengk/pattern/command/order/dto/CloseOrderCommandParam.java`

```java
package io.github.atengk.pattern.command.order.dto;

import lombok.Data;

/**
 * 关闭订单命令参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class CloseOrderCommandParam {

    private String orderNo;

    private String closeReason;

}
```

第三，新增具体命令类。

文件位置：`src/main/java/io/github/atengk/pattern/command/order/command/impl/CloseOrderCommand.java`

```java
package io.github.atengk.pattern.command.order.command.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.command.common.BizException;
import io.github.atengk.pattern.command.order.command.OrderCommand;
import io.github.atengk.pattern.command.order.command.OrderCommandCodes;
import io.github.atengk.pattern.command.order.dto.CloseOrderCommandParam;
import io.github.atengk.pattern.command.order.dto.CommandExecuteRequest;
import io.github.atengk.pattern.command.order.dto.CommandExecuteResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 关闭订单命令
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class CloseOrderCommand implements OrderCommand {

    /**
     * 返回命令编码
     *
     * @return 命令编码
     */
    @Override
    public String commandCode() {
        return OrderCommandCodes.ORDER_CLOSE;
    }

    /**
     * 返回命令名称
     *
     * @return 命令名称
     */
    @Override
    public String commandName() {
        return "关闭订单";
    }

    /**
     * 执行关闭订单命令
     *
     * @param request 命令执行请求
     * @return 命令执行结果
     */
    @Override
    public CommandExecuteResult execute(CommandExecuteRequest request) {
        CloseOrderCommandParam param = BeanUtil.toBean(request.getParams(), CloseOrderCommandParam.class);
        if (StrUtil.isBlank(param.getOrderNo())) {
            throw new BizException("订单编号不能为空");
        }

        log.info("关闭订单命令执行成功，orderNo：{}，closeReason：{}",
                param.getOrderNo(), param.getCloseReason());

        return CommandExecuteResult.builder()
                .commandCode(commandCode())
                .businessNo(param.getOrderNo())
                .message("订单关闭成功")
                .build();
    }

}
```

新增命令类后，Spring 会自动把它注入到 `List<OrderCommand>` 中，`OrderCommandExecutor` 会自动注册该命令，不需要修改执行器代码。

## 验证方式

启动项目后，观察控制台日志，应能看到命令注册日志：

```text
注册订单命令，commandCode：ORDER_CREATE，commandName：创建订单
注册订单命令，commandCode：ORDER_CANCEL，commandName：取消订单
注册订单命令，commandCode：ORDER_REFUND，commandName：订单退款
```

执行创建订单命令后，应能看到类似日志：

```text
开始执行订单命令，commandCode：ORDER_CREATE，commandName：创建订单
订单创建成功，orderNo：OD1998948715737427968，userId：10001，productId：20001
创建订单命令执行成功，orderNo：OD1998948715737427968
订单命令执行完成，commandCode：ORDER_CREATE，businessNo：OD1998948715737427968
```

如果传入不存在的命令编码：

```bash
curl -X POST "http://localhost:8080/order-commands/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "commandCode": "ORDER_UNKNOWN",
    "params": {
      "orderNo": "OD1998948715737427968"
    }
  }'
```

返回示例：

```json
{
  "code": 500,
  "message": "不支持的订单命令：ORDER_UNKNOWN",
  "data": null
}
```

## 优点和注意事项

命令模式的核心价值是把“请求动作”对象化，使调用方和具体业务动作解耦。调用方只需要提交命令编码和参数，执行器负责查找并触发具体命令。

| 注意事项             | 说明                                                         |
| -------------------- | ------------------------------------------------------------ |
| 命令编码要稳定       | `commandCode` 通常会被前端、MQ、任务调度或外部系统依赖，不建议随意修改 |
| 命令参数要清晰       | 不同命令参数不同，建议每个命令定义独立参数类                 |
| 命令类不宜过重       | 命令类适合做参数转换、命令校验和调用领域服务，不建议堆积复杂业务 |
| 业务逻辑放到领域服务 | 真正的订单创建、取消、退款逻辑应放在 Service 或领域服务中    |
| 注意幂等处理         | 如果命令来自 MQ、定时任务或外部系统，必须考虑重复执行问题    |
| 可以结合审计日志     | 命令模式天然适合记录操作人、操作时间、命令参数和执行结果     |

## 和责任链模式的区别

命令模式和责任链模式都可以解耦业务代码，但关注点不同。

| 模式       | 关注点                       | 典型场景                              |
| ---------- | ---------------------------- | ------------------------------------- |
| 责任链模式 | 一个请求依次经过多个处理节点 | 校验链、审批链、过滤链、风控链        |
| 命令模式   | 把一个动作封装成一个命令对象 | 操作中心、任务调度、MQ 消费、撤销重做 |

简单来说，责任链模式强调“一个请求被多个节点处理”，命令模式强调“一个动作被封装成独立对象执行”。

## 小结

命令模式在 Spring Boot 项目中的常见落地方式是：定义统一命令接口，使用多个具体命令类实现不同业务动作，再通过统一执行器根据命令编码分发执行。
在订单操作、后台管理、任务调度、消息消费和业务补偿场景中，命令模式可以减少 `if-else` 分支，提高业务动作的扩展性和可维护性。
