# RabbitMQ生产者模块

RabbitMQ 是一种开源的消息代理软件，广泛用于实现消息队列系统。它支持多种消息传递模式，满足不同的应用需求。RabbitMQ 基于 AMQP (Advanced Message Queuing Protocol) 协议，它为不同的消息传递方式提供了多种机制，通常通过 交换机（Exchange）、队列（Queue）和 路由键（Routing Key）来定义消息的流动和处理方式。


## 简单队列模式 (Point-to-Point)

这种方式通常是基于 **Direct Exchange** 来实现的，适用于多个生产者将消息发送到一个队列，然后由消费者消费这些消息。每次有消息生产，消费者就会消费一条消息。

**特点：**

- 一个生产者发送消息到一个队列，多个消费者轮询消费队列中的消息。
- 这种模式最常用于任务队列或者负载均衡的场景。

**典型应用场景：**

- 任务调度：多个消费者（比如 worker）从同一个队列中拉取任务进行处理。

**实现：**

- 生产者发送消息到队列（Queue）。
- 消费者从队列中拉取消息。

**创建配置绑定队列和交换机**

```java
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 简单队列模式 (Point-to-Point)
 * 这种方式通常是基于 Direct Exchange 来实现的，适用于多个生产者将消息发送到一个队列，然后由消费者消费这些消息。每次有消息生产，消费者就会消费一条消息。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-02
 */
@Configuration
public class PointToPointConfig {

    public static final String TOPIC_QUEUE_NAME = "ateng.rabbit.queue.p2p";
    public static final String TOPIC_EXCHANGE_NAME = "ateng.rabbit.exchange.p2p";
    public static final String TOPIC_ROUTING_KEY = "ateng.rabbit.routingkey.p2p";

    @Bean
    public Queue queue() {
        return new Queue(TOPIC_QUEUE_NAME, true);
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(TOPIC_EXCHANGE_NAME);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(queue()).to(exchange()).with(TOPIC_ROUTING_KEY);
    }

}
```

**创建任务发送消息**

```java
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 发送消息
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-02
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class PointToPointTask {
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 5000)
    public void send() {
        String message = StrUtil.format("{}:Hello Rabbit", DateUtil.now());
        rabbitTemplate.convertAndSend(PointToPointConfig.TOPIC_EXCHANGE_NAME, PointToPointConfig.TOPIC_ROUTING_KEY, message);
        log.info("[简单队列模式] 发送消息：{}", message);
    }

}
```



## 发布/订阅模式 (Publish/Subscribe)

这种模式通常使用 **Fanout Exchange** 来实现，生产者将消息发送到交换机，交换机将消息广播到所有绑定的队列。消费者从队列中接收消息。每个消费者都会接收到相同的消息。

**特点**：

- 生产者发送消息到交换机，交换机会将消息广播到多个队列。
- 每个消费者会接收到消息的副本。
- 适合于将相同的消息广播到多个接收者。

**典型应用场景**：

- 日志收集：多个服务将日志消息发送到日志队列，消费者从队列中消费日志信息。
- 实时消息推送：同一条消息需要推送给多个接收者。

**实现**：

- 生产者发送消息到交换机（Fanout Exchange）。
- 交换机广播消息到所有绑定的队列。

**创建配置绑定队列和交换机**

```java
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 发布/订阅模式 (Publish/Subscribe)
 * 这种模式通常使用 Fanout Exchange 来实现，生产者将消息发送到交换机，交换机将消息广播到所有绑定的队列。消费者从队列中接收消息。每个消费者都会接收到相同的消息。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-02
 */
@Configuration
public class PublishSubscribeConfig {

    public static final String TOPIC_QUEUE_NAME1 = "ateng.rabbit.queue.pubsub1";
    public static final String TOPIC_QUEUE_NAME2 = "ateng.rabbit.queue.pubsub2";
    public static final String TOPIC_EXCHANGE_NAME = "ateng.rabbit.exchange.pubsub";

    @Bean
    public Queue publishSubscribeQueue1() {
        return new Queue(TOPIC_QUEUE_NAME1, true);
    }

    @Bean
    public Queue publishSubscribeQueue2() {
        return new Queue(TOPIC_QUEUE_NAME2, true);
    }

    @Bean
    public FanoutExchange publishSubscribeExchange() {
        return new FanoutExchange(TOPIC_EXCHANGE_NAME);
    }

    @Bean
    public Binding publishSubscribeBinding1() {
        return BindingBuilder.bind(publishSubscribeQueue1()).to(publishSubscribeExchange());
    }

    @Bean
    public Binding publishSubscribeBinding2() {
        return BindingBuilder.bind(publishSubscribeQueue2()).to(publishSubscribeExchange());
    }
}
```

**创建任务发送消息**

```java
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 发送消息
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-02
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class PublishSubscribeTask {
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 5000)
    public void send() {
        String message = StrUtil.format("{}:Hello Rabbit", DateUtil.now());
        rabbitTemplate.convertAndSend(PublishSubscribeConfig.TOPIC_EXCHANGE_NAME, null, message);
        log.info("[发布/订阅模式] 发送消息：{}", message);
    }

}
```

## 路由模式 (Routing)

这种模式使用 **Direct Exchange** 和 **路由键**来控制消息的路由。消息通过一个精确的路由键（Routing Key）发送到特定的队列。只有匹配该路由键的队列才能收到消息。

**特点**：

- 生产者将消息发送到交换机，并且指定一个路由键。
- 消费者的队列绑定交换机时，指定与路由键匹配的路由规则。
- 比较灵活，生产者和消费者之间通过路由键来实现精确匹配。

**典型应用场景**：

- 根据消息内容或类型进行精确路由（例如，处理不同的日志级别：`info`、`warn`、`error`）。

**实现**：

- 生产者发送消息到交换机，并使用路由键（如`error`）。
- 队列根据路由键的匹配来接收消息。

**创建配置绑定队列和交换机**

```java
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 路由模式 (Routing)
 * 这种模式使用 Direct Exchange 和 路由键来控制消息的路由。消息通过一个精确的路由键（Routing Key）发送到特定的队列。只有匹配该路由键的队列才能收到消息。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-02
 */
@Configuration
public class RoutingConfig {

    public static final String TOPIC_QUEUE_NAME1 = "ateng.rabbit.queue.routing1";
    public static final String TOPIC_QUEUE_NAME2 = "ateng.rabbit.queue.routing2";
    public static final String TOPIC_EXCHANGE_NAME = "ateng.rabbit.exchange.routing";
    public static final String TOPIC_ROUTING_KEY1 = "ateng.rabbit.routingkey.routing1";
    public static final String TOPIC_ROUTING_KEY2 = "ateng.rabbit.routingkey.routing2";


    @Bean
    public Queue routingQueue1() {
        return new Queue(TOPIC_QUEUE_NAME1, true);
    }

    @Bean
    public Queue routingQueue2() {
        return new Queue(TOPIC_QUEUE_NAME2, true);
    }

    @Bean
    public DirectExchange routingExchange() {
        return new DirectExchange(TOPIC_EXCHANGE_NAME);
    }

    @Bean
    public Binding routingBinding1() {
        return BindingBuilder.bind(routingQueue1()).to(routingExchange()).with(TOPIC_ROUTING_KEY1);
    }

    @Bean
    public Binding routingBinding2() {
        return BindingBuilder.bind(routingQueue2()).to(routingExchange()).with(TOPIC_ROUTING_KEY2);
    }
}
```

**创建任务发送消息**

```java
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 发送消息
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-02
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class RoutingTask {
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 5000)
    public void send1() {
        String message = StrUtil.format("k1:{}:Hello Rabbit", DateUtil.now());
        rabbitTemplate.convertAndSend(RoutingConfig.TOPIC_EXCHANGE_NAME, RoutingConfig.TOPIC_ROUTING_KEY1, message);
        log.info("[路由模式k1] 发送消息：{}", message);
    }

    @Scheduled(fixedRate = 5000)
    public void send2() {
        String message = StrUtil.format("k2:{}:Hello Rabbit", DateUtil.now());
        rabbitTemplate.convertAndSend(RoutingConfig.TOPIC_EXCHANGE_NAME, RoutingConfig.TOPIC_ROUTING_KEY2, message);
        log.info("[路由模式k2] 发送消息：{}", message);
    }

}
```

## 主题模式 (Topic)

在 **Topic Exchange** 中，路由键使用了通配符（`*` 和 `#`）来进行消息路由。`*` 匹配一个词，`#` 匹配多个词。这样可以实现更加灵活的消息路由规则。

**特点**：

- 生产者使用路由键（包含多个词）发送消息。
- 消费者可以通过指定路由键的模式，灵活地匹配消息。
- 用于处理层次化的消息，比如`log.error.critical`、`log.info.debug`等。

**典型应用场景**：

- 日志收集系统，能够根据日志级别、服务名称等灵活过滤消息。
- 事件驱动架构中，能够通过模式匹配路由消息。

**实现**：

- 生产者发送消息到交换机，并使用包含多个词的路由键。
- 消费者根据路由键的模式来过滤消息。

**创建配置绑定队列和交换机**

```java
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 主题模式 (Topic)
 * 在 Topic Exchange 中，路由键使用了通配符（* 和 #）来进行消息路由。* 匹配一个词，# 匹配多个词。这样可以实现更加灵活的消息路由规则。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-02
 */
@Configuration
public class TopicConfig {

    public static final String TOPIC_QUEUE_NAME1 = "ateng.rabbit.queue.topic1";
    public static final String TOPIC_QUEUE_NAME2 = "ateng.rabbit.queue.topic2";
    public static final String TOPIC_EXCHANGE_NAME = "ateng.rabbit.exchange.topic";
    public static final String TOPIC_ROUTING_KEY1 = "ateng.rabbit.topickey.topic.dev.t1";
    public static final String TOPIC_ROUTING_KEY2 = "ateng.rabbit.topickey.topic.prod.#";


    @Bean
    public Queue topicQueue1() {
        return new Queue(TOPIC_QUEUE_NAME1, true);
    }

    @Bean
    public Queue topicQueue2() {
        return new Queue(TOPIC_QUEUE_NAME2, true);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE_NAME);
    }

    @Bean
    public Binding topicBinding1() {
        return BindingBuilder.bind(topicQueue1()).to(topicExchange()).with(TOPIC_ROUTING_KEY1);
    }

    @Bean
    public Binding topicBinding2() {
        return BindingBuilder.bind(topicQueue2()).to(topicExchange()).with(TOPIC_ROUTING_KEY2);
    }
}
```

**创建任务发送消息**

```java
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 发送消息
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-02
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class TopicTask {
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 5000)
    public void send1() {
        String message = StrUtil.format("k1:{}:Hello Rabbit", DateUtil.now());
        rabbitTemplate.convertAndSend(TopicConfig.TOPIC_EXCHANGE_NAME, TopicConfig.TOPIC_ROUTING_KEY1, message);
        log.info("[主题模式k1] 发送消息：{}", message);
    }

    @Scheduled(fixedRate = 5000)
    public void send2() {
        String message = StrUtil.format("k2:{}:Hello Rabbit", DateUtil.now());
        rabbitTemplate.convertAndSend(TopicConfig.TOPIC_EXCHANGE_NAME, "ateng.rabbit.topickey.topic.prod.t2", message);
        log.info("[主题模式k2] 发送消息：{}", message);
    }

    @Scheduled(fixedRate = 5000)
    public void send3() {
        String message = StrUtil.format("k3:{}:Hello Rabbit", DateUtil.now());
        rabbitTemplate.convertAndSend(TopicConfig.TOPIC_EXCHANGE_NAME, "ateng.rabbit.topickey.topic.prod.t3", message);
        log.info("[主题模式k3] 发送消息：{}", message);
    }

}
```

## 死信队列 (Dead Letter Queue, DLQ)

死信队列（DLQ）用于处理无法被正常消费的消息。死信通常出现在以下几种场景：

- 消息被消费失败并且设置了最大重试次数。
- 消息在队列中过期（TTL 到期）。
- 队列达到最大长度。

**特点**：

- 为无法消费的消息提供一个“后备”队列，便于后期处理。
- 通过设置交换机、队列的属性，可以将死信消息路由到其他队列。

**典型应用场景**：

- 异常处理：处理因消费失败或达到过期时间的消息。
- 重试机制：把失败的消息重试放入死信队列，或者做日志记录。

**实现**：

通过设置队列的 `x-dead-letter-exchange` 和 `x-dead-letter-routing-key` 来指定死信队列。

**创建配置绑定队列和交换机**

```java
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 死信队列 (Dead Letter Queue, DLQ)
 * 死信队列（DLQ）用于处理无法被正常消费的消息。死信通常出现在以下几种场景：
 * 消息被消费失败并且设置了最大重试次数。
 * 消息在队列中过期（TTL 到期）。
 * 队列达到最大长度。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-02
 */
@Configuration
public class DeadLetterConfig {

    public static final String QUEUE_NAME_NORMAL = "ateng.rabbit.queue.deadletter.normal";
    public static final String QUEUE_NAME_DLX = "ateng.rabbit.queue.deadletter.dlx";
    public static final String EXCHANGE_NAME = "ateng.rabbit.exchange.deadletter.normal";
    public static final String EXCHANGE_NAME_DLX = "ateng.rabbit.exchange.deadletter.dlx";
    public static final String ROUTING_KEY_NORMAL = "ateng.rabbit.topickey.deadletter.normal";
    public static final String ROUTING_KEY_DLX = "ateng.rabbit.topickey.deadletter.dlx";

    // 普通队列
    @Bean
    public Queue deadLetterQueueNormal() {
        return QueueBuilder.durable(QUEUE_NAME_NORMAL)
                .withArgument("x-dead-letter-exchange", EXCHANGE_NAME_DLX)  // 设置死信交换机
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLX)  // 设置死信队列的路由键
                .build();
    }

    // 死信队列
    @Bean
    public Queue deadLetterQueueDlx() {
        return new Queue(QUEUE_NAME_DLX);
    }

    // 普通交换机
    @Bean
    public DirectExchange deadLetterExchangeNormal() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    // 死信交换机
    @Bean
    public DirectExchange deadLetterExchangeDlx() {
        return new DirectExchange(EXCHANGE_NAME_DLX);
    }

    // 普通队列绑定到普通交换机
    @Bean
    public Binding deadLetterBindingNormal() {
        return BindingBuilder.bind(deadLetterQueueNormal()).to(deadLetterExchangeNormal()).with(ROUTING_KEY_NORMAL);
    }

    // 死信队列绑定到死信交换机
    @Bean
    public Binding deadLetterBindingDlx() {
        return BindingBuilder.bind(deadLetterQueueDlx()).to(deadLetterExchangeDlx()).with(ROUTING_KEY_DLX);
    }
}
```

**创建任务发送消息**

```java
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 发送消息
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-02
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class DeadLetterTask {
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 5000)
    public void send() {
        String message = StrUtil.format("{}:Hello Rabbit", DateUtil.now());
        rabbitTemplate.convertAndSend(DeadLetterConfig.EXCHANGE_NAME, DeadLetterConfig.ROUTING_KEY_NORMAL, message);
        log.info("[死信队列] 发送消息：{}", message);
    }

}
```

