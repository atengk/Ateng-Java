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
