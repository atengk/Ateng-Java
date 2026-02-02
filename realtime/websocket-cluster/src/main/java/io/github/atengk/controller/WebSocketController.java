package io.github.atengk.controller;

import com.alibaba.fastjson2.JSONObject;
import io.github.atengk.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

/**
 * WebSocket 管理控制器
 *
 * <p>
 * 提供基于 HTTP 的 WebSocket 运维与管理接口，
 * 用于查询在线状态、主动推送消息、广播消息、
 * 以及强制断开用户或 Session 连接。
 * </p>
 *
 * <p>
 * 说明：
 * <ul>
 *     <li>仅用于服务端运维 / 管理场景</li>
 *     <li>不参与 WebSocket 实时通信流程</li>
 *     <li>所有操作最终委托给 {@link WebSocketService}</li>
 * </ul>
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Slf4j
@RestController
@RequestMapping("/websocket")
@RequiredArgsConstructor
public class WebSocketController {

    /**
     * WebSocket 核心服务
     */
    private final WebSocketService webSocketService;

    /**
     * 获取在线用户数量
     *
     * @return 在线用户数
     */
    @GetMapping("/online/count")
    public long getOnlineUserCount() {
        long count = webSocketService.getOnlineUserCount();
        log.debug("查询在线用户数量，count={}", count);
        return count;
    }

    /**
     * 获取当前节点在线 Session 数量
     *
     * @return Session 数量
     */
    @GetMapping("/online/session/count")
    public int getOnlineSessionCount() {
        int count = webSocketService.getOnlineSessionCount();
        log.debug("查询在线 Session 数量，count={}", count);
        return count;
    }

    /**
     * 获取在线用户列表
     *
     * @return 用户ID集合
     */
    @GetMapping("/online/users")
    public Set<String> getOnlineUsers() {
        Set<String> users = webSocketService.getOnlineUsers();
        log.debug("查询在线用户列表，size={}", users.size());
        return users;
    }

    /**
     * 向指定用户推送 WebSocket 消息
     *
     * @param userId 用户ID
     * @param body   请求体（message 字段）
     */
    @PostMapping("/send/user/{userId}")
    public void sendToUser(
            @PathVariable String userId,
            @RequestBody JSONObject body
    ) {
        String message = body.getString("message");

        log.info(
                "HTTP 推送 WebSocket 消息给用户，userId={}, message={}",
                userId,
                message
        );

        webSocketService.sendToUser(userId, message);
    }

    /**
     * 向多个用户群发 WebSocket 消息
     *
     * @param body 请求体（userIds + message）
     */
    @PostMapping("/send/users")
    public void sendToUsers(@RequestBody JSONObject body) {
        Set<String> userIds = new HashSet<>(
                body.getJSONArray("userIds").toJavaList(String.class)
        );
        String message = body.getString("message");

        log.info(
                "HTTP 群发 WebSocket 消息，userIds={}, message={}",
                userIds,
                message
        );

        webSocketService.sendToUsers(userIds, message);
    }

    /**
     * 向指定 Session 推送 WebSocket 消息
     *
     * @param sessionId Session ID
     * @param body      请求体（message 字段）
     */
    @PostMapping("/send/session/{sessionId}")
    public void sendToSession(
            @PathVariable String sessionId,
            @RequestBody JSONObject body
    ) {
        String message = body.getString("message");

        log.info(
                "HTTP 推送 WebSocket 消息给 Session，sessionId={}, message={}",
                sessionId,
                message
        );

        webSocketService.sendToSession(sessionId, message);
    }

    /**
     * 广播 WebSocket 消息（所有在线用户）
     *
     * @param body 请求体（message 字段）
     */
    @PostMapping("/broadcast")
    public void broadcast(@RequestBody JSONObject body) {
        String message = body.getString("message");

        log.info(
                "HTTP 广播 WebSocket 消息，message={}",
                message
        );

        webSocketService.broadcast(message);
    }

    /**
     * 强制踢指定用户下线（关闭其所有 Session）
     *
     * @param userId 用户ID
     */
    @PostMapping("/kick/user/{userId}")
    public void kickUser(@PathVariable String userId) {
        log.warn(
                "HTTP 踢用户下线，userId={}",
                userId
        );

        webSocketService.kickUser(userId);
    }

    /**
     * 强制踢指定 Session 下线
     *
     * @param sessionId Session ID
     */
    @PostMapping("/kick/session/{sessionId}")
    public void kickSession(@PathVariable String sessionId) {
        log.warn(
                "HTTP 踢 Session 下线，sessionId={}",
                sessionId
        );

        webSocketService.closeSession(sessionId);
    }

    /**
     * 手动触发 WebSocket 心跳超时检测
     *
     * <p>
     * 一般用于运维排查或调试场景，
     * 正常情况下由定时任务自动触发。
     * </p>
     */
    @PostMapping("/heartbeat/check")
    public void checkHeartbeat() {
        log.info("HTTP 手动触发 WebSocket 心跳超时检测");
        webSocketService.checkHeartbeatTimeout();
    }
}
