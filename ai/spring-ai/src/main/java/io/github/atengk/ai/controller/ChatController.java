package io.github.atengk.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient.Builder chatClientBuilder;

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        ChatClient chatClient = chatClientBuilder.build();

        return chatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }

}

