package io.github.atengk.config;

import io.github.atengk.handler.WebSocketHandler;
import io.github.atengk.interceptor.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置类
 *
 * <p>
 * 基于 Spring 原生 WebSocket 实现（非 STOMP 协议），
 * 负责注册 WebSocket Handler、鉴权拦截器以及跨域配置。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    /**
     * WebSocket 核心处理器
     */
    private final WebSocketHandler webSocketHandler;

    /**
     * WebSocket 握手鉴权拦截器
     */
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * WebSocket 配置属性
     */
    private final WebSocketProperties webSocketProperties;

    /**
     * 注册 WebSocket Handler
     *
     * <p>
     * 指定 WebSocket 访问路径、拦截器以及允许的跨域来源，
     * 所有配置均由 WebSocketProperties 统一管理。
     * </p>
     *
     * @param registry WebSocket Handler 注册器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry.addHandler(
                        webSocketHandler,
                        webSocketProperties.getEndpoint()
                )
                // 添加 WebSocket 握手阶段鉴权拦截器
                .addInterceptors(webSocketAuthInterceptor)
                // 设置允许的跨域来源
                .setAllowedOrigins(
                        webSocketProperties
                                .getAllowedOrigins()
                                .toArray(new String[0])
                );
    }
}
