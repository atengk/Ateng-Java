package io.github.atengk.service;

import com.alibaba.fastjson2.JSONObject;
import io.github.atengk.config.WebSocketProperties;
import io.github.atengk.constants.WebSocketMqConstants;
import io.github.atengk.entity.WebSocketBroadcastMessage;
import io.github.atengk.entity.WebSocketMessage;
import io.github.atengk.enums.WebSocketMessageType;
import io.github.atengk.util.NodeIdUtil;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket 核心服务类（支持集群）
 *
 * <p>
 * 负责 WebSocket Session 生命周期管理、心跳维护、
 * 本地消息投递以及跨节点消息广播。
 * </p>
 *
 * <p>
 * 设计原则：
 * <ul>
 *     <li>Session 仅存在于本地内存，不跨节点共享</li>
 *     <li>Redis 维护全局状态</li>
 *     <li>MQ 仅用于跨节点消息投递</li>
 * </ul>
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    /**
     * Redis Key：在线用户集合
     */
    private static final String KEY_USERS = "ws:online:users";

    /**
     * Redis Key：sessionId -> userId
     */
    private static final String KEY_SESSION_USER = "ws:session:user";

    /**
     * Redis Key：sessionId -> nodeId
     */
    private static final String KEY_SESSION_NODE = "ws:session:node";

    /**
     * Redis Key：nodeId -> sessions
     */
    private static final String KEY_NODE_SESSIONS = "ws:node:sessions:";

    /**
     * Redis Key：nodeId -> 心跳 ZSet
     */
    private static final String KEY_HEARTBEAT_ZSET = "ws:heartbeat:zset:";

    /**
     * 本地 Session 缓存（sessionId -> WebSocketSession）
     */
    private static final Map<String, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 本地用户 Session 映射（userId -> sessionId 集合）
     */
    private static final Map<String, Set<String>> USER_SESSION_MAP = new ConcurrentHashMap<>();

    private final WebSocketProperties webSocketProperties;
    private final WebSocketBizDispatcher bizDispatcher;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;

    /**
     * 当前节点标识
     */
    private final String nodeId = NodeIdUtil.getNodeId();

    /**
     * 应用是否正在关闭标识
     */
    private static final AtomicBoolean SHUTTING_DOWN = new AtomicBoolean(false);

    /**
     * 应用关闭前标记状态
     */
    @PreDestroy
    public void onShutdown() {
        SHUTTING_DOWN.set(true);
        log.info("WebSocketService 正在关闭，nodeId={}", nodeId);
    }

    /**
     * WebSocket 用户鉴权
     *
     * @param userId 用户ID
     * @return 是否通过鉴权
     */
    public boolean authenticate(String userId) {
        return userId != null && !userId.isBlank();
    }

    /**
     * 注册 WebSocket Session
     *
     * @param userId  用户ID
     * @param session WebSocket Session
     */
    public void registerSession(String userId, WebSocketSession session) {
        String sessionId = session.getId();

        log.info(
                "注册 WebSocket Session，nodeId={}, userId={}, sessionId={}",
                nodeId, userId, sessionId
        );

        SESSION_MAP.put(sessionId, session);
        USER_SESSION_MAP
                .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);

        redisTemplate.opsForSet().add(KEY_USERS, userId);
        redisTemplate.opsForHash().put(KEY_SESSION_USER, sessionId, userId);
        redisTemplate.opsForHash().put(KEY_SESSION_NODE, sessionId, nodeId);
        redisTemplate.opsForSet().add(KEY_NODE_SESSIONS + nodeId, sessionId);
        redisTemplate.opsForZSet().add(
                KEY_HEARTBEAT_ZSET + nodeId,
                sessionId,
                System.currentTimeMillis()
        );
    }

    /**
     * 移除 WebSocket Session
     *
     * @param session WebSocket Session
     */
    public void removeSession(WebSocketSession session) {
        if (session == null) {
            return;
        }

        String sessionId = session.getId();
        SESSION_MAP.remove(sessionId);

        if (SHUTTING_DOWN.get()) {
            USER_SESSION_MAP.values().forEach(set -> set.remove(sessionId));
            return;
        }

        Object userId = redisTemplate.opsForHash().get(KEY_SESSION_USER, sessionId);

        redisTemplate.opsForHash().delete(KEY_SESSION_USER, sessionId);
        redisTemplate.opsForHash().delete(KEY_SESSION_NODE, sessionId);
        redisTemplate.opsForSet().remove(KEY_NODE_SESSIONS + nodeId, sessionId);
        redisTemplate.opsForZSet().remove(KEY_HEARTBEAT_ZSET + nodeId, sessionId);

        if (userId != null) {
            Set<String> sessions = USER_SESSION_MAP.get(userId.toString());
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    USER_SESSION_MAP.remove(userId.toString());
                    redisTemplate.opsForSet().remove(KEY_USERS, userId.toString());
                }
            }
        }

        log.info(
                "移除 WebSocket Session，nodeId={}, sessionId={}, userId={}",
                nodeId, sessionId, userId
        );
    }

    /**
     * 处理心跳消息
     *
     * @param session WebSocket Session
     */
    public void handleHeartbeat(WebSocketSession session) {
        redisTemplate.opsForZSet().add(
                KEY_HEARTBEAT_ZSET + nodeId,
                session.getId(),
                System.currentTimeMillis()
        );

        try {
            if (session.isOpen()) {
                WebSocketMessage msg = new WebSocketMessage();
                msg.setType(WebSocketMessageType.HEARTBEAT_ACK.getCode());
                session.sendMessage(
                        new TextMessage(JSONObject.toJSONString(msg))
                );
            }
        } catch (Exception e) {
            log.warn(
                    "心跳响应失败，准备关闭 Session，sessionId={}",
                    session.getId(),
                    e
            );
            closeSession(session.getId(), CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * 检测心跳超时 Session
     */
    public void checkHeartbeatTimeout() {
        long now = System.currentTimeMillis();
        long timeoutMillis = webSocketProperties.getHeartbeatTimeout().toMillis();
        String heartbeatKey = KEY_HEARTBEAT_ZSET + nodeId;

        Set<String> timeoutSessionIds = redisTemplate.opsForZSet()
                .rangeByScore(heartbeatKey, 0, now - timeoutMillis);

        if (timeoutSessionIds == null || timeoutSessionIds.isEmpty()) {
            return;
        }

        log.warn(
                "检测到心跳超时 Session，nodeId={}, count={}",
                nodeId, timeoutSessionIds.size()
        );

        for (String sessionId : timeoutSessionIds) {
            closeSession(sessionId, CloseStatus.SESSION_NOT_RELIABLE);
        }
    }

    /**
     * 向指定 Session 发送消息
     */
    public void sendToSession(String sessionId, String message) {
        WebSocketSession session = SESSION_MAP.get(sessionId);
        if (session == null || !session.isOpen()) {
            if (session != null) {
                removeSession(session);
            }
            return;
        }

        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            log.warn(
                    "发送消息失败，sessionId={}",
                    sessionId,
                    e
            );
            closeSession(sessionId, CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * 向指定用户发送消息（本节点）
     */
    public void sendToUser(String userId, String message) {
        Set<String> sessionIds =
                USER_SESSION_MAP.getOrDefault(userId, Collections.emptySet());

        for (String sessionId : Set.copyOf(sessionIds)) {
            sendToSession(sessionId, message);
        }
    }

    /**
     * 本地向多个用户发送消息
     */
    private void sendToUsersLocal(Set<String> userIds, String message) {
        for (String userId : userIds) {
            Set<String> sessionIds = USER_SESSION_MAP.get(userId);
            if (sessionIds == null || sessionIds.isEmpty()) {
                continue;
            }
            for (String sessionId : Set.copyOf(sessionIds)) {
                sendToSession(sessionId, message);
            }
        }
    }

    /**
     * 向多个用户发送消息（集群）
     */
    public void sendToUsers(Set<String> userIds, String message) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        sendToUsersLocal(userIds, message);

        rabbitTemplate.convertAndSend(
                WebSocketMqConstants.EXCHANGE_WS_BROADCAST,
                WebSocketMqConstants.ROUTING_KEY,
                new WebSocketBroadcastMessage(nodeId, message, userIds)
        );
    }

    /**
     * 广播消息（集群）
     */
    public void broadcast(String message) {
        broadcastLocal(message);

        rabbitTemplate.convertAndSend(
                WebSocketMqConstants.EXCHANGE_WS_BROADCAST,
                WebSocketMqConstants.ROUTING_KEY,
                new WebSocketBroadcastMessage(nodeId, message, null)
        );
    }

    /**
     * MQ 消息监听（跨节点广播）
     */
    @RabbitListener(queues = "#{wsBroadcastQueue.name}")
    public void onBroadcast(WebSocketBroadcastMessage message) {
        if (nodeId.equals(message.getFromNode())) {
            return;
        }

        if (message.getTargetUsers() == null || message.getTargetUsers().isEmpty()) {
            broadcastLocal(message.getPayload());
            return;
        }

        sendToUsersLocal(message.getTargetUsers(), message.getPayload());
    }

    /**
     * 本地广播消息
     */
    private void broadcastLocal(String message) {
        SESSION_MAP.values().forEach(session -> {
            if (!session.isOpen()) {
                removeSession(session);
                return;
            }
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                closeSession(session.getId(), CloseStatus.SERVER_ERROR);
            }
        });
    }

    /**
     * 踢用户下线
     */
    public void kickUser(String userId) {
        Set<String> sessionIds =
                USER_SESSION_MAP.getOrDefault(userId, Collections.emptySet());

        for (String sessionId : Set.copyOf(sessionIds)) {
            closeSession(sessionId);
        }
    }

    /**
     * 关闭 Session（默认状态）
     */
    public void closeSession(String sessionId) {
        closeSession(sessionId, CloseStatus.NORMAL);
    }

    /**
     * 关闭 Session
     */
    public void closeSession(String sessionId, CloseStatus status) {
        WebSocketSession session = SESSION_MAP.get(sessionId);
        if (session == null) {
            return;
        }

        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException ignored) {
        } finally {
            removeSession(session);
        }
    }

    /**
     * 分发业务消息
     */
    public void handleBizMessage(WebSocketSession session, WebSocketMessage message) {
        bizDispatcher.dispatch(session, message.getCode(), message);
    }

    /**
     * 获取在线用户列表
     */
    public Set<String> getOnlineUsers() {
        Set<String> users = redisTemplate.opsForSet().members(KEY_USERS);
        return users == null ? Collections.emptySet() : users;
    }

    /**
     * 获取在线用户数量
     */
    public int getOnlineUserCount() {
        Long size = redisTemplate.opsForSet().size(KEY_USERS);
        return size == null ? 0 : size.intValue();
    }

    /**
     * 获取当前节点 Session 数量
     */
    public int getOnlineSessionCount() {
        return SESSION_MAP.size();
    }

    /**
     * 获取用户 Session 数量
     */
    public int getUserSessionCount(String userId) {
        Set<String> sessions = USER_SESSION_MAP.get(userId);
        return sessions == null ? 0 : sessions.size();
    }

    /**
     * 获取指定节点的 Session 列表
     */
    public Set<String> getNodeSessions(String nodeId) {
        Set<String> sessions =
                redisTemplate.opsForSet().members(KEY_NODE_SESSIONS + nodeId);
        return sessions == null ? Collections.emptySet() : sessions;
    }

}
