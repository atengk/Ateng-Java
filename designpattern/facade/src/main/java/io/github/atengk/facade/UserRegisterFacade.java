package io.github.atengk.facade;


import io.github.atengk.model.User;
import io.github.atengk.service.LogService;
import io.github.atengk.service.MailService;
import io.github.atengk.service.PointService;
import io.github.atengk.service.UserService;
import org.springframework.stereotype.Component;

/**
 * 外观类：对外暴露统一的注册接口
 */
@Component
public class UserRegisterFacade {

    private final UserService userService;
    private final MailService mailService;
    private final PointService pointService;
    private final LogService logService;

    public UserRegisterFacade(UserService userService,
                              MailService mailService,
                              PointService pointService,
                              LogService logService) {
        this.userService = userService;
        this.mailService = mailService;
        this.pointService = pointService;
        this.logService = logService;
    }

    /**
     * 一键注册：封装整个注册流程
     * @param username 用户名
     * @param email 邮箱
     */
    public void register(String username, String email) {
        User user = new User(username, email);
        userService.saveUser(user);
        mailService.sendWelcomeEmail(user);
        pointService.addRegisterPoints(user);
        logService.recordRegisterLog(user);
        System.out.println("【UserRegisterFacade】注册流程完成！");
    }
}