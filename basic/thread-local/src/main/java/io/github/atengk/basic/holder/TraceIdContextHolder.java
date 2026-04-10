package io.github.atengk.basic.holder;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.IdUtil;

/**
 * TraceId 上下文工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class TraceIdContextHolder {

    /**
     * 存储 TraceId
     */
    private static final ThreadLocal<String> TRACE_ID_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置 TraceId
     *
     * @param traceId 链路ID
     */
    public static void set(String traceId) {
        if (StrUtil.isNotBlank(traceId)) {
            TRACE_ID_THREAD_LOCAL.set(traceId);
        }
    }

    /**
     * 获取 TraceId
     *
     * @return TraceId
     */
    public static String get() {
        return TRACE_ID_THREAD_LOCAL.get();
    }

    /**
     * 获取或生成 TraceId（推荐方法）
     *
     * @return TraceId
     */
    public static String getOrCreate() {
        String traceId = get();
        if (StrUtil.isBlank(traceId)) {
            traceId = IdUtil.fastSimpleUUID();
            set(traceId);
        }
        return traceId;
    }

    /**
     * 清理（必须调用）
     */
    public static void clear() {
        TRACE_ID_THREAD_LOCAL.remove();
    }
}
