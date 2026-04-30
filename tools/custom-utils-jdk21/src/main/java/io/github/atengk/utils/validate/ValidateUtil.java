package io.github.atengk.utils.validate;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ArrayUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.groups.Default;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 校验工具类
 * 基于 Spring Boot 3、Jakarta Validation 和 JDK 21，提供对象校验、分组校验、字段校验、集合校验、方法参数校验等常用能力。
 *
 * @author Ateng
 * @since 2026-04-27
 */
public final class ValidateUtil {

    private static final String NULL_BEAN_MESSAGE = "验证对象不能为空";
    private static final String BLANK_PROPERTY_MESSAGE = "字段名称不能为空";
    private static final String DEFAULT_SEPARATOR = " ";
    private static final String ROOT_PROPERTY = "";

    /**
     * 默认 ValidatorFactory 仅用于非 Spring 容器场景。
     * Spring Boot 项目中建议通过 setValidator 注入容器管理的 Validator。
     */
    private static final ValidatorFactory DEFAULT_VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    /**
     * Validator 为线程安全对象，可全局复用。
     */
    private static volatile Validator validator = DEFAULT_VALIDATOR_FACTORY.getValidator();

    private static final Comparator<ValidateError> ERROR_COMPARATOR = Comparator
            .comparing(ValidateError::propertyPath, Comparator.nullsFirst(String::compareTo))
            .thenComparing(ValidateError::message, Comparator.nullsFirst(String::compareTo));

    private ValidateUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 校验错误信息。
     *
     * @param propertyPath 属性路径
     * @param message      错误消息
     * @param invalidValue 非法值
     * @author Ateng
     * @since 2026-04-27
     */
    public record ValidateError(String propertyPath, String message, Object invalidValue) {

        /**
         * 格式化错误信息。
         *
         * @param includePropertyPath 是否包含字段路径
         * @return 格式化后的错误信息
         */
        public String format(boolean includePropertyPath) {
            if (includePropertyPath && CharSequenceUtil.isNotBlank(propertyPath)) {
                return propertyPath + DEFAULT_SEPARATOR + message;
            }
            return message;
        }
    }

    /**
     * 设置 Validator。
     * 用于接入 Spring Boot 容器管理的 Validator，以便复用 Spring 的消息源和自定义约束校验器。
     *
     * @param springValidator Spring 容器中的 Validator
     */
    public static void setValidator(Validator springValidator) {
        validator = Objects.requireNonNull(springValidator, "Validator 不能为空");
    }

    /**
     * 获取当前 Validator。
     *
     * @return 当前 Validator
     */
    public static Validator getValidator() {
        return validator;
    }

    /**
     * 获取方法级校验器。
     *
     * @return 方法级校验器
     */
    public static ExecutableValidator getExecutableValidator() {
        return validator.forExecutables();
    }

    /**
     * 校验对象字段，返回第一条错误信息，不包含字段名。
     *
     * @param bean 校验对象
     * @return 无错误返回 null，否则返回错误描述
     */
    public static String validateFirst(Object bean) {
        return validateFirst(bean, false);
    }

    /**
     * 校验对象字段，返回第一条错误信息。
     *
     * @param bean                校验对象
     * @param includePropertyPath 是否包含字段名
     * @return 无错误返回 null，否则返回错误描述
     */
    public static String validateFirst(Object bean, boolean includePropertyPath) {
        return validateFirst(bean, includePropertyPath, (Class<?>[]) null);
    }

    /**
     * 分组校验，返回第一条错误信息，不包含字段名。
     *
     * @param bean   被校验对象
     * @param groups 校验分组
     * @return 第一条错误信息，无错误返回 null
     */
    public static String validateFirst(Object bean, Class<?>... groups) {
        return validateFirst(bean, false, groups);
    }

    /**
     * 分组校验，返回第一条错误信息。
     *
     * @param bean                被校验对象
     * @param includePropertyPath 是否包含字段名
     * @param groups              校验分组
     * @return 第一条错误信息，无错误返回 null
     */
    public static String validateFirst(Object bean, boolean includePropertyPath, Class<?>... groups) {
        List<ValidateError> errors = validateErrors(bean, groups);
        return CollUtil.isEmpty(errors) ? null : errors.getFirst().format(includePropertyPath);
    }

    /**
     * 校验对象字段，返回所有错误信息，不包含字段名。
     *
     * @param bean 校验对象
     * @return 错误列表，无错误返回空集合
     */
    public static List<String> validateAll(Object bean) {
        return validateAll(bean, false);
    }

    /**
     * 校验对象字段，返回所有错误信息。
     *
     * @param bean                校验对象
     * @param includePropertyPath 是否包含字段名
     * @return 错误列表，无错误返回空集合
     */
    public static List<String> validateAll(Object bean, boolean includePropertyPath) {
        return validateAll(bean, includePropertyPath, (Class<?>[]) null);
    }

    /**
     * 分组校验，返回所有错误信息，不包含字段名。
     *
     * @param bean   被校验对象
     * @param groups 校验分组
     * @return 错误信息列表，无错误返回空列表
     */
    public static List<String> validateAll(Object bean, Class<?>... groups) {
        return validateAll(bean, false, groups);
    }

    /**
     * 分组校验，返回所有错误信息。
     *
     * @param bean                被校验对象
     * @param includePropertyPath 是否包含字段名
     * @param groups              校验分组
     * @return 错误列表，无错误返回空列表
     */
    public static List<String> validateAll(Object bean, boolean includePropertyPath, Class<?>... groups) {
        return validateErrors(bean, groups).stream()
                .map(error -> error.format(includePropertyPath))
                .toList();
    }

    /**
     * 校验对象字段，返回结构化错误信息。
     *
     * @param bean   被校验对象
     * @param groups 校验分组
     * @return 结构化错误信息列表
     */
    public static List<ValidateError> validateErrors(Object bean, Class<?>... groups) {
        if (bean == null) {
            return List.of(new ValidateError(ROOT_PROPERTY, NULL_BEAN_MESSAGE, null));
        }
        Set<ConstraintViolation<Object>> violations = validator.validate(bean, getGroups(groups));
        return toErrors(violations);
    }

    /**
     * 校验对象字段，校验失败则抛出 IllegalArgumentException。
     * 默认不展示字段名。
     *
     * @param bean 校验对象
     */
    public static void validateThrow(Object bean) {
        validateThrow(bean, false);
    }

    /**
     * 校验对象字段，校验失败则抛出 IllegalArgumentException。
     *
     * @param bean                校验对象
     * @param includePropertyPath 是否包含字段名
     */
    public static void validateThrow(Object bean, boolean includePropertyPath) {
        validateThrow(bean, includePropertyPath, (Class<?>[]) null);
    }

    /**
     * 分组校验对象字段，校验失败则抛出 IllegalArgumentException。
     *
     * @param bean                校验对象
     * @param includePropertyPath 是否包含字段名
     * @param groups              校验分组
     */
    public static void validateThrow(Object bean, boolean includePropertyPath, Class<?>... groups) {
        String error = validateFirst(bean, includePropertyPath, groups);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
    }

    /**
     * 校验对象字段，校验失败时抛出自定义异常。
     *
     * @param bean              校验对象
     * @param exceptionFunction 异常构造函数
     */
    public static void validateThrow(Object bean, Function<String, ? extends RuntimeException> exceptionFunction) {
        validateThrow(bean, exceptionFunction, false);
    }

    /**
     * 校验对象字段，校验失败时抛出自定义异常。
     *
     * @param bean                校验对象
     * @param exceptionFunction   异常构造函数
     * @param includePropertyPath 是否包含字段名
     * @param groups              校验分组
     */
    public static void validateThrow(
            Object bean,
            Function<String, ? extends RuntimeException> exceptionFunction,
            boolean includePropertyPath,
            Class<?>... groups) {

        Objects.requireNonNull(exceptionFunction, "异常构造函数不能为空");
        String error = validateFirst(bean, includePropertyPath, groups);
        if (error != null) {
            throw exceptionFunction.apply(error);
        }
    }

    /**
     * 校验对象字段，返回字段名与第一条错误信息的映射。
     * 同一字段存在多条错误时仅保留第一条，完整错误列表请使用 validateToMultiMap。
     *
     * @param bean   校验对象
     * @param groups 校验分组
     * @return 字段名与错误信息映射，无错误返回空 Map
     */
    public static Map<String, String> validateToMap(Object bean, Class<?>... groups) {
        Map<String, String> errorMap = new LinkedHashMap<>();
        validateErrors(bean, groups).forEach(error -> errorMap.putIfAbsent(error.propertyPath(), error.message()));
        return errorMap;
    }

    /**
     * 校验对象字段，返回字段名与全部错误信息的映射。
     *
     * @param bean   校验对象
     * @param groups 校验分组
     * @return 字段名与全部错误信息映射，无错误返回空 Map
     */
    public static Map<String, List<String>> validateToMultiMap(Object bean, Class<?>... groups) {
        Map<String, List<String>> errorMap = new LinkedHashMap<>();
        validateErrors(bean, groups).forEach(error -> errorMap
                .computeIfAbsent(error.propertyPath(), key -> new ArrayList<>())
                .add(error.message()));
        return errorMap;
    }

    /**
     * 校验指定字段，返回该字段的第一条错误信息，不包含字段名。
     *
     * @param bean         校验对象
     * @param propertyName 字段名称
     * @param <T>          对象类型
     * @return 第一条错误信息，无错误返回 null
     */
    public static <T> String validatePropertyFirst(T bean, String propertyName) {
        return validatePropertyFirst(bean, propertyName, false);
    }

    /**
     * 校验指定字段，返回该字段的第一条错误信息。
     *
     * @param bean                校验对象
     * @param propertyName        字段名称
     * @param includePropertyPath 是否包含字段名
     * @param <T>                 对象类型
     * @return 第一条错误信息，无错误返回 null
     */
    public static <T> String validatePropertyFirst(T bean, String propertyName, boolean includePropertyPath) {
        return validatePropertyFirst(bean, propertyName, includePropertyPath, (Class<?>[]) null);
    }

    /**
     * 分组校验指定字段，返回该字段的第一条错误信息。
     *
     * @param bean                校验对象
     * @param propertyName        字段名称
     * @param includePropertyPath 是否包含字段名
     * @param groups              校验分组
     * @param <T>                 对象类型
     * @return 第一条错误信息，无错误返回 null
     */
    public static <T> String validatePropertyFirst(T bean, String propertyName, boolean includePropertyPath, Class<?>... groups) {
        List<String> errors = validateProperty(bean, propertyName, includePropertyPath, groups);
        return CollUtil.isEmpty(errors) ? null : errors.getFirst();
    }

    /**
     * 校验指定字段，返回该字段的所有错误信息，不包含字段名。
     *
     * @param bean         校验对象
     * @param propertyName 字段名称
     * @param <T>          对象类型
     * @return 错误列表，无错误返回空列表
     */
    public static <T> List<String> validateProperty(T bean, String propertyName) {
        return validateProperty(bean, propertyName, false);
    }

    /**
     * 校验指定字段，返回该字段的所有错误信息。
     *
     * @param bean                校验对象
     * @param propertyName        字段名称
     * @param includePropertyPath 是否包含字段名
     * @param <T>                 对象类型
     * @return 错误列表，无错误返回空列表
     */
    public static <T> List<String> validateProperty(T bean, String propertyName, boolean includePropertyPath) {
        return validateProperty(bean, propertyName, includePropertyPath, (Class<?>[]) null);
    }

    /**
     * 分组校验指定字段，返回该字段的所有错误信息。
     *
     * @param bean                校验对象
     * @param propertyName        字段名称
     * @param includePropertyPath 是否包含字段名
     * @param groups              校验分组
     * @param <T>                 对象类型
     * @return 错误列表，无错误返回空列表
     */
    public static <T> List<String> validateProperty(T bean, String propertyName, boolean includePropertyPath, Class<?>... groups) {
        if (bean == null) {
            return List.of(NULL_BEAN_MESSAGE);
        }
        if (CharSequenceUtil.isBlank(propertyName)) {
            return List.of(BLANK_PROPERTY_MESSAGE);
        }
        Set<ConstraintViolation<T>> violations = validator.validateProperty(bean, propertyName, getGroups(groups));
        return toErrors(violations).stream()
                .map(error -> error.format(includePropertyPath))
                .toList();
    }

    /**
     * 校验指定类型的字段值，适用于未创建对象实例时的单字段校验。
     *
     * @param beanType            Bean 类型
     * @param propertyName        字段名称
     * @param value               字段值
     * @param includePropertyPath 是否包含字段名
     * @param groups              校验分组
     * @param <T>                 对象类型
     * @return 错误列表，无错误返回空列表
     */
    public static <T> List<String> validateValue(
            Class<T> beanType,
            String propertyName,
            Object value,
            boolean includePropertyPath,
            Class<?>... groups) {

        Objects.requireNonNull(beanType, "Bean 类型不能为空");
        if (CharSequenceUtil.isBlank(propertyName)) {
            return List.of(BLANK_PROPERTY_MESSAGE);
        }
        Set<ConstraintViolation<T>> violations = validator.validateValue(beanType, propertyName, value, getGroups(groups));
        return toErrors(violations).stream()
                .map(error -> error.format(includePropertyPath))
                .toList();
    }

    /**
     * 判断对象是否通过全部校验。
     *
     * @param bean   需要校验的对象
     * @param groups 可选的校验分组；未指定时默认使用 Default 分组
     * @return 校验通过返回 true，否则返回 false
     */
    public static boolean isValid(Object bean, Class<?>... groups) {
        return CollUtil.isEmpty(validateErrors(bean, groups));
    }

    /**
     * 判断对象是否校验失败。
     *
     * @param bean   需要校验的对象
     * @param groups 可选的校验分组；未指定时默认使用 Default 分组
     * @return 校验失败返回 true，否则返回 false
     */
    public static boolean isInvalid(Object bean, Class<?>... groups) {
        return !isValid(bean, groups);
    }

    /**
     * 校验集合中的每一个元素，返回错误信息 Map。
     * key 为元素在集合中的下标，value 为当前元素的所有错误消息。
     *
     * @param collection 需要校验的集合
     * @param groups     可选的校验分组；未指定时默认使用 Default 分组
     * @return 错误消息 Map；若全部通过校验则返回空 Map
     */
    public static Map<Integer, List<String>> validateCollection(Collection<?> collection, Class<?>... groups) {
        return validateCollection(collection, false, groups);
    }

    /**
     * 校验集合中的每一个元素，返回错误信息 Map。
     * key 为元素在集合中的下标，value 为当前元素的所有错误消息。
     *
     * @param collection          需要校验的集合
     * @param includePropertyPath 是否包含字段名
     * @param groups              可选的校验分组；未指定时默认使用 Default 分组
     * @return 错误消息 Map；若全部通过校验则返回空 Map
     */
    public static Map<Integer, List<String>> validateCollection(
            Collection<?> collection,
            boolean includePropertyPath,
            Class<?>... groups) {

        Map<Integer, List<String>> errorMap = new LinkedHashMap<>();
        if (CollUtil.isEmpty(collection)) {
            return errorMap;
        }

        int index = 0;
        for (Object item : collection) {
            List<String> errors = validateAll(item, includePropertyPath, groups);
            if (CollUtil.isNotEmpty(errors)) {
                errorMap.put(index, errors);
            }
            index++;
        }
        return errorMap;
    }

    /**
     * 校验集合中的每一个元素，返回结构化错误信息 Map。
     *
     * @param collection 需要校验的集合
     * @param groups     校验分组
     * @return 结构化错误信息 Map
     */
    public static Map<Integer, List<ValidateError>> validateCollectionErrors(Collection<?> collection, Class<?>... groups) {
        Map<Integer, List<ValidateError>> errorMap = new LinkedHashMap<>();
        if (CollUtil.isEmpty(collection)) {
            return errorMap;
        }

        int index = 0;
        for (Object item : collection) {
            List<ValidateError> errors = validateErrors(item, groups);
            if (CollUtil.isNotEmpty(errors)) {
                errorMap.put(index, errors);
            }
            index++;
        }
        return errorMap;
    }

    /**
     * 校验对象，如果通过则执行 onValid，否则执行 onInvalid，并传入所有错误信息。
     *
     * @param bean      需要校验的对象
     * @param onValid   校验通过时执行的逻辑
     * @param onInvalid 校验失败时执行的逻辑，参数为全部错误消息
     * @param groups    可选分组；未指定时默认使用 Default 分组
     */
    public static void validateOrElse(
            Object bean,
            Runnable onValid,
            Consumer<List<String>> onInvalid,
            Class<?>... groups) {

        validateOrElse(bean, onValid, onInvalid, false, groups);
    }

    /**
     * 校验对象，如果通过则执行 onValid，否则执行 onInvalid，并传入所有错误信息。
     *
     * @param bean                需要校验的对象
     * @param onValid             校验通过时执行的逻辑
     * @param onInvalid           校验失败时执行的逻辑，参数为全部错误消息
     * @param includePropertyPath 是否包含字段名
     * @param groups              可选分组；未指定时默认使用 Default 分组
     */
    public static void validateOrElse(
            Object bean,
            Runnable onValid,
            Consumer<List<String>> onInvalid,
            boolean includePropertyPath,
            Class<?>... groups) {

        Objects.requireNonNull(onValid, "校验通过回调不能为空");
        Objects.requireNonNull(onInvalid, "校验失败回调不能为空");

        List<String> errors = validateAll(bean, includePropertyPath, groups);
        if (CollUtil.isEmpty(errors)) {
            onValid.run();
        } else {
            onInvalid.accept(errors);
        }
    }

    /**
     * 校验方法参数，适用于手动触发方法级参数校验。
     *
     * @param target              目标对象
     * @param method              目标方法
     * @param parameterValues     参数值
     * @param includePropertyPath 是否包含参数路径
     * @param groups              校验分组
     * @param <T>                 目标对象类型
     * @return 错误列表，无错误返回空列表
     */
    public static <T> List<String> validateMethodParameters(
            T target,
            Method method,
            Object[] parameterValues,
            boolean includePropertyPath,
            Class<?>... groups) {

        Objects.requireNonNull(target, "目标对象不能为空");
        Objects.requireNonNull(method, "目标方法不能为空");
        Object[] values = parameterValues == null ? new Object[0] : parameterValues;
        Set<ConstraintViolation<T>> violations = getExecutableValidator()
                .validateParameters(target, method, values, getGroups(groups));
        return toErrors(violations).stream()
                .map(error -> error.format(includePropertyPath))
                .toList();
    }

    /**
     * 校验方法返回值，适用于手动触发方法级返回值校验。
     *
     * @param target              目标对象
     * @param method              目标方法
     * @param returnValue         返回值
     * @param includePropertyPath 是否包含返回值路径
     * @param groups              校验分组
     * @param <T>                 目标对象类型
     * @return 错误列表，无错误返回空列表
     */
    public static <T> List<String> validateMethodReturnValue(
            T target,
            Method method,
            Object returnValue,
            boolean includePropertyPath,
            Class<?>... groups) {

        Objects.requireNonNull(target, "目标对象不能为空");
        Objects.requireNonNull(method, "目标方法不能为空");
        Set<ConstraintViolation<T>> violations = getExecutableValidator()
                .validateReturnValue(target, method, returnValue, getGroups(groups));
        return toErrors(violations).stream()
                .map(error -> error.format(includePropertyPath))
                .toList();
    }

    private static Class<?>[] getGroups(Class<?>... groups) {
        if (ArrayUtil.isEmpty(groups)) {
            return new Class[]{Default.class};
        }
        Class<?>[] filteredGroups = Arrays.stream(groups)
                .filter(Objects::nonNull)
                .distinct()
                .toArray(Class<?>[]::new);
        return ArrayUtil.isEmpty(filteredGroups) ? new Class[]{Default.class} : filteredGroups;
    }

    private static <T> List<ValidateError> toErrors(Set<ConstraintViolation<T>> violations) {
        if (CollUtil.isEmpty(violations)) {
            return List.of();
        }
        return violations.stream()
                .map(ValidateUtil::toError)
                .sorted(ERROR_COMPARATOR)
                .collect(Collectors.toList());
    }

    private static ValidateError toError(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath() == null
                ? ROOT_PROPERTY
                : violation.getPropertyPath().toString();
        return new ValidateError(propertyPath, violation.getMessage(), violation.getInvalidValue());
    }

}
