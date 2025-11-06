package io.github.atengk.controller;

import io.github.atengk.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制层，用于触发注册流程
 */
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String register(String username) {
        userService.registerUser(username);
        return "用户注册完成";
    }
}
