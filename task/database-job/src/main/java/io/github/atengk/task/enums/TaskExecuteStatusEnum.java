package io.github.atengk.task.enums;

import java.util.Arrays;

/**
 * 任务执行状态枚举
 *
 * @author Ateng
 * @since 2026-02-12
 */
public enum TaskExecuteStatusEnum {

    /**
     * 待执行
     */
    PENDING(0, "待执行"),

    /**
     * 执行中
     */
    RUNNING(1, "执行中"),

    /**
     * 执行失败
     */
    FAILED(2, "失败"),

    /**
     * 执行成功
     */
    SUCCESS(3, "成功");


    private final int code;

    private final String name;

    TaskExecuteStatusEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static TaskExecuteStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("未知的任务执行状态 code: " + code));
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
