package local.ateng.java.websocket.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * WebSocket服务类
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-05
 */
@Component
@Slf4j
public class WebSocketService {
    // 保存已连接的客户端信息的映射
    public static final Map<String, Set<StompHeaderAccessor>> sessionMap = new ConcurrentHashMap<>();

    /**
     * 获取所有的Session
     *
     * @return StompHeaderAccessor集合
     */
    public static Set<StompHeaderAccessor> getAllSessions() {
        return sessionMap.values().stream() // 获取 sessionMap 的所有值
                .flatMap(Set::stream) // 将每个 Set 转换为 Stream 并展平
                .collect(Collectors.toSet()); // 将结果收集到一个新的 Set 中
    }

    /**
     * 根据userId删除StompHeaderAccessor
     *
     * @param userId
     */
    public static void deleteSession(String userId, String sessionId) {
        Set<StompHeaderAccessor> accessors = sessionMap.get(userId);
        Optional<StompHeaderAccessor> optional = accessors.stream().filter(accessor -> sessionId.equals(accessor.getSessionId())).findFirst();
        if (optional.isPresent()) {
            accessors.remove(optional.get());
        }
    }

    /**
     * 处理WebSocket连接事件的方法。
     * 当有新的WebSocket连接建立时，将会话ID和用户ID添加到对应的映射中。
     *
     * @param event WebSocket连接事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String userId = accessor.getFirstNativeHeader("userId"); // 获取用户ID

        // 保存userId到Attribute
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        sessionAttributes.put("userId", userId);

        // 保存会话
        sessionMap
                .computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>())
                .add(accessor);
        // 打印连接信息日志
        logSessionInfo();
    }

    /**
     * 处理WebSocket断开连接事件的方法。
     * 当WebSocket连接断开时，从connectedClients和userSessionMap中移除对应的映射关系。
     *
     * @param event WebSocket断开连接事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        // 获取userId
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        String userId = (String) sessionAttributes.get("userId");

        // 删除会话
        deleteSession(userId, sessionId);
        // 打印连接信息日志
        logSessionInfo();
    }

    /**
     * 记录会话信息的方法。
     */
    public void logSessionInfo() {
        Map<String, Object> stats = getConnectionStats();
        log.info("总连接数: {}, 用户连接数: {}, 连接信息详情: {}", stats.get("totalConnections"), stats.get("totalUsers"), stats.get("userConnectionCounts"));
    }

    /**
     * 统计指定路径下的 WebSocket 连接信息
     * 返回一个包含路径下统计信息的 Map
     *
     * @return 连接信息 Map
     */
    public Map<String, Object> getConnectionStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();

        int totalConnections = 0;  // 总连接数
        int totalUsers = 0;        // 用户数
        Map<String, Integer> userConnectionCounts = new ConcurrentHashMap<>(); // 用户连接数详情

        if (sessionMap != null) {
            // 获取用户数
            totalUsers = sessionMap.size();

            // 直接遍历每个用户的会话，更新连接数
            for (Map.Entry<String, Set<StompHeaderAccessor>> entry : sessionMap.entrySet()) {
                String userId = entry.getKey();
                Set<StompHeaderAccessor> sessions = entry.getValue();
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

}
