package io.github.atengk.service.abstraction;

import io.github.atengk.service.implementor.MessageSender;

/**
 * 抽象消息类（抽象部分）
 */
public abstract class AbstractMessage {

    protected final MessageSender messageSender;

    /**
     * 构造方法注入实现者
     *
     * @param messageSender 消息发送实现
     */
    protected AbstractMessage(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    /**
     * 发送消息
     *
     * @param content 消息内容
     */
    public abstract void send(String content);
}
