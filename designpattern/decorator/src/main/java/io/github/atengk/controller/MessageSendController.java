package io.github.atengk.controller;

import io.github.atengk.service.decorator.LogMessageDecorator;
import io.github.atengk.service.decorator.RetryMessageDecorator;
import io.github.atengk.service.impl.DefaultMessageSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 装饰器模式演示控制器
 */
@RestController
@RequestMapping("/api/message")
public class MessageSendController {

    private final DefaultMessageSender defaultMessageSender;

    public MessageSendController(DefaultMessageSender defaultMessageSender) {
        this.defaultMessageSender = defaultMessageSender;
    }

    @GetMapping("/send")
    public String send(@RequestParam String msg) {
        // 构建装饰链：日志 -> 重试 -> 真实发送
        RetryMessageDecorator retryDecorator =
                new RetryMessageDecorator(new LogMessageDecorator(defaultMessageSender));
        retryDecorator.send(msg);
        return "消息发送流程执行完成。";
    }
}
