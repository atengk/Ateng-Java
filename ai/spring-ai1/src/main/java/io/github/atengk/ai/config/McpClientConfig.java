package io.github.atengk.ai.config;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * MCP Client 配置
 *
 * @author Ateng
 * @since 2026-04-22
 */
@Configuration
@RequiredArgsConstructor
public class McpClientConfig {

    /**
     * 构建 ChatClient，并接入 MCP Tool（支持模型自动调用）
     *
     * @param builder              ChatClient 构建器
     * @param toolCallbackProvider 工具提供者（包含 MCP Tool / 本地 Tool）
     * @return ChatClient
     */
    @Bean
    public ChatClient mcpChatClient(
            ChatClient.Builder builder,
            ToolCallbackProvider toolCallbackProvider) {

        return builder
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    /**
     * 提供默认 McpSyncClient（用于手动调用 Resource / Prompt / Tool）
     *
     * @param mcpSyncClients Spring 自动注入的 MCP Client 列表
     * @return 默认 McpSyncClient
     */
    @Bean
    public McpSyncClient defaultMcpSyncClient(List<McpSyncClient> mcpSyncClients) {
        if (mcpSyncClients == null || mcpSyncClients.isEmpty()) {
            throw new IllegalStateException("未找到可用的 MCP Sync Client");
        }
        return mcpSyncClients.get(0);
    }

}