package io.github.atengk.redisson.service;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * 分布式会话服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final RedissonClient redissonClient;

    private static final String SESSION_KEY_PREFIX = "session:";

    /**
     * 创建会话
     *
     * @param token token
     * @param session 会话信息
     * @param ttl 过期时间（秒）
     */
    public void createSession(String token, UserSession session, long ttl) {

        RBucket<UserSession> bucket = redissonClient.getBucket(SESSION_KEY_PREFIX + token);

        bucket.set(session, ttl, TimeUnit.SECONDS);

        log.info("创建会话成功，token={}", token);
    }

    /**
     * 获取会话
     *
     * @param token token
     * @return 会话信息
     */
    public UserSession getSession(String token) {

        if (ObjectUtil.isEmpty(token)) {
            return null;
        }

        RBucket<UserSession> bucket = redissonClient.getBucket(SESSION_KEY_PREFIX + token);

        return bucket.get();
    }

    /**
     * 删除会话（登出）
     *
     * @param token token
     */
    public void deleteSession(String token) {

        RBucket<Object> bucket = redissonClient.getBucket(SESSION_KEY_PREFIX + token);

        bucket.delete();

        log.info("删除会话成功，token={}", token);
    }

    /**
     * 刷新过期时间
     *
     * @param token token
     * @param ttl 过期时间（秒）
     */
    public void refreshSession(String token, long ttl) {

        RBucket<Object> bucket = redissonClient.getBucket(SESSION_KEY_PREFIX + token);

        boolean success = bucket.expire(ttl, TimeUnit.SECONDS);

        if (success) {
            log.info("刷新会话成功，token={}", token);
        }

    }

    /**
     * 会话对象
     */
    @Data
    public static class UserSession implements Serializable {

        private Long userId;

        private String username;

    }

}
