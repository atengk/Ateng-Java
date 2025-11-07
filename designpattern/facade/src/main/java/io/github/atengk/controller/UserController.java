package io.github.atengk.controller;


import io.github.atengk.facade.UserRegisterFacade;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户注册控制器
 */
@RestController
public class UserController {

    private final UserRegisterFacade userRegisterFacade;

    public UserController(UserRegisterFacade userRegisterFacade) {
        this.userRegisterFacade = userRegisterFacade;
    }

    @GetMapping("/register")
    public String register(@RequestParam String username, @RequestParam String email) {
        userRegisterFacade.register(username, email);
        return "用户注册成功！";
    }
}