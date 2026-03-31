package io.github.atengk.interceptor.controller;

import io.github.atengk.interceptor.annotation.ValidateParam;
import io.github.atengk.interceptor.context.UserContext;
import io.github.atengk.interceptor.dto.UserDTO;
import org.springframework.web.bind.annotation.*;

/**
 * 测试接口
 */
@RestController
public class UserController {

    /**
     * 创建用户（参数校验）
     */
    @ValidateParam
    @PostMapping("/user/create")
    public String createUser(@RequestBody UserDTO userDTO) {

        return "创建成功";
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/user/info")
    public String getUserInfo() {

        Long userId = UserContext.getUserId();
        String username = UserContext.getUsername();

        return "用户ID：" + userId + "，用户名：" + username;
    }
}
