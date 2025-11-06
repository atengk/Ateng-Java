package io.github.atengk.event.listener;

import io.github.atengk.event.UserRegisterEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 数据统计监听器
 * 用户注册后更新注册统计数据
 */
@Component
public class StatisticUpdateListener {

    @EventListener
    public void handleUserRegister(UserRegisterEvent event) {
        System.out.println("【数据统计】已更新用户注册数量：" + event.getUsername());
    }
}
