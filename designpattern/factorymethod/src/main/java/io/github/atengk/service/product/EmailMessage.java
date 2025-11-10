package io.github.atengk.service.product;

import org.springframework.stereotype.Component;

/**
 * 邮件消息实现
 */
@Component
public class EmailMessage implements Message {

    @Override
    public void send() {
        System.out.println("【邮件消息】发送成功！");
    }
}
