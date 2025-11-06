package io.github.atengk.service;

import io.github.atengk.event.UserRegisterEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 用户业务服务
 * 负责用户注册并发布注册事件
 */
@Service
public class UserService {

    private final ApplicationEventPublisher eventPublisher;

    public UserService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 用户注册逻辑
     *
     * @param username 用户名
     */
    public void registerUser(String username) {
        System.out.println("【用户注册】用户 " + username + " 注册成功");

        // 发布注册事件
        eventPublisher.publishEvent(new UserRegisterEvent(this, username));
    }
}
