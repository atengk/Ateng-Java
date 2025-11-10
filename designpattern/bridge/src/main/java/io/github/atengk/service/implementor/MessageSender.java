package io.github.atengk.service.implementor;

/**
 * 消息发送实现接口（实现部分）
 */
public interface MessageSender {

    /**
     * 发送消息
     *
     * @param message 消息内容
     */
    void sendMessage(String message);
}
