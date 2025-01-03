package local.ateng.java.kafka.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 发送字符串数据
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-03
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class StrProducer {
    private final KafkaTemplate kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void send() {
        String topic = "ateng.kafka.str";
        String key = DateUtil.now();
        String data = JSONObject.of(
                "id", IdUtil.fastSimpleUUID(),
                "dateTime", DateUtil.date()
        ).toJSONString();
        kafkaTemplate.send(topic, key, data);
        log.info("发送[str]消息到Kafka, topic: {}, key: {}, data: {}", topic, key, data);
    }

}
