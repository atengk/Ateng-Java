package io.github.atengk.mcp.prompt;

import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Prompt：问候语生成服务
 * <p>
 * 该组件用于根据用户传入的姓名，
 * 生成一段自然、友好的问候提示内容，
 * 供 MCP Client 或大模型继续使用。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class GreetingPrompt {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(GreetingPrompt.class);

    /**
     * 生成问候提示内容
     * <p>
     * 该方法会根据传入的用户名，
     * 组装一段适合大模型使用的问候提示语。
     *
     * @param name 用户名
     * @return MCP Prompt 返回结果
     */
    @McpPrompt(
            name = "greeting",
            title = "问候提示词",
            description = "根据用户名生成一段自然、友好的问候提示语"
    )
    public GetPromptResult greeting(
            @McpArg(name = "name", description = "用户名", required = true) String name) {

        log.debug("MCP提示[greeting]执行，参数name={}", name);

        // 生成给模型使用的提示内容
        String message = StrUtil.format(
                "请用自然、友好的语气向用户“{}”打招呼，可以适当加入寒暄或祝福语。",
                name
        );

        log.debug("MCP提示[greeting]生成完成");

        // 返回标准的 Prompt 结果，供 MCP Client 或模型继续处理
        return new GetPromptResult(
                "问候提示词",
                List.of(new PromptMessage(Role.USER, new TextContent(message)))
        );
    }
}