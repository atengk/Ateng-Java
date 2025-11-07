package io.github.atengk.chain;


import io.github.atengk.model.User;
import org.springframework.stereotype.Component;

/**
 * 校验邮箱格式合法
 */
@Component
public class EmailFormatHandler extends AbstractHandler {

    @Override
    protected void doHandle(User user) {
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        System.out.println("【校验通过】邮箱格式合法");
    }
}
