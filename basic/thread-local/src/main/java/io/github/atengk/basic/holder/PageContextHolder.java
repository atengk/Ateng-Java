package io.github.atengk.basic.holder;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.basic.model.Page;

/**
 * 分页上下文工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class PageContextHolder {

    /**
     * 存储分页对象
     */
    private static final ThreadLocal<Page> PAGE_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置分页参数
     *
     * @param page 页码（从1开始）
     * @param size 每页条数
     */
    public static void set(int page, int size) {
        PAGE_THREAD_LOCAL.set(new Page(page, size));
    }

    /**
     * 获取分页参数
     *
     * @return Page
     */
    public static Page get() {
        return PAGE_THREAD_LOCAL.get();
    }

    /**
     * 获取 offset（SQL 偏移量）
     */
    public static int getOffset() {
        Page page = get();
        if (ObjectUtil.isNull(page)) {
            return 0;
        }
        return (page.getPage() - 1) * page.getSize();
    }

    /**
     * 获取 limit
     */
    public static int getLimit() {
        Page page = get();
        return ObjectUtil.isNotNull(page) ? page.getSize() : 0;
    }

    /**
     * 清理
     */
    public static void clear() {
        PAGE_THREAD_LOCAL.remove();
    }
}