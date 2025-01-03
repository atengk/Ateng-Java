package local.ateng.java.rabbitmq.topic;

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
