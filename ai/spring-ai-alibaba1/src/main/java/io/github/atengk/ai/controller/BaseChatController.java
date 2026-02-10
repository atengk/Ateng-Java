package io.github.atengk.ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai")
public class BaseChatController {

    private final ChatClient dashScopeClient;
    private final ChatClient openAiChatClient;

    public BaseChatController(
            @Qualifier("dashScopeClient") ChatClient dashScopeClient,
            @Qualifier("openAiChatClient") ChatClient openAiChatClient
    ) {
        this.dashScopeClient = dashScopeClient;
        this.openAiChatClient = openAiChatClient;
    }

    /**
     * 最基础的同步对话
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return openAiChatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * 流式对话（SSE / WebFlux 场景）
     */
    @GetMapping("/chat/stream")
    public Flux<String> stream(@RequestParam String message) {
        return dashScopeClient
                .prompt()
                .user(message)
                .stream()
                .content();
    }

    /**
     * 带 System Prompt 的基础用法
     */
    @GetMapping("/chat/system")
    public String chatWithSystem(
            @RequestParam String system,
            @RequestParam String message) {

        return dashScopeClient
                .prompt()
                .system(system)
                .user(message)
                .call()
                .content();
    }

    /**
     * 使用 Prompt Template 的基础示例
     */
    @GetMapping("/chat/template")
    public String chatWithTemplate(
            @RequestParam String topic,
            @RequestParam(defaultValue = "Java") String language) {

        return dashScopeClient
                .prompt()
                .user(u -> u.text("""
                        请用 {language} 的视角，
                        解释一下 {topic}，
                        并给出一个简单示例
                        """)
                        .param("topic", topic)
                        .param("language", language)
                )
                .call()
                .content();
    }
}
