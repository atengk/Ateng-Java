package io.github.atengk.basic.holder;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志上下文工具类（MDC 简化版，基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class LogContextHolder {

    /**
     * 存储日志上下文（key-value）
     */
    private static final ThreadLocal<Map<String, String>> CONTEXT_THREAD_LOCAL =
            ThreadLocal.withInitial(HashMap::new);

    /**
     * 设置上下文
     *
     * @param key   键
     * @param value 值
     */
    public static void put(String key, String value) {
        CONTEXT_THREAD_LOCAL.get().put(key, value);
    }

    /**
     * 获取上下文值
     *
     * @param key 键
     * @return 值
     */
    public static String get(String key) {
        return CONTEXT_THREAD_LOCAL.get().get(key);
    }

    /**
     * 获取全部上下文
     */
    public static Map<String, String> getAll() {
        return CONTEXT_THREAD_LOCAL.get();
    }

    /**
     * 移除某个 key
     */
    public static void remove(String key) {
        CONTEXT_THREAD_LOCAL.get().remove(key);
    }

    /**
     * 清空
     */
    public static void clear() {
        CONTEXT_THREAD_LOCAL.remove();
    }
}
