package io.github.atengk.service;

import io.github.atengk.model.User;
import org.springframework.stereotype.Service;

/**
 * 邮件服务：发送欢迎邮件
 */
@Service
public class MailService {
    public void sendWelcomeEmail(User user) {
        System.out.println("【MailService】发送欢迎邮件至：" + user.getEmail());
    }
}
