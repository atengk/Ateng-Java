package io.github.atengk.service;

import io.github.atengk.model.User;
import org.springframework.stereotype.Service;

/**
 * 积分服务：注册赠送积分
 */
@Service
public class PointService {
    public void addRegisterPoints(User user) {
        System.out.println("【PointService】用户 " + user.getUsername() + " 获得新手积分：100");
    }
}
