package io.github.atengk.core;

/**
 * 启停状态枚举契约。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public interface EnabledEnum {

    /**
     * 获取是否启用。
     *
     * @return 是否启用
     */
    default Boolean getEnabled() {
        return Boolean.TRUE;
    }
}
