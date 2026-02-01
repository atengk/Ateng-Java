package io.github.atengk.controller;

import io.github.atengk.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * WebSocket 管理控制器
 *
 * <p>
 * 提供基于 HTTP 的 WebSocket 管理与运维接口，
 * 用于查询连接状态、主动推送消息、踢用户下线等操作。
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
     * WebSocket 服务
     */
    private final WebSocketService webSocketService;

    /**
     * 获取当前在线用户数
     *
     * @return 在线用户数量
     */
    @GetMapping("/online/count")
    public int getOnlineCount() {
        return webSocketService.getOnlineUserCount();
    }

    /**
     * 获取当前所有在线连接信息
     *
     * @return SessionID 与连接信息的映射关系
     */
    @GetMapping("/connections")
    public Map<String, WebSocketService.ConnectionInfo> getConnections() {
        return webSocketService.getAllConnectionInfo();
    }

    /**
     * 向指定用户发送 WebSocket 消息
     *
     * <p>
     * 如果用户存在多个连接，将向该用户的所有 Session 推送消息。
     * </p>
     *
     * @param userId  用户ID
     * @param message 消息内容
     */
    @PostMapping("/send/user/{userId}")
    public void sendToUser(
            @PathVariable String userId,
            @RequestBody String message
    ) {
        log.info("HTTP 推送 WebSocket 消息给用户，userId：{}，message：{}", userId, message);
        webSocketService.sendToUser(userId, message);
    }

    /**
     * 向指定用户集合群发 WebSocket 消息
     *
     * <p>
     * 根据用户ID集合进行群发，
     * 每个用户的所有在线 Session 都会收到消息。
     * </p>
     *
     * @param userIds 用户ID集合
     * @param message 消息内容
     */
    @PostMapping("/send/users")
    public void sendToUsers(
            @RequestParam Set<String> userIds,
            @RequestBody String message
    ) {
        log.info(
                "HTTP 群发 WebSocket 消息，userIds：{}，message：{}",
                userIds,
                message
        );
        webSocketService.sendToUsers(userIds, message);
    }

    /**
     * 向指定 Session 发送 WebSocket 消息
     *
     * @param sessionId WebSocket SessionID
     * @param message   消息内容
     */
    @PostMapping("/send/session/{sessionId}")
    public void sendToSession(
            @PathVariable String sessionId,
            @RequestBody String message
    ) {
        log.info("HTTP 推送 WebSocket 消息给 Session，sessionId：{}，message：{}", sessionId, message);
        webSocketService.sendToSession(sessionId, message);
    }

    /**
     * 广播 WebSocket 消息
     *
     * <p>
     * 向当前所有在线 Session 推送消息。
     * </p>
     *
     * @param message 消息内容
     */
    @PostMapping("/broadcast")
    public void broadcast(@RequestBody String message) {
        log.info("HTTP 广播 WebSocket 消息，message：{}", message);
        webSocketService.broadcast(message);
    }

    /**
     * 踢指定用户下线
     *
     * <p>
     * 将关闭该用户的所有 WebSocket 连接，并向其发送下线原因。
     * </p>
     *
     * @param userId 用户ID
     * @param reason 下线原因
     */
    @PostMapping("/kick/{userId}")
    public void kickUser(
            @PathVariable String userId,
            @RequestParam(required = false, defaultValue = "管理员强制下线") String reason
    ) {
        log.warn("HTTP 请求踢用户下线，userId：{}，reason：{}", userId, reason);
        webSocketService.kickUser(userId, reason);
    }

    /**
     * 手动触发一次 WebSocket 心跳超时检测
     *
     * <p>
     * 主要用于运维或调试场景。
     * </p>
     */
    @PostMapping("/heartbeat/check")
    public void checkHeartbeat() {
        log.info("HTTP 触发 WebSocket 心跳超时检测");
        webSocketService.checkHeartbeatTimeout();
    }
}
