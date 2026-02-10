package io.github.atengk.ai.enums;

public enum AiModelType {

    OPENAI("openAiChatClient"),
    DASH_SCOPE("dashScopeChatClient");

    private final String chatClientBeanName;

    AiModelType(String chatClientBeanName) {
        this.chatClientBeanName = chatClientBeanName;
    }

    public String getBeanName() {
        return chatClientBeanName;
    }
}
