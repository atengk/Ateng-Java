package io.github.atengk.service.mediator;

/**
 * 聊天用户实现
 */
public class User extends ChatUser {

    public User(String name, ChatRoom chatRoom) {
        super(name, chatRoom);
    }

    @Override
    public void send(String to, String message) {
        System.out.println("【" + name + " 发送】-> " + to + ": " + message);
        chatRoom.sendMessage(name, to, message);
    }

    @Override
    public void receive(String from, String message) {
        System.out.println("【" + name + " 接收】<- " + from + ": " + message);
    }
}
