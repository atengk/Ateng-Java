package io.github.atengk.crypto.util;

import cn.hutool.core.util.ObjectUtil;
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

    /**
     * 默认时间窗口（毫秒）
     */
    private static final long DEFAULT_EXPIRE_TIME = 5 * 60 * 1000;

    /**
     * 允许的最大未来偏移（毫秒）
     */
    private static final long MAX_FUTURE_TIME = 60 * 1000;

    /**
     * Redis Key 前缀
     */
    private static final String NONCE_PREFIX = "crypto:nonce:";

    /**
     * 校验时间戳
     */
    public static void checkTimestamp(Long timestamp) {

        checkTimestamp(timestamp, DEFAULT_EXPIRE_TIME);
    }

    /**
     * 校验时间戳（支持自定义窗口）
     */
    public static void checkTimestamp(Long timestamp, long expireTime) {

        if (ObjectUtil.isNull(timestamp)) {
            throw new RuntimeException("timestamp 不能为空");
        }

        long now = System.currentTimeMillis();

        /*
         * 1. 防止过期请求
         */
        if (now - timestamp > expireTime) {
            throw new RuntimeException("请求已过期");
        }

        /*
         * 2. 防止未来时间攻击（客户端时间伪造）
         */
        if (timestamp - now > MAX_FUTURE_TIME) {
            throw new RuntimeException("非法请求（时间异常）");
        }
    }

    /**
     * 校验 nonce（默认）
     */
    public static void checkNonce(String nonce,
                                  StringRedisTemplate redisTemplate) {

        checkNonce(null, nonce, redisTemplate, DEFAULT_EXPIRE_TIME);
    }

    /**
     * 校验 nonce（支持 appId 隔离）
     */
    public static void checkNonce(String appId,
                                  String nonce,
                                  StringRedisTemplate redisTemplate,
                                  long expireTime) {

        if (StrUtil.isBlank(nonce)) {
            throw new RuntimeException("nonce 不能为空");
        }

        /*
         * 构建 Redis Key
         * 格式：
         * crypto:nonce:{appId}:{nonce}
         */
        String key = buildNonceKey(appId, nonce);

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofMillis(expireTime));

        /*
         * null 也视为失败（极端情况）
         */
        if (!Boolean.TRUE.equals(success)) {
            throw new RuntimeException("重复请求");
        }
    }

    /**
     * 构建 nonce key
     */
    private static String buildNonceKey(String appId, String nonce) {

        if (StrUtil.isBlank(appId)) {
            return NONCE_PREFIX + nonce;
        }

        return NONCE_PREFIX + appId + ":" + nonce;
    }
}