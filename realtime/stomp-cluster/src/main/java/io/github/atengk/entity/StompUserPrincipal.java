package io.github.atengk.entity;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.Objects;

/**
 * STOMP 用户身份实现
 *
 * <p>
 * 用于在 WebSocket / STOMP 通信过程中标识当前用户，
 * 通常由 CONNECT 阶段解析并绑定到会话。
 * </p>
 *
 * <p>
 * 该类可安全用于：
 * <ul>
 *     <li>Spring Messaging UserDestination</li>
 *     <li>在线用户映射</li>
 *     <li>分布式会话存储</li>
 * </ul>
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
public class StompUserPrincipal implements Principal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务用户 ID
     */
    private final String userId;

    public StompUserPrincipal(String userId) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
    }

    @Override
    public String getName() {
        return userId;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StompUserPrincipal)) {
            return false;
        }
        StompUserPrincipal that = (StompUserPrincipal) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "StompUserPrincipal{userId='" + userId + "'}";
    }
}
