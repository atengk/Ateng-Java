package io.github.atengk.enums.sample.scanned;


import io.github.atengk.utils.enumutil.annotation.EnumDict;
import io.github.atengk.utils.enumutil.core.FrontendEnum;

import java.util.Map;

/**
 * 扫描测试枚举。
 *
 * @author Ateng
 * @since 2026-04-29
 */
@EnumDict(key = "scanned-status", title = "扫描状态", module = "scan", description = "扫描注册测试")
public enum ScannedStatusEnum implements FrontendEnum<Integer> {
    NORMAL(1, "正常"),
    ABNORMAL(2, "异常");

    private final Integer code;
    private final String label;

    ScannedStatusEnum(Integer code, String label) {
        this.code = code;
        this.label = label;
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
    public Map<String, Object> getExtra() {
        return Map.of("source", "scan");
    }
}
