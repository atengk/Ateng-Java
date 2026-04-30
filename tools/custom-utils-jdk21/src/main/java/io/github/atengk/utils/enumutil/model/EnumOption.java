package io.github.atengk.utils.enumutil.model;

import java.util.List;
import java.util.Map;

/**
 * 前端枚举选项模型。
 *
 * @param code 编码
 * @param label 展示名称
 * @param name 枚举名称
 * @param desc 描述
 * @param sort 排序值
 * @param enabled 是否启用
 * @param group 分组
 * @param parentCode 父级编码
 * @param children 子选项
 * @param extra 扩展字段
 * @author Ateng
 * @since 2026-04-29
 */
public record EnumOption(
        Object code,
        String label,
        String name,
        String desc,
        Integer sort,
        Boolean enabled,
        Object group,
        Object parentCode,
        List<EnumOption> children,
        Map<String, Object> extra
) {

    /**
     * 创建不可变枚举选项。
     */
    public EnumOption {
        children = children == null ? List.of() : List.copyOf(children);
        extra = extra == null ? Map.of() : Map.copyOf(extra);
    }

    /**
     * 创建简单枚举选项。
     *
     * @param code 编码
     * @param label 展示名称
     * @return 枚举选项
     */
    public static EnumOption of(Object code, String label) {
        return new EnumOption(code, label, null, null, null, null, null, null, List.of(), Map.of());
    }

    /**
     * 返回带子选项的新对象。
     *
     * @param children 子选项
     * @return 新枚举选项
     */
    public EnumOption withChildren(List<EnumOption> children) {
        return new EnumOption(code, label, name, desc, sort, enabled, group, parentCode, children, extra);
    }
}
