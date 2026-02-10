package io.github.atengk.ai.service;

import io.github.atengk.ai.enums.AiModelType;
import org.springframework.ai.chat.client.ChatClient;

public interface ChatClientStrategy {

    /**
     * 当前策略支持的模型类型
     *
     * @return AiModelType
     */
    AiModelType getModelType();

    /**
     * 返回对应的 ChatClient
     *
     * @return ChatClient
     */
    ChatClient getChatClient();
}
