package io.github.atengk.redisson.service;


import cn.hutool.core.date.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RHyperLogLog;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * UV统计服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UvService {

    private final RedissonClient redissonClient;

    private static final String UV_KEY_PREFIX = "uv:";

    /**
     * 记录访问（按天）
     *
     * @param bizKey 业务标识（如：首页、商品页）
     * @param userFlag 用户标识（如IP、userId）
     */
    public void record(String bizKey, String userFlag) {

        String date = DateUtil.formatDate(new Date());

        String key = UV_KEY_PREFIX + bizKey + ":" + date;

        RHyperLogLog<String> hyperLogLog = redissonClient.getHyperLogLog(key);

        hyperLogLog.add(userFlag);

        log.info("记录UV成功，key={}，userFlag={}", key, userFlag);
    }

    /**
     * 获取当日UV
     *
     * @param bizKey 业务标识
     * @return UV数量
     */
    public long countToday(String bizKey) {

        String date = DateUtil.formatDate(new Date());

        String key = UV_KEY_PREFIX + bizKey + ":" + date;

        RHyperLogLog<String> hyperLogLog = redissonClient.getHyperLogLog(key);

        return hyperLogLog.count();
    }

}
