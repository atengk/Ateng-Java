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
 * 提供 MCP Server 的基础运行状态，仅用于只读查询。
 */
@Component
public class SystemResource {

    private static final Logger log = LoggerFactory.getLogger(SystemResource.class);

    @McpResource(
            uri = "system://runtime/info",
            name = "systemRuntimeInfo",
            title = "System Runtime Information",
            description = "提供 MCP Server 的运行状态、启动时间及 JVM 基础信息，仅用于只读查询"
    )
    public String systemInfo() {
        log.debug("MCP Resource accessed: uri=system://runtime/info");

        String info = buildSystemInfo();

        log.debug("MCP Resource response generated, length={}", info.length());
        return info;
    }

    /**
     * 构建系统运行信息（只读）
     */
    private String buildSystemInfo() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();

        return """
                MCP Server Runtime Status
                -------------------------
                Status      : RUNNING
                Start Time  : %s
                Uptime      : %d ms
                JVM Name    : %s
                """.formatted(
                Instant.now(),
                uptime,
                ManagementFactory.getRuntimeMXBean().getVmName()
        );
    }
}
