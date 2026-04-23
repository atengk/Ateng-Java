package io.github.atengk.mcp.prompt;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Prompt：固定写作模板服务
 * <p>
 * 该组件用于提供一套固定的写作提示模板，
 * 适合不需要入参的通用 Prompt 场景。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class FixedWritingPrompt {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(FixedWritingPrompt.class);

    /**
     * 返回固定写作提示模板
     *
     * @return MCP Prompt 返回结果
     */
    @McpPrompt(
            name = "fixedWritingPrompt",
            title = "固定写作模板",
            description = "提供一套固定的中文写作提示模板"
    )
    public GetPromptResult fixedWritingPrompt() {
        log.debug("MCP提示[fixedWritingPrompt]执行");

        String message = """
                你是一名专业的中文写作助手，请严格遵循以下要求：
                1. 表达准确、简洁、自然
                2. 先给结论，再补充细节
                3. 输出结构清晰，必要时分点说明
                4. 如果涉及代码，请补充关键注释
                """;

        log.debug("MCP提示[fixedWritingPrompt]生成完成");
        return new GetPromptResult(
                "固定写作模板",
                List.of(new PromptMessage(Role.USER, new TextContent(message)))
        );
    }
}