package io.github.atengk.mcp.prompt;

import org.springaicommunity.mcp.annotation.McpPrompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * MCP Prompt：问候语生成
 *
 * @author Ateng
 * @since 2026-04-22
 */
@Component
public class GreetingPrompt {

    private static final Logger log = LoggerFactory.getLogger(GreetingPrompt.class);

    @McpPrompt(
            name = "greeting",
            title = "Greeting Prompt",
            description = "根据用户名生成一段自然、友好的问候提示语，用于引导模型输出问候内容"
    )
    public String greeting(String name) {
        log.debug("MCP提示[greeting]执行，参数 name={}", name);

        String prompt = "请用自然、友好的语气向用户“" + name + "”打招呼，可以适当加入寒暄或祝福语。";

        log.debug("MCP提示[greeting]生成完成");
        return prompt;
    }
}