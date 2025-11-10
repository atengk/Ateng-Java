package io.github.atengk.controller;

import io.github.atengk.service.factory.EmailMessageFactory;
import io.github.atengk.service.factory.MessageFactory;
import io.github.atengk.service.factory.SmsMessageFactory;
import io.github.atengk.service.product.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息控制器，演示工厂方法模式
 */
@RestController
public class MessageController {

    private final EmailMessageFactory emailFactory;
    private final SmsMessageFactory smsFactory;

    public MessageController(EmailMessageFactory emailFactory, SmsMessageFactory smsFactory) {
        this.emailFactory = emailFactory;
        this.smsFactory = smsFactory;
    }

    /**
     * 根据 type 参数发送不同类型消息
     *
     * @param type 消息类型（email / sms）
     * @return 发送结果
     */
    @GetMapping("/send")
    public String sendMessage(@RequestParam(defaultValue = "email") String type) {
        MessageFactory factory;
        if ("sms".equalsIgnoreCase(type)) {
            factory = smsFactory;
        } else {
            factory = emailFactory;
        }
        Message message = factory.createMessage();
        message.send();
        return "消息发送完成：" + message.getClass().getSimpleName();
    }
}
