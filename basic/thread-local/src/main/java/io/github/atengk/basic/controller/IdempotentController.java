package io.github.atengk.basic.controller;

import io.github.atengk.basic.service.OrderIdempotentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 幂等测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class IdempotentController {

    private final OrderIdempotentService service;

    public IdempotentController(OrderIdempotentService service) {
        this.service = service;
    }

    /**
     * 测试接口
     * <p>
     * curl 示例：
     * curl -H "X-Idempotent-Token: abc123" http://localhost:8080/test/idempotent
     * <p>
     * 同一个 Token 重复请求会报错
     */
    @GetMapping("/test/idempotent")
    public String test() {
        return service.createOrder();
    }
}
