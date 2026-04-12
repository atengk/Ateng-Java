package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * 黑白名单服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlackWhiteListService {

    private final RedissonClient redissonClient;

    private static final String BLACK_KEY = "black:list";
    private static final String WHITE_KEY = "white:list";

    /**
     * 添加黑名单
     */
    public void addBlack(String value) {

        RSet<String> set = redissonClient.getSet(BLACK_KEY);

        set.add(value);

        log.info("加入黑名单，value={}", value);
    }

    /**
     * 添加白名单
     */
    public void addWhite(String value) {

        RSet<String> set = redissonClient.getSet(WHITE_KEY);

        set.add(value);

        log.info("加入白名单，value={}", value);
    }

    /**
     * 是否在黑名单
     */
    public boolean isBlack(String value) {

        RSet<String> set = redissonClient.getSet(BLACK_KEY);

        return set.contains(value);
    }

    /**
     * 是否在白名单
     */
    public boolean isWhite(String value) {

        RSet<String> set = redissonClient.getSet(WHITE_KEY);

        return set.contains(value);
    }

}
