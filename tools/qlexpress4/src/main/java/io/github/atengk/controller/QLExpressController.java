package io.github.atengk.controller;

import io.github.atengk.service.QLExpressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private final QLExpressService service;

    /**
     * 表达式执行
     */
    @PostMapping("/exec")
    public Object exec(@RequestBody Map<String, Object> req) {
        String expression = (String) req.get("expression");
        Map<String, Object> params = (Map<String, Object>) req.get("params");

        return service.execute(expression, params);
    }

}