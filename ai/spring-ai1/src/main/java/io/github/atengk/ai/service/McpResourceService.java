package io.github.atengk.ai.service;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP Resource 客户端服务
 * <p>
 * 提供 MCP Resource 的查询与读取能力：
 * - 支持多 Server 资源聚合
 * - 支持按指定 Client 精确读取
 *
 * @author Ateng
 * @since 2026-04-22
 */
@Service
public class McpResourceService {

    private static final Logger log = LoggerFactory.getLogger(McpResourceService.class);

    private final McpClientRouter router;

    public McpResourceService(McpClientRouter router) {
        this.router = router;
    }

    /**
     * 获取所有 MCP Server 的资源（聚合）
     *
     * @return Resource 列表
     */
    public List<McpSchema.Resource> listAllResources() {
        List<McpSchema.Resource> result = new ArrayList<>();

        for (McpSyncClient client : router.getAll()) {
            McpSchema.ListResourcesResult response = client.listResources();
            if (response != null && response.resources() != null) {
                result.addAll(response.resources());
            }
        }

        log.info("聚合 Resource 数量: {}", result.size());
        return result;
    }

    /**
     * 读取指定 Resource（默认 Client）
     *
     * @param uri Resource 唯一标识（如：system://runtime/info）
     * @return Resource 内容
     */
    public McpSchema.ReadResourceResult read(String uri) {
        log.info("读取 Resource, uri={}", uri);
        return router.getDefaultClient()
                .readResource(new McpSchema.ReadResourceRequest(uri));
    }

    /**
     * 读取指定 Resource（指定 Client）
     *
     * @param uri         Resource 唯一标识
     * @param clientIndex MCP Client 索引（对应 connections 顺序）
     * @return Resource 内容
     */
    public McpSchema.ReadResourceResult read(String uri, int clientIndex) {
        log.info("读取 Resource, uri={}, clientIndex={}", uri, clientIndex);
        return router.getByIndex(clientIndex)
                .readResource(new McpSchema.ReadResourceRequest(uri));
    }
}