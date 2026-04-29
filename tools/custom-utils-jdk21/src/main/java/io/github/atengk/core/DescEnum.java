package io.github.atengk.core;

/**
 * 描述枚举契约。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public interface DescEnum {

    /**
     * 获取描述。
     *
     * @return 描述
     */
    default String getDesc() {
        return null;
    }
}
