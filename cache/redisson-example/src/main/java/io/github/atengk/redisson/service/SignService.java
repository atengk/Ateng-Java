package io.github.atengk.redisson.service;

import cn.hutool.core.date.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 用户签到服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignService {

    private final RedissonClient redissonClient;

    private static final String SIGN_KEY_PREFIX = "sign:";

    /**
     * 签到
     *
     * @param userId 用户ID
     */
    public void sign(Long userId) {

        Date today = new Date();

        int dayOfYear = DateUtil.dayOfYear(today);

        int year = DateUtil.year(today);

        String key = SIGN_KEY_PREFIX + userId + ":" + year;

        RBitSet bitSet = redissonClient.getBitSet(key);

        bitSet.set(dayOfYear);

        log.info("签到成功，userId={}，dayOfYear={}", userId, dayOfYear);
    }

    /**
     * 判断当天是否签到
     *
     * @param userId 用户ID
     * @return 是否签到
     */
    public boolean isSigned(Long userId) {

        Date today = new Date();

        int dayOfYear = DateUtil.dayOfYear(today);

        int year = DateUtil.year(today);

        String key = SIGN_KEY_PREFIX + userId + ":" + year;

        RBitSet bitSet = redissonClient.getBitSet(key);

        return bitSet.get(dayOfYear);
    }

    /**
     * 获取当年签到天数
     *
     * @param userId 用户ID
     * @return 签到天数
     */
    public long getSignCount(Long userId) {

        int year = DateUtil.year(new Date());

        String key = SIGN_KEY_PREFIX + userId + ":" + year;

        RBitSet bitSet = redissonClient.getBitSet(key);

        return bitSet.cardinality();
    }

    /**
     * 获取连续签到天数（从今天往前）
     *
     * @param userId 用户ID
     * @return 连续天数
     */
    public int getContinuousSignCount(Long userId) {

        Date today = new Date();

        int dayOfYear = DateUtil.dayOfYear(today);

        int year = DateUtil.year(today);

        String key = SIGN_KEY_PREFIX + userId + ":" + year;

        RBitSet bitSet = redissonClient.getBitSet(key);

        int count = 0;

        for (int i = dayOfYear; i > 0; i--) {

            if (bitSet.get(i)) {
                count++;
            } else {
                break;
            }

        }

        return count;
    }

}
