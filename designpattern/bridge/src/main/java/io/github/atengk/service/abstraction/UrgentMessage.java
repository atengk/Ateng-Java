package io.github.atengk.service.abstraction;


import io.github.atengk.service.implementor.MessageSender;

/**
 * 紧急消息（扩展抽象）
 */
public class UrgentMessage extends AbstractMessage {

    public UrgentMessage(MessageSender messageSender) {
        super(messageSender);
    }

    @Override
    public void send(String content) {
        System.out.println("【紧急消息】开始发送...");
        messageSender.sendMessage(content);
        System.out.println("【紧急消息】发送完成");
    }
}
