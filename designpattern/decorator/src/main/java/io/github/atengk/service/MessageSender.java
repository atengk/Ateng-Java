package io.github.atengk.service;

/**
 * 消息发送接口
 * 定义核心行为
 */
public interface MessageSender {

    /**
     * 发送消息
     *
     * @param message 消息内容
     */
    void send(String message);
}
