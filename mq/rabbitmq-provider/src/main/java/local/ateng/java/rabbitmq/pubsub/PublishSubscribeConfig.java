package local.ateng.java.rabbitmq.pubsub;

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

    public static final String QUEUE_NAME1 = "ateng.rabbit.queue.pubsub1";
    public static final String QUEUE_NAME2 = "ateng.rabbit.queue.pubsub2";
    public static final String EXCHANGE_NAME = "ateng.rabbit.exchange.pubsub";

    @Bean
    public Queue publishSubscribeQueue1() {
        return new Queue(QUEUE_NAME1, true);
    }

    @Bean
    public Queue publishSubscribeQueue2() {
        return new Queue(QUEUE_NAME2, true);
    }

    @Bean
    public FanoutExchange publishSubscribeExchange() {
        return new FanoutExchange(EXCHANGE_NAME);
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
