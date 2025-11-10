package io.github.atengk.service.mediator;


/**
 * 聊天中介者接口
 */
public interface ChatRoom {

    /**
     * 注册用户
     *
     * @param user 用户实例
     */
    void registerUser(ChatUser user);

    /**
     * 发送消息给指定用户
     *
     * @param from    发送者用户名
     * @param to      接收者用户名
     * @param message 消息内容
     */
    void sendMessage(String from, String to, String message);
}
