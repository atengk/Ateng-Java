# Kafka生产者模块

Kafka 作为生产者发送消息是 Kafka 系统中一个核心的操作。生产者（Producer）是向 Kafka 主题（Topic）发布消息的客户端，通常我们会通过生产者发送消息到 Kafka 集群中的特定主题，消费者（Consumer）再从这些主题中消费消息。



## 编辑配置文件

编辑 ` application.yml` ，配置kafka信息

```yaml
---
# Kafka配置
spring:
  kafka:
    bootstrap-servers: 192.168.1.10:9094
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      properties:
        max.request.size: 10485760  # 生产者可以发送的单个请求的最大大小为10MB（10485760字节）
```

## 发送消息

**发送字符串消息**

```java
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 发送字符串数据
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-03
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class StrProducer {
    private final KafkaTemplate kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void send() {
        String topic = "ateng.kafka.str";
        String key = DateUtil.now();
        String data = JSONObject.of(
                "id", IdUtil.fastSimpleUUID(),
                "dateTime", DateUtil.date()
        ).toJSONString();
        kafkaTemplate.send(topic, key, data);
        log.info("发送[str]消息到Kafka, topic: {}, key: {}, data: {}", topic, key, data);
    }

}
```

**发送对象实体消息**

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
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.kafka.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 发送实体对象数据
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-03
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ObjProducer {
    private final KafkaTemplate kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void send() {
        String topic = "ateng.kafka.user";
        User user = new User(IdUtil.fastSimpleUUID(), RandomUtil.randomString(5), LocalDateTimeUtil.now());
        String msg = JSONObject.toJSONString(user);
        kafkaTemplate.send(topic, msg);
        log.info("发送[obj]消息到Kafka, topic: {}, data: {}", topic, msg);
    }

}
```

