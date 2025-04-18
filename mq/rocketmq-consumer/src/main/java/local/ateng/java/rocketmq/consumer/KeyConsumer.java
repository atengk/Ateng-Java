package local.ateng.java.rocketmq.consumer;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@RocketMQMessageListener(topic = "key-topic", consumerGroup = "consumer-key-group")
@Component
public class KeyConsumer implements RocketMQListener<MessageExt> {

    @Override
    public void onMessage(MessageExt message) {
        String msgBody = new String(message.getBody());
        String keys = message.getKeys(); // ✅ 获取消息的 key
        String tags = message.getTags(); // 可选：获取 Tag

        System.out.println("收到Key消息: " + msgBody);
        System.out.println("Key: " + keys);
        System.out.println("Tag: " + tags);
        System.out.println("MsgId: " + message.getMsgId());
    }
}

