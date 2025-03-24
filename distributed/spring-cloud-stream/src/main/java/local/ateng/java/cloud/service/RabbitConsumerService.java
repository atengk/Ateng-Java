//package local.ateng.java.cloud.service;
//
//import com.rabbitmq.client.Channel;
//import local.ateng.java.cloud.entity.MyUser;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.amqp.support.AmqpHeaders;
//import org.springframework.context.annotation.Bean;
//import org.springframework.messaging.Message;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Random;
//import java.util.function.Consumer;
//
//@Service
//@Slf4j
//public class RabbitConsumerService {
//
//    @Bean
//    public Consumer<MyUser> myUser() {
//        return message -> {
//            if (message.getId() == 0) {
//                System.err.println("接受消息发送异常");
//                throw new RuntimeException("模拟业务异常");
//            }
//            System.out.println("接受消息: " + message);
//        };
//    }
//
//    @Bean
//    public Consumer<Message<MyUser>> myUserDelay() {
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
//    private static final int MAX_RETRIES = 3; // 最大重试次数
//
//    @Bean
//    public Consumer<Message<MyUser>> myUserManual() {
//        return message -> {
//            // 获取消息体
//            MyUser payload = message.getPayload();
//            log.info("接收到用户信息: {}", payload);
//
//            // 获取 RabbitMQ 的 Channel 和 DeliveryTag
//            Channel channel = message.getHeaders().get(AmqpHeaders.CHANNEL, Channel.class);
//            Long deliveryTag = message.getHeaders().get(AmqpHeaders.DELIVERY_TAG, Long.class);
//
//            if (channel != null && deliveryTag != null) {
//                try {
//                    // 处理业务逻辑
//                    // processUser(myUser);
//                    if (new Random().nextInt(0, 2) == 0 || payload.getId() == 0) {
//                        throw new RuntimeException("模拟业务异常");
//                    }
//
//                    // 手动确认消息
//                    channel.basicAck(deliveryTag, false);
//                    log.info("消息已成功确认 (ACK)");
//                } catch (Exception e) {
//                    log.error("处理消息失败，错误信息: {}", e.getMessage(), e);
//                    throw new RuntimeException(e);
//                    /*try {
//                        // - 拒绝并不重新投递
//                        //channel.basicNack(deliveryTag, false, false);
//                        // - 拒绝并重新投递
//                        channel.basicNack(deliveryTag, false, true);
//                        log.warn("消息处理失败，已重新投递");
//                    } catch (IOException ex) {
//                        log.error("消息拒绝失败，错误信息: {}", ex.getMessage(), ex);
//                        throw new RuntimeException(ex);
//                    }*/
//                }
//            } else {
//                log.error("消息头中缺少 Channel 或 DeliveryTag，无法进行手动确认");
//            }
//        };
//    }
//
//    @Bean
//    public Consumer<MyUser> myUserTwo() {
//        return message -> {
//            if (message.getId() == 0) {
//                System.err.println("接受消息发送异常");
//                throw new RuntimeException("模拟业务异常");
//            }
//            System.out.println("接受消息: " + message);
//        };
//    }
//
//    @Bean
//    public Consumer<MyUser> myUserTwoDlq() {
//        return message -> {
//            // 处理死信队列中的消息
//            System.out.println("接受死信队列消息: " + message);
//            // 这里可以添加具体的处理逻辑
//        };
//    }
//
//    @Bean
//    public Consumer<String> atengTopic() {
//        return message -> {
//            System.out.println("接受消息: " + message);
//        };
//    }
//
//}