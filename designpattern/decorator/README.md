# 🎨 装饰器模式（Decorator Pattern）

## 🧭 一、模式简介

**装饰器模式（Decorator Pattern）** 是一种结构型设计模式。
 它允许我们在**不修改原有类代码**的前提下，**动态地给对象添加额外行为或功能**。

> ✅ 核心思想：
>  用“包装类”去**扩展对象功能**，而不是继承类。

------

## 💡 二、现实类比

你去点一杯咖啡 ☕，咖啡本身只是基础饮品（基础类）。
 加牛奶、加糖、加冰等属于附加功能（装饰器）。
 每个装饰器都围绕“咖啡”工作，却能层层叠加、自由组合。

------

## 💼 三、项目场景：消息通知增强系统

假设系统中存在一个「消息发送服务」，用于发送通知。
 现在我们希望：

- 在发送消息前后记录日志；
- 增加重试机制；
- 甚至未来加上性能监控、限流等。

但是我们**不希望修改原始的 MessageSender 实现类**。
 装饰器模式正好可以解决。

------

## 🧱 四、项目结构设计

```
io.github.atengk.designpattern.decorator
├── component
│   ├── MessageSender.java              # 抽象组件接口
│   └── DefaultMessageSender.java       # 具体实现类
├── decorator
│   ├── AbstractMessageSenderDecorator.java  # 抽象装饰器
│   ├── LogMessageDecorator.java        # 日志增强装饰器
│   └── RetryMessageDecorator.java      # 重试增强装饰器
└── controller
    └── MessageSendController.java      # 控制器演示
```

------

## ⚙️ 五、核心代码实现

### 1️⃣ 抽象组件接口

```java
package io.github.atengk.designpattern.decorator.component;

/**
 * 消息发送接口
 * 定义核心行为
 */
public interface MessageSender {

    /**
     * 发送消息
     *
     * @param message 消息内容
     */
    void send(String message);
}
```

------

### 2️⃣ 基础实现类

```java
package io.github.atengk.designpattern.decorator.component;

import org.springframework.stereotype.Service;

/**
 * 默认的消息发送实现
 */
@Service("defaultMessageSender")
public class DefaultMessageSender implements MessageSender {

    @Override
    public void send(String message) {
        System.out.println("【发送消息】" + message);
    }
}
```

------

### 3️⃣ 抽象装饰器类（核心）

```java
package io.github.atengk.designpattern.decorator.decorator;

import io.github.atengk.designpattern.decorator.component.MessageSender;

/**
 * 抽象装饰器
 * 持有一个 MessageSender 实例，并实现相同接口
 */
public abstract class AbstractMessageSenderDecorator implements MessageSender {

    protected final MessageSender delegate;

    protected AbstractMessageSenderDecorator(MessageSender delegate) {
        this.delegate = delegate;
    }

    @Override
    public void send(String message) {
        delegate.send(message);
    }
}
```

------

### 4️⃣ 日志增强装饰器

```java
package io.github.atengk.designpattern.decorator.decorator;

import io.github.atengk.designpattern.decorator.component.MessageSender;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 日志增强装饰器
 * 在消息发送前后打印日志
 */
@Component
public class LogMessageDecorator extends AbstractMessageSenderDecorator {

    public LogMessageDecorator(@Qualifier("defaultMessageSender") MessageSender delegate) {
        super(delegate);
    }

    @Override
    public void send(String message) {
        System.out.println("【日志】准备发送消息...");
        super.send(message);
        System.out.println("【日志】消息发送完成。");
    }
}
```

------

### 5️⃣ 重试增强装饰器

```java
package io.github.atengk.designpattern.decorator.decorator;

import io.github.atengk.designpattern.decorator.component.MessageSender;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 重试增强装饰器
 * 在发送失败时进行重试
 */
@Component
public class RetryMessageDecorator extends AbstractMessageSenderDecorator {

    public RetryMessageDecorator(@Qualifier("defaultMessageSender") MessageSender delegate) {
        super(delegate);
    }

    @Override
    public void send(String message) {
        int retries = 3;
        while (retries-- > 0) {
            try {
                super.send(message);
                System.out.println("【重试机制】发送成功。");
                return;
            } catch (Exception e) {
                System.out.println("【重试机制】发送失败，剩余重试次数：" + retries);
                if (retries == 0) {
                    System.out.println("【重试机制】最终发送失败！");
                }
            }
        }
    }
}
```

------

### 6️⃣ 控制层演示

```java
package io.github.atengk.designpattern.decorator.controller;

import io.github.atengk.designpattern.decorator.component.DefaultMessageSender;
import io.github.atengk.designpattern.decorator.decorator.LogMessageDecorator;
import io.github.atengk.designpattern.decorator.decorator.RetryMessageDecorator;
import org.springframework.web.bind.annotation.*;

/**
 * 装饰器模式演示控制器
 */
@RestController
@RequestMapping("/api/message")
public class MessageSendController {

    private final DefaultMessageSender defaultMessageSender;

    public MessageSendController(DefaultMessageSender defaultMessageSender) {
        this.defaultMessageSender = defaultMessageSender;
    }

    @GetMapping("/send")
    public String send(@RequestParam String msg) {
        // 构建装饰链：日志 -> 重试 -> 真实发送
        RetryMessageDecorator retryDecorator =
                new RetryMessageDecorator(new LogMessageDecorator(defaultMessageSender));
        retryDecorator.send(msg);
        return "消息发送流程执行完成。";
    }
}
```

------

## 🚀 七、运行结果示例

请求：

```
GET http://localhost:8080/api/message/send?msg=测试装饰器
```

输出：

```
【日志】准备发送消息...
【发送消息】测试装饰器
【日志】消息发送完成。
【重试机制】发送成功。
```

------

## ✅ 八、模式总结

| 特点                | 说明                                                         |
| ------------------- | ------------------------------------------------------------ |
| **核心目标**        | 在不修改原始类的情况下，动态增强功能                         |
| **实现方式**        | 使用组合（包含）而非继承                                     |
| **优点**            | 可层层叠加、自由组合装饰逻辑                                 |
| **常见应用**        | I/O 流（InputStream）、AOP拦截链、日志增强                   |
| **Spring 实现对应** | Spring AOP、HandlerInterceptor、FilterChain 都是装饰思想的体现 |

------

## 🔧 九、可拓展建议

在实际项目中，可以进一步扩展：

- ✅ 将装饰链注册到 Spring 容器中，实现自动拼接；
- ✅ 结合 AOP 动态代理实现自动装饰；
- ✅ 应用于消息、缓存、日志、接口防重放等模块。

