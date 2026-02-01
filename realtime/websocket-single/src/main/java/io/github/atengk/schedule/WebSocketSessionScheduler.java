package io.github.atengk.schedule;

import io.github.atengk.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * WebSocket 会话定时任务
 *
 * <p>
 * 负责周期性执行 WebSocket 会话相关的后台任务，
 * 目前用于检测并清理心跳超时的连接。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Slf4j
//@Component
@RequiredArgsConstructor
public class WebSocketSessionScheduler {

    /**
     * WebSocket 服务
     */
    private final WebSocketService webSocketService;

    /**
     * 定期检测 WebSocket 心跳超时连接
     *
     * <p>
     * 按固定延迟执行，通过配置项
     * websocket.heartbeat-check-interval 控制执行间隔，
     * 默认 30 秒执行一次。
     * </p>
     */
    @Scheduled(fixedDelayString = "${websocket.heartbeat-check-interval:30000}")
    public void checkHeartbeat() {
        try {
            log.debug("开始执行 WebSocket 心跳超时检测任务");
            webSocketService.checkHeartbeatTimeout();
        } catch (Exception e) {
            log.error("WebSocket 心跳检测任务执行异常", e);
        }
    }

}
