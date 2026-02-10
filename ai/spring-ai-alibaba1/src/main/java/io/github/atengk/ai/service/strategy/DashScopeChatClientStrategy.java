package io.github.atengk.ai.service.strategy;

import io.github.atengk.ai.enums.AiModelType;
import io.github.atengk.ai.service.ChatClientStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class DashScopeChatClientStrategy implements ChatClientStrategy {

    private final ChatClient chatClient;

    public DashScopeChatClientStrategy(
            @Qualifier("dashScopeChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public AiModelType getModelType() {
        return AiModelType.DASH_SCOPE;
    }

    @Override
    public ChatClient getChatClient() {
        return chatClient;
    }
}

