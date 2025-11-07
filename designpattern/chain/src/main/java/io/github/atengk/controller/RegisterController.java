package io.github.atengk.controller;

import io.github.atengk.chain.UsernameNotEmptyHandler;
import io.github.atengk.model.User;
import io.github.atengk.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户注册控制器
 */
@RestController
@RequestMapping("/chain")
public class RegisterController {

    private final UsernameNotEmptyHandler handlerChain;
    private final UserService userService;

    public RegisterController(UsernameNotEmptyHandler handlerChain, UserService userService) {
        this.handlerChain = handlerChain;
        this.userService = userService;
    }

    @PostMapping("/register")
    public String register(@RequestBody User user) {
        handlerChain.handle(user);
        userService.save(user.getUsername());
        return "注册成功：" + user.getUsername();
    }
}
