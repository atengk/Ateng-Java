package io.github.atengk.crypto.controller;

import io.github.atengk.crypto.annotation.Crypto;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/crypto")
public class TestController {

    @Crypto
    @GetMapping("/get")
    public String get(@RequestParam String name) {
        return "GET 接收参数：" + name;
    }

    @Crypto(decrypt = true, encrypt = false)
    @PostMapping("/post")
    public Map<String, Object> post(@RequestBody Map<String, Object> body) {
        return body;
    }

    @PostMapping("/ignore")
    public Map<String, Object> ignore(@RequestBody Map<String, Object> body) {
        return body;
    }
}
