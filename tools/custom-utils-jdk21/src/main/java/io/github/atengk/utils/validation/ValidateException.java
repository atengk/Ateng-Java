package io.github.atengk.utils.validation;


import io.github.atengk.utils.validation.model.ValidateError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 校验异常。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class ValidateException extends RuntimeException {

    private final List<ValidateError> errors;

    /**
     * 创建校验异常。
     *
     * @param message 异常消息
     */
    public ValidateException(String message) {
        this(message, List.of());
    }

    /**
     * 创建校验异常。
     *
     * @param message 异常消息
     * @param errors 错误信息列表
     */
    public ValidateException(String message, List<ValidateError> errors) {
        super(message == null ? "校验失败" : message);
        List<ValidateError> safeErrors = errors == null ? List.of() : errors.stream().filter(Objects::nonNull).toList();
        this.errors = Collections.unmodifiableList(new ArrayList<>(safeErrors));
    }

    /**
     * 获取错误信息列表。
     *
     * @return 不可变错误信息列表
     */
    public List<ValidateError> getErrors() {
        return errors;
    }
}
