package io.github.atengk.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数字通用工具类，提供转换、判断、比较、计算、格式化、统计、随机、金额和分页等常用数字处理能力。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class NumberUtil {

    /** BigDecimal 数值 0。 */
    public static final BigDecimal ZERO = BigDecimal.ZERO;

    /** BigDecimal 数值 1。 */
    public static final BigDecimal ONE = BigDecimal.ONE;

    /** BigDecimal 数值 10。 */
    public static final BigDecimal TEN = BigDecimal.TEN;

    /** BigDecimal 数值 100。 */
    public static final BigDecimal HUNDRED = new BigDecimal("100");

    /** BigDecimal 数值 1000。 */
    public static final BigDecimal THOUSAND = new BigDecimal("1000");

    /** 默认保留小数位。 */
    public static final int DEFAULT_SCALE = 2;

    /** 默认除法保留小数位。 */
    public static final int DEFAULT_DIVIDE_SCALE = 6;

    /** 默认舍入模式。 */
    public static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;

    /** 默认最大分页大小。 */
    public static final int MAX_PAGE_SIZE = 500;

    /** 默认最小页码。 */
    public static final int MIN_PAGE_NUM = 1;

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_RANGE_SIZE = 100_000;
    private static final BigDecimal BYTE_UNIT = new BigDecimal("1024");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?((\\d+(\\.\\d*)?)|(\\.\\d+))([eE][-+]?\\d+)?");
    private static final DecimalFormatSymbols FORMAT_SYMBOLS = DecimalFormatSymbols.getInstance(Locale.ROOT);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private NumberUtil() {
        throw new UnsupportedOperationException("NumberUtil 是静态工具类，不能实例化");
    }

    /**
     * 将对象转换为 Integer，转换失败返回 null。
     *
     * @param value 待转换对象
     * @return Integer 数值，转换失败返回 null
     */
    public static Integer toInt(Object value) {
        return toInt(value, null);
    }

    /**
     * 将对象转换为 Integer，转换失败返回默认值。
     *
     * @param value 待转换对象
     * @param defaultValue 默认值
     * @return Integer 数值
     */
    public static Integer toInt(Object value, Integer defaultValue) {
        BigDecimal decimal = toBigDecimal(value);
        if (decimal == null) {
            return defaultValue;
        }
        try {
            return decimal.stripTrailingZeros().intValueExact();
        } catch (ArithmeticException ex) {
            return defaultValue;
        }
    }

    /**
     * 将对象转换为 Long，转换失败返回 null。
     *
     * @param value 待转换对象
     * @return Long 数值，转换失败返回 null
     */
    public static Long toLong(Object value) {
        return toLong(value, null);
    }

    /**
     * 将对象转换为 Long，转换失败返回默认值。
     *
     * @param value 待转换对象
     * @param defaultValue 默认值
     * @return Long 数值
     */
    public static Long toLong(Object value, Long defaultValue) {
        BigDecimal decimal = toBigDecimal(value);
        if (decimal == null) {
            return defaultValue;
        }
        try {
            return decimal.stripTrailingZeros().longValueExact();
        } catch (ArithmeticException ex) {
            return defaultValue;
        }
    }

    /**
     * 将对象转换为 Double，转换失败返回 null。
     *
     * @param value 待转换对象
     * @return Double 数值，转换失败返回 null
     */
    public static Double toDouble(Object value) {
        return toDouble(value, null);
    }

    /**
     * 将对象转换为 Double，转换失败返回默认值。
     *
     * @param value 待转换对象
     * @param defaultValue 默认值
     * @return Double 数值
     */
    public static Double toDouble(Object value, Double defaultValue) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null ? defaultValue : decimal.doubleValue();
    }

    /**
     * 将对象转换为 BigDecimal，转换失败返回 null。
     *
     * @param value 待转换对象
     * @return BigDecimal 数值，转换失败返回 null
     */
    public static BigDecimal toBigDecimal(Object value) {
        return toBigDecimal(value, null);
    }

    /**
     * 将对象转换为 BigDecimal，转换失败返回默认值。
     *
     * @param value 待转换对象
     * @param defaultValue 默认值
     * @return BigDecimal 数值
     */
    public static BigDecimal toBigDecimal(Object value, BigDecimal defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof BigInteger integer) {
            return new BigDecimal(integer);
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return BigDecimal.valueOf(((Number) value).longValue());
        }
        if (value instanceof Float floatValue) {
            return Float.isFinite(floatValue) ? BigDecimal.valueOf(floatValue.doubleValue()) : defaultValue;
        }
        if (value instanceof Double doubleValue) {
            return Double.isFinite(doubleValue) ? BigDecimal.valueOf(doubleValue) : defaultValue;
        }
        if (value instanceof Number number) {
            return parseDecimal(number.toString(), defaultValue);
        }
        if (value instanceof CharSequence sequence) {
            return parseDecimal(sequence.toString(), defaultValue);
        }
        return parseDecimal(value.toString(), defaultValue);
    }

    /**
     * 将字符串转换为 Number，转换失败返回 null。
     *
     * @param value 待转换字符串
     * @return Number 数值，转换失败返回 null
     */
    public static Number toNumber(String value) {
        return toBigDecimal(value);
    }

    /**
     * 将数字转换为字符串，数字为空返回 null。
     *
     * @param value 数字
     * @return 数字字符串
     */
    public static String toStr(Number value) {
        return toStr(value, null);
    }

    /**
     * 将数字转换为字符串，数字为空返回默认值。
     *
     * @param value 数字
     * @param defaultValue 默认值
     * @return 数字字符串
     */
    public static String toStr(Number value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null ? defaultValue : decimal.toPlainString();
    }

    /**
     * 数字为空时返回默认值。
     *
     * @param value 数字
     * @param defaultValue 默认值
     * @return 原数字或默认值
     */
    public static Number defaultIfNull(Number value, Number defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * 对象不能转换为合法数字时返回默认值。
     *
     * @param value 待检查对象
     * @param defaultValue 默认值
     * @return 原数字对应 BigDecimal 或默认值
     */
    public static Number defaultIfInvalid(Object value, Number defaultValue) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null ? defaultValue : decimal;
    }

    /**
     * Integer 为空时返回 0。
     *
     * @param value Integer 数字
     * @return 非空 Integer 数字
     */
    public static Integer zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * Long 为空时返回 0。
     *
     * @param value Long 数字
     * @return 非空 Long 数字
     */
    public static Long zeroIfNull(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * BigDecimal 为空时返回 BigDecimal.ZERO。
     *
     * @param value BigDecimal 数字
     * @return 非空 BigDecimal 数字
     */
    public static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    /**
     * 数字为空时返回空字符串。
     *
     * @param value 数字
     * @return 数字字符串或空字符串
     */
    public static String emptyIfNull(Number value) {
        return value == null ? "" : toStr(value, "");
    }

    /**
     * 数字为 0 时返回 null，否则返回 BigDecimal。
     *
     * @param value 数字
     * @return 非零 BigDecimal 或 null
     */
    public static BigDecimal nullIfZero(Number value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null || isZero(decimal) ? null : decimal;
    }

    /**
     * 数字为负数时返回 null，否则返回 BigDecimal。
     *
     * @param value 数字
     * @return 非负 BigDecimal 或 null
     */
    public static BigDecimal nullIfNegative(Number value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null || decimal.signum() < 0 ? null : decimal;
    }

    /**
     * 对象不能转换为合法数字时返回 null。
     *
     * @param value 待检查对象
     * @return 合法 BigDecimal 或 null
     */
    public static BigDecimal nullIfInvalid(Object value) {
        return toBigDecimal(value);
    }

    /**
     * 判断字符串是否为合法数字。
     *
     * @param value 字符串
     * @return 是数字返回 true
     */
    public static boolean isNumber(String value) {
        return toBigDecimal(value) != null;
    }

    /**
     * 判断字符串是否为整数。
     *
     * @param value 字符串
     * @return 是整数返回 true
     */
    public static boolean isInteger(String value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal != null && decimal.stripTrailingZeros().scale() <= 0;
    }

    /**
     * 判断字符串是否为小数。
     *
     * @param value 字符串
     * @return 是小数返回 true
     */
    public static boolean isDecimal(String value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal != null && decimal.stripTrailingZeros().scale() > 0;
    }

    /**
     * 判断数字是否为正数。
     *
     * @param value 数字
     * @return 正数返回 true
     */
    public static boolean isPositive(Number value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal != null && decimal.signum() > 0;
    }

    /**
     * 判断数字是否为负数。
     *
     * @param value 数字
     * @return 负数返回 true
     */
    public static boolean isNegative(Number value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal != null && decimal.signum() < 0;
    }

    /**
     * 判断数字是否为 0。
     *
     * @param value 数字
     * @return 为 0 返回 true
     */
    public static boolean isZero(Number value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal != null && decimal.compareTo(ZERO) == 0;
    }

    /**
     * 判断数字是否不为 0。
     *
     * @param value 数字
     * @return 不为 0 返回 true
     */
    public static boolean isNotZero(Number value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal != null && decimal.compareTo(ZERO) != 0;
    }

    /**
     * 判断数字是否大于等于 0。
     *
     * @param value 数字
     * @return 大于等于 0 返回 true
     */
    public static boolean isPositiveOrZero(Number value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal != null && decimal.signum() >= 0;
    }

    /**
     * 判断数字是否小于等于 0。
     *
     * @param value 数字
     * @return 小于等于 0 返回 true
     */
    public static boolean isNegativeOrZero(Number value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal != null && decimal.signum() <= 0;
    }

    /**
     * 判断数字是否为偶数。
     *
     * @param value 数字
     * @return 偶数返回 true
     */
    public static boolean isEven(Number value) {
        Long number = toLong(value);
        return number != null && number % 2 == 0;
    }

    /**
     * 判断数字是否为奇数。
     *
     * @param value 数字
     * @return 奇数返回 true
     */
    public static boolean isOdd(Number value) {
        Long number = toLong(value);
        return number != null && Math.abs(number % 2) == 1;
    }

    /**
     * 判断 Double 是否为有限数字。
     *
     * @param value Double 数字
     * @return 不是 NaN 和 Infinity 返回 true
     */
    public static boolean isFinite(Double value) {
        return value != null && Double.isFinite(value);
    }

    /**
     * 判断 Double 是否为 NaN。
     *
     * @param value Double 数字
     * @return 是 NaN 返回 true
     */
    public static boolean isNaN(Double value) {
        return value != null && Double.isNaN(value);
    }

    /**
     * 比较两个数字大小。
     *
     * @param a 数字 a
     * @param b 数字 b
     * @return a 小于 b 返回负数，等于返回 0，大于返回正数
     */
    public static int compare(Number a, Number b) {
        return requireDecimal(a, "a").compareTo(requireDecimal(b, "b"));
    }

    /**
     * 判断两个数字是否相等。
     *
     * @param a 数字 a
     * @param b 数字 b
     * @return 相等返回 true
     */
    public static boolean eq(Number a, Number b) {
        return compare(a, b) == 0;
    }

    /**
     * 判断两个数字是否不相等。
     *
     * @param a 数字 a
     * @param b 数字 b
     * @return 不相等返回 true
     */
    public static boolean ne(Number a, Number b) {
        return compare(a, b) != 0;
    }

    /**
     * 判断 a 是否大于 b。
     *
     * @param a 数字 a
     * @param b 数字 b
     * @return a 大于 b 返回 true
     */
    public static boolean gt(Number a, Number b) {
        return compare(a, b) > 0;
    }

    /**
     * 判断 a 是否大于等于 b。
     *
     * @param a 数字 a
     * @param b 数字 b
     * @return a 大于等于 b 返回 true
     */
    public static boolean ge(Number a, Number b) {
        return compare(a, b) >= 0;
    }

    /**
     * 判断 a 是否小于 b。
     *
     * @param a 数字 a
     * @param b 数字 b
     * @return a 小于 b 返回 true
     */
    public static boolean lt(Number a, Number b) {
        return compare(a, b) < 0;
    }

    /**
     * 判断 a 是否小于等于 b。
     *
     * @param a 数字 a
     * @param b 数字 b
     * @return a 小于等于 b 返回 true
     */
    public static boolean le(Number a, Number b) {
        return compare(a, b) <= 0;
    }

    /**
     * 判断数字是否在闭区间内。
     *
     * @param value 数字
     * @param min 最小值
     * @param max 最大值
     * @return 在范围内返回 true
     */
    public static boolean between(Number value, Number min, Number max) {
        return between(value, min, max, true);
    }

    /**
     * 判断数字是否在指定区间内。
     *
     * @param value 数字
     * @param min 最小值
     * @param max 最大值
     * @param includeBounds 是否包含边界
     * @return 在范围内返回 true
     */
    public static boolean between(Number value, Number min, Number max, boolean includeBounds) {
        validateMinMax(min, max);
        BigDecimal decimal = toBigDecimal(value);
        if (decimal == null) {
            return false;
        }
        BigDecimal minValue = requireDecimal(min, "min");
        BigDecimal maxValue = requireDecimal(max, "max");
        int lower = decimal.compareTo(minValue);
        int upper = decimal.compareTo(maxValue);
        return includeBounds ? lower >= 0 && upper <= 0 : lower > 0 && upper < 0;
    }

    /**
     * 获取最大数字，忽略 null 和非法数字。
     *
     * @param values 数字数组
     * @return 最大值，没有合法数字返回 null
     */
    public static BigDecimal max(Number... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        BigDecimal result = null;
        for (Number value : values) {
            BigDecimal decimal = toBigDecimal(value);
            if (decimal != null && (result == null || decimal.compareTo(result) > 0)) {
                result = decimal;
            }
        }
        return result;
    }

    /**
     * 获取最小数字，忽略 null 和非法数字。
     *
     * @param values 数字数组
     * @return 最小值，没有合法数字返回 null
     */
    public static BigDecimal min(Number... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        BigDecimal result = null;
        for (Number value : values) {
            BigDecimal decimal = toBigDecimal(value);
            if (decimal != null && (result == null || decimal.compareTo(result) < 0)) {
                result = decimal;
            }
        }
        return result;
    }

    /**
     * 两个数字相加，null 按 0 处理。
     *
     * @param a 数字 a
     * @param b 数字 b
     * @return 相加结果
     */
    public static BigDecimal add(Number a, Number b) {
        return decimalOrZero(a).add(decimalOrZero(b));
    }

    /**
     * 多个数字相加，null 按 0 处理。
     *
     * @param values 数字数组
     * @return 相加结果
     */
    public static BigDecimal add(Number... values) {
        BigDecimal result = ZERO;
        if (values == null) {
            return result;
        }
        for (Number value : values) {
            result = result.add(decimalOrZero(value));
        }
        return result;
    }

    /**
     * 两个数字相减，null 按 0 处理。
     *
     * @param a 被减数
     * @param b 减数
     * @return 相减结果
     */
    public static BigDecimal sub(Number a, Number b) {
        return decimalOrZero(a).subtract(decimalOrZero(b));
    }

    /**
     * 两个数字相乘，null 按 0 处理。
     *
     * @param a 数字 a
     * @param b 数字 b
     * @return 相乘结果
     */
    public static BigDecimal mul(Number a, Number b) {
        return decimalOrZero(a).multiply(decimalOrZero(b));
    }

    /**
     * 多个数字相乘，空数组返回 0。
     *
     * @param values 数字数组
     * @return 相乘结果
     */
    public static BigDecimal mul(Number... values) {
        if (values == null || values.length == 0) {
            return ZERO;
        }
        BigDecimal result = ONE;
        for (Number value : values) {
            result = result.multiply(decimalOrZero(value));
        }
        return result;
    }

    /**
     * 两个数字相除，使用默认除法精度。
     *
     * @param a 被除数
     * @param b 除数
     * @return 相除结果
     */
    public static BigDecimal div(Number a, Number b) {
        return div(a, b, DEFAULT_DIVIDE_SCALE);
    }

    /**
     * 两个数字相除，指定小数位。
     *
     * @param a 被除数
     * @param b 除数
     * @param scale 小数位
     * @return 相除结果
     */
    public static BigDecimal div(Number a, Number b, int scale) {
        return div(a, b, scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 两个数字相除，指定小数位和舍入模式。
     *
     * @param a 被除数
     * @param b 除数
     * @param scale 小数位
     * @param mode 舍入模式
     * @return 相除结果
     */
    public static BigDecimal div(Number a, Number b, int scale, RoundingMode mode) {
        checkScale(scale);
        Objects.requireNonNull(mode, "舍入模式不能为空");
        BigDecimal divisor = requireNonZeroDecimal(b, "除数不能为0");
        return decimalOrZero(a).divide(divisor, scale, mode);
    }

    /**
     * 安全除法，除数为空或为 0 时返回 0。
     *
     * @param a 被除数
     * @param b 除数
     * @return 相除结果或 0
     */
    public static BigDecimal safeDiv(Number a, Number b) {
        return safeDiv(a, b, ZERO);
    }

    /**
     * 安全除法，除数为空或为 0 时返回默认值。
     *
     * @param a 被除数
     * @param b 除数
     * @param defaultValue 默认值
     * @return 相除结果或默认值
     */
    public static BigDecimal safeDiv(Number a, Number b, BigDecimal defaultValue) {
        BigDecimal divisor = toBigDecimal(b);
        if (divisor == null || divisor.compareTo(ZERO) == 0) {
            return defaultValue;
        }
        return decimalOrZero(a).divide(divisor, DEFAULT_DIVIDE_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 两个数字取余。
     *
     * @param a 被除数
     * @param b 除数
     * @return 取余结果
     */
    public static BigDecimal remainder(Number a, Number b) {
        BigDecimal divisor = requireNonZeroDecimal(b, "除数不能为0");
        return decimalOrZero(a).remainder(divisor);
    }

    /**
     * 获取数字绝对值，null 按 0 处理。
     *
     * @param value 数字
     * @return 绝对值
     */
    public static BigDecimal abs(Number value) {
        return decimalOrZero(value).abs();
    }

    /**
     * 获取数字相反数，null 按 0 处理。
     *
     * @param value 数字
     * @return 相反数
     */
    public static BigDecimal negate(Number value) {
        return decimalOrZero(value).negate();
    }

    /**
     * 获取数字的整数幂。
     *
     * @param value 数字
     * @param n 幂指数，不能小于 0
     * @return 幂运算结果
     */
    public static BigDecimal pow(Number value, int n) {
        if (n < 0) {
            throw new IllegalArgumentException("幂指数不能小于0");
        }
        return decimalOrZero(value).pow(n);
    }

    /**
     * 四舍五入到指定小数位。
     *
     * @param value 数字
     * @param scale 小数位
     * @return 舍入结果
     */
    public static BigDecimal round(Number value, int scale) {
        return round(value, scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 按指定舍入模式处理小数位。
     *
     * @param value 数字
     * @param scale 小数位
     * @param mode 舍入模式
     * @return 舍入结果
     */
    public static BigDecimal round(Number value, int scale, RoundingMode mode) {
        checkScale(scale);
        Objects.requireNonNull(mode, "舍入模式不能为空");
        return decimalOrZero(value).setScale(scale, mode);
    }

    /**
     * 按四舍五入处理小数位。
     *
     * @param value 数字
     * @param scale 小数位
     * @return 舍入结果
     */
    public static BigDecimal roundHalfUp(Number value, int scale) {
        return round(value, scale, RoundingMode.HALF_UP);
    }

    /**
     * 向下截断小数位。
     *
     * @param value 数字
     * @param scale 小数位
     * @return 截断结果
     */
    public static BigDecimal roundDown(Number value, int scale) {
        return round(value, scale, RoundingMode.DOWN);
    }

    /**
     * 向上进位小数位。
     *
     * @param value 数字
     * @param scale 小数位
     * @return 进位结果
     */
    public static BigDecimal roundUp(Number value, int scale) {
        return round(value, scale, RoundingMode.UP);
    }

    /**
     * 向下取整。
     *
     * @param value 数字
     * @return 整数结果
     */
    public static BigDecimal floor(Number value) {
        return decimalOrZero(value).setScale(0, RoundingMode.FLOOR);
    }

    /**
     * 向上取整。
     *
     * @param value 数字
     * @return 整数结果
     */
    public static BigDecimal ceil(Number value) {
        return decimalOrZero(value).setScale(0, RoundingMode.CEILING);
    }

    /**
     * 截断到指定小数位。
     *
     * @param value 数字
     * @param scale 小数位
     * @return 截断结果
     */
    public static BigDecimal truncate(Number value, int scale) {
        return round(value, scale, RoundingMode.DOWN);
    }

    /**
     * 去除 BigDecimal 尾部多余 0。
     *
     * @param value 数字
     * @return 去除尾部 0 的数字
     */
    public static BigDecimal stripTrailingZeros(Number value) {
        BigDecimal decimal = decimalOrZero(value).stripTrailingZeros();
        return decimal.scale() < 0 ? decimal.setScale(0) : decimal;
    }

    /**
     * 获取数字有效小数位数。
     *
     * @param value 数字
     * @return 小数位数
     */
    public static int scale(Number value) {
        BigDecimal decimal = stripTrailingZeros(value);
        return Math.max(decimal.scale(), 0);
    }

    /**
     * 设置小数位，使用默认舍入模式。
     *
     * @param value 数字
     * @param scale 小数位
     * @return 设置小数位后的数字
     */
    public static BigDecimal setScale(Number value, int scale) {
        return setScale(value, scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 设置小数位，使用指定舍入模式。
     *
     * @param value 数字
     * @param scale 小数位
     * @param mode 舍入模式
     * @return 设置小数位后的数字
     */
    public static BigDecimal setScale(Number value, int scale, RoundingMode mode) {
        return round(value, scale, mode);
    }

    /**
     * 将数字限制在指定闭区间内，value 为空时返回最小值。
     *
     * @param value 数字
     * @param min 最小值
     * @param max 最大值
     * @return 限制后的数字
     */
    public static BigDecimal clamp(Number value, Number min, Number max) {
        validateMinMax(min, max);
        BigDecimal decimal = toBigDecimal(value);
        BigDecimal minValue = requireDecimal(min, "min");
        BigDecimal maxValue = requireDecimal(max, "max");
        if (decimal == null || decimal.compareTo(minValue) < 0) {
            return minValue;
        }
        if (decimal.compareTo(maxValue) > 0) {
            return maxValue;
        }
        return decimal;
    }

    /**
     * 将数字限制在指定闭区间内。
     *
     * @param value 数字
     * @param min 最小值
     * @param max 最大值
     * @return 限制后的数字
     */
    public static BigDecimal limit(Number value, Number min, Number max) {
        return clamp(value, min, max);
    }

    /**
     * 判断数字是否在指定闭区间内。
     *
     * @param value 数字
     * @param min 最小值
     * @param max 最大值
     * @return 在范围内返回 true
     */
    public static boolean isInRange(Number value, Number min, Number max) {
        return between(value, min, max);
    }

    /**
     * 判断数字是否超出指定闭区间。
     *
     * @param value 数字
     * @param min 最小值
     * @param max 最大值
     * @return 超出范围返回 true
     */
    public static boolean isOutOfRange(Number value, Number min, Number max) {
        return !isInRange(value, min, max);
    }

    /**
     * 判断数字是否小于最小值。
     *
     * @param value 数字
     * @param min 最小值
     * @return 小于最小值返回 true
     */
    public static boolean lessThanMin(Number value, Number min) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal != null && decimal.compareTo(requireDecimal(min, "min")) < 0;
    }

    /**
     * 判断数字是否大于最大值。
     *
     * @param value 数字
     * @param max 最大值
     * @return 大于最大值返回 true
     */
    public static boolean greaterThanMax(Number value, Number max) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal != null && decimal.compareTo(requireDecimal(max, "max")) > 0;
    }

    /**
     * 数字超出范围时返回默认值。
     *
     * @param value 数字
     * @param min 最小值
     * @param max 最大值
     * @param defaultValue 默认值
     * @return 原数字或默认值
     */
    public static BigDecimal defaultIfOutOfRange(Number value, Number min, Number max, Number defaultValue) {
        return isInRange(value, min, max) ? toBigDecimal(value) : toBigDecimal(defaultValue);
    }

    /**
     * 数字小于最小值时返回最小值。
     *
     * @param value 数字
     * @param min 最小值
     * @return 原数字或最小值
     */
    public static BigDecimal minLimit(Number value, Number min) {
        BigDecimal minValue = requireDecimal(min, "min");
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null || decimal.compareTo(minValue) < 0 ? minValue : decimal;
    }

    /**
     * 数字大于最大值时返回最大值。
     *
     * @param value 数字
     * @param max 最大值
     * @return 原数字或最大值
     */
    public static BigDecimal maxLimit(Number value, Number max) {
        BigDecimal maxValue = requireDecimal(max, "max");
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null || decimal.compareTo(maxValue) > 0 ? maxValue : decimal;
    }

    /**
     * 计算百分比，默认保留 2 位小数。
     *
     * @param numerator 分子
     * @param denominator 分母
     * @return 百分比数字
     */
    public static BigDecimal percent(Number numerator, Number denominator) {
        return percent(numerator, denominator, DEFAULT_SCALE);
    }

    /**
     * 计算百分比，指定小数位。
     *
     * @param numerator 分子
     * @param denominator 分母
     * @param scale 小数位
     * @return 百分比数字
     */
    public static BigDecimal percent(Number numerator, Number denominator, int scale) {
        return percent(numerator, denominator, scale, ZERO);
    }

    /**
     * 计算百分比，分母无效时返回默认值。
     *
     * @param numerator 分子
     * @param denominator 分母
     * @param scale 小数位
     * @param defaultValue 默认值
     * @return 百分比数字
     */
    public static BigDecimal percent(Number numerator, Number denominator, int scale, BigDecimal defaultValue) {
        checkScale(scale);
        BigDecimal divisor = toBigDecimal(denominator);
        if (divisor == null || divisor.compareTo(ZERO) == 0) {
            return defaultValue;
        }
        return decimalOrZero(numerator).multiply(HUNDRED).divide(divisor, scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 计算百分比文本，默认保留 2 位小数。
     *
     * @param numerator 分子
     * @param denominator 分母
     * @return 百分比字符串
     */
    public static String percentText(Number numerator, Number denominator) {
        return percent(numerator, denominator).toPlainString() + "%";
    }

    /**
     * 计算比例，默认使用除法精度。
     *
     * @param numerator 分子
     * @param denominator 分母
     * @return 比例数字
     */
    public static BigDecimal ratio(Number numerator, Number denominator) {
        return ratio(numerator, denominator, DEFAULT_DIVIDE_SCALE);
    }

    /**
     * 计算比例，指定小数位。
     *
     * @param numerator 分子
     * @param denominator 分母
     * @param scale 小数位
     * @return 比例数字
     */
    public static BigDecimal ratio(Number numerator, Number denominator, int scale) {
        return ratio(numerator, denominator, scale, ZERO);
    }

    /**
     * 计算比例，分母无效时返回默认值。
     *
     * @param numerator 分子
     * @param denominator 分母
     * @param scale 小数位
     * @param defaultValue 默认值
     * @return 比例数字
     */
    public static BigDecimal ratio(Number numerator, Number denominator, int scale, BigDecimal defaultValue) {
        checkScale(scale);
        BigDecimal divisor = toBigDecimal(denominator);
        if (divisor == null || divisor.compareTo(ZERO) == 0) {
            return defaultValue;
        }
        return decimalOrZero(numerator).divide(divisor, scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 计算完成率或占比，等同于 ratio。
     *
     * @param current 当前值
     * @param total 总值
     * @return 比例数字
     */
    public static BigDecimal rate(Number current, Number total) {
        return ratio(current, total);
    }

    /**
     * 计算增长率百分比。
     *
     * @param current 当前值
     * @param previous 之前值
     * @return 增长率百分比
     */
    public static BigDecimal growthRate(Number current, Number previous) {
        return changeRate(current, previous);
    }

    /**
     * 计算下降率百分比。
     *
     * @param current 当前值
     * @param previous 之前值
     * @return 下降率百分比
     */
    public static BigDecimal decreaseRate(Number current, Number previous) {
        return percent(decimalOrZero(previous).subtract(decimalOrZero(current)), previous);
    }

    /**
     * 计算折扣率百分比。
     *
     * @param price 成交价
     * @param originalPrice 原价
     * @return 折扣率百分比
     */
    public static BigDecimal discount(Number price, Number originalPrice) {
        return percent(price, originalPrice);
    }

    /**
     * 计算加价率百分比。
     *
     * @param price 售价
     * @param cost 成本
     * @return 加价率百分比
     */
    public static BigDecimal markupRate(Number price, Number cost) {
        return percent(decimalOrZero(price).subtract(decimalOrZero(cost)), cost);
    }

    /**
     * 计算变化率百分比。
     *
     * @param current 当前值
     * @param previous 之前值
     * @return 变化率百分比
     */
    public static BigDecimal changeRate(Number current, Number previous) {
        return percent(decimalOrZero(current).subtract(decimalOrZero(previous)), previous);
    }

    /**
     * 使用默认格式格式化数字。
     *
     * @param value 数字
     * @return 格式化字符串
     */
    public static String format(Number value) {
        return format(value, "0.######");
    }

    /**
     * 使用指定模板格式化数字。
     *
     * @param value 数字
     * @param pattern DecimalFormat 模板
     * @return 格式化字符串
     */
    public static String format(Number value, String pattern) {
        BigDecimal decimal = toBigDecimal(value);
        if (decimal == null) {
            return "";
        }
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("数字格式模板不能为空");
        }
        DecimalFormat decimalFormat = new DecimalFormat(pattern, FORMAT_SYMBOLS);
        decimalFormat.setRoundingMode(DEFAULT_ROUNDING_MODE);
        return decimalFormat.format(decimal);
    }

    /**
     * 按固定小数位格式化数字。
     *
     * @param value 数字
     * @param scale 小数位
     * @return 格式化字符串
     */
    public static String formatDecimal(Number value, int scale) {
        return round(value, scale).toPlainString();
    }

    /**
     * 按千分位格式化数字。
     *
     * @param value 数字
     * @return 千分位字符串
     */
    public static String formatThousands(Number value) {
        return format(value, "#,##0.######");
    }

    /**
     * 将比例数字格式化为百分比文本，默认保留 2 位小数。
     *
     * @param value 比例数字，例如 0.25
     * @return 百分比文本，例如 25.00%
     */
    public static String formatPercent(Number value) {
        return formatPercent(value, DEFAULT_SCALE);
    }

    /**
     * 将比例数字格式化为百分比文本，指定小数位。
     *
     * @param value 比例数字，例如 0.25
     * @param scale 小数位
     * @return 百分比文本
     */
    public static String formatPercent(Number value, int scale) {
        checkScale(scale);
        return round(decimalOrZero(value).multiply(HUNDRED), scale).toPlainString() + "%";
    }

    /**
     * 将数字格式化为普通字符串，避免科学计数法。
     *
     * @param value 数字
     * @return 普通数字字符串
     */
    public static String formatPlain(Number value) {
        return toPlainString(value);
    }

    /**
     * 格式化金额，默认保留 2 位小数。
     *
     * @param value 金额
     * @return 金额字符串
     */
    public static String formatMoney(Number value) {
        return formatMoney(value, DEFAULT_SCALE);
    }

    /**
     * 格式化金额，指定小数位。
     *
     * @param value 金额
     * @param scale 小数位
     * @return 金额字符串
     */
    public static String formatMoney(Number value, int scale) {
        checkScale(scale);
        return format(round(value, scale), moneyPattern(scale));
    }

    /**
     * 去除尾部多余 0 后转换为字符串。
     *
     * @param value 数字
     * @return 数字字符串
     */
    public static String removeTrailingZeros(Number value) {
        return stripTrailingZeros(value).toPlainString();
    }

    /**
     * 将数字转换为普通字符串，避免科学计数法。
     *
     * @param value 数字
     * @return 普通数字字符串
     */
    public static String toPlainString(Number value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null ? "" : decimal.toPlainString();
    }

    /**
     * 对集合数字求和，忽略 null 和非法数字。
     *
     * @param values 数字集合
     * @return 求和结果
     */
    public static BigDecimal sum(Collection<? extends Number> values) {
        BigDecimal result = ZERO;
        if (values == null || values.isEmpty()) {
            return result;
        }
        for (Number value : values) {
            result = result.add(decimalOrZero(value));
        }
        return result;
    }

    /**
     * 对集合数字求平均值，默认使用除法精度。
     *
     * @param values 数字集合
     * @return 平均值
     */
    public static BigDecimal avg(Collection<? extends Number> values) {
        return avg(values, DEFAULT_DIVIDE_SCALE);
    }

    /**
     * 对集合数字求平均值，指定小数位。
     *
     * @param values 数字集合
     * @param scale 小数位
     * @return 平均值
     */
    public static BigDecimal avg(Collection<? extends Number> values, int scale) {
        checkScale(scale);
        if (values == null || values.isEmpty()) {
            return ZERO.setScale(scale, DEFAULT_ROUNDING_MODE);
        }
        List<BigDecimal> decimals = validDecimals(values);
        if (decimals.isEmpty()) {
            return ZERO.setScale(scale, DEFAULT_ROUNDING_MODE);
        }
        return sum(decimals).divide(BigDecimal.valueOf(decimals.size()), scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 获取集合最大值，忽略 null 和非法数字。
     *
     * @param values 数字集合
     * @return 最大值，没有合法数字返回 null
     */
    public static BigDecimal max(Collection<? extends Number> values) {
        return values == null ? null : values.stream()
                .map(NumberUtil::toBigDecimal)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * 获取集合最小值，忽略 null 和非法数字。
     *
     * @param values 数字集合
     * @return 最小值，没有合法数字返回 null
     */
    public static BigDecimal min(Collection<? extends Number> values) {
        return values == null ? null : values.stream()
                .map(NumberUtil::toBigDecimal)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * 统计集合中正数数量。
     *
     * @param values 数字集合
     * @return 正数数量
     */
    public static long countPositive(Collection<? extends Number> values) {
        return countBy(values, NumberUtil::isPositive);
    }

    /**
     * 统计集合中负数数量。
     *
     * @param values 数字集合
     * @return 负数数量
     */
    public static long countNegative(Collection<? extends Number> values) {
        return countBy(values, NumberUtil::isNegative);
    }

    /**
     * 统计集合中零值数量。
     *
     * @param values 数字集合
     * @return 零值数量
     */
    public static long countZero(Collection<? extends Number> values) {
        return countBy(values, NumberUtil::isZero);
    }

    /**
     * 计算集合数字中位数。
     *
     * @param values 数字集合
     * @return 中位数，没有合法数字返回 0
     */
    public static BigDecimal median(Collection<? extends Number> values) {
        List<BigDecimal> decimals = validDecimals(values);
        if (decimals.isEmpty()) {
            return ZERO;
        }
        decimals.sort(Comparator.naturalOrder());
        int size = decimals.size();
        int middle = size / 2;
        if (size % 2 == 1) {
            return decimals.get(middle);
        }
        return decimals.get(middle - 1).add(decimals.get(middle)).divide(BigDecimal.valueOf(2), DEFAULT_DIVIDE_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 计算集合数字总体方差。
     *
     * @param values 数字集合
     * @return 总体方差
     */
    public static BigDecimal variance(Collection<? extends Number> values) {
        List<BigDecimal> decimals = validDecimals(values);
        if (decimals.isEmpty()) {
            return ZERO.setScale(DEFAULT_DIVIDE_SCALE, DEFAULT_ROUNDING_MODE);
        }
        BigDecimal average = avg(decimals, DEFAULT_DIVIDE_SCALE);
        BigDecimal total = ZERO;
        for (BigDecimal decimal : decimals) {
            BigDecimal diff = decimal.subtract(average);
            total = total.add(diff.multiply(diff));
        }
        return total.divide(BigDecimal.valueOf(decimals.size()), DEFAULT_DIVIDE_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 计算集合数字总体标准差。
     *
     * @param values 数字集合
     * @return 总体标准差
     */
    public static BigDecimal standardDeviation(Collection<? extends Number> values) {
        BigDecimal variance = variance(values);
        return variance.sqrt(MathContext.DECIMAL64).setScale(DEFAULT_DIVIDE_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 按对象字段求和。
     *
     * @param list 对象集合
     * @param mapper 数字字段映射函数
     * @param <T> 对象类型
     * @return 求和结果
     */
    public static <T> BigDecimal sumBy(Collection<T> list, Function<T, ? extends Number> mapper) {
        Objects.requireNonNull(mapper, "字段映射函数不能为空");
        if (list == null || list.isEmpty()) {
            return ZERO;
        }
        BigDecimal result = ZERO;
        for (T item : list) {
            if (item != null) {
                result = result.add(decimalOrZero(mapper.apply(item)));
            }
        }
        return result;
    }

    /**
     * 按对象字段求平均值。
     *
     * @param list 对象集合
     * @param mapper 数字字段映射函数
     * @param <T> 对象类型
     * @return 平均值
     */
    public static <T> BigDecimal avgBy(Collection<T> list, Function<T, ? extends Number> mapper) {
        Objects.requireNonNull(mapper, "字段映射函数不能为空");
        if (list == null || list.isEmpty()) {
            return ZERO.setScale(DEFAULT_DIVIDE_SCALE, DEFAULT_ROUNDING_MODE);
        }
        List<BigDecimal> decimals = new ArrayList<>();
        for (T item : list) {
            if (item != null) {
                BigDecimal decimal = toBigDecimal(mapper.apply(item));
                if (decimal != null) {
                    decimals.add(decimal);
                }
            }
        }
        return avg(decimals, DEFAULT_DIVIDE_SCALE);
    }

    /**
     * 按对象字段取最大值。
     *
     * @param list 对象集合
     * @param mapper 数字字段映射函数
     * @param <T> 对象类型
     * @return 最大值，没有合法数字返回 null
     */
    public static <T> BigDecimal maxBy(Collection<T> list, Function<T, ? extends Number> mapper) {
        Objects.requireNonNull(mapper, "字段映射函数不能为空");
        if (list == null || list.isEmpty()) {
            return null;
        }
        BigDecimal result = null;
        for (T item : list) {
            if (item != null) {
                BigDecimal decimal = toBigDecimal(mapper.apply(item));
                if (decimal != null && (result == null || decimal.compareTo(result) > 0)) {
                    result = decimal;
                }
            }
        }
        return result;
    }

    /**
     * 按对象字段取最小值。
     *
     * @param list 对象集合
     * @param mapper 数字字段映射函数
     * @param <T> 对象类型
     * @return 最小值，没有合法数字返回 null
     */
    public static <T> BigDecimal minBy(Collection<T> list, Function<T, ? extends Number> mapper) {
        Objects.requireNonNull(mapper, "字段映射函数不能为空");
        if (list == null || list.isEmpty()) {
            return null;
        }
        BigDecimal result = null;
        for (T item : list) {
            if (item != null) {
                BigDecimal decimal = toBigDecimal(mapper.apply(item));
                if (decimal != null && (result == null || decimal.compareTo(result) < 0)) {
                    result = decimal;
                }
            }
        }
        return result;
    }

    /**
     * 批量转换为 BigDecimal 列表，忽略非法数字。
     *
     * @param values 原始集合
     * @return BigDecimal 列表
     */
    public static List<BigDecimal> toBigDecimalList(Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<BigDecimal> result = new ArrayList<>();
        for (Object value : values) {
            BigDecimal decimal = toBigDecimal(value);
            if (decimal != null) {
                result.add(decimal);
            }
        }
        return result;
    }

    /**
     * 批量转换为 Long 列表，忽略非法数字。
     *
     * @param values 原始集合
     * @return Long 列表
     */
    public static List<Long> toLongList(Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (Object value : values) {
            Long number = toLong(value);
            if (number != null) {
                result.add(number);
            }
        }
        return result;
    }

    /**
     * 批量转换为 Integer 列表，忽略非法数字。
     *
     * @param values 原始集合
     * @return Integer 列表
     */
    public static List<Integer> toIntList(Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>();
        for (Object value : values) {
            Integer number = toInt(value);
            if (number != null) {
                result.add(number);
            }
        }
        return result;
    }

    /**
     * 过滤集合中的 null 数字。
     *
     * @param values 数字集合
     * @return 非空数字列表
     */
    public static List<BigDecimal> filterNull(Collection<? extends Number> values) {
        return validDecimals(values);
    }

    /**
     * 过滤集合中的正数。
     *
     * @param values 数字集合
     * @return 正数列表
     */
    public static List<BigDecimal> filterPositive(Collection<? extends Number> values) {
        return filterBy(values, NumberUtil::isPositive);
    }

    /**
     * 过滤集合中的负数。
     *
     * @param values 数字集合
     * @return 负数列表
     */
    public static List<BigDecimal> filterNegative(Collection<? extends Number> values) {
        return filterBy(values, NumberUtil::isNegative);
    }

    /**
     * 过滤集合中的零值。
     *
     * @param values 数字集合
     * @return 零值列表
     */
    public static List<BigDecimal> filterZero(Collection<? extends Number> values) {
        return filterBy(values, NumberUtil::isZero);
    }

    /**
     * 对数字集合去重，忽略 null 和非法数字。
     *
     * @param values 数字集合
     * @return 去重后的列表
     */
    public static List<BigDecimal> distinct(Collection<? extends Number> values) {
        List<BigDecimal> result = new ArrayList<>();
        for (BigDecimal decimal : validDecimals(values)) {
            boolean exists = result.stream().anyMatch(item -> item.compareTo(decimal) == 0);
            if (!exists) {
                result.add(decimal);
            }
        }
        return result;
    }

    /**
     * 对数字集合升序排序。
     *
     * @param values 数字集合
     * @return 升序列表
     */
    public static List<BigDecimal> sortAsc(Collection<? extends Number> values) {
        List<BigDecimal> result = validDecimals(values);
        result.sort(Comparator.naturalOrder());
        return result;
    }

    /**
     * 对数字集合降序排序。
     *
     * @param values 数字集合
     * @return 降序列表
     */
    public static List<BigDecimal> sortDesc(Collection<? extends Number> values) {
        List<BigDecimal> result = validDecimals(values);
        result.sort(Comparator.reverseOrder());
        return result;
    }

    /**
     * 生成 Integer 闭区间列表，支持升序和降序。
     *
     * @param start 开始值
     * @param end 结束值
     * @return 区间列表
     */
    public static List<Integer> range(Integer start, Integer end) {
        if (start == null || end == null) {
            return List.of();
        }
        long size = Math.abs((long) end - start) + 1;
        checkRangeSize(size);
        List<Integer> result = new ArrayList<>((int) size);
        if (start <= end) {
            for (int i = start; i <= end; i++) {
                result.add(i);
            }
        } else {
            for (int i = start; i >= end; i--) {
                result.add(i);
            }
        }
        return result;
    }

    /**
     * 生成 Long 闭区间列表，支持升序和降序。
     *
     * @param start 开始值
     * @param end 结束值
     * @return 区间列表
     */
    public static List<Long> range(Long start, Long end) {
        if (start == null || end == null) {
            return List.of();
        }
        BigInteger size = BigInteger.valueOf(end).subtract(BigInteger.valueOf(start)).abs().add(BigInteger.ONE);
        if (size.compareTo(BigInteger.valueOf(MAX_RANGE_SIZE)) > 0) {
            throw new IllegalArgumentException("区间长度不能超过" + MAX_RANGE_SIZE);
        }
        List<Long> result = new ArrayList<>(size.intValue());
        if (start <= end) {
            for (long i = start; i <= end; i++) {
                result.add(i);
            }
        } else {
            for (long i = start; i >= end; i--) {
                result.add(i);
            }
        }
        return result;
    }

    /**
     * 生成指定闭区间内的随机整数。
     *
     * @param min 最小值
     * @param max 最大值
     * @return 随机整数
     */
    public static int randomInt(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("最小值不能大于最大值");
        }
        return (int) randomLong(min, max);
    }

    /**
     * 生成指定闭区间内的随机长整数。
     *
     * @param min 最小值
     * @param max 最大值
     * @return 随机长整数
     */
    public static long randomLong(long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException("最小值不能大于最大值");
        }
        if (min == max) {
            return min;
        }
        BigInteger bound = BigInteger.valueOf(max).subtract(BigInteger.valueOf(min)).add(BigInteger.ONE);
        if (bound.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0) {
            long offset = ThreadLocalRandom.current().nextLong(bound.longValue());
            return min + offset;
        }
        long candidate;
        do {
            candidate = ThreadLocalRandom.current().nextLong();
        } while (candidate < min || candidate > max);
        return candidate;
    }

    /**
     * 生成指定半开区间内的随机小数，范围为 [min, max)。
     *
     * @param min 最小值
     * @param max 最大值
     * @return 随机小数
     */
    public static double randomDouble(double min, double max) {
        if (!Double.isFinite(min) || !Double.isFinite(max) || min >= max) {
            throw new IllegalArgumentException("随机小数范围不合法");
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    /**
     * 生成指定范围内的随机 BigDecimal。
     *
     * @param min 最小值
     * @param max 最大值
     * @param scale 小数位
     * @return 随机 BigDecimal
     */
    public static BigDecimal randomBigDecimal(BigDecimal min, BigDecimal max, int scale) {
        checkScale(scale);
        if (min == null || max == null || min.compareTo(max) > 0) {
            throw new IllegalArgumentException("随机 BigDecimal 范围不合法");
        }
        if (min.compareTo(max) == 0) {
            return min.setScale(scale, DEFAULT_ROUNDING_MODE);
        }
        BigDecimal range = max.subtract(min);
        BigDecimal random = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble());
        return min.add(range.multiply(random)).setScale(scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 生成指定长度的数字字符串。
     *
     * @param length 长度
     * @return 数字字符串
     */
    public static String randomDigits(int length) {
        return randomDigits(length, false);
    }

    /**
     * 生成指定长度的数字验证码。
     *
     * @param length 长度
     * @return 数字验证码
     */
    public static String randomCode(int length) {
        return randomDigits(length);
    }

    /**
     * 生成指定长度的安全随机数字验证码。
     *
     * @param length 长度
     * @return 安全随机数字验证码
     */
    public static String secureRandomCode(int length) {
        return secureRandomDigits(length);
    }

    /**
     * 生成指定长度的安全随机数字字符串。
     *
     * @param length 长度
     * @return 安全随机数字字符串
     */
    public static String secureRandomDigits(int length) {
        return randomDigits(length, true);
    }

    /**
     * 生成 1 到 max 之间的随机正整数。
     *
     * @param max 最大值
     * @return 随机正整数
     */
    public static int randomPositiveInt(int max) {
        if (max <= 0) {
            throw new IllegalArgumentException("最大值必须大于0");
        }
        return randomInt(1, max);
    }

    /**
     * 生成 min 到 -1 之间的随机负整数。
     *
     * @param min 最小值，必须小于 0
     * @return 随机负整数
     */
    public static int randomNegativeInt(int min) {
        if (min >= 0) {
            throw new IllegalArgumentException("最小值必须小于0");
        }
        return randomInt(min, -1);
    }

    /**
     * 清理数字字符串中的空格和千分位逗号。
     *
     * @param value 原始字符串
     * @return 清理后的字符串
     */
    public static String cleanNumber(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace(",", "").replace(" ", "");
    }

    /**
     * 移除字符串中的千分位逗号。
     *
     * @param value 原始字符串
     * @return 移除逗号后的字符串
     */
    public static String removeComma(String value) {
        return value == null ? "" : value.replace(",", "");
    }

    /**
     * 标准化数字字符串，去除空格、逗号、常见货币符号。
     *
     * @param value 原始字符串
     * @return 标准化字符串
     */
    public static String normalizeNumber(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replace(",", "")
                .replace(" ", "")
                .replace("￥", "")
                .replace("¥", "")
                .replace("$", "");
    }

    /**
     * 判断字符串是否为可解析的数字文本。
     *
     * @param value 字符串
     * @return 可解析返回 true
     */
    public static boolean isNumericText(String value) {
        return toBigDecimal(normalizeNumber(value)) != null;
    }

    /**
     * 解析数字文本。
     *
     * @param value 数字文本
     * @return BigDecimal 数字，解析失败返回 null
     */
    public static BigDecimal parseNumberText(String value) {
        return toBigDecimal(normalizeNumber(value));
    }

    /**
     * 解析百分比文本，返回比例值，例如 25% 返回 0.25。
     *
     * @param value 百分比文本
     * @return 比例值，解析失败返回 null
     */
    public static BigDecimal parsePercentText(String value) {
        if (value == null) {
            return null;
        }
        String text = normalizeNumber(value).replace("%", "");
        BigDecimal decimal = toBigDecimal(text);
        return decimal == null ? null : decimal.divide(HUNDRED, DEFAULT_DIVIDE_SCALE, DEFAULT_ROUNDING_MODE).stripTrailingZeros();
    }

    /**
     * 解析金额文本，去除常见货币符号和千分位。
     *
     * @param value 金额文本
     * @return 金额数字，解析失败返回 null
     */
    public static BigDecimal parseMoneyText(String value) {
        return parseNumberText(value);
    }

    /**
     * 判断字符串是否包含数字。
     *
     * @param value 字符串
     * @return 包含数字返回 true
     */
    public static boolean containsNumber(String value) {
        return value != null && NUMBER_PATTERN.matcher(value).find();
    }

    /**
     * 提取字符串中的第一个数字。
     *
     * @param value 字符串
     * @return 第一个数字，未找到返回 null
     */
    public static BigDecimal extractNumber(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(value);
        return matcher.find() ? toBigDecimal(matcher.group()) : null;
    }

    /**
     * 提取字符串中的所有数字。
     *
     * @param value 字符串
     * @return 数字列表
     */
    public static List<BigDecimal> extractNumbers(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<BigDecimal> result = new ArrayList<>();
        Matcher matcher = NUMBER_PATTERN.matcher(value);
        while (matcher.find()) {
            BigDecimal decimal = toBigDecimal(matcher.group());
            if (decimal != null) {
                result.add(decimal);
            }
        }
        return result;
    }

    /**
     * 将数字处理为标准金额，默认保留 2 位小数。
     *
     * @param value 金额数字
     * @return 标准金额
     */
    public static BigDecimal money(Number value) {
        return money(value, DEFAULT_SCALE);
    }

    /**
     * 将数字处理为标准金额，指定小数位。
     *
     * @param value 金额数字
     * @param scale 小数位
     * @return 标准金额
     */
    public static BigDecimal money(Number value, int scale) {
        return round(value, scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 金额相加。
     *
     * @param a 金额 a
     * @param b 金额 b
     * @return 金额结果
     */
    public static BigDecimal addMoney(Number a, Number b) {
        return money(add(a, b));
    }

    /**
     * 金额相减。
     *
     * @param a 金额 a
     * @param b 金额 b
     * @return 金额结果
     */
    public static BigDecimal subMoney(Number a, Number b) {
        return money(sub(a, b));
    }

    /**
     * 单价乘数量计算金额。
     *
     * @param price 单价
     * @param quantity 数量
     * @return 金额结果
     */
    public static BigDecimal mulMoney(Number price, Number quantity) {
        return money(mul(price, quantity));
    }

    /**
     * 金额除法。
     *
     * @param amount 金额
     * @param divisor 除数
     * @return 金额结果
     */
    public static BigDecimal divMoney(Number amount, Number divisor) {
        return money(div(amount, divisor, DEFAULT_DIVIDE_SCALE));
    }

    /**
     * 金额四舍五入，默认保留 2 位小数。
     *
     * @param value 金额数字
     * @return 金额结果
     */
    public static BigDecimal roundMoney(Number value) {
        return money(value);
    }

    /**
     * 将分转换为元。
     *
     * @param fen 分
     * @return 元金额
     */
    public static BigDecimal fenToYuan(Number fen) {
        return money(div(decimalOrZero(fen), HUNDRED, DEFAULT_SCALE));
    }

    /**
     * 将元转换为分。
     *
     * @param yuan 元金额
     * @return 分金额
     */
    public static Long yuanToFen(Number yuan) {
        return money(yuan).multiply(HUNDRED).setScale(0, DEFAULT_ROUNDING_MODE).longValueExact();
    }

    /**
     * 判断金额是否合法，要求非空、非负且最多 2 位小数。
     *
     * @param value 金额数字
     * @return 合法返回 true
     */
    public static boolean isValidMoney(Number value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal != null && decimal.signum() >= 0 && scale(decimal) <= DEFAULT_SCALE;
    }

    /**
     * 数字乘以 100。
     *
     * @param value 数字
     * @return 结果
     */
    public static BigDecimal multiplyBy100(Number value) {
        return decimalOrZero(value).multiply(HUNDRED);
    }

    /**
     * 数字除以 100。
     *
     * @param value 数字
     * @return 结果
     */
    public static BigDecimal divideBy100(Number value) {
        return decimalOrZero(value).divide(HUNDRED, DEFAULT_DIVIDE_SCALE, DEFAULT_ROUNDING_MODE).stripTrailingZeros();
    }

    /**
     * 计算千分比。
     *
     * @param numerator 分子
     * @param denominator 分母
     * @return 千分比数字
     */
    public static BigDecimal permillage(Number numerator, Number denominator) {
        BigDecimal divisor = toBigDecimal(denominator);
        if (divisor == null || divisor.compareTo(ZERO) == 0) {
            return ZERO.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
        }
        return decimalOrZero(numerator).multiply(THOUSAND).divide(divisor, DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 将比例值转换为基点，1 个基点等于 0.01%。
     *
     * @param value 比例值
     * @return 基点数
     */
    public static BigDecimal basisPoint(Number value) {
        return decimalOrZero(value).multiply(new BigDecimal("10000")).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * KB 转字节。
     *
     * @param kb KB 数值
     * @return 字节数
     */
    public static Long kbToBytes(Number kb) {
        return bytesFromUnit(kb, 1);
    }

    /**
     * MB 转字节。
     *
     * @param mb MB 数值
     * @return 字节数
     */
    public static Long mbToBytes(Number mb) {
        return bytesFromUnit(mb, 2);
    }

    /**
     * GB 转字节。
     *
     * @param gb GB 数值
     * @return 字节数
     */
    public static Long gbToBytes(Number gb) {
        return bytesFromUnit(gb, 3);
    }

    /**
     * 字节转 KB。
     *
     * @param bytes 字节数
     * @return KB 数值
     */
    public static BigDecimal bytesToKb(Number bytes) {
        return bytesToUnit(bytes, 1);
    }

    /**
     * 字节转 MB。
     *
     * @param bytes 字节数
     * @return MB 数值
     */
    public static BigDecimal bytesToMb(Number bytes) {
        return bytesToUnit(bytes, 2);
    }

    /**
     * 字节转 GB。
     *
     * @param bytes 字节数
     * @return GB 数值
     */
    public static BigDecimal bytesToGb(Number bytes) {
        return bytesToUnit(bytes, 3);
    }

    /**
     * 格式化字节大小。
     *
     * @param bytes 字节数
     * @return 文件大小文本
     */
    public static String formatBytes(Number bytes) {
        BigDecimal value = decimalOrZero(bytes);
        if (value.compareTo(BYTE_UNIT) < 0) {
            return value.setScale(0, DEFAULT_ROUNDING_MODE).toPlainString() + " B";
        }
        String[] units = {"KB", "MB", "GB", "TB"};
        BigDecimal current = value;
        int index = -1;
        do {
            current = current.divide(BYTE_UNIT, DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
            index++;
        } while (current.compareTo(BYTE_UNIT) >= 0 && index < units.length - 1);
        return removeTrailingZeros(current) + " " + units[index];
    }

    /**
     * 获取安全页码，页码为空或小于 1 时返回 1。
     *
     * @param pageNum 页码
     * @return 安全页码
     */
    public static Integer safePageNum(Integer pageNum) {
        return pageNum == null || pageNum < MIN_PAGE_NUM ? MIN_PAGE_NUM : pageNum;
    }

    /**
     * 获取安全分页大小，默认最大值为 500。
     *
     * @param pageSize 分页大小
     * @return 安全分页大小
     */
    public static Integer safePageSize(Integer pageSize) {
        return safePageSize(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 获取安全分页大小，指定最大分页大小。
     *
     * @param pageSize 分页大小
     * @param maxPageSize 最大分页大小
     * @return 安全分页大小
     */
    public static Integer safePageSize(Integer pageSize, Integer maxPageSize) {
        int max = maxPageSize == null || maxPageSize < 1 ? MAX_PAGE_SIZE : maxPageSize;
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, max);
    }

    /**
     * 计算分页偏移量。
     *
     * @param pageNum 页码
     * @param pageSize 分页大小
     * @return 偏移量
     */
    public static Long offset(Integer pageNum, Integer pageSize) {
        int safePageNum = safePageNum(pageNum);
        int safePageSize = safePageSize(pageSize);
        return (long) (safePageNum - 1) * safePageSize;
    }

    /**
     * 计算总页数。
     *
     * @param total 总条数
     * @param pageSize 分页大小
     * @return 总页数
     */
    public static Integer totalPage(Long total, Integer pageSize) {
        long safeTotal = total == null || total < 0 ? 0 : total;
        int safePageSize = safePageSize(pageSize);
        if (safeTotal == 0) {
            return 0;
        }
        return Math.toIntExact((safeTotal + safePageSize - 1) / safePageSize);
    }

    /**
     * 判断是否存在下一页。
     *
     * @param pageNum 页码
     * @param pageSize 分页大小
     * @param total 总条数
     * @return 存在下一页返回 true
     */
    public static boolean hasNextPage(Integer pageNum, Integer pageSize, Long total) {
        return safePageNum(pageNum) < totalPage(total, pageSize);
    }

    /**
     * 获取安全数量，空值或负数返回 0。
     *
     * @param quantity 数量
     * @return 安全数量
     */
    public static BigDecimal safeQuantity(Number quantity) {
        BigDecimal decimal = toBigDecimal(quantity);
        return decimal == null || decimal.signum() < 0 ? ZERO : decimal;
    }

    /**
     * 校验数量是否合法，要求非空且大于等于 0。
     *
     * @param quantity 数量
     * @return 合法返回 true
     */
    public static boolean checkQuantity(Number quantity) {
        return isPositiveOrZero(quantity);
    }

    /**
     * 限制数量上限，数量为空或负数按 0 处理。
     *
     * @param quantity 数量
     * @param max 最大值
     * @return 限制后的数量
     */
    public static BigDecimal limitQuantity(Number quantity, Number max) {
        BigDecimal maxValue = requireDecimal(max, "max");
        if (maxValue.signum() < 0) {
            throw new IllegalArgumentException("最大数量不能小于0");
        }
        BigDecimal safeQuantity = safeQuantity(quantity);
        return safeQuantity.compareTo(maxValue) > 0 ? maxValue : safeQuantity;
    }

    /**
     * 判断数字是否为 2 的幂。
     *
     * @param value 数字
     * @return 是 2 的幂返回 true
     */
    public static boolean isPowerOfTwo(long value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    /**
     * 获取大于等于当前值的下一个 2 的幂。
     *
     * @param value 数字
     * @return 2 的幂
     */
    public static long nextPowerOfTwo(long value) {
        if (value <= 0) {
            return 1L;
        }
        if (value > (1L << 62)) {
            throw new IllegalArgumentException("数值过大，无法计算下一个2的幂");
        }
        long highest = Long.highestOneBit(value);
        return value == highest ? value : highest << 1;
    }

    /**
     * 统计 long 数字二进制中 1 的数量。
     *
     * @param value 数字
     * @return bit 数量
     */
    public static int bitCount(long value) {
        return Long.bitCount(value);
    }

    /**
     * 判断指定 bit 位是否为 1。
     *
     * @param value 数字
     * @param bitIndex bit 下标，范围 0 到 63
     * @return 指定 bit 位为 1 返回 true
     */
    public static boolean hasBit(long value, int bitIndex) {
        checkBitIndex(bitIndex);
        return (value & (1L << bitIndex)) != 0;
    }

    /**
     * 将指定 bit 位设置为 1。
     *
     * @param value 数字
     * @param bitIndex bit 下标，范围 0 到 63
     * @return 处理后的数字
     */
    public static long setBit(long value, int bitIndex) {
        checkBitIndex(bitIndex);
        return value | (1L << bitIndex);
    }

    /**
     * 将指定 bit 位设置为 0。
     *
     * @param value 数字
     * @param bitIndex bit 下标，范围 0 到 63
     * @return 处理后的数字
     */
    public static long clearBit(long value, int bitIndex) {
        checkBitIndex(bitIndex);
        return value & ~(1L << bitIndex);
    }

    /**
     * 反转指定 bit 位。
     *
     * @param value 数字
     * @param bitIndex bit 下标，范围 0 到 63
     * @return 处理后的数字
     */
    public static long toggleBit(long value, int bitIndex) {
        checkBitIndex(bitIndex);
        return value ^ (1L << bitIndex);
    }

    /**
     * 将数字转换为二进制字符串。
     *
     * @param value 数字
     * @return 二进制字符串
     */
    public static String toBinaryString(Number value) {
        Long number = toLong(value);
        return number == null ? "" : Long.toBinaryString(number);
    }

    /**
     * 将数字转换为十六进制字符串。
     *
     * @param value 数字
     * @return 十六进制字符串
     */
    public static String toHexString(Number value) {
        Long number = toLong(value);
        return number == null ? "" : Long.toHexString(number);
    }

    private static BigDecimal parseDecimal(String value, BigDecimal defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = cleanNumber(value);
        if (text.isBlank()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static BigDecimal decimalOrZero(Number value) {
        return toBigDecimal(value, ZERO);
    }

    private static BigDecimal requireDecimal(Number value, String name) {
        BigDecimal decimal = toBigDecimal(value);
        if (decimal == null) {
            throw new IllegalArgumentException(name + "不能为空或非法数字");
        }
        return decimal;
    }

    private static BigDecimal requireNonZeroDecimal(Number value, String message) {
        BigDecimal decimal = requireDecimal(value, "value");
        if (decimal.compareTo(ZERO) == 0) {
            throw new ArithmeticException(message);
        }
        return decimal;
    }

    private static void validateMinMax(Number min, Number max) {
        BigDecimal minValue = requireDecimal(min, "min");
        BigDecimal maxValue = requireDecimal(max, "max");
        if (minValue.compareTo(maxValue) > 0) {
            throw new IllegalArgumentException("最小值不能大于最大值");
        }
    }

    private static void checkScale(int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("小数位不能小于0");
        }
    }

    private static String moneyPattern(int scale) {
        if (scale == 0) {
            return "#,##0";
        }
        return "#,##0." + "0".repeat(scale);
    }

    private static List<BigDecimal> validDecimals(Collection<? extends Number> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        List<BigDecimal> result = new ArrayList<>();
        for (Number value : values) {
            BigDecimal decimal = toBigDecimal(value);
            if (decimal != null) {
                result.add(decimal);
            }
        }
        return result;
    }

    private static long countBy(Collection<? extends Number> values, java.util.function.Predicate<Number> predicate) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        long count = 0L;
        for (Number value : values) {
            if (predicate.test(value)) {
                count++;
            }
        }
        return count;
    }

    private static List<BigDecimal> filterBy(Collection<? extends Number> values, java.util.function.Predicate<Number> predicate) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<BigDecimal> result = new ArrayList<>();
        for (Number value : values) {
            if (predicate.test(value)) {
                result.add(toBigDecimal(value));
            }
        }
        return result;
    }

    private static void checkRangeSize(long size) {
        if (size > MAX_RANGE_SIZE) {
            throw new IllegalArgumentException("区间长度不能超过" + MAX_RANGE_SIZE);
        }
    }

    private static String randomDigits(int length, boolean secure) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int digit = secure ? SECURE_RANDOM.nextInt(10) : ThreadLocalRandom.current().nextInt(10);
            builder.append(digit);
        }
        return builder.toString();
    }

    private static Long bytesFromUnit(Number value, int power) {
        BigDecimal result = decimalOrZero(value);
        for (int i = 0; i < power; i++) {
            result = result.multiply(BYTE_UNIT);
        }
        return result.setScale(0, DEFAULT_ROUNDING_MODE).longValueExact();
    }

    private static BigDecimal bytesToUnit(Number value, int power) {
        BigDecimal result = decimalOrZero(value);
        for (int i = 0; i < power; i++) {
            result = result.divide(BYTE_UNIT, DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
        }
        return result;
    }

    private static void checkBitIndex(int bitIndex) {
        if (bitIndex < 0 || bitIndex > 63) {
            throw new IllegalArgumentException("bit 下标范围必须在0到63之间");
        }
    }
}
