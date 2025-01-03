package local.ateng.java.rabbitmq.p2p;

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
public class PointToPointTask {
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 5000)
    public void send() {
        String message = StrUtil.format("{}:Hello Rabbit", DateUtil.now());
        rabbitTemplate.convertAndSend(PointToPointConfig.EXCHANGE_NAME, PointToPointConfig.ROUTING_KEY, message);
        log.info("[简单队列模式] 发送消息：{}", message);
    }

}
