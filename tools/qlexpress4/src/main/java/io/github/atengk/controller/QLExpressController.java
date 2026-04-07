package io.github.atengk.controller;

import io.github.atengk.util.QLExpressUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * QLExpress 测试接口
 *
 * @author Ateng
 * @date 2026/04/07
 */
@RestController
@RequestMapping("/ql")
@RequiredArgsConstructor
public class QLExpressController {

    private final QLExpressUtil qlExpressUtil;

    /**
     * 测试表达式执行
     */
    @GetMapping("/test")
    public Object test() {

        Map<String, Object> params = new HashMap<>();
        params.put("a", 10);
        params.put("b", 20);

        String expression = "a + b * 2";

        return qlExpressUtil.execute(expression, params);
    }
}