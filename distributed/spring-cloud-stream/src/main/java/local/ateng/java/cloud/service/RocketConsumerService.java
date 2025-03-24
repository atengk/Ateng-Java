package local.ateng.java.cloud.service;

import com.alibaba.cloud.stream.binder.rocketmq.constant.RocketMQConst;
import local.ateng.java.cloud.entity.MyUser;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class RocketConsumerService {

    @Bean
    public Consumer<String> myString() {
        return message -> {
            System.out.println("接受消息: " + message);
        };
    }

    @Bean
    public Consumer<MyUser> myUser() {
        return message -> {
            System.out.println("接受消息: " + message);
        };
    }

    @Bean
    public Consumer<Message<MyUser>> myUserParam() {
        return message -> {
            MessageHeaders headers = message.getHeaders();
            MyUser payload = message.getPayload();
            String topic = headers.get(RocketMQConst.PROPERTY_REAL_TOPIC, String.class);
            String key = headers.get(RocketMQConst.PROPERTY_KEYS, String.class);
            String tags = headers.get(RocketMQConst.PROPERTY_TAGS, String.class);
            Long timestamp = headers.get(RocketMQConst.PROPERTY_BORN_TIMESTAMP, Long.class);
            System.out.println(String.format("接受消息: Topic=%s, Payload=%s, Key=%s, Tag=%s, Timestamp=%s",
                    topic, payload, key, tags, timestamp));
        };
    }

}
