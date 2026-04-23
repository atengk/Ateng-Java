package io.github.atengk.mcp.resource;

import cn.hutool.core.date.DateUtil;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.List;

/**
 * MCP Resource：系统运行信息资源服务
 * <p>
 * 该组件用于向 MCP Client 提供当前服务的运行信息，
 * 包括运行状态、当前时间、运行时长以及 JVM 名称。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class SystemResource {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(SystemResource.class);

    /**
     * 读取系统运行信息资源
     * <p>
     * 该方法会组装当前 MCP Server 的运行状态信息，
     * 并以文本资源的形式返回给客户端。
     *
     * @return 系统运行信息资源结果
     */
    @McpResource(
            uri = "system://runtime/info",
            name = "systemRuntimeInfo",
            title = "系统运行信息",
            description = "获取 MCP Server 的运行状态、启动时间、运行时长及 JVM 信息（只读）"
    )
    public ReadResourceResult systemInfo() {
        log.debug("MCP资源[systemRuntimeInfo]被访问");

        // 构建系统运行信息文本
        String info = buildSystemInfo();

        log.debug("MCP资源[systemRuntimeInfo]返回成功，内容长度={}", info.length());
        return new ReadResourceResult(List.of(
                new TextResourceContents("system://runtime/info", "text/plain", info)
        ));
    }

    /**
     * 构建系统运行信息文本内容
     * <p>
     * 主要包含当前时间、服务运行时长和 JVM 名称。
     *
     * @return 系统运行信息文本
     */
    private String buildSystemInfo() {
        // 获取 JVM 已运行时长，单位为毫秒
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();

        // 获取当前时间并格式化为常见日期时间格式
        Instant now = Instant.now();
        String startTime = DateUtil.formatDateTime(DateUtil.date(now.toEpochMilli()));

        // 返回格式化后的系统信息文本
        return """
                MCP Server Runtime Status
                -------------------------
                Status      : RUNNING
                Current Time: %s
                Uptime      : %d ms
                JVM Name    : %s
                """.formatted(
                startTime,
                uptime,
                ManagementFactory.getRuntimeMXBean().getVmName()
        );
    }
}