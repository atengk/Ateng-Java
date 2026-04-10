package io.github.atengk.basic.controller;

import io.github.atengk.basic.holder.UserContextHolder;
import io.github.atengk.basic.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户上下文测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class UserController {

    private final OrderService orderService;

    public UserController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 测试接口：创建订单并打印当前用户ID
     *
     * 访问：http://localhost:8080/test/user
     *
     * @return 返回当前用户ID
     */
    @GetMapping("/test/user")
    public String testUser() {
        orderService.createOrder();
        return "当前用户ID：" + UserContextHolder.getUserId();
    }
}
