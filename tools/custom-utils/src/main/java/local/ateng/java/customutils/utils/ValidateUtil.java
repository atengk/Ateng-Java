package local.ateng.java.customutils.utils;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 校验工具类
 * 提供常见的参数校验方法
 *
 * @author Ateng
 * @since 2025-07-28
 */
public final class ValidateUtil {

    /**
     * 禁止实例化工具类
     */
    private ValidateUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 判断对象是否为 null
     *
     * @param obj 任意对象
     * @return 为 null 返回 true，否则返回 false
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }

    /**
     * 判断对象是否不为 null
     *
     * @param obj 任意对象
     * @return 不为 null 返回 true，否则返回 false
     */
    public static boolean isNotNull(Object obj) {
        return obj != null;
    }

    /**
     * 判断集合是否为 null 或空
     *
     * @param collection 集合对象
     * @return 为 null 或空集合返回 true，否则返回 false
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断 Map 是否为 null 或空
     *
     * @param map Map 对象
     * @return 为 null 或空返回 true，否则返回 false
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 判断字符串是否符合指定正则表达式
     *
     * @param str     待校验字符串
     * @param pattern 正则表达式
     * @return 匹配返回 true，不匹配或 str 为 null 返回 false
     */
    public static boolean matches(String str, String pattern) {
        return str != null && Pattern.matches(pattern, str);
    }

    /**
     * 校验是否为合法手机号（仅中国大陆）
     *
     * @param phone 手机号字符串
     * @return 合法返回 true，否则返回 false
     */
    public static boolean isMobile(String phone) {
        return matches(phone, "^1[3-9]\\d{9}$");
    }

    /**
     * 校验是否为合法邮箱
     *
     * @param email 邮箱地址
     * @return 合法返回 true，否则返回 false
     */
    public static boolean isEmail(String email) {
        return matches(email, "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    /**
     * 校验是否为正整数
     *
     * @param str 输入字符串
     * @return 是正整数返回 true，否则返回 false
     */
    public static boolean isPositiveInteger(String str) {
        return matches(str, "^[1-9]\\d*$");
    }

    /**
     * 校验是否为整数（可正可负）
     *
     * @param str 输入字符串
     * @return 是整数返回 true，否则返回 false
     */
    public static boolean isInteger(String str) {
        return matches(str, "^-?\\d+$");
    }

    /**
     * 校验实体对象字段上的注解，如 @NotNull、@NotBlank 等
     *
     * @param object 要校验的对象
     * @return 如果无违规，返回 null；否则返回第一个错误信息
     */
    public static String validateBean(Object object) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        Set<ConstraintViolation<Object>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            // 返回第一条校验错误信息
            return violations.iterator().next().getMessage();
        }
        return null;
    }

    /**
     * 校验实体对象字段上的注解，如 @NotNull、@NotBlank 等
     *
     * @param object 要校验的对象
     * @return 如果无违规，返回空集合；否则返回所有错误信息
     */
    public static List<String> validateBeanAll(Object object) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        Set<ConstraintViolation<Object>> violations = validator.validate(object);
        List<String> errors = new ArrayList<>();
        for (ConstraintViolation<Object> violation : violations) {
            errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
        }
        return errors;
    }

}
