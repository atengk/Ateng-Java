# RocketMQ消费者模块

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
  consumer:
    group: ${spring.application.name}
    pull-batch-size: 32          # 每次拉取消息最大数（默认32）
    message-model: CLUSTERING    # 消费模式：BROADCASTING 或 CLUSTERING（集群）
```



## 消息接受

### 普通消息消费者

```java
@RocketMQMessageListener(topic = "test-topic", consumerGroup = "consumer-group-1")
@Component
public class SimpleConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        System.out.println("收到普通消息: " + message);
    }
}
```

### 带 Tag 的消息（监听某个特定 tag）

```java
@RocketMQMessageListener(
    topic = "test-topic", 
    consumerGroup = "consumer-group-tag",
    selectorExpression = "TagA"  // 只消费 TagA 的消息
)
@Component
public class TagConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        System.out.println("收到带TagA的消息: " + message);
    }
}
```

### 顺序消息消费者（顺序消费同一个 `sharding key` 的消息）

```java
@RocketMQMessageListener(
    topic = "order-topic", 
    consumerGroup = "consumer-group-order", 
    consumeMode = ConsumeMode.ORDERLY
)
@Component
public class OrderlyConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        System.out.println("顺序消息消费: " + message);
    }
}
```

### 延迟消息消费者（正常消费，不用额外处理）

```java
@RocketMQMessageListener(topic = "delay-topic", consumerGroup = "consumer-group-delay")
@Component
public class DelayConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        System.out.println("收到延迟消息: " + message);
    }
}
```

### 事务消息消费者（只要事务成功提交，正常消费即可）

```java
@RocketMQMessageListener(topic = "tx-order-topic", consumerGroup = "consumer-group-tx")
@Component
public class TransactionConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        System.out.println("收到事务消息: " + message);
    }
}
```

### 对象消息（自动反序列化为 POJO）

```java
@RocketMQMessageListener(topic = "user-topic", consumerGroup = "consumer-group-user")
@Component
public class ObjectConsumer implements RocketMQListener<RocketMQSendController.User> {

    @Override
    public void onMessage(RocketMQSendController.User user) {
        System.out.println("收到用户对象消息: id=" + user.getId() + ", name=" + user.getName());
    }
}
```

### 带有Key的消息

```java
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@RocketMQMessageListener(topic = "test-topic", consumerGroup = "consumer-key-group")
@Component
public class KeyConsumer implements RocketMQListener<MessageExt> {

    @Override
    public void onMessage(MessageExt message) {
        String msgBody = new String(message.getBody());
        String keys = message.getKeys(); // ✅ 获取消息的 key
        String tags = message.getTags(); // 可选：获取 Tag

        System.out.println("收到消息: " + msgBody);
        System.out.println("Key: " + keys);
        System.out.println("Tag: " + tags);
        System.out.println("MsgId: " + message.getMsgId());
    }
}
```

