//package local.ateng.java.cloud.service;
//
//import local.ateng.java.cloud.entity.MyUser;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cloud.stream.function.StreamBridge;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.support.MessageBuilder;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor(onConstructor = @__(@Autowired))
//public class RabbitProducerService {
//
//    private final StreamBridge streamBridge;
//
//    public void sendMessage(MyUser data) {
//        Message<MyUser> message = MessageBuilder.withPayload(data).build();
//        streamBridge.send("myUser-out-0", message);
//    }
//
//    public void sendMessageBatch(MyUser data) {
//        Message<MyUser> message = MessageBuilder.withPayload(data).build();
//        streamBridge.send("myUserBatch-out-0", message);
//    }
//
//    public void sendMessageDelay(MyUser data) {
//        Message<MyUser> message = MessageBuilder.withPayload(data)
//                .setHeader("x-delay", 5000) // 设置延迟时间（单位：毫秒）
//                .build();
//        streamBridge.send("myUserDelay-out-0", message);
//    }
//
//    public void sendMessageManual(MyUser data) {
//        Message<MyUser> message = MessageBuilder.withPayload(data).build();
//        streamBridge.send("myUserManual-out-0", message);
//    }
//
//    public void sendMessageTwo(MyUser data) {
//        Message<MyUser> message = MessageBuilder.withPayload(data).build();
//        streamBridge.send("myUserTwo-out-0", message);
//    }
//
//    public void sendMessageAteng(String data) {
//        Message<String> message = MessageBuilder.withPayload(data).build();
//        streamBridge.send("atengTopic-out-0", message);
//    }
//
//}
