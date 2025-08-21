package local.ateng.java.customutils.enums;

public enum TypeEnum implements BaseEnum<Integer,String> {
    ENABLED(1, "启用"),
    DISABLED(0, "禁用");

    private final Integer code;
    private final String name;

    TypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getName() {
        return name;
    }
}

