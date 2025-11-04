package io.github.atengk.enums;

/**
 * 日志类型枚举。
 *
 * <p>说明：
 * 通过枚举定义可扩展的日志类型，便于统一管理和校验。
 * </p>
 */
public enum LogTypeEnum {

    /**
     * 文件日志。
     */
    FILE("fileLogStrategy","文件日志"),

    /**
     * 数据库日志。
     */
    DB("databaseLogStrategy","数据库日志");

    private final String code;
    private final String name;

    LogTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

}

