package io.github.atengk.xss.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * XSS 测试接口
 */
@RestController
@RequestMapping("/xss")
public class XssTestController {

    /**
     * GET 请求测试
     * 示例：
     * /xss/get?name=<script>alert(1)</script>
     */
    @GetMapping("/get")
    public String testGet(@RequestParam String name) {
        return "GET 接收参数：" + name;
    }

    /**
     * POST JSON 测试
     */
    @PostMapping("/post")
    public Map<String, Object> testPost(@RequestBody Map<String, Object> body) {
        return body;
    }
}
