package io.github.atengk.controller;


import io.github.atengk.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 状态模式测试控制器
 */
@RestController
public class StateController {

    private final OrderService orderService;

    public StateController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/state/test")
    public String testStatePattern() {
        orderService.initOrder();
        orderService.payOrder();
        orderService.shipOrder();
        orderService.completeOrder();
        return "订单状态流转测试完成，请查看控制台输出";
    }
}