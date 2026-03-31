package io.github.atengk.interceptor.controller;

import io.github.atengk.interceptor.annotation.SignCheck;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试接口
 */
@RestController
public class ApiController {

    /**
     * 需要签名校验
     */
    @SignCheck(expire = 60)
    @GetMapping("/api/test")
    public String test() {
        return "访问成功";
    }
}
