package io.github.atengk.service.decorator;

import io.github.atengk.service.MessageSender;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 日志增强装饰器
 * 在消息发送前后打印日志
 */
@Component
public class LogMessageDecorator extends AbstractMessageSenderDecorator {

    public LogMessageDecorator(@Qualifier("defaultMessageSender") MessageSender delegate) {
        super(delegate);
    }

    @Override
    public void send(String message) {
        System.out.println("【日志】准备发送消息...");
        super.send(message);
        System.out.println("【日志】消息发送完成。");
    }
}