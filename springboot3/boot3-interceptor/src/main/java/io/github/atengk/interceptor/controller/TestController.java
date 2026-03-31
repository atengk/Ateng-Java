package io.github.atengk.interceptor.controller;

import com.alibaba.fastjson2.JSONObject;
import io.github.atengk.interceptor.annotation.RateLimit;
import io.github.atengk.interceptor.annotation.SlidingWindowLimit;
import org.springframework.web.bind.annotation.*;

/**
 * 测试接口
 */
@RestController
public class TestController {

    @PostMapping("/test")
    public String test(@RequestBody JSONObject body) {
        return "ok";
    }

    /**
     * 10秒内最多5次（滑动窗口）
     */
    @SlidingWindowLimit(time = 10, count = 5)
    @GetMapping("/limit/sw")
    public String test() {
        return "请求成功";
    }
}
