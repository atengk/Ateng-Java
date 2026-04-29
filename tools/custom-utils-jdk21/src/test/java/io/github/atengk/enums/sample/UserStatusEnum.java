package io.github.atengk.enums.sample;


import io.github.atengk.annotation.EnumDict;
import io.github.atengk.core.FrontendEnum;
import io.github.atengk.core.GroupEnum;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户状态枚举。
 *
 * @author Ateng
 * @since 2026-04-29
 */
@EnumDict(
        key = "user-status",
        title = "用户状态",
        module = "system",
        description = "用户账号状态字典",
        sort = 1,
        group = "user",
        extraFields = {"color", "tagType", "permission", "remark"}
)
public enum UserStatusEnum implements FrontendEnum<Integer>, GroupEnum<String> {

    ENABLED(1, "启用", "用户可正常登录", 1, true, "normal", "green", "success", "user:enabled", "启用状态"),
    DISABLED(0, "禁用", "用户禁止登录", 2, false, "normal", "red", "danger", "user:disabled", "禁用状态");

    private final Integer code;
    private final String label;
    private final String desc;
    private final Integer sort;
    private final Boolean enabled;
    private final String group;
    private final String color;
    private final String tagType;
    private final String permission;
    private final String remark;

    UserStatusEnum(Integer code, String label, String desc, Integer sort, Boolean enabled, String group, String color, String tagType, String permission, String remark) {
        this.code = code;
        this.label = label;
        this.desc = desc;
        this.sort = sort;
        this.enabled = enabled;
        this.group = group;
        this.color = color;
        this.tagType = tagType;
        this.permission = permission;
        this.remark = remark;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    @Override
    public Integer getSort() {
        return sort;
    }

    @Override
    public Boolean getEnabled() {
        return enabled;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public Map<String, Object> getExtra() {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("color", color);
        extra.put("tagType", tagType);
        extra.put("permission", permission);
        extra.put("remark", remark);
        return extra;
    }
}
