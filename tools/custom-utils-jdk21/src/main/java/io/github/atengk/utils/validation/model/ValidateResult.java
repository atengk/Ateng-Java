package io.github.atengk.utils.validation.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 校验结果。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class ValidateResult {

    private final boolean valid;

    private final List<ValidateError> errors;

    /**
     * 创建校验结果。
     *
     * @param valid 是否校验通过
     * @param errors 错误信息列表
     */
    public ValidateResult(boolean valid, List<ValidateError> errors) {
        List<ValidateError> safeErrors = errors == null ? List.of() : errors.stream().filter(Objects::nonNull).toList();
        this.valid = valid && safeErrors.isEmpty();
        this.errors = Collections.unmodifiableList(new ArrayList<>(safeErrors));
    }

    /**
     * 创建校验通过结果。
     *
     * @return 校验通过结果
     */
    public static ValidateResult valid() {
        return new ValidateResult(true, List.of());
    }

    /**
     * 创建校验失败结果。
     *
     * @param errors 错误信息列表
     * @return 校验失败结果
     */
    public static ValidateResult invalid(List<ValidateError> errors) {
        return new ValidateResult(false, errors);
    }

    /**
     * 判断是否校验通过。
     *
     * @return 通过返回 true，否则返回 false
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * 判断是否存在校验错误。
     *
     * @return 存在错误返回 true，否则返回 false
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * 获取错误信息列表。
     *
     * @return 不可变错误信息列表
     */
    public List<ValidateError> getErrors() {
        return errors;
    }

    /**
     * 获取错误消息列表。
     *
     * @return 错误消息列表
     */
    public List<String> getMessages() {
        return errors.stream().map(ValidateError::getMessage).toList();
    }

    /**
     * 获取第一条错误消息。
     *
     * @return 第一条错误消息
     */
    public Optional<String> getFirstMessage() {
        return errors.stream().map(ValidateError::getMessage).filter(message -> !message.isBlank()).findFirst();
    }

    /**
     * 转换为字段错误映射。
     *
     * @return 字段错误映射
     */
    public Map<String, String> toFieldErrorMap() {
        Map<String, String> result = new LinkedHashMap<>();
        for (ValidateError error : errors) {
            if (error.hasField()) {
                result.putIfAbsent(error.getField(), error.getMessage());
            }
        }
        return result;
    }

    /**
     * 返回结果摘要。
     *
     * @return 结果摘要
     */
    @Override
    public String toString() {
        return "ValidateResult{valid=" + valid + ", errors=" + errors + '}';
    }

    /**
     * 判断结果是否相同。
     *
     * @param obj 比较对象
     * @return 相同返回 true，否则返回 false
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ValidateResult that)) {
            return false;
        }
        return valid == that.valid && Objects.equals(errors, that.errors);
    }

    /**
     * 获取哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(valid, errors);
    }
}
