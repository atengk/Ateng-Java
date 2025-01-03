package local.ateng.java.rabbitmq.delayed;

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
                .withArgument("x-message-ttl", 60000)  // 设置消息TTL，单位为毫秒，10分钟（60000 ms）
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
