package io.github.atengk.config;

import io.github.atengk.interceptor.WebSocketAuthInterceptor;
import io.github.atengk.interceptor.WebSocketHeartbeatInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket + STOMP + RabbitMQ（Broker Relay 模式）核心配置
 *
 * <p>架构说明：
 * <ul>
 *     <li>使用 RabbitMQ STOMP Broker Relay 替代 Spring SimpleBroker</li>
 *     <li>支持多实例 Spring Boot 横向扩展</li>
 *     <li>消息完全由 MQ 承载，应用层无状态</li>
 * </ul>
 *
 * <p>通道职责划分：
 * <ul>
 *     <li>Application Destination：处理业务消息（/app/**）</li>
 *     <li>Broker Destination：由 RabbitMQ 分发（/topic、/queue）</li>
 * </ul>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketProperties webSocketProperties;
    private final WebSocketHeartbeatInterceptor webSocketHeartbeatInterceptor;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * 配置 STOMP 消息代理（RabbitMQ Broker Relay）
     *
     * <p>说明：
     * <ul>
     *     <li>不使用 enableSimpleBroker（单机、内存实现）</li>
     *     <li>Broker Relay 适用于生产环境与集群部署</li>
     *     <li>系统心跳由 Spring 与 RabbitMQ 之间维护</li>
     * </ul>
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(webSocketProperties.getBrokerRelay().getRelayHost())
                .setRelayPort(webSocketProperties.getBrokerRelay().getRelayPort())
                .setClientLogin(webSocketProperties.getBrokerRelay().getClientLogin())
                .setClientPasscode(webSocketProperties.getBrokerRelay().getClientPasscode())
                .setSystemLogin(webSocketProperties.getBrokerRelay().getSystemLogin())
                .setSystemPasscode(webSocketProperties.getBrokerRelay().getSystemPasscode())
                .setSystemHeartbeatSendInterval(
                        webSocketProperties.getBrokerRelay().getSystemHeartbeatSendInterval())
                .setSystemHeartbeatReceiveInterval(
                        webSocketProperties.getBrokerRelay().getSystemHeartbeatReceiveInterval());

        registry.setApplicationDestinationPrefixes(
                webSocketProperties.getApplicationDestinationPrefix());

        registry.setUserDestinationPrefix(
                webSocketProperties.getUserDestinationPrefix());
    }

    /**
     * 注册 WebSocket STOMP 端点
     *
     * <p>说明：
     * <ul>
     *     <li>支持 SockJS，兼容老旧浏览器</li>
     *     <li>allowedOriginPatterns 用于支持多域名部署</li>
     * </ul>
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint(webSocketProperties.getUrl())
                .setAllowedOriginPatterns(webSocketProperties.getAllowedOrigins())
                .withSockJS();
    }

    /**
     * 配置客户端 Inbound 通道拦截器
     *
     * <p>执行顺序（非常重要）：
     * <ol>
     *     <li>WebSocketHeartbeatInterceptor：心跳短路，不进入业务层</li>
     *     <li>WebSocketAuthInterceptor：CONNECT / SUBSCRIBE 鉴权</li>
     * </ol>
     *
     * <p>设计原则：
     * <ul>
     *     <li>所有治理逻辑前置在通道层</li>
     *     <li>Controller 只关心业务</li>
     * </ul>
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {

        registration.interceptors(
                webSocketHeartbeatInterceptor,
                webSocketAuthInterceptor
        );
    }

}
