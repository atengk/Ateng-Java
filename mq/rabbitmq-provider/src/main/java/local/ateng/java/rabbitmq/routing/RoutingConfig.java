package local.ateng.java.rabbitmq.routing;

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

    public static final String QUEUE_NAME1 = "ateng.rabbit.queue.routing1";
    public static final String QUEUE_NAME2 = "ateng.rabbit.queue.routing2";
    public static final String EXCHANGE_NAME = "ateng.rabbit.exchange.routing";
    public static final String ROUTING_KEY1 = "ateng.rabbit.routingkey.routing1";
    public static final String ROUTING_KEY2 = "ateng.rabbit.routingkey.routing2";


    @Bean
    public Queue routingQueue1() {
        return new Queue(QUEUE_NAME1, true);
    }

    @Bean
    public Queue routingQueue2() {
        return new Queue(QUEUE_NAME2, true);
    }

    @Bean
    public DirectExchange routingExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding routingBinding1() {
        return BindingBuilder.bind(routingQueue1()).to(routingExchange()).with(ROUTING_KEY1);
    }

    @Bean
    public Binding routingBinding2() {
        return BindingBuilder.bind(routingQueue2()).to(routingExchange()).with(ROUTING_KEY2);
    }
}
