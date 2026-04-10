package io.github.atengk.basic.util;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.basic.context.LogContext;

/**
 * 自定义日志工具类
 *
 * 统一日志格式，自动追加 TraceId
 *
 * @author Ateng
 * @since 2026-04-10
 */
public final class LogUtil {

    private LogUtil() {
    }

    /**
     * info日志
     *
     * @param msg 日志内容
     */
    public static void info(String msg) {
        String traceId = LogContext.getTraceId();
        System.out.println(StrUtil.format("[INFO] [traceId={}] {}", traceId, msg));
    }

    /**
     * error日志
     *
     * @param msg 日志内容
     * @param e   异常
     */
    public static void error(String msg, Throwable e) {
        String traceId = LogContext.getTraceId();
        System.out.println(StrUtil.format("[ERROR] [traceId={}] {}", traceId, msg));
        e.printStackTrace();
    }
}
