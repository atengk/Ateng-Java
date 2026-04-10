package io.github.atengk.basic.controller;

import io.github.atengk.basic.service.PermissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限控制器示例
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 登录态与权限测试接口
     * <p>
     * curl -H "X-USER-ID: u100" -H "X-USERNAME: tom" -H "X-TENANT-ID: t001" -H "X-TRACE-ID: abc123" http://localhost:10005/permission/check
     *
     * @return 结果
     */
    @GetMapping("/permission/check")
    public String check() {
        permissionService.checkPermission();
        return "ok";
    }
}
