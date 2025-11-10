# 🧩 工厂方法模式（Factory Method Pattern）

---

## 🌟 一、模式简介

**工厂方法模式（Factory Method Pattern）** 是一种**创建型设计模式**，它定义了**一个创建对象的接口**，让子类决定实例化哪一个类。
与简单工厂不同，工厂方法模式**遵循开闭原则**，新增产品时无需修改已有工厂代码，只需新增具体工厂即可。

在 **Spring Boot** 项目中，工厂方法模式常用于：

* 根据不同配置、策略创建不同业务对象
* 解耦对象的创建与使用
* 多厂商 SDK 或消息服务切换

---

## 🧠 二、场景举例（实战导向）

假设我们有一个「消息发送系统」，既可以发送**邮件**，也可以发送**短信**。
每种消息都有不同的实现逻辑，但客户端只关心“发送消息”接口，而不关心具体类型。
工厂方法模式可以将消息创建逻辑交给各自的工厂类实现，客户端无需修改。

---

## 🏗️ 三、项目结构

```
io.github.atengk
 ├── controller/
 │    └── MessageController.java
 ├── service/
 │    ├── product/
 │    │     ├── Message.java
 │    │     ├── EmailMessage.java
 │    │     └── SmsMessage.java
 │    └── factory/
 │          ├── MessageFactory.java
 │          ├── EmailMessageFactory.java
 │          └── SmsMessageFactory.java
 └── DesignPatternApplication.java
```

---

## 💡 四、代码实现（Spring Boot 实战版）

---

### 1️⃣ 抽象产品：`Message`

```java
package io.github.atengk.service.product;

/**
 * 消息接口
 */
public interface Message {

    /**
     * 发送消息
     */
    void send();
}
```

---

### 2️⃣ 具体产品：`EmailMessage`

```java
package io.github.atengk.service.product;

import org.springframework.stereotype.Component;

/**
 * 邮件消息实现
 */
@Component
public class EmailMessage implements Message {

    @Override
    public void send() {
        System.out.println("【邮件消息】发送成功！");
    }
}
```

---

### 3️⃣ 具体产品：`SmsMessage`

```java
package io.github.atengk.service.product;

import org.springframework.stereotype.Component;

/**
 * 短信消息实现
 */
@Component
public class SmsMessage implements Message {

    @Override
    public void send() {
        System.out.println("【短信消息】发送成功！");
    }
}
```

---

### 4️⃣ 抽象工厂：`MessageFactory`

```java
package io.github.atengk.service.factory;

import io.github.atengk.service.product.Message;

/**
 * 消息工厂接口
 */
public interface MessageFactory {

    /**
     * 创建消息对象
     *
     * @return Message 实例
     */
    Message createMessage();
}
```

---

### 5️⃣ 具体工厂：`EmailMessageFactory`

```java
package io.github.atengk.service.factory;

import io.github.atengk.service.product.EmailMessage;
import io.github.atengk.service.product.Message;
import org.springframework.stereotype.Service;

/**
 * 邮件消息工厂
 */
@Service
public class EmailMessageFactory implements MessageFactory {

    @Override
    public Message createMessage() {
        return new EmailMessage();
    }
}
```

---

### 6️⃣ 具体工厂：`SmsMessageFactory`

```java
package io.github.atengk.service.factory;

import io.github.atengk.service.product.Message;
import io.github.atengk.service.product.SmsMessage;
import org.springframework.stereotype.Service;

/**
 * 短信消息工厂
 */
@Service
public class SmsMessageFactory implements MessageFactory {

    @Override
    public Message createMessage() {
        return new SmsMessage();
    }
}
```

---

### 7️⃣ 控制层：`MessageController`

```java
package io.github.atengk.controller;

import io.github.atengk.service.factory.EmailMessageFactory;
import io.github.atengk.service.factory.MessageFactory;
import io.github.atengk.service.factory.SmsMessageFactory;
import io.github.atengk.service.product.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息控制器，演示工厂方法模式
 */
@RestController
public class MessageController {

    private final EmailMessageFactory emailFactory;
    private final SmsMessageFactory smsFactory;

    public MessageController(EmailMessageFactory emailFactory, SmsMessageFactory smsFactory) {
        this.emailFactory = emailFactory;
        this.smsFactory = smsFactory;
    }

    /**
     * 根据 type 参数发送不同类型消息
     *
     * @param type 消息类型（email / sms）
     * @return 发送结果
     */
    @GetMapping("/send")
    public String sendMessage(@RequestParam(defaultValue = "email") String type) {
        MessageFactory factory;
        if ("sms".equalsIgnoreCase(type)) {
            factory = smsFactory;
        } else {
            factory = emailFactory;
        }
        Message message = factory.createMessage();
        message.send();
        return "消息发送完成：" + message.getClass().getSimpleName();
    }
}
```

---

## 🧩 五、运行效果

请求：

```
http://localhost:8080/send?type=email
```

控制台输出：

```
【邮件消息】发送成功！
```

请求：

```
http://localhost:8080/send?type=sms
```

控制台输出：

```
【短信消息】发送成功！
```

返回结果 JSON（Spring Boot 默认 `String` 输出）：

```
消息发送完成：SmsMessage
```

---

## 📘 六、总结与要点

| 特性              | 说明                                                                                                                     |
| --------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **模式类型**        | 创建型（Creational Pattern）                                                                                                |
| **核心角色**        | 抽象产品（Message） + 具体产品（EmailMessage / SmsMessage） + 抽象工厂（MessageFactory） + 具体工厂（EmailMessageFactory / SmsMessageFactory） |
| **Spring 实战应用** | 依赖注入管理具体工厂 Bean，通过工厂创建对象，解耦对象创建与使用                                                                                     |
| **适用场景**        | 客户端无需关心对象具体类型时，需要封装对象创建逻辑                                                                                              |

