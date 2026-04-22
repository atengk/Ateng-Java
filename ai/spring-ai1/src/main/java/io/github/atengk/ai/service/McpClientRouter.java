package io.github.atengk.ai.service;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP Client 路由器
 * <p>
 * 用于管理 Spring AI 自动注入的多个 McpSyncClient，
 * 提供默认获取、按索引选择、批量访问等能力。
 * <p>
 * 说明：
 * - Spring AI 会为每个 MCP Server 连接创建一个 McpSyncClient
 * - 当前未提供 connectionName -> client 的直接映射
 *
 * @author Ateng
 * @since 2026-04-22
 */
@Component
public class McpClientRouter {

    private final List<McpSyncClient> clients;

    public McpClientRouter(List<McpSyncClient> clients) {
        this.clients = clients;
    }

    /**
     * 获取默认 Client（适用于单 MCP Server 场景）
     *
     * @return 默认 McpSyncClient（列表第一个）
     */
    public McpSyncClient getDefaultClient() {
        if (clients == null || clients.isEmpty()) {
            throw new IllegalStateException("未获取到任何 MCP Client");
        }
        return clients.get(0);
    }

    /**
     * 按索引获取指定 Client
     * <p>
     * 注意：
     * - index 与 spring.ai.mcp.client.sse.connections 的配置顺序一致
     * - 多 Server 场景建议避免硬编码 index
     *
     * @param index MCP Server 索引（从 0 开始）
     * @return 对应的 McpSyncClient
     */
    public McpSyncClient getByIndex(int index) {
        if (clients == null || clients.size() <= index) {
            throw new IllegalArgumentException("MCP Client 不存在, index=" + index);
        }
        return clients.get(index);
    }

    /**
     * 获取全部 Client（用于聚合调用或广播）
     *
     * @return MCP Client 列表
     */
    public List<McpSyncClient> getAll() {
        return clients;
    }
}