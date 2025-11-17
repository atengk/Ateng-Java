package local.ateng.java.customutils.utils;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.groups.Default;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 校验工具类
 * 提供常见的参数校验方法
 *
 * @author Ateng
 * @since 2025-07-28
 */
public final class ValidateUtil {

    /**
     * Validator 为线程安全对象，可在全局中复用。
     */
    private static final Validator VALIDATOR;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        VALIDATOR = factory.getValidator();
    }

    /**
     * 禁止实例化工具类
     */
    private ValidateUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 校验对象字段，返回第一条错误信息，不包含字段名。
     *
     * @param object 校验对象
     * @return 无错误返回 null，否则返回错误描述
     */
    public static String validateFirst(Object object) {
        return validateFirst(object, false);
    }

    /**
     * 校验对象字段，返回第一条错误信息。
     *
     * @param object                校验对象
     * @param includePropertyPath   是否包含字段名
     * @return 无错误返回 null，否则返回错误描述
     */
    public static String validateFirst(Object object, boolean includePropertyPath) {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(object);
        if (!violations.isEmpty()) {
            ConstraintViolation<Object> violation = violations.iterator().next();
            if (includePropertyPath) {
                return violation.getPropertyPath() + " " + violation.getMessage();
            }
            return violation.getMessage();
        }
        return null;
    }

    /**
     * 校验对象字段，返回所有错误信息，不包含字段名。
     *
     * @param object 校验对象
     * @return 错误列表，无错误返回空集合
     */
    public static List<String> validateAll(Object object) {
        return validateAll(object, false);
    }

    /**
     * 校验对象字段，返回所有错误信息。
     *
     * @param object                校验对象
     * @param includePropertyPath   是否包含字段名
     * @return 错误列表，无错误返回空集合
     */
    public static List<String> validateAll(Object object, boolean includePropertyPath) {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(object);
        List<String> errors = new ArrayList<>();

        for (ConstraintViolation<Object> violation : violations) {
            if (includePropertyPath) {
                errors.add(violation.getPropertyPath() + " " + violation.getMessage());
            } else {
                errors.add(violation.getMessage());
            }
        }
        return errors;
    }

    /**
     * 校验对象字段，校验失败则抛出 IllegalArgumentException。
     * 默认不展示字段名。
     *
     * @param object 校验对象
     */
    public static void validateThrow(Object object) {
        validateThrow(object, false);
    }

    /**
     * 校验对象字段，校验失败则抛出 IllegalArgumentException。
     *
     * @param object               校验对象
     * @param includePropertyPath  是否包含字段名
     */
    public static void validateThrow(Object object, boolean includePropertyPath) {
        String error = validateFirst(object, includePropertyPath);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
    }

    /**
     * 校验对象字段，校验失败时抛出指定的异常。
     * 默认不展示字段名。
     *
     * @param object   校验对象
     * @param exception 业务自定义异常
     */
    public static void validateThrow(Object object, RuntimeException exception) {
        String error = validateFirst(object);
        if (error != null) {
            throw exception;
        }
    }

    /**
     * 校验对象字段，返回字段名与错误信息的映射。
     *
     * @param object 校验对象
     * @return 字段名与错误信息映射，无错误返回空 Map
     */
    public static Map<String, String> validateToMap(Object object) {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(object);
        Map<String, String> errorMap = new LinkedHashMap<>();

        for (ConstraintViolation<Object> violation : violations) {
            String field = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errorMap.put(field, message);
        }
        return errorMap;
    }

    /**
     * 分组校验，返回第一条错误信息，不包含字段名。
     *
     * @param object 被校验对象
     * @param groups 校验分组
     * @return 第一条错误信息，无错误返回 null
     */
    public static String validateFirst(Object object, Class<?>... groups) {
        return validateFirst(object, false, groups);
    }

    /**
     * 分组校验，返回第一条错误信息。
     *
     * @param object               被校验对象
     * @param includePropertyPath  是否包含字段名
     * @param groups               校验分组
     * @return 第一条错误信息，无错误返回 null
     */
    public static String validateFirst(Object object, boolean includePropertyPath, Class<?>... groups) {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(object, groups);
        if (!violations.isEmpty()) {
            ConstraintViolation<Object> violation = violations.iterator().next();
            if (includePropertyPath) {
                return violation.getPropertyPath() + " " + violation.getMessage();
            }
            return violation.getMessage();
        }
        return null;
    }

    /**
     * 分组校验，返回所有错误信息，不包含字段名。
     *
     * @param object 被校验对象
     * @param groups 校验分组
     * @return 错误信息列表，无错误返回空列表
     */
    public static List<String> validateAll(Object object, Class<?>... groups) {
        return validateAll(object, false, groups);
    }

    /**
     * 分组校验，返回所有错误信息。
     *
     * @param object               被校验对象
     * @param includePropertyPath  是否包含字段名
     * @param groups               校验分组
     * @return 错误列表，无错误返回空列表
     */
    public static List<String> validateAll(Object object, boolean includePropertyPath, Class<?>... groups) {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(object, groups);
        List<String> errors = new ArrayList<>();

        for (ConstraintViolation<Object> violation : violations) {
            if (includePropertyPath) {
                errors.add(violation.getPropertyPath() + " " + violation.getMessage());
            } else {
                errors.add(violation.getMessage());
            }
        }
        return errors;
    }

    /**
     * 校验指定字段，返回该字段的第一条错误信息，不包含字段名。
     *
     * @param object       校验对象
     * @param propertyName 字段名称
     * @return 第一条错误信息，无错误返回 null
     */
    public static <T> String validatePropertyFirst(T object, String propertyName) {
        return validatePropertyFirst(object, propertyName, false);
    }

    /**
     * 校验指定字段，返回该字段的第一条错误信息。
     *
     * @param object               校验对象
     * @param propertyName         字段名称
     * @param includePropertyPath  是否包含字段名
     * @return 第一条错误信息，无错误返回 null
     */
    public static <T> String validatePropertyFirst(T object, String propertyName, boolean includePropertyPath) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validateProperty(object, propertyName);
        if (!violations.isEmpty()) {
            ConstraintViolation<T> violation = violations.iterator().next();
            if (includePropertyPath) {
                return violation.getPropertyPath() + " " + violation.getMessage();
            }
            return violation.getMessage();
        }
        return null;
    }

    /**
     * 校验指定字段，返回该字段的所有错误信息，不包含字段名。
     *
     * @param object       校验对象
     * @param propertyName 字段名称
     * @return 错误列表，无错误返回空列表
     */
    public static <T> List<String> validateProperty(T object, String propertyName) {
        return validateProperty(object, propertyName, false);
    }

    /**
     * 校验指定字段，返回该字段的所有错误信息。
     *
     * @param object               校验对象
     * @param propertyName         字段名称
     * @param includePropertyPath  是否包含字段名
     * @return 错误列表，无错误返回空列表
     */
    public static <T> List<String> validateProperty(T object, String propertyName, boolean includePropertyPath) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validateProperty(object, propertyName);
        List<String> errors = new ArrayList<>();

        for (ConstraintViolation<T> violation : violations) {
            if (includePropertyPath) {
                errors.add(violation.getPropertyPath() + " " + violation.getMessage());
            } else {
                errors.add(violation.getMessage());
            }
        }
        return errors;
    }

    /**
     * 判断对象是否通过全部校验。
     *
     * @param bean   需要校验的对象
     * @param groups 可选的校验分组；未指定时默认使用 Default 分组
     * @return 校验通过返回 true，否则返回 false
     */
    public static boolean isValid(Object bean, Class<?>... groups) {
        if (bean == null) {
            return false;
        }
        Class<?>[] useGroups = groups == null || groups.length == 0 ? new Class[]{Default.class} : groups;
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(bean, useGroups);
        return violations.isEmpty();
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
        Map<Integer, List<String>> errorMap = new LinkedHashMap<>();
        if (collection == null || collection.isEmpty()) {
            return errorMap;
        }

        Class<?>[] useGroups = (groups == null || groups.length == 0) ? new Class[]{Default.class} : groups;

        int index = 0;
        for (Object item : collection) {
            Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(item, useGroups);
            if (!violations.isEmpty()) {
                List<String> messages = violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.toList());
                errorMap.put(index, messages);
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

        if (bean == null) {
            onInvalid.accept(Collections.singletonList("验证对象不能为空"));
            return;
        }

        Class<?>[] useGroups = (groups == null || groups.length == 0) ? new Class[]{Default.class} : groups;
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(bean, useGroups);

        if (violations.isEmpty()) {
            onValid.run();
        } else {
            List<String> messages = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.toList());
            onInvalid.accept(messages);
        }
    }

}
