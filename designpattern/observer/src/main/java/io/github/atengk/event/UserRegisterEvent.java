package io.github.atengk.event;

import org.springframework.context.ApplicationEvent;

/**
 * 用户注册事件（观察者模式中的“主题”）
 */
public class UserRegisterEvent extends ApplicationEvent {

    private final String username;

    public UserRegisterEvent(Object source, String username) {
        super(source);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
