package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * 秒杀库存服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillStockService {

    private final RedissonClient redissonClient;

    private static final String STOCK_KEY_PREFIX = "stock:";

    /**
     * 初始化库存
     *
     * @param productId 商品ID
     * @param stock 库存
     */
    public void initStock(Long productId, long stock) {

        RAtomicLong atomicLong = redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId);

        atomicLong.set(stock);

        log.info("初始化库存成功，productId={}，stock={}", productId, stock);
    }

    /**
     * 扣减库存
     *
     * @param productId 商品ID
     * @return 是否成功
     */
    public boolean deduct(Long productId) {

        RAtomicLong atomicLong = redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId);

        long remain = atomicLong.decrementAndGet();

        if (remain < 0) {
            atomicLong.incrementAndGet();
            log.warn("库存不足，productId={}", productId);
            return false;
        }

        log.info("扣减库存成功，productId={}，剩余库存={}", productId, remain);

        return true;
    }

    /**
     * 查询库存
     */
    public long getStock(Long productId) {

        RAtomicLong atomicLong = redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId);

        return atomicLong.get();
    }

}
