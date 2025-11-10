package io.github.atengk.service.implementor;

import org.springframework.stereotype.Component;

/**
 * 邮件发送实现
 */
@Component
public class EmailSender implements MessageSender {

    @Override
    public void sendMessage(String message) {
        System.out.println("【邮件发送】内容：" + message);
    }
}
