package io.github.atengk.schedule;

import io.github.atengk.service.WebSocketSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * WebSocket 会话定时任务
 *
 * <p>
 * 定期触发 WebSocket 会话清理逻辑，
 * 用于清除心跳超时、异常断开的连接，
 * 防止在线状态与 Redis 数据长期不一致。
 * </p>
 *
 * <p>
 * 该类仅负责调度，不包含具体清理规则，
 * 实际清理逻辑由 {@link WebSocketSessionService} 统一维护。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionScheduler {
    private final WebSocketSessionService sessionService;

    /**
     * 定时清理心跳超时的 WebSocket 会话
     *
     * <p>
     * 默认每 60 秒执行一次，用于回收：
     * <ul>
     *     <li>非正常断开的连接</li>
     *     <li>心跳丢失的 session</li>
     * </ul>
     * </p>
     */
    @Scheduled(fixedDelay = 60_000)
    public void cleanupExpiredSessions() {
        log.debug("【WebSocket 定时任务】开始执行会话清理");
        sessionService.cleanupExpiredSessions();
    }
}
