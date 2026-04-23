package io.github.atengk.mcp.prompt;

import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Prompt：个性化简介生成服务
 * <p>
 * 该组件用于根据用户的可选信息，
 * 动态生成个性化简介提示内容。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class ProfileIntroPrompt {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(ProfileIntroPrompt.class);

    /**
     * 生成个性化简介提示内容
     *
     * @param name      用户名
     * @param age       年龄
     * @param interests 兴趣爱好
     * @param tone      语气风格
     * @return MCP Prompt 返回结果
     */
    @McpPrompt(
            name = "profileIntro",
            title = "个性化简介提示词",
            description = "根据姓名、年龄、兴趣和语气风格生成个性化简介提示内容"
    )
    public GetPromptResult profileIntro(
            @McpArg(name = "name", description = "用户名", required = true) String name,
            @McpArg(name = "age", description = "年龄", required = false) Integer age,
            @McpArg(name = "interests", description = "兴趣爱好，多个可用逗号分隔", required = false) String interests,
            @McpArg(name = "tone", description = "语气风格，例如：正式、轻松、活泼", required = false) String tone) {

        log.debug("MCP提示[profileIntro]执行，name={}，age={}，interests={}，tone={}", name, age, interests, tone);

        StringBuilder message = new StringBuilder();
        message.append("请根据以下信息生成一段自然、简洁的个人简介。").append("\n");
        message.append("姓名：").append(name).append("\n");

        if (age != null) {
            message.append("年龄：").append(age).append("\n");
        }

        if (StrUtil.isNotBlank(interests)) {
            message.append("兴趣：").append(interests).append("\n");
        }

        if (StrUtil.isNotBlank(tone)) {
            message.append("语气要求：").append(tone).append("\n");
        } else {
            message.append("语气要求：自然、友好").append("\n");
        }

        message.append("要求输出 100 字以内，适合作为个人资料简介。");

        log.debug("MCP提示[profileIntro]生成完成");
        return new GetPromptResult(
                "个性化简介提示词",
                List.of(new PromptMessage(Role.USER, new TextContent(message.toString())))
        );
    }
}