package io.github.atengk.ai.service;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MCP Prompt 客户端服务
 * <p>
 * 提供 MCP Prompt 的查询与获取能力：
 * - 支持多 Server Prompt 聚合
 * - 支持按指定 Client 获取 Prompt 模板
 *
 * @author Ateng
 * @since 2026-04-22
 */
@Service
public class McpPromptService {

    private static final Logger log = LoggerFactory.getLogger(McpPromptService.class);

    private final McpClientRouter router;

    public McpPromptService(McpClientRouter router) {
        this.router = router;
    }

    /**
     * 获取所有 Prompt（聚合）
     *
     * @return Prompt 列表
     */
    public List<McpSchema.Prompt> listAllPrompts() {
        List<McpSchema.Prompt> result = new ArrayList<>();

        for (McpSyncClient client : router.getAll()) {
            McpSchema.ListPromptsResult response = client.listPrompts();
            if (response != null && response.prompts() != null) {
                result.addAll(response.prompts());
            }
        }

        log.info("聚合 Prompt 数量: {}", result.size());
        return result;
    }

    /**
     * 获取 Prompt 内容（默认 Client）
     *
     * @param name Prompt 名称（如：greeting）
     * @param args Prompt 参数（与服务端定义一致）
     * @return Prompt 内容结果
     */
    public McpSchema.GetPromptResult getPrompt(String name, Map<String, Object> args) {
        log.info("获取 Prompt, name={}, args={}", name, args);

        return router.getDefaultClient()
                .getPrompt(new McpSchema.GetPromptRequest(name, args));
    }

    /**
     * 获取 Prompt 内容（指定 Client）
     *
     * @param name        Prompt 名称
     * @param args        Prompt 参数
     * @param clientIndex MCP Client 索引（对应 connections 顺序）
     * @return Prompt 内容结果
     */
    public McpSchema.GetPromptResult getPrompt(String name, Map<String, Object> args, int clientIndex) {
        log.info("获取 Prompt, name={}, clientIndex={}", name, clientIndex);

        return router.getByIndex(clientIndex)
                .getPrompt(new McpSchema.GetPromptRequest(name, args));
    }
}