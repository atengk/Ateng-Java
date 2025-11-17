package local.ateng.java.customutils.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 断言工具类
 * 提供常用的断言方法，用于校验参数或业务状态
 * 校验不通过时抛出调用方自定义异常
 * 所有方法均为静态方法，支持 Supplier 异常懒加载
 * <p>
 * 使用示例：
 * AssertUtil.notNull(obj, () -> new BusinessException("USER_ID_EMPTY", "用户ID不能为空"));
 *
 * @author Ateng
 * @since 2025-11-17
 */
public final class AssertUtil {

    /**
     * 禁止实例化工具类
     */
    private AssertUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    // ==================== 基础断言 ====================

    /**
     * 断言表达式为 true，否则抛出调用方提供的异常
     *
     * @param expression        断言表达式
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当表达式为 false 时抛出
     */
    public static void isTrue(boolean expression, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!expression) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言表达式为 false，否则抛出调用方提供的异常
     *
     * @param expression        断言表达式
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当表达式为 true 时抛出
     */
    public static void isFalse(boolean expression, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (expression) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言所有布尔表达式均为 true，否则抛出调用方提供的异常
     *
     * @param exceptionSupplier 异常供应器
     * @param expressions       可变布尔表达式数组
     * @throws RuntimeException 当任意表达式为 false 时抛出
     */
    public static void allTrue(Supplier<? extends RuntimeException> exceptionSupplier, boolean... expressions) {
        if (expressions == null) {
            throw exceptionSupplier.get();
        }
        for (boolean expr : expressions) {
            if (!expr) {
                throw exceptionSupplier.get();
            }
        }
    }

    /**
     * 断言至少有一个布尔表达式为 true，否则抛出调用方提供的异常
     *
     * @param exceptionSupplier 异常供应器
     * @param expressions       可变布尔表达式数组
     * @throws RuntimeException 当所有表达式均为 false 时抛出
     */
    public static void anyTrue(Supplier<? extends RuntimeException> exceptionSupplier, boolean... expressions) {
        if (expressions == null || expressions.length == 0) {
            throw exceptionSupplier.get();
        }
        for (boolean expr : expressions) {
            if (expr) {
                return;
            }
        }
        throw exceptionSupplier.get();
    }

    /**
     * 断言对象不为 null，否则抛出调用方提供的异常
     *
     * @param object            被检查对象
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当对象为 null 时抛出
     */
    public static void notNull(Object object, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (object == null) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言对象为 null，否则抛出调用方提供的异常
     *
     * @param object            被检查对象
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当对象不为 null 时抛出
     */
    public static void isNull(Object object, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (object != null) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言字符串不为 null 且不为空，否则抛出调用方提供的异常
     *
     * @param text              被检查字符串
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当字符串为 null 或空串时抛出
     */
    public static void notEmpty(String text, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (text == null || text.isEmpty()) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言字符串不为 null 且不为空白，否则抛出调用方提供的异常
     *
     * @param text              被检查字符串
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当字符串为 null 或仅包含空白字符时抛出
     */
    public static void notBlank(String text, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (text == null || text.trim().isEmpty()) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言数组不为 null 且长度大于 0，否则抛出调用方提供的异常
     *
     * @param array             被检查数组
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当数组为 null 或长度为 0 时抛出
     */
    public static void notEmpty(Object[] array, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (array == null || array.length == 0) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言集合不为 null 且不为空，否则抛出调用方提供的异常
     *
     * @param collection        被检查集合
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当集合为 null 或为空时抛出
     */
    public static void notEmpty(Collection<?> collection, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (collection == null || collection.isEmpty()) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言 Map 不为 null 且不为空，否则抛出调用方提供的异常
     *
     * @param map               被检查 Map
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当 Map 为 null 或为空时抛出
     */
    public static void notEmpty(Map<?, ?> map, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (map == null || map.isEmpty()) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言集合为空（null 或无元素），否则抛出调用方提供的异常
     *
     * @param collection        被检查集合
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当集合不为空时抛出
     */
    public static void isEmpty(Collection<?> collection, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (collection != null && !collection.isEmpty()) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言 Map 为空（null 或无元素），否则抛出调用方提供的异常
     *
     * @param map               被检查 Map
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当 Map 不为空时抛出
     */
    public static void isEmpty(Map<?, ?> map, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (map != null && !map.isEmpty()) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言数组为空（null 或长度为 0），否则抛出调用方提供的异常
     *
     * @param array             被检查数组
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当数组不为空时抛出
     */
    public static void isEmpty(Object[] array, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (array != null && array.length > 0) {
            throw exceptionSupplier.get();
        }
    }

    // ==================== 数值类断言 ====================

    /**
     * 断言 value > target，否则抛出调用方提供的异常
     *
     * @param value             被检查数值
     * @param target            比较目标数值
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当 value <= target 时抛出
     */
    public static void greaterThan(Number value, Number target, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null || target == null || value.doubleValue() <= target.doubleValue()) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言 value >= target，否则抛出调用方提供的异常
     *
     * @param value             被检查数值
     * @param target            比较目标数值
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当 value < target 时抛出
     */
    public static void greaterOrEqual(Number value, Number target, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null || target == null || value.doubleValue() < target.doubleValue()) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言 value < target，否则抛出调用方提供的异常
     *
     * @param value             被检查数值
     * @param target            比较目标数值
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当 value >= target 时抛出
     */
    public static void lessThan(Number value, Number target, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null || target == null || value.doubleValue() >= target.doubleValue()) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言 value <= target，否则抛出调用方提供的异常
     *
     * @param value             被检查数值
     * @param target            比较目标数值
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当 value > target 时抛出
     */
    public static void lessOrEqual(Number value, Number target, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null || target == null || value.doubleValue() > target.doubleValue()) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言 value == target，否则抛出调用方提供的异常
     *
     * @param value             被检查数值
     * @param target            比较目标数值
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当 value 不等于 target 时抛出
     */
    public static void equals(Number value, Number target, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null || target == null || Double.compare(value.doubleValue(), target.doubleValue()) != 0) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言 value != target，否则抛出调用方提供的异常
     *
     * @param value             被检查数值
     * @param target            比较目标数值
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当 value 等于 target 时抛出
     */
    public static void notEquals(Number value, Number target, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null || target == null || Double.compare(value.doubleValue(), target.doubleValue()) == 0) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言数值在指定区间 [min, max] 内，否则抛出调用方提供的异常
     *
     * @param value             被检查数值
     * @param min               区间最小值（包含）
     * @param max               区间最大值（包含）
     * @param exceptionSupplier 异常供应器
     * @throws RuntimeException 当数值为 null 或不在区间内时抛出
     */
    public static void between(Number value, Number min, Number max, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null || min == null || max == null) {
            throw exceptionSupplier.get();
        }
        double val = value.doubleValue();
        if (val < min.doubleValue() || val > max.doubleValue()) {
            throw exceptionSupplier.get();
        }
    }

    // ==================== 字符串格式类断言 ====================

    /**
     * 断言字符串符合指定正则，否则抛出调用方提供的异常
     *
     * @param text              被检查字符串
     * @param regex             正则表达式
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当字符串为 null 或不匹配正则时抛出
     */
    public static void matches(String text, String regex, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (text == null || !Pattern.matches(regex, text)) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言字符串为合法邮箱，否则抛出调用方提供的异常
     *
     * @param text              被检查邮箱字符串
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当字符串为 null 或不是合法邮箱格式时抛出
     */
    public static void isEmail(String text, Supplier<? extends RuntimeException> exceptionSupplier) {
        String emailRegex = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
        matches(text, emailRegex, exceptionSupplier);
    }

    /**
     * 断言字符串为合法手机号（中国手机号），否则抛出调用方提供的异常
     *
     * @param text              被检查手机号字符串
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当字符串为 null 或不是合法手机号格式时抛出
     */
    public static void isMobile(String text, Supplier<? extends RuntimeException> exceptionSupplier) {
        String mobileRegex = "^1[3-9]\\d{9}$";
        matches(text, mobileRegex, exceptionSupplier);
    }

    /**
     * 断言字符串长度在指定区间 [min, max] 内，否则抛出调用方提供的异常
     *
     * @param text              被检查字符串
     * @param min               最小长度（包含）
     * @param max               最大长度（包含）
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当字符串为 null 或长度不在区间内时抛出
     */
    public static void lengthBetween(String text, int min, int max, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (text == null || text.length() < min || text.length() > max) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言字符串以指定前缀开头，否则抛出调用方提供的异常
     *
     * @param text              被检查字符串
     * @param prefix            指定前缀
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当字符串为 null 或不以 prefix 开头时抛出
     */
    public static void startsWith(String text, String prefix, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (text == null || prefix == null || !text.startsWith(prefix)) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言字符串以指定后缀结尾，否则抛出调用方提供的异常
     *
     * @param text              被检查字符串
     * @param suffix            指定后缀
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当字符串为 null 或不以 suffix 结尾时抛出
     */
    public static void endsWith(String text, String suffix, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (text == null || suffix == null || !text.endsWith(suffix)) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言字符串包含指定子串，否则抛出调用方提供的异常
     *
     * @param text              被检查字符串
     * @param substring         指定子串
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当字符串为 null 或不包含 substring 时抛出
     */
    public static void contains(String text, String substring, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (text == null || substring == null || !text.contains(substring)) {
            throw exceptionSupplier.get();
        }
    }

    // ==================== 集合 / 数组类断言 ====================

    /**
     * 断言集合包含指定元素，否则抛出调用方提供的异常
     *
     * @param collection        被检查集合
     * @param element           元素
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当集合为 null 或不包含元素时抛出
     */
    public static void contains(Collection<?> collection, Object element, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (collection == null || !collection.contains(element)) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言集合不包含指定元素，否则抛出调用方提供的异常
     *
     * @param collection        被检查集合
     * @param element           元素
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当集合包含该元素时抛出
     */
    public static void notContains(Collection<?> collection, Object element, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (collection != null && collection.contains(element)) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言集合大小在指定区间 [min, max] 内，否则抛出调用方提供的异常
     *
     * @param collection        被检查集合
     * @param min               最小大小（包含）
     * @param max               最大大小（包含）
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当集合为 null 或大小不在区间内时抛出
     */
    public static void sizeBetween(Collection<?> collection, int min, int max, Supplier<? extends RuntimeException> exceptionSupplier) {
        int size = collection == null ? 0 : collection.size();
        if (size < min || size > max) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言集合大小等于指定值，否则抛出调用方提供的异常
     *
     * @param collection        被检查集合
     * @param size              期望大小
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当集合为 null 或大小不等于指定值时抛出
     */
    public static void sizeEquals(Collection<?> collection, int size, Supplier<? extends RuntimeException> exceptionSupplier) {
        int actualSize = collection == null ? 0 : collection.size();
        if (actualSize != size) {
            throw exceptionSupplier.get();
        }
    }


    // ==================== 对象比较断言 ====================

    /**
     * 断言两个对象值相等，否则抛出调用方提供的异常
     *
     * @param a                 被比较对象 a
     * @param b                 被比较对象 b
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当 a 和 b 值不相等时抛出
     */
    public static void equals(Object a, Object b, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!Objects.equals(a, b)) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言两个对象值不相等，否则抛出调用方提供的异常
     *
     * @param a                 被比较对象 a
     * @param b                 被比较对象 b
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当 a 和 b 值相等时抛出
     */
    public static void notEquals(Object a, Object b, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (Objects.equals(a, b)) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言两个对象引用相同，否则抛出调用方提供的异常
     *
     * @param a                 被比较对象 a
     * @param b                 被比较对象 b
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当 a 和 b 不是同一个引用时抛出
     */
    public static void same(Object a, Object b, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (a != b) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 断言两个对象引用不同，否则抛出调用方提供的异常
     *
     * @param a                 被比较对象 a
     * @param b                 被比较对象 b
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当 a 和 b 是同一个引用时抛出
     */
    public static void notSame(Object a, Object b, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (a == b) {
            throw exceptionSupplier.get();
        }
    }

    // ==================== 其他 ====================

    /**
     * 手动抛出调用方提供的异常
     *
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 始终抛出调用方提供的异常
     */
    public static void fail(Supplier<? extends RuntimeException> exceptionSupplier) {
        throw exceptionSupplier.get();
    }

    /**
     * 断言业务状态为 true，否则抛出调用方提供的异常
     *
     * @param expression        业务状态表达式
     * @param exceptionSupplier 异常供应器，用于提供自定义异常
     * @throws RuntimeException 当 expression 为 false 时抛出
     */
    public static void state(boolean expression, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!expression) {
            throw exceptionSupplier.get();
        }
    }

}
