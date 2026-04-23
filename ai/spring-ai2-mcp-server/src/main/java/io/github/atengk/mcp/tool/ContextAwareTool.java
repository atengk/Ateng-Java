package io.github.atengk.mcp.tool;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpMeta;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Tool：上下文感知工具服务
 * <p>
 * 该组件用于演示通过 McpMeta 读取请求元数据，
 * 并根据用户上下文返回不同结果。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class ContextAwareTool {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(ContextAwareTool.class);

    /**
     * 根据请求元数据执行上下文感知处理
     * <p>
     * 该方法会从 MCP 请求元数据中读取用户信息，
     * 并返回带上下文标识的结构化结果。
     *
     * @param keyword 查询关键字
     * @param meta    MCP 请求元数据
     * @return 上下文处理结果
     */
    @McpTool(
            name = "contextAwareSearch",
            title = "上下文感知查询",
            description = "根据请求元数据中的用户信息，返回带上下文的查询结果",
            annotations = @McpTool.McpAnnotations(
                    title = "上下文感知查询工具",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public ContextSearchResult contextAwareSearch(
            @McpToolParam(description = "查询关键字", required = true) String keyword,
            McpMeta meta) {

        String userId = Convert.toStr(meta.get("userId"), "anonymous");
        String userRole = Convert.toStr(meta.get("userRole"), "guest");
        String tenantId = Convert.toStr(meta.get("tenantId"), "default");

        log.debug("MCP工具[contextAwareSearch]执行，keyword={}，userId={}，userRole={}，tenantId={}",
                keyword, userId, userRole, tenantId);

        String scope = StrUtil.equalsIgnoreCase(userRole, "admin") ? "全部数据范围" : "当前租户数据范围";
        String summary = StrUtil.format(
                "用户[{}]以角色[{}]在租户[{}]下查询关键字[{}]，当前生效范围：{}",
                userId, userRole, tenantId, keyword, scope
        );

        return new ContextSearchResult(
                keyword,
                userId,
                userRole,
                tenantId,
                scope,
                true,
                summary
        );
    }

    /**
     * MCP Tool：上下文查询结果对象
     *
     * @param keyword  查询关键字
     * @param userId   用户标识
     * @param userRole 用户角色
     * @param tenantId 租户标识
     * @param scope    生效范围
     * @param success  是否成功
     * @param message  结果说明
     * @author Ateng
     * @since 2026-04-23
     */
    public record ContextSearchResult(
            String keyword,
            String userId,
            String userRole,
            String tenantId,
            String scope,
            Boolean success,
            String message
    ) {
    }
}
