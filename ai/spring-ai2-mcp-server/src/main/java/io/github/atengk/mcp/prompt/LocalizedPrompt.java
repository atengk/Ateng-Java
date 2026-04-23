package io.github.atengk.mcp.prompt;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpMeta;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Prompt：本地化提示生成服务
 * <p>
 * 该组件用于根据请求元数据中的语言和地区信息，
 * 动态生成本地化提示内容。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class LocalizedPrompt {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(LocalizedPrompt.class);

    /**
     * 根据语言和地区生成本地化提示内容
     *
     * @param topic 主题
     * @param meta  MCP 请求元数据
     * @return MCP Prompt 返回结果
     */
    @McpPrompt(
            name = "localizedPrompt",
            title = "本地化提示词",
            description = "根据请求元数据中的语言和地区生成本地化提示内容"
    )
    public GetPromptResult localizedPrompt(
            @McpArg(name = "topic", description = "主题", required = true) String topic,
            McpMeta meta) {

        String language = Convert.toStr(meta.get("language"), "zh-CN");
        String region = Convert.toStr(meta.get("region"), "CN");

        log.debug("MCP提示[localizedPrompt]执行，topic={}，language={}，region={}", topic, language, region);

        String message;
        if (StrUtil.startWithIgnoreCase(language, "en")) {
            message = StrUtil.format(
                    "Please generate a concise and natural prompt about [{}], suitable for users in region [{}].",
                    topic,
                    region
            );
        } else {
            message = StrUtil.format(
                    "请围绕“{}”生成一段简洁、自然的提示内容，适用于地区“{}”的用户。",
                    topic,
                    region
            );
        }

        log.debug("MCP提示[localizedPrompt]生成完成");
        return new GetPromptResult(
                "本地化提示词",
                List.of(new PromptMessage(Role.USER, new TextContent(message)))
        );
    }
}