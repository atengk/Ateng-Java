package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 购物车服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final RedissonClient redissonClient;

    private static final String CART_KEY_PREFIX = "cart:";

    /**
     * 加入购物车（增加数量）
     *
     * @param userId 用户ID
     * @param productId 商品ID
     * @param count 数量
     */
    public void add(Long userId, Long productId, int count) {

        String key = CART_KEY_PREFIX + userId;

        RMap<Long, Integer> map = redissonClient.getMap(key);

        map.addAndGet(productId, count);

        log.info("加入购物车，userId={}，productId={}，count={}", userId, productId, count);
    }

    /**
     * 减少数量
     *
     * @param userId 用户ID
     * @param productId 商品ID
     * @param count 数量
     */
    public void decrease(Long userId, Long productId, int count) {

        String key = CART_KEY_PREFIX + userId;

        RMap<Long, Integer> map = redissonClient.getMap(key);

        long result = map.addAndGet(productId, -count);

        if (result <= 0) {
            map.remove(productId);
        }

        log.info("减少购物车商品，userId={}，productId={}，count={}", userId, productId, count);
    }

    /**
     * 查询购物车
     *
     * @param userId 用户ID
     * @return 商品列表
     */
    public Map<Long, Integer> list(Long userId) {

        String key = CART_KEY_PREFIX + userId;

        RMap<Long, Integer> map = redissonClient.getMap(key);

        return map.readAllMap();
    }

    /**
     * 清空购物车
     *
     * @param userId 用户ID
     */
    public void clear(Long userId) {

        String key = CART_KEY_PREFIX + userId;

        RMap<Long, Integer> map = redissonClient.getMap(key);

        map.delete();

        log.info("清空购物车，userId={}", userId);
    }

}
