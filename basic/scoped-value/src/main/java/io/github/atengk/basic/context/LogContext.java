package io.github.atengk.basic.context;

/**
 * 日志上下文工具类
 *
 * 提供统一获取 TraceId 的能力
 *
 * @author Ateng
 * @since 2026-04-10
 */
public final class LogContext {

    private LogContext() {
    }

    /**
     * 获取当前 TraceId
     *
     * @return TraceId
     */
    public static String getTraceId() {
        return RequestContext.TRACE_ID.isBound()
                ? RequestContext.TRACE_ID.get()
                : "N/A";
    }
}