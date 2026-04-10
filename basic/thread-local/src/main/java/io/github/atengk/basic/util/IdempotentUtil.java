package io.github.atengk.basic.util;

import cn.hutool.core.util.StrUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 幂等校验工具类（示例：本地缓存实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class IdempotentUtil {

    /**
     * 已使用 Token 缓存（生产建议使用 Redis）
     */
    private static final Set<String> TOKEN_CACHE = ConcurrentHashMap.newKeySet();

    /**
     * 校验并标记 Token
     *
     * @param token 幂等 Token
     */
    public static void checkAndSave(String token) {

        if (StrUtil.isBlank(token)) {
            throw new RuntimeException("幂等 Token 不能为空");
        }

        // 如果已存在，说明重复请求
        if (!TOKEN_CACHE.add(token)) {
            throw new RuntimeException("重复请求");
        }
    }
}
