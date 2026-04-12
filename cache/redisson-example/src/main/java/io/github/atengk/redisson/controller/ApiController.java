package io.github.atengk.redisson.controller;


import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.redisson.service.ApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 滑动窗口限流测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final ApiService apiService;

    /**
     * 接口调用（1分钟最多10次）
     * <p>
     * curl "http://localhost:8080/api/call?userId=1"
     */
    @GetMapping("/call")
    public Object call(@RequestParam String userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        return apiService.call(userId);
    }

}