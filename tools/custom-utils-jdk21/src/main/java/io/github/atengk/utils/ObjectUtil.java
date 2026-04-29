package io.github.atengk.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 对象基础处理工具类，提供空值、默认值、类型、安全执行、比较、克隆等通用能力。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class ObjectUtil {

    private ObjectUtil() {
        throw new UnsupportedOperationException("ObjectUtil 不能被实例化");
    }

    /**
     * 判断对象是否为 null。
     *
     * @param obj 待判断对象
     * @return 为 null 返回 true，否则返回 false
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }

    /**
     * 判断对象是否不为 null。
     *
     * @param obj 待判断对象
     * @return 不为 null 返回 true，否则返回 false
     */
    public static boolean isNotNull(Object obj) {
        return obj != null;
    }

    /**
     * 判断多个对象中是否存在 null。
     *
     * @param objs 待判断对象数组
     * @return 存在 null 返回 true，否则返回 false
     */
    public static boolean isAnyNull(Object... objs) {
        if (objs == null || objs.length == 0) {
            return false;
        }
        for (Object obj : objs) {
            if (obj == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断多个对象是否全部为 null。
     *
     * @param objs 待判断对象数组
     * @return 全部为 null 返回 true，空数组或数组本身为 null 返回 false
     */
    public static boolean isAllNull(Object... objs) {
        if (objs == null || objs.length == 0) {
            return false;
        }
        for (Object obj : objs) {
            if (obj != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断多个对象是否全部不为 null。
     *
     * @param objs 待判断对象数组
     * @return 全部不为 null 返回 true，否则返回 false
     */
    public static boolean isNoneNull(Object... objs) {
        return !isAnyNull(objs);
    }

    /**
     * 统计 null 对象数量。
     *
     * @param objs 待统计对象数组
     * @return null 对象数量
     */
    public static int nullCount(Object... objs) {
        if (objs == null || objs.length == 0) {
            return 0;
        }
        int count = 0;
        for (Object obj : objs) {
            if (obj == null) {
                count++;
            }
        }
        return count;
    }

    /**
     * 统计非 null 对象数量。
     *
     * @param objs 待统计对象数组
     * @return 非 null 对象数量
     */
    public static int nonNullCount(Object... objs) {
        if (objs == null || objs.length == 0) {
            return 0;
        }
        return objs.length - nullCount(objs);
    }

    /**
     * 判断对象是否为空内容。
     * <p>支持 null、CharSequence、Collection、Map、数组、Optional。</p>
     *
     * @param obj 待判断对象
     * @return 为空内容返回 true，否则返回 false
     */
    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        }
        if (obj instanceof CharSequence value) {
            return value.isEmpty();
        }
        if (obj instanceof Collection<?> value) {
            return value.isEmpty();
        }
        if (obj instanceof Map<?, ?> value) {
            return value.isEmpty();
        }
        if (obj instanceof Optional<?> value) {
            return value.isEmpty();
        }
        if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        }
        return false;
    }

    /**
     * 判断对象是否非空内容。
     *
     * @param obj 待判断对象
     * @return 非空内容返回 true，否则返回 false
     */
    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    /**
     * 判断多个对象中是否存在空内容。
     *
     * @param objs 待判断对象数组
     * @return 存在空内容返回 true，否则返回 false
     */
    public static boolean isAnyEmpty(Object... objs) {
        if (objs == null || objs.length == 0) {
            return false;
        }
        for (Object obj : objs) {
            if (isEmpty(obj)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断多个对象是否全部为空内容。
     *
     * @param objs 待判断对象数组
     * @return 全部为空内容返回 true，空数组或数组本身为 null 返回 false
     */
    public static boolean isAllEmpty(Object... objs) {
        if (objs == null || objs.length == 0) {
            return false;
        }
        for (Object obj : objs) {
            if (isNotEmpty(obj)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断多个对象是否全部非空内容。
     *
     * @param objs 待判断对象数组
     * @return 全部非空内容返回 true，否则返回 false
     */
    public static boolean isNoneEmpty(Object... objs) {
        return !isAnyEmpty(objs);
    }

    /**
     * 统计空内容对象数量。
     *
     * @param objs 待统计对象数组
     * @return 空内容对象数量
     */
    public static int emptyCount(Object... objs) {
        if (objs == null || objs.length == 0) {
            return 0;
        }
        int count = 0;
        for (Object obj : objs) {
            if (isEmpty(obj)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 统计非空内容对象数量。
     *
     * @param objs 待统计对象数组
     * @return 非空内容对象数量
     */
    public static int nonEmptyCount(Object... objs) {
        if (objs == null || objs.length == 0) {
            return 0;
        }
        return objs.length - emptyCount(objs);
    }

    /**
     * 判断对象是否为空白内容。
     * <p>null 为空白；CharSequence 全部为空白字符时为空白；非 CharSequence 对象不视为空白。</p>
     *
     * @param obj 待判断对象
     * @return 为空白内容返回 true，否则返回 false
     */
    public static boolean isBlank(Object obj) {
        if (obj == null) {
            return true;
        }
        if (!(obj instanceof CharSequence value)) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断对象是否不是空白内容。
     *
     * @param obj 待判断对象
     * @return 不是空白内容返回 true，否则返回 false
     */
    public static boolean isNotBlank(Object obj) {
        return !isBlank(obj);
    }

    /**
     * 判断多个对象中是否存在空白内容。
     *
     * @param objs 待判断对象数组
     * @return 存在空白内容返回 true，否则返回 false
     */
    public static boolean isAnyBlank(Object... objs) {
        if (objs == null || objs.length == 0) {
            return false;
        }
        for (Object obj : objs) {
            if (isBlank(obj)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断多个对象是否全部为空白内容。
     *
     * @param objs 待判断对象数组
     * @return 全部为空白内容返回 true，空数组或数组本身为 null 返回 false
     */
    public static boolean isAllBlank(Object... objs) {
        if (objs == null || objs.length == 0) {
            return false;
        }
        for (Object obj : objs) {
            if (isNotBlank(obj)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断多个对象是否全部不是空白内容。
     *
     * @param objs 待判断对象数组
     * @return 全部不是空白内容返回 true，否则返回 false
     */
    public static boolean isNoneBlank(Object... objs) {
        return !isAnyBlank(objs);
    }

    /**
     * 对象为 null 时返回默认值。
     *
     * @param obj          原对象
     * @param defaultValue 默认值
     * @param <T>          对象类型
     * @return 原对象或默认值
     */
    public static <T> T defaultIfNull(T obj, T defaultValue) {
        return obj == null ? defaultValue : obj;
    }

    /**
     * 对象为 null 时通过 Supplier 获取默认值。
     *
     * @param obj      原对象
     * @param supplier 默认值提供器
     * @param <T>      对象类型
     * @return 原对象或 Supplier 返回值
     */
    public static <T> T defaultIfNullGet(T obj, Supplier<? extends T> supplier) {
        if (obj != null) {
            return obj;
        }
        Objects.requireNonNull(supplier, "supplier 不能为 null");
        return supplier.get();
    }

    /**
     * 对象为空内容时返回默认值。
     *
     * @param obj          原对象
     * @param defaultValue 默认值
     * @param <T>          对象类型
     * @return 原对象或默认值
     */
    public static <T> T defaultIfEmpty(T obj, T defaultValue) {
        return isEmpty(obj) ? defaultValue : obj;
    }

    /**
     * 对象为空内容时通过 Supplier 获取默认值。
     *
     * @param obj      原对象
     * @param supplier 默认值提供器
     * @param <T>      对象类型
     * @return 原对象或 Supplier 返回值
     */
    public static <T> T defaultIfEmptyGet(T obj, Supplier<? extends T> supplier) {
        if (isNotEmpty(obj)) {
            return obj;
        }
        Objects.requireNonNull(supplier, "supplier 不能为 null");
        return supplier.get();
    }

    /**
     * 对象为空白内容时返回默认值。
     *
     * @param obj          原对象
     * @param defaultValue 默认值
     * @param <T>          对象类型
     * @return 原对象或默认值
     */
    public static <T> T defaultIfBlank(T obj, T defaultValue) {
        return isBlank(obj) ? defaultValue : obj;
    }

    /**
     * 对象为空白内容时通过 Supplier 获取默认值。
     *
     * @param obj      原对象
     * @param supplier 默认值提供器
     * @param <T>      对象类型
     * @return 原对象或 Supplier 返回值
     */
    public static <T> T defaultIfBlankGet(T obj, Supplier<? extends T> supplier) {
        if (isNotBlank(obj)) {
            return obj;
        }
        Objects.requireNonNull(supplier, "supplier 不能为 null");
        return supplier.get();
    }

    /**
     * 返回第一个非 null 值。
     *
     * @param values 候选值数组
     * @param <T>    对象类型
     * @return 第一个非 null 值，没有则返回 null
     */
    @SafeVarargs
    public static <T> T firstNonNull(T... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * 返回第一个非空内容值。
     *
     * @param values 候选值数组
     * @param <T>    对象类型
     * @return 第一个非空内容值，没有则返回 null
     */
    @SafeVarargs
    public static <T> T firstNonEmpty(T... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (T value : values) {
            if (isNotEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 返回第一个非空白内容值。
     *
     * @param values 候选值数组
     * @param <T>    对象类型
     * @return 第一个非空白内容值，没有则返回 null
     */
    @SafeVarargs
    public static <T> T firstNonBlank(T... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (T value : values) {
            if (isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 要求对象不能为 null。
     *
     * @param obj 待校验对象
     * @param <T> 对象类型
     * @return 原对象
     */
    public static <T> T requireNonNull(T obj) {
        return Objects.requireNonNull(obj, "对象不能为 null");
    }

    /**
     * 要求对象不能为 null。
     *
     * @param obj     待校验对象
     * @param message 异常消息
     * @param <T>     对象类型
     * @return 原对象
     */
    public static <T> T requireNonNull(T obj, String message) {
        return Objects.requireNonNull(obj, message);
    }

    /**
     * 要求对象不能为空内容。
     *
     * @param obj     待校验对象
     * @param message 异常消息
     * @param <T>     对象类型
     * @return 原对象
     */
    public static <T> T requireNonEmpty(T obj, String message) {
        if (isEmpty(obj)) {
            throw new IllegalArgumentException(message);
        }
        return obj;
    }

    /**
     * 要求对象不能为空白内容。
     *
     * @param obj     待校验对象
     * @param message 异常消息
     * @param <T>     对象类型
     * @return 原对象
     */
    public static <T> T requireNonBlank(T obj, String message) {
        if (isBlank(obj)) {
            throw new IllegalArgumentException(message);
        }
        return obj;
    }

    /**
     * 要求所有对象都不能为 null。
     *
     * @param message 异常消息
     * @param objs    待校验对象数组
     */
    public static void requireAllNonNull(String message, Object... objs) {
        if (objs == null || objs.length == 0 || isAnyNull(objs)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 要求所有对象都不能为空内容。
     *
     * @param message 异常消息
     * @param objs    待校验对象数组
     */
    public static void requireAllNonEmpty(String message, Object... objs) {
        if (objs == null || objs.length == 0 || isAnyEmpty(objs)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 要求至少一个对象不为 null。
     *
     * @param message 异常消息
     * @param objs    待校验对象数组
     */
    public static void requireAnyNonNull(String message, Object... objs) {
        if (objs == null || objs.length == 0 || isAllNull(objs)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 要求至少一个对象不能为空内容。
     *
     * @param message 异常消息
     * @param objs    待校验对象数组
     */
    public static void requireAnyNonEmpty(String message, Object... objs) {
        if (objs == null || objs.length == 0 || isAllEmpty(objs)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 安全判断两个对象是否相等。
     *
     * @param obj1 对象一
     * @param obj2 对象二
     * @return 相等返回 true，否则返回 false
     */
    public static boolean equals(Object obj1, Object obj2) {
        return Objects.equals(obj1, obj2);
    }

    /**
     * 安全判断两个对象是否不相等。
     *
     * @param obj1 对象一
     * @param obj2 对象二
     * @return 不相等返回 true，否则返回 false
     */
    public static boolean notEquals(Object obj1, Object obj2) {
        return !equals(obj1, obj2);
    }

    /**
     * 深度判断两个对象是否相等，支持数组内容比较。
     *
     * @param obj1 对象一
     * @param obj2 对象二
     * @return 深度相等返回 true，否则返回 false
     */
    public static boolean deepEquals(Object obj1, Object obj2) {
        return Objects.deepEquals(obj1, obj2);
    }

    /**
     * 判断对象是否等于任意目标值。
     *
     * @param obj     待判断对象
     * @param targets 目标值数组
     * @return 等于任意目标值返回 true，否则返回 false
     */
    public static boolean equalsAny(Object obj, Object... targets) {
        if (targets == null || targets.length == 0) {
            return false;
        }
        for (Object target : targets) {
            if (equals(obj, target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断对象是否等于所有目标值。
     *
     * @param obj     待判断对象
     * @param targets 目标值数组
     * @return 等于所有目标值返回 true，空数组或数组本身为 null 返回 false
     */
    public static boolean equalsAll(Object obj, Object... targets) {
        if (targets == null || targets.length == 0) {
            return false;
        }
        for (Object target : targets) {
            if (notEquals(obj, target)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断对象是否不等于任何目标值。
     *
     * @param obj     待判断对象
     * @param targets 目标值数组
     * @return 不等于任何目标值返回 true，否则返回 false
     */
    public static boolean notEqualsAny(Object obj, Object... targets) {
        return !equalsAny(obj, targets);
    }

    /**
     * 判断两个对象是否为同一引用。
     *
     * @param obj1 对象一
     * @param obj2 对象二
     * @return 同一引用返回 true，否则返回 false
     */
    public static boolean same(Object obj1, Object obj2) {
        return obj1 == obj2;
    }

    /**
     * 判断两个对象是否不是同一引用。
     *
     * @param obj1 对象一
     * @param obj2 对象二
     * @return 不是同一引用返回 true，否则返回 false
     */
    public static boolean notSame(Object obj1, Object obj2) {
        return obj1 != obj2;
    }

    /**
     * 获取对象 hashCode，支持 null。
     *
     * @param obj 待处理对象
     * @return hashCode 值，null 返回 0
     */
    public static int hashCode(Object obj) {
        return Objects.hashCode(obj);
    }

    /**
     * 根据多个对象生成组合 hash。
     *
     * @param values 待处理对象数组
     * @return 组合 hash 值
     */
    public static int hash(Object... values) {
        return Objects.hash(values);
    }

    /**
     * 获取深度 hashCode，支持对象数组和基本类型数组。
     *
     * @param obj 待处理对象
     * @return 深度 hash 值
     */
    public static int deepHashCode(Object obj) {
        if (obj == null) {
            return 0;
        }
        Class<?> type = obj.getClass();
        if (!type.isArray()) {
            return obj.hashCode();
        }
        if (obj instanceof Object[] value) {
            return Arrays.deepHashCode(value);
        }
        if (obj instanceof int[] value) {
            return Arrays.hashCode(value);
        }
        if (obj instanceof long[] value) {
            return Arrays.hashCode(value);
        }
        if (obj instanceof short[] value) {
            return Arrays.hashCode(value);
        }
        if (obj instanceof byte[] value) {
            return Arrays.hashCode(value);
        }
        if (obj instanceof char[] value) {
            return Arrays.hashCode(value);
        }
        if (obj instanceof boolean[] value) {
            return Arrays.hashCode(value);
        }
        if (obj instanceof float[] value) {
            return Arrays.hashCode(value);
        }
        if (obj instanceof double[] value) {
            return Arrays.hashCode(value);
        }
        return obj.hashCode();
    }

    /**
     * 获取对象身份 hashCode。
     *
     * @param obj 待处理对象
     * @return 身份 hashCode 值，null 返回 0
     */
    public static int identityHashCode(Object obj) {
        return obj == null ? 0 : System.identityHashCode(obj);
    }

    /**
     * 判断对象是否为指定类型实例。
     *
     * @param obj  待判断对象
     * @param type 目标类型
     * @return 是指定类型实例返回 true，否则返回 false
     */
    public static boolean isInstance(Object obj, Class<?> type) {
        return type != null && type.isInstance(obj);
    }

    /**
     * 判断对象是否不是指定类型实例。
     *
     * @param obj  待判断对象
     * @param type 目标类型
     * @return 不是指定类型实例返回 true，否则返回 false
     */
    public static boolean isNotInstance(Object obj, Class<?> type) {
        return !isInstance(obj, type);
    }

    /**
     * 判断对象是否属于任意指定类型。
     *
     * @param obj   待判断对象
     * @param types 目标类型数组
     * @return 属于任意指定类型返回 true，否则返回 false
     */
    public static boolean isAnyInstance(Object obj, Class<?>... types) {
        if (types == null || types.length == 0) {
            return false;
        }
        for (Class<?> type : types) {
            if (isInstance(obj, type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断对象是否同时符合所有指定类型。
     *
     * @param obj   待判断对象
     * @param types 目标类型数组
     * @return 同时符合所有指定类型返回 true，空数组或数组本身为 null 返回 false
     */
    public static boolean isAllInstance(Object obj, Class<?>... types) {
        if (types == null || types.length == 0) {
            return false;
        }
        for (Class<?> type : types) {
            if (!isInstance(obj, type)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断源类型是否可以赋值给目标类型。
     *
     * @param sourceType 源类型
     * @param targetType 目标类型
     * @return 可以赋值返回 true，否则返回 false
     */
    public static boolean isAssignable(Class<?> sourceType, Class<?> targetType) {
        return sourceType != null && targetType != null && targetType.isAssignableFrom(sourceType);
    }

    /**
     * 判断对象是否为基础常用类型。
     *
     * @param obj 待判断对象
     * @return 是基础常用类型返回 true，否则返回 false
     */
    public static boolean isBasicType(Object obj) {
        if (obj == null) {
            return false;
        }
        return obj instanceof CharSequence
                || obj instanceof Number
                || obj instanceof Boolean
                || obj instanceof Character
                || obj instanceof BigDecimal
                || obj instanceof BigInteger
                || obj instanceof LocalDate
                || obj instanceof LocalDateTime
                || obj instanceof LocalTime
                || obj instanceof Date
                || obj instanceof Enum<?>;
    }

    /**
     * 判断对象是否为基本类型包装类。
     *
     * @param obj 待判断对象
     * @return 是基本类型包装类返回 true，否则返回 false
     */
    public static boolean isPrimitiveWrapper(Object obj) {
        if (obj == null) {
            return false;
        }
        return obj instanceof Boolean
                || obj instanceof Byte
                || obj instanceof Short
                || obj instanceof Integer
                || obj instanceof Long
                || obj instanceof Float
                || obj instanceof Double
                || obj instanceof Character;
    }

    /**
     * 判断对象是否为 JDK 类型。
     *
     * @param obj 待判断对象
     * @return 是 JDK 类型返回 true，否则返回 false
     */
    public static boolean isJdkType(Object obj) {
        if (obj == null) {
            return false;
        }
        Package objectPackage = obj.getClass().getPackage();
        if (objectPackage == null) {
            return false;
        }
        String packageName = objectPackage.getName();
        return packageName.equals("java") || packageName.startsWith("java.")
                || packageName.equals("javax") || packageName.startsWith("javax.")
                || packageName.equals("jdk") || packageName.startsWith("jdk.")
                || packageName.equals("com.sun") || packageName.startsWith("com.sun.");
    }

    /**
     * 泛型强制转换。
     *
     * @param obj 待转换对象
     * @param <T> 目标类型
     * @return 转换后的对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object obj) {
        return (T) obj;
    }

    /**
     * 按指定类型转换对象。
     *
     * @param obj  待转换对象
     * @param type 目标类型
     * @param <T>  目标类型
     * @return 转换后的对象
     */
    public static <T> T cast(Object obj, Class<T> type) {
        Objects.requireNonNull(type, "type 不能为 null");
        return type.cast(obj);
    }

    /**
     * 按指定类型安全转换对象，转换失败返回 null。
     *
     * @param obj  待转换对象
     * @param type 目标类型
     * @param <T>  目标类型
     * @return 转换后的对象，失败返回 null
     */
    public static <T> T castOrNull(Object obj, Class<T> type) {
        if (type == null || obj == null || !type.isInstance(obj)) {
            return null;
        }
        return type.cast(obj);
    }

    /**
     * 按指定类型安全转换对象，转换失败返回默认值。
     *
     * @param obj          待转换对象
     * @param type         目标类型
     * @param defaultValue 默认值
     * @param <T>          目标类型
     * @return 转换后的对象或默认值
     */
    public static <T> T castOrDefault(Object obj, Class<T> type, T defaultValue) {
        T result = castOrNull(obj, type);
        return result == null ? defaultValue : result;
    }

    /**
     * 按指定类型转换对象，转换失败抛出异常。
     *
     * @param obj     待转换对象
     * @param type    目标类型
     * @param message 异常消息
     * @param <T>     目标类型
     * @return 转换后的对象
     */
    public static <T> T castOrThrow(Object obj, Class<T> type, String message) {
        Objects.requireNonNull(type, "type 不能为 null");
        if (obj == null || !type.isInstance(obj)) {
            throw new IllegalArgumentException(message);
        }
        return type.cast(obj);
    }

    /**
     * 按指定类型安全转换对象并返回 Optional。
     *
     * @param obj  待转换对象
     * @param type 目标类型
     * @param <T>  目标类型
     * @return 转换结果 Optional
     */
    public static <T> Optional<T> safeCast(Object obj, Class<T> type) {
        return Optional.ofNullable(castOrNull(obj, type));
    }

    /**
     * 按指定类型安全转换对象。
     *
     * @param obj  待转换对象
     * @param type 目标类型
     * @param <T>  目标类型
     * @return 转换后的对象，失败返回 null
     */
    public static <T> T as(Object obj, Class<T> type) {
        return castOrNull(obj, type);
    }

    /**
     * 将对象包装为 Optional。
     *
     * @param obj 待包装对象
     * @param <T> 对象类型
     * @return Optional 对象
     */
    public static <T> Optional<T> optional(T obj) {
        return Optional.ofNullable(obj);
    }

    /**
     * 对象非空内容时包装为 Optional。
     *
     * @param obj 待包装对象
     * @param <T> 对象类型
     * @return Optional 对象
     */
    public static <T> Optional<T> optionalIfNotEmpty(T obj) {
        return isEmpty(obj) ? Optional.empty() : Optional.of(obj);
    }

    /**
     * 对象非空白内容时包装为 Optional。
     *
     * @param obj 待包装对象
     * @param <T> 对象类型
     * @return Optional 对象
     */
    public static <T> Optional<T> optionalIfNotBlank(T obj) {
        return isBlank(obj) ? Optional.empty() : Optional.of(obj);
    }

    /**
     * 从 Optional 中取值，没有则返回 null。
     *
     * @param optional Optional 对象
     * @param <T>      值类型
     * @return Optional 中的值或 null
     */
    public static <T> T unwrap(Optional<T> optional) {
        return optional == null ? null : optional.orElse(null);
    }

    /**
     * 从 Optional 中取值，没有则返回默认值。
     *
     * @param optional     Optional 对象
     * @param defaultValue 默认值
     * @param <T>          值类型
     * @return Optional 中的值或默认值
     */
    public static <T> T unwrapOrDefault(Optional<T> optional, T defaultValue) {
        return optional == null ? defaultValue : optional.orElse(defaultValue);
    }

    /**
     * 从 Optional 中取值，没有则通过 Supplier 获取默认值。
     *
     * @param optional Optional 对象
     * @param supplier 默认值提供器
     * @param <T>      值类型
     * @return Optional 中的值或 Supplier 返回值
     */
    public static <T> T unwrapOrGet(Optional<T> optional, Supplier<? extends T> supplier) {
        if (optional != null && optional.isPresent()) {
            return optional.get();
        }
        Objects.requireNonNull(supplier, "supplier 不能为 null");
        return supplier.get();
    }

    /**
     * 对象为 null 时执行动作。
     *
     * @param obj    待判断对象
     * @param action 执行动作
     */
    public static void ifNull(Object obj, Runnable action) {
        Objects.requireNonNull(action, "action 不能为 null");
        if (obj == null) {
            action.run();
        }
    }

    /**
     * 对象非 null 时执行动作。
     *
     * @param obj    待判断对象
     * @param action 执行动作
     * @param <T>    对象类型
     */
    public static <T> void ifNotNull(T obj, Consumer<? super T> action) {
        Objects.requireNonNull(action, "action 不能为 null");
        if (obj != null) {
            action.accept(obj);
        }
    }

    /**
     * 对象为空内容时执行动作。
     *
     * @param obj    待判断对象
     * @param action 执行动作
     */
    public static void ifEmpty(Object obj, Runnable action) {
        Objects.requireNonNull(action, "action 不能为 null");
        if (isEmpty(obj)) {
            action.run();
        }
    }

    /**
     * 对象非空内容时执行动作。
     *
     * @param obj    待判断对象
     * @param action 执行动作
     * @param <T>    对象类型
     */
    public static <T> void ifNotEmpty(T obj, Consumer<? super T> action) {
        Objects.requireNonNull(action, "action 不能为 null");
        if (isNotEmpty(obj)) {
            action.accept(obj);
        }
    }

    /**
     * 对象为空白内容时执行动作。
     *
     * @param obj    待判断对象
     * @param action 执行动作
     */
    public static void ifBlank(Object obj, Runnable action) {
        Objects.requireNonNull(action, "action 不能为 null");
        if (isBlank(obj)) {
            action.run();
        }
    }

    /**
     * 对象非空白内容时执行动作。
     *
     * @param obj    待判断对象
     * @param action 执行动作
     * @param <T>    对象类型
     */
    public static <T> void ifNotBlank(T obj, Consumer<? super T> action) {
        Objects.requireNonNull(action, "action 不能为 null");
        if (isNotBlank(obj)) {
            action.accept(obj);
        }
    }

    /**
     * 对象非 null 时执行映射转换。
     *
     * @param obj    待转换对象
     * @param mapper 转换器
     * @param <T>    原对象类型
     * @param <R>    结果类型
     * @return 转换结果，对象为 null 时返回 null
     */
    public static <T, R> R mapIfNotNull(T obj, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");
        return obj == null ? null : mapper.apply(obj);
    }

    /**
     * 对象非空内容时执行映射转换。
     *
     * @param obj    待转换对象
     * @param mapper 转换器
     * @param <T>    原对象类型
     * @param <R>    结果类型
     * @return 转换结果，对象为空内容时返回 null
     */
    public static <T, R> R mapIfNotEmpty(T obj, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");
        return isEmpty(obj) ? null : mapper.apply(obj);
    }

    /**
     * 对象非空内容时执行映射转换，否则返回默认值。
     *
     * @param obj          待转换对象
     * @param mapper       转换器
     * @param defaultValue 默认值
     * @param <T>          原对象类型
     * @param <R>          结果类型
     * @return 转换结果或默认值
     */
    public static <T, R> R mapOrDefault(T obj, Function<? super T, ? extends R> mapper, R defaultValue) {
        Objects.requireNonNull(mapper, "mapper 不能为 null");
        return isEmpty(obj) ? defaultValue : mapper.apply(obj);
    }

    /**
     * 比较两个 Comparable 对象，null 默认小于非 null。
     *
     * @param obj1 对象一
     * @param obj2 对象二
     * @param <T>  可比较类型
     * @return 比较结果
     */
    public static <T extends Comparable<? super T>> int compare(T obj1, T obj2) {
        return compare(obj1, obj2, false);
    }

    /**
     * 比较两个 Comparable 对象，并指定 null 排序规则。
     *
     * @param obj1        对象一
     * @param obj2        对象二
     * @param nullGreater null 是否大于非 null
     * @param <T>         可比较类型
     * @return 比较结果
     */
    public static <T extends Comparable<? super T>> int compare(T obj1, T obj2, boolean nullGreater) {
        if (obj1 == obj2) {
            return 0;
        }
        if (obj1 == null) {
            return nullGreater ? 1 : -1;
        }
        if (obj2 == null) {
            return nullGreater ? -1 : 1;
        }
        return obj1.compareTo(obj2);
    }

    /**
     * 返回两个对象中的较小值。
     *
     * @param obj1 对象一
     * @param obj2 对象二
     * @param <T>  可比较类型
     * @return 较小值
     */
    public static <T extends Comparable<? super T>> T min(T obj1, T obj2) {
        return compare(obj1, obj2) <= 0 ? obj1 : obj2;
    }

    /**
     * 返回两个对象中的较大值。
     *
     * @param obj1 对象一
     * @param obj2 对象二
     * @param <T>  可比较类型
     * @return 较大值
     */
    public static <T extends Comparable<? super T>> T max(T obj1, T obj2) {
        return compare(obj1, obj2) >= 0 ? obj1 : obj2;
    }

    /**
     * 判断值是否在闭区间内。
     *
     * @param value 待判断值
     * @param min   最小值
     * @param max   最大值
     * @param <T>   可比较类型
     * @return 在闭区间内返回 true，否则返回 false
     */
    public static <T extends Comparable<? super T>> boolean between(T value, T min, T max) {
        if (value == null || min == null || max == null) {
            return false;
        }
        validateRange(min, max);
        return compare(value, min) >= 0 && compare(value, max) <= 0;
    }

    /**
     * 判断值是否不在闭区间内。
     *
     * @param value 待判断值
     * @param min   最小值
     * @param max   最大值
     * @param <T>   可比较类型
     * @return 不在闭区间内返回 true，否则返回 false
     */
    public static <T extends Comparable<? super T>> boolean notBetween(T value, T min, T max) {
        return !between(value, min, max);
    }

    /**
     * 将值限制在指定闭区间内。
     *
     * @param value 待限制值
     * @param min   最小值，可为 null
     * @param max   最大值，可为 null
     * @param <T>   可比较类型
     * @return 限制后的值
     */
    public static <T extends Comparable<? super T>> T clamp(T value, T min, T max) {
        if (min != null && max != null) {
            validateRange(min, max);
        }
        if (value == null) {
            return null;
        }
        if (min != null && compare(value, min) < 0) {
            return min;
        }
        if (max != null && compare(value, max) > 0) {
            return max;
        }
        return value;
    }

    /**
     * 对象转字符串，null 返回字符串 "null"。
     *
     * @param obj 待转换对象
     * @return 字符串结果
     */
    public static String toString(Object obj) {
        return String.valueOf(obj);
    }

    /**
     * 对象转字符串，null 返回默认值。
     *
     * @param obj          待转换对象
     * @param defaultValue 默认值
     * @return 字符串结果
     */
    public static String toString(Object obj, String defaultValue) {
        return Objects.toString(obj, defaultValue);
    }

    /**
     * 对象转字符串，null 返回空字符串。
     *
     * @param obj 待转换对象
     * @return 字符串结果
     */
    public static String toStringOrEmpty(Object obj) {
        return Objects.toString(obj, "");
    }

    /**
     * 对象转字符串，null 返回 null。
     *
     * @param obj 待转换对象
     * @return 字符串结果
     */
    public static String toStringOrNull(Object obj) {
        return obj == null ? null : obj.toString();
    }

    /**
     * 返回对象身份字符串。
     *
     * @param obj 待处理对象
     * @return 身份字符串，null 返回 null
     */
    public static String identityToString(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(obj));
    }

    /**
     * 获取对象简单类名。
     *
     * @param obj 待处理对象
     * @return 简单类名，null 返回 null
     */
    public static String simpleClassName(Object obj) {
        return getSimpleClassName(obj);
    }

    /**
     * 获取对象完整类名。
     *
     * @param obj 待处理对象
     * @return 完整类名，null 返回 null
     */
    public static String className(Object obj) {
        return getClassName(obj);
    }

    /**
     * 执行 Supplier 并返回结果。
     *
     * @param supplier 值提供器
     * @param <T>      值类型
     * @return Supplier 返回值
     */
    public static <T> T get(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier 不能为 null");
        return supplier.get();
    }

    /**
     * 执行 Supplier 并返回结果，发生运行时异常时返回 null。
     *
     * @param supplier 值提供器
     * @param <T>      值类型
     * @return Supplier 返回值或 null
     */
    public static <T> T getOrNull(Supplier<T> supplier) {
        if (supplier == null) {
            return null;
        }
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * 执行 Supplier 并返回结果，结果为 null 或发生运行时异常时返回默认值。
     *
     * @param supplier     值提供器
     * @param defaultValue 默认值
     * @param <T>          值类型
     * @return Supplier 返回值或默认值
     */
    public static <T> T getOrDefault(Supplier<T> supplier, T defaultValue) {
        T value = getOrNull(supplier);
        return value == null ? defaultValue : value;
    }

    /**
     * 解包 Optional 或 Supplier，其他对象直接返回。
     *
     * @param obj 待解包对象
     * @return 解包后的值
     */
    public static Object unwrap(Object obj) {
        if (obj instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        if (obj instanceof Supplier<?> supplier) {
            return supplier.get();
        }
        return obj;
    }

    /**
     * 解包 Optional。
     *
     * @param optional Optional 对象
     * @param <T>      值类型
     * @return Optional 中的值或 null
     */
    public static <T> T unwrapOptional(Optional<T> optional) {
        return unwrap(optional);
    }

    /**
     * 执行 Supplier 获取值。
     *
     * @param supplier 值提供器
     * @param <T>      值类型
     * @return Supplier 返回值
     */
    public static <T> T unwrapSupplier(Supplier<T> supplier) {
        return supplier == null ? null : supplier.get();
    }

    /**
     * 判断对象是否为数组。
     *
     * @param obj 待判断对象
     * @return 是数组返回 true，否则返回 false
     */
    public static boolean isArray(Object obj) {
        return obj != null && obj.getClass().isArray();
    }

    /**
     * 获取数组长度。
     *
     * @param array 数组对象
     * @return 数组长度，null 返回 0
     */
    public static int arrayLength(Object array) {
        if (array == null) {
            return 0;
        }
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("array 必须是数组类型");
        }
        return Array.getLength(array);
    }

    /**
     * 判断对象是否为 null 或空数组。
     *
     * @param array 数组对象
     * @return 为 null 或空数组返回 true，否则返回 false
     */
    public static boolean isEmptyArray(Object array) {
        return arrayLength(array) == 0;
    }

    /**
     * 判断对象数组是否包含目标对象。
     *
     * @param array  对象数组
     * @param target 目标对象
     * @return 包含返回 true，否则返回 false
     */
    public static boolean contains(Object[] array, Object target) {
        if (array == null || array.length == 0) {
            return false;
        }
        for (Object value : array) {
            if (equals(value, target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断对象数组是否包含任意目标对象。
     *
     * @param array   对象数组
     * @param targets 目标对象数组
     * @return 包含任意目标对象返回 true，否则返回 false
     */
    public static boolean containsAny(Object[] array, Object... targets) {
        if (array == null || array.length == 0 || targets == null || targets.length == 0) {
            return false;
        }
        for (Object target : targets) {
            if (contains(array, target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断对象数组是否包含所有目标对象。
     *
     * @param array   对象数组
     * @param targets 目标对象数组
     * @return 包含所有目标对象返回 true，目标为空返回 false
     */
    public static boolean containsAll(Object[] array, Object... targets) {
        if (array == null || array.length == 0 || targets == null || targets.length == 0) {
            return false;
        }
        for (Object target : targets) {
            if (!contains(array, target)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 克隆对象。
     *
     * @param obj 待克隆对象
     * @param <T> 对象类型
     * @return 克隆后的对象，null 返回 null
     */
    public static <T> T clone(T obj) {
        if (obj == null) {
            return null;
        }
        T cloned = cloneInternal(obj);
        if (cloned == null) {
            throw new IllegalArgumentException("对象不支持克隆或克隆失败");
        }
        return cloned;
    }

    /**
     * 对象支持克隆时返回克隆对象，否则返回原对象。
     *
     * @param obj 待克隆对象
     * @param <T> 对象类型
     * @return 克隆对象或原对象
     */
    public static <T> T cloneIfPossible(T obj) {
        T cloned = cloneInternal(obj);
        return cloned == null ? obj : cloned;
    }

    /**
     * 判断对象是否支持克隆。
     *
     * @param obj 待判断对象
     * @return 支持克隆返回 true，否则返回 false
     */
    public static boolean isCloneable(Object obj) {
        return obj != null && (obj.getClass().isArray() || obj instanceof Cloneable);
    }

    /**
     * 基于 Java 序列化复制对象。
     *
     * @param obj 待复制对象
     * @param <T> 对象类型
     * @return 复制后的对象，null 返回 null
     */
    public static <T> T copyIfSerializable(T obj) {
        if (obj == null) {
            return null;
        }
        if (!(obj instanceof Serializable serializable)) {
            throw new IllegalArgumentException("对象必须实现 Serializable");
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(serializable);
            objectOutputStream.flush();
            try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
                @SuppressWarnings("unchecked")
                T copied = (T) objectInputStream.readObject();
                return copied;
            }
        } catch (IOException | ClassNotFoundException ex) {
            throw new IllegalStateException("对象序列化复制失败", ex);
        }
    }

    /**
     * 直接返回原对象。
     *
     * @param obj 原对象
     * @param <T> 对象类型
     * @return 原对象
     */
    public static <T> T identity(T obj) {
        return obj;
    }

    /**
     * 判断 Boolean 是否为 true。
     *
     * @param value Boolean 值
     * @return 为 true 返回 true，否则返回 false
     */
    public static boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    /**
     * 判断 Boolean 是否为 false。
     *
     * @param value Boolean 值
     * @return 为 false 返回 true，否则返回 false
     */
    public static boolean isFalse(Boolean value) {
        return Boolean.FALSE.equals(value);
    }

    /**
     * 判断 Boolean 是否不是 true。
     *
     * @param value Boolean 值
     * @return 不是 true 返回 true，否则返回 false
     */
    public static boolean isNotTrue(Boolean value) {
        return !isTrue(value);
    }

    /**
     * 判断 Boolean 是否不是 false。
     *
     * @param value Boolean 值
     * @return 不是 false 返回 true，否则返回 false
     */
    public static boolean isNotFalse(Boolean value) {
        return !isFalse(value);
    }

    /**
     * 判断对象是否为常见默认值。
     *
     * @param obj 待判断对象
     * @return 是常见默认值返回 true，否则返回 false
     */
    public static boolean isDefaultValue(Object obj) {
        if (obj == null) {
            return true;
        }
        if (obj instanceof Boolean value) {
            return !value;
        }
        if (obj instanceof Character value) {
            return value == '\0';
        }
        if (obj instanceof Number value) {
            return isZero(value);
        }
        return isEmpty(obj);
    }

    /**
     * 判断数字是否为 0。
     *
     * @param number 数字对象
     * @return 为 0 返回 true，否则返回 false
     */
    public static boolean isZero(Number number) {
        BigDecimal value = toBigDecimalOrNull(number);
        return value != null && value.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 判断数字是否为正数。
     *
     * @param number 数字对象
     * @return 为正数返回 true，否则返回 false
     */
    public static boolean isPositive(Number number) {
        BigDecimal value = toBigDecimalOrNull(number);
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判断数字是否为负数。
     *
     * @param number 数字对象
     * @return 为负数返回 true，否则返回 false
     */
    public static boolean isNegative(Number number) {
        BigDecimal value = toBigDecimalOrNull(number);
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * 获取对象 Class。
     *
     * @param obj 待处理对象
     * @return 对象 Class，null 返回 null
     */
    public static Class<?> getClass(Object obj) {
        return obj == null ? null : obj.getClass();
    }

    /**
     * 获取对象完整类名。
     *
     * @param obj 待处理对象
     * @return 完整类名，null 返回 null
     */
    public static String getClassName(Object obj) {
        Class<?> type = getClass(obj);
        return type == null ? null : type.getName();
    }

    /**
     * 获取对象简单类名。
     *
     * @param obj 待处理对象
     * @return 简单类名，null 返回 null
     */
    public static String getSimpleClassName(Object obj) {
        Class<?> type = getClass(obj);
        return type == null ? null : type.getSimpleName();
    }

    /**
     * 获取对象包名。
     *
     * @param obj 待处理对象
     * @return 包名，null 或无包名返回 null
     */
    public static String getPackageName(Object obj) {
        Class<?> type = getClass(obj);
        if (type == null || type.getPackage() == null) {
            return null;
        }
        return type.getPackage().getName();
    }

    /**
     * 获取对象规范类名。
     *
     * @param obj 待处理对象
     * @return 规范类名，null 返回 null
     */
    public static String getCanonicalName(Object obj) {
        Class<?> type = getClass(obj);
        return type == null ? null : type.getCanonicalName();
    }

    /**
     * 获取对象类型名称。
     *
     * @param obj 待处理对象
     * @return 类型名称，null 返回 null
     */
    public static String getTypeName(Object obj) {
        Class<?> type = getClass(obj);
        return type == null ? null : type.getTypeName();
    }

    /**
     * 判断两个对象是否属于同一个 Class。
     *
     * @param obj1 对象一
     * @param obj2 对象二
     * @return 属于同一个 Class 返回 true，否则返回 false
     */
    public static boolean isSameClass(Object obj1, Object obj2) {
        return obj1 != null && obj2 != null && obj1.getClass() == obj2.getClass();
    }

    /**
     * 首选值为 null 时返回备用值。
     *
     * @param preferred 首选值
     * @param fallback  备用值
     * @param <T>       对象类型
     * @return 首选值或备用值
     */
    public static <T> T choose(T preferred, T fallback) {
        return chooseIfNull(preferred, fallback);
    }

    /**
     * 值为 null 时返回备用值。
     *
     * @param value    原值
     * @param fallback 备用值
     * @param <T>      对象类型
     * @return 原值或备用值
     */
    public static <T> T chooseIfNull(T value, T fallback) {
        return value == null ? fallback : value;
    }

    /**
     * 值为空内容时返回备用值。
     *
     * @param value    原值
     * @param fallback 备用值
     * @param <T>      对象类型
     * @return 原值或备用值
     */
    public static <T> T chooseIfEmpty(T value, T fallback) {
        return isEmpty(value) ? fallback : value;
    }

    /**
     * 值为空白内容时返回备用值。
     *
     * @param value    原值
     * @param fallback 备用值
     * @param <T>      对象类型
     * @return 原值或备用值
     */
    public static <T> T chooseIfBlank(T value, T fallback) {
        return isBlank(value) ? fallback : value;
    }

    /**
     * 返回第一个满足条件的值。
     *
     * @param predicate 判断条件
     * @param values    候选值数组
     * @param <T>       对象类型
     * @return 第一个满足条件的值，没有则返回 null
     */
    @SafeVarargs
    public static <T> T firstMatch(Predicate<? super T> predicate, T... values) {
        Objects.requireNonNull(predicate, "predicate 不能为 null");
        if (values == null || values.length == 0) {
            return null;
        }
        for (T value : values) {
            if (predicate.test(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 返回第一个有值的 Optional。
     *
     * @param optionals Optional 候选数组
     * @param <T>       值类型
     * @return 第一个有值的 Optional，没有则返回 Optional.empty()
     */
    @SafeVarargs
    public static <T> Optional<T> firstPresent(Optional<T>... optionals) {
        if (optionals == null || optionals.length == 0) {
            return Optional.empty();
        }
        for (Optional<T> optional : optionals) {
            if (optional != null && optional.isPresent()) {
                return optional;
            }
        }
        return Optional.empty();
    }

    /**
     * 返回第一个非 null 值。
     *
     * @param values 候选值数组
     * @param <T>    对象类型
     * @return 第一个非 null 值，没有则返回 null
     */
    @SafeVarargs
    public static <T> T coalesce(T... values) {
        return firstNonNull(values);
    }

    private static <T extends Comparable<? super T>> void validateRange(T min, T max) {
        if (compare(min, max) > 0) {
            throw new IllegalArgumentException("min 不能大于 max");
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T cloneInternal(T obj) {
        if (obj == null) {
            return null;
        }
        Class<?> type = obj.getClass();
        if (type.isArray()) {
            int length = Array.getLength(obj);
            Object clonedArray = Array.newInstance(type.getComponentType(), length);
            System.arraycopy(obj, 0, clonedArray, 0, length);
            return (T) clonedArray;
        }
        if (!(obj instanceof Cloneable)) {
            return null;
        }
        Method cloneMethod = findCloneMethod(type);
        if (cloneMethod == null) {
            return null;
        }
        try {
            if (!cloneMethod.canAccess(obj)) {
                cloneMethod.setAccessible(true);
            }
            return (T) cloneMethod.invoke(obj);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException ex) {
            return null;
        }
    }

    private static Method findCloneMethod(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod("clone");
            } catch (NoSuchMethodException ex) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static BigDecimal toBigDecimalOrNull(Number number) {
        if (number == null) {
            return null;
        }
        if (number instanceof Double value && (value.isNaN() || value.isInfinite())) {
            return null;
        }
        if (number instanceof Float value && (value.isNaN() || value.isInfinite())) {
            return null;
        }
        if (number instanceof BigDecimal value) {
            return value;
        }
        if (number instanceof BigInteger value) {
            return new BigDecimal(value);
        }
        if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long) {
            return BigDecimal.valueOf(number.longValue());
        }
        if (number instanceof Float || number instanceof Double) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
