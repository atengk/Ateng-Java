package local.ateng.java.websocket.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务类，用于管理WebSocket会话和消息发送。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-05
 */
@Component
@Slf4j
public class WebSocketService {

    // 存储 path -> (userId -> Set<WebSocketSession>)
    // 每个 path 下的每个 userId 可能有多个会话
    private final Map<String, Map<String, Set<WebSocketSession>>> sessionMap = new ConcurrentHashMap<>();

    /**
     * 添加 WebSocket 连接
     *
     * @param session WebSocket会话
     */
    public void addSession(WebSocketSession session) {
        // 从连接URL中获取参数
        String path = session.getUri().getPath();
        String token = getParamValueFromUrl(session.getUri().getQuery(), "token");
        String userId = getParamValueFromUrl(session.getUri().getQuery(), "userId");
        String sessionId = session.getId();
        // 校验token是否有效
        if (!isValidToken(token)) {
            // 如果token无效，则发送消息给客户端，并关闭连接
            try {
                session.sendMessage(new TextMessage("token校验不通过"));
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            log.error("WebSocket: token校验不通过, path: {}, userId: {}", path, userId);
            return;
        }
        // 获取路径下的所有用户会话
        sessionMap
                .computeIfAbsent(path, k -> new ConcurrentHashMap<>()) // 获取指定路径的用户会话 Map
                .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()) // 获取指定用户的会话 Set
                .add(session); // 将 WebSocketSession 添加到 Set 中
        // 打印连接信息日志
        logSessionInfo(path);
    }

    /**
     * 校验token是否有效的方法。
     *
     * @param token 要校验的token
     * @return 校验结果，true表示有效，false表示无效
     */
    private boolean isValidToken(String token) {
        // 实际应用中应根据具体逻辑进行校验
        return "Admin@123".equals(token);
    }

    /**
     * 移除 WebSocket 连接
     *
     * @param session WebSocket会话
     */
    public void removeSession(WebSocketSession session) {
        // 从连接URL中获取参数
        String path = session.getUri().getPath();
        String token = getParamValueFromUrl(session.getUri().getQuery(), "token");
        String userId = getParamValueFromUrl(session.getUri().getQuery(), "userId");
        String sessionId = session.getId();
        Map<String, Set<WebSocketSession>> userSessionsMap = sessionMap.get(path);
        if (userSessionsMap != null) {
            Set<WebSocketSession> userSessions = userSessionsMap.get(userId);
            if (userSessions != null) {
                userSessions.removeIf(userSession -> userSession.getId().equals(sessionId));
                if (userSessions.isEmpty()) {
                    userSessionsMap.remove(userId); // 如果用户没有会话，移除该用户
                }
            }
            if (userSessionsMap.isEmpty()) {
                sessionMap.remove(path); // 如果路径下没有会话，移除路径
            }
        }
        // 打印连接信息日志
        logSessionInfo(path);
    }

    /**
     * 获取指定路径和用户的所有 WebSocket 会话
     *
     * @param path   路径
     * @param userId 用户ID
     * @return 用户的所有 WebSocket 会话
     */
    public Set<WebSocketSession> getSessionsByPathAndUserId(String path, String userId) {
        Map<String, Set<WebSocketSession>> userSessionsMap = sessionMap.get(path);
        return userSessionsMap != null ? userSessionsMap.getOrDefault(userId, Set.of()) : Set.of();
    }

    /**
     * 获取指定路径和多个用户列表的所有 WebSocket 会话
     *
     * @param path    路径
     * @param userIds 用户ID列表
     * @return 指定路径下多个用户的 WebSocket 会话集合
     */
    public Set<WebSocketSession> getSessionsByPathAndUserIds(String path, Set<String> userIds) {
        Set<WebSocketSession> allSessions = ConcurrentHashMap.newKeySet();
        Map<String, Set<WebSocketSession>> userSessionsMap = sessionMap.get(path);
        if (userSessionsMap != null) {
            for (String userId : userIds) {
                Set<WebSocketSession> userSessions = userSessionsMap.get(userId);
                if (userSessions != null) {
                    allSessions.addAll(userSessions); // 收集多个用户的会话
                }
            }
        }
        return allSessions;
    }

    /**
     * 获取指定路径下的所有 WebSocket 会话（适用于广播）
     *
     * @param path 路径
     * @return 路径下的所有 WebSocket 会话
     */
    public Set<WebSocketSession> getSessionsByPath(String path) {
        Map<String, Set<WebSocketSession>> userSessionsMap = sessionMap.get(path);
        Set<WebSocketSession> allSessions = ConcurrentHashMap.newKeySet();
        if (userSessionsMap != null) {
            for (Set<WebSocketSession> sessions : userSessionsMap.values()) {
                allSessions.addAll(sessions);
            }
        }
        return allSessions;
    }

    /**
     * 获取所有路径下的所有 WebSocket 会话
     *
     * @return 所有路径的 WebSocket 会话集合
     */
    public Set<WebSocketSession> getAllSessions() {
        Set<WebSocketSession> allSessions = ConcurrentHashMap.newKeySet();
        for (Map<String, Set<WebSocketSession>> userSessionsMap : sessionMap.values()) {
            for (Set<WebSocketSession> sessions : userSessionsMap.values()) {
                allSessions.addAll(sessions);
            }
        }
        return allSessions;
    }

    /**
     * 统计指定路径下的 WebSocket 连接信息
     * 返回一个包含路径下统计信息的 Map
     *
     * @param path 路径
     * @return 连接信息 Map
     */
    public Map<String, Object> getConnectionStatsByPath(String path) {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        Map<String, Set<WebSocketSession>> userSessionsMap = sessionMap.get(path);

        int totalConnections = 0;  // 路径下的总连接数
        int totalUsers = 0;        // 路径下的用户数
        Map<String, Integer> userConnectionCounts = new ConcurrentHashMap<>(); // 用户连接数

        if (userSessionsMap != null) {
            // 获取路径下用户数
            totalUsers = userSessionsMap.size();

            // 直接遍历每个用户的会话，更新连接数
            for (Map.Entry<String, Set<WebSocketSession>> entry : userSessionsMap.entrySet()) {
                String userId = entry.getKey();
                Set<WebSocketSession> sessions = entry.getValue();
                int userSessionCount = sessions.size(); // 获取该用户的会话数量
                totalConnections += userSessionCount; // 累加总连接数
                userConnectionCounts.put(userId, userSessionCount); // 记录每个用户的连接数
            }
        }

        // 将统计信息放入 Map 中返回
        stats.put("totalConnections", totalConnections);
        stats.put("totalUsers", totalUsers);
        stats.put("userConnectionCounts", userConnectionCounts);

        return stats;
    }


    /**
     * 记录会话信息的方法。
     */
    public void logSessionInfo(String path) {
        Map<String, Object> stats = getConnectionStatsByPath(path);
        log.info("连接路径: {}, 总连接数: {}, 用户连接数: {}, 连接信息详情: {}", path, stats.get("totalConnections"), stats.get("totalUsers"), stats.get("userConnectionCounts"));
    }

    /**
     * 发送消息给所有连接的客户端（广播）
     *
     * @param path    请求路径
     * @param message 要发送的消息
     */
    public void sendToAll(String path, String message) {
        Set<WebSocketSession> sessionSet = getSessionsByPath(path);
        for (WebSocketSession session : sessionSet) {
            sendMessageIfSessionOpen(session, message);
        }
    }

    /**
     * 发送消息给群组（群发）
     *
     * @param path    请求路径
     * @param userIds 用户组ID
     * @param message 要发送的消息
     */
    public void sendToGroup(String path, Set<String> userIds, String message) {
        Set<WebSocketSession> sessionSet = getSessionsByPathAndUserIds(path, userIds);
        for (WebSocketSession session : sessionSet) {
            sendMessageIfSessionOpen(session, message);
        }
    }

    /**
     * 发送消息给指定用户（点对点）
     *
     * @param path    请求路径
     * @param userId  用户ID
     * @param message 要发送的消息
     */
    public void sendToUser(String path, String userId, String message) {
        Set<WebSocketSession> sessionSet = getSessionsByPathAndUserId(path, userId);
        for (WebSocketSession session : sessionSet) {
            sendMessageIfSessionOpen(session, message);
        }
    }

    /**
     * 如果会话是打开状态，则向会话发送消息。
     *
     * @param session WebSocket会话
     * @param message 要发送的消息
     */
    private void sendMessageIfSessionOpen(WebSocketSession session, String message) {
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("发送消息失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 从URL中获取参数值的方法。
     *
     * @param query     URL查询字符串
     * @param paramName 参数名称
     * @return 参数值，如果参数不存在则返回null
     */
    private String getParamValueFromUrl(String query, String paramName) {
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && paramName.equals(keyValue[0])) {
                    return keyValue[1];
                }
            }
        }
        return null;
    }
}
