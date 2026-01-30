package io.github.atengk.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * WebSocket 配置属性绑定类
 *
 * <p>
 * 统一管理 WebSocket + STOMP 相关配置，包括：
 * <ul>
 *     <li>WebSocket 端点配置</li>
 *     <li>STOMP 应用级前缀</li>
 *     <li>用户级消息前缀</li>
 *     <li>心跳相关配置</li>
 *     <li>RabbitMQ STOMP Broker Relay 配置</li>
 * </ul>
 *
 * <p>
 * 该配置通过 {@code application.yml} 中 {@code websocket.*} 前缀进行绑定，
 * 作为整个 WebSocket 架构的统一参数入口。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Data
@Component
@ConfigurationProperties(prefix = "websocket")
public class WebSocketProperties {

    /**
     * WebSocket STOMP 端点地址
     * <p>示例：/ws</p>
     */
    @NotBlank
    private String url;

    /**
     * 允许跨域的来源
     * <p>支持通配符配置，如：*</p>
     */
    @NotBlank
    private String allowedOrigins;

    /**
     * 前端心跳发送间隔（毫秒）
     */
    @NotNull
    private Long heartbeatInterval;

    /**
     * STOMP 应用级消息前缀
     * <p>客户端向服务端发送消息时使用</p>
     * <p>示例：/app</p>
     */
    @NotBlank
    private String applicationDestinationPrefix;

    /**
     * STOMP 用户级消息前缀
     * <p>用于点对点私聊消息</p>
     * <p>示例：/user</p>
     */
    @NotBlank
    private String userDestinationPrefix;

    /**
     * 心跳消息发送目的地
     * <p>示例：/app/heartbeat</p>
     */
    @NotBlank
    private String heartbeatDestination;

    /**
     * RabbitMQ STOMP Broker Relay 配置
     */
    @Valid
    @NotNull
    private BrokerRelay brokerRelay;

    /**
     * RabbitMQ STOMP Broker Relay 配置项
     *
     * <p>
     * 用于配置 Spring 与 RabbitMQ 之间的 STOMP 协议通信参数，
     * 支持 Broker 集群、系统心跳检测等能力。
     * </p>
     *
     * @author 孔余
     * @since 2026-01-30
     */
    @Data
    public static class BrokerRelay {

        /**
         * RabbitMQ STOMP Relay 主机地址
         */
        @NotBlank
        private String relayHost;

        /**
         * RabbitMQ STOMP Relay 端口
         */
        @NotNull
        private Integer relayPort;

        /**
         * 客户端连接 Broker 使用的用户名
         */
        @NotBlank
        private String clientLogin;

        /**
         * 客户端连接 Broker 使用的密码
         */
        @NotBlank
        private String clientPasscode;

        /**
         * 系统级连接 Broker 使用的用户名
         */
        @NotBlank
        private String systemLogin;

        /**
         * 系统级连接 Broker 使用的密码
         */
        @NotBlank
        private String systemPasscode;

        /**
         * 系统向 Broker 发送心跳的时间间隔（毫秒）
         */
        @NotNull
        private Long systemHeartbeatSendInterval;

        /**
         * 系统从 Broker 接收心跳的时间间隔（毫秒）
         */
        @NotNull
        private Long systemHeartbeatReceiveInterval;
    }
}
