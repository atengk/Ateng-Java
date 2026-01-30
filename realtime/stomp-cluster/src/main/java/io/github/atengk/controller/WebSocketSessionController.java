package io.github.atengk.controller;

import io.github.atengk.service.WebSocketSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * WebSocket 会话与在线状态监控接口
 *
 * <p>主要用途：
 * <ul>
 *     <li>提供 WebSocket 在线用户与连接统计</li>
 *     <li>用于运维监控、管理后台、健康检查</li>
 * </ul>
 *
 * <p>设计说明：
 * <ul>
 *     <li>只读接口，不涉及任何写操作</li>
 *     <li>数据来源于 Redis，支持集群环境</li>
 * </ul>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@RestController
@RequestMapping("/websocket-session")
@RequiredArgsConstructor
public class WebSocketSessionController {

    private final WebSocketSessionService webSocketSessionService;

    /**
     * 获取当前 WebSocket 在线统计信息
     *
     * <p>返回数据说明：
     * <ul>
     *     <li>totalUsers：当前在线用户数（去重）</li>
     *     <li>totalConnections：当前在线 WebSocket 连接总数</li>
     *     <li>avgConnectionsPerUser：人均连接数（用于发现多端登录情况）</li>
     *     <li>timestamp：统计时间点（Unix 秒级时间戳）</li>
     * </ul>
     *
     * <p>使用场景：
     * <ul>
     *     <li>管理后台实时展示在线人数</li>
     *     <li>监控系统采集指标</li>
     *     <li>压测 / 容量评估</li>
     * </ul>
     *
     * @return WebSocket 在线统计数据
     */
    @GetMapping("/online-stats")
    public Map<String, Object> getOnlineStats() {
        return webSocketSessionService.getOnlineStats();
    }
}
