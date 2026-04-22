package io.github.atengk.mcp.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.time.Instant;

/**
 * MCP Resource：系统运行信息
 * <p>
 * 提供服务运行状态、启动时间、运行时长及 JVM 信息
 *
 * @author Ateng
 * @since 2026-04-22
 */
@Component
public class SystemResource {

    private static final Logger log = LoggerFactory.getLogger(SystemResource.class);

    @McpResource(
            uri = "system://runtime/info",
            name = "systemRuntimeInfo",
            title = "System Runtime Information",
            description = "获取 MCP Server 的运行状态、启动时间、运行时长及 JVM 信息（只读）"
    )
    public String systemInfo() {
        log.debug("MCP资源[systemRuntimeInfo]被访问");

        String info = buildSystemInfo();

        log.debug("MCP资源[systemRuntimeInfo]返回成功，内容长度={}", info.length());
        return info;
    }

    /**
     * 构建系统运行信息（只读）
     */
    private String buildSystemInfo() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        Instant now = Instant.now();

        return """
                MCP Server Runtime Status
                -------------------------
                Status      : RUNNING
                Current Time: %s
                Uptime      : %d ms
                JVM Name    : %s
                """.formatted(
                now,
                uptime,
                ManagementFactory.getRuntimeMXBean().getVmName()
        );
    }
}