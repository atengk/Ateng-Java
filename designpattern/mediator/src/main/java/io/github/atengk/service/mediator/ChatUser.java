package io.github.atengk.service.mediator;

/**
 * 聊天用户（同事类）
 */
public abstract class ChatUser {

    protected final String name;
    protected final ChatRoom chatRoom;

    /**
     * 构造方法
     *
     * @param name     用户名
     * @param chatRoom 中介者实例
     */
    protected ChatUser(String name, ChatRoom chatRoom) {
        this.name = name;
        this.chatRoom = chatRoom;
        chatRoom.registerUser(this);
    }

    /**
     * 发送消息
     *
     * @param to      接收者用户名
     * @param message 消息内容
     */
    public abstract void send(String to, String message);

    /**
     * 接收消息
     *
     * @param from    发送者用户名
     * @param message 消息内容
     */
    public abstract void receive(String from, String message);

    public String getName() {
        return name;
    }
}
