package io.github.atengk.service.decorator;

import io.github.atengk.service.MessageSender;

/**
 * 抽象装饰器
 * 持有一个 MessageSender 实例，并实现相同接口
 */
public abstract class AbstractMessageSenderDecorator implements MessageSender {
    protected final MessageSender delegate;

    protected AbstractMessageSenderDecorator(MessageSender delegate) {
        this.delegate = delegate;
    }

    @Override
    public void send(String message) {
        delegate.send(message);
    }

}

