# RocketMQ生产者模块

**RocketMQ** 是 **Apache** 顶级开源的**分布式消息队列**，最初由 **阿里巴巴** 开发，具备**高吞吐、低延迟、高可用**等特性，广泛用于**异步解耦、分布式事务、流式计算**等场景。RocketMQ **5.x** 版本引入 **Controller、Proxy、云原生支持**，增强了**多协议兼容性（HTTP/gRPC/MQTT）、自动主从切换、存储优化**。其核心组件包括 **NameServer（注册中心）、Broker（存储转发）、Controller（高可用管理）、Proxy（协议适配）**，适合**云环境和高并发业务** 🚀。

- [官网链接](https://rocketmq.apache.org/zh/)



## 基础配置

**添加依赖**

```xml
<!-- RocketMQ Starter 依赖 -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.3.3</version>
</dependency>
```

**配置application.yml**

```yaml
---
# RocketMQ配置
rocketmq:
  name-server: 192.168.1.12:9876  # RocketMQ nameserver 地址
  producer:
    group: ${spring.application.name}  # 生产者组名
    send-message-timeout: 3000  # 发送消息超时时间(毫秒)
    retry-times-when-send-failed: 2  # 发送失败重试次数
```

**创建测试类**

```java
@RestController
@RequestMapping("/mq")
public class RocketMqProducerController {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

}
```



## 消息发送

### 同步发送普通消息

```java
    // 1. 同步发送普通消息
    @GetMapping("/sendSync")
    public String sendSync(@RequestParam String msg) {
        SendResult result = rocketMQTemplate.syncSend("test-topic", msg);
        return "同步发送成功: " + result.getSendStatus();
    }
```

### 异步发送消息

```java
    // 2. 异步发送消息
    @GetMapping("/sendAsync")
    public String sendAsync(@RequestParam String msg) {
        rocketMQTemplate.asyncSend("test-topic", msg, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                System.out.println("异步发送成功: " + sendResult.getMsgId());
            }

            @Override
            public void onException(Throwable throwable) {
                System.out.println("异步发送失败: " + throwable.getMessage());
            }
        });
        return "异步发送中，请看控制台日志";
    }
```

### 单向发送

```java
    // 3. 单向发送（不关心结果）
    @GetMapping("/sendOneWay")
    public String sendOneWay(@RequestParam String msg) {
        rocketMQTemplate.sendOneWay("test-topic", msg);
        return "单向发送成功（不保证送达）";
    }
```

### 发送带Tag的消息

```java
    // 4. 发送带Tag的消息（例如 test-topic:tagA）
    @GetMapping("/sendWithTag")
    public String sendWithTag(@RequestParam String msg) {
        rocketMQTemplate.convertAndSend("test-topic:TagA", msg);
        return "带Tag发送成功";
    }
```

### 同步发送普通消息

```java
    // 1. 同步发送普通消息
    @GetMapping("/sendSync")
    public String sendSync(@RequestParam String msg) {
        SendResult result = rocketMQTemplate.syncSend("test-topic", msg);
        return "同步发送成功: " + result.getSendStatus();
    }
```

### 顺序消息发送

```java
    // 5. 顺序消息发送（如订单步骤）
    @GetMapping("/sendOrderly")
    public String sendOrderly(@RequestParam String msg, @RequestParam Long orderId) {
        rocketMQTemplate.syncSendOrderly("order-topic", msg, String.valueOf(orderId));
        return "顺序消息发送成功";
    }
```

### 延迟消息发送

```java
    // 6. 延迟消息发送（延迟级别是预定义的）
    @GetMapping("/sendDelay")
    public String sendDelay(@RequestParam String msg) {
        // 延迟级别：1 = 1s, 2 = 5s, 3 = 10s, 4 = 30s, 5 = 1m ...
        int delayLevel = 3; // 10秒后发送
        SendResult result = rocketMQTemplate.syncSend("delay-topic", MessageBuilder.withPayload(msg).build(), 3000, delayLevel);
        return "延迟消息发送成功: " + result.getSendStatus();
    }
```

### 发送对象消息

```java
    // 7. 发送对象消息（自动JSON序列化）
    @GetMapping("/sendObject")
    public String sendObject() {
        User user = new User(1L, "张三");
        rocketMQTemplate.convertAndSend("user-topic", user);
        return "对象消息发送成功";
    }
```



## 发送带 Key 的消息

**编辑配置文件**

添加 `enable-msg-trace: true` 配置

```yaml
---
# RocketMQ配置
rocketmq:
  name-server: 192.168.1.12:9876  # RocketMQ nameserver 地址
  producer:
    group: ${spring.application.name}  # 生产者组名
    send-message-timeout: 3000  # 发送消息超时时间(毫秒)
    retry-times-when-send-failed: 2  # 发送失败重试次数
    enable-msg-trace: true  # 开启 消息轨迹（Trace）
```

**发送带 Key 的消息**

```java
    // 8. 发送带 Key 的消息
    @GetMapping("/sendWithKey")
    public String sendWithKey(@RequestParam String msg, @RequestParam String key) throws MQBrokerException, RemotingException, InterruptedException, MQClientException {
        // 手动构建 RocketMQ 原生 Message 对象，指定 key
        org.apache.rocketmq.common.message.Message message = new org.apache.rocketmq.common.message.Message(
                "key-topic",         // topic
                "KeyTag",            // tag（可选）
                key,                 // key
                msg.getBytes()       // body
        );

        SendResult result = rocketMQTemplate.getProducer().send(message);
        return "发送带Key成功: " + result.getSendStatus() + ", key=" + key;
    }
```

## 事务消息

### 创建事务监听器

```java
package local.ateng.java.kafka.listener;

import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RocketMQTransactionListener
public class OrderTransactionListener implements RocketMQLocalTransactionListener {

    // 模拟订单状态记录（实际项目中可接数据库）
    private final Map<String, Boolean> orderDB = new ConcurrentHashMap<>();

    // 执行本地事务
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        String orderId = (String) msg.getHeaders().get("orderId");
        System.out.println("【本地事务执行中】订单ID: " + orderId);

        try {
            // 模拟业务逻辑：成功或失败（例如模拟失败的订单）
            if (orderId.startsWith("fail")) {
                System.out.println("模拟本地事务失败，回滚消息");
                return RocketMQLocalTransactionState.ROLLBACK;
            }

            // 模拟写入“数据库”
            orderDB.put(orderId, true);
            System.out.println("本地事务成功，提交消息");
            return RocketMQLocalTransactionState.COMMIT;

        } catch (Exception e) {
            e.printStackTrace();
            return RocketMQLocalTransactionState.UNKNOWN;
        }
    }

    // MQ 事务回查逻辑（如果上面返回 UNKNOWN）
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String orderId = (String) msg.getHeaders().get("orderId");
        System.out.println("【事务回查】订单ID: " + orderId);

        Boolean status = orderDB.get(orderId);
        if (status != null && status) {
            return RocketMQLocalTransactionState.COMMIT;
        } else {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }
}
```

### 发送事务消息

```java
    @GetMapping("/sendTx")
    public String sendTransactionMsg(@RequestParam String orderId) {
        String topic = "tx-topic";

        // 构造消息，带上订单ID作为头部参数
        org.springframework.messaging.Message<String> message = MessageBuilder
                .withPayload("订单创建消息: " + orderId)
                .setHeader("orderId", orderId)  // 设置业务键
                .build();

        // 发送事务消息
        rocketMQTemplate.sendMessageInTransaction(topic, message, null);
        return "事务消息已发送，订单ID: " + orderId;
    }
```

