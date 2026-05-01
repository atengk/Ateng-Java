# 设计模式：状态模式

状态模式用于把对象在不同状态下的行为拆分到独立状态类中，避免在业务代码中堆积大量基于状态字段的 `if else` 或 `switch` 判断。在 JDK21 和 Spring Boot 3 项目中，状态模式常用于订单状态流转、审批状态流转、任务状态流转、支付单状态、工单处理、优惠券生命周期、文件处理任务等场景。

需要注意：状态模式关注的是“同一个对象在不同状态下行为不同”。如果只是根据类型选择不同算法，更适合策略模式；如果是一个请求依次经过多个处理步骤，更适合责任链模式；如果是固定流程中某些步骤可变，更适合模板方法模式。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证状态模式行为 -->
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

状态模式的核心目标是让状态自己决定当前状态下允许执行什么行为，以及执行行为后如何流转到下一个状态。

常见角色如下：

| 角色          | 说明                                                 |
| ------------- | ---------------------------------------------------- |
| Context       | 上下文对象，持有当前状态，并把操作委托给状态对象     |
| State         | 状态接口，定义不同状态下支持的行为                   |
| ConcreteState | 具体状态类，实现当前状态下的业务行为和状态流转       |
| StateFactory  | 状态工厂或状态上下文，根据当前状态获取对应状态处理器 |
| Client        | 调用方，只关心执行操作，不直接写状态判断逻辑         |

常见实现方式如下：

| 实现方式                | 是否推荐         | 适用场景                                     |
| ----------------------- | ---------------- | -------------------------------------------- |
| 普通 Java 状态模式      | 推荐用于理解原理 | 本地状态流转、简单状态机                     |
| Spring Bean 状态模式    | 强烈推荐         | Spring Boot 项目中的订单、审批、工单状态流转 |
| 枚举状态机              | 推荐用于简单规则 | 状态和动作较少，流转规则固定                 |
| 状态模式 + 工厂         | 推荐             | 根据当前状态获取对应状态处理器               |
| 大量 `if else` 判断状态 | 不推荐           | 状态增多后维护成本高                         |

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring Bean 状态模式 > 普通 Java 状态模式 > 大量 if else 状态判断
```

状态模式适合状态数量较多、状态行为差异明显、状态流转规则相对复杂的场景。

## 普通 Java 状态模式

普通 Java 状态模式适合不依赖 Spring 容器的状态流转场景。下面以任务处理为例，任务有待处理、处理中、已完成、已取消四种状态。不同状态下允许执行的操作不同。

状态流转如下：

```text
待处理 -> 开始处理 -> 处理中 -> 完成任务 -> 已完成
待处理 -> 取消任务 -> 已取消
处理中 -> 取消任务 -> 已取消
```

如果使用 `if else`，每个操作方法都要判断当前状态。使用状态模式后，每个状态类只处理自己状态下允许的行为。

### 文件结构

```text
src/main/java/io/github/atengk/design/state/simple/
├── TaskStatus.java
├── TaskContext.java
├── TaskState.java
├── PendingTaskState.java
├── ProcessingTaskState.java
├── FinishedTaskState.java
└── CanceledTaskState.java
```

文件位置：`src/main/java/io/github/atengk/design/state/simple/TaskStatus.java`

下面是任务状态枚举。

```java
package io.github.atengk.design.state.simple;

/**
 * 任务状态
 *
 * @author Ateng
 * @since 2026-04-30
 */
public enum TaskStatus {

    /**
     * 待处理
     */
    PENDING,

    /**
     * 处理中
     */
    PROCESSING,

    /**
     * 已完成
     */
    FINISHED,

    /**
     * 已取消
     */
    CANCELED
}
```

文件位置：`src/main/java/io/github/atengk/design/state/simple/TaskState.java`

下面是任务状态接口，定义不同状态下可能执行的操作。

```java
package io.github.atengk.design.state.simple;

/**
 * 任务状态
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface TaskState {

    /**
     * 开始处理任务
     *
     * @param context 任务上下文
     */
    void start(TaskContext context);

    /**
     * 完成任务
     *
     * @param context 任务上下文
     */
    void finish(TaskContext context);

    /**
     * 取消任务
     *
     * @param context 任务上下文
     */
    void cancel(TaskContext context);

    /**
     * 获取状态
     *
     * @return 任务状态
     */
    TaskStatus status();
}
```

文件位置：`src/main/java/io/github/atengk/design/state/simple/TaskContext.java`

下面是任务上下文对象，持有当前状态，并把操作委托给当前状态对象。

```java
package io.github.atengk.design.state.simple;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务上下文
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Getter
public class TaskContext {

    private final String taskId;
    private TaskState state;

    /**
     * 创建任务上下文
     *
     * @param taskId 任务ID
     * @param state  初始状态
     */
    public TaskContext(String taskId, TaskState state) {
        if (StrUtil.isBlank(taskId)) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        if (state == null) {
            throw new IllegalArgumentException("任务状态不能为空");
        }

        this.taskId = taskId;
        this.state = state;
    }

    /**
     * 切换任务状态
     *
     * @param state 新状态
     */
    public void changeState(TaskState state) {
        if (state == null) {
            throw new IllegalArgumentException("新状态不能为空");
        }

        log.info("任务状态变更，任务ID：{}，原状态：{}，新状态：{}", taskId, this.state.status(), state.status());
        this.state = state;
    }

    /**
     * 开始处理任务
     */
    public void start() {
        state.start(this);
    }

    /**
     * 完成任务
     */
    public void finish() {
        state.finish(this);
    }

    /**
     * 取消任务
     */
    public void cancel() {
        state.cancel(this);
    }

    /**
     * 获取当前状态
     *
     * @return 当前任务状态
     */
    public TaskStatus currentStatus() {
        return state.status();
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/state/simple/PendingTaskState.java`

下面是待处理状态，允许开始处理或取消任务。

```java
package io.github.atengk.design.state.simple;

import lombok.extern.slf4j.Slf4j;

/**
 * 待处理任务状态
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class PendingTaskState implements TaskState {

    /**
     * 开始处理任务
     *
     * @param context 任务上下文
     */
    @Override
    public void start(TaskContext context) {
        log.info("待处理任务开始处理，任务ID：{}", context.getTaskId());
        context.changeState(new ProcessingTaskState());
    }

    /**
     * 完成任务
     *
     * @param context 任务上下文
     */
    @Override
    public void finish(TaskContext context) {
        log.warn("待处理任务不能直接完成，任务ID：{}", context.getTaskId());
        throw new IllegalStateException("待处理任务不能直接完成");
    }

    /**
     * 取消任务
     *
     * @param context 任务上下文
     */
    @Override
    public void cancel(TaskContext context) {
        log.info("待处理任务取消成功，任务ID：{}", context.getTaskId());
        context.changeState(new CanceledTaskState());
    }

    /**
     * 获取状态
     *
     * @return 任务状态
     */
    @Override
    public TaskStatus status() {
        return TaskStatus.PENDING;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/state/simple/ProcessingTaskState.java`

下面是处理中状态，允许完成任务或取消任务。

```java
package io.github.atengk.design.state.simple;

import lombok.extern.slf4j.Slf4j;

/**
 * 处理中任务状态
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class ProcessingTaskState implements TaskState {

    /**
     * 开始处理任务
     *
     * @param context 任务上下文
     */
    @Override
    public void start(TaskContext context) {
        log.warn("任务已经处于处理中，不能重复开始，任务ID：{}", context.getTaskId());
        throw new IllegalStateException("任务已经处于处理中");
    }

    /**
     * 完成任务
     *
     * @param context 任务上下文
     */
    @Override
    public void finish(TaskContext context) {
        log.info("任务处理完成，任务ID：{}", context.getTaskId());
        context.changeState(new FinishedTaskState());
    }

    /**
     * 取消任务
     *
     * @param context 任务上下文
     */
    @Override
    public void cancel(TaskContext context) {
        log.info("处理中任务取消成功，任务ID：{}", context.getTaskId());
        context.changeState(new CanceledTaskState());
    }

    /**
     * 获取状态
     *
     * @return 任务状态
     */
    @Override
    public TaskStatus status() {
        return TaskStatus.PROCESSING;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/state/simple/FinishedTaskState.java`

下面是已完成状态。已完成任务不允许继续执行开始、完成或取消操作。

```java
package io.github.atengk.design.state.simple;

import lombok.extern.slf4j.Slf4j;

/**
 * 已完成任务状态
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class FinishedTaskState implements TaskState {

    /**
     * 开始处理任务
     *
     * @param context 任务上下文
     */
    @Override
    public void start(TaskContext context) {
        log.warn("已完成任务不能重新开始，任务ID：{}", context.getTaskId());
        throw new IllegalStateException("已完成任务不能重新开始");
    }

    /**
     * 完成任务
     *
     * @param context 任务上下文
     */
    @Override
    public void finish(TaskContext context) {
        log.warn("任务已经完成，不能重复完成，任务ID：{}", context.getTaskId());
        throw new IllegalStateException("任务已经完成");
    }

    /**
     * 取消任务
     *
     * @param context 任务上下文
     */
    @Override
    public void cancel(TaskContext context) {
        log.warn("已完成任务不能取消，任务ID：{}", context.getTaskId());
        throw new IllegalStateException("已完成任务不能取消");
    }

    /**
     * 获取状态
     *
     * @return 任务状态
     */
    @Override
    public TaskStatus status() {
        return TaskStatus.FINISHED;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/state/simple/CanceledTaskState.java`

下面是已取消状态。已取消任务不允许继续执行状态变更操作。

```java
package io.github.atengk.design.state.simple;

import lombok.extern.slf4j.Slf4j;

/**
 * 已取消任务状态
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class CanceledTaskState implements TaskState {

    /**
     * 开始处理任务
     *
     * @param context 任务上下文
     */
    @Override
    public void start(TaskContext context) {
        log.warn("已取消任务不能开始处理，任务ID：{}", context.getTaskId());
        throw new IllegalStateException("已取消任务不能开始处理");
    }

    /**
     * 完成任务
     *
     * @param context 任务上下文
     */
    @Override
    public void finish(TaskContext context) {
        log.warn("已取消任务不能完成，任务ID：{}", context.getTaskId());
        throw new IllegalStateException("已取消任务不能完成");
    }

    /**
     * 取消任务
     *
     * @param context 任务上下文
     */
    @Override
    public void cancel(TaskContext context) {
        log.warn("任务已经取消，不能重复取消，任务ID：{}", context.getTaskId());
        throw new IllegalStateException("任务已经取消");
    }

    /**
     * 获取状态
     *
     * @return 任务状态
     */
    @Override
    public TaskStatus status() {
        return TaskStatus.CANCELED;
    }
}
```

使用方式：

```java
TaskContext taskContext = new TaskContext("TASK10001", new PendingTaskState());

taskContext.start();
taskContext.finish();

TaskStatus status = taskContext.currentStatus();
```

执行后状态会从 `PENDING` 流转到 `PROCESSING`，再流转到 `FINISHED`。如果对已完成任务执行取消操作，会由 `FinishedTaskState` 抛出异常。

## Spring Boot 状态模式

Spring Boot 项目中更常见的写法，是把每个状态处理器注册为 Spring Bean，再由状态上下文根据当前状态选择对应处理器。下面以订单状态流转为例。

订单状态如下：

```text
CREATED   已创建
PAID      已支付
SHIPPED   已发货
FINISHED  已完成
CANCELED  已取消
```

状态流转规则如下：

```text
已创建 -> 支付 -> 已支付
已创建 -> 取消 -> 已取消
已支付 -> 发货 -> 已发货
已发货 -> 完成 -> 已完成
```

不允许的操作会直接抛出异常。例如已取消订单不能支付，已完成订单不能取消。

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── StateApplication.java
├── controller/
│   └── OrderStateController.java
├── context/
│   └── OrderStateContext.java
├── dto/
│   ├── OrderOperateCommand.java
│   └── OrderOperateResponse.java
├── enums/
│   └── OrderStatus.java
└── state/
    ├── OrderStateHandler.java
    ├── AbstractOrderStateHandler.java
    ├── CreatedOrderStateHandler.java
    ├── PaidOrderStateHandler.java
    ├── ShippedOrderStateHandler.java
    ├── FinishedOrderStateHandler.java
    └── CanceledOrderStateHandler.java
```

文件位置：`src/main/java/io/github/atengk/design/StateApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 状态模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class StateApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(StateApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/enums/OrderStatus.java`

下面是订单状态枚举。

```java
package io.github.atengk.design.enums;

/**
 * 订单状态
 *
 * @author Ateng
 * @since 2026-04-30
 */
public enum OrderStatus {

    /**
     * 已创建
     */
    CREATED,

    /**
     * 已支付
     */
    PAID,

    /**
     * 已发货
     */
    SHIPPED,

    /**
     * 已完成
     */
    FINISHED,

    /**
     * 已取消
     */
    CANCELED
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderOperateCommand.java`

下面是订单操作命令对象。示例中通过 `currentStatus` 模拟订单当前状态，实际项目中通常从数据库查询订单当前状态。

```java
package io.github.atengk.design.dto;

import io.github.atengk.design.enums.OrderStatus;

/**
 * 订单操作命令
 *
 * @param orderNo       订单号
 * @param currentStatus 当前订单状态
 * @param operatorId    操作人ID
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderOperateCommand(
        String orderNo,
        OrderStatus currentStatus,
        Long operatorId
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderOperateResponse.java`

下面是订单操作响应对象。

```java
package io.github.atengk.design.dto;

import io.github.atengk.design.enums.OrderStatus;

/**
 * 订单操作响应
 *
 * @param orderNo      订单号
 * @param beforeStatus 操作前状态
 * @param afterStatus  操作后状态
 * @param message      结果消息
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderOperateResponse(
        String orderNo,
        OrderStatus beforeStatus,
        OrderStatus afterStatus,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/state/OrderStateHandler.java`

下面是订单状态处理器接口。每个状态处理器都实现当前状态下允许执行的动作。

```java
package io.github.atengk.design.state;

import io.github.atengk.design.dto.OrderOperateCommand;
import io.github.atengk.design.dto.OrderOperateResponse;
import io.github.atengk.design.enums.OrderStatus;

/**
 * 订单状态处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderStateHandler {

    /**
     * 获取支持的订单状态
     *
     * @return 订单状态
     */
    OrderStatus supportStatus();

    /**
     * 支付订单
     *
     * @param command 订单操作命令
     * @return 订单操作响应
     */
    OrderOperateResponse pay(OrderOperateCommand command);

    /**
     * 发货订单
     *
     * @param command 订单操作命令
     * @return 订单操作响应
     */
    OrderOperateResponse ship(OrderOperateCommand command);

    /**
     * 完成订单
     *
     * @param command 订单操作命令
     * @return 订单操作响应
     */
    OrderOperateResponse finish(OrderOperateCommand command);

    /**
     * 取消订单
     *
     * @param command 订单操作命令
     * @return 订单操作响应
     */
    OrderOperateResponse cancel(OrderOperateCommand command);
}
```

文件位置：`src/main/java/io/github/atengk/design/state/AbstractOrderStateHandler.java`

下面是订单状态抽象处理器，提供不支持操作的默认实现，具体状态类只需要覆盖自己允许的动作。

```java
package io.github.atengk.design.state;

import io.github.atengk.design.dto.OrderOperateCommand;
import io.github.atengk.design.dto.OrderOperateResponse;
import io.github.atengk.design.enums.OrderStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * 订单状态抽象处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public abstract class AbstractOrderStateHandler implements OrderStateHandler {

    /**
     * 支付订单
     *
     * @param command 订单操作命令
     * @return 订单操作响应
     */
    @Override
    public OrderOperateResponse pay(OrderOperateCommand command) {
        return unsupported(command, "支付");
    }

    /**
     * 发货订单
     *
     * @param command 订单操作命令
     * @return 订单操作响应
     */
    @Override
    public OrderOperateResponse ship(OrderOperateCommand command) {
        return unsupported(command, "发货");
    }

    /**
     * 完成订单
     *
     * @param command 订单操作命令
     * @return 订单操作响应
     */
    @Override
    public OrderOperateResponse finish(OrderOperateCommand command) {
        return unsupported(command, "完成");
    }

    /**
     * 取消订单
     *
     * @param command 订单操作命令
     * @return 订单操作响应
     */
    @Override
    public OrderOperateResponse cancel(OrderOperateCommand command) {
        return unsupported(command, "取消");
    }

    /**
     * 构建状态流转响应
     *
     * @param command     订单操作命令
     * @param afterStatus 操作后状态
     * @param message     结果消息
     * @return 订单操作响应
     */
    protected OrderOperateResponse response(OrderOperateCommand command, OrderStatus afterStatus, String message) {
        log.info("订单状态流转成功，订单号：{}，操作前状态：{}，操作后状态：{}，结果：{}",
                command.orderNo(), command.currentStatus(), afterStatus, message);

        return new OrderOperateResponse(
                command.orderNo(),
                command.currentStatus(),
                afterStatus,
                message
        );
    }

    /**
     * 不支持的状态操作
     *
     * @param command 订单操作命令
     * @param action  操作名称
     * @return 订单操作响应
     */
    private OrderOperateResponse unsupported(OrderOperateCommand command, String action) {
        log.warn("订单状态操作失败，当前状态不支持该操作，订单号：{}，当前状态：{}，操作：{}",
                command.orderNo(), command.currentStatus(), action);

        throw new IllegalStateException("当前状态不支持" + action + "操作：" + command.currentStatus());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/state/CreatedOrderStateHandler.java`

下面是已创建状态处理器。已创建订单允许支付和取消。

```java
package io.github.atengk.design.state;

import io.github.atengk.design.dto.OrderOperateCommand;
import io.github.atengk.design.dto.OrderOperateResponse;
import io.github.atengk.design.enums.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 已创建订单状态处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class CreatedOrderStateHandler extends AbstractOrderStateHandler {

    /**
     * 获取支持的订单状态
     *
     * @return 订单状态
     */
    @Override
    public OrderStatus supportStatus() {
        return OrderStatus.CREATED;
    }

    /**
     * 支付订单
     *
     * @param command 订单操作命令
     * @return 订单操作响应
     */
    @Override
    public OrderOperateResponse pay(OrderOperateCommand command) {
        log.info("执行订单支付，订单号：{}，操作人ID：{}", command.orderNo(), command.operatorId());
        return response(command, OrderStatus.PAID, "支付成功");
    }

    /**
     * 取消订单
     *
     * @param command 订单操作命令
     * @return 订单操作响应
     */
    @Override
    public OrderOperateResponse cancel(OrderOperateCommand command) {
        log.info("取消已创建订单，订单号：{}，操作人ID：{}", command.orderNo(), command.operatorId());
        return response(command, OrderStatus.CANCELED, "取消成功");
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/state/PaidOrderStateHandler.java`

下面是已支付状态处理器。已支付订单允许发货。

```java
package io.github.atengk.design.state;

import io.github.atengk.design.dto.OrderOperateCommand;
import io.github.atengk.design.dto.OrderOperateResponse;
import io.github.atengk.design.enums.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 已支付订单状态处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class PaidOrderStateHandler extends AbstractOrderStateHandler {

    /**
     * 获取支持的订单状态
     *
     * @return 订单状态
     */
    @Override
    public OrderStatus supportStatus() {
        return OrderStatus.PAID;
    }

    /**
     * 发货订单
     *
     * @param command 订单操作命令
     * @return 订单操作响应
     */
    @Override
    public OrderOperateResponse ship(OrderOperateCommand command) {
        log.info("执行订单发货，订单号：{}，操作人ID：{}", command.orderNo(), command.operatorId());
        return response(command, OrderStatus.SHIPPED, "发货成功");
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/state/ShippedOrderStateHandler.java`

下面是已发货状态处理器。已发货订单允许完成。

```java
package io.github.atengk.design.state;

import io.github.atengk.design.dto.OrderOperateCommand;
import io.github.atengk.design.dto.OrderOperateResponse;
import io.github.atengk.design.enums.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 已发货订单状态处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class ShippedOrderStateHandler extends AbstractOrderStateHandler {

    /**
     * 获取支持的订单状态
     *
     * @return 订单状态
     */
    @Override
    public OrderStatus supportStatus() {
        return OrderStatus.SHIPPED;
    }

    /**
     * 完成订单
     *
     * @param command 订单操作命令
     * @return 订单操作响应
     */
    @Override
    public OrderOperateResponse finish(OrderOperateCommand command) {
        log.info("执行订单完成，订单号：{}，操作人ID：{}", command.orderNo(), command.operatorId());
        return response(command, OrderStatus.FINISHED, "完成成功");
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/state/FinishedOrderStateHandler.java`

下面是已完成状态处理器。已完成订单不允许继续执行状态变更操作，因此使用抽象类默认的不支持逻辑即可。

```java
package io.github.atengk.design.state;

import io.github.atengk.design.enums.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * 已完成订单状态处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Component
public class FinishedOrderStateHandler extends AbstractOrderStateHandler {

    /**
     * 获取支持的订单状态
     *
     * @return 订单状态
     */
    @Override
    public OrderStatus supportStatus() {
        return OrderStatus.FINISHED;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/state/CanceledOrderStateHandler.java`

下面是已取消状态处理器。已取消订单不允许继续执行状态变更操作。

```java
package io.github.atengk.design.state;

import io.github.atengk.design.enums.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * 已取消订单状态处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Component
public class CanceledOrderStateHandler extends AbstractOrderStateHandler {

    /**
     * 获取支持的订单状态
     *
     * @return 订单状态
     */
    @Override
    public OrderStatus supportStatus() {
        return OrderStatus.CANCELED;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/context/OrderStateContext.java`

下面是订单状态上下文。它接收所有状态处理器，并根据当前订单状态选择对应处理器执行操作。

```java
package io.github.atengk.design.context;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.OrderOperateCommand;
import io.github.atengk.design.dto.OrderOperateResponse;
import io.github.atengk.design.enums.OrderStatus;
import io.github.atengk.design.state.OrderStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 订单状态上下文
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderStateContext {

    private final Map<OrderStatus, OrderStateHandler> handlerMap = new EnumMap<>(OrderStatus.class);

    /**
     * 创建订单状态上下文
     *
     * @param handlers 订单状态处理器列表
     */
    public OrderStateContext(List<OrderStateHandler> handlers) {
        if (CollUtil.isEmpty(handlers)) {
            log.warn("订单状态处理器列表为空");
            return;
        }

        for (OrderStateHandler handler : handlers) {
            handlerMap.put(handler.supportStatus(), handler);
        }

        log.info("初始化订单状态上下文，支持状态：{}", handlerMap.keySet());
    }

    /**
     * 支付订单
     *
     * @param command 订单操作命令
     * @return 订单操作响应
     */
    public OrderOperateResponse pay(OrderOperateCommand command) {
        return getHandler(command).pay(command);
    }

    /**
     * 发货订单
     *
     * @param command 订单操作命令
     * @return 订单操作响应
     */
    public OrderOperateResponse ship(OrderOperateCommand command) {
        return getHandler(command).ship(command);
    }

    /**
     * 完成订单
     *
     * @param command 订单操作命令
     * @return 订单操作响应
     */
    public OrderOperateResponse finish(OrderOperateCommand command) {
        return getHandler(command).finish(command);
    }

    /**
     * 取消订单
     *
     * @param command 订单操作命令
     * @return 订单操作响应
     */
    public OrderOperateResponse cancel(OrderOperateCommand command) {
        return getHandler(command).cancel(command);
    }

    /**
     * 获取订单状态处理器
     *
     * @param command 订单操作命令
     * @return 订单状态处理器
     */
    private OrderStateHandler getHandler(OrderOperateCommand command) {
        validateCommand(command);

        OrderStateHandler handler = handlerMap.get(command.currentStatus());
        if (handler == null) {
            log.warn("获取订单状态处理器失败，状态未注册，订单号：{}，当前状态：{}",
                    command.orderNo(), command.currentStatus());
            throw new IllegalStateException("订单状态处理器未注册：" + command.currentStatus());
        }

        return handler;
    }

    /**
     * 校验订单操作命令
     *
     * @param command 订单操作命令
     */
    private void validateCommand(OrderOperateCommand command) {
        if (command == null) {
            log.warn("订单状态操作失败，命令为空");
            throw new IllegalArgumentException("订单操作命令不能为空");
        }

        if (StrUtil.isBlank(command.orderNo())) {
            log.warn("订单状态操作失败，订单号为空");
            throw new IllegalArgumentException("订单号不能为空");
        }

        if (command.currentStatus() == null) {
            log.warn("订单状态操作失败，当前状态为空，订单号：{}", command.orderNo());
            throw new IllegalArgumentException("当前状态不能为空");
        }

        if (command.operatorId() == null || command.operatorId() <= 0) {
            log.warn("订单状态操作失败，操作人ID不合法，订单号：{}，操作人ID：{}",
                    command.orderNo(), command.operatorId());
            throw new IllegalArgumentException("操作人ID必须大于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/OrderStateController.java`

下面是订单状态操作接口，用于验证状态模式效果。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.context.OrderStateContext;
import io.github.atengk.design.dto.OrderOperateCommand;
import io.github.atengk.design.dto.OrderOperateResponse;
import io.github.atengk.design.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单状态控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/state/order")
public class OrderStateController {

    private final OrderStateContext orderStateContext;

    /**
     * 支付订单
     *
     * @param orderNo       订单号
     * @param currentStatus 当前状态
     * @param operatorId    操作人ID
     * @return 订单操作响应
     */
    @PostMapping("/pay")
    public OrderOperateResponse pay(@RequestParam String orderNo,
                                    @RequestParam OrderStatus currentStatus,
                                    @RequestParam Long operatorId) {
        OrderOperateCommand command = new OrderOperateCommand(orderNo, currentStatus, operatorId);
        return orderStateContext.pay(command);
    }

    /**
     * 发货订单
     *
     * @param orderNo       订单号
     * @param currentStatus 当前状态
     * @param operatorId    操作人ID
     * @return 订单操作响应
     */
    @PostMapping("/ship")
    public OrderOperateResponse ship(@RequestParam String orderNo,
                                     @RequestParam OrderStatus currentStatus,
                                     @RequestParam Long operatorId) {
        OrderOperateCommand command = new OrderOperateCommand(orderNo, currentStatus, operatorId);
        return orderStateContext.ship(command);
    }

    /**
     * 完成订单
     *
     * @param orderNo       订单号
     * @param currentStatus 当前状态
     * @param operatorId    操作人ID
     * @return 订单操作响应
     */
    @PostMapping("/finish")
    public OrderOperateResponse finish(@RequestParam String orderNo,
                                       @RequestParam OrderStatus currentStatus,
                                       @RequestParam Long operatorId) {
        OrderOperateCommand command = new OrderOperateCommand(orderNo, currentStatus, operatorId);
        return orderStateContext.finish(command);
    }

    /**
     * 取消订单
     *
     * @param orderNo       订单号
     * @param currentStatus 当前状态
     * @param operatorId    操作人ID
     * @return 订单操作响应
     */
    @PostMapping("/cancel")
    public OrderOperateResponse cancel(@RequestParam String orderNo,
                                       @RequestParam OrderStatus currentStatus,
                                       @RequestParam Long operatorId) {
        OrderOperateCommand command = new OrderOperateCommand(orderNo, currentStatus, operatorId);
        return orderStateContext.cancel(command);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/state/order/pay?orderNo=ORDER10001&currentStatus=CREATED&operatorId=10001"

curl -X POST "http://localhost:8080/state/order/ship?orderNo=ORDER10001&currentStatus=PAID&operatorId=10001"

curl -X POST "http://localhost:8080/state/order/finish?orderNo=ORDER10001&currentStatus=SHIPPED&operatorId=10001"
```

支付成功可能返回：

```json
{
  "orderNo": "ORDER10001",
  "beforeStatus": "CREATED",
  "afterStatus": "PAID",
  "message": "支付成功"
}
```

如果执行不允许的操作，例如已创建订单直接发货：

```bash
curl -X POST "http://localhost:8080/state/order/ship?orderNo=ORDER10001&currentStatus=CREATED&operatorId=10001"
```

会抛出异常：

```text
当前状态不支持发货操作：CREATED
```

实际项目中建议结合全局异常处理器，将业务异常转换成统一响应结构。

## 扩展一个新状态

在 Spring Boot 状态模式中，新增状态通常需要做两件事：新增状态枚举值，新增对应状态处理器。下面以退款状态为例，增加已退款状态 `REFUNDED`。

文件位置：`src/main/java/io/github/atengk/design/enums/OrderStatus.java`

在枚举中新增状态：

```java
REFUNDED
```

文件位置：`src/main/java/io/github/atengk/design/state/PaidOrderStateHandler.java`

如果希望已支付订单允许退款，可以在已支付状态处理器中新增退款动作。实际项目中建议将动作定义完整，例如新增 `refund` 方法。这里给出关键调整示例：

```java
public OrderOperateResponse refund(OrderOperateCommand command) {
    log.info("执行订单退款，订单号：{}，操作人ID：{}", command.orderNo(), command.operatorId());
    return response(command, OrderStatus.REFUNDED, "退款成功");
}
```

文件位置：`src/main/java/io/github/atengk/design/state/RefundedOrderStateHandler.java`

下面是已退款状态处理器。已退款订单一般不允许继续支付、发货、完成或取消。

```java
package io.github.atengk.design.state;

import io.github.atengk.design.enums.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * 已退款订单状态处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Component
public class RefundedOrderStateHandler extends AbstractOrderStateHandler {

    /**
     * 获取支持的订单状态
     *
     * @return 订单状态
     */
    @Override
    public OrderStatus supportStatus() {
        return OrderStatus.REFUNDED;
    }
}
```

如果项目中状态动作较多，例如支付、取消、发货、确认收货、退款、关闭、售后完成，建议提前设计统一动作模型，避免状态接口方法无限膨胀。

一种改进方向是把动作也建模为枚举：

```text
PAY
CANCEL
SHIP
FINISH
REFUND
CLOSE
```

然后状态处理器统一实现：

```java
OrderOperateResponse handle(OrderOperateCommand command, OrderAction action);
```

这种方式适合动作数量持续增长的业务状态机。

## 状态模式和策略模式的区别

状态模式和策略模式结构上都可能表现为“接口 + 多个实现类 + 上下文分发”，但二者意图不同。

| 对比项           | 状态模式                     | 策略模式                     |
| ---------------- | ---------------------------- | ---------------------------- |
| 核心目的         | 对象在不同状态下行为不同     | 从多个算法中选择一种执行     |
| 是否关心状态流转 | 关心                         | 通常不关心                   |
| 行为依赖         | 依赖对象当前状态             | 依赖业务类型或策略类型       |
| 执行结果         | 常常改变对象状态             | 通常只返回算法结果           |
| 典型场景         | 订单状态、审批状态、工单状态 | 优惠计算、支付渠道、物流计费 |

简单理解：

```text
状态模式：对象当前是什么状态，决定它能做什么，以及做完变成什么状态。
策略模式：我要做一件事，但具体用哪种算法处理。
```

订单从已创建到已支付，再到已发货和已完成，适合状态模式。订单优惠从满减、折扣、新人优惠中选择一种，适合策略模式。

## 状态模式和责任链模式的区别

状态模式和责任链模式都能减少复杂判断，但适用问题不同。

| 对比项       | 状态模式                         | 责任链模式                 |
| ------------ | -------------------------------- | -------------------------- |
| 核心目的     | 封装不同状态下的行为             | 多个处理器按顺序处理请求   |
| 处理数量     | 当前状态通常只匹配一个状态处理器 | 一个请求通常经过多个处理器 |
| 是否强调顺序 | 状态流转有顺序                   | 处理器执行有顺序           |
| 中断方式     | 当前状态不允许则拒绝操作         | 任意处理器可中断链路       |
| 典型场景     | 订单状态流转、审批状态流转       | 参数校验链、风控链、过滤链 |

简单理解：

```text
状态模式：当前状态决定行为。
责任链模式：请求要过多个关卡。
```

已创建订单能支付不能发货，是状态模式问题。订单提交前依次校验参数、用户、库存、金额，是责任链模式问题。

## 状态模式和枚举状态机

简单状态流转可以使用枚举完成，不一定要引入状态模式。例如状态数量少、动作少、规则稳定时，可以直接在枚举中定义允许流转关系。

示例：

```java
public enum SimpleOrderStatus {

    CREATED,
    PAID,
    CANCELED;

    public boolean canPay() {
        return this == CREATED;
    }
}
```

但当状态行为变多时，枚举会迅速膨胀。

不推荐在枚举中塞入大量复杂业务：

```java
public enum OrderStatus {

    CREATED {
        public void pay() {
            // 校验支付金额
            // 调用支付服务
            // 修改订单状态
            // 发消息
            // 写日志
        }
    }
}
```

如果状态下的行为涉及数据库、Redis、MQ、第三方服务、事务控制等复杂逻辑，建议使用 Spring Bean 状态模式，让每个状态处理器成为独立 Bean。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行已创建订单支付：

```bash
curl -X POST "http://localhost:8080/state/order/pay?orderNo=ORDER10001&currentStatus=CREATED&operatorId=10001"
```

执行已支付订单发货：

```bash
curl -X POST "http://localhost:8080/state/order/ship?orderNo=ORDER10001&currentStatus=PAID&operatorId=10001"
```

执行已发货订单完成：

```bash
curl -X POST "http://localhost:8080/state/order/finish?orderNo=ORDER10001&currentStatus=SHIPPED&operatorId=10001"
```

如果状态模式正常，可以看到类似日志：

```text
初始化订单状态上下文，支持状态：[CREATED, PAID, SHIPPED, FINISHED, CANCELED]
执行订单支付，订单号：ORDER10001，操作人ID：10001
订单状态流转成功，订单号：ORDER10001，操作前状态：CREATED，操作后状态：PAID，结果：支付成功
执行订单发货，订单号：ORDER10001，操作人ID：10001
订单状态流转成功，订单号：ORDER10001，操作前状态：PAID，操作后状态：SHIPPED，结果：发货成功
执行订单完成，订单号：ORDER10001，操作人ID：10001
订单状态流转成功，订单号：ORDER10001，操作前状态：SHIPPED，操作后状态：FINISHED，结果：完成成功
```

执行不允许的状态操作：

```bash
curl -X POST "http://localhost:8080/state/order/cancel?orderNo=ORDER10001&currentStatus=FINISHED&operatorId=10001"
```

异常日志示例：

```text
订单状态操作失败，当前状态不支持该操作，订单号：ORDER10001，当前状态：FINISHED，操作：取消
```

实际项目中，接口不应该完全信任前端传入的 `currentStatus`。应根据 `orderNo` 从数据库查询当前真实状态，再执行状态处理器。

## 注意事项

状态模式适合状态行为复杂的对象，但不要在状态很少、逻辑很简单时过度设计。只有两个状态、两个操作时，简单判断可能更直接。

适合使用状态模式的场景：

```text
状态数量较多
不同状态下行为差异明显
状态流转规则复杂
状态流转需要扩展
状态操作涉及较多业务逻辑
希望消除大量 if else 状态判断
```

不太适合使用状态模式的场景：

```text
状态很少
状态行为差异很小
状态只用于展示
没有复杂流转规则
只是简单字段枚举
```

不推荐在业务服务中堆积大量状态判断：

```java
if (status == OrderStatus.CREATED) {
    // 支付、取消
} else if (status == OrderStatus.PAID) {
    // 发货
} else if (status == OrderStatus.SHIPPED) {
    // 完成
} else if (status == OrderStatus.FINISHED) {
    // 拒绝操作
}
```

推荐使用状态上下文分发：

```java
OrderOperateResponse response = orderStateContext.pay(command);
```

状态处理器中不要保存请求级状态。Spring Bean 默认是单例，成员变量会被多个请求共享。

错误示例：

```java
private String currentOrderNo;
private Long currentOperatorId;
private OrderStatus currentStatus;
```

推荐使用方法参数和局部变量：

```java
public OrderOperateResponse pay(OrderOperateCommand command) {
    return response(command, OrderStatus.PAID, "支付成功");
}
```

实际项目中，状态流转通常需要保证并发安全。比如两个请求同时支付同一个订单，不能只依赖本地状态模式判断。

推荐结合数据库乐观锁或条件更新：

```sql
-- 只有当前状态为 CREATED 时才允许更新为 PAID
UPDATE t_order
SET status = 'PAID',
    update_time = NOW()
WHERE order_no = #{orderNo}
  AND status = 'CREATED';
```

如果更新行数为 0，说明状态已被其他请求修改，应返回状态冲突。

生产环境中，订单状态流转还需要考虑：

```text
数据库事务
并发状态更新
状态流转日志
幂等控制
消息可靠投递
异常补偿
操作权限
状态回滚策略
```

状态模式只负责组织状态行为和状态流转规则，不自动保证分布式一致性。

## 总结

在 JDK21 和 Spring Boot 3 项目中，状态模式的实践重点是把不同状态下的行为拆分到独立状态处理器中，让当前状态决定对象能做什么，以及操作后流转到什么状态。

普通 Java 状态模式适合理解原理和本地状态流转。Spring Boot 项目中更推荐使用“状态枚举 + 状态处理器接口 + 多个状态处理器 Bean + 状态上下文分发”的结构。对于订单、审批、工单、支付单、优惠券、任务处理等状态流转复杂的场景，状态模式可以显著减少状态判断分支，让状态行为更清晰、更容易扩展。

状态模式不是为了替代所有枚举判断，而是为了处理“状态行为复杂、状态流转明确、不同状态下操作规则差异明显”的业务对象。实际落地时，需要结合数据库条件更新、事务、幂等和状态变更日志，才能保证状态流转的业务可靠性。
