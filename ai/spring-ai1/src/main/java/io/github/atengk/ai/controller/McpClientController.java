package io.github.atengk.ai.controller;

import io.github.atengk.ai.service.McpPromptService;
import io.github.atengk.ai.service.McpResourceService;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * MCP Client 测试接口
 * <p>
 * 提供 MCP Tool / Resource / Prompt 的调用示例接口
 *
 * @author Ateng
 * @since 2026-04-22
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpClientController {

    private final ChatClient mcpServerChatClient;
    private final McpResourceService resourceService;
    private final McpPromptService promptService;

    /**
     * 对话接口（支持 MCP Tool 自动调用）
     * <p>
     * 示例：
     * curl "http://localhost:19001/mcp/chat?message=计算 1+2"
     * curl "http://localhost:19001/mcp/chat?message=获取北京天气"
     *
     * @param message 用户输入
     * @return 模型回复
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return mcpServerChatClient
                .prompt()
                .system("""
                        你可以在必要时调用系统提供的工具，
                        工具的返回结果是可信的，
                        不要自行编造结果。
                        """)
                .user(message)
                .call()
                .content();
    }

    /**
     * 获取所有 Resource（聚合）
     * <p>
     * 示例：
     * curl "http://localhost:19001/mcp/resources"
     *
     * @return Resource 列表
     */
    @GetMapping("/resources")
    public List<McpSchema.Resource> resources() {
        return resourceService.listAllResources();
    }

    /**
     * 读取指定 Resource
     * <p>
     * 示例：
     * curl "http://localhost:19001/mcp/resource?uri=system://runtime/info"
     *
     * @param uri Resource 唯一标识
     * @return Resource 内容
     */
    @GetMapping("/resource")
    public McpSchema.ReadResourceResult read(@RequestParam String uri) {
        return resourceService.read(uri);
    }

    /**
     * 获取所有 Prompt（聚合）
     * <p>
     * 示例：
     * curl "http://localhost:19001/mcp/prompts"
     *
     * @return Prompt 列表
     */
    @GetMapping("/prompts")
    public List<McpSchema.Prompt> prompts() {
        return promptService.listAllPrompts();
    }

    /**
     * 获取指定 Prompt
     * <p>
     * 示例：
     * curl "http://localhost:19001/mcp/prompt?name=greeting&userName=Ateng"
     *
     * @param name     Prompt 名称
     * @param userName Prompt 参数（对应服务端定义）
     * @return Prompt 内容
     */
    @GetMapping("/prompt")
    public McpSchema.GetPromptResult prompt(@RequestParam String name,
                                            @RequestParam String userName) {
        return promptService.getPrompt(name, Map.of("name", userName));
    }
}