package io.github.atengk.controller;


import io.github.atengk.service.abstraction.AbstractMessage;
import io.github.atengk.service.abstraction.UrgentMessage;
import io.github.atengk.service.implementor.EmailSender;
import io.github.atengk.service.implementor.MessageSender;
import io.github.atengk.service.implementor.SmsSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 桥接模式控制器演示
 */
@RestController
public class BridgeDemoController {

    private final EmailSender emailSender;
    private final SmsSender smsSender;

    public BridgeDemoController(EmailSender emailSender, SmsSender smsSender) {
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }

    /**
     * 根据 channel 参数发送紧急消息
     *
     * @param channel 渠道（email / sms）
     * @param content 消息内容
     * @return 发送结果
     */
    @GetMapping("/bridge/send")
    public String sendMessage(@RequestParam(defaultValue = "email") String channel,
                              @RequestParam(defaultValue = "测试内容") String content) {

        MessageSender sender;
        if ("sms".equalsIgnoreCase(channel)) {
            sender = smsSender;
        } else {
            sender = emailSender;
        }

        AbstractMessage message = new UrgentMessage(sender);
        message.send(content);
        return "消息发送完成：" + channel;
    }
}