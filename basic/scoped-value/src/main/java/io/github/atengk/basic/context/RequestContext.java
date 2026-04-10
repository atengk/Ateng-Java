package io.github.atengk.basic.context;

import java.lang.ScopedValue;
import java.util.UUID;

/**
 * 请求上下文工具类（基于 ScopedValue 实现）
 *
 * 用于在整个调用链中传递用户信息和链路追踪ID
 *
 * @author Ateng
 * @since 2026-04-10
 */
public final class RequestContext {

    /**
     * 用户ID
     */
    public static final ScopedValue<String> USER_ID = ScopedValue.newInstance();

    /**
     * 链路追踪ID
     */
    public static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();

    private RequestContext() {
    }

    /**
     * 生成 TraceId
     *
     * @return TraceId
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
