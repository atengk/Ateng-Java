# ⚙️ 状态模式（State Pattern）

## 一、模式简介

**状态模式（State Pattern）** 是一种行为型设计模式。
 它允许一个对象在其内部状态改变时改变它的行为，看起来就像是对象的类发生了改变一样。

**核心思想**：将状态与行为解耦，不同状态封装成独立类，避免冗长的 `if/else` 或 `switch` 语句。

在 Spring Boot 项目中，状态模式经常用于以下业务场景：

- 订单状态流转（待支付 → 已支付 → 已发货 → 已完成）
- 审批流（待提交 → 审批中 → 审批通过 / 拒绝）
- 流程节点状态控制
- 状态机实现

------

## 二、示例目标

我们构建一个**订单状态流转系统**，包含四种状态：

1. **待支付（PendingPayment）**
2. **已支付（Paid）**
3. **已发货（Shipped）**
4. **已完成（Completed）**

不同状态下，订单允许的操作不同。

------

## 三、包结构设计

```
io.github.atengk
└── designpattern
    └── state
        ├── StateApplication.java                   # 启动类
        ├── context
        │   └── OrderContext.java                   # 上下文类（持有当前状态）
        ├── state
        │   ├── OrderState.java                     # 抽象状态接口
        │   ├── PendingPaymentState.java            # 待支付状态
        │   ├── PaidState.java                      # 已支付状态
        │   ├── ShippedState.java                   # 已发货状态
        │   └── CompletedState.java                 # 已完成状态
        ├── service
        │   └── OrderService.java                   # 状态触发服务
        └── controller
            └── StateController.java                # 测试控制器
```

------

## 四、代码实现

### 1. 抽象状态接口：`OrderState.java`

```java
package io.github.atengk.designpattern.state.state;

/**
 * 订单状态接口
 * 定义状态的行为规范
 */
public interface OrderState {

    /**
     * 支付操作
     */
    void pay();

    /**
     * 发货操作
     */
    void ship();

    /**
     * 完成订单操作
     */
    void complete();
}
```

------

### 2. 状态上下文类：`OrderContext.java`

```java
package io.github.atengk.designpattern.state.context;

import io.github.atengk.designpattern.state.state.OrderState;
import org.springframework.stereotype.Component;

/**
 * 订单上下文类
 * 保存当前状态，并将操作委托给具体状态实现
 */
@Component
public class OrderContext {

    private OrderState currentState;

    /**
     * 设置当前状态
     *
     * @param state 状态实例
     */
    public void setState(OrderState state) {
        this.currentState = state;
    }

    /**
     * 支付操作
     */
    public void pay() {
        currentState.pay();
    }

    /**
     * 发货操作
     */
    public void ship() {
        currentState.ship();
    }

    /**
     * 完成订单操作
     */
    public void complete() {
        currentState.complete();
    }
}
```

------

### 3. 具体状态类实现

#### (1) 待支付状态：`PendingPaymentState.java`

```java
package io.github.atengk.state;

import cn.hutool.extra.spring.SpringUtil;
import io.github.atengk.context.OrderContext;
import org.springframework.stereotype.Component;

/**
 * 待支付状态
 */
@Component
public class PendingPaymentState implements OrderState {

    private final OrderContext context;

    public PendingPaymentState(OrderContext context) {
        this.context = context;
    }

    @Override
    public void pay() {
        System.out.println("订单已支付，状态切换为：已支付");
        context.setState(SpringUtil.getBean(PaidState.class));
    }

    @Override
    public void ship() {
        System.out.println("不能发货，订单尚未支付");
    }

    @Override
    public void complete() {
        System.out.println("不能完成订单，订单尚未支付");
    }
}

```

#### (2) 已支付状态：`PaidState.java`

```java
package io.github.atengk.state;

import cn.hutool.extra.spring.SpringUtil;
import io.github.atengk.context.OrderContext;
import org.springframework.stereotype.Component;

/**
 * 已支付状态
 */
@Component
public class PaidState implements OrderState {

    private final OrderContext context;

    public PaidState(OrderContext context) {
        this.context = context;
    }

    @Override
    public void pay() {
        System.out.println("订单已支付，请勿重复支付");
    }

    @Override
    public void ship() {
        System.out.println("订单发货成功，状态切换为：已发货");
        context.setState(SpringUtil.getBean(ShippedState.class));
    }

    @Override
    public void complete() {
        System.out.println("不能直接完成，需先发货");
    }
}

```

#### (3) 已发货状态：`ShippedState.java`

```java
package io.github.atengk.state;


import cn.hutool.extra.spring.SpringUtil;
import io.github.atengk.context.OrderContext;
import org.springframework.stereotype.Component;

/**
 * 已发货状态
 */
@Component
public class ShippedState implements OrderState {

    private final OrderContext context;

    public ShippedState(OrderContext context) {
        this.context = context;
    }

    @Override
    public void pay() {
        System.out.println("订单已发货，不能再支付");
    }

    @Override
    public void ship() {
        System.out.println("订单已发货，请勿重复操作");
    }

    @Override
    public void complete() {
        System.out.println("订单已完成，状态切换为：已完成");
        context.setState(SpringUtil.getBean(CompletedState.class));
    }
}
```

#### (4) 已完成状态：`CompletedState.java`

```java
package io.github.atengk.designpattern.state.state;

import io.github.atengk.designpattern.state.context.OrderContext;
import org.springframework.stereotype.Component;

/**
 * 已完成状态
 */
@Component
public class CompletedState implements OrderState {

    public CompletedState(OrderContext context) {}

    @Override
    public void pay() {
        System.out.println("订单已完成，无法支付");
    }

    @Override
    public void ship() {
        System.out.println("订单已完成，无法发货");
    }

    @Override
    public void complete() {
        System.out.println("订单已完成，请勿重复操作");
    }
}
```

------

### 4. 服务层：`OrderService.java`

```java
package io.github.atengk.designpattern.state.service;

import io.github.atengk.designpattern.state.context.OrderContext;
import io.github.atengk.designpattern.state.state.PendingPaymentState;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * 订单状态服务类
 * 用于初始化和状态流转操作
 */
@Service
public class OrderService {

    private final OrderContext orderContext;
    private final ApplicationContext applicationContext;

    public OrderService(OrderContext orderContext, ApplicationContext applicationContext) {
        this.orderContext = orderContext;
        this.applicationContext = applicationContext;
    }

    /**
     * 初始化订单状态为“待支付”
     */
    public void initOrder() {
        orderContext.setState(applicationContext.getBean(PendingPaymentState.class));
        System.out.println("订单初始化完成，当前状态：待支付");
    }

    /**
     * 执行支付操作
     */
    public void payOrder() {
        orderContext.pay();
    }

    /**
     * 执行发货操作
     */
    public void shipOrder() {
        orderContext.ship();
    }

    /**
     * 执行完成操作
     */
    public void completeOrder() {
        orderContext.complete();
    }
}
```

------

### 5. 控制器：`StateController.java`

```java
package io.github.atengk.designpattern.state.controller;

import io.github.atengk.designpattern.state.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 状态模式测试控制器
 */
@RestController
public class StateController {

    private final OrderService orderService;

    public StateController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/state/test")
    public String testStatePattern() {
        orderService.initOrder();
        orderService.payOrder();
        orderService.shipOrder();
        orderService.completeOrder();
        return "订单状态流转测试完成，请查看控制台输出";
    }
}
```

------

### 6. 启动类：`StateApplication.java`

```java
package io.github.atengk.designpattern.state;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 状态模式示例启动类
 */
@SpringBootApplication
public class StateApplication {

    public static void main(String[] args) {
        SpringApplication.run(StateApplication.class, args);
    }
}
```

------

## 五、运行结果示例

访问：

```
GET http://localhost:8080/state/test
```

控制台输出：

```
订单初始化完成，当前状态：待支付
订单已支付，状态切换为：已支付
订单发货成功，状态切换为：已发货
订单已完成，状态切换为：已完成
```

------

## 六、总结表格

| 项目         | 内容                                                 |
| ------------ | ---------------------------------------------------- |
| **模式名称** | 状态模式（State Pattern）                            |
| **核心思想** | 将不同状态的行为封装到独立类中，状态变化即行为变化   |
| **优点**     | 避免冗长的 `if/else`，行为变化封装更清晰，可扩展性强 |
| **缺点**     | 类数量较多，状态切换逻辑需要精心管理                 |
| **应用场景** | 订单流转、审批流、任务状态机、流程控制               |

