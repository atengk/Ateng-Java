package io.github.atengk.utils.validation.model;

import java.util.Objects;

/**
 * 校验错误信息。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class ValidateError {

    private final String field;

    private final String message;

    private final Object rejectedValue;

    private final String messageTemplate;

    private final String propertyPath;

    /**
     * 创建校验错误信息。
     *
     * @param field 字段名
     * @param message 错误消息
     * @param rejectedValue 被拒绝的值
     * @param messageTemplate 消息模板
     * @param propertyPath 完整属性路径
     */
    public ValidateError(String field, String message, Object rejectedValue, String messageTemplate, String propertyPath) {
        this.field = field == null ? "" : field;
        this.message = message == null ? "" : message;
        this.rejectedValue = rejectedValue;
        this.messageTemplate = messageTemplate == null ? "" : messageTemplate;
        this.propertyPath = propertyPath == null ? "" : propertyPath;
    }

    /**
     * 创建校验错误信息。
     *
     * @param field 字段名
     * @param message 错误消息
     * @param rejectedValue 被拒绝的值
     * @param messageTemplate 消息模板
     * @param propertyPath 完整属性路径
     * @return 校验错误信息
     */
    public static ValidateError of(String field, String message, Object rejectedValue, String messageTemplate, String propertyPath) {
        return new ValidateError(field, message, rejectedValue, messageTemplate, propertyPath);
    }

    /**
     * 获取字段名。
     *
     * @return 字段名
     */
    public String getField() {
        return field;
    }

    /**
     * 获取错误消息。
     *
     * @return 错误消息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 获取被拒绝的值。
     *
     * @return 被拒绝的值
     */
    public Object getRejectedValue() {
        return rejectedValue;
    }

    /**
     * 获取消息模板。
     *
     * @return 消息模板
     */
    public String getMessageTemplate() {
        return messageTemplate;
    }

    /**
     * 获取完整属性路径。
     *
     * @return 完整属性路径
     */
    public String getPropertyPath() {
        return propertyPath;
    }

    /**
     * 判断是否为字段错误。
     *
     * @return 有字段名返回 true，否则返回 false
     */
    public boolean hasField() {
        return !field.isBlank();
    }

    /**
     * 返回错误消息文本。
     *
     * @return 错误消息文本
     */
    @Override
    public String toString() {
        if (field.isBlank()) {
            return message;
        }
        return field + ": " + message;
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
        if (!(obj instanceof ValidateError that)) {
            return false;
        }
        return Objects.equals(field, that.field)
                && Objects.equals(message, that.message)
                && Objects.equals(rejectedValue, that.rejectedValue)
                && Objects.equals(messageTemplate, that.messageTemplate)
                && Objects.equals(propertyPath, that.propertyPath);
    }

    /**
     * 获取哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(field, message, rejectedValue, messageTemplate, propertyPath);
    }
}
