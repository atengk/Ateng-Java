package local.ateng.java.rabbitmq.delayed;

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
