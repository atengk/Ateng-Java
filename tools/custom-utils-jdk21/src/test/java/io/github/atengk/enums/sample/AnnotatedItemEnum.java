package io.github.atengk.enums.sample;


import io.github.atengk.annotation.EnumDict;
import io.github.atengk.annotation.EnumItem;

/**
 * 注解式枚举项。
 *
 * @author Ateng
 * @since 2026-04-29
 */
@EnumDict(key = "annotated-item", title = "注解枚举", module = "test", description = "测试注解项")
public enum AnnotatedItemEnum {
    @EnumItem(label = "成功", desc = "操作成功", sort = 1, enabled = true, group = "result", color = "green", tagType = "success", parentCode = "root")
    SUCCESS,

    @EnumItem(label = "失败", desc = "操作失败", sort = 2, enabled = false, group = "result", color = "red", tagType = "danger", parentCode = "root")
    FAIL
}
