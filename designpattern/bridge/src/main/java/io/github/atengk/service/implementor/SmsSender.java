package io.github.atengk.service.implementor;

import org.springframework.stereotype.Component;

/**
 * 短信发送实现
 */
@Component
public class SmsSender implements MessageSender {

    @Override
    public void sendMessage(String message) {
        System.out.println("【短信发送】内容：" + message);
    }
}
