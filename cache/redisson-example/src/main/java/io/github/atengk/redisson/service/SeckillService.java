package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 秒杀业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillService {

    private final DistributedLockService distributedLockService;

    private int stock = 10;

    /**
     * 秒杀下单
     *
     * @param userId 用户ID
     */
    public void seckill(Long userId) {

        String lockKey = "lock:seckill:stock";

        distributedLockService.executeWithLock(lockKey, 3, -1, () -> {

            if (stock <= 0) {
                log.warn("库存不足，userId={}", userId);
                throw new RuntimeException("库存不足");
            }

            stock--;

            log.info("扣减库存成功，userId={}，剩余库存={}", userId, stock);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        });

    }

}