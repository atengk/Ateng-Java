# 🧩 桥接模式（Bridge Pattern）

---

## 🌟 一、模式简介

**桥接模式（Bridge Pattern）** 是一种**结构型设计模式**，用于将**抽象部分与实现部分分离**，使它们可以独立变化。
通过桥接模式，抽象和实现可以在不同的维度上独立扩展，避免类的爆炸式继承。

在 **Spring Boot** 中，桥接模式常用于：

* 多维度业务组合，例如不同支付方式 + 不同渠道
* 将业务逻辑与具体实现解耦
* 支持动态切换实现细节而不影响抽象层

---

## 🧠 二、场景举例（实战导向）

假设我们有一个「消息发送系统」，

* 抽象维度：消息类型（普通消息、紧急消息）
* 实现维度：发送渠道（邮件、短信）

如果直接继承，每新增一种类型和渠道组合就会产生新的类，容易导致类爆炸。
桥接模式通过将消息类型（抽象）和发送渠道（实现）分离，解决了这个问题。

---

## 🏗️ 三、项目结构

```
io.github.atengk
 ├── controller/
 │    └── BridgeDemoController.java
 ├── service/
 │    ├── abstraction/
 │    │     ├── AbstractMessage.java
 │    │     └── UrgentMessage.java
 │    ├── implementor/
 │    │     ├── MessageSender.java
 │    │     ├── EmailSender.java
 │    │     └── SmsSender.java
 └── DesignPatternApplication.java
```

---

## 💡 四、代码实现（Spring Boot 实战版）

---

### 1️⃣ 实现接口：`MessageSender`

```java
package io.github.atengk.service.implementor;

/**
 * 消息发送实现接口（实现部分）
 */
public interface MessageSender {

    /**
     * 发送消息
     *
     * @param message 消息内容
     */
    void sendMessage(String message);
}
```

---

### 2️⃣ 具体实现：`EmailSender`

```java
package io.github.atengk.service.implementor;

import org.springframework.stereotype.Component;

/**
 * 邮件发送实现
 */
@Component
public class EmailSender implements MessageSender {

    @Override
    public void sendMessage(String message) {
        System.out.println("【邮件发送】内容：" + message);
    }
}
```

---

### 3️⃣ 具体实现：`SmsSender`

```java
package io.github.atengk.service.implementor;

import org.springframework.stereotype.Component;

/**
 * 短信发送实现
 */
@Component
public class SmsSender implements MessageSender {

    @Override
    public void sendMessage(String message) {
        System.out.println("【短信发送】内容：" + message);
    }
}
```

---

### 4️⃣ 抽象类：`AbstractMessage`

```java
package io.github.atengk.service.abstraction;

import io.github.atengk.service.implementor.MessageSender;

/**
 * 抽象消息类（抽象部分）
 */
public abstract class AbstractMessage {

    protected final MessageSender messageSender;

    /**
     * 构造方法注入实现者
     *
     * @param messageSender 消息发送实现
     */
    protected AbstractMessage(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    /**
     * 发送消息
     *
     * @param content 消息内容
     */
    public abstract void send(String content);
}
```

---

### 5️⃣ 具体抽象类：`UrgentMessage`

```java
package io.github.atengk.service.abstraction;

import io.github.atengk.service.implementor.MessageSender;

/**
 * 紧急消息（扩展抽象）
 */
public class UrgentMessage extends AbstractMessage {

    public UrgentMessage(MessageSender messageSender) {
        super(messageSender);
    }

    @Override
    public void send(String content) {
        System.out.println("【紧急消息】开始发送...");
        messageSender.sendMessage(content);
        System.out.println("【紧急消息】发送完成");
    }
}
```

> 💡 注意：Spring Boot 可通过构造方法注入不同 `MessageSender` 实现。

---

### 6️⃣ 控制层：`BridgeDemoController`

```java
package io.github.atengk.controller;

import io.github.atengk.service.abstraction.AbstractMessage;
import io.github.atengk.service.abstraction.UrgentMessage;
import io.github.atengk.service.implementor.EmailSender;
import io.github.atengk.service.implementor.MessageSender;
import io.github.atengk.service.implementor.SmsSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 桥接模式控制器演示
 */
@RestController
public class BridgeDemoController {

    private final EmailSender emailSender;
    private final SmsSender smsSender;

    public BridgeDemoController(EmailSender emailSender, SmsSender smsSender) {
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }

    /**
     * 根据 channel 参数发送紧急消息
     *
     * @param channel 渠道（email / sms）
     * @param content 消息内容
     * @return 发送结果
     */
    @GetMapping("/bridge/send")
    public String sendMessage(@RequestParam(defaultValue = "email") String channel,
                              @RequestParam(defaultValue = "测试内容") String content) {

        MessageSender sender;
        if ("sms".equalsIgnoreCase(channel)) {
            sender = smsSender;
        } else {
            sender = emailSender;
        }

        AbstractMessage message = new UrgentMessage(sender);
        message.send(content);
        return "消息发送完成：" + channel;
    }
}
```

---

## 🧩 五、运行效果

请求：

```
http://localhost:8080/bridge/send?channel=email&content=紧急通知
```

控制台输出：

```
【紧急消息】开始发送...
【邮件发送】内容：紧急通知
【紧急消息】发送完成
```

请求：

```
http://localhost:8080/bridge/send?channel=sms&content=紧急通知
```

控制台输出：

```
【紧急消息】开始发送...
【短信发送】内容：紧急通知
【紧急消息】发送完成
```

---

## 📘 六、总结与要点

| 特性              | 说明                                                                                               |
| --------------- | ------------------------------------------------------------------------------------------------ |
| **模式类型**        | 结构型（Structural Pattern）                                                                          |
| **核心角色**        | 抽象类（AbstractMessage） + 扩展抽象（UrgentMessage） + 实现接口（MessageSender） + 具体实现（EmailSender / SmsSender） |
| **Spring 实战应用** | 构造方法注入不同实现 Bean，实现消息抽象与发送渠道解耦                                                                    |
| **适用场景**        | 多维度业务组合，需要抽象与实现独立扩展时                                                                             |

