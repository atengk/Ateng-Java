package io.github.atengk.model;

import java.util.List;

/**
 * 枚举字典元数据模型。
 *
 * @param key 字典唯一标识
 * @param title 字典标题
 * @param module 所属模块
 * @param description 字典描述
 * @param expose 是否允许暴露
 * @param sort 排序值
 * @param group 字典分组
 * @param deprecatedFlag 是否废弃
 * @param extraFields 扩展字段
 * @param className 类全名
 * @param simpleName 类短名
 * @author Ateng
 * @since 2026-04-29
 */
public record EnumMetadata(
        String key,
        String title,
        String module,
        String description,
        boolean expose,
        int sort,
        String group,
        boolean deprecatedFlag,
        List<String> extraFields,
        String className,
        String simpleName
) {

    /**
     * 创建不可变枚举元数据。
     */
    public EnumMetadata {
        extraFields = extraFields == null ? List.of() : List.copyOf(extraFields);
    }
}
