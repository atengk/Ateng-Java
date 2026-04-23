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
 * MCP Prompt：文章大纲生成服务
 * <p>
 * 该组件用于通过多条消息组合的方式，
 * 生成结构更清晰的文章大纲提示内容。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class ArticleOutlinePrompt {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(ArticleOutlinePrompt.class);

    /**
     * 生成文章大纲提示内容
     *
     * @param topic    文章主题
     * @param audience 目标读者
     * @return MCP Prompt 返回结果
     */
    @McpPrompt(
            name = "articleOutline",
            title = "文章大纲提示词",
            description = "根据主题和目标读者生成结构化文章大纲提示内容"
    )
    public GetPromptResult articleOutline(
            @McpArg(name = "topic", description = "文章主题", required = true) String topic,
            @McpArg(name = "audience", description = "目标读者，例如：初学者、后端开发者、产品经理", required = true) String audience) {

        log.debug("MCP提示[articleOutline]执行，topic={}，audience={}", topic, audience);

        String assistantMessage = """
                你是一名资深内容策划顾问，
                擅长把复杂主题拆解成清晰、可执行的文章结构。
                """;

        String userMessage = StrUtil.format("""
                请围绕主题“{}”生成一份文章大纲。
                目标读者：{}
                输出要求：
                1. 给出文章标题
                2. 给出一级、二级结构
                3. 每一部分补充一句核心说明
                4. 整体结构清晰、适合技术文章写作
                """, topic, audience);

        log.debug("MCP提示[articleOutline]生成完成");
        return new GetPromptResult(
                "文章大纲提示词",
                List.of(
                        new PromptMessage(Role.ASSISTANT, new TextContent(assistantMessage)),
                        new PromptMessage(Role.USER, new TextContent(userMessage))
                )
        );
    }
}