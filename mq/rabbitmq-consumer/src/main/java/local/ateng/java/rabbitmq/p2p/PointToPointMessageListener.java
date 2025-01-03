package local.ateng.java.rabbitmq.p2p;

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
