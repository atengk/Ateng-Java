package io.github.atengk.interceptor.controller;

import io.github.atengk.interceptor.annotation.RequireRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试接口
 */
@RestController
public class AdminController {

    /**
     * 仅 ADMIN 可访问
     */
    @RequireRole({"ADMIN"})
    @GetMapping("/admin")
    public String admin() {
        return "管理员接口";
    }

    /**
     * ADMIN / USER 均可访问
     */
    @RequireRole({"ADMIN", "USER"})
    @GetMapping("/user")
    public String user() {
        return "用户接口";
    }
}
