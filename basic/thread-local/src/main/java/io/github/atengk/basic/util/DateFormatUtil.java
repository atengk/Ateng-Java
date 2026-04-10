package io.github.atengk.basic.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 线程安全的日期工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class DateFormatUtil {

    /**
     * 默认日期格式
     */
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * ThreadLocal 缓存 SimpleDateFormat
     */
    private static final ThreadLocal<SimpleDateFormat> SDF_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> new SimpleDateFormat(DEFAULT_PATTERN));

    /**
     * 格式化日期
     *
     * @param date 日期
     * @return 字符串
     */
    public static String format(Date date) {
        return SDF_THREAD_LOCAL.get().format(date);
    }

    /**
     * 解析日期
     *
     * @param dateStr 日期字符串
     * @return Date
     */
    public static Date parse(String dateStr) {
        try {
            return SDF_THREAD_LOCAL.get().parse(dateStr);
        } catch (ParseException e) {
            throw new RuntimeException("日期解析失败：" + dateStr, e);
        }
    }

    /**
     * 获取当前线程的 SimpleDateFormat（扩展用）
     */
    public static SimpleDateFormat get() {
        return SDF_THREAD_LOCAL.get();
    }

    /**
     * 清理 ThreadLocal（一般可不手动调用，线程结束自动释放）
     */
    public static void clear() {
        SDF_THREAD_LOCAL.remove();
    }
}
