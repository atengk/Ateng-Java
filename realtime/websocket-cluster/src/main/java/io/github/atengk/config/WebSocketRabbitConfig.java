package io.github.atengk.config;

import io.github.atengk.constants.WebSocketMqConstants;
import io.github.atengk.util.NodeIdUtil;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WebSocket RabbitMQ 配置类
 *
 * <p>
 * 负责 WebSocket 集群场景下的 MQ 基础设施配置，
 * 用于实现跨节点 WebSocket 消息广播与定向投递。
 * </p>
 *
 * <p>
 * 设计说明：
 * <ul>
 *     <li>使用 DirectExchange + RoutingKey 进行广播</li>
 *     <li>每个节点拥有独立 Queue（通过 nodeId 区分）</li>
 *     <li>消息体统一使用 JSON 序列化</li>
 * </ul>
 * </p>
 *
 * <p>
 * 消息流向示意：
 * <pre>
 * WebSocketService
 *        ↓
 *   ws.exchange
 *        ↓
 *  ws.queue.{nodeId}
 *        ↓
 * 当前节点 WebSocketService 消费
 * </pre>
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Configuration
public class WebSocketRabbitConfig {

    /**
     * WebSocket 广播交换机
     *
     * <p>
     * 使用 DirectExchange，通过固定 routingKey
     * 将消息投递到所有绑定的节点队列。
     * </p>
     *
     * @return DirectExchange
     */
    @Bean
    public DirectExchange wsExchange() {
        return new DirectExchange(
                WebSocketMqConstants.EXCHANGE_WS_BROADCAST,
                true,
                false
        );
    }

    /**
     * WebSocket 广播队列（节点级）
     *
     * <p>
     * 每个 WebSocket 节点创建一个独立队列，
     * 队列名称中包含 nodeId，用于区分不同节点。
     * </p>
     *
     * <p>
     * 示例：
     * <pre>
     * ws.queue.node-1
     * ws.queue.node-2
     * </pre>
     * </p>
     *
     * @return Queue
     */
    @Bean
    public Queue wsBroadcastQueue() {
        return new Queue(
                WebSocketMqConstants.QUEUE_WS_BROADCAST + NodeIdUtil.getNodeId(),
                true
        );
    }

    /**
     * WebSocket 广播队列绑定关系
     *
     * <p>
     * 将当前节点的广播队列绑定到 WebSocket 广播交换机，
     * 使用统一的 routingKey 接收所有广播消息。
     * </p>
     *
     * @param wsBroadcastQueue 当前节点广播队列
     * @param wsExchange       WebSocket 广播交换机
     * @return Binding
     */
    @Bean
    public Binding wsBroadcastBinding(
            Queue wsBroadcastQueue,
            DirectExchange wsExchange
    ) {
        return BindingBuilder
                .bind(wsBroadcastQueue)
                .to(wsExchange)
                .with(WebSocketMqConstants.ROUTING_KEY);
    }

    /**
     * WebSocket MQ 消息 JSON 转换器
     *
     * <p>
     * 用于将 MQ 消息与 Java 对象之间进行 JSON 序列化 / 反序列化，
     * 并限制反序列化时允许的 Java 包路径，防止反序列化安全问题。
     * </p>
     *
     * <p>
     * 仅信任 {@code io.github.atengk.entity} 包下的消息实体，
     * 适用于 WebSocketBroadcastMessage 等内部消息模型。
     * </p>
     *
     * @return Jackson2JsonMessageConverter
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        Jackson2JsonMessageConverter converter =
                new Jackson2JsonMessageConverter();

        DefaultJackson2JavaTypeMapper typeMapper =
                new DefaultJackson2JavaTypeMapper();

        typeMapper.setTrustedPackages(
                "io.github.atengk.entity"
        );

        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

}
