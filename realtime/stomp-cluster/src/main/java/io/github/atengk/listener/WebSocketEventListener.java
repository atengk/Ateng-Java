package io.github.atengk.listener;

import io.github.atengk.service.WebSocketSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * WebSocket 连接生命周期事件监听器
 *
 * <p>
 * 负责监听 STOMP 连接 / 断开事件，
 * 并将会话生命周期交由 {@link WebSocketSessionService} 统一管理。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final WebSocketSessionService sessionService;

    /**
     * STOMP CONNECT 事件
     *
     * <p>
     * 触发时机：
     * - 客户端 CONNECT 帧通过鉴权
     * - {@link io.github.atengk.interceptor.WebSocketAuthInterceptor}
     * 已完成 Principal 绑定
     * </p>
     */
    @EventListener
    public void handleConnect(SessionConnectEvent event) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal principal = accessor.getUser();

        if (principal == null) {
            log.warn("【WebSocket CONNECT】未获取到 Principal，sessionId={}", sessionId);
            return;
        }

        String userId = principal.getName();

        sessionService.onConnect(userId, sessionId);

        log.info("【WebSocket CONNECT】userId={}, sessionId={}", userId, sessionId);
    }

    /**
     * STOMP DISCONNECT 事件
     *
     * <p>
     * 说明：
     * - 该事件不一定可靠（浏览器异常断开可能不会触发）
     * - 实际连接治理以 Redis 心跳清理为准
     * </p>
     */
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (sessionId == null) {
            return;
        }

        sessionService.onDisconnect(sessionId);

        log.info("【WebSocket DISCONNECT】sessionId={}", sessionId);
    }
}
