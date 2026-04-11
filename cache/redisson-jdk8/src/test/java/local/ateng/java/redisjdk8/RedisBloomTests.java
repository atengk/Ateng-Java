package local.ateng.java.redisjdk8;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Redisson RedisBloom 模块使用测试类
 *
 * 用于测试 BloomFilter / CuckooFilter / CountMinSketch / TopK 等概率数据结构能力
 *
 * @author ateng
 * @since 2026-04-11
 */
@Slf4j
@SpringBootTest
public class RedisBloomTests {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 1. BloomFilter 基础用法：判断是否存在（去重场景）
     */
    @Test
    void bloomFilter_basic() {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("bf:user");

        // 初始化：预计插入 100000 数据，误判率 0.01
        bloomFilter.tryInit(100000, 0.01);

        bloomFilter.add("user:1001");

        boolean exists = bloomFilter.contains("user:1001");

        log.info("BloomFilter 判断结果 exists={}", exists);
    }

    /**
     * 2. BloomFilter 解决缓存穿透（典型工程用法）
     */
    @Test
    void bloomFilter_cachePenetration() {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("bf:order");

        bloomFilter.tryInit(1000000, 0.01);

        // 预先写入合法ID（通常来自DB初始化）
        bloomFilter.add(10001L);
        bloomFilter.add(10002L);

        Long queryId = 10003L;

        // 防穿透判断
        if (!bloomFilter.contains(queryId)) {
            log.warn("非法请求，直接拦截 id={}", queryId);
            return;
        }

        log.info("允许访问 id={}", queryId);
    }

    /**
     * 3. CuckooFilter：通过 Redis 原生命令（CF.*）
     *
     * 说明：
     * Redisson 不提供 API，需要 RScript 执行
     */
    @Test
    void cuckoo_filter_by_script() {
        String script = ""
                + "return redis.call('CF.ADD', KEYS[1], ARGV[1])";

        redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE,
                        script,
                        RScript.ReturnType.INTEGER,
                        java.util.Collections.singletonList("cf:user"),
                        "user:1001");

        log.info("CuckooFilter 插入完成");
    }

    /**
     * 4. Count-Min Sketch：通过 Redis 原生命令（CMS.INCRBY / CMS.QUERY）
     *
     * 用于：UV / PV / 访问计数（近似统计）
     */
    @Test
    void count_min_sketch_by_script() {
        String key = "cms:visit";

        // 初始化 CMS（只需执行一次）
        redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                "return redis.call('CMS.INITBYPROB', KEYS[1], 0.001, 0.01)",
                RScript.ReturnType.STATUS,
                java.util.Collections.singletonList(key)
        );

        // 计数
        redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                "return redis.call('CMS.INCRBY', KEYS[1], ARGV[1], 1)",
                RScript.ReturnType.INTEGER,
                java.util.Collections.singletonList(key),
                "user:1001"
        );

        log.info("CMS 计数完成");
    }

    /**
     * 5. TopK：热点排行（通过 Redis 原生命令）
     *
     * 用于：热词 / 热用户 / 热商品
     */
    @Test
    void topk_by_script() {
        String key = "topk:keyword";

        // 初始化 TopK（只需一次）
        redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                "return redis.call('TOPK.RESERVE', KEYS[1], 10, 200, 7, 0.9)",
                RScript.ReturnType.STATUS,
                java.util.Collections.singletonList(key)
        );

        // 添加数据
        redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                "return redis.call('TOPK.ADD', KEYS[1], ARGV[1])",
                RScript.ReturnType.INTEGER,
                java.util.Collections.singletonList(key),
                "java"
        );

        log.info("TopK 更新完成");
    }
}
