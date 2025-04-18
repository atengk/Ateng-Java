package local.ateng.java.rocketmq.controller;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mq")
public class RocketMqProducerController {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    // 1. 同步发送普通消息
    @GetMapping("/sendSync")
    public String sendSync(@RequestParam String msg) {
        SendResult result = rocketMQTemplate.syncSend("test-topic", msg);
        return "同步发送成功: " + result.getSendStatus();
    }

    // 2. 异步发送消息
    @GetMapping("/sendAsync")
    public String sendAsync(@RequestParam String msg) {
        rocketMQTemplate.asyncSend("test-topic", msg, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                System.out.println("异步发送成功: " + sendResult.getMsgId());
            }

            @Override
            public void onException(Throwable throwable) {
                System.out.println("异步发送失败: " + throwable.getMessage());
            }
        });
        return "异步发送中，请看控制台日志";
    }

    // 3. 单向发送（不关心结果）
    @GetMapping("/sendOneWay")
    public String sendOneWay(@RequestParam String msg) {
        rocketMQTemplate.sendOneWay("test-topic", msg);
        return "单向发送成功（不保证送达）";
    }

    // 4. 发送带Tag的消息（例如 test-topic:tagA）
    @GetMapping("/sendWithTag")
    public String sendWithTag(@RequestParam String msg) {
        rocketMQTemplate.convertAndSend("test-topic:TagA", msg);
        return "带Tag发送成功";
    }

    // 5. 顺序消息发送（如订单步骤）
    @GetMapping("/sendOrderly")
    public String sendOrderly(@RequestParam String msg, @RequestParam Long orderId) {
        rocketMQTemplate.syncSendOrderly("order-topic", msg, String.valueOf(orderId));
        return "顺序消息发送成功";
    }

    // 6. 延迟消息发送（延迟级别是预定义的）
    @GetMapping("/sendDelay")
    public String sendDelay(@RequestParam String msg) {
        // 延迟级别：1 = 1s, 2 = 5s, 3 = 10s, 4 = 30s, 5 = 1m ...
        int delayLevel = 3; // 10秒后发送
        SendResult result = rocketMQTemplate.syncSend("delay-topic", MessageBuilder.withPayload(msg).build(), 3000, delayLevel);
        return "延迟消息发送成功: " + result.getSendStatus();
    }

    // 7. 发送对象消息（自动JSON序列化）
    @GetMapping("/sendObject")
    public String sendObject() {
        User user = new User(1L, "张三");
        rocketMQTemplate.convertAndSend("user-topic", user);
        return "对象消息发送成功";
    }

    // 8. 发送带 Key 的消息
    @GetMapping("/sendWithKey")
    public String sendWithKey(@RequestParam String msg, @RequestParam String key) throws MQBrokerException, RemotingException, InterruptedException, MQClientException {
        // 手动构建 RocketMQ 原生 Message 对象，指定 key
        org.apache.rocketmq.common.message.Message message = new org.apache.rocketmq.common.message.Message(
                "key-topic",         // topic
                "KeyTag",            // tag（可选）
                key,                 // key
                msg.getBytes()       // body
        );

        SendResult result = rocketMQTemplate.getProducer().send(message);
        return "发送带Key成功: " + result.getSendStatus() + ", key=" + key;
    }


    @GetMapping("/sendTx")
    public String sendTransactionMsg(@RequestParam String orderId) {
        String topic = "tx-topic";

        // 构造消息，带上订单ID作为头部参数
        org.springframework.messaging.Message<String> message = MessageBuilder
                .withPayload("订单创建消息: " + orderId)
                .setHeader("orderId", orderId)  // 设置业务键
                .build();

        // 发送事务消息
        rocketMQTemplate.sendMessageInTransaction(topic, message, null);
        return "事务消息已发送，订单ID: " + orderId;
    }

    // 内部使用的对象类（模拟一个业务对象）
    static class User {
        private Long id;
        private String name;

        public User() {
        }

        public User(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        // Getter 和 Setter 必须有（用于序列化）
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

