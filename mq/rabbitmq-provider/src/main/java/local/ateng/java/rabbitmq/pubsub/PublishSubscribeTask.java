package local.ateng.java.rabbitmq.pubsub;

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
public class PublishSubscribeTask {
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 5000)
    public void send() {
        String message = StrUtil.format("{}:Hello Rabbit", DateUtil.now());
        rabbitTemplate.convertAndSend(PublishSubscribeConfig.EXCHANGE_NAME, null, message);
        log.info("[发布/订阅模式] 发送消息：{}", message);
    }

}
