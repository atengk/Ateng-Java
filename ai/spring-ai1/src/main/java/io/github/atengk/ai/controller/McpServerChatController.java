package io.github.atengk.ai.controller;

import io.github.atengk.ai.tool.CommonTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/mcp-server")
public class McpServerChatController {

    private final ChatClient mcpServerChatClient;

    /**
     * 最基础的同步对话
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

}
