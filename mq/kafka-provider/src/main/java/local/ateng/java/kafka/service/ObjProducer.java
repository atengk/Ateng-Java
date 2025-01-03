package local.ateng.java.kafka.service;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.kafka.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 发送实体对象数据
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-03
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ObjProducer {
    private final KafkaTemplate kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void send() {
        String topic = "ateng.kafka.user";
        User user = new User(IdUtil.fastSimpleUUID(), RandomUtil.randomString(5), LocalDateTimeUtil.now());
        String msg = JSONObject.toJSONString(user);
        kafkaTemplate.send(topic, msg);
        log.info("发送[obj]消息到Kafka, topic: {}, data: {}", topic, msg);
    }

}
