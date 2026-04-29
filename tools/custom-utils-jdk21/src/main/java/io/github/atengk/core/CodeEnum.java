package io.github.atengk.core;

/**
 * 业务编码枚举契约。
 *
 * @param <C> 编码类型
 * @author Ateng
 * @since 2026-04-29
 */
public interface CodeEnum<C> {

    /**
     * 获取业务编码。
     *
     * @return 业务编码
     */
    C getCode();
}
