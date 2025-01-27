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
        log.info("接受[str]消息, topic: {}, timestamp: {}, key: {}, data: {}", topic, timestamp, key, data);
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
package local.ateng.java.kafka.service;

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
        log.info("接受[obj]消息, topic: {}, timestamp: {}, key: {}, data: {}", topic, timestamp, key, user);
    }

}
```

## 批量接受消息

### 设置批量消费工厂

```java
package local.ateng.java.kafka.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class KafkaConfig {
    // 配置 Kafka 消费者的属性
    @Value("${kafka.bootstrap.servers:192.168.1.10:9094}")
    private String bootstrapServers;

    @Value("${kafka.consumer.group-id:kafka.ateng.local}")
    private String groupId;

    @Value("${kafka.consumer.max-poll-records:3}")
    private int maxPollRecords;

    @Value("${kafka.consumer.max-poll-interval-ms:20000}")
    private int maxPollIntervalMs;

    @Value("${kafka.consumer.auto-commit-interval-ms:100}")
    private int autoCommitIntervalMs;


    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "batch." + groupId);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);  // 每次拉取的最大记录数
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, autoCommitIntervalMs);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        log.info("Kafka Consumer Configs: {}", props);  // 日志记录消费者配置
        return props;
    }

    // 配置 KafkaListener 的批量消费工厂
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>> batchFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumerConfigs()));
        factory.setConcurrency(3);  // 设置并发数量(分区数量)
        factory.setBatchListener(true);  // 启用批量监听
        return factory;
    }


}
```

### 批量消费消息

```java
package local.ateng.java.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

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

    // 批量消息消费
    @KafkaListener(topics = "ateng.kafka.str", containerFactory = "batchFactory")
    public void listenBatch(List<ConsumerRecord<String, String>> records) {
        log.info("批量消息消费，数据条数：{}", records.size());
        for (ConsumerRecord<String, String> record : records) {
            String key = record.key();
            String topic = record.topic();
            long timestamp = record.timestamp();
            String data = record.value();
            //log.info("接受[str]批量消息, topic: {}, timestamp: {}, key: {}, data: {}", topic, timestamp, key, data);
        }
    }

}
```

