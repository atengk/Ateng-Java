package local.ateng.java.cloud.service;

import com.alibaba.cloud.stream.binder.rocketmq.constant.RocketMQConst;
import local.ateng.java.cloud.entity.MyUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RocketProducerService {

    private final StreamBridge streamBridge;

    public void sendMessage(String data) {
        Message<String> message = MessageBuilder.withPayload(data).build();
        streamBridge.send("myString-out-0", message);
    }

    public void sendMessage(MyUser data) {
        Message<MyUser> message = MessageBuilder.withPayload(data).build();
        streamBridge.send("myUser-out-0", message);
    }

    public void sendMessageParam(MyUser data) {
        String key = System.currentTimeMillis() + "";
        Message<MyUser> message = MessageBuilder
                .withPayload(data)
                .setHeader(RocketMQConst.PROPERTY_KEYS, key)
                .setHeader(RocketMQConst.PROPERTY_TAGS, "Ateng")
                .build();
        streamBridge.send("myUserParam-out-0", message);
        System.out.println("发送消息：" + message);
    }

}
