package io.github.atengk.ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }

    @GetMapping("/chat/stream")
    public Flux<String> chatStream(@RequestParam String message) {
        return chatClient
                .prompt()
                .user(message)
                .stream()
                .content();
    }

    @GetMapping("/chat/system")
    public String chatWithSystem(@RequestParam String message) {
        return chatClient
                .prompt()
                .system("你是一个资深 Java 架构师，回答必须专业、简洁")
                .user(message)
                .call()
                .content();
    }

    @GetMapping("/chat/template")
    public String chatWithTemplate(
            @RequestParam String topic,
            @RequestParam(defaultValue = "Java") String language) {

        return chatClient
                .prompt()
                .system("你是一名技术写作专家")
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

