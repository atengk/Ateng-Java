package local.ateng.java.rocketmq.consumer;

import local.ateng.java.rocketmq.entity.User;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@RocketMQMessageListener(topic = "user-topic", consumerGroup = "consumer-group-user")
@Component
public class ObjectConsumer implements RocketMQListener<User> {

    @Override
    public void onMessage(User user) {
        System.out.println("收到用户对象消息: id=" + user.getId() + ", name=" + user.getName());
    }
}

