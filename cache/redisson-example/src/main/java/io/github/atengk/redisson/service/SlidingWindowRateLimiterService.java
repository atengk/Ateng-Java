package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 滑动窗口限流服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlidingWindowRateLimiterService {

    private final RedissonClient redissonClient;

    /**
     * 是否允许请求
     *
     * @param key        限流Key
     * @param windowSize 窗口大小（毫秒）
     * @param maxCount   窗口内最大请求数
     * @return 是否允许
     */
    public boolean allow(String key, long windowSize, int maxCount) {

        long now = System.currentTimeMillis();

        long windowStart = now - windowSize;

        RScoredSortedSet<Long> zSet = redissonClient.getScoredSortedSet(key);

        zSet.removeRangeByScore(0, true, windowStart, true);

        int current = zSet.size();

        if (current >= maxCount) {
            log.warn("触发滑动窗口限流，key={}", key);
            return false;
        }

        zSet.add(now, now);

        zSet.expire(Duration.ofMillis(windowSize));

        return true;
    }

}