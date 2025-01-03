package local.ateng.java.rabbitmq.routing;

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
public class RoutingTask {
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 5000)
    public void send1() {
        String message = StrUtil.format("k1:{}:Hello Rabbit", DateUtil.now());
        rabbitTemplate.convertAndSend(RoutingConfig.EXCHANGE_NAME, RoutingConfig.ROUTING_KEY1, message);
        log.info("[路由模式k1] 发送消息：{}", message);
    }

    @Scheduled(fixedRate = 5000)
    public void send2() {
        String message = StrUtil.format("k2:{}:Hello Rabbit", DateUtil.now());
        rabbitTemplate.convertAndSend(RoutingConfig.EXCHANGE_NAME, RoutingConfig.ROUTING_KEY2, message);
        log.info("[路由模式k2] 发送消息：{}", message);
    }

}
