//package local.ateng.java.cloud.service;
//
//import local.ateng.java.cloud.entity.MyUser;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cloud.stream.function.StreamBridge;
//import org.springframework.kafka.support.KafkaHeaders;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.support.MessageBuilder;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor(onConstructor = @__(@Autowired))
//public class KafkaProducerService {
//
//    private final StreamBridge streamBridge;
//
//    public void sendMessage(MyUser data) {
//        Message<MyUser> message = MessageBuilder.withPayload(data).build();
//        streamBridge.send("myUser-out-0", message);
//    }
//
//    public void sendMessage(String data) {
//        Message<String> message = MessageBuilder.withPayload(data).build();
//        streamBridge.send("myString-out-0", message);
//    }
//
//    public void sendMessageParam(MyUser data) {
//        String key = System.currentTimeMillis() + "";
//        Message<MyUser> message = MessageBuilder
//                .withPayload(data)
//                .setHeader(KafkaHeaders.KEY, key.getBytes()) // Kafka消息Key
//                .setHeader(KafkaHeaders.TIMESTAMP, System.currentTimeMillis()) // 时间戳
//                .setHeader(KafkaHeaders.PARTITION, 0) // 指定分区
//                .build();
//        streamBridge.send("myUser-out-0", message);
//        System.out.println("发送消息：" + message);
//    }
//
//    public void sendMessageBatch(MyUser data) {
//        Message<MyUser> message = MessageBuilder.withPayload(data).build();
//        streamBridge.send("myUserBatch-out-0", message);
//    }
//
//    public void sendMessageManual(MyUser data) {
//        Message<MyUser> message = MessageBuilder.withPayload(data).build();
//        streamBridge.send("myUserManual-out-0", message);
//    }
//
//    public void sendMessageOther(MyUser data) {
//        Message<MyUser> message = MessageBuilder.withPayload(data).build();
//        streamBridge.send("myUserOther-out-0", message);
//    }
//
//}
