package local.ateng.java.redis.listener;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * RedisMessageListener 类用于监听来自 Redis 频道的消息。
 * 它实现了 Spring Data Redis 提供的 MessageListener 接口，
 * 并且作为一个 Spring Bean 被自动注册，可以用于接收发布/订阅（Pub/Sub）模式下的消息。
 * <p>
 * 该类将会监听一个指定的 Redis 频道，并处理接收到的消息。
 */
@Component  // 注解表明这是一个 Spring 管理的组件，将自动被注册为 Bean
public class RedisMessageListener implements MessageListener {

    /**
     * 当 Redis 频道接收到消息时，调用此方法。
     * 该方法由 Spring Data Redis 自动触发，当 Redis 发布消息到指定频道时会被调用。
     *
     * @param message Redis 消息对象，包含了消息的具体内容
     * @param pattern 可选的消息模式，通常为空，但如果使用了模式匹配订阅（如 PSUBSCRIBE），此参数表示匹配的模式
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 获取消息的内容，将其从字节数组转换为字符串
        String msg = new String(message.getBody());

        // 打印接收到的消息内容
        // 这里可以根据需求做进一步的消息处理，如日志记录、存储等
        System.out.println("Received message: " + msg);
    }
}
