package io.github.atengk.chain;


import io.github.atengk.model.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 校验用户名不能为空
 */
@Component
public class UsernameNotEmptyHandler extends AbstractHandler {

    @Override
    protected void doHandle(User user) {
        if (!StringUtils.hasText(user.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        System.out.println("【校验通过】用户名不为空");
    }
}
