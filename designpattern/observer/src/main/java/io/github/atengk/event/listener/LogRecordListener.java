package io.github.atengk.event.listener;

import io.github.atengk.event.UserRegisterEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 日志记录监听器
 * 用户注册后记录系统日志
 */
@Component
public class LogRecordListener {

    @EventListener
    public void handleUserRegister(UserRegisterEvent event) {
        System.out.println("【系统日志】记录用户注册事件：" + event.getUsername());
    }
}
