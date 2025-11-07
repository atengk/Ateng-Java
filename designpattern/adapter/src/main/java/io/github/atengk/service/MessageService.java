package io.github.atengk.service;


import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 消息服务：通过适配器统一调用不同消息渠道
 */
@Service
public class MessageService {

    private final Map<String, MessageSender> senderMap;

    public MessageService(Map<String, MessageSender> senderMap) {
        this.senderMap = senderMap;
    }

    public void send(String channel, String userId, String message) {
        MessageSender sender = senderMap.get(channel);
        if (sender == null) {
            throw new IllegalArgumentException("未知的消息渠道：" + channel);
        }
        sender.sendMessage(userId, message);
    }
}