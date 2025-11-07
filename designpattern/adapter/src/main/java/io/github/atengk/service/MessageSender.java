package io.github.atengk.service;


/**
 * 定义系统内部统一的消息发送接口
 */
public interface MessageSender {
    /**
     * 发送消息
     *
     * @param userId  接收者ID
     * @param content 消息内容
     */
    void sendMessage(String userId, String content);
}
