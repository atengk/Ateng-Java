## 🧩 适配器模式（Adapter Pattern）

### 📘 模式简介

**适配器模式**（Adapter Pattern）是一种结构型设计模式，它允许原本接口不兼容的类可以一起工作。
 该模式通过定义一个中间层“适配器”，将**现有接口转换为客户端期望的接口**，实现系统的灵活扩展。

在实际业务中，适配器常用于对接第三方系统（如：微信、钉钉、邮件、短信等），统一接口规范。

------

## 💡 实战案例：统一第三方消息推送接口

我们希望系统中有一个统一的消息发送接口 `MessageSender`，
 但不同渠道（如微信、钉钉、短信）提供的第三方SDK接口各不相同。
 这时我们就用适配器模式让它们“看起来一样”。

------

### 🏗️ 项目结构

```
io.github.atengk.design.adapter
 ├── adapter
 │    ├── WeChatMessageAdapter.java
 │    └── DingTalkMessageAdapter.java
 ├── service
 │    ├── MessageSender.java
 │    └── MessageService.java
 ├── thirdparty
 │    ├── WeChatApi.java
 │    └── DingTalkApi.java
 └── controller
      └── MessageController.java
```

------

### 🧠 1. 定义统一的消息发送接口

```java
package io.github.atengk.design.adapter.service;

/**
 * 定义系统内部统一的消息发送接口
 */
public interface MessageSender {
    /**
     * 发送消息
     * @param userId 接收者ID
     * @param content 消息内容
     */
    void sendMessage(String userId, String content);
}
```

------

### 🧩 2. 模拟第三方SDK接口（不可修改）

```java
package io.github.atengk.design.adapter.thirdparty;

/**
 * 模拟第三方微信SDK
 */
public class WeChatApi {
    public void pushToUser(String openId, String text) {
        System.out.println("【WeChat】推送给用户：" + openId + "，内容：" + text);
    }
}
package io.github.atengk.design.adapter.thirdparty;

/**
 * 模拟第三方钉钉SDK
 */
public class DingTalkApi {
    public void sendMsg(String userCode, String msg) {
        System.out.println("【DingTalk】发送给用户：" + userCode + "，消息：" + msg);
    }
}
```

> ⚠️ 注意：以上为第三方SDK接口，不能直接改动。

------

### 🔌 3. 创建适配器（Adapter）

```java
package io.github.atengk.design.adapter.adapter;

import io.github.atengk.design.adapter.service.MessageSender;
import io.github.atengk.design.adapter.thirdparty.WeChatApi;
import org.springframework.stereotype.Component;

/**
 * 微信消息适配器，实现系统统一接口
 */
@Component("weChatAdapter")
public class WeChatMessageAdapter implements MessageSender {

    private final WeChatApi weChatApi = new WeChatApi();

    @Override
    public void sendMessage(String userId, String content) {
        // 调用第三方接口
        weChatApi.pushToUser(userId, content);
    }
}
package io.github.atengk.design.adapter.adapter;

import io.github.atengk.design.adapter.service.MessageSender;
import io.github.atengk.design.adapter.thirdparty.DingTalkApi;
import org.springframework.stereotype.Component;

/**
 * 钉钉消息适配器，实现系统统一接口
 */
@Component("dingTalkAdapter")
public class DingTalkMessageAdapter implements MessageSender {

    private final DingTalkApi dingTalkApi = new DingTalkApi();

    @Override
    public void sendMessage(String userId, String content) {
        // 调用第三方接口
        dingTalkApi.sendMsg(userId, content);
    }
}
```

------

### ⚙️ 4. 定义业务服务类

```java
package io.github.atengk.design.adapter.service;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 消息服务：通过适配器统一调用不同消息渠道
 */
@Service
public class MessageService {

    private final Map<String, MessageSender> senderMap;

    public MessageService(Map<String, MessageSender> senderMap) {
        this.senderMap = senderMap;
    }

    public void send(String channel, String userId, String message) {
        MessageSender sender = senderMap.get(channel);
        if (sender == null) {
            throw new IllegalArgumentException("未知的消息渠道：" + channel);
        }
        sender.sendMessage(userId, message);
    }
}
```

------

### 🧭 5. 控制器调用示例

```java
package io.github.atengk.design.adapter.controller;

import io.github.atengk.design.adapter.service.MessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息发送控制器
 */
@RestController
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/send")
    public String send(@RequestParam String channel, 
                       @RequestParam String userId, 
                       @RequestParam String content) {
        messageService.send(channel, userId, content);
        return "消息已通过 " + channel + " 发送";
    }
}
```

------

### 🚀 6. 运行结果

访问接口：

```
http://localhost:8080/send?channel=weChatAdapter&userId=wx123&content=你好
http://localhost:8080/send?channel=dingTalkAdapter&userId=dd456&content=测试
```

输出日志：

```
【WeChat】推送给用户：wx123，内容：你好
【DingTalk】发送给用户：dd456，消息：测试
```

------

### 🧭 总结

| 角色                                             | 说明                           |
| ------------------------------------------------ | ------------------------------ |
| `MessageSender`                                  | 目标接口（Target）             |
| `WeChatApi`、`DingTalkApi`                       | 第三方类（Adaptee）            |
| `WeChatMessageAdapter`、`DingTalkMessageAdapter` | 适配器（Adapter）              |
| `MessageService`                                 | 客户端（Client），统一调用接口 |

**优点：**

- 解耦业务逻辑与第三方SDK。
- 新增渠道仅需增加一个适配器类，不影响原有逻辑。
- 符合开闭原则（OCP）。

