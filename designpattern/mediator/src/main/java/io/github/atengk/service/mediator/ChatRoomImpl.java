package io.github.atengk.service.mediator;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 聊天中介者实现
 */
@Component
public class ChatRoomImpl implements ChatRoom {

    private final Map<String, ChatUser> users = new HashMap<>();

    @Override
    public void registerUser(ChatUser user) {
        users.put(user.getName(), user);
    }

    @Override
    public void sendMessage(String from, String to, String message) {
        ChatUser user = users.get(to);
        if (user != null) {
            user.receive(from, message);
        } else {
            System.out.println("用户 " + to + " 不存在！");
        }
    }
}
