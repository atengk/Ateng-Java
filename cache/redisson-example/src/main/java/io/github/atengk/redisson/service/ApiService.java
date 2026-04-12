package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 精准限流业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Service
@RequiredArgsConstructor
public class ApiService {

    private final SlidingWindowRateLimiterService rateLimiterService;

    /**
     * 接口调用
     */
    public String call(String userId) {

        String key = "sliding:api:" + userId;

        boolean allowed = rateLimiterService.allow(key, 60000, 10);

        if (!allowed) {
            return "请求过于频繁";
        }

        return "调用成功";
    }

}
