package io.github.atengk.crypto.util;


import cn.hutool.core.util.StrUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 防重放工具类
 *
 * @author 孔余
 * @since 2026-01-29
 */
public class ReplayAttackUtil {

    private static final long EXPIRE_TIME = 5 * 60 * 1000;

    /**
     * 校验时间戳
     */
    public static void checkTimestamp(Long timestamp) {

        long now = System.currentTimeMillis();

        if (timestamp == null || Math.abs(now - timestamp) > EXPIRE_TIME) {
            throw new RuntimeException("请求已过期");
        }
    }

    /**
     * 校验 nonce（必须唯一）
     */
    public static void checkNonce(String nonce, StringRedisTemplate redisTemplate) {

        if (StrUtil.isBlank(nonce)) {
            throw new RuntimeException("nonce 不能为空");
        }

        String key = "crypto:nonce:" + nonce;

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofMinutes(5));

        if (Boolean.FALSE.equals(success)) {
            throw new RuntimeException("重复请求");
        }
    }
}
