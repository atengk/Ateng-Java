package io.github.atengk.utils.enumutil.core;

/**
 * 字典枚举契约。
 *
 * @param <C> 编码类型
 * @author Ateng
 * @since 2026-04-29
 */
public interface DictEnum<C> extends BaseEnum<C>, DescEnum, SortEnum, EnabledEnum {
}
