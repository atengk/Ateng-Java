package io.github.atengk.crypto.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/crypto")
public class TestController {

    @GetMapping("/get")
    public String testGet(@RequestParam String name) {
        return "GET 接收参数：" + name;
    }

    @PostMapping("/post")
    public Map<String, Object> testPost(@RequestBody Map<String, Object> body) {
        return body;
    }
}
