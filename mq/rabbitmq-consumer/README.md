# RabbitMQ消费者模块

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

**接收消息**

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 简单队列模式 (Point-to-Point)
 * 这种方式通常是基于 Direct Exchange 来实现的，适用于多个生产者将消息发送到一个队列，然后由消费者消费这些消息。每次有消息生产，消费者就会消费一条消息。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-02
 */
@Component
@Slf4j
public class PointToPointMessageListener {

    @RabbitListener(queues = "ateng.rabbit.queue.p2p")
    public void receiveSimpleMessage(String message) {
        log.info("[简单队列模式] 收到消息：{}", message);
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

**接收消息**

生产者通过 **FanoutExchange** 将消息广播到所有队列中，消费者通过消费不同队列来获取消息。

```java
package local.ateng.java.rabbitmq.pubsub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 发布/订阅模式 (Publish/Subscribe)
 * 这种模式通常使用 Fanout Exchange 来实现，生产者将消息发送到交换机，交换机将消息广播到所有绑定的队列。消费者从队列中接收消息。每个消费者都会接收到相同的消息。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-02
 */
@Component
@Slf4j
public class PublishSubscribeMessageListener {

    @RabbitListener(queues = "ateng.rabbit.queue.pubsub1")
    public void receiveFromQueue1(String message) {
        log.info("[发布/订阅模式t1] 收到消息：{}", message);
    }

    @RabbitListener(queues = "ateng.rabbit.queue.pubsub2")
    public void receiveFromQueue2(String message) {
        log.info("[发布/订阅模式t2] 收到消息：{}", message);
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

**接收消息**

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 路由模式 (Routing)
 * 这种模式使用 Direct Exchange 和 路由键来控制消息的路由。消息通过一个精确的路由键（Routing Key）发送到特定的队列。只有匹配该路由键的队列才能收到消息。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-02
 */
@Component
@Slf4j
public class RoutingMessageListener {

    @RabbitListener(queues = "ateng.rabbit.queue.routing1")
    public void receiveFromQueue1(String message) {
        log.info("[路由模式t1] 收到消息：{}", message);
    }

    @RabbitListener(queues = "ateng.rabbit.queue.routing2")
    public void receiveFromQueue2(String message) {
        log.info("[路由模式t2] 收到消息：{}", message);
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

**接收消息**

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 主题模式 (Topic)
 * 在 Topic Exchange 中，路由键使用了通配符（* 和 #）来进行消息路由。* 匹配一个词，# 匹配多个词。这样可以实现更加灵活的消息路由规则。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-02
 */
@Component
@Slf4j
public class TopicMessageListener {

    @RabbitListener(queues = "ateng.rabbit.queue.topic1")
    public void receiveFromQueue1(String message) {
        log.info("[主题模式t1] 收到消息：{}", message);
    }

    @RabbitListener(queues = "ateng.rabbit.queue.topic2")
    public void receiveFromQueue2(String message) {
        log.info("[主题模式t2] 收到消息：{}", message);
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

**接收消息**

使用 `@RabbitListener` 注解时，确保在 `ackMode` 属性中指定手动确认模式，并在消息处理逻辑中显式调用 `channel.basicAck()` 或 `channel.basicReject()`。确保你在消费失败时 **拒绝消息**，这样消息才会进入死信队列。

```java
import cn.hutool.core.util.RandomUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
@Component
@Slf4j
public class DeadLetterMessageListener {

    // 设置 ackMode 为 "MANUAL"，而不是 "AUTO"，这样才能手动确认消息。
    @RabbitListener(queues = "ateng.rabbit.queue.deadletter.normal", ackMode = "MANUAL")
    public void receiveFromQueue1(String message, Channel channel, Message rabbitMessage) {
        try {
            if (RandomUtil.randomInt(0, 3) == 1) {
                log.error("[死信队列-正常队列] 收到错误消息，转发到死信队列：{}", message);
                throw new RuntimeException("模拟报错");
            }
            log.info("[死信队列-正常队列] 收到消息：{}", message);

            // 手动确认消息
            channel.basicAck(rabbitMessage.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 如果消息处理失败，拒绝该消息，并且不重新入队
            try {
                channel.basicReject(rabbitMessage.getMessageProperties().getDeliveryTag(), false);
            } catch (IOException ex) {
                log.error("消息拒绝失败", ex);
            }
        }
    }

    @RabbitListener(queues = "ateng.rabbit.queue.deadletter.dlx")
    public void receiveFromQueue2(String message) {
        log.info("[死信队列-死信队列] 收到消息：{}", message);
    }
}
```



