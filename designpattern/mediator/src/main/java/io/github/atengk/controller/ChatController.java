package io.github.atengk.controller;

import io.github.atengk.service.mediator.ChatRoom;
import io.github.atengk.service.mediator.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 中介者模式控制器
 */
@RestController
public class ChatController {

    private final ChatRoom chatRoom;

    public ChatController(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    /**
     * 发送消息接口
     *
     * @param from    发送者
     * @param to      接收者
     * @param message 消息内容
     * @return 状态提示
     */
    @GetMapping("/chat/send")
    public String sendMessage(@RequestParam String from,
                              @RequestParam String to,
                              @RequestParam String message) {

        User sender = new User(from, chatRoom);
        User receiver = new User(to, chatRoom);
        sender.send(to, message);
        return "消息发送完成：" + from + " -> " + to;
    }
}
