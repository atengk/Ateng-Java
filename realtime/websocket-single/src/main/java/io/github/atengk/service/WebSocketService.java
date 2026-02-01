package io.github.atengk.service;

import com.alibaba.fastjson2.JSONObject;
import io.github.atengk.config.WebSocketProperties;
import io.github.atengk.entity.WebSocketMessage;
import io.github.atengk.enums.WebSocketMessageType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 核心服务
 *
 * <p>
 * 负责 WebSocket 会话的统一管理，包括：
 * </p>
 * <ul>
 *     <li>Session 与用户关系维护</li>
 *     <li>心跳检测与连接清理</li>
 *     <li>消息推送（单播 / 多播 / 广播）</li>
 *     <li>用户踢下线与重复登录控制</li>
 *     <li>业务消息分发</li>
 * </ul>
 *
 * <p>
 * 该实现基于单机内存模型，适用于单实例部署场景
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
     * SessionId -> WebSocketSession 映射
     *
     * <p>
     * 用于根据 SessionId 精确操作 WebSocket 连接
     * </p>
     */
    private static final Map<String, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 用户ID -> SessionId 集合 映射
     *
     * <p>
     * 支持同一用户多端同时在线
     * </p>
     */
    private static final Map<String, Set<String>> USER_SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * SessionId -> 连接信息 映射
     *
     * <p>
     * 记录连接建立时间、最近心跳时间等运行时信息
     * </p>
     */
    private static final Map<String, ConnectionInfo> CONNECTION_INFO_MAP = new ConcurrentHashMap<>();

    /**
     * WebSocket 配置属性
     */
    private final WebSocketProperties webSocketProperties;

    /**
     * WebSocket 业务消息分发器
     */
    private final WebSocketBizDispatcher bizDispatcher;

    /**
     * 注册新的 WebSocket 会话
     *
     * @param userId  用户ID
     * @param session WebSocket 会话
     */
    public void registerSession(String userId, WebSocketSession session) {
        SESSION_MAP.put(session.getId(), session);

        USER_SESSION_MAP
                .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(session.getId());

        ConnectionInfo info = new ConnectionInfo(
                userId,
                session.getId(),
                session.getRemoteAddress() != null
                        ? session.getRemoteAddress().toString()
                        : "未知",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        CONNECTION_INFO_MAP.put(session.getId(), info);

        log.info(
                "WebSocket 用户连接成功，用户ID：{}，SessionID：{}",
                userId,
                session.getId()
        );
    }

    /**
     * 移除 WebSocket 会话并清理相关映射关系
     *
     * @param session WebSocket 会话
     */
    public void removeSession(WebSocketSession session) {
        if (session == null) {
            return;
        }

        ConnectionInfo info = CONNECTION_INFO_MAP.remove(session.getId());
        SESSION_MAP.remove(session.getId());

        if (info != null) {
            String userId = info.getUserId();
            Set<String> sessions = USER_SESSION_MAP.get(userId);
            if (sessions != null) {
                sessions.remove(session.getId());
                if (sessions.isEmpty()) {
                    USER_SESSION_MAP.remove(userId);
                }
            }

            log.info(
                    "WebSocket 用户断开连接，用户ID：{}，SessionID：{}",
                    userId,
                    session.getId()
            );
        } else {
            log.info("WebSocket Session 断开连接，SessionID：{}", session.getId());
        }
    }

    /**
     * WebSocket 鉴权校验
     *
     * @param userId 用户ID
     * @return 是否鉴权通过
     */
    public boolean authenticate(String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn("WebSocket 鉴权失败，用户ID为空");
            return false;
        }
        log.info("WebSocket 鉴权通过，用户ID：{}", userId);
        return true;
    }

    /**
     * 处理心跳消息
     *
     * <p>
     * 刷新当前 Session 的最近心跳时间
     * </p>
     *
     * @param session WebSocket 会话
     */
    public void handleHeartbeat(WebSocketSession session) {
        ConnectionInfo info = CONNECTION_INFO_MAP.get(session.getId());
        if (info == null) {
            log.debug(
                    "收到心跳但未找到连接信息，SessionID：{}",
                    session.getId()
            );
            return;
        }

        info.refreshHeartbeat();

        log.debug(
                "收到 WebSocket 心跳，SessionID：{}，更新时间：{}",
                session.getId(),
                info.getLastHeartbeatTime()
        );

        // 返回心跳响应
        try {
            if (session.isOpen()) {
                WebSocketMessage webSocketMessage = new WebSocketMessage();
                webSocketMessage.setType(WebSocketMessageType.HEARTBEAT_ACK.getCode());
                String message = JSONObject.toJSONString(webSocketMessage);
                session.sendMessage(new TextMessage(message));
            }
        } catch (Exception e) {
            log.warn(
                    "发送心跳响应失败，SessionID：{}",
                    session.getId(),
                    e
            );
        }
    }

    /**
     * 检测心跳超时的连接并主动关闭
     */
    public void checkHeartbeatTimeout() {
        LocalDateTime now = LocalDateTime.now();

        CONNECTION_INFO_MAP.values().forEach(info -> {
            if (Duration.between(info.getLastHeartbeatTime(), now)
                    .compareTo(webSocketProperties.getHeartbeatTimeout()) > 0) {
                closeSession(info.getSessionId(), CloseStatus.SESSION_NOT_RELIABLE);
            }
        });
    }

    /**
     * 踢指定用户下线
     *
     * @param userId 用户ID
     * @param reason 踢下线原因
     */
    public void kickUser(String userId, String reason) {
        Set<String> sessionIds = USER_SESSION_MAP.get(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }

        for (String sessionId : Set.copyOf(sessionIds)) {
            sendToSession(sessionId, reason);
            closeSession(sessionId, CloseStatus.NORMAL);
        }
    }

    /**
     * 向指定 Session 发送消息
     *
     * @param sessionId SessionID
     * @param message   消息内容
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
            log.error("WebSocket 消息发送异常，SessionID：{}", sessionId, e);
            closeSession(sessionId, CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * 向指定用户发送消息（多端同时接收）
     *
     * @param userId  用户ID
     * @param message 消息内容
     */
    public void sendToUser(String userId, String message) {
        Set<String> sessionIds = USER_SESSION_MAP.getOrDefault(userId, Collections.emptySet());
        for (String sessionId : Set.copyOf(sessionIds)) {
            sendToSession(sessionId, message);
        }
    }

    /**
     * 向多个用户发送消息
     *
     * @param userIds 用户ID集合
     * @param message 消息内容
     */
    public void sendToUsers(Set<String> userIds, String message) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (String userId : Set.copyOf(userIds)) {
            sendToUser(userId, message);
        }
    }

    /**
     * 向多个用户发送消息（排除指定用户）
     *
     * @param userIds        目标用户集合
     * @param excludeUserIds 排除的用户集合
     * @param message        消息内容
     */
    public void sendToUsersExclude(Set<String> userIds,
                                   Set<String> excludeUserIds,
                                   String message) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (String userId : Set.copyOf(userIds)) {
            if (excludeUserIds != null && excludeUserIds.contains(userId)) {
                continue;
            }
            sendToUser(userId, message);
        }
    }

    /**
     * 广播消息给所有在线 Session
     *
     * @param message 消息内容
     */
    public void broadcast(String message) {
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
     * 关闭指定 Session
     *
     * @param sessionId SessionID
     * @param status    关闭状态
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
        } catch (IOException e) {
            log.error("关闭 WebSocket Session 异常，SessionID：{}", sessionId, e);
        } finally {
            removeSession(session);
        }
    }

    /**
     * 获取当前在线用户数量
     *
     * @return 在线用户数
     */
    public int getOnlineUserCount() {
        return USER_SESSION_MAP.size();
    }

    /**
     * 获取所有在线连接信息
     *
     * @return 连接信息映射
     */
    public Map<String, ConnectionInfo> getAllConnectionInfo() {
        return Collections.unmodifiableMap(CONNECTION_INFO_MAP);
    }

    /**
     * 处理业务消息
     *
     * @param session WebSocket 会话
     * @param message 业务消息
     */
    public void handleBizMessage(WebSocketSession session, WebSocketMessage message) {
        boolean handled = bizDispatcher.dispatch(
                session,
                message.getCode(),
                message
        );

        if (!handled) {
            log.warn(
                    "未找到对应的 BizHandler，sessionId：{}，code：{}",
                    session.getId(),
                    message.getCode()
            );
        }
    }

    /**
     * 踢除指定用户的所有连接
     *
     * @param userId 用户ID
     */
    public void kickIfDuplicateLogin(String userId) {
        Set<String> sessionIds = USER_SESSION_MAP.get(userId);
        if (sessionIds == null || sessionIds.size() <= 1) {
            return;
        }

        for (String sessionId : Set.copyOf(sessionIds)) {
            closeSession(sessionId, CloseStatus.NORMAL);
        }
    }

    /**
     * 踢除指定用户除当前 Session 外的其他连接
     *
     * @param userId           用户ID
     * @param currentSessionId 当前 SessionID
     */
    public void kickIfDuplicateLogin(String userId, String currentSessionId) {
        Set<String> sessionIds = USER_SESSION_MAP.get(userId);
        if (sessionIds == null || sessionIds.size() <= 1) {
            return;
        }

        for (String sessionId : Set.copyOf(sessionIds)) {
            if (!sessionId.equals(currentSessionId)) {
                closeSession(sessionId, CloseStatus.NORMAL);
            }
        }
    }

    /**
     * WebSocket 连接信息
     */
    @Getter
    public static class ConnectionInfo {

        /**
         * 用户ID
         */
        private final String userId;

        /**
         * SessionID
         */
        private final String sessionId;

        /**
         * 客户端地址
         */
        private final String clientAddress;

        /**
         * 连接建立时间
         */
        private final LocalDateTime connectTime;

        /**
         * 最近一次心跳时间
         */
        private volatile LocalDateTime lastHeartbeatTime;

        public ConnectionInfo(String userId,
                              String sessionId,
                              String clientAddress,
                              LocalDateTime connectTime,
                              LocalDateTime lastHeartbeatTime) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.clientAddress = clientAddress;
            this.connectTime = connectTime;
            this.lastHeartbeatTime = lastHeartbeatTime;
        }

        /**
         * 刷新心跳时间
         */
        public void refreshHeartbeat() {
            this.lastHeartbeatTime = LocalDateTime.now();
        }
    }
}