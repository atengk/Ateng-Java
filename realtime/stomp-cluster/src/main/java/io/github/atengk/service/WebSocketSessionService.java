package io.github.atengk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * WebSocket 会话管理服务（Redis 企业级实现）
 *
 * <p>
 * 主要职责：
 * <ul>
 *   <li>维护 session 与用户的映射关系</li>
 *   <li>维护用户在线 / 离线状态（支持多端）</li>
 *   <li>基于心跳机制自动清理失效连接</li>
 *   <li>提供在线统计数据，支撑监控与运维</li>
 * </ul>
 * </p>
 *
 * <p>
 * 设计说明：
 * <ul>
 *   <li>使用 Set 支持用户多 session</li>
 *   <li>使用 ZSet 管理 session 心跳超时</li>
 *   <li>所有操作保证幂等性</li>
 * </ul>
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSessionService {

    private final RedisTemplate<String, String> redisTemplate;

    /* ======================= Redis Key ======================= */

    /** WebSocket 用户 -> session 集合 */
    private static final String KEY_USER_SESSIONS = "ws:user:sessions:";

    /** WebSocket session -> user 映射 */
    private static final String KEY_SESSION_USER = "ws:session:user:";

    /** WebSocket 在线用户集合 */
    private static final String KEY_ONLINE_USERS = "ws:stats:online:users";

    /** WebSocket session 心跳 ZSET */
    private static final String KEY_SESSION_HEARTBEAT = "ws:session:heartbeat:zset";

    /** 心跳超时时间（秒） */
    private static final long HEARTBEAT_TIMEOUT_SECONDS = 90;

    /* ======================= 连接管理 ======================= */

    /**
     * STOMP CONNECT 时调用
     *
     * @param userId    用户 ID
     * @param sessionId WebSocket sessionId
     */
    public void onConnect(String userId, String sessionId) {
        // 初始化心跳
        onHeartbeat(sessionId);

        redisTemplate.opsForSet()
                .add(KEY_USER_SESSIONS + userId, sessionId);

        redisTemplate.opsForValue()
                .set(KEY_SESSION_USER + sessionId, userId);

        redisTemplate.opsForSet()
                .add(KEY_ONLINE_USERS, userId);

        log.info("【WebSocket 连接建立】userId={}, sessionId={}", userId, sessionId);
    }

    /**
     * WebSocket 断连处理（幂等）
     *
     * @param sessionId WebSocket sessionId
     */
    public void onDisconnect(String sessionId) {
        // 移除心跳
        redisTemplate.opsForZSet()
                .remove(KEY_SESSION_HEARTBEAT, sessionId);

        String userId = redisTemplate.opsForValue()
                .get(KEY_SESSION_USER + sessionId);

        if (userId == null) {
            log.debug("【WebSocket 断连】session 已被清理，sessionId={}", sessionId);
            return;
        }

        redisTemplate.opsForSet()
                .remove(KEY_USER_SESSIONS + userId, sessionId);

        redisTemplate.delete(KEY_SESSION_USER + sessionId);

        Long remain = redisTemplate.opsForSet()
                .size(KEY_USER_SESSIONS + userId);

        if (remain == null || remain == 0) {
            redisTemplate.opsForSet()
                    .remove(KEY_ONLINE_USERS, userId);
            log.info("【WebSocket 用户离线】userId={}", userId);
        } else {
            log.info("【WebSocket 连接断开】userId={}, 剩余连接数={}", userId, remain);
        }
    }

    /* ======================= 心跳管理 ======================= */

    /**
     * 心跳续期
     *
     * @param sessionId WebSocket sessionId
     */
    public void onHeartbeat(String sessionId) {
        long now = Instant.now().getEpochSecond();

        redisTemplate.opsForZSet()
                .add(KEY_SESSION_HEARTBEAT, sessionId, now);

        log.debug("【WebSocket 心跳刷新】sessionId={}, time={}", sessionId, now);
    }

    /**
     * 清理心跳超时的 WebSocket session
     *
     * <p>
     * 该方法仅负责清理规则与 Redis 操作，
     * 由定时任务或其他触发方式调用。
     * </p>
     */
    public void cleanupExpiredSessions() {
        long expireBefore = Instant.now().getEpochSecond() - HEARTBEAT_TIMEOUT_SECONDS;

        Set<String> expiredSessions = redisTemplate.opsForZSet()
                .rangeByScore(KEY_SESSION_HEARTBEAT, 0, expireBefore);

        if (expiredSessions == null || expiredSessions.isEmpty()) {
            return;
        }

        for (String sessionId : expiredSessions) {
            log.warn("【WebSocket 心跳超时】清理 sessionId={}", sessionId);
            onDisconnect(sessionId);
        }

        log.info("【WebSocket 会话清理完成】清理失效 session 数={}", expiredSessions.size());
    }

    /* ======================= 在线统计 ======================= */

    /**
     * 获取当前 WebSocket 在线统计信息
     *
     * <p>
     * 本方法返回的是一份「系统在线状态快照」，主要用于：
     * <ul>
     *     <li>管理后台在线人数展示</li>
     *     <li>运维监控与容量评估</li>
     *     <li>排查异常连接或多端登录情况</li>
     * </ul>
     * </p>
     *
     * <p>
     * 返回 Map 中各字段含义如下：
     * <ul>
     *     <li>
     *         totalUsers：
     *         当前在线用户数（按用户维度去重）。
     *         <br/>
     *         只要某个用户至少存在一条有效 WebSocket 连接，即视为在线用户。
     *     </li>
     *     <li>
     *         totalConnections：
     *         当前在线的 WebSocket 连接总数。
     *         <br/>
     *         每一个 session 视为一条独立连接，支持多端同时在线。
     *     </li>
     *     <li>
     *         avgConnectionsPerUser：
     *         人均 WebSocket 连接数。
     *         <br/>
     *         计算公式：totalConnections / totalUsers，
     *         用于反映用户多端登录情况或是否存在异常连接。
     *     </li>
     *     <li>
     *         timestamp：
     *         统计数据生成时间的时间戳（秒级，Unix Epoch）。
     *         <br/>
     *         用于标识该统计快照的有效时间点，便于监控与前端判断数据是否过期。
     *     </li>
     * </ul>
     * </p>
     *
     * <p>
     * 说明：
     * <ul>
     *     <li>该统计为实时计算结果，不做缓存</li>
     *     <li>仅用于监控与展示，不参与业务逻辑判断</li>
     * </ul>
     * </p>
     *
     * @return 在线统计数据快照
     *
     * @author 孔余
     * @since 2026-01-30
     */
    public Map<String, Object> getOnlineStats() {
        Map<String, Object> stats = new HashMap<>();

        Set<String> users = redisTemplate.opsForSet()
                .members(KEY_ONLINE_USERS);

        int totalUsers = users == null ? 0 : users.size();
        int totalConnections = 0;

        if (users != null) {
            for (String userId : users) {
                Long count = redisTemplate.opsForSet()
                        .size(KEY_USER_SESSIONS + userId);
                totalConnections += count == null ? 0 : count.intValue();
            }
        }

        stats.put("totalUsers", totalUsers);
        stats.put("totalConnections", totalConnections);
        stats.put("avgConnectionsPerUser",
                totalUsers == 0 ? 0 : (double) totalConnections / totalUsers);
        stats.put("timestamp", Instant.now().getEpochSecond());

        return stats;
    }

}
