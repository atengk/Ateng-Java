package io.github.atengk.utils.validation.model;

import java.util.Objects;

/**
 * 批量校验错误信息。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class BatchValidateError {

    private final int index;

    private final int rowNumber;

    private final ValidateError error;

    /**
     * 创建批量校验错误信息。
     *
     * @param index 集合下标，从 0 开始
     * @param rowNumber 行号，从 1 开始
     * @param error 校验错误信息
     */
    public BatchValidateError(int index, int rowNumber, ValidateError error) {
        if (index < 0) {
            throw new IllegalArgumentException("集合下标不能小于 0");
        }
        if (rowNumber < 1) {
            throw new IllegalArgumentException("行号不能小于 1");
        }
        this.index = index;
        this.rowNumber = rowNumber;
        this.error = Objects.requireNonNull(error, "校验错误信息不能为空");
    }

    /**
     * 创建批量校验错误信息。
     *
     * @param index 集合下标，从 0 开始
     * @param rowNumber 行号，从 1 开始
     * @param error 校验错误信息
     * @return 批量校验错误信息
     */
    public static BatchValidateError of(int index, int rowNumber, ValidateError error) {
        return new BatchValidateError(index, rowNumber, error);
    }

    /**
     * 获取集合下标。
     *
     * @return 集合下标
     */
    public int getIndex() {
        return index;
    }

    /**
     * 获取行号。
     *
     * @return 行号
     */
    public int getRowNumber() {
        return rowNumber;
    }

    /**
     * 获取校验错误信息。
     *
     * @return 校验错误信息
     */
    public ValidateError getError() {
        return error;
    }

    /**
     * 获取字段名。
     *
     * @return 字段名
     */
    public String getField() {
        return error.getField();
    }

    /**
     * 获取错误消息。
     *
     * @return 错误消息
     */
    public String getMessage() {
        return error.getMessage();
    }

    /**
     * 返回错误摘要。
     *
     * @return 错误摘要
     */
    @Override
    public String toString() {
        return "第 " + rowNumber + " 行 " + error;
    }

    /**
     * 判断错误信息是否相同。
     *
     * @param obj 比较对象
     * @return 相同返回 true，否则返回 false
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BatchValidateError that)) {
            return false;
        }
        return index == that.index && rowNumber == that.rowNumber && Objects.equals(error, that.error);
    }

    /**
     * 获取哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(index, rowNumber, error);
    }
}
