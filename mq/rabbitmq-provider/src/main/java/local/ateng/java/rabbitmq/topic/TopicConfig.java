package local.ateng.java.rabbitmq.topic;

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

    public static final String QUEUE_NAME1 = "ateng.rabbit.queue.topic1";
    public static final String QUEUE_NAME2 = "ateng.rabbit.queue.topic2";
    public static final String EXCHANGE_NAME = "ateng.rabbit.exchange.topic";
    public static final String ROUTING_KEY1 = "ateng.rabbit.topickey.topic.dev.t1";
    public static final String ROUTING_KEY2 = "ateng.rabbit.topickey.topic.prod.#";


    @Bean
    public Queue topicQueue1() {
        return new Queue(QUEUE_NAME1, true);
    }

    @Bean
    public Queue topicQueue2() {
        return new Queue(QUEUE_NAME2, true);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding topicBinding1() {
        return BindingBuilder.bind(topicQueue1()).to(topicExchange()).with(ROUTING_KEY1);
    }

    @Bean
    public Binding topicBinding2() {
        return BindingBuilder.bind(topicQueue2()).to(topicExchange()).with(ROUTING_KEY2);
    }
}
