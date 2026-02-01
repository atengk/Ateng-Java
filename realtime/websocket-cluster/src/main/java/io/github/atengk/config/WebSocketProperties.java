package io.github.atengk.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * WebSocket 配置属性绑定类
 *
 * <p>
 * 用于统一管理 WebSocket 相关的配置项，
 * 通过 {@code websocket.*} 前缀从配置文件中加载。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Data
@Component
@ConfigurationProperties(prefix = "websocket")
public class WebSocketProperties {

    /**
     * WebSocket 端点路径
     *
     * <p>
     * 客户端建立 WebSocket 连接时访问的路径，
     * 例如：/ws
     * </p>
     */
    @NotBlank
    private String endpoint;

    /**
     * 允许跨域访问的来源列表
     *
     * <p>
     * 用于配置 WebSocket 的跨域访问控制，
     * 可配置多个前端访问地址。
     * </p>
     *
     * <pre>
     * 示例：
     * - http://localhost:5173
     * - https://www.example.com
     * </pre>
     */
    @NotEmpty
    private List<String> allowedOrigins;

    /**
     * WebSocket 心跳超时时间
     *
     * <p>
     * 在该时间范围内未收到客户端心跳消息，
     * 将认为连接已失效并主动关闭。
     * </p>
     */
    @NotNull
    private Duration heartbeatTimeout;

    /**
     * WebSocket 心跳检测执行间隔
     *
     * <p>
     * 表示后台定时任务检测心跳超时的执行频率，
     * 单位为毫秒。
     * </p>
     */
    @NotNull
    private Long heartbeatCheckInterval;
}
