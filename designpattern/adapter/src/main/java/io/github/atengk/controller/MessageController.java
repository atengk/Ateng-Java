package io.github.atengk.controller;


import io.github.atengk.service.MessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息发送控制器
 */
@RestController
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/send")
    public String send(@RequestParam String channel,
                       @RequestParam String userId,
                       @RequestParam String content) {
        messageService.send(channel, userId, content);
        return "消息已通过 " + channel + " 发送";
    }
}
