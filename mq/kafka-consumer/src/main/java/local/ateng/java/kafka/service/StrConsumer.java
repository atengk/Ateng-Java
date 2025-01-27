package local.ateng.java.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 接收字符串数据
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-03
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class StrConsumer {

    // 消费单条消息
    @KafkaListener(topics = "ateng.kafka.str")
    public void listen(ConsumerRecord<String, String> record) {
        String key = record.key();
        String topic = record.topic();
        long timestamp = record.timestamp();
        String data = record.value();
        log.info("接受[str]消息, topic: {}, timestamp: {}, key: {}, data: {}", topic, timestamp, key, data);
    }

    // 批量消息消费
    @KafkaListener(topics = "ateng.kafka.str", containerFactory = "batchFactory")
    public void listenBatch(List<ConsumerRecord<String, String>> records) {
        log.info("批量消息消费，数据条数：{}", records.size());
        for (ConsumerRecord<String, String> record : records) {
            String key = record.key();
            String topic = record.topic();
            long timestamp = record.timestamp();
            String data = record.value();
            //log.info("接受[str]批量消息, topic: {}, timestamp: {}, key: {}, data: {}", topic, timestamp, key, data);
        }
    }

}
