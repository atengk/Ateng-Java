package io.github.atengk.core;

/**
 * 排序枚举契约。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public interface SortEnum {

    /**
     * 获取排序值。
     *
     * @return 排序值
     */
    default Integer getSort() {
        return null;
    }
}
