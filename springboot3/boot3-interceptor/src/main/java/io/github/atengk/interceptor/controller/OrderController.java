package io.github.atengk.interceptor.controller;

import io.github.atengk.interceptor.annotation.RepeatSubmit;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试接口
 */
@RestController
public class OrderController {

    /**
     * 模拟下单接口（防重复提交）
     */
    @RepeatSubmit(expire = 10) // 10秒内不能重复提交
    @PostMapping("/order")
    public String createOrder() {

        // 模拟业务处理
        return "下单成功";
    }
}
