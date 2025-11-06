package io.github.atengk.event.listener;

import io.github.atengk.event.UserRegisterEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 邮件通知监听器
 * 当用户注册成功后自动发送欢迎邮件
 */
@Component
public class EmailNotificationListener {

    @EventListener
    public void handleUserRegister(UserRegisterEvent event) {
        System.out.println("【邮件通知】已向 " + event.getUsername() + " 发送欢迎邮件");
    }
}
