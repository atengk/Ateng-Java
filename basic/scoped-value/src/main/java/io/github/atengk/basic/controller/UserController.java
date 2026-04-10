package io.github.atengk.basic.controller;

import io.github.atengk.basic.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制层示例
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 测试接口
     *
     * curl -H "X-USER-ID: abc123" -H "X-TRACE-ID: abc123" http://localhost:10005/test
     *
     * @return 结果
     */
    @GetMapping("/test")
    public String test() {
        userService.process();
        return "ok";
    }
}
