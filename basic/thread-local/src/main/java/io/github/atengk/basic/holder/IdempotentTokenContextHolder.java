package io.github.atengk.basic.holder;

import cn.hutool.core.util.StrUtil;

/**
 * 幂等 Token 上下文工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class IdempotentTokenContextHolder {

    /**
     * 存储 Token
     */
    private static final ThreadLocal<String> TOKEN_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置 Token
     *
     * @param token 幂等 Token
     */
    public static void set(String token) {
        if (StrUtil.isNotBlank(token)) {
            TOKEN_THREAD_LOCAL.set(token);
        }
    }

    /**
     * 获取 Token
     *
     * @return Token
     */
    public static String get() {
        return TOKEN_THREAD_LOCAL.get();
    }

    /**
     * 清理
     */
    public static void clear() {
        TOKEN_THREAD_LOCAL.remove();
    }
}
