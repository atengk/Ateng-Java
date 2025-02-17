package local.ateng.java.redis.config;

import local.ateng.java.redis.listener.RedisMessageListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * RedisConfig 类是一个 Spring 配置类，用于配置 Redis 的消息监听器容器。
 * 该配置类定义了 Redis 消息的订阅和处理机制，允许应用监听指定的 Redis 频道，接收并处理消息。
 */
@Configuration  // 注解标识此类是一个配置类，Spring 将在启动时加载该配置
public class RedisConfig {

    /**
     * 创建一个 RedisMessageListenerContainer，用于监听 Redis 频道中的消息。
     * 它会管理消息监听器的生命周期，确保可以接收来自 Redis 发布/订阅模式的消息。
     *
     * @param factory         Redis 连接工厂，用于创建 Redis 连接
     * @param messageListener 消息监听器，接收到消息时会执行处理逻辑
     * @param topic           订阅的 Redis 频道
     * @return RedisMessageListenerContainer 实例
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory factory,
                                                                       MessageListener messageListener,
                                                                       Topic topic) {
        // 创建一个 Redis 消息监听容器
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();

        // 设置 Redis 连接工厂，确保 Redis 连接的正确配置
        container.setConnectionFactory(factory);

        // 添加消息监听器和订阅的 Redis 频道
        container.addMessageListener(messageListener, topic);

        // 返回 Redis 消息监听容器
        return container;
    }

    /**
     * 创建一个 Topic 对象，用于指定订阅的 Redis 频道。
     * 此处使用的是 PatternTopic，表示订阅一个具体的频道名称。
     *
     * @return Topic 对象，表示一个 Redis 频道
     */
    @Bean
    public Topic topic() {
        // 返回一个新的 PatternTopic，表示订阅名为 "myChannel" 的 Redis 频道
        return new org.springframework.data.redis.listener.PatternTopic("myChannel");
    }

    /**
     * 创建一个消息监听器，用于接收来自 Redis 频道的消息。
     * 该监听器将处理接收到的消息，并触发相应的回调方法。
     *
     * @return MessageListener 实例，处理来自 Redis 的消息
     */
    @Bean
    public MessageListener messageListener() {
        // 使用 MessageListenerAdapter 封装 RedisMessageListener，确保消息传递和处理逻辑的适配
        return new MessageListenerAdapter(new RedisMessageListener());
    }

}
