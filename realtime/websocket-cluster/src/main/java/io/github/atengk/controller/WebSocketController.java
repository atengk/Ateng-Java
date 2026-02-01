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
 * 提供基于 HTTP 的 WebSocket 管理与运维接口，
 * 用于查询连接状态、主动推送消息、踢用户下线等操作。
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Slf4j
@RestController
@RequestMapping("/websocket")
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketService webSocketService;

    @GetMapping("/online/count")
    public long getOnlineUserCount() {
        return webSocketService.getOnlineUserCount();
    }

    @GetMapping("/online/session/count")
    public int getOnlineSessionCount() {
        return webSocketService.getOnlineSessionCount();
    }

    @GetMapping("/online/users")
    public Set<String> getOnlineUsers() {
        return webSocketService.getOnlineUsers();
    }

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

    @PostMapping("/send/users")
    public void sendToUsers(@RequestBody JSONObject body) {
        Set<String> userIds = new HashSet<>(body.getJSONArray("userIds")
                .toJavaList(String.class));
        String message = body.getString("message");

        log.info(
                "HTTP 群发 WebSocket 消息，userIds={}, message={}",
                userIds,
                message
        );

        webSocketService.sendToUsers(userIds, message);
    }

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

    @PostMapping("/broadcast")
    public void broadcast(@RequestBody JSONObject body) {
        String message = body.getString("message");

        log.info("HTTP 广播 WebSocket 消息，message={}", message);

        webSocketService.broadcast(message);
    }

    @PostMapping("/kick/user/{userId}")
    public void kickUser(@PathVariable String userId) {
        log.info("HTTP 踢用户下线，userId={}", userId);
        webSocketService.kickUser(userId);
    }

    @PostMapping("/kick/session/{sessionId}")
    public void kickSession(@PathVariable String sessionId) {
        log.info("HTTP 踢 Session 下线，sessionId={}", sessionId);
        webSocketService.closeSession(sessionId);
    }

    @PostMapping("/heartbeat/check")
    public void checkHeartbeat() {
        log.info("HTTP 触发 WebSocket 心跳超时检测");
        webSocketService.checkHeartbeatTimeout();
    }
}
