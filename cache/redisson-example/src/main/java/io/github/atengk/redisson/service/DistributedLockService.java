package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedissonClient redissonClient;

    /**
     * 执行业务（带分布式锁）
     *
     * @param lockKey 锁Key
     * @param waitTime 最大等待时间（秒）
     * @param leaseTime 锁持有时间（秒，-1 表示自动续期）
     * @param business 业务逻辑
     */
    public void executeWithLock(String lockKey,
                                long waitTime,
                                long leaseTime,
                                Runnable business) {

        RLock lock = redissonClient.getLock(lockKey);

        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);

            if (!isLocked) {
                log.warn("获取分布式锁失败，lockKey={}", lockKey);
                throw new RuntimeException("系统繁忙，请稍后重试");
            }

            log.info("获取分布式锁成功，lockKey={}", lockKey);

            business.run();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断，lockKey={}", lockKey, e);
            throw new RuntimeException("系统异常");
        } finally {

            if (isLocked && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                    log.info("释放分布式锁成功，lockKey={}", lockKey);
                } catch (Exception e) {
                    log.error("释放分布式锁异常，lockKey={}", lockKey, e);
                }
            }

        }
    }

}