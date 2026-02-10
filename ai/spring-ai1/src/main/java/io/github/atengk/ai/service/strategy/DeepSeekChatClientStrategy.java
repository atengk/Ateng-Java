package io.github.atengk.ai.service.strategy;

import io.github.atengk.ai.enums.AiModelType;
import io.github.atengk.ai.service.ChatClientStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekChatClientStrategy implements ChatClientStrategy {

    private final ChatClient chatClient;

    public DeepSeekChatClientStrategy(
            @Qualifier("deepSeekChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public AiModelType getModelType() {
        return AiModelType.DEEPSEEK;
    }

    @Override
    public ChatClient getChatClient() {
        return chatClient;
    }
}

