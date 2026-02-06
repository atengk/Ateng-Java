package io.github.atengk.ai.config;

import io.github.atengk.ai.tool.CommonTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ChatClientConfig {

    private final CommonTools commonTools;

    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            ChatMemory chatMemory) {

        return builder
                .defaultTools(commonTools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor
                                .builder(chatMemory)
                                .build()
                )
                .build();
    }

    @Bean
    public ChatClient mcpServerChatClient(
            ChatClient.Builder builder,
            ToolCallbackProvider mcpToolCallbackProvider) {

        return builder
                .defaultToolCallbacks(mcpToolCallbackProvider)
                .build();
    }

}