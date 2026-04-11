package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * 计数器服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CounterService {

    private final RedissonClient redissonClient;

    /**
     * 自增计数
     *
     * @param key 计数Key
     * @return 当前值
     */
    public long increment(String key) {

        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);

        long value = atomicLong.incrementAndGet();

        log.info("计数器自增，key={}，value={}", key, value);

        return value;
    }

    /**
     * 获取当前值
     *
     * @param key 计数Key
     * @return 当前值
     */
    public long get(String key) {

        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);

        return atomicLong.get();
    }

    /**
     * 重置计数
     *
     * @param key 计数Key
     */
    public void reset(String key) {

        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);

        atomicLong.set(0);

        log.info("计数器已重置，key={}", key);
    }

}