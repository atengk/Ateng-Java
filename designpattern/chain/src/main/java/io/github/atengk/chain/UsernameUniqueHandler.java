package io.github.atengk.chain;

import io.github.atengk.model.User;
import io.github.atengk.service.UserService;
import org.springframework.stereotype.Component;

/**
 * 校验用户名不能重复
 */
@Component
public class UsernameUniqueHandler extends AbstractHandler {

    private final UserService userService;

    public UsernameUniqueHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doHandle(User user) {
        if (userService.exists(user.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        System.out.println("【校验通过】用户名未重复");
    }
}
