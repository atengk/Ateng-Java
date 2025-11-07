package io.github.atengk.service;

import io.github.atengk.model.UserProfile;
import org.springframework.stereotype.Service;

/**
 * 用户档案业务逻辑
 */
@Service
public class UserProfileService {

    public String register(UserProfile userProfile) {
        System.out.println("【UserProfileService】保存用户信息：" + userProfile);
        return "注册成功：" + userProfile.getUsername();
    }
}