package local.ateng.java.rocketmq.consumer;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@RocketMQMessageListener(
        topic = "test-topic",
        consumerGroup = "consumer-group-tag",
        selectorExpression = "TagA"  // 只消费 TagA 的消息
)
@Component
public class TagConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        System.out.println("收到带TagA的消息: " + message);
    }
}

