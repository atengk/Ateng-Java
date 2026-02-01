package io.github.atengk.service;

import com.alibaba.fastjson2.JSONObject;
import io.github.atengk.config.WebSocketProperties;
import io.github.atengk.constants.WebSocketMqConstants;
import io.github.atengk.entity.WebSocketMessage;
import io.github.atengk.entity.WebSocketBroadcastMessage;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private static final String KEY_USERS = "ws:online:users";
    private static final String KEY_SESSION_USER = "ws:session:user";
    private static final String KEY_SESSION_NODE = "ws:session:node";
    private static final String KEY_NODE_SESSIONS = "ws:node:sessions:";
    private static final String KEY_HEARTBEAT_ZSET = "ws:heartbeat:zset:";

    private static final Map<String, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> USER_SESSION_MAP = new ConcurrentHashMap<>();

    private final WebSocketProperties webSocketProperties;
    private final WebSocketBizDispatcher bizDispatcher;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;

    private final String nodeId = NodeIdUtil.getNodeId();

    private static final AtomicBoolean SHUTTING_DOWN = new AtomicBoolean(false);

    @PreDestroy
    public void onShutdown() {
        SHUTTING_DOWN.set(true);
    }

    public boolean authenticate(String userId) {
        return userId != null && !userId.isBlank();
    }

    public void registerSession(String userId, WebSocketSession session) {
        String sessionId = session.getId();

        SESSION_MAP.put(sessionId, session);
        USER_SESSION_MAP.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

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
    }

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
                session.sendMessage(new TextMessage(JSONObject.toJSONString(msg)));
            }
        } catch (Exception e) {
            closeSession(session.getId(), CloseStatus.SERVER_ERROR);
        }
    }

    public void checkHeartbeatTimeout() {
        long now = System.currentTimeMillis();
        long timeoutMillis = webSocketProperties.getHeartbeatTimeout().toMillis();

        String heartbeatKey = KEY_HEARTBEAT_ZSET + nodeId;

        log.debug(
                "开始 WebSocket 心跳超时检测，nodeId={}, key={}, now={}, timeoutMillis={}",
                nodeId,
                heartbeatKey,
                now,
                timeoutMillis
        );

        Set<String> timeoutSessionIds = redisTemplate.opsForZSet()
                .rangeByScore(heartbeatKey, 0, now - timeoutMillis);

        if (timeoutSessionIds == null || timeoutSessionIds.isEmpty()) {
            log.debug("WebSocket 心跳检测完成，未发现超时 Session，nodeId={}", nodeId);
            return;
        }

        log.debug(
                "检测到 WebSocket 心跳超时 Session，nodeId={}, count={}, sessionIds={}",
                nodeId,
                timeoutSessionIds.size(),
                timeoutSessionIds
        );

        for (String sessionId : timeoutSessionIds) {
            log.debug(
                    "触发心跳超时 Session 关闭，nodeId={}, sessionId={}",
                    nodeId,
                    sessionId
            );
            closeSession(sessionId, CloseStatus.SESSION_NOT_RELIABLE);
        }

        log.debug(
                "WebSocket 心跳超时处理完成，nodeId={}, processedCount={}",
                nodeId,
                timeoutSessionIds.size()
        );
    }

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
            closeSession(sessionId, CloseStatus.SERVER_ERROR);
        }
    }

    public void sendToUser(String userId, String message) {
        Set<String> sessionIds = USER_SESSION_MAP.getOrDefault(userId, Collections.emptySet());
        for (String sessionId : Set.copyOf(sessionIds)) {
            sendToSession(sessionId, message);
        }
    }

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

    public void sendToUsers(Set<String> userIds, String message) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        sendToUsersLocal(userIds, message);

        WebSocketBroadcastMessage mqMessage = new WebSocketBroadcastMessage();
        mqMessage.setFromNode(nodeId);
        mqMessage.setPayload(message);
        mqMessage.setTargetUsers(userIds);

        rabbitTemplate.convertAndSend(
                WebSocketMqConstants.EXCHANGE_WS_BROADCAST,
                WebSocketMqConstants.ROUTING_KEY,
                mqMessage
        );
    }

    public void broadcast(String message) {
        broadcastLocal(message);
        rabbitTemplate.convertAndSend(
                WebSocketMqConstants.EXCHANGE_WS_BROADCAST,
                WebSocketMqConstants.ROUTING_KEY,
                new WebSocketBroadcastMessage(nodeId, message, null)
        );
    }

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

    public void kickUser(String userId) {
        Set<String> sessionIds = USER_SESSION_MAP.getOrDefault(userId, Collections.emptySet());
        for (String sessionId : Set.copyOf(sessionIds)) {
            closeSession(sessionId);
        }
    }

    public void closeSession(String sessionId) {
        closeSession(sessionId, CloseStatus.NORMAL);
    }

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

    public void handleBizMessage(WebSocketSession session, WebSocketMessage message) {
        bizDispatcher.dispatch(session, message.getCode(), message);
    }

    public Set<String> getOnlineUsers() {
        Set<String> users = redisTemplate.opsForSet().members(KEY_USERS);
        return users == null ? Collections.emptySet() : users;
    }

    public int getOnlineUserCount() {
        Long size = redisTemplate.opsForSet().size(KEY_USERS);
        return size == null ? 0 : size.intValue();
    }

    public int getOnlineSessionCount() {
        return SESSION_MAP.size();
    }

    public int getUserSessionCount(String userId) {
        Set<String> sessions = USER_SESSION_MAP.get(userId);
        return sessions == null ? 0 : sessions.size();
    }

    public Set<String> getNodeSessions(String nodeId) {
        Set<String> sessions = redisTemplate.opsForSet().members(KEY_NODE_SESSIONS + nodeId);
        return sessions == null ? Collections.emptySet() : sessions;
    }

}
