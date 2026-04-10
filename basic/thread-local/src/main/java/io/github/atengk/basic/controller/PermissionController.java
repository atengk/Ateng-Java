package io.github.atengk.basic.controller;

import io.github.atengk.basic.service.OrderPermissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class PermissionController {

    private final OrderPermissionService service;

    public PermissionController(OrderPermissionService service) {
        this.service = service;
    }

    /**
     * 测试创建订单权限
     *
     * http://localhost:8080/test/permission/create
     */
    @GetMapping("/test/permission/create")
    public String create() {
        return service.createOrder();
    }

    /**
     * 测试查看订单权限
     *
     * http://localhost:8080/test/permission/view
     */
    @GetMapping("/test/permission/view")
    public String view() {
        return service.viewOrder();
    }
}
