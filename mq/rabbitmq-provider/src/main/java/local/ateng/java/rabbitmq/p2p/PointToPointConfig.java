package local.ateng.java.rabbitmq.p2p;

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

    public static final String QUEUE_NAME = "ateng.rabbit.queue.p2p";
    public static final String EXCHANGE_NAME = "ateng.rabbit.exchange.p2p";
    public static final String ROUTING_KEY = "ateng.rabbit.routingkey.p2p";

    @Bean
    public Queue pointToPointQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public DirectExchange pointToPointExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding pointToPointBinding() {
        return BindingBuilder.bind(pointToPointQueue()).to(pointToPointExchange()).with(ROUTING_KEY);
    }

}
