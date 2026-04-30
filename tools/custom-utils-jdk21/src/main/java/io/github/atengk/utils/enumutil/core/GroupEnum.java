package io.github.atengk.utils.enumutil.core;

/**
 * 分组枚举契约。
 *
 * @param <G> 分组类型
 * @author Ateng
 * @since 2026-04-29
 */
public interface GroupEnum<G> {

    /**
     * 获取分组值。
     *
     * @return 分组值
     */
    default G getGroup() {
        return null;
    }
}
