package local.ateng.java.customutils.enums;

public enum StatusEnum {
    ENABLED(1, "启用"),
    DISABLED(0, "禁用");

    private final int code;
    private final String name;

    StatusEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}

