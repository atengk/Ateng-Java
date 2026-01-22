package io.github.atengk.enums;

public enum UserStatus {
    NORMAL(0, "正常"),
    FROZEN(1, "冻结"),
    DELETED(2, "已删除");

    private final int code;
    private final String name;

    UserStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据 code 获取 name
     */
    public static String getNameByCode(int code) {
        for (UserStatus status : values()) {
            if (status.code == code) {
                return status.name;
            }
        }
        return null;
    }

    /**
     * 根据 name 获取枚举
     */
    public static UserStatus getByName(String name) {
        for (UserStatus status : values()) {
            if (status.name.equals(name)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 生成供 @Excel(replace) 使用的字符串
     * 格式： "显示文本_存储值, 显示文本_存储值, ..."
     */
    public static String getReplaceString() {
        UserStatus[] values = values();
        String[] replaceArray = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            // 格式必须为 “显示文本_存储值”
            replaceArray[i] = values[i].getName() + "_" + values[i].getCode();
        }
        return String.join(", ", replaceArray);
    }
}
