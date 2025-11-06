# 🧩 观察者模式（Observer Pattern）

------

## 🌟 一、模式简介

**观察者模式（Observer Pattern）** 是一种行为型设计模式。
 它定义了对象之间的一对多依赖关系，当一个对象状态发生改变时，
 所有依赖它的对象都会自动收到通知并更新。

在 Spring Boot 中，**事件发布机制（ApplicationEventPublisher + @EventListener）**
 就是典型的观察者模式应用。

------

## 💡 二、场景举例（实战导向）

假设我们有一个系统，用户注册成功后需要：

- 发送欢迎邮件
- 记录注册日志
- 触发数据统计

如果直接在注册方法里依次调用，会让代码高度耦合；
 而使用观察者模式，我们可以让“注册”与“通知逻辑”解耦，
 让这些动作自动触发且互不影响。

------

## 🏗️ 三、项目结构

```
io.github.atengk
 ├── controller/
 │    └── UserController.java
 ├── event/
 │    ├── UserRegisterEvent.java
 │    ├── listener/
 │    │    ├── EmailNotificationListener.java
 │    │    ├── LogRecordListener.java
 │    │    └── StatisticUpdateListener.java
 ├── service/
 │    └── UserService.java
 └── DesignPatternApplication.java
```

------

## 💻 四、代码实现（Spring Boot 实战版）

------

### 1️⃣ 定义事件类：`UserRegisterEvent`

```java
package io.github.atengk.event;

import org.springframework.context.ApplicationEvent;

/**
 * 用户注册事件（观察者模式中的“主题”）
 */
public class UserRegisterEvent extends ApplicationEvent {

    private final String username;

    public UserRegisterEvent(Object source, String username) {
        super(source);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
```

------

### 2️⃣ 用户服务：`UserService`

```java
package io.github.atengk.service;

import io.github.atengk.event.UserRegisterEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 用户业务服务
 * 负责用户注册并发布注册事件
 */
@Service
public class UserService {

    private final ApplicationEventPublisher eventPublisher;

    public UserService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 用户注册逻辑
     *
     * @param username 用户名
     */
    public void registerUser(String username) {
        System.out.println("【用户注册】用户 " + username + " 注册成功");

        // 发布注册事件
        eventPublisher.publishEvent(new UserRegisterEvent(this, username));
    }
}
```

------

### 3️⃣ 监听器1：发送邮件通知

```java
package io.github.atengk.event.listener;

import io.github.atengk.event.UserRegisterEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 邮件通知监听器
 * 当用户注册成功后自动发送欢迎邮件
 */
@Component
public class EmailNotificationListener {

    @EventListener
    public void handleUserRegister(UserRegisterEvent event) {
        System.out.println("【邮件通知】已向 " + event.getUsername() + " 发送欢迎邮件");
    }
}
```

------

### 4️⃣ 监听器2：记录日志

```java
package io.github.atengk.event.listener;

import io.github.atengk.event.UserRegisterEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 日志记录监听器
 * 用户注册后记录系统日志
 */
@Component
public class LogRecordListener {

    @EventListener
    public void handleUserRegister(UserRegisterEvent event) {
        System.out.println("【系统日志】记录用户注册事件：" + event.getUsername());
    }
}
```

------

### 5️⃣ 监听器3：更新统计信息

```java
package io.github.atengk.event.listener;

import io.github.atengk.event.UserRegisterEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 数据统计监听器
 * 用户注册后更新注册统计数据
 */
@Component
public class StatisticUpdateListener {

    @EventListener
    public void handleUserRegister(UserRegisterEvent event) {
        System.out.println("【数据统计】已更新用户注册数量：" + event.getUsername());
    }
}
```

------

### 6️⃣ 控制层：`UserController`

```java
package io.github.atengk.controller;

import io.github.atengk.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制层，用于触发注册流程
 */
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String register(String username) {
        userService.registerUser(username);
        return "用户注册完成";
    }
}
```

------

## 🧩 五、运行效果

请求：

```
http://localhost:8080/register?username=atengk
```

控制台输出：

```
【用户注册】用户 atengk 注册成功
【邮件通知】已向 atengk 发送欢迎邮件
【系统日志】记录用户注册事件：atengk
【数据统计】已更新用户注册数量：atengk
```

------

## 📘 六、总结与要点

| 特性            | 说明                                            |
| --------------- | ----------------------------------------------- |
| **模式类型**    | 行为型（Behavioral Pattern）                    |
| **核心角色**    | Subject（主题）+ Observer（观察者）             |
| **Spring 实战** | `ApplicationEventPublisher` 与 `@EventListener` |
| **优势**        | 降低耦合度，让“事件源”与“响应者”解耦            |
| **适用场景**    | 系统通知、日志、异步消息、模块解耦等            |

------

## 🪄 七、扩展应用

| 场景             | 实现方式                | 应用实例           |
| ---------------- | ----------------------- | ------------------ |
| **系统事件通知** | Spring ApplicationEvent | 注册、下单、支付等 |
| **消息分发**     | Guava EventBus / MQ     | 多模块异步通信     |
| **分布式事件**   | RocketMQ / Kafka        | 跨系统通知机制     |

