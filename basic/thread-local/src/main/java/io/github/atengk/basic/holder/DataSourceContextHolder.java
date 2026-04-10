package io.github.atengk.basic.holder;

import cn.hutool.core.util.StrUtil;

/**
 * 数据源上下文工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class DataSourceContextHolder {

    /**
     * 存储当前线程的数据源 key
     */
    private static final ThreadLocal<String> DATASOURCE_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 默认数据源
     */
    public static final String DEFAULT_DS = "master";

    /**
     * 设置数据源
     *
     * @param ds 数据源标识
     */
    public static void set(String ds) {
        if (StrUtil.isNotBlank(ds)) {
            DATASOURCE_THREAD_LOCAL.set(ds);
        }
    }

    /**
     * 获取当前数据源
     *
     * @return 数据源标识
     */
    public static String get() {
        String ds = DATASOURCE_THREAD_LOCAL.get();
        return StrUtil.isNotBlank(ds) ? ds : DEFAULT_DS;
    }

    /**
     * 清理
     */
    public static void clear() {
        DATASOURCE_THREAD_LOCAL.remove();
    }
}