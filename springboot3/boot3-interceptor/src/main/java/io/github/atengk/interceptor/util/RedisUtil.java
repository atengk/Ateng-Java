package io.github.atengk.interceptor.util;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 */
@Component
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;

    public RedisUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * setIfAbsent（SETNX）
     */
    public boolean setIfAbsent(String key, String value, long expireSeconds) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, value, expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 防重放（SETNX）
     */
    public boolean setIfAbsent(String key, long expireSeconds) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 自增并设置过期时间
     */
    public Long increment(String key, long expireSeconds) {

        Long count = redisTemplate.opsForValue().increment(key);

        // 第一次设置过期时间
        if (count != null && count == 1) {
            redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
        }

        return count;
    }

    /**
     * 滑动窗口计数
     */
    public Long slidingWindowCount(String key, long windowSeconds) {

        long now = System.currentTimeMillis();
        long windowStart = now - windowSeconds * 1000;

        // 删除窗口外数据
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // 当前请求加入
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);

        // 设置过期时间
        redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);

        // 返回当前窗口内数量
        return redisTemplate.opsForZSet().zCard(key);
    }

}
