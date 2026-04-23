package io.github.atengk.mcp.resource;

import cn.hutool.core.convert.Convert;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpMeta;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Resource：安全数据资源服务
 * <p>
 * 该组件用于演示通过请求元数据进行权限判断，
 * 只有具备指定访问级别的调用方才能读取真实数据。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class SecureDataResource {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(SecureDataResource.class);

    /**
     * 根据资源编号读取安全数据
     *
     * @param id   资源编号
     * @param meta MCP 请求元数据
     * @return 安全数据资源结果
     */
    @McpResource(
            uri = "secure-data://{id}",
            name = "secureDataResource",
            title = "安全数据资源",
            description = "根据访问级别读取安全数据",
            mimeType = "text/plain"
    )
    public ReadResourceResult getSecureData(String id, McpMeta meta) {
        String requestingUser = Convert.toStr(meta.get("requestingUser"), "anonymous");
        String accessLevel = Convert.toStr(meta.get("accessLevel"), "guest");

        log.debug("MCP资源[secureDataResource]被访问，id={}，requestingUser={}，accessLevel={}",
                id, requestingUser, accessLevel);

        String content;
        if (!"admin".equalsIgnoreCase(accessLevel)) {
            log.warn("MCP资源[secureDataResource]访问被拒绝，id={}，requestingUser={}", id, requestingUser);
            content = "Access denied";
        } else {
            content = "机密数据内容：合同编号=" + id + "，当前状态=已生效";
        }

        return new ReadResourceResult(List.of(
                new TextResourceContents(
                        "secure-data://" + id,
                        "text/plain",
                        content
                )
        ));
    }
}