package io.github.atengk.service;

import io.github.atengk.model.User;
import org.springframework.stereotype.Service;

/**
 * 用户服务：处理用户注册
 */
@Service
public class UserService {
    public void saveUser(User user) {
        System.out.println("【UserService】用户注册成功：" + user.getUsername());
    }
}