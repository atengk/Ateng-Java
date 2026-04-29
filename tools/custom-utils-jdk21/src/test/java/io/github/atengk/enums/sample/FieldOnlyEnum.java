package io.github.atengk.enums.sample;

/**
 * 字段式枚举。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public enum FieldOnlyEnum {
    LOW("L", "低", 20, true),
    HIGH("H", "高", 10, false);

    private final String code;
    private final String label;
    private final int sort;
    private final boolean enabled;

    FieldOnlyEnum(String code, String label, int sort, boolean enabled) {
        this.code = code;
        this.label = label;
        this.sort = sort;
        this.enabled = enabled;
    }
}
