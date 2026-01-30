package io.github.atengk.interceptor;

import io.github.atengk.entity.StompUserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * WebSocket 鉴权拦截器
 *
 * <p>功能说明：
 * <ul>
 *     <li>CONNECT：WebSocket 连接阶段鉴权，并绑定 Principal</li>
 *     <li>SUBSCRIBE：订阅阶段进行资源级鉴权（如群 topic 权限）</li>
 * </ul>
 *
 * <p>设计原则：
 * <ul>
 *     <li>所有安全逻辑前置在 ChannelInterceptor 层，Controller 无感知</li>
 *     <li>通过绑定 Principal，保证后续 SEND / SUBSCRIBE 身份不可伪造</li>
 *     <li>鉴权失败直接抛出 MessagingException，中断连接或订阅</li>
 * </ul>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    /**
     * 群 topic 前缀（RabbitMQ STOMP 规范）
     */
    private static final String GROUP_TOPIC_PREFIX = "/topic/group.";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        }

        return message;
    }

    /* ====================== CONNECT ====================== */

    /**
     * WebSocket CONNECT 阶段鉴权
     *
     * <p>处理逻辑：
     * <ul>
     *     <li>提取 userId / Authorization</li>
     *     <li>校验 token 合法性</li>
     *     <li>绑定 Principal，贯穿整个 WebSocket 会话生命周期</li>
     * </ul>
     */
    private void handleConnect(StompHeaderAccessor accessor) {

        String userId = extractRequiredHeader(accessor, "userId");
        String token = extractRequiredHeader(accessor, "Authorization");

        validateToken(userId, token);

        accessor.setUser(new StompUserPrincipal(userId));

        log.info("【WebSocket CONNECT】鉴权成功 userId={}", userId);
    }

    /**
     * Token 校验
     *
     * <p>说明：
     * <ul>
     *     <li>当前为示例实现</li>
     *     <li>可替换为 JWT / OAuth2 / Redis / RPC 鉴权</li>
     * </ul>
     */
    private void validateToken(String userId, String token) {

        if (!"Bearer Admin@123".equals(token)) {
            log.warn("【WebSocket CONNECT】token 校验失败 userId={}, token={}", userId, token);
            throw new MessagingException("WebSocket 鉴权失败");
        }
    }

    /* ====================== SUBSCRIBE ====================== */

    /**
     * WebSocket SUBSCRIBE 阶段鉴权
     *
     * <p>仅对群 topic 进行权限校验，防止非法监听敏感消息
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {

        String destination = accessor.getDestination();
        Principal principal = accessor.getUser();

        if (destination == null || principal == null) {
            return;
        }

        if (!destination.startsWith(GROUP_TOPIC_PREFIX)) {
            return;
        }

        String groupId = destination.substring(GROUP_TOPIC_PREFIX.length());
        String userId = principal.getName();

        if (!isGroupMember(userId, groupId)) {
            log.warn("【WebSocket SUBSCRIBE】无权订阅 groupId={} userId={}", groupId, userId);
            throw new MessagingException("无权订阅该群");
        }

        log.info("【WebSocket SUBSCRIBE】订阅成功 groupId={} userId={}", groupId, userId);
    }

    /* ====================== Utils ====================== */

    /**
     * 提取必传 Header
     */
    private String extractRequiredHeader(StompHeaderAccessor accessor, String headerName) {

        String value = accessor.getFirstNativeHeader(headerName);
        if (value == null || value.isEmpty()) {
            log.warn("【WebSocket CONNECT】缺少必要 Header：{}", headerName);
            throw new MessagingException("WebSocket 缺少必要参数：" + headerName);
        }
        return value;
    }

    /**
     * 群成员校验
     *
     * <p>示例实现，实际应由：
     * <ul>
     *     <li>数据库</li>
     *     <li>Redis</li>
     *     <li>远程服务</li>
     * </ul>
     * 提供
     */
    private boolean isGroupMember(String userId, String groupId) {

        if ("group-001".equals(groupId)) {
            return "10001".equals(userId) || "10002".equals(userId);
        }

        return false;
    }
}
