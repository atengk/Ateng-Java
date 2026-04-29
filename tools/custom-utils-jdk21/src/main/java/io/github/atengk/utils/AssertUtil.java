package io.github.atengk.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 通用断言工具类，覆盖参数、状态、空值、字符串、集合、数组、Map、数值、对象、类型、时间、枚举、正则和文件路径等常用断言场景。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class AssertUtil {

    private static final String DEFAULT_ASSERT_MESSAGE = "断言失败";

    private AssertUtil() {
        throw new UnsupportedOperationException("AssertUtil 不允许实例化");
    }

    /**
     * 创建默认运行时异常。如需全局替换为业务异常，优先修改此方法。
     *
     * @param message 异常消息
     * @return 运行时异常
     */
    private static RuntimeException exception(String message) {
        return new IllegalArgumentException(message);
    }

    /**
     * 创建带原因的默认运行时异常。如需全局替换为业务异常，优先修改此方法。
     *
     * @param message 异常消息
     * @param cause 原始异常
     * @return 运行时异常
     */
    private static RuntimeException exception(String message, Throwable cause) {
        return new IllegalArgumentException(message, cause);
    }

    /**
     * 创建状态异常。如需全局替换状态断言异常，优先修改此方法。
     *
     * @param message 异常消息
     * @return 运行时异常
     */
    private static RuntimeException stateException(String message) {
        return new IllegalStateException(message);
    }

    /**
     * 使用默认异常类型抛出断言异常。
     *
     * @param message 异常消息
     */
    private static void raise(String message) {
        throw exception(message);
    }

    /**
     * 使用默认异常类型抛出带原因的断言异常。
     *
     * @param message 异常消息
     * @param cause 原始异常
     */
    private static void raise(String message, Throwable cause) {
        throw exception(message, cause);
    }

    /**
     * 使用自定义异常提供器抛出断言异常。
     *
     * @param exceptionSupplier 异常提供器
     */
    private static void raise(Supplier<? extends RuntimeException> exceptionSupplier) {
        RuntimeException runtimeException = exceptionSupplier == null ? null : exceptionSupplier.get();
        if (runtimeException == null) {
            throw exception(DEFAULT_ASSERT_MESSAGE);
        }
        throw runtimeException;
    }

    /**
     * 断言表达式必须为 true。
     *
     * @param expression 表达式
     * @param message 异常消息
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            raise(message);
        }
    }

    /**
     * 断言表达式必须为 true。
     *
     * @param expression 表达式
     * @param exceptionSupplier 自定义异常提供器
     */
    public static void isTrue(boolean expression, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!expression) {
            raise(exceptionSupplier);
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
            raise(message);
        }
    }

    /**
     * 断言表达式必须为 false。
     *
     * @param expression 表达式
     * @param exceptionSupplier 自定义异常提供器
     */
    public static void isFalse(boolean expression, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (expression) {
            raise(exceptionSupplier);
        }
    }

    /**
     * 断言业务条件必须成立。
     *
     * @param expression 表达式
     * @param message 异常消息
     */
    public static void valid(boolean expression, String message) {
        isTrue(expression, message);
    }

    /**
     * 断言业务条件必须成立。
     *
     * @param expression 表达式
     * @param exceptionSupplier 自定义异常提供器
     */
    public static void valid(boolean expression, Supplier<? extends RuntimeException> exceptionSupplier) {
        isTrue(expression, exceptionSupplier);
    }

    /**
     * 断言对象状态必须满足。
     *
     * @param expression 表达式
     * @param message 异常消息
     */
    public static void state(boolean expression, String message) {
        if (!expression) {
            throw stateException(message);
        }
    }

    /**
     * 断言对象状态必须满足。
     *
     * @param expression 表达式
     * @param exceptionSupplier 自定义异常提供器
     */
    public static void state(boolean expression, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!expression) {
            raise(exceptionSupplier);
        }
    }

    /**
     * 主动抛出断言异常。
     *
     * @param message 异常消息
     */
    public static void fail(String message) {
        raise(message);
    }

    /**
     * 主动抛出自定义断言异常。
     *
     * @param exceptionSupplier 自定义异常提供器
     */
    public static void fail(Supplier<? extends RuntimeException> exceptionSupplier) {
        raise(exceptionSupplier);
    }

    /**
     * 条件成立时抛出断言异常。
     *
     * @param expression 表达式
     * @param message 异常消息
     */
    public static void failIf(boolean expression, String message) {
        if (expression) {
            raise(message);
        }
    }

    /**
     * 条件成立时抛出自定义断言异常。
     *
     * @param expression 表达式
     * @param exceptionSupplier 自定义异常提供器
     */
    public static void failIf(boolean expression, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (expression) {
            raise(exceptionSupplier);
        }
    }

    /**
     * 值为 null 时抛出断言异常。
     *
     * @param value 待检查值
     * @param message 异常消息
     * @param <T> 值类型
     * @return 原始值
     */
    public static <T> T failIfNull(T value, String message) {
        return notNull(value, message);
    }

    /**
     * 值为 null 时抛出自定义断言异常。
     *
     * @param value 待检查值
     * @param exceptionSupplier 自定义异常提供器
     * @param <T> 值类型
     * @return 原始值
     */
    public static <T> T failIfNull(T value, Supplier<? extends RuntimeException> exceptionSupplier) {
        return notNull(value, exceptionSupplier);
    }

    /**
     * 值不为 null 时抛出断言异常。
     *
     * @param value 待检查值
     * @param message 异常消息
     */
    public static void failIfPresent(Object value, String message) {
        isNull(value, message);
    }

    /**
     * 值不为 null 时抛出自定义断言异常。
     *
     * @param value 待检查值
     * @param exceptionSupplier 自定义异常提供器
     */
    public static void failIfPresent(Object value, Supplier<? extends RuntimeException> exceptionSupplier) {
        isNull(value, exceptionSupplier);
    }

    /**
     * 断言业务条件成立，不成立时抛出自定义业务异常。
     *
     * @param expression 表达式
     * @param exceptionSupplier 自定义异常提供器
     */
    public static void validBiz(boolean expression, Supplier<? extends RuntimeException> exceptionSupplier) {
        valid(expression, exceptionSupplier);
    }

    /**
     * 主动抛出自定义业务异常。
     *
     * @param exceptionSupplier 自定义异常提供器
     */
    public static void failBiz(Supplier<? extends RuntimeException> exceptionSupplier) {
        fail(exceptionSupplier);
    }

    /**
     * 断言对象不能为 null，不满足时抛出自定义业务异常。
     *
     * @param value 待检查值
     * @param exceptionSupplier 自定义异常提供器
     * @param <T> 值类型
     * @return 原始值
     */
    public static <T> T notNullBiz(T value, Supplier<? extends RuntimeException> exceptionSupplier) {
        return notNull(value, exceptionSupplier);
    }

    /**
     * 断言字符串不能为空白，不满足时抛出自定义业务异常。
     *
     * @param value 待检查字符串
     * @param exceptionSupplier 自定义异常提供器
     * @return 原始字符串
     */
    public static String notBlankBiz(String value, Supplier<? extends RuntimeException> exceptionSupplier) {
        return notBlank(value, exceptionSupplier);
    }

    /**
     * 断言对象不能为 null。
     *
     * @param value 待检查对象
     * @param message 异常消息
     * @param <T> 对象类型
     * @return 原始对象
     */
    public static <T> T notNull(T value, String message) {
        if (value == null) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言对象不能为 null。
     *
     * @param value 待检查对象
     * @param exceptionSupplier 自定义异常提供器
     * @param <T> 对象类型
     * @return 原始对象
     */
    public static <T> T notNull(T value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null) {
            raise(exceptionSupplier);
        }
        return value;
    }

    /**
     * 断言对象必须为 null。
     *
     * @param value 待检查对象
     * @param message 异常消息
     */
    public static void isNull(Object value, String message) {
        if (value != null) {
            raise(message);
        }
    }

    /**
     * 断言对象必须为 null。
     *
     * @param value 待检查对象
     * @param exceptionSupplier 自定义异常提供器
     */
    public static void isNull(Object value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value != null) {
            raise(exceptionSupplier);
        }
    }

    /**
     * 断言所有对象均不能为 null。
     *
     * @param message 异常消息
     * @param values 待检查对象
     */
    public static void allNotNull(String message, Object... values) {
        notNull(values, message);
        for (Object value : values) {
            if (value == null) {
                raise(message);
            }
        }
    }

    /**
     * 断言至少一个对象不能为 null。
     *
     * @param message 异常消息
     * @param values 待检查对象
     */
    public static void anyNotNull(String message, Object... values) {
        notNull(values, message);
        for (Object value : values) {
            if (value != null) {
                return;
            }
        }
        raise(message);
    }

    /**
     * 断言所有对象均必须为 null。
     *
     * @param message 异常消息
     * @param values 待检查对象
     */
    public static void allNull(String message, Object... values) {
        notNull(values, message);
        for (Object value : values) {
            if (value != null) {
                raise(message);
            }
        }
    }

    /**
     * 断言 Optional 必须有值。
     *
     * @param optional Optional 对象
     * @param message 异常消息
     * @param <T> 值类型
     * @return 原始 Optional
     */
    public static <T> Optional<T> notEmpty(Optional<T> optional, String message) {
        if (optional == null || optional.isEmpty()) {
            raise(message);
        }
        return optional;
    }

    /**
     * 断言 Optional 必须为空。
     *
     * @param optional Optional 对象
     * @param message 异常消息
     */
    public static void isEmpty(Optional<?> optional, String message) {
        if (optional != null && optional.isPresent()) {
            raise(message);
        }
    }

    /**
     * 断言字符串不能为空白。
     *
     * @param value 待检查字符串
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String notBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言字符串不能为空白。
     *
     * @param value 待检查字符串
     * @param exceptionSupplier 自定义异常提供器
     * @return 原始字符串
     */
    public static String notBlank(String value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null || value.isBlank()) {
            raise(exceptionSupplier);
        }
        return value;
    }

    /**
     * 断言字符串必须为空白。
     *
     * @param value 待检查字符串
     * @param message 异常消息
     */
    public static void isBlank(String value, String message) {
        if (value != null && !value.isBlank()) {
            raise(message);
        }
    }

    /**
     * 断言字符串不能为 null 或空串。
     *
     * @param value 待检查字符串
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String notEmpty(String value, String message) {
        if (value == null || value.isEmpty()) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言字符串必须为 null 或空串。
     *
     * @param value 待检查字符串
     * @param message 异常消息
     */
    public static void isEmpty(String value, String message) {
        if (value != null && !value.isEmpty()) {
            raise(message);
        }
    }

    /**
     * 断言字符串必须包含有效文本。
     *
     * @param value 待检查字符串
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String hasText(String value, String message) {
        return notBlank(value, message);
    }

    /**
     * 断言字符串长度必须等于指定值。
     *
     * @param value 待检查字符串
     * @param length 指定长度
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String length(String value, int length, String message) {
        checkNonNegative(length, "length");
        if (value == null || value.length() != length) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言字符串长度不能小于指定值。
     *
     * @param value 待检查字符串
     * @param min 最小长度
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String minLength(String value, int min, String message) {
        checkNonNegative(min, "min");
        if (value == null || value.length() < min) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言字符串长度不能大于指定值。
     *
     * @param value 待检查字符串
     * @param max 最大长度
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String maxLength(String value, int max, String message) {
        checkNonNegative(max, "max");
        if (value == null || value.length() > max) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言字符串长度必须在指定范围内，包含边界。
     *
     * @param value 待检查字符串
     * @param min 最小长度
     * @param max 最大长度
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String lengthBetween(String value, int min, int max, String message) {
        checkRange(min, max, "min", "max");
        if (value == null || value.length() < min || value.length() > max) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言字符串必须以指定前缀开头。
     *
     * @param value 待检查字符串
     * @param prefix 前缀
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String startsWith(String value, String prefix, String message) {
        notNull(prefix, "prefix 不能为 null");
        if (value == null || !value.startsWith(prefix)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言字符串必须以指定后缀结尾。
     *
     * @param value 待检查字符串
     * @param suffix 后缀
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String endsWith(String value, String suffix, String message) {
        notNull(suffix, "suffix 不能为 null");
        if (value == null || !value.endsWith(suffix)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言字符串必须包含指定内容。
     *
     * @param value 待检查字符串
     * @param keyword 关键字
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String contains(String value, String keyword, String message) {
        notNull(keyword, "keyword 不能为 null");
        if (value == null || !value.contains(keyword)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言字符串不能包含指定内容。
     *
     * @param value 待检查字符串
     * @param keyword 关键字
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String notContains(String value, String keyword, String message) {
        notNull(keyword, "keyword 不能为 null");
        if (value != null && value.contains(keyword)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言字符串必须匹配正则表达式。
     *
     * @param value 待检查字符串
     * @param regex 正则表达式
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String matches(String value, String regex, String message) {
        Pattern pattern = compilePattern(regex);
        if (value == null || !pattern.matcher(value).matches()) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言字符串不能匹配正则表达式。
     *
     * @param value 待检查字符串
     * @param regex 正则表达式
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String notMatches(String value, String regex, String message) {
        Pattern pattern = compilePattern(regex);
        if (value != null && pattern.matcher(value).matches()) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言集合不能为 null 或空。
     *
     * @param value 待检查集合
     * @param message 异常消息
     * @param <C> 集合类型
     * @return 原始集合
     */
    public static <C extends Collection<?>> C notEmpty(C value, String message) {
        if (value == null || value.isEmpty()) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言集合必须为 null 或空。
     *
     * @param value 待检查集合
     * @param message 异常消息
     */
    public static void isEmpty(Collection<?> value, String message) {
        if (value != null && !value.isEmpty()) {
            raise(message);
        }
    }

    /**
     * 断言集合大小必须等于指定值。
     *
     * @param value 待检查集合
     * @param size 指定大小
     * @param message 异常消息
     * @param <C> 集合类型
     * @return 原始集合
     */
    public static <C extends Collection<?>> C size(C value, int size, String message) {
        checkNonNegative(size, "size");
        if (value == null || value.size() != size) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言集合大小不能小于指定值。
     *
     * @param value 待检查集合
     * @param min 最小大小
     * @param message 异常消息
     * @param <C> 集合类型
     * @return 原始集合
     */
    public static <C extends Collection<?>> C minSize(C value, int min, String message) {
        checkNonNegative(min, "min");
        if (value == null || value.size() < min) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言集合大小不能大于指定值。
     *
     * @param value 待检查集合
     * @param max 最大大小
     * @param message 异常消息
     * @param <C> 集合类型
     * @return 原始集合
     */
    public static <C extends Collection<?>> C maxSize(C value, int max, String message) {
        checkNonNegative(max, "max");
        if (value == null || value.size() > max) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言集合大小必须在指定范围内，包含边界。
     *
     * @param value 待检查集合
     * @param min 最小大小
     * @param max 最大大小
     * @param message 异常消息
     * @param <C> 集合类型
     * @return 原始集合
     */
    public static <C extends Collection<?>> C sizeBetween(C value, int min, int max, String message) {
        checkRange(min, max, "min", "max");
        if (value == null || value.size() < min || value.size() > max) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言集合必须包含指定元素。
     *
     * @param value 待检查集合
     * @param element 元素
     * @param message 异常消息
     * @param <C> 集合类型
     * @return 原始集合
     */
    public static <C extends Collection<?>> C contains(C value, Object element, String message) {
        if (value == null || !value.contains(element)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言集合不能包含指定元素。
     *
     * @param value 待检查集合
     * @param element 元素
     * @param message 异常消息
     * @param <C> 集合类型
     * @return 原始集合
     */
    public static <C extends Collection<?>> C notContains(C value, Object element, String message) {
        if (value != null && value.contains(element)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言集合不能包含 null 元素。
     *
     * @param value 待检查集合
     * @param message 异常消息
     * @param <C> 集合类型
     * @return 原始集合
     */
    public static <C extends Collection<?>> C noNullElements(C value, String message) {
        notNull(value, message);
        for (Object item : value) {
            if (item == null) {
                raise(message);
            }
        }
        return value;
    }

    /**
     * 断言集合必须包含 null 元素。
     *
     * @param value 待检查集合
     * @param message 异常消息
     * @param <C> 集合类型
     * @return 原始集合
     */
    public static <C extends Collection<?>> C hasNullElements(C value, String message) {
        notNull(value, message);
        for (Object item : value) {
            if (item == null) {
                return value;
            }
        }
        raise(message);
        return value;
    }

    /**
     * 断言集合元素必须全部满足条件。
     *
     * @param value 待检查集合
     * @param predicate 判断条件
     * @param message 异常消息
     * @param <T> 元素类型
     * @param <C> 集合类型
     * @return 原始集合
     */
    public static <T, C extends Collection<T>> C allMatch(C value, Predicate<? super T> predicate, String message) {
        notNull(value, message);
        notNull(predicate, "predicate 不能为 null");
        if (!value.stream().allMatch(predicate)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言集合元素至少一个满足条件。
     *
     * @param value 待检查集合
     * @param predicate 判断条件
     * @param message 异常消息
     * @param <T> 元素类型
     * @param <C> 集合类型
     * @return 原始集合
     */
    public static <T, C extends Collection<T>> C anyMatch(C value, Predicate<? super T> predicate, String message) {
        notNull(value, message);
        notNull(predicate, "predicate 不能为 null");
        if (!value.stream().anyMatch(predicate)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言集合元素均不满足条件。
     *
     * @param value 待检查集合
     * @param predicate 判断条件
     * @param message 异常消息
     * @param <T> 元素类型
     * @param <C> 集合类型
     * @return 原始集合
     */
    public static <T, C extends Collection<T>> C noneMatch(C value, Predicate<? super T> predicate, String message) {
        notNull(value, message);
        notNull(predicate, "predicate 不能为 null");
        if (!value.stream().noneMatch(predicate)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数组不能为 null 或空。
     *
     * @param value 待检查数组
     * @param message 异常消息
     * @param <T> 元素类型
     * @return 原始数组
     */
    public static <T> T[] notEmpty(T[] value, String message) {
        if (value == null || value.length == 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数组必须为 null 或空。
     *
     * @param value 待检查数组
     * @param message 异常消息
     */
    public static void isEmpty(Object[] value, String message) {
        if (value != null && value.length > 0) {
            raise(message);
        }
    }

    /**
     * 断言数组长度必须等于指定值。
     *
     * @param value 待检查数组
     * @param length 指定长度
     * @param message 异常消息
     * @param <T> 元素类型
     * @return 原始数组
     */
    public static <T> T[] length(T[] value, int length, String message) {
        checkNonNegative(length, "length");
        if (value == null || value.length != length) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数组长度不能小于指定值。
     *
     * @param value 待检查数组
     * @param min 最小长度
     * @param message 异常消息
     * @param <T> 元素类型
     * @return 原始数组
     */
    public static <T> T[] minLength(T[] value, int min, String message) {
        checkNonNegative(min, "min");
        if (value == null || value.length < min) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数组长度不能大于指定值。
     *
     * @param value 待检查数组
     * @param max 最大长度
     * @param message 异常消息
     * @param <T> 元素类型
     * @return 原始数组
     */
    public static <T> T[] maxLength(T[] value, int max, String message) {
        checkNonNegative(max, "max");
        if (value == null || value.length > max) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数组长度必须在指定范围内，包含边界。
     *
     * @param value 待检查数组
     * @param min 最小长度
     * @param max 最大长度
     * @param message 异常消息
     * @param <T> 元素类型
     * @return 原始数组
     */
    public static <T> T[] lengthBetween(T[] value, int min, int max, String message) {
        checkRange(min, max, "min", "max");
        if (value == null || value.length < min || value.length > max) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数组不能包含 null 元素。
     *
     * @param value 待检查数组
     * @param message 异常消息
     * @param <T> 元素类型
     * @return 原始数组
     */
    public static <T> T[] noNullElements(T[] value, String message) {
        notNull(value, message);
        for (T item : value) {
            if (item == null) {
                raise(message);
            }
        }
        return value;
    }

    /**
     * 断言数组必须包含指定元素。
     *
     * @param value 待检查数组
     * @param element 元素
     * @param message 异常消息
     * @param <T> 元素类型
     * @return 原始数组
     */
    public static <T> T[] contains(T[] value, Object element, String message) {
        notNull(value, message);
        for (T item : value) {
            if (Objects.equals(item, element)) {
                return value;
            }
        }
        raise(message);
        return value;
    }

    /**
     * 断言数组不能包含指定元素。
     *
     * @param value 待检查数组
     * @param element 元素
     * @param message 异常消息
     * @param <T> 元素类型
     * @return 原始数组
     */
    public static <T> T[] notContains(T[] value, Object element, String message) {
        if (value != null) {
            for (T item : value) {
                if (Objects.equals(item, element)) {
                    raise(message);
                }
            }
        }
        return value;
    }

    /**
     * 断言 Map 不能为 null 或空。
     *
     * @param value 待检查 Map
     * @param message 异常消息
     * @param <M> Map 类型
     * @return 原始 Map
     */
    public static <M extends Map<?, ?>> M notEmpty(M value, String message) {
        if (value == null || value.isEmpty()) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言 Map 必须为 null 或空。
     *
     * @param value 待检查 Map
     * @param message 异常消息
     */
    public static void isEmpty(Map<?, ?> value, String message) {
        if (value != null && !value.isEmpty()) {
            raise(message);
        }
    }

    /**
     * 断言 Map 必须包含指定 key。
     *
     * @param value 待检查 Map
     * @param key key
     * @param message 异常消息
     * @param <M> Map 类型
     * @return 原始 Map
     */
    public static <M extends Map<?, ?>> M containsKey(M value, Object key, String message) {
        if (value == null || !value.containsKey(key)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言 Map 不能包含指定 key。
     *
     * @param value 待检查 Map
     * @param key key
     * @param message 异常消息
     * @param <M> Map 类型
     * @return 原始 Map
     */
    public static <M extends Map<?, ?>> M notContainsKey(M value, Object key, String message) {
        if (value != null && value.containsKey(key)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言 Map 必须包含指定 value。
     *
     * @param value 待检查 Map
     * @param val value
     * @param message 异常消息
     * @param <M> Map 类型
     * @return 原始 Map
     */
    public static <M extends Map<?, ?>> M containsValue(M value, Object val, String message) {
        if (value == null || !value.containsValue(val)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言 Map 不能包含指定 value。
     *
     * @param value 待检查 Map
     * @param val value
     * @param message 异常消息
     * @param <M> Map 类型
     * @return 原始 Map
     */
    public static <M extends Map<?, ?>> M notContainsValue(M value, Object val, String message) {
        if (value != null && value.containsValue(val)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言 Map 必须包含全部指定 key。
     *
     * @param value 待检查 Map
     * @param message 异常消息
     * @param keys key 列表
     * @param <M> Map 类型
     * @return 原始 Map
     */
    public static <M extends Map<?, ?>> M requiredKeys(M value, String message, Object... keys) {
        notNull(value, message);
        notNull(keys, "keys 不能为 null");
        for (Object key : keys) {
            if (!value.containsKey(key)) {
                raise(message);
            }
        }
        return value;
    }

    /**
     * 断言 Map 不能包含 null key。
     *
     * @param value 待检查 Map
     * @param message 异常消息
     * @param <M> Map 类型
     * @return 原始 Map
     */
    public static <M extends Map<?, ?>> M noNullKeys(M value, String message) {
        notNull(value, message);
        if (value.containsKey(null)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言 Map 不能包含 null value。
     *
     * @param value 待检查 Map
     * @param message 异常消息
     * @param <M> Map 类型
     * @return 原始 Map
     */
    public static <M extends Map<?, ?>> M noNullValues(M value, String message) {
        notNull(value, message);
        if (value.containsValue(null)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数值必须大于 0。
     *
     * @param value 待检查数值
     * @param message 异常消息
     * @param <N> 数值类型
     * @return 原始数值
     */
    public static <N extends Number> N positive(N value, String message) {
        if (compareNumber(value, 0) <= 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数值必须小于 0。
     *
     * @param value 待检查数值
     * @param message 异常消息
     * @param <N> 数值类型
     * @return 原始数值
     */
    public static <N extends Number> N negative(N value, String message) {
        if (compareNumber(value, 0) >= 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数值必须小于等于 0。
     *
     * @param value 待检查数值
     * @param message 异常消息
     * @param <N> 数值类型
     * @return 原始数值
     */
    public static <N extends Number> N nonPositive(N value, String message) {
        if (compareNumber(value, 0) > 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数值必须大于等于 0。
     *
     * @param value 待检查数值
     * @param message 异常消息
     * @param <N> 数值类型
     * @return 原始数值
     */
    public static <N extends Number> N nonNegative(N value, String message) {
        if (compareNumber(value, 0) < 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数值必须等于 0。
     *
     * @param value 待检查数值
     * @param message 异常消息
     * @param <N> 数值类型
     * @return 原始数值
     */
    public static <N extends Number> N zero(N value, String message) {
        if (compareNumber(value, 0) != 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数值不能等于 0。
     *
     * @param value 待检查数值
     * @param message 异常消息
     * @param <N> 数值类型
     * @return 原始数值
     */
    public static <N extends Number> N notZero(N value, String message) {
        if (compareNumber(value, 0) == 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数值必须大于指定值。
     *
     * @param value 待检查数值
     * @param min 最小值
     * @param message 异常消息
     * @param <N> 数值类型
     * @return 原始数值
     */
    public static <N extends Number> N greaterThan(N value, Number min, String message) {
        if (compareNumber(value, min) <= 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数值必须大于等于指定值。
     *
     * @param value 待检查数值
     * @param min 最小值
     * @param message 异常消息
     * @param <N> 数值类型
     * @return 原始数值
     */
    public static <N extends Number> N greaterThanOrEqual(N value, Number min, String message) {
        if (compareNumber(value, min) < 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数值必须小于指定值。
     *
     * @param value 待检查数值
     * @param max 最大值
     * @param message 异常消息
     * @param <N> 数值类型
     * @return 原始数值
     */
    public static <N extends Number> N lessThan(N value, Number max, String message) {
        if (compareNumber(value, max) >= 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数值必须小于等于指定值。
     *
     * @param value 待检查数值
     * @param max 最大值
     * @param message 异常消息
     * @param <N> 数值类型
     * @return 原始数值
     */
    public static <N extends Number> N lessThanOrEqual(N value, Number max, String message) {
        if (compareNumber(value, max) > 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数值必须在指定范围内，包含边界。
     *
     * @param value 待检查数值
     * @param min 最小值
     * @param max 最大值
     * @param message 异常消息
     * @param <N> 数值类型
     * @return 原始数值
     */
    public static <N extends Number> N between(N value, Number min, Number max, String message) {
        BigDecimal minValue = toBigDecimal(min, "min");
        BigDecimal maxValue = toBigDecimal(max, "max");
        if (minValue.compareTo(maxValue) > 0) {
            raise("min 不能大于 max");
        }
        BigDecimal current = toBigDecimal(value, "value");
        if (current.compareTo(minValue) < 0 || current.compareTo(maxValue) > 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言数值不能在指定范围内，包含边界。
     *
     * @param value 待检查数值
     * @param min 最小值
     * @param max 最大值
     * @param message 异常消息
     * @param <N> 数值类型
     * @return 原始数值
     */
    public static <N extends Number> N notBetween(N value, Number min, Number max, String message) {
        BigDecimal minValue = toBigDecimal(min, "min");
        BigDecimal maxValue = toBigDecimal(max, "max");
        if (minValue.compareTo(maxValue) > 0) {
            raise("min 不能大于 max");
        }
        BigDecimal current = toBigDecimal(value, "value");
        if (current.compareTo(minValue) >= 0 && current.compareTo(maxValue) <= 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言实际值必须等于期望值。
     *
     * @param actual 实际值
     * @param expected 期望值
     * @param message 异常消息
     * @param <T> 实际值类型
     * @return 原始实际值
     */
    public static <T> T equalsTo(T actual, Object expected, String message) {
        if (!Objects.equals(actual, expected)) {
            raise(message);
        }
        return actual;
    }

    /**
     * 断言实际值不能等于期望值。
     *
     * @param actual 实际值
     * @param expected 期望值
     * @param message 异常消息
     * @param <T> 实际值类型
     * @return 原始实际值
     */
    public static <T> T notEquals(T actual, Object expected, String message) {
        if (Objects.equals(actual, expected)) {
            raise(message);
        }
        return actual;
    }

    /**
     * 断言两个对象必须为同一引用。
     *
     * @param actual 实际对象
     * @param expected 期望对象
     * @param message 异常消息
     * @param <T> 实际对象类型
     * @return 原始实际对象
     */
    public static <T> T same(T actual, Object expected, String message) {
        if (actual != expected) {
            raise(message);
        }
        return actual;
    }

    /**
     * 断言两个对象不能为同一引用。
     *
     * @param actual 实际对象
     * @param expected 期望对象
     * @param message 异常消息
     * @param <T> 实际对象类型
     * @return 原始实际对象
     */
    public static <T> T notSame(T actual, Object expected, String message) {
        if (actual == expected) {
            raise(message);
        }
        return actual;
    }

    /**
     * 断言值必须存在于候选集合中。
     *
     * @param value 待检查值
     * @param candidates 候选集合
     * @param message 异常消息
     * @param <T> 值类型
     * @return 原始值
     */
    public static <T> T in(T value, Collection<?> candidates, String message) {
        if (candidates == null || !candidates.contains(value)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言值不能存在于候选集合中。
     *
     * @param value 待检查值
     * @param candidates 候选集合
     * @param message 异常消息
     * @param <T> 值类型
     * @return 原始值
     */
    public static <T> T notIn(T value, Collection<?> candidates, String message) {
        if (candidates != null && candidates.contains(value)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言值必须为候选值之一。
     *
     * @param value 待检查值
     * @param message 异常消息
     * @param candidates 候选值
     * @param <T> 值类型
     * @return 原始值
     */
    public static <T> T oneOf(T value, String message, Object... candidates) {
        notNull(candidates, "candidates 不能为 null");
        for (Object candidate : candidates) {
            if (Objects.equals(value, candidate)) {
                return value;
            }
        }
        raise(message);
        return value;
    }

    /**
     * 断言值不能为任一候选值。
     *
     * @param value 待检查值
     * @param message 异常消息
     * @param candidates 候选值
     * @param <T> 值类型
     * @return 原始值
     */
    public static <T> T notOneOf(T value, String message, Object... candidates) {
        notNull(candidates, "candidates 不能为 null");
        for (Object candidate : candidates) {
            if (Objects.equals(value, candidate)) {
                raise(message);
            }
        }
        return value;
    }

    /**
     * 断言对象必须是指定类型的实例。
     *
     * @param value 待检查对象
     * @param type 目标类型
     * @param message 异常消息
     * @param <T> 对象类型
     * @return 原始对象
     */
    public static <T> T instanceOf(T value, Class<?> type, String message) {
        notNull(type, "type 不能为 null");
        if (!type.isInstance(value)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言对象不能是指定类型的实例。
     *
     * @param value 待检查对象
     * @param type 目标类型
     * @param message 异常消息
     * @param <T> 对象类型
     * @return 原始对象
     */
    public static <T> T notInstanceOf(T value, Class<?> type, String message) {
        notNull(type, "type 不能为 null");
        if (type.isInstance(value)) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言子类型必须可以赋值给父类型。
     *
     * @param parent 父类型
     * @param child 子类型
     * @param message 异常消息
     */
    public static void assignableFrom(Class<?> parent, Class<?> child, String message) {
        notNull(parent, "parent 不能为 null");
        notNull(child, "child 不能为 null");
        if (!parent.isAssignableFrom(child)) {
            raise(message);
        }
    }

    /**
     * 断言子类型不能赋值给父类型。
     *
     * @param parent 父类型
     * @param child 子类型
     * @param message 异常消息
     */
    public static void notAssignableFrom(Class<?> parent, Class<?> child, String message) {
        notNull(parent, "parent 不能为 null");
        notNull(child, "child 不能为 null");
        if (parent.isAssignableFrom(child)) {
            raise(message);
        }
    }

    /**
     * 断言两个对象类型必须一致。
     *
     * @param actual 实际对象
     * @param expected 期望对象
     * @param message 异常消息
     */
    public static void sameType(Object actual, Object expected, String message) {
        notNull(actual, message);
        notNull(expected, message);
        if (!actual.getClass().equals(expected.getClass())) {
            raise(message);
        }
    }

    /**
     * 断言两个对象类型不能一致。
     *
     * @param actual 实际对象
     * @param expected 期望对象
     * @param message 异常消息
     */
    public static void notSameType(Object actual, Object expected, String message) {
        notNull(actual, message);
        notNull(expected, message);
        if (actual.getClass().equals(expected.getClass())) {
            raise(message);
        }
    }

    /**
     * 断言时间必须早于目标时间。
     *
     * @param value 待检查时间
     * @param target 目标时间
     * @param message 异常消息
     * @param <T> 时间类型
     * @return 原始时间
     */
    public static <T extends Temporal & Comparable<? super T>> T before(T value, T target, String message) {
        notNull(value, message);
        notNull(target, "target 不能为 null");
        if (value.compareTo(target) >= 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言时间必须晚于目标时间。
     *
     * @param value 待检查时间
     * @param target 目标时间
     * @param message 异常消息
     * @param <T> 时间类型
     * @return 原始时间
     */
    public static <T extends Temporal & Comparable<? super T>> T after(T value, T target, String message) {
        notNull(value, message);
        notNull(target, "target 不能为 null");
        if (value.compareTo(target) <= 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言时间必须在指定范围内，包含边界。
     *
     * @param value 待检查时间
     * @param start 开始时间
     * @param end 结束时间
     * @param message 异常消息
     * @param <T> 时间类型
     * @return 原始时间
     */
    public static <T extends Temporal & Comparable<? super T>> T between(T value, T start, T end, String message) {
        notNull(value, message);
        checkComparableRange(start, end);
        if (value.compareTo(start) < 0 || value.compareTo(end) > 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言时间不能在指定范围内，包含边界。
     *
     * @param value 待检查时间
     * @param start 开始时间
     * @param end 结束时间
     * @param message 异常消息
     * @param <T> 时间类型
     * @return 原始时间
     */
    public static <T extends Temporal & Comparable<? super T>> T notBetween(T value, T start, T end, String message) {
        notNull(value, message);
        checkComparableRange(start, end);
        if (value.compareTo(start) >= 0 && value.compareTo(end) <= 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言时间必须是过去时间。
     *
     * @param value 待检查时间
     * @param message 异常消息
     * @param <T> 时间类型
     * @return 原始时间
     */
    public static <T extends Temporal & Comparable<? super T>> T past(T value, String message) {
        return past(value, Clock.systemDefaultZone(), message);
    }

    /**
     * 断言时间必须是未来时间。
     *
     * @param value 待检查时间
     * @param message 异常消息
     * @param <T> 时间类型
     * @return 原始时间
     */
    public static <T extends Temporal & Comparable<? super T>> T future(T value, String message) {
        return future(value, Clock.systemDefaultZone(), message);
    }

    /**
     * 断言时间必须是过去或当前时间。
     *
     * @param value 待检查时间
     * @param message 异常消息
     * @param <T> 时间类型
     * @return 原始时间
     */
    public static <T extends Temporal & Comparable<? super T>> T pastOrPresent(T value, String message) {
        return pastOrPresent(value, Clock.systemDefaultZone(), message);
    }

    /**
     * 断言时间必须是未来或当前时间。
     *
     * @param value 待检查时间
     * @param message 异常消息
     * @param <T> 时间类型
     * @return 原始时间
     */
    public static <T extends Temporal & Comparable<? super T>> T futureOrPresent(T value, String message) {
        return futureOrPresent(value, Clock.systemDefaultZone(), message);
    }

    /**
     * 断言时间必须是过去时间，并使用指定时钟计算当前时间。
     *
     * @param value 待检查时间
     * @param clock 时钟
     * @param message 异常消息
     * @param <T> 时间类型
     * @return 原始时间
     */
    public static <T extends Temporal & Comparable<? super T>> T past(T value, Clock clock, String message) {
        T now = currentLike(value, clock);
        return before(value, now, message);
    }

    /**
     * 断言时间必须是未来时间，并使用指定时钟计算当前时间。
     *
     * @param value 待检查时间
     * @param clock 时钟
     * @param message 异常消息
     * @param <T> 时间类型
     * @return 原始时间
     */
    public static <T extends Temporal & Comparable<? super T>> T future(T value, Clock clock, String message) {
        T now = currentLike(value, clock);
        return after(value, now, message);
    }

    /**
     * 断言时间必须是过去或当前时间，并使用指定时钟计算当前时间。
     *
     * @param value 待检查时间
     * @param clock 时钟
     * @param message 异常消息
     * @param <T> 时间类型
     * @return 原始时间
     */
    public static <T extends Temporal & Comparable<? super T>> T pastOrPresent(T value, Clock clock, String message) {
        T now = currentLike(value, clock);
        if (value.compareTo(now) > 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言时间必须是未来或当前时间，并使用指定时钟计算当前时间。
     *
     * @param value 待检查时间
     * @param clock 时钟
     * @param message 异常消息
     * @param <T> 时间类型
     * @return 原始时间
     */
    public static <T extends Temporal & Comparable<? super T>> T futureOrPresent(T value, Clock clock, String message) {
        T now = currentLike(value, clock);
        if (value.compareTo(now) < 0) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言开始时间必须早于结束时间。
     *
     * @param start 开始时间
     * @param end 结束时间
     * @param message 异常消息
     * @param <T> 时间类型
     */
    public static <T extends Temporal & Comparable<? super T>> void startBeforeEnd(T start, T end, String message) {
        before(start, end, message);
    }

    /**
     * 断言开始时间必须早于或等于结束时间。
     *
     * @param start 开始时间
     * @param end 结束时间
     * @param message 异常消息
     * @param <T> 时间类型
     */
    public static <T extends Temporal & Comparable<? super T>> void startBeforeOrEqualEnd(T start, T end, String message) {
        try {
            checkComparableRange(start, end);
        } catch (RuntimeException ex) {
            raise(message);
        }
    }

    /**
     * 断言字符串必须是指定枚举类型的名称。
     *
     * @param name 枚举名称
     * @param enumType 枚举类型
     * @param message 异常消息
     * @param <E> 枚举类型
     * @return 原始枚举名称
     */
    public static <E extends Enum<E>> String enumNameOf(String name, Class<E> enumType, String message) {
        notBlank(name, message);
        notNull(enumType, "enumType 不能为 null");
        E[] constants = enumType.getEnumConstants();
        if (constants == null) {
            raise("enumType 必须是枚举类型");
        }
        for (E item : constants) {
            if (item.name().equals(name)) {
                return name;
            }
        }
        raise(message);
        return name;
    }

    /**
     * 断言值必须属于指定枚举值集合。
     *
     * @param value 待检查值
     * @param enumValues 枚举值集合
     * @param message 异常消息
     * @param <T> 值类型
     * @return 原始值
     */
    public static <T> T enumValueOf(T value, Collection<?> enumValues, String message) {
        return in(value, enumValues, message);
    }

    /**
     * 断言枚举必须存在于候选集合中。
     *
     * @param value 待检查枚举
     * @param candidates 候选枚举集合
     * @param message 异常消息
     * @param <E> 枚举类型
     * @return 原始枚举
     */
    public static <E extends Enum<E>> E enumIn(E value, Collection<E> candidates, String message) {
        return in(value, candidates, message);
    }

    /**
     * 断言枚举不能存在于候选集合中。
     *
     * @param value 待检查枚举
     * @param candidates 候选枚举集合
     * @param message 异常消息
     * @param <E> 枚举类型
     * @return 原始枚举
     */
    public static <E extends Enum<E>> E enumNotIn(E value, Collection<E> candidates, String message) {
        return notIn(value, candidates, message);
    }

    /**
     * 断言正则表达式必须合法。
     *
     * @param regex 正则表达式
     * @param message 异常消息
     * @return 原始正则表达式
     */
    public static String validPattern(String regex, String message) {
        try {
            compilePattern(regex);
            return regex;
        } catch (RuntimeException ex) {
            raise(message, ex);
            return regex;
        }
    }

    /**
     * 断言字符串必须匹配指定 Pattern。
     *
     * @param value 待检查字符串
     * @param pattern Pattern 对象
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String format(String value, Pattern pattern, String message) {
        notNull(pattern, "pattern 不能为 null");
        if (value == null || !pattern.matcher(value).matches()) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言字符串不能匹配指定 Pattern。
     *
     * @param value 待检查字符串
     * @param pattern Pattern 对象
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String notFormat(String value, Pattern pattern, String message) {
        notNull(pattern, "pattern 不能为 null");
        if (value != null && pattern.matcher(value).matches()) {
            raise(message);
        }
        return value;
    }

    /**
     * 断言字符串必须是常见邮箱格式。
     *
     * @param value 待检查字符串
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String email(String value, String message) {
        return matches(value, "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message);
    }

    /**
     * 断言字符串必须是常见 URL 格式。
     *
     * @param value 待检查字符串
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String url(String value, String message) {
        return matches(value, "^(https?://).+", message);
    }

    /**
     * 断言字符串必须是标准 UUID 格式。
     *
     * @param value 待检查字符串
     * @param message 异常消息
     * @return 原始字符串
     */
    public static String uuid(String value, String message) {
        return matches(value, "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message);
    }

    /**
     * 断言路径必须存在。
     *
     * @param path 路径
     * @param message 异常消息
     * @return 原始路径
     */
    public static Path exists(Path path, String message) {
        if (path == null || !Files.exists(path)) {
            raise(message);
        }
        return path;
    }

    /**
     * 断言路径必须不存在。
     *
     * @param path 路径
     * @param message 异常消息
     * @return 原始路径
     */
    public static Path notExists(Path path, String message) {
        if (path != null && Files.exists(path)) {
            raise(message);
        }
        return path;
    }

    /**
     * 断言路径必须是普通文件。
     *
     * @param path 路径
     * @param message 异常消息
     * @return 原始路径
     */
    public static Path isFile(Path path, String message) {
        if (path == null || !Files.isRegularFile(path)) {
            raise(message);
        }
        return path;
    }

    /**
     * 断言路径必须是目录。
     *
     * @param path 路径
     * @param message 异常消息
     * @return 原始路径
     */
    public static Path isDirectory(Path path, String message) {
        if (path == null || !Files.isDirectory(path)) {
            raise(message);
        }
        return path;
    }

    /**
     * 断言路径必须可读。
     *
     * @param path 路径
     * @param message 异常消息
     * @return 原始路径
     */
    public static Path readable(Path path, String message) {
        if (path == null || !Files.isReadable(path)) {
            raise(message);
        }
        return path;
    }

    /**
     * 断言路径必须可写。
     *
     * @param path 路径
     * @param message 异常消息
     * @return 原始路径
     */
    public static Path writable(Path path, String message) {
        if (path == null || !Files.isWritable(path)) {
            raise(message);
        }
        return path;
    }

    /**
     * 断言文件大小必须小于指定字节数。
     *
     * @param path 文件路径
     * @param maxBytes 最大字节数
     * @param message 异常消息
     * @return 原始路径
     */
    public static Path fileSizeLessThan(Path path, long maxBytes, String message) {
        if (maxBytes < 0) {
            raise("maxBytes 不能小于 0");
        }
        isFile(path, message);
        if (fileSize(path) >= maxBytes) {
            raise(message);
        }
        return path;
    }

    /**
     * 断言文件大小必须在指定字节范围内，包含边界。
     *
     * @param path 文件路径
     * @param minBytes 最小字节数
     * @param maxBytes 最大字节数
     * @param message 异常消息
     * @return 原始路径
     */
    public static Path fileSizeBetween(Path path, long minBytes, long maxBytes, String message) {
        if (minBytes < 0 || maxBytes < 0) {
            raise("minBytes 和 maxBytes 不能小于 0");
        }
        if (minBytes > maxBytes) {
            raise("minBytes 不能大于 maxBytes");
        }
        isFile(path, message);
        long size = fileSize(path);
        if (size < minBytes || size > maxBytes) {
            raise(message);
        }
        return path;
    }

    private static void checkNonNegative(int value, String name) {
        if (value < 0) {
            raise(name + " 不能小于 0");
        }
    }

    private static void checkRange(int min, int max, String minName, String maxName) {
        checkNonNegative(min, minName);
        checkNonNegative(max, maxName);
        if (min > max) {
            raise(minName + " 不能大于 " + maxName);
        }
    }

    private static Pattern compilePattern(String regex) {
        if (regex == null) {
            raise("regex 不能为 null");
        }
        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException ex) {
            raise("regex 格式不合法", ex);
            return Pattern.compile(".*");
        }
    }

    private static int compareNumber(Number left, Number right) {
        return toBigDecimal(left, "value").compareTo(toBigDecimal(right, "target"));
    }

    private static BigDecimal toBigDecimal(Number number, String name) {
        if (number == null) {
            raise(name + " 不能为 null");
        }
        if (number instanceof Double doubleValue && (doubleValue.isNaN() || doubleValue.isInfinite())) {
            raise(name + " 不能为 NaN 或 Infinity");
        }
        if (number instanceof Float floatValue && (floatValue.isNaN() || floatValue.isInfinite())) {
            raise(name + " 不能为 NaN 或 Infinity");
        }
        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException ex) {
            raise(name + " 不是有效数值", ex);
            return BigDecimal.ZERO;
        }
    }

    private static <T extends Comparable<? super T>> void checkComparableRange(T start, T end) {
        notNull(start, "start 不能为 null");
        notNull(end, "end 不能为 null");
        if (start.compareTo(end) > 0) {
            raise("start 不能晚于 end");
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Temporal & Comparable<? super T>> T currentLike(T value, Clock clock) {
        notNull(value, "value 不能为 null");
        notNull(clock, "clock 不能为 null");
        if (value instanceof LocalDate) {
            return (T) LocalDate.now(clock);
        }
        if (value instanceof LocalDateTime) {
            return (T) LocalDateTime.now(clock);
        }
        if (value instanceof Instant) {
            return (T) Instant.now(clock);
        }
        if (value instanceof ZonedDateTime) {
            return (T) ZonedDateTime.now(clock);
        }
        if (value instanceof OffsetDateTime) {
            return (T) OffsetDateTime.now(clock);
        }
        raise("暂不支持的时间类型：" + value.getClass().getName());
        return value;
    }

    private static long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            raise("读取文件大小失败", ex);
            return -1;
        }
    }
}
