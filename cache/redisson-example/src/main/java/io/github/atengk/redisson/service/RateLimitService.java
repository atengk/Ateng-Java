package io.github.atengk.redisson.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * 分布式限流服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedissonClient redissonClient;

    /**
     * 尝试获取令牌
     *
     * @param key 限流Key
     * @param permits 每秒允许的请求数
     * @return 是否允许通过
     */
    public boolean tryAcquire(String key, long permits) {

        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

        if (!rateLimiter.isExists()) {
            rateLimiter.trySetRate(RateType.OVERALL, permits, 1, RateIntervalUnit.SECONDS);
        }

        boolean result = rateLimiter.tryAcquire(1);

        if (!result) {
            log.warn("触发限流，key={}", key);
        }

        return result;
    }

}
