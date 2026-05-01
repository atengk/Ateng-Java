# 设计模式：命令模式

命令模式用于把一次操作封装成一个独立命令对象，让调用方不直接调用真正的业务执行者，而是把请求交给命令对象执行。在 JDK21 和 Spring Boot 3 项目中，命令模式常用于订单操作、任务调度、异步队列、操作日志、撤销重做、审批动作、批量执行、菜单按钮动作、远程调用封装等场景。

需要注意：命令模式关注的是“把请求封装成对象”。如果只是根据类型选择不同算法，更适合策略模式；如果是多个处理器顺序处理请求，更适合责任链模式；如果是对象在不同状态下行为不同，更适合状态模式。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证命令模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、ID、金额、集合等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- Lombok，简化日志对象、构造方法、Getter 等样板代码 -->
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

命令模式的核心目标是把“请求发起者”和“请求执行者”解耦。调用方只需要提交命令，不需要知道命令内部具体调用哪个服务、执行哪些步骤。

常见角色如下：

| 角色            | 说明                               |
| --------------- | ---------------------------------- |
| Command         | 命令接口，定义统一执行方法         |
| ConcreteCommand | 具体命令，封装一个具体操作         |
| Receiver        | 接收者，真正执行业务逻辑的对象     |
| Invoker         | 调用者，负责触发命令执行           |
| Client          | 客户端，创建或选择命令并交给调用者 |

常见实现方式如下：

| 实现方式                    | 是否推荐         | 适用场景                              |
| --------------------------- | ---------------- | ------------------------------------- |
| 普通 Java 命令              | 推荐用于理解原理 | 本地操作封装、撤销重做                |
| Spring Bean 命令            | 强烈推荐         | Spring Boot 项目中的业务动作分发      |
| 命令队列                    | 推荐             | 异步执行、批量执行、延迟执行          |
| 命令日志                    | 推荐             | 操作审计、失败重试、回放              |
| 大量 `if else` 调用业务方法 | 不推荐           | 动作增多后 Controller 或 Service 膨胀 |

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring Bean 命令 + Invoker > 普通 Java 命令 > 大量 if else 分支调用
```

命令模式适合把一个操作封装为对象，从而支持排队、延迟执行、异步执行、撤销、重做、日志记录和统一调度。

## 普通 Java 命令

普通 Java 命令适合不依赖 Spring 容器的操作封装。下面以账户余额操作为例，存款和扣款都封装成命令对象，并支持撤销操作。

整体流程如下：

```text
创建账户 -> 创建命令 -> 命令调用账户对象执行操作 -> 需要时撤销命令
```

### 文件结构

```text
src/main/java/io/github/atengk/design/command/simple/
├── Account.java
├── AccountCommand.java
├── DepositCommand.java
├── WithdrawCommand.java
└── AccountCommandInvoker.java
```

文件位置：`src/main/java/io/github/atengk/design/command/simple/Account.java`

下面是账户对象，它是真正执行业务动作的接收者。

```java
package io.github.atengk.design.command.simple;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 账户
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Getter
public class Account {

    private final String accountNo;
    private BigDecimal balance;

    /**
     * 创建账户
     *
     * @param accountNo 账户号
     * @param balance   初始余额
     */
    public Account(String accountNo, BigDecimal balance) {
        if (StrUtil.isBlank(accountNo)) {
            throw new IllegalArgumentException("账户号不能为空");
        }

        if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("账户余额不能小于0");
        }

        this.accountNo = accountNo;
        this.balance = balance;
    }

    /**
     * 存款
     *
     * @param amount 金额
     */
    public void deposit(BigDecimal amount) {
        validateAmount(amount);

        this.balance = NumberUtil.add(this.balance, amount);
        log.info("账户存款成功，账户号：{}，金额：{}，余额：{}", accountNo, amount, balance);
    }

    /**
     * 扣款
     *
     * @param amount 金额
     */
    public void withdraw(BigDecimal amount) {
        validateAmount(amount);

        if (this.balance.compareTo(amount) < 0) {
            log.warn("账户扣款失败，余额不足，账户号：{}，余额：{}，扣款金额：{}", accountNo, balance, amount);
            throw new IllegalStateException("账户余额不足");
        }

        this.balance = NumberUtil.sub(this.balance, amount);
        log.info("账户扣款成功，账户号：{}，金额：{}，余额：{}", accountNo, amount, balance);
    }

    /**
     * 校验金额
     *
     * @param amount 金额
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("账户操作金额不合法，账户号：{}，金额：{}", accountNo, amount);
            throw new IllegalArgumentException("操作金额必须大于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/command/simple/AccountCommand.java`

下面是账户命令接口，定义执行和撤销两个动作。

```java
package io.github.atengk.design.command.simple;

/**
 * 账户命令
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface AccountCommand {

    /**
     * 执行命令
     */
    void execute();

    /**
     * 撤销命令
     */
    void undo();

    /**
     * 获取命令名称
     *
     * @return 命令名称
     */
    String commandName();
}
```

文件位置：`src/main/java/io/github/atengk/design/command/simple/DepositCommand.java`

下面是存款命令。执行时调用账户存款，撤销时调用账户扣款。

```java
package io.github.atengk.design.command.simple;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 存款命令
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class DepositCommand implements AccountCommand {

    private final Account account;
    private final BigDecimal amount;

    /**
     * 创建存款命令
     *
     * @param account 账户
     * @param amount  存款金额
     */
    public DepositCommand(Account account, BigDecimal amount) {
        if (account == null) {
            throw new IllegalArgumentException("账户不能为空");
        }

        this.account = account;
        this.amount = amount;
    }

    /**
     * 执行命令
     */
    @Override
    public void execute() {
        log.info("执行存款命令，账户号：{}，金额：{}", account.getAccountNo(), amount);
        account.deposit(amount);
    }

    /**
     * 撤销命令
     */
    @Override
    public void undo() {
        log.info("撤销存款命令，账户号：{}，金额：{}", account.getAccountNo(), amount);
        account.withdraw(amount);
    }

    /**
     * 获取命令名称
     *
     * @return 命令名称
     */
    @Override
    public String commandName() {
        return "DEPOSIT";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/command/simple/WithdrawCommand.java`

下面是扣款命令。执行时调用账户扣款，撤销时调用账户存款。

```java
package io.github.atengk.design.command.simple;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 扣款命令
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class WithdrawCommand implements AccountCommand {

    private final Account account;
    private final BigDecimal amount;

    /**
     * 创建扣款命令
     *
     * @param account 账户
     * @param amount  扣款金额
     */
    public WithdrawCommand(Account account, BigDecimal amount) {
        if (account == null) {
            throw new IllegalArgumentException("账户不能为空");
        }

        this.account = account;
        this.amount = amount;
    }

    /**
     * 执行命令
     */
    @Override
    public void execute() {
        log.info("执行扣款命令，账户号：{}，金额：{}", account.getAccountNo(), amount);
        account.withdraw(amount);
    }

    /**
     * 撤销命令
     */
    @Override
    public void undo() {
        log.info("撤销扣款命令，账户号：{}，金额：{}", account.getAccountNo(), amount);
        account.deposit(amount);
    }

    /**
     * 获取命令名称
     *
     * @return 命令名称
     */
    @Override
    public String commandName() {
        return "WITHDRAW";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/command/simple/AccountCommandInvoker.java`

下面是命令调用者。它负责执行命令，并记录最近一次命令以支持撤销。

```java
package io.github.atengk.design.command.simple;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 账户命令调用者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class AccountCommandInvoker {

    private final Deque<AccountCommand> historyCommands = new ArrayDeque<>();

    /**
     * 执行命令
     *
     * @param command 账户命令
     */
    public void execute(AccountCommand command) {
        if (command == null) {
            log.warn("执行账户命令失败，命令为空");
            throw new IllegalArgumentException("命令不能为空");
        }

        command.execute();
        historyCommands.push(command);

        log.info("账户命令执行完成，命令：{}，历史命令数量：{}", command.commandName(), historyCommands.size());
    }

    /**
     * 撤销最近一次命令
     */
    public void undoLast() {
        if (historyCommands.isEmpty()) {
            log.warn("撤销账户命令失败，历史命令为空");
            throw new IllegalStateException("没有可撤销的命令");
        }

        AccountCommand command = historyCommands.pop();
        command.undo();

        log.info("账户命令撤销完成，命令：{}，剩余历史命令数量：{}", command.commandName(), historyCommands.size());
    }
}
```

使用方式：

```java
Account account = new Account("ACC10001", BigDecimal.valueOf(1000));
AccountCommandInvoker invoker = new AccountCommandInvoker();

invoker.execute(new DepositCommand(account, BigDecimal.valueOf(200)));
invoker.execute(new WithdrawCommand(account, BigDecimal.valueOf(100)));
invoker.undoLast();
```

执行流程是：账户先存入 200，再扣款 100，最后撤销扣款命令，账户余额回到扣款前状态。

## Spring Boot 命令模式

Spring Boot 项目中更常见的写法，是把每个命令注册为 Spring Bean，再通过命令调用器根据动作类型选择命令执行。下面以订单操作为例，把支付、取消、发货三个动作封装成不同命令。

整体流程如下：

```text
Controller 接收请求 -> Invoker 匹配命令 -> 命令调用 Receiver -> 返回操作结果
```

示例中的角色如下：

```text
OrderCommand              命令接口
PayOrderCommand           支付订单命令
CancelOrderCommand        取消订单命令
ShipOrderCommand          发货订单命令
OrderDomainService        接收者，真正执行业务逻辑
OrderCommandInvoker       调用者，负责选择并执行命令
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── CommandApplication.java
├── command/
│   ├── OrderCommand.java
│   ├── PayOrderCommand.java
│   ├── CancelOrderCommand.java
│   └── ShipOrderCommand.java
├── controller/
│   └── OrderCommandController.java
├── dto/
│   ├── OrderCommandRequest.java
│   └── OrderCommandResponse.java
├── invoker/
│   └── OrderCommandInvoker.java
└── service/
    ├── OrderDomainService.java
    └── impl/
        └── OrderDomainServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/CommandApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 命令模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class CommandApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CommandApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderCommandRequest.java`

下面是订单命令请求对象，用于承载一次订单操作需要的参数。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 订单命令请求
 *
 * @param action     操作类型
 * @param orderNo    订单号
 * @param operatorId 操作人ID
 * @param amount     操作金额
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderCommandRequest(
        String action,
        String orderNo,
        Long operatorId,
        BigDecimal amount
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderCommandResponse.java`

下面是订单命令响应对象。

```java
package io.github.atengk.design.dto;

/**
 * 订单命令响应
 *
 * @param action  操作类型
 * @param orderNo 订单号
 * @param success 是否成功
 * @param message 响应消息
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderCommandResponse(
        String action,
        String orderNo,
        Boolean success,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/service/OrderDomainService.java`

下面是订单领域服务接口，它是命令真正调用的接收者。

```java
package io.github.atengk.design.service;

import java.math.BigDecimal;

/**
 * 订单领域服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderDomainService {

    /**
     * 支付订单
     *
     * @param orderNo    订单号
     * @param operatorId 操作人ID
     * @param amount     支付金额
     */
    void pay(String orderNo, Long operatorId, BigDecimal amount);

    /**
     * 取消订单
     *
     * @param orderNo    订单号
     * @param operatorId 操作人ID
     */
    void cancel(String orderNo, Long operatorId);

    /**
     * 发货订单
     *
     * @param orderNo    订单号
     * @param operatorId 操作人ID
     */
    void ship(String orderNo, Long operatorId);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/OrderDomainServiceImpl.java`

下面是订单领域服务实现。实际项目中这里通常会操作数据库、更新状态、写操作日志或发送领域事件。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.service.OrderDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 订单领域服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class OrderDomainServiceImpl implements OrderDomainService {

    /**
     * 支付订单
     *
     * @param orderNo    订单号
     * @param operatorId 操作人ID
     * @param amount     支付金额
     */
    @Override
    public void pay(String orderNo, Long operatorId, BigDecimal amount) {
        validateOrderNoAndOperator(orderNo, operatorId);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("支付订单失败，支付金额不合法，订单号：{}，金额：{}", orderNo, amount);
            throw new IllegalArgumentException("支付金额必须大于0");
        }

        log.info("订单支付成功，订单号：{}，操作人ID：{}，支付金额：{}", orderNo, operatorId, amount);
    }

    /**
     * 取消订单
     *
     * @param orderNo    订单号
     * @param operatorId 操作人ID
     */
    @Override
    public void cancel(String orderNo, Long operatorId) {
        validateOrderNoAndOperator(orderNo, operatorId);
        log.info("订单取消成功，订单号：{}，操作人ID：{}", orderNo, operatorId);
    }

    /**
     * 发货订单
     *
     * @param orderNo    订单号
     * @param operatorId 操作人ID
     */
    @Override
    public void ship(String orderNo, Long operatorId) {
        validateOrderNoAndOperator(orderNo, operatorId);
        log.info("订单发货成功，订单号：{}，操作人ID：{}", orderNo, operatorId);
    }

    /**
     * 校验订单号和操作人
     *
     * @param orderNo    订单号
     * @param operatorId 操作人ID
     */
    private void validateOrderNoAndOperator(String orderNo, Long operatorId) {
        if (StrUtil.isBlank(orderNo)) {
            log.warn("订单操作失败，订单号为空");
            throw new IllegalArgumentException("订单号不能为空");
        }

        if (operatorId == null || operatorId <= 0) {
            log.warn("订单操作失败，操作人ID不合法，操作人ID：{}", operatorId);
            throw new IllegalArgumentException("操作人ID必须大于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/command/OrderCommand.java`

下面是订单命令接口。每个命令都声明自己支持的动作类型，并实现统一执行方法。

```java
package io.github.atengk.design.command;

import io.github.atengk.design.dto.OrderCommandRequest;
import io.github.atengk.design.dto.OrderCommandResponse;

/**
 * 订单命令
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderCommand {

    /**
     * 获取支持的动作类型
     *
     * @return 动作类型
     */
    String supportAction();

    /**
     * 执行订单命令
     *
     * @param request 订单命令请求
     * @return 订单命令响应
     */
    OrderCommandResponse execute(OrderCommandRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/command/PayOrderCommand.java`

下面是支付订单命令。它只负责支付动作的参数校验和接收者调用。

```java
package io.github.atengk.design.command;

import io.github.atengk.design.dto.OrderCommandRequest;
import io.github.atengk.design.dto.OrderCommandResponse;
import io.github.atengk.design.service.OrderDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付订单命令
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayOrderCommand implements OrderCommand {

    private final OrderDomainService orderDomainService;

    /**
     * 获取支持的动作类型
     *
     * @return 动作类型
     */
    @Override
    public String supportAction() {
        return "pay";
    }

    /**
     * 执行订单命令
     *
     * @param request 订单命令请求
     * @return 订单命令响应
     */
    @Override
    public OrderCommandResponse execute(OrderCommandRequest request) {
        log.info("开始执行支付订单命令，订单号：{}，操作人ID：{}", request.orderNo(), request.operatorId());

        orderDomainService.pay(request.orderNo(), request.operatorId(), request.amount());

        return new OrderCommandResponse(
                supportAction(),
                request.orderNo(),
                true,
                "支付成功"
        );
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/command/CancelOrderCommand.java`

下面是取消订单命令。它封装取消订单动作。

```java
package io.github.atengk.design.command;

import io.github.atengk.design.dto.OrderCommandRequest;
import io.github.atengk.design.dto.OrderCommandResponse;
import io.github.atengk.design.service.OrderDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 取消订单命令
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CancelOrderCommand implements OrderCommand {

    private final OrderDomainService orderDomainService;

    /**
     * 获取支持的动作类型
     *
     * @return 动作类型
     */
    @Override
    public String supportAction() {
        return "cancel";
    }

    /**
     * 执行订单命令
     *
     * @param request 订单命令请求
     * @return 订单命令响应
     */
    @Override
    public OrderCommandResponse execute(OrderCommandRequest request) {
        log.info("开始执行取消订单命令，订单号：{}，操作人ID：{}", request.orderNo(), request.operatorId());

        orderDomainService.cancel(request.orderNo(), request.operatorId());

        return new OrderCommandResponse(
                supportAction(),
                request.orderNo(),
                true,
                "取消成功"
        );
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/command/ShipOrderCommand.java`

下面是发货订单命令。它封装发货订单动作。

```java
package io.github.atengk.design.command;

import io.github.atengk.design.dto.OrderCommandRequest;
import io.github.atengk.design.dto.OrderCommandResponse;
import io.github.atengk.design.service.OrderDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 发货订单命令
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShipOrderCommand implements OrderCommand {

    private final OrderDomainService orderDomainService;

    /**
     * 获取支持的动作类型
     *
     * @return 动作类型
     */
    @Override
    public String supportAction() {
        return "ship";
    }

    /**
     * 执行订单命令
     *
     * @param request 订单命令请求
     * @return 订单命令响应
     */
    @Override
    public OrderCommandResponse execute(OrderCommandRequest request) {
        log.info("开始执行发货订单命令，订单号：{}，操作人ID：{}", request.orderNo(), request.operatorId());

        orderDomainService.ship(request.orderNo(), request.operatorId());

        return new OrderCommandResponse(
                supportAction(),
                request.orderNo(),
                true,
                "发货成功"
        );
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/invoker/OrderCommandInvoker.java`

下面是订单命令调用器。它接收所有命令 Bean，按动作类型缓存，并统一执行命令。

```java
package io.github.atengk.design.invoker;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.command.OrderCommand;
import io.github.atengk.design.dto.OrderCommandRequest;
import io.github.atengk.design.dto.OrderCommandResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单命令调用器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderCommandInvoker {

    private final Map<String, OrderCommand> commandMap;

    /**
     * 创建订单命令调用器
     *
     * @param commands 订单命令列表
     */
    public OrderCommandInvoker(List<OrderCommand> commands) {
        if (CollUtil.isEmpty(commands)) {
            log.warn("订单命令列表为空");
            this.commandMap = Map.of();
            return;
        }

        this.commandMap = commands.stream()
                .collect(Collectors.toUnmodifiableMap(
                        command -> StrUtil.trim(command.supportAction()).toLowerCase(),
                        Function.identity()
                ));

        log.info("初始化订单命令调用器，支持动作：{}", commandMap.keySet());
    }

    /**
     * 执行订单命令
     *
     * @param request 订单命令请求
     * @return 订单命令响应
     */
    public OrderCommandResponse invoke(OrderCommandRequest request) {
        validateRequest(request);

        String action = StrUtil.trim(request.action()).toLowerCase();
        OrderCommand command = commandMap.get(action);

        if (command == null) {
            log.warn("执行订单命令失败，不支持的动作类型：{}", request.action());
            throw new IllegalArgumentException("不支持的动作类型：" + request.action());
        }

        log.info("匹配订单命令成功，动作：{}，订单号：{}", action, request.orderNo());
        return command.execute(request);
    }

    /**
     * 校验订单命令请求
     *
     * @param request 订单命令请求
     */
    private void validateRequest(OrderCommandRequest request) {
        if (request == null) {
            log.warn("执行订单命令失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.isBlank(request.action())) {
            log.warn("执行订单命令失败，动作类型为空");
            throw new IllegalArgumentException("动作类型不能为空");
        }

        if (StrUtil.isBlank(request.orderNo())) {
            log.warn("执行订单命令失败，订单号为空");
            throw new IllegalArgumentException("订单号不能为空");
        }

        if (request.operatorId() == null || request.operatorId() <= 0) {
            log.warn("执行订单命令失败，操作人ID不合法，操作人ID：{}", request.operatorId());
            throw new IllegalArgumentException("操作人ID必须大于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/OrderCommandController.java`

下面是订单命令接口。Controller 不直接判断动作类型，只把请求交给命令调用器。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.OrderCommandRequest;
import io.github.atengk.design.dto.OrderCommandResponse;
import io.github.atengk.design.invoker.OrderCommandInvoker;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 订单命令控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/command/order")
public class OrderCommandController {

    private final OrderCommandInvoker orderCommandInvoker;

    /**
     * 执行订单命令
     *
     * @param action     操作类型
     * @param orderNo    订单号
     * @param operatorId 操作人ID
     * @param amount     操作金额
     * @return 订单命令响应
     */
    @PostMapping("/execute")
    public OrderCommandResponse execute(@RequestParam String action,
                                        @RequestParam String orderNo,
                                        @RequestParam Long operatorId,
                                        @RequestParam(required = false) BigDecimal amount) {
        OrderCommandRequest request = new OrderCommandRequest(
                action,
                orderNo,
                operatorId,
                amount
        );

        return orderCommandInvoker.invoke(request);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/command/order/execute?action=pay&orderNo=ORDER10001&operatorId=10001&amount=99.90"

curl -X POST "http://localhost:8080/command/order/execute?action=cancel&orderNo=ORDER10002&operatorId=10001"

curl -X POST "http://localhost:8080/command/order/execute?action=ship&orderNo=ORDER10003&operatorId=10001"
```

支付成功可能返回：

```json
{
  "action": "pay",
  "orderNo": "ORDER10001",
  "success": true,
  "message": "支付成功"
}
```

这种方式的优点是 Controller 不需要写 `if else` 判断动作类型。新增操作时，只需要新增一个命令 Bean。

## 扩展一个新命令

在 Spring Boot 命令模式中，新增动作通常只需要新增一个命令实现类。下面以确认收货命令为例。

首先在接收者中新增确认收货方法。

文件位置：`src/main/java/io/github/atengk/design/service/OrderDomainService.java`

```java
/**
 * 确认收货
 *
 * @param orderNo    订单号
 * @param operatorId 操作人ID
 */
void receive(String orderNo, Long operatorId);
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/OrderDomainServiceImpl.java`

```java
/**
 * 确认收货
 *
 * @param orderNo    订单号
 * @param operatorId 操作人ID
 */
@Override
public void receive(String orderNo, Long operatorId) {
    validateOrderNoAndOperator(orderNo, operatorId);
    log.info("订单确认收货成功，订单号：{}，操作人ID：{}", orderNo, operatorId);
}
```

文件位置：`src/main/java/io/github/atengk/design/command/ReceiveOrderCommand.java`

下面是确认收货命令。该命令被 Spring 扫描后，会自动加入 `OrderCommandInvoker` 的命令映射中。

```java
package io.github.atengk.design.command;

import io.github.atengk.design.dto.OrderCommandRequest;
import io.github.atengk.design.dto.OrderCommandResponse;
import io.github.atengk.design.service.OrderDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 确认收货命令
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReceiveOrderCommand implements OrderCommand {

    private final OrderDomainService orderDomainService;

    /**
     * 获取支持的动作类型
     *
     * @return 动作类型
     */
    @Override
    public String supportAction() {
        return "receive";
    }

    /**
     * 执行订单命令
     *
     * @param request 订单命令请求
     * @return 订单命令响应
     */
    @Override
    public OrderCommandResponse execute(OrderCommandRequest request) {
        log.info("开始执行确认收货命令，订单号：{}，操作人ID：{}", request.orderNo(), request.operatorId());

        orderDomainService.receive(request.orderNo(), request.operatorId());

        return new OrderCommandResponse(
                supportAction(),
                request.orderNo(),
                true,
                "确认收货成功"
        );
    }
}
```

调用示例：

```bash
curl -X POST "http://localhost:8080/command/order/execute?action=receive&orderNo=ORDER10004&operatorId=10001"
```

新增命令后，`OrderCommandController` 和 `OrderCommandInvoker` 不需要修改。

## 命令队列

命令模式的一个重要扩展是命令队列。命令对象可以先被放入队列，再统一执行。它适合批量操作、异步执行、失败重试、延迟执行等场景。

下面是一个简化的本地命令队列示例。

### 文件结构

```text
src/main/java/io/github/atengk/design/command/queue/
├── CommandTask.java
└── LocalCommandQueue.java
```

文件位置：`src/main/java/io/github/atengk/design/command/queue/CommandTask.java`

下面是命令任务对象，用于包装可执行命令和任务编号。

```java
package io.github.atengk.design.command.queue;

import cn.hutool.core.util.IdUtil;

/**
 * 命令任务
 *
 * @param taskId   任务ID
 * @param taskName 任务名称
 * @param runnable 可执行命令
 * @author Ateng
 * @since 2026-04-30
 */
public record CommandTask(
        String taskId,
        String taskName,
        Runnable runnable
) {

    /**
     * 创建命令任务
     *
     * @param taskName 任务名称
     * @param runnable 可执行命令
     * @return 命令任务
     */
    public static CommandTask of(String taskName, Runnable runnable) {
        return new CommandTask("CMD" + IdUtil.getSnowflakeNextId(), taskName, runnable);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/command/queue/LocalCommandQueue.java`

下面是本地命令队列。示例使用内存队列，生产环境可替换为 MQ、数据库任务表或调度系统。

```java
package io.github.atengk.design.command.queue;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 本地命令队列
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class LocalCommandQueue {

    private final Queue<CommandTask> queue = new ConcurrentLinkedQueue<>();

    /**
     * 添加命令任务
     *
     * @param task 命令任务
     */
    public void addTask(CommandTask task) {
        if (task == null || StrUtil.isBlank(task.taskName()) || task.runnable() == null) {
            log.warn("添加命令任务失败，任务不完整");
            throw new IllegalArgumentException("命令任务不能为空");
        }

        queue.offer(task);
        log.info("添加命令任务成功，任务ID：{}，任务名称：{}，队列大小：{}", task.taskId(), task.taskName(), queue.size());
    }

    /**
     * 执行所有命令任务
     */
    public void executeAll() {
        CommandTask task;
        while ((task = queue.poll()) != null) {
            try {
                log.info("开始执行命令任务，任务ID：{}，任务名称：{}", task.taskId(), task.taskName());
                task.runnable().run();
                log.info("命令任务执行成功，任务ID：{}", task.taskId());
            } catch (Exception exception) {
                log.warn("命令任务执行失败，任务ID：{}，异常：{}", task.taskId(), exception.getMessage());
            }
        }
    }
}
```

使用方式：

```java
LocalCommandQueue commandQueue = new LocalCommandQueue();

commandQueue.addTask(CommandTask.of("支付订单", () -> orderDomainService.pay("ORDER10001", 10001L, BigDecimal.valueOf(99.90))));
commandQueue.addTask(CommandTask.of("发货订单", () -> orderDomainService.ship("ORDER10001", 10001L)));

commandQueue.executeAll();
```

本地队列只适合演示或单 JVM 内部任务。如果要保证任务可靠执行，建议使用数据库任务表、RabbitMQ、Kafka、Redis Stream、XXL-JOB 等方案。

## 命令模式和策略模式的区别

命令模式和策略模式在代码结构上都可能表现为“接口 + 多个实现 + 上下文分发”，但二者关注点不同。

| 对比项         | 命令模式                     | 策略模式                     |
| -------------- | ---------------------------- | ---------------------------- |
| 核心目的       | 把请求封装成对象             | 从多个算法中选择一个执行     |
| 是否强调接收者 | 强调，命令通常调用 Receiver  | 不强调，策略通常自己完成算法 |
| 是否适合队列   | 适合，可排队、延迟、重试     | 通常不强调排队               |
| 是否适合撤销   | 适合，可封装 undo            | 通常不处理撤销               |
| 典型场景       | 订单操作、菜单动作、任务调度 | 优惠计算、支付渠道、物流计费 |

简单理解：

```text
命令模式：把“做这件事”封装成一个对象，可以执行、排队、记录、撤销。
策略模式：把“怎么算”封装成一个算法，可以替换。
```

订单支付、取消、发货这些操作更像命令。满减、折扣、新人优惠这些算法更像策略。

## 命令模式和责任链模式的区别

命令模式和责任链模式都能减少主流程中的分支代码，但处理方式不同。

| 对比项       | 命令模式                     | 责任链模式               |
| ------------ | ---------------------------- | ------------------------ |
| 核心目的     | 封装一个请求操作             | 多个处理器按顺序处理请求 |
| 执行数量     | 通常执行一个命令             | 通常执行多个处理器       |
| 是否强调顺序 | 命令队列时强调               | 强调链路顺序             |
| 典型场景     | 操作按钮、任务调度、异步命令 | 参数校验、风控链、过滤链 |
| 中断方式     | 命令执行成功或失败           | 任意处理器可中断链路     |

简单理解：

```text
命令模式：我要执行一个动作。
责任链模式：这个请求要经过多个关卡。
```

订单执行“支付”动作适合命令模式。订单提交前依次经过参数校验、库存校验、风控校验，更适合责任链模式。

## 命令模式和状态模式的关系

命令模式和状态模式可以组合使用。命令模式封装“要执行什么动作”，状态模式判断“当前状态是否允许执行这个动作”。

例如订单支付命令可以先查询订单状态，再交给状态处理器判断是否允许支付。

组合方式如下：

```text
PayOrderCommand
    -> 查询订单当前状态
    -> OrderStateContext.pay(command)
    -> 状态允许则更新订单
```

这种组合适合订单、工单、审批这类状态流转严格的业务。命令负责动作封装，状态模式负责状态规则。

不要把所有状态规则都塞进命令中，否则命令类会越来越复杂。

不推荐写法：

```java
public class PayOrderCommand implements OrderCommand {

    public OrderCommandResponse execute(OrderCommandRequest request) {
        // 判断 CREATED 可以支付
        // 判断 PAID 不能重复支付
        // 判断 CANCELED 不能支付
        // 判断 FINISHED 不能支付
        // 执行支付
        return null;
    }
}
```

推荐将状态规则交给状态上下文：

```java
public OrderCommandResponse execute(OrderCommandRequest request) {
    orderStateContext.pay(command);
    return new OrderCommandResponse("pay", request.orderNo(), true, "支付成功");
}
```

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行支付命令：

```bash
curl -X POST "http://localhost:8080/command/order/execute?action=pay&orderNo=ORDER10001&operatorId=10001&amount=99.90"
```

执行取消命令：

```bash
curl -X POST "http://localhost:8080/command/order/execute?action=cancel&orderNo=ORDER10002&operatorId=10001"
```

执行发货命令：

```bash
curl -X POST "http://localhost:8080/command/order/execute?action=ship&orderNo=ORDER10003&operatorId=10001"
```

如果命令模式正常，可以看到类似日志：

```text
初始化订单命令调用器，支持动作：[pay, cancel, ship]
匹配订单命令成功，动作：pay，订单号：ORDER10001
开始执行支付订单命令，订单号：ORDER10001，操作人ID：10001
订单支付成功，订单号：ORDER10001，操作人ID：10001，支付金额：99.90
```

执行不支持的命令：

```bash
curl -X POST "http://localhost:8080/command/order/execute?action=refund&orderNo=ORDER10004&operatorId=10001"
```

异常日志示例：

```text
执行订单命令失败，不支持的动作类型：refund
```

实际项目中建议结合全局异常处理器，将业务异常转换成统一响应结构。

## 注意事项

命令模式适合封装操作，但不建议把所有业务逻辑都放进命令类。命令类应该负责动作封装、参数适配、调用接收者，不应该承担复杂领域规则。

推荐命令类承担这些职责：

```text
声明支持的动作
校验动作级参数
调用 Receiver 执行业务
组装命令响应
记录命令级日志
```

不推荐命令类承担这些职责：

```text
大量数据库 CRUD
复杂状态机判断
复杂营销规则
复杂库存扣减算法
多个子系统深度编排
```

错误示例：

```java
public class PayOrderCommand implements OrderCommand {

    public OrderCommandResponse execute(OrderCommandRequest request) {
        // 查询订单
        // 判断状态
        // 计算优惠
        // 扣减库存
        // 创建支付单
        // 调用支付渠道
        // 发送通知
        // 写审计
        return null;
    }
}
```

推荐将命令作为操作入口，把具体能力交给领域服务或外观服务：

```java
public OrderCommandResponse execute(OrderCommandRequest request) {
    orderDomainService.pay(request.orderNo(), request.operatorId(), request.amount());
    return new OrderCommandResponse(supportAction(), request.orderNo(), true, "支付成功");
}
```

如果命令需要撤销，必须明确撤销的业务含义。不是所有操作都能简单撤销。

适合撤销的操作：

```text
编辑器操作
本地内存操作
草稿配置变更
未提交事务内的操作
```

不适合简单撤销的操作：

```text
已完成支付
已发货订单
已发送短信
已提交第三方系统的请求
已对外产生法律或资金效果的操作
```

对于无法简单撤销的业务，应使用补偿命令，而不是直接 undo。例如支付成功后不能直接撤销支付命令，而是执行退款命令。

```text
支付命令 -> 补偿命令：退款命令
发货命令 -> 补偿命令：拦截发货或售后退货
发送短信命令 -> 通常不可撤销，只能发送补充通知
```

Spring Bean 默认是单例，命令类中不要保存请求级状态。

错误示例：

```java
private String currentOrderNo;
private Long currentOperatorId;
private BigDecimal currentAmount;
```

推荐将请求数据放在方法参数、局部变量或命令请求对象中。

```java
public OrderCommandResponse execute(OrderCommandRequest request) {
    orderDomainService.pay(request.orderNo(), request.operatorId(), request.amount());
    return new OrderCommandResponse(supportAction(), request.orderNo(), true, "支付成功");
}
```

如果命令需要异步执行或失败重试，不建议只使用本地内存队列。生产环境建议使用可靠存储或消息中间件。

常见生产方案：

```text
命令请求入库 -> 定时任务扫描执行 -> 记录执行结果 -> 失败重试
命令请求投递 MQ -> 消费者执行 -> 失败重试或死信队列
命令请求提交 XXL-JOB -> 调度执行 -> 记录日志
```

对于核心业务命令，还需要考虑幂等和并发安全。例如支付命令需要避免重复支付，发货命令需要避免重复发货。

常见做法：

```text
命令唯一请求号
数据库唯一索引
状态条件更新
分布式锁
操作日志表
幂等结果缓存
```

## 总结

在 JDK21 和 Spring Boot 3 项目中，命令模式的实践重点是把一次业务操作封装成独立命令对象，让调用方、命令对象和真正执行者之间解耦。

普通 Java 命令适合理解原理、撤销重做和本地操作封装。Spring Boot 项目中更推荐使用“命令接口 + 多个命令 Bean + 命令调用器 + Receiver 领域服务”的结构。对于订单操作、任务调度、菜单动作、审批动作、异步任务、批量执行等场景，命令模式可以显著降低调用方复杂度，并为命令日志、异步队列、失败重试和补偿机制提供扩展空间。

命令模式不是为了替代所有 Service 方法，而是为了处理“操作本身需要被对象化、可调度、可记录、可排队、可扩展”的场景。实际落地时，需要结合状态模式、幂等控制、事务、消息队列和补偿机制，才能保证核心业务命令可靠执行。
