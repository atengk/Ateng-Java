package io.github.atengk.enums.sample;


import io.github.atengk.annotation.EnumDict;
import io.github.atengk.core.FrontendEnum;
import io.github.atengk.core.GroupEnum;

import java.util.Map;

/**
 * 订单状态枚举。
 *
 * @author Ateng
 * @since 2026-04-29
 */
@EnumDict(key = "order-status", title = "订单状态", module = "order", description = "订单状态字典", sort = 2)
public enum OrderStatusEnum implements FrontendEnum<String>, GroupEnum<String> {

    CREATED("created", "已创建", "订单已创建", 1, true, "open", null),
    PAID("paid", "已支付", "订单已支付", 2, true, "open", "created"),
    FINISHED("finished", "已完成", "订单已完成", 3, true, "closed", "paid"),
    CANCELED("canceled", "已取消", "订单已取消", 4, false, "closed", "created");

    private final String code;
    private final String label;
    private final String desc;
    private final Integer sort;
    private final Boolean enabled;
    private final String group;
    private final String parentCode;

    OrderStatusEnum(String code, String label, String desc, Integer sort, Boolean enabled, String group, String parentCode) {
        this.code = code;
        this.label = label;
        this.desc = desc;
        this.sort = sort;
        this.enabled = enabled;
        this.group = group;
        this.parentCode = parentCode;
    }

    @Override
    public String getCode() {
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
        return Map.of("parentCode", parentCode == null ? "" : parentCode);
    }

    public String getParentCode() {
        return parentCode;
    }
}
