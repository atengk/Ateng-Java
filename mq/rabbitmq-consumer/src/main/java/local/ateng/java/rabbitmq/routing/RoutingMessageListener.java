package local.ateng.java.rabbitmq.routing;

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
