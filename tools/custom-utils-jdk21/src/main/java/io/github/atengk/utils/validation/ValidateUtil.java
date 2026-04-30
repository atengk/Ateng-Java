package io.github.atengk.utils.validation;

import io.github.atengk.utils.validation.model.BatchValidateError;
import io.github.atengk.utils.validation.model.BatchValidateResult;
import io.github.atengk.utils.validation.model.ValidateError;
import io.github.atengk.utils.validation.model.ValidateResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jakarta Validation 静态校验工具类。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class ValidateUtil {

    private static final String DEFAULT_DELIMITER = "; ";

    private static final Pattern INDEX_PATTERN = Pattern.compile("\\[(\\d+)]");

    private static final ValidatorFactory DEFAULT_VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    private static final Validator DEFAULT_VALIDATOR = DEFAULT_VALIDATOR_FACTORY.getValidator();

    private static volatile Validator customValidator;

    private ValidateUtil() {
        throw new UnsupportedOperationException("ValidateUtil 不允许实例化");
    }

    /**
     * 获取当前使用的 Validator。
     *
     * @return 当前 Validator
     */
    public static Validator getValidator() {
        Validator validator = customValidator;
        return validator == null ? DEFAULT_VALIDATOR : validator;
    }

    /**
     * 获取默认 ValidatorFactory。
     *
     * @return 默认 ValidatorFactory
     */
    public static ValidatorFactory getValidatorFactory() {
        return DEFAULT_VALIDATOR_FACTORY;
    }

    /**
     * 获取方法参数与返回值校验器。
     *
     * @return 方法参数与返回值校验器
     */
    public static ExecutableValidator getExecutableValidator() {
        return getValidator().forExecutables();
    }

    /**
     * 设置全局自定义 Validator。
     *
     * @param validator 自定义 Validator
     */
    public static void setValidator(Validator validator) {
        customValidator = Objects.requireNonNull(validator, "Validator 不能为空");
    }

    /**
     * 重置为默认 Validator。
     */
    public static void resetValidator() {
        customValidator = null;
    }

    /**
     * 使用指定 Validator 校验对象。
     *
     * @param validator Validator 实例
     * @param bean 待校验对象
     * @param groups 校验分组
     * @param <T> 对象类型
     * @return 约束违反集合
     */
    public static <T> Set<ConstraintViolation<T>> validateWith(Validator validator, T bean, Class<?>... groups) {
        return requireValidator(validator).validate(requireBean(bean), safeGroups(groups));
    }

    /**
     * 校验对象全部约束。
     *
     * @param bean 待校验对象
     * @param groups 校验分组
     * @param <T> 对象类型
     * @return 约束违反集合
     */
    public static <T> Set<ConstraintViolation<T>> validate(T bean, Class<?>... groups) {
        return getValidator().validate(requireBean(bean), safeGroups(groups));
    }

    /**
     * 校验对象并返回结构化结果。
     *
     * @param bean 待校验对象
     * @param groups 校验分组
     * @param <T> 对象类型
     * @return 校验结果
     */
    public static <T> ValidateResult validateResult(T bean, Class<?>... groups) {
        return toValidateResult(validate(bean, groups));
    }

    /**
     * 校验对象并返回第一条错误消息。
     *
     * @param bean 待校验对象
     * @param groups 校验分组
     * @param <T> 对象类型
     * @return 第一条错误消息
     */
    public static <T> Optional<String> validateFirst(T bean, Class<?>... groups) {
        return getFirstMessage(validate(bean, groups));
    }

    /**
     * 判断对象是否校验通过。
     *
     * @param bean 待校验对象
     * @param groups 校验分组
     * @param <T> 对象类型
     * @return 通过返回 true，否则返回 false
     */
    public static <T> boolean isValid(T bean, Class<?>... groups) {
        return validate(bean, groups).isEmpty();
    }

    /**
     * 校验对象，失败时抛出 ValidateException。
     *
     * @param bean 待校验对象
     * @param groups 校验分组
     * @param <T> 对象类型
     */
    public static <T> void validateOrThrow(T bean, Class<?>... groups) {
        throwIfHasErrors(validate(bean, groups));
    }

    /**
     * 校验对象，失败时抛出 ValidateException。
     *
     * @param bean 待校验对象
     * @param groups 校验分组
     * @param <T> 对象类型
     */
    public static <T> void throwIfInvalid(T bean, Class<?>... groups) {
        validateOrThrow(bean, groups);
    }

    /**
     * 校验对象中的指定属性。
     *
     * @param bean 待校验对象
     * @param propertyName 属性名
     * @param groups 校验分组
     * @param <T> 对象类型
     * @return 约束违反集合
     */
    public static <T> Set<ConstraintViolation<T>> validateProperty(T bean, String propertyName, Class<?>... groups) {
        return getValidator().validateProperty(requireBean(bean), requireText(propertyName, "属性名不能为空"), safeGroups(groups));
    }

    /**
     * 校验对象中的指定属性并返回结构化结果。
     *
     * @param bean 待校验对象
     * @param propertyName 属性名
     * @param groups 校验分组
     * @param <T> 对象类型
     * @return 校验结果
     */
    public static <T> ValidateResult validatePropertyResult(T bean, String propertyName, Class<?>... groups) {
        return toValidateResult(validateProperty(bean, propertyName, groups));
    }

    /**
     * 校验对象中的指定属性并返回第一条错误消息。
     *
     * @param bean 待校验对象
     * @param propertyName 属性名
     * @param groups 校验分组
     * @param <T> 对象类型
     * @return 第一条错误消息
     */
    public static <T> Optional<String> validatePropertyFirst(T bean, String propertyName, Class<?>... groups) {
        return getFirstMessage(validateProperty(bean, propertyName, groups));
    }

    /**
     * 判断对象中的指定属性是否校验通过。
     *
     * @param bean 待校验对象
     * @param propertyName 属性名
     * @param groups 校验分组
     * @param <T> 对象类型
     * @return 通过返回 true，否则返回 false
     */
    public static <T> boolean isPropertyValid(T bean, String propertyName, Class<?>... groups) {
        return validateProperty(bean, propertyName, groups).isEmpty();
    }

    /**
     * 校验对象中的指定属性，失败时抛出 ValidateException。
     *
     * @param bean 待校验对象
     * @param propertyName 属性名
     * @param groups 校验分组
     * @param <T> 对象类型
     */
    public static <T> void validatePropertyOrThrow(T bean, String propertyName, Class<?>... groups) {
        throwIfHasErrors(validateProperty(bean, propertyName, groups));
    }

    /**
     * 校验指定 Bean 类型中某个属性值。
     *
     * @param beanType Bean 类型
     * @param propertyName 属性名
     * @param value 属性值
     * @param groups 校验分组
     * @param <T> Bean 类型
     * @return 约束违反集合
     */
    public static <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value, Class<?>... groups) {
        return getValidator().validateValue(requireBeanType(beanType), requireText(propertyName, "属性名不能为空"), value, safeGroups(groups));
    }

    /**
     * 校验指定 Bean 类型中某个属性值并返回结构化结果。
     *
     * @param beanType Bean 类型
     * @param propertyName 属性名
     * @param value 属性值
     * @param groups 校验分组
     * @param <T> Bean 类型
     * @return 校验结果
     */
    public static <T> ValidateResult validateValueResult(Class<T> beanType, String propertyName, Object value, Class<?>... groups) {
        return toValidateResult(validateValue(beanType, propertyName, value, groups));
    }

    /**
     * 校验指定 Bean 类型中某个属性值并返回第一条错误消息。
     *
     * @param beanType Bean 类型
     * @param propertyName 属性名
     * @param value 属性值
     * @param groups 校验分组
     * @param <T> Bean 类型
     * @return 第一条错误消息
     */
    public static <T> Optional<String> validateValueFirst(Class<T> beanType, String propertyName, Object value, Class<?>... groups) {
        return getFirstMessage(validateValue(beanType, propertyName, value, groups));
    }

    /**
     * 判断指定 Bean 类型中某个属性值是否校验通过。
     *
     * @param beanType Bean 类型
     * @param propertyName 属性名
     * @param value 属性值
     * @param groups 校验分组
     * @param <T> Bean 类型
     * @return 通过返回 true，否则返回 false
     */
    public static <T> boolean isValueValid(Class<T> beanType, String propertyName, Object value, Class<?>... groups) {
        return validateValue(beanType, propertyName, value, groups).isEmpty();
    }

    /**
     * 校验指定 Bean 类型中某个属性值，失败时抛出 ValidateException。
     *
     * @param beanType Bean 类型
     * @param propertyName 属性名
     * @param value 属性值
     * @param groups 校验分组
     * @param <T> Bean 类型
     */
    public static <T> void validateValueOrThrow(Class<T> beanType, String propertyName, Object value, Class<?>... groups) {
        throwIfHasErrors(validateValue(beanType, propertyName, value, groups));
    }

    /**
     * 校验集合中的每个对象。
     *
     * @param beans 对象集合
     * @param groups 校验分组
     * @param <T> 对象类型
     * @return 每个对象对应的校验结果
     */
    public static <T> List<ValidateResult> validateEach(Collection<T> beans, Class<?>... groups) {
        Collection<T> safeBeans = requireCollection(beans);
        List<ValidateResult> results = new ArrayList<>(safeBeans.size());
        for (T bean : safeBeans) {
            results.add(validateResult(bean, groups));
        }
        return Collections.unmodifiableList(results);
    }

    /**
     * 校验集合中的每个对象，并且每个对象只保留第一条错误。
     *
     * @param beans 对象集合
     * @param groups 校验分组
     * @param <T> 对象类型
     * @return 每个对象对应的校验结果
     */
    public static <T> List<ValidateResult> validateEachFirst(Collection<T> beans, Class<?>... groups) {
        Collection<T> safeBeans = requireCollection(beans);
        List<ValidateResult> results = new ArrayList<>(safeBeans.size());
        for (T bean : safeBeans) {
            List<ValidateError> errors = toFieldErrors(validate(bean, groups));
            results.add(errors.isEmpty() ? ValidateResult.valid() : ValidateResult.invalid(List.of(errors.getFirst())));
        }
        return Collections.unmodifiableList(results);
    }

    /**
     * 校验集合中的全部对象并返回批量校验结果。
     *
     * @param beans 对象集合
     * @param groups 校验分组
     * @param <T> 对象类型
     * @return 批量校验结果
     */
    public static <T> BatchValidateResult validateAll(Collection<T> beans, Class<?>... groups) {
        Collection<T> safeBeans = requireCollection(beans);
        List<BatchValidateError> errors = new ArrayList<>();
        int index = 0;
        for (T bean : safeBeans) {
            for (ValidateError error : toFieldErrors(validate(bean, groups))) {
                errors.add(BatchValidateError.of(index, index + 1, error));
            }
            index++;
        }
        return BatchValidateResult.of(safeBeans.size(), errors);
    }

    /**
     * 校验集合中的全部对象，存在错误时抛出 ValidateException。
     *
     * @param beans 对象集合
     * @param groups 校验分组
     * @param <T> 对象类型
     */
    public static <T> void validateAllOrThrow(Collection<T> beans, Class<?>... groups) {
        BatchValidateResult result = validateAll(beans, groups);
        if (result.hasErrors()) {
            throw new ValidateException(result.getFirstMessage().orElse("批量校验失败"), result.getErrors().stream().map(BatchValidateError::getError).toList());
        }
    }

    /**
     * 判断集合中的全部对象是否校验通过。
     *
     * @param beans 对象集合
     * @param groups 校验分组
     * @param <T> 对象类型
     * @return 全部通过返回 true，否则返回 false
     */
    public static <T> boolean isAllValid(Collection<T> beans, Class<?>... groups) {
        return validateAll(beans, groups).isValid();
    }

    /**
     * 转换为校验结果。
     *
     * @param violations 约束违反集合
     * @param <T> 对象类型
     * @return 校验结果
     */
    public static <T> ValidateResult toValidateResult(Set<ConstraintViolation<T>> violations) {
        List<ValidateError> errors = toFieldErrors(violations);
        return errors.isEmpty() ? ValidateResult.valid() : ValidateResult.invalid(errors);
    }

    /**
     * 提取错误消息列表。
     *
     * @param violations 约束违反集合
     * @param <T> 对象类型
     * @return 错误消息列表
     */
    public static <T> List<String> toMessages(Set<ConstraintViolation<T>> violations) {
        if (violations == null || violations.isEmpty()) {
            return List.of();
        }
        return violations.stream().map(ValidateUtil::getMessage).filter(message -> !message.isBlank()).toList();
    }

    /**
     * 合并错误消息。
     *
     * @param violations 约束违反集合
     * @param <T> 对象类型
     * @return 合并后的错误消息
     */
    public static <T> String toMessage(Set<ConstraintViolation<T>> violations) {
        return joinMessages(toMessages(violations));
    }

    /**
     * 转换为字段错误列表。
     *
     * @param violations 约束违反集合
     * @param <T> 对象类型
     * @return 字段错误列表
     */
    public static <T> List<ValidateError> toFieldErrors(Set<ConstraintViolation<T>> violations) {
        if (violations == null || violations.isEmpty()) {
            return List.of();
        }
        return violations.stream().map(ValidateUtil::toValidateError).toList();
    }

    /**
     * 转换为字段错误映射。
     *
     * @param violations 约束违反集合
     * @param <T> 对象类型
     * @return 字段错误映射
     */
    public static <T> Map<String, String> toFieldErrorMap(Set<ConstraintViolation<T>> violations) {
        Map<String, String> result = new LinkedHashMap<>();
        for (ValidateError error : toFieldErrors(violations)) {
            if (error.hasField()) {
                result.putIfAbsent(error.getField(), error.getMessage());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * 转换为详细错误对象列表。
     *
     * @param violations 约束违反集合
     * @param <T> 对象类型
     * @return 详细错误对象列表
     */
    public static <T> List<ValidateError> toErrorDetails(Set<ConstraintViolation<T>> violations) {
        return toFieldErrors(violations);
    }

    /**
     * 获取第一条错误消息。
     *
     * @param violations 约束违反集合
     * @param <T> 对象类型
     * @return 第一条错误消息
     */
    public static <T> Optional<String> getFirstMessage(Set<ConstraintViolation<T>> violations) {
        return toMessages(violations).stream().findFirst();
    }

    /**
     * 获取第一条错误字段名。
     *
     * @param violations 约束违反集合
     * @param <T> 对象类型
     * @return 第一条错误字段名
     */
    public static <T> Optional<String> getFirstField(Set<ConstraintViolation<T>> violations) {
        return toFieldErrors(violations).stream().map(ValidateError::getField).filter(field -> !field.isBlank()).findFirst();
    }

    /**
     * 存在错误时抛出 ValidateException。
     *
     * @param violations 约束违反集合
     * @param <T> 对象类型
     */
    public static <T> void throwIfHasErrors(Set<ConstraintViolation<T>> violations) {
        if (violations != null && !violations.isEmpty()) {
            throw buildValidateException(violations);
        }
    }

    /**
     * 存在错误时抛出带消息前缀的 ValidateException。
     *
     * @param violations 约束违反集合
     * @param messagePrefix 消息前缀
     * @param <T> 对象类型
     */
    public static <T> void throwIfHasErrors(Set<ConstraintViolation<T>> violations, String messagePrefix) {
        if (violations != null && !violations.isEmpty()) {
            throw buildValidateException(messagePrefix, violations);
        }
    }

    /**
     * 构建 ValidateException。
     *
     * @param violations 约束违反集合
     * @param <T> 对象类型
     * @return ValidateException
     */
    public static <T> ValidateException buildValidateException(Set<ConstraintViolation<T>> violations) {
        return buildValidateException(null, violations);
    }

    /**
     * 构建带消息前缀的 ValidateException。
     *
     * @param messagePrefix 消息前缀
     * @param violations 约束违反集合
     * @param <T> 对象类型
     * @return ValidateException
     */
    public static <T> ValidateException buildValidateException(String messagePrefix, Set<ConstraintViolation<T>> violations) {
        List<ValidateError> errors = toFieldErrors(violations);
        String message = buildMessage(violations);
        if (messagePrefix != null && !messagePrefix.isBlank()) {
            message = message.isBlank() ? messagePrefix.trim() : messagePrefix.trim() + ": " + message;
        }
        return new ValidateException(message.isBlank() ? "校验失败" : message, errors);
    }

    /**
     * 构建错误消息。
     *
     * @param violations 约束违反集合
     * @param <T> 对象类型
     * @return 错误消息
     */
    public static <T> String buildMessage(Set<ConstraintViolation<T>> violations) {
        return toMessage(violations);
    }

    /**
     * 级联校验对象。
     *
     * @param bean 待校验对象
     * @param groups 校验分组
     * @param <T> 对象类型
     * @return 约束违反集合
     */
    public static <T> Set<ConstraintViolation<T>> validateCascade(T bean, Class<?>... groups) {
        return validate(bean, groups);
    }

    /**
     * 级联校验对象，失败时抛出 ValidateException。
     *
     * @param bean 待校验对象
     * @param groups 校验分组
     * @param <T> 对象类型
     */
    public static <T> void validateCascadeOrThrow(T bean, Class<?>... groups) {
        validateOrThrow(bean, groups);
    }

    /**
     * 校验嵌套对象。
     *
     * @param bean 待校验对象
     * @param groups 校验分组
     * @param <T> 对象类型
     * @return 约束违反集合
     */
    public static <T> Set<ConstraintViolation<T>> validateNested(T bean, Class<?>... groups) {
        return validate(bean, groups);
    }

    /**
     * 校验嵌套对象，失败时抛出 ValidateException。
     *
     * @param bean 待校验对象
     * @param groups 校验分组
     * @param <T> 对象类型
     */
    public static <T> void validateNestedOrThrow(T bean, Class<?>... groups) {
        validateOrThrow(bean, groups);
    }

    /**
     * 判断是否存在嵌套字段错误。
     *
     * @param violations 约束违反集合
     * @param <T> 对象类型
     * @return 存在嵌套字段错误返回 true，否则返回 false
     */
    public static <T> boolean hasNestedError(Set<ConstraintViolation<T>> violations) {
        if (violations == null || violations.isEmpty()) {
            return false;
        }
        return violations.stream().map(ValidateUtil::getPropertyPath).anyMatch(ValidateUtil::isNestedField);
    }

    /**
     * 获取约束违反的完整属性路径。
     *
     * @param violation 约束违反对象
     * @return 完整属性路径
     */
    public static String getPropertyPath(ConstraintViolation<?> violation) {
        if (violation == null || violation.getPropertyPath() == null) {
            return "";
        }
        return normalizeFieldPath(violation.getPropertyPath().toString());
    }

    /**
     * 校验方法参数。
     *
     * @param target 目标对象
     * @param method 方法对象
     * @param args 方法实参数组
     * @param groups 校验分组
     * @param <T> 目标对象类型
     * @return 约束违反集合
     */
    public static <T> Set<ConstraintViolation<T>> validateParameters(T target, Method method, Object[] args, Class<?>... groups) {
        Object[] safeArgs = safeArguments(method, args);
        return getExecutableValidator().validateParameters(requireTarget(target), requireMethod(method), safeArgs, safeGroups(groups));
    }

    /**
     * 校验方法参数，失败时抛出 ValidateException。
     *
     * @param target 目标对象
     * @param method 方法对象
     * @param args 方法实参数组
     * @param groups 校验分组
     * @param <T> 目标对象类型
     */
    public static <T> void validateParametersOrThrow(T target, Method method, Object[] args, Class<?>... groups) {
        throwIfHasErrors(validateParameters(target, method, args, groups));
    }

    /**
     * 判断方法参数是否校验通过。
     *
     * @param target 目标对象
     * @param method 方法对象
     * @param args 方法实参数组
     * @param groups 校验分组
     * @param <T> 目标对象类型
     * @return 通过返回 true，否则返回 false
     */
    public static <T> boolean isParametersValid(T target, Method method, Object[] args, Class<?>... groups) {
        return validateParameters(target, method, args, groups).isEmpty();
    }

    /**
     * 校验方法返回值。
     *
     * @param target 目标对象
     * @param method 方法对象
     * @param returnValue 方法返回值
     * @param groups 校验分组
     * @param <T> 目标对象类型
     * @return 约束违反集合
     */
    public static <T> Set<ConstraintViolation<T>> validateReturnValue(T target, Method method, Object returnValue, Class<?>... groups) {
        return getExecutableValidator().validateReturnValue(requireTarget(target), requireMethod(method), returnValue, safeGroups(groups));
    }

    /**
     * 校验方法返回值，失败时抛出 ValidateException。
     *
     * @param target 目标对象
     * @param method 方法对象
     * @param returnValue 方法返回值
     * @param groups 校验分组
     * @param <T> 目标对象类型
     */
    public static <T> void validateReturnValueOrThrow(T target, Method method, Object returnValue, Class<?>... groups) {
        throwIfHasErrors(validateReturnValue(target, method, returnValue, groups));
    }

    /**
     * 判断方法返回值是否校验通过。
     *
     * @param target 目标对象
     * @param method 方法对象
     * @param returnValue 方法返回值
     * @param groups 校验分组
     * @param <T> 目标对象类型
     * @return 通过返回 true，否则返回 false
     */
    public static <T> boolean isReturnValueValid(T target, Method method, Object returnValue, Class<?>... groups) {
        return validateReturnValue(target, method, returnValue, groups).isEmpty();
    }

    /**
     * 获取约束违反消息。
     *
     * @param violation 约束违反对象
     * @return 错误消息
     */
    public static String getMessage(ConstraintViolation<?> violation) {
        return violation == null || violation.getMessage() == null ? "" : violation.getMessage();
    }

    /**
     * 获取约束违反消息模板。
     *
     * @param violation 约束违反对象
     * @return 消息模板
     */
    public static String getMessageTemplate(ConstraintViolation<?> violation) {
        return violation == null || violation.getMessageTemplate() == null ? "" : violation.getMessageTemplate();
    }

    /**
     * 格式化约束违反消息。
     *
     * @param violation 约束违反对象
     * @return 格式化后的错误消息
     */
    public static String formatMessage(ConstraintViolation<?> violation) {
        if (violation == null) {
            return "";
        }
        String fieldName = getFieldName(violation);
        String message = getMessage(violation);
        return fieldName.isBlank() ? message : fieldName + ": " + message;
    }

    /**
     * 使用默认分隔符合并消息。
     *
     * @param messages 消息集合
     * @return 合并后的消息
     */
    public static String joinMessages(Collection<String> messages) {
        return joinMessages(messages, DEFAULT_DELIMITER);
    }

    /**
     * 使用指定分隔符合并消息。
     *
     * @param messages 消息集合
     * @param delimiter 分隔符
     * @return 合并后的消息
     */
    public static String joinMessages(Collection<String> messages, String delimiter) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        String safeDelimiter = delimiter == null ? DEFAULT_DELIMITER : delimiter;
        List<String> effectiveMessages = messages.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(message -> !message.isBlank())
                .toList();
        return String.join(safeDelimiter, effectiveMessages);
    }

    /**
     * 解析约束违反消息。
     *
     * @param violation 约束违反对象
     * @return 解析后的消息
     */
    public static String resolveMessage(ConstraintViolation<?> violation) {
        return getMessage(violation);
    }

    /**
     * 获取字段名。
     *
     * @param violation 约束违反对象
     * @return 字段名
     */
    public static String getFieldName(ConstraintViolation<?> violation) {
        return getLeafFieldName(violation);
    }

    /**
     * 获取完整字段路径。
     *
     * @param violation 约束违反对象
     * @return 完整字段路径
     */
    public static String getFieldPath(ConstraintViolation<?> violation) {
        return getPropertyPath(violation);
    }

    /**
     * 获取最后一级字段名。
     *
     * @param violation 约束违反对象
     * @return 最后一级字段名
     */
    public static String getLeafFieldName(ConstraintViolation<?> violation) {
        String path = getPropertyPath(violation);
        if (path.isBlank()) {
            return "";
        }
        String normalized = removeIndexFromPath(path);
        int dotIndex = normalized.lastIndexOf('.');
        return dotIndex < 0 ? normalized : normalized.substring(dotIndex + 1);
    }

    /**
     * 标准化字段路径。
     *
     * @param path 字段路径
     * @return 标准化后的字段路径
     */
    public static String normalizeFieldPath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String value = path.trim().replaceAll("\\s+", "").replaceAll("\\.{2,}", ".");
        while (value.startsWith(".")) {
            value = value.substring(1);
        }
        while (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * 判断字段路径是否为嵌套字段。
     *
     * @param path 字段路径
     * @return 是嵌套字段返回 true，否则返回 false
     */
    public static boolean isNestedField(String path) {
        String normalized = normalizeFieldPath(path);
        return normalized.contains(".") || normalized.contains("[");
    }

    /**
     * 移除字段路径中的集合下标。
     *
     * @param path 字段路径
     * @return 移除下标后的字段路径
     */
    public static String removeIndexFromPath(String path) {
        return normalizeFieldPath(path).replaceAll("\\[\\d+]", "");
    }

    /**
     * 提取字段路径中的第一个集合下标。
     *
     * @param path 字段路径
     * @return 第一个集合下标
     */
    public static OptionalInt extractIndexFromPath(String path) {
        Matcher matcher = INDEX_PATTERN.matcher(normalizeFieldPath(path));
        if (!matcher.find()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException ex) {
            return OptionalInt.empty();
        }
    }

    /**
     * 提取字段路径中的全部集合下标。
     *
     * @param path 字段路径
     * @return 集合下标列表
     */
    public static List<Integer> extractIndexesFromPath(String path) {
        Matcher matcher = INDEX_PATTERN.matcher(normalizeFieldPath(path));
        List<Integer> indexes = new ArrayList<>();
        while (matcher.find()) {
            try {
                indexes.add(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                // 正则已限制数字，理论上不会进入该分支。
            }
        }
        return Collections.unmodifiableList(indexes);
    }

    /**
     * 断言对象不能为 null。
     *
     * @param value 待判断对象
     * @param message 异常消息
     * @param <T> 对象类型
     * @return 原对象
     */
    public static <T> T notNull(T value, String message) {
        if (value == null) {
            throw new ValidateException(defaultMessage(message, "对象不能为空"));
        }
        return value;
    }

    /**
     * 断言字符串不能为空白。
     *
     * @param value 待判断字符串
     * @param message 异常消息
     * @return 原字符串
     */
    public static String notBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidateException(defaultMessage(message, "字符串不能为空"));
        }
        return value;
    }

    /**
     * 断言集合不能为空。
     *
     * @param value 待判断集合
     * @param message 异常消息
     * @param <T> 集合类型
     * @return 原集合
     */
    public static <T extends Collection<?>> T notEmpty(T value, String message) {
        if (value == null || value.isEmpty()) {
            throw new ValidateException(defaultMessage(message, "集合不能为空"));
        }
        return value;
    }

    /**
     * 断言 Map 不能为空。
     *
     * @param value 待判断 Map
     * @param message 异常消息
     * @param <T> Map 类型
     * @return 原 Map
     */
    public static <T extends Map<?, ?>> T notEmpty(T value, String message) {
        if (value == null || value.isEmpty()) {
            throw new ValidateException(defaultMessage(message, "Map 不能为空"));
        }
        return value;
    }

    /**
     * 断言表达式必须为 true。
     *
     * @param expression 表达式
     * @param message 异常消息
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new ValidateException(defaultMessage(message, "表达式必须为 true"));
        }
    }

    /**
     * 断言表达式必须为 false。
     *
     * @param expression 表达式
     * @param message 异常消息
     */
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw new ValidateException(defaultMessage(message, "表达式必须为 false"));
        }
    }

    /**
     * 断言两个对象必须相等。
     *
     * @param expected 期望值
     * @param actual 实际值
     * @param message 异常消息
     */
    public static void equals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new ValidateException(defaultMessage(message, "两个对象必须相等"));
        }
    }

    /**
     * 断言两个对象必须不相等。
     *
     * @param expected 期望值
     * @param actual 实际值
     * @param message 异常消息
     */
    public static void notEquals(Object expected, Object actual, String message) {
        if (Objects.equals(expected, actual)) {
            throw new ValidateException(defaultMessage(message, "两个对象必须不相等"));
        }
    }

    /**
     * 断言值必须在候选集合中。
     *
     * @param value 待判断值
     * @param candidates 候选集合
     * @param message 异常消息
     * @param <T> 值类型
     * @return 原值
     */
    public static <T> T in(T value, Collection<? extends T> candidates, String message) {
        if (candidates == null || !candidates.contains(value)) {
            throw new ValidateException(defaultMessage(message, "值不在允许范围内"));
        }
        return value;
    }

    /**
     * 断言值必须不在候选集合中。
     *
     * @param value 待判断值
     * @param candidates 候选集合
     * @param message 异常消息
     * @param <T> 值类型
     * @return 原值
     */
    public static <T> T notIn(T value, Collection<? extends T> candidates, String message) {
        if (candidates != null && candidates.contains(value)) {
            throw new ValidateException(defaultMessage(message, "值不能在禁止范围内"));
        }
        return value;
    }

    private static Validator requireValidator(Validator validator) {
        return Objects.requireNonNull(validator, "Validator 不能为空");
    }

    private static <T> T requireBean(T bean) {
        if (bean == null) {
            throw new IllegalArgumentException("待校验对象不能为空");
        }
        return bean;
    }

    private static <T> Class<T> requireBeanType(Class<T> beanType) {
        return Objects.requireNonNull(beanType, "Bean 类型不能为空");
    }

    private static <T> T requireTarget(T target) {
        if (target == null) {
            throw new IllegalArgumentException("目标对象不能为空");
        }
        return target;
    }

    private static Method requireMethod(Method method) {
        return Objects.requireNonNull(method, "方法对象不能为空");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static Class<?>[] safeGroups(Class<?>... groups) {
        if (groups == null || groups.length == 0) {
            return new Class<?>[0];
        }
        Class<?>[] safeGroups = new Class<?>[groups.length];
        for (int i = 0; i < groups.length; i++) {
            if (groups[i] == null) {
                throw new IllegalArgumentException("校验分组不能包含 null");
            }
            safeGroups[i] = groups[i];
        }
        return safeGroups;
    }

    private static <T> Collection<T> requireCollection(Collection<T> beans) {
        if (beans == null) {
            throw new IllegalArgumentException("对象集合不能为空");
        }
        for (T bean : beans) {
            if (bean == null) {
                throw new IllegalArgumentException("对象集合不能包含 null 元素");
            }
        }
        return beans;
    }

    private static Object[] safeArguments(Method method, Object[] args) {
        Method safeMethod = requireMethod(method);
        Object[] safeArgs = args == null ? new Object[0] : args.clone();
        if (safeMethod.getParameterCount() != safeArgs.length) {
            throw new IllegalArgumentException("方法实参数量与形参数量不一致");
        }
        return safeArgs;
    }

    private static ValidateError toValidateError(ConstraintViolation<?> violation) {
        String fieldPath = getFieldPath(violation);
        return ValidateError.of(getLeafFieldName(violation), getMessage(violation), violation == null ? null : violation.getInvalidValue(), getMessageTemplate(violation), fieldPath);
    }

    private static String defaultMessage(String message, String defaultMessage) {
        return message == null || message.isBlank() ? defaultMessage : message;
    }

    @SuppressWarnings("unused")
    private static <T> Set<T> immutableSet(Collection<T> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }
}
