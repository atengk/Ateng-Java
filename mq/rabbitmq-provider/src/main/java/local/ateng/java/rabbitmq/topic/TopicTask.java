package local.ateng.java.rabbitmq.topic;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 发送消息
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-02
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class TopicTask {
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 5000)
    public void send1() {
        String message = StrUtil.format("k1:{}:Hello Rabbit", DateUtil.now());
        rabbitTemplate.convertAndSend(TopicConfig.EXCHANGE_NAME, TopicConfig.ROUTING_KEY1, message);
        log.info("[主题模式k1] 发送消息：{}", message);
    }

    @Scheduled(fixedRate = 5000)
    public void send2() {
        String message = StrUtil.format("k2:{}:Hello Rabbit", DateUtil.now());
        rabbitTemplate.convertAndSend(TopicConfig.EXCHANGE_NAME, "ateng.rabbit.topickey.topic.prod.t2", message);
        log.info("[主题模式k2] 发送消息：{}", message);
    }

    @Scheduled(fixedRate = 5000)
    public void send3() {
        String message = StrUtil.format("k3:{}:Hello Rabbit", DateUtil.now());
        rabbitTemplate.convertAndSend(TopicConfig.EXCHANGE_NAME, "ateng.rabbit.topickey.topic.prod.t3", message);
        log.info("[主题模式k3] 发送消息：{}", message);
    }

}
