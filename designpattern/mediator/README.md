# 🧩 中介者模式（Mediator Pattern）

---

## 🌟 一、模式简介

**中介者模式（Mediator Pattern）** 是一种**行为型设计模式**，它通过一个中介者对象来**封装一系列对象之间的交互**，使各对象不再直接引用彼此，从而降低耦合性。

在 **Spring Boot** 项目中，中介者模式常用于：

* 多个服务/组件间复杂交互协调
* 消息分发与事件处理
* 避免对象之间形成紧密耦合网络

---

## 🧠 二、场景举例（实战导向）

假设我们有一个「聊天系统」，用户 A、B、C 想要互相发送消息。
如果每个用户都直接引用其他用户，会形成复杂依赖。
通过中介者模式，所有消息通过一个 **ChatRoom 中介者** 转发，实现解耦和集中管理。

---

## 🏗️ 三、项目结构

```
io.github.atengk
 ├── controller/
 │    └── ChatController.java
 ├── service/
 │    ├── mediator/
 │    │     ├── ChatRoom.java
 │    │     └── ChatUser.java
 └── DesignPatternApplication.java
```

---

## 💡 四、代码实现（Spring Boot 实战版）

---

### 1️⃣ 中介者接口：`ChatRoom`

```java
package io.github.atengk.service.mediator;

/**
 * 聊天中介者接口
 */
public interface ChatRoom {

    /**
     * 注册用户
     *
     * @param user 用户实例
     */
    void registerUser(ChatUser user);

    /**
     * 发送消息给指定用户
     *
     * @param from    发送者用户名
     * @param to      接收者用户名
     * @param message 消息内容
     */
    void sendMessage(String from, String to, String message);
}
```

---

### 2️⃣ 抽象同事类：`ChatUser`

```java
package io.github.atengk.service.mediator;

/**
 * 聊天用户（同事类）
 */
public abstract class ChatUser {

    protected final String name;
    protected final ChatRoom chatRoom;

    /**
     * 构造方法
     *
     * @param name     用户名
     * @param chatRoom 中介者实例
     */
    protected ChatUser(String name, ChatRoom chatRoom) {
        this.name = name;
        this.chatRoom = chatRoom;
        chatRoom.registerUser(this);
    }

    /**
     * 发送消息
     *
     * @param to      接收者用户名
     * @param message 消息内容
     */
    public abstract void send(String to, String message);

    /**
     * 接收消息
     *
     * @param from    发送者用户名
     * @param message 消息内容
     */
    public abstract void receive(String from, String message);

    public String getName() {
        return name;
    }
}
```

---

### 3️⃣ 具体中介者：`ChatRoomImpl`

```java
package io.github.atengk.service.mediator;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 聊天中介者实现
 */
@Component
public class ChatRoomImpl implements ChatRoom {

    private final Map<String, ChatUser> users = new HashMap<>();

    @Override
    public void registerUser(ChatUser user) {
        users.put(user.getName(), user);
    }

    @Override
    public void sendMessage(String from, String to, String message) {
        ChatUser user = users.get(to);
        if (user != null) {
            user.receive(from, message);
        } else {
            System.out.println("用户 " + to + " 不存在！");
        }
    }
}
```

---

### 4️⃣ 具体同事类：`User`

```java
package io.github.atengk.service.mediator;

import org.springframework.stereotype.Component;

/**
 * 聊天用户实现
 */
@Component
public class User extends ChatUser {

    public User(String name, ChatRoom chatRoom) {
        super(name, chatRoom);
    }

    @Override
    public void send(String to, String message) {
        System.out.println("【" + name + " 发送】-> " + to + ": " + message);
        chatRoom.sendMessage(name, to, message);
    }

    @Override
    public void receive(String from, String message) {
        System.out.println("【" + name + " 接收】<- " + from + ": " + message);
    }
}
```

> 💡 注意：在实际 Spring Boot 项目中，你可以通过工厂方法或 `@Bean` 创建多个 User 实例，并注入同一个 ChatRoom。

---

### 5️⃣ 控制层：`ChatController`

```java
package io.github.atengk.controller;

import io.github.atengk.service.mediator.ChatRoom;
import io.github.atengk.service.mediator.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 中介者模式控制器
 */
@RestController
public class ChatController {

    private final ChatRoom chatRoom;

    public ChatController(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    /**
     * 发送消息接口
     *
     * @param from    发送者
     * @param to      接收者
     * @param message 消息内容
     * @return 状态提示
     */
    @GetMapping("/chat/send")
    public String sendMessage(@RequestParam String from,
                              @RequestParam String to,
                              @RequestParam String message) {

        User sender = new User(from, chatRoom);
        User receiver = new User(to, chatRoom);
        sender.send(to, message);
        return "消息发送完成：" + from + " -> " + to;
    }
}
```

---

## 🧩 五、运行效果

请求：

```
http://localhost:8080/chat/send?from=Alice&to=Bob&message=Hello
```

控制台输出：

```
【Alice 发送】-> Bob: Hello
【Bob 接收】<- Alice: Hello
```

---

## 📘 六、总结与要点

| 特性              | 说明                                   |
| --------------- | ------------------------------------ |
| **模式类型**        | 行为型（Behavioral Pattern）              |
| **核心角色**        | 中介者（ChatRoom） + 同事类（ChatUser / User） |
| **Spring 实战应用** | 多组件消息或事件解耦，通过中介者集中管理交互逻辑             |
| **适用场景**        | 多对象交互复杂、需要降低耦合、便于扩展和维护               |

