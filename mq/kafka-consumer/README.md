# Kafka消费者模块

Kafka 作为消费者并不直接发送消息，而是 **消费**（接收）消息。Kafka 的消费者通过订阅一个或多个 **主题（Topic）** 来接收生产者发布到这些主题中的消息。消费者从 Kafka 集群的 **分区（Partition）** 中拉取（pull）消息。

## 编辑配置文件

**编辑 `application.yml` ，配置kafka信息**

`auto-offset-reset` 是 Kafka 消费者的配置项，用于指定消费者在没有偏移量（首次消费或偏移量丢失）时，从哪个位置开始消费消息。它有以下三种可选值：

1. **`earliest`**：从最早的消息开始消费。
    - 适用场景：消费者首次启动或偏移量丢失时，需要从主题的最早消息开始消费。
2. **`latest`**：从最新的消息开始消费（跳过历史消息）。
    - 适用场景：只关心新的消息，不处理历史消息。
3. **`none`**：如果没有偏移量，则抛出错误，消费者无法启动。
    - 适用场景：确保偏移量必须有效，否则不能消费。

```yaml
---
# Kafka配置
spring:
  kafka:
    bootstrap-servers: 192.168.1.10:9094
    consumer:
      group-id: kafka.ateng.local
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

## 接收消息

**接收字符串消息**

```java
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * 接收字符串数据
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-03
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class StrConsumer {

    // 消费单条消息
    @KafkaListener(topics = "ateng.kafka.str")
    public void listen(ConsumerRecord<String, String> record) {
        String key = record.key();
        String topic = record.topic();
        long timestamp = record.timestamp();
        String data = record.value();
        log.info("发送[str]消息到Kafka, topic: {}, timestamp: {}, key: {}, data: {}", topic, timestamp, key, data);
    }

}
```

**接受对象实体消息**

创建对象实体

```java
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private LocalDateTime createdAt;

}
```

发送实体对象数据

```java
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.kafka.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * 接收实体对象数据
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-03
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ObjConsumer {

    // 消费单条消息
    @KafkaListener(topics = "ateng.kafka.user")
    public void listen(ConsumerRecord<String, String> record) {
        String key = record.key();
        String topic = record.topic();
        long timestamp = record.timestamp();
        String dataStr = record.value();
        User user = JSONObject.parseObject(dataStr).toJavaObject(User.class);
        log.info("发送[obj]消息到Kafka, topic: {}, timestamp: {}, key: {}, data: {}", topic, timestamp, key, user);
    }

}
```
