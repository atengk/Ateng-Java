//package local.ateng.java.cloud.service;
//
//import local.ateng.java.cloud.entity.MyUser;
//import org.springframework.context.annotation.Bean;
//import org.springframework.kafka.support.Acknowledgment;
//import org.springframework.kafka.support.KafkaHeaders;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.MessageHeaders;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Random;
//import java.util.function.Consumer;
//
//@Service
//public class KafkaConsumerService {
//
//    @Bean
//    public Consumer<Message<MyUser>> myUser() {
//        return message -> {
//            MessageHeaders headers = message.getHeaders();
//            MyUser payload = message.getPayload();
//            String topic = headers.get(KafkaHeaders.RECEIVED_TOPIC, String.class);
//            byte[] keyBytes = headers.get(KafkaHeaders.RECEIVED_KEY, byte[].class);
//            String key = (keyBytes != null) ? new String(keyBytes) : "null";
//            Long timestamp = headers.get(KafkaHeaders.RECEIVED_TIMESTAMP, Long.class);
//            Integer partition = headers.get(KafkaHeaders.RECEIVED_PARTITION, Integer.class);
//
//            System.out.println(String.format("接受消息: Topic=%s, Payload=%s, Key=%s, Timestamp=%s, Partition=%s",
//                    topic, payload, key, timestamp, partition));
//        };
//    }
//
//    @Bean
//    public Consumer<String> myString() {
//        return message -> {
//            System.out.println("接受消息: " + message);
//        };
//    }
//
//    @Bean
//    public Consumer<List<MyUser>> myUserBatch() {
//        return message -> {
//            System.out.println("批量接受消息: " + message.size());
//        };
//    }
//
//    @Bean
//    public Consumer<Message<MyUser>> myUserManual() {
//        return message -> {
//            MyUser payload = message.getPayload();
//            System.out.println("收到消息: " + payload);
//            Acknowledgment acknowledgment = message.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);
//
//            if (acknowledgment != null) {
//                System.out.println("检测到消息确认对象，开始处理...");
//                // 处理业务逻辑
//                // processUser(myUser);
//                if (new Random().nextInt(0, 2) == 0 || payload.getId() == 0) {
//                    System.err.println("处理消息时发生异常，抛出异常，重新消费消息");
//                    // 关键：抛出异常，防止偏移量提交
//                    throw new RuntimeException("模拟业务异常");
//                }
//
//                // 手动确认消息
//                acknowledgment.acknowledge();
//                System.out.println("消息已成功确认。");
//            } else {
//                System.err.println("未找到消息确认对象，无法确认消息。");
//            }
//        };
//    }
//
//    @Bean
//    public Consumer<String> myUserDql() {
//        return message -> {
//            System.out.println("接受死信队列的消息: " + message);
//        };
//    }
//
//    @Bean
//    public Consumer<MyUser> myUserOther() {
//        return message -> {
//            System.out.println("接受消息: " + message);
//        };
//    }
//
//}
//
