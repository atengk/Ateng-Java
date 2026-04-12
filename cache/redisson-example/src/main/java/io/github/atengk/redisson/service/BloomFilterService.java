package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * 布隆过滤器服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BloomFilterService {

    private final RedissonClient redissonClient;

    private static final String BLOOM_KEY = "bf:user";

    /**
     * 初始化布隆过滤器
     *
     * @param expectedInsertions 预计元素数量
     * @param falseProbability 误判率
     */
    public void init(long expectedInsertions, double falseProbability) {

        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(BLOOM_KEY);

        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(expectedInsertions, falseProbability);
            log.info("初始化布隆过滤器成功");
        }

    }

    /**
     * 添加元素
     */
    public void add(Long userId) {

        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(BLOOM_KEY);

        bloomFilter.add(userId);

        log.info("添加元素到布隆过滤器，userId={}", userId);
    }

    /**
     * 判断是否存在
     */
    public boolean contains(Long userId) {

        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(BLOOM_KEY);

        return bloomFilter.contains(userId);
    }

}
