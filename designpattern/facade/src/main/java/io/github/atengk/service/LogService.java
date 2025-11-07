package io.github.atengk.service;

import io.github.atengk.model.User;
import org.springframework.stereotype.Service;

/**
 * 日志服务：记录操作日志
 */
@Service
public class LogService {
    public void recordRegisterLog(User user) {
        System.out.println("【LogService】记录注册日志：" + user.getUsername());
    }
}