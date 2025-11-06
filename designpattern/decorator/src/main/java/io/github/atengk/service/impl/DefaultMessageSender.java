package io.github.atengk.service.impl;

import io.github.atengk.service.MessageSender;
import org.springframework.stereotype.Service;

/**
 * 默认的消息发送实现
 */
@Service("defaultMessageSender")
public class DefaultMessageSender implements MessageSender {

    @Override
    public void send(String message) {
        System.out.println("【发送消息】" + message);
    }
}
