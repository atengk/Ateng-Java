package local.ateng.java.aop.controller;

import local.ateng.java.aop.annotation.PermissionCheck;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/permission")
public class PermissionController {

    // 模拟的接口：只有 "ADMIN" 权限的用户可以访问
    @PermissionCheck(value = "ADMIN")  // 需要 ADMIN 权限
    @GetMapping("/admin")
    public String getAdminInfo() {
        return "管理员信息";
    }

    // 模拟的接口：只有 "USER" 权限的用户可以访问
    @PermissionCheck(value = "USER")  // 需要 USER 权限
    @GetMapping("/user")
    public String getUserInfo() {
        return "用户信息";
    }

}
