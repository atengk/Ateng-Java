package io.github.atengk.service.product;

import org.springframework.stereotype.Component;

/**
 * 短信消息实现
 */
@Component
public class SmsMessage implements Message {

    @Override
    public void send() {
        System.out.println("【短信消息】发送成功！");
    }
}
