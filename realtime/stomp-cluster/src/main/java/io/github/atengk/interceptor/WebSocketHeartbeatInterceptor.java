package io.github.atengk.interceptor;

import io.github.atengk.config.WebSocketProperties;
import io.github.atengk.service.WebSocketSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

/**
 * WebSocket 心跳拦截器
 *
 * <p>
 * 用于拦截客户端发送的业务心跳消息，
 * 在不进入业务处理链路的前提下，
 * 刷新对应 WebSocket session 的心跳时间。
 * </p>
 *
 * <p>
 * 拦截规则：
 * <ul>
 *     <li>仅处理 STOMP SEND 帧</li>
 *     <li>仅命中配置的心跳 destination</li>
 * </ul>
 * </p>
 *
 * <p>
 * 设计原则：
 * <ul>
 *     <li>逻辑必须足够轻量</li>
 *     <li>不参与任何业务处理</li>
 *     <li>允许高频调用</li>
 * </ul>
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHeartbeatInterceptor implements ChannelInterceptor {

    private final WebSocketSessionService webSocketSessionService;
    private final WebSocketProperties webSocketProperties;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        StompCommand command = accessor.getCommand();
        if (!StompCommand.SEND.equals(command)) {
            return message;
        }

        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();

        if (ObjectUtils.isEmpty(destination)
                || ObjectUtils.isEmpty(sessionId)
                || ObjectUtils.isEmpty(webSocketProperties.getHeartbeatDestination())) {
            return message;
        }

        // 命中业务心跳 destination
        if (destination.equals(webSocketProperties.getHeartbeatDestination())) {
            webSocketSessionService.onHeartbeat(sessionId);
            log.debug("【WebSocket 心跳续期】sessionId={}", sessionId);

            // 心跳消息无需进入后续业务链路
            return null;
        }

        return message;
    }
}
