package io.github.atengk.constants;

/**
 * WebSocket MQ 相关常量定义
 *
 * <p>
 * 统一管理 WebSocket 集群模式下使用的
 * RabbitMQ 交换机、队列及路由键名称，
 * 避免在代码中出现硬编码字符串。
 * </p>
 *
 * <p>
 * 使用约定：
 * <ul>
 *     <li>交换机固定为单一广播交换机</li>
 *     <li>队列名称按节点维度动态拼接 nodeId</li>
 *     <li>所有消息使用统一 routingKey</li>
 * </ul>
 * </p>
 *
 * <p>
 * 示例：
 * <pre>
 * Exchange : ws.exchange
 * Queue    : ws.queue.{nodeId}
 * Routing  : ws.broadcast
 * </pre>
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
public final class WebSocketMqConstants {

    /**
     * WebSocket 广播交换机名称
     *
     * <p>
     * 用于 WebSocket 集群间消息广播，
     * 所有节点队列均绑定到该交换机。
     * </p>
     */
    public static final String EXCHANGE_WS_BROADCAST = "ws.exchange";

    /**
     * WebSocket 广播队列名称前缀
     *
     * <p>
     * 实际队列名称由该前缀与节点标识 nodeId 拼接而成，
     * 每个节点拥有独立的消息队列。
     * </p>
     *
     * <pre>
     * ws.queue.node-1
     * ws.queue.node-2
     * </pre>
     */
    public static final String QUEUE_WS_BROADCAST = "ws.queue.";

    /**
     * WebSocket 广播路由键
     *
     * <p>
     * 所有 WebSocket 广播消息均使用该 routingKey，
     * 简化路由规则，避免复杂绑定。
     * </p>
     */
    public static final String ROUTING_KEY = "ws.broadcast";

    /**
     * 私有构造方法，防止实例化
     */
    private WebSocketMqConstants() {
    }
}
