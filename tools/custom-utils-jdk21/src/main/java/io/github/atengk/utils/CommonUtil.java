package io.github.atengk.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.*;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import cn.hutool.json.*;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.crypto.KeyGenerator;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 通用基础工具类（基于 Hutool 工具库）
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class CommonUtil {

    private static final String EMPTY = "";
    private static final String ELLIPSIS = "...";
    private static final String RANDOM_NUMBERS = "0123456789";
    private static final String RANDOM_LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String RANDOM_LETTERS_NUMBERS = RANDOM_LETTERS + RANDOM_NUMBERS;

    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String TIME_PATTERN = "HH:mm:ss";
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_TIME_MILLI_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
    private static final DateTimeFormatter DATE_TIME_MILLI_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_MILLI_PATTERN);

    private static final int DEFAULT_AMOUNT_SCALE = 2;
    private static final int DEFAULT_PERCENT_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final int DEFAULT_IO_BUFFER_SIZE = 8192;

    private static final int AES_KEY_128_LENGTH = 16;
    private static final int AES_KEY_192_LENGTH = 24;
    private static final int AES_KEY_256_LENGTH = 32;

    private static final String SPRING_PROFILES_ACTIVE_PROPERTY = "spring.profiles.active";
    private static final String SPRING_PROFILES_DEFAULT_PROPERTY = "spring.profiles.default";
    private static final String SPRING_PROFILES_ACTIVE_ENV = "SPRING_PROFILES_ACTIVE";
    private static final String SPRING_PROFILES_DEFAULT_ENV = "SPRING_PROFILES_DEFAULT";
    private static final long BYTES_PER_KB = 1024L;
    private static final long BYTES_PER_MB = BYTES_PER_KB * 1024L;
    private static final long BYTES_PER_GB = BYTES_PER_MB * 1024L;

    /**
     * 复用 Snowflake 实例，避免频繁创建带来的性能开销。
     */
    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake();

    /**
     * 私有构造方法，禁止实例化工具类。
     */
    private CommonUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ============================== 字符串判空 ==============================

    /**
     * 判断字符串是否为空白。
     *
     * @param value 字符串
     * @return 是否为空白
     */
    public static boolean isBlank(CharSequence value) {
        return StrUtil.isBlank(value);
    }

    /**
     * 判断字符串是否非空白。
     *
     * @param value 字符串
     * @return 是否非空白
     */
    public static boolean isNotBlank(CharSequence value) {
        return StrUtil.isNotBlank(value);
    }

    /**
     * 判断字符串是否为空。
     *
     * @param value 字符串
     * @return 是否为空
     */
    public static boolean isEmpty(CharSequence value) {
        return StrUtil.isEmpty(value);
    }

    /**
     * 判断字符串是否非空。
     *
     * @param value 字符串
     * @return 是否非空
     */
    public static boolean isNotEmpty(CharSequence value) {
        return StrUtil.isNotEmpty(value);
    }

    /**
     * 判断多个字符串中是否存在空白字符串。
     *
     * @param values 字符串数组
     * @return 是否存在空白字符串
     */
    public static boolean isAnyBlank(CharSequence... values) {
        if (values == null || values.length == 0) {
            return true;
        }
        for (CharSequence value : values) {
            if (StrUtil.isBlank(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断多个字符串是否全部为空白。
     *
     * @param values 字符串数组
     * @return 是否全部为空白
     */
    public static boolean isAllBlank(CharSequence... values) {
        if (values == null || values.length == 0) {
            return true;
        }
        for (CharSequence value : values) {
            if (StrUtil.isNotBlank(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断多个字符串中是否存在空字符串。
     *
     * @param values 字符串数组
     * @return 是否存在空字符串
     */
    public static boolean isAnyEmpty(CharSequence... values) {
        if (values == null || values.length == 0) {
            return true;
        }
        for (CharSequence value : values) {
            if (StrUtil.isEmpty(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断多个字符串是否全部为空字符串。
     *
     * @param values 字符串数组
     * @return 是否全部为空字符串
     */
    public static boolean isAllEmpty(CharSequence... values) {
        if (values == null || values.length == 0) {
            return true;
        }
        for (CharSequence value : values) {
            if (StrUtil.isNotEmpty(value)) {
                return false;
            }
        }
        return true;
    }

    // ============================== 默认值处理 ==============================

    /**
     * 将 null 字符串转换为空字符串。
     *
     * @param value 字符串
     * @return 非 null 字符串
     */
    public static String nullToEmpty(String value) {
        return value == null ? EMPTY : value;
    }

    /**
     * 将空字符串转换为 null。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String emptyToNull(String value) {
        return StrUtil.isEmpty(value) ? null : value;
    }

    /**
     * 将空白字符串转换为 null。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : value;
    }

    /**
     * 当字符串为 null 时返回默认值。
     *
     * @param value        字符串
     * @param defaultValue 默认值
     * @return 原字符串或默认值
     */
    public static String defaultIfNull(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * 当字符串为空时返回默认值。
     *
     * @param value        字符串
     * @param defaultValue 默认值
     * @return 原字符串或默认值
     */
    public static String defaultIfEmpty(String value, String defaultValue) {
        return StrUtil.isEmpty(value) ? defaultValue : value;
    }

    /**
     * 当字符串为空白时返回默认值。
     *
     * @param value        字符串
     * @param defaultValue 默认值
     * @return 原字符串或默认值
     */
    public static String defaultIfBlank(String value, String defaultValue) {
        return StrUtil.isBlank(value) ? defaultValue : value;
    }

    /**
     * 返回第一个非空白字符串。
     *
     * @param values 字符串数组
     * @return 第一个非空白字符串
     */
    public static String firstNotBlank(String... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 返回第一个非空字符串。
     *
     * @param values 字符串数组
     * @return 第一个非空字符串
     */
    public static String firstNotEmpty(String... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    // ============================== 空格与换行处理 ==============================

    /**
     * 去除字符串首尾空白。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String trim(String value) {
        return value == null ? null : StrUtil.trim(value);
    }

    /**
     * 去除字符串首尾空白，并将 null 转为空字符串。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String trimToEmpty(String value) {
        return value == null ? EMPTY : StrUtil.trim(value);
    }

    /**
     * 去除字符串首尾空白，并将空白结果转为 null。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String trimToNull(String value) {
        String trimValue = trim(value);
        return StrUtil.isBlank(trimValue) ? null : trimValue;
    }

    /**
     * 清除字符串中的所有空白字符。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String cleanBlank(String value) {
        return value == null ? null : StrUtil.cleanBlank(value);
    }

    /**
     * 清除字符串中的所有空白字符，并将 null 转为空字符串。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String cleanBlankToEmpty(String value) {
        return value == null ? EMPTY : StrUtil.cleanBlank(value);
    }

    /**
     * 移除字符串中的换行符。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String removeLineBreak(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\r", EMPTY).replace("\n", EMPTY);
    }

    /**
     * 移除字符串中的换行符，并将 null 转为空字符串。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String removeLineBreakToEmpty(String value) {
        return value == null ? EMPTY : removeLineBreak(value);
    }

    /**
     * 规范化空白字符，将连续空白压缩为单个空格。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String normalizeBlank(String value) {
        if (StrUtil.isBlank(value)) {
            return EMPTY;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    // ============================== 字符串比较 ==============================

    /**
     * 判断两个字符串是否相等。
     *
     * @param first  第一个字符串
     * @param second 第二个字符串
     * @return 是否相等
     */
    public static boolean equals(String first, String second) {
        return Objects.equals(first, second);
    }

    /**
     * 忽略大小写判断两个字符串是否相等。
     *
     * @param first  第一个字符串
     * @param second 第二个字符串
     * @return 是否相等
     */
    public static boolean equalsIgnoreCase(String first, String second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.equalsIgnoreCase(second);
    }

    /**
     * 判断字符串是否包含指定关键字。
     *
     * @param value   字符串
     * @param keyword 关键字
     * @return 是否包含
     */
    public static boolean contains(String value, String keyword) {
        if (value == null || keyword == null) {
            return false;
        }
        return value.contains(keyword);
    }

    /**
     * 忽略大小写判断字符串是否包含指定关键字。
     *
     * @param value   字符串
     * @param keyword 关键字
     * @return 是否包含
     */
    public static boolean containsIgnoreCase(String value, String keyword) {
        if (value == null || keyword == null) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    /**
     * 判断字符串是否以指定前缀开头。
     *
     * @param value  字符串
     * @param prefix 前缀
     * @return 是否以指定前缀开头
     */
    public static boolean startsWith(String value, String prefix) {
        if (value == null || prefix == null) {
            return false;
        }
        return value.startsWith(prefix);
    }

    /**
     * 判断字符串是否以指定后缀结尾。
     *
     * @param value  字符串
     * @param suffix 后缀
     * @return 是否以指定后缀结尾
     */
    public static boolean endsWith(String value, String suffix) {
        if (value == null || suffix == null) {
            return false;
        }
        return value.endsWith(suffix);
    }

    // ============================== 字符串截取 ==============================

    /**
     * 安全截取字符串。
     *
     * @param value      字符串
     * @param startIndex 开始索引
     * @param endIndex   结束索引
     * @return 截取后的字符串
     */
    public static String sub(String value, int startIndex, int endIndex) {
        if (value == null) {
            return null;
        }
        int length = value.length();
        int start = Math.max(0, startIndex);
        int end = Math.min(length, endIndex);
        if (start >= end) {
            return EMPTY;
        }
        return value.substring(start, end);
    }

    /**
     * 从左侧截取指定长度字符串。
     *
     * @param value  字符串
     * @param length 截取长度
     * @return 截取后的字符串
     */
    public static String left(String value, int length) {
        if (value == null) {
            return null;
        }
        if (length <= 0) {
            return EMPTY;
        }
        return value.length() <= length ? value : value.substring(0, length);
    }

    /**
     * 从右侧截取指定长度字符串。
     *
     * @param value  字符串
     * @param length 截取长度
     * @return 截取后的字符串
     */
    public static String right(String value, int length) {
        if (value == null) {
            return null;
        }
        if (length <= 0) {
            return EMPTY;
        }
        int valueLength = value.length();
        return valueLength <= length ? value : value.substring(valueLength - length);
    }

    /**
     * 按最大长度截断字符串。
     *
     * @param value     字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (maxLength <= 0) {
            return EMPTY;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * 按最大长度截断字符串，并在末尾追加省略号。
     *
     * @param value     字符串
     * @param maxLength 最大长度
     * @return 处理后的字符串
     */
    public static String ellipsis(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (maxLength <= 0) {
            return EMPTY;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        if (maxLength <= ELLIPSIS.length()) {
            return value.substring(0, maxLength);
        }
        return value.substring(0, maxLength - ELLIPSIS.length()) + ELLIPSIS;
    }

    // ============================== 前后缀处理 ==============================

    /**
     * 当字符串不存在指定前缀时追加前缀。
     *
     * @param value  字符串
     * @param prefix 前缀
     * @return 处理后的字符串
     */
    public static String addPrefixIfAbsent(String value, String prefix) {
        if (value == null) {
            return null;
        }
        if (StrUtil.isEmpty(prefix) || value.startsWith(prefix)) {
            return value;
        }
        return prefix + value;
    }

    /**
     * 当字符串不存在指定后缀时追加后缀。
     *
     * @param value  字符串
     * @param suffix 后缀
     * @return 处理后的字符串
     */
    public static String addSuffixIfAbsent(String value, String suffix) {
        if (value == null) {
            return null;
        }
        if (StrUtil.isEmpty(suffix) || value.endsWith(suffix)) {
            return value;
        }
        return value + suffix;
    }

    /**
     * 移除字符串指定前缀。
     *
     * @param value  字符串
     * @param prefix 前缀
     * @return 处理后的字符串
     */
    public static String removePrefix(String value, String prefix) {
        if (value == null || StrUtil.isEmpty(prefix)) {
            return value;
        }
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    /**
     * 移除字符串指定后缀。
     *
     * @param value  字符串
     * @param suffix 后缀
     * @return 处理后的字符串
     */
    public static String removeSuffix(String value, String suffix) {
        if (value == null || StrUtil.isEmpty(suffix)) {
            return value;
        }
        return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
    }

    /**
     * 确保字符串以指定前缀开头。
     *
     * @param value  字符串
     * @param prefix 前缀
     * @return 处理后的字符串
     */
    public static String ensureStartWith(String value, String prefix) {
        return addPrefixIfAbsent(value, prefix);
    }

    /**
     * 确保字符串以指定后缀结尾。
     *
     * @param value  字符串
     * @param suffix 后缀
     * @return 处理后的字符串
     */
    public static String ensureEndWith(String value, String suffix) {
        return addSuffixIfAbsent(value, suffix);
    }

    // ============================== 大小写与命名转换 ==============================

    /**
     * 将字符串首字母转为大写。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String upperFirst(String value) {
        return value == null ? null : StrUtil.upperFirst(value);
    }

    /**
     * 将字符串首字母转为小写。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String lowerFirst(String value) {
        return value == null ? null : StrUtil.lowerFirst(value);
    }

    /**
     * 将字符串转换为驼峰格式。
     *
     * @param value 字符串
     * @return 驼峰格式字符串
     */
    public static String toCamelCase(String value) {
        return value == null ? null : StrUtil.toCamelCase(value);
    }

    /**
     * 将字符串转换为下划线格式。
     *
     * @param value 字符串
     * @return 下划线格式字符串
     */
    public static String toUnderlineCase(String value) {
        return value == null ? null : StrUtil.toUnderlineCase(value);
    }

    /**
     * 将字符串转换为大写。
     *
     * @param value 字符串
     * @return 大写字符串
     */
    public static String toUpperCase(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    /**
     * 将字符串转换为小写。
     *
     * @param value 字符串
     * @return 小写字符串
     */
    public static String toLowerCase(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    // ============================== 格式化与占位符 ==============================

    /**
     * 使用 Hutool 占位符格式化字符串。
     *
     * @param template 字符串模板
     * @param values   参数数组
     * @return 格式化后的字符串
     */
    public static String format(String template, Object... values) {
        if (template == null) {
            return null;
        }
        return StrUtil.format(template, values);
    }

    /**
     * 使用 Map 参数替换模板中的命名占位符。
     *
     * @param template 字符串模板
     * @param params   参数 Map
     * @return 格式化后的字符串
     */
    public static String formatByMap(String template, Map<String, ?> params) {
        if (template == null) {
            return null;
        }
        if (params == null || params.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            String key = entry.getKey();
            if (StrUtil.isBlank(key)) {
                continue;
            }

            String value = Objects.toString(entry.getValue(), EMPTY);
            result = result.replace("{" + key + "}", value);
            result = result.replace("${" + key + "}", value);
        }
        return result;
    }

    /**
     * 使用指定分隔符拼接集合元素。
     *
     * @param delimiter 分隔符
     * @param values    集合元素
     * @return 拼接后的字符串
     */
    public static String join(CharSequence delimiter, Iterable<?> values) {
        if (values == null) {
            return EMPTY;
        }

        List<String> list = new ArrayList<>();
        for (Object value : values) {
            if (value != null) {
                list.add(value.toString());
            }
        }
        return String.join(delimiter == null ? EMPTY : delimiter, list);
    }

    /**
     * 使用指定分隔符拼接集合中的非空白元素。
     *
     * @param delimiter 分隔符
     * @param values    集合元素
     * @return 拼接后的字符串
     */
    public static String joinIgnoreBlank(CharSequence delimiter, Iterable<?> values) {
        if (values == null) {
            return EMPTY;
        }

        List<String> list = new ArrayList<>();
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String str = value.toString();
            if (StrUtil.isNotBlank(str)) {
                list.add(str);
            }
        }
        return String.join(delimiter == null ? EMPTY : delimiter, list);
    }

    // ============================== 字符串拆分 ==============================

    /**
     * 使用指定分隔符拆分字符串。
     *
     * @param value     字符串
     * @param separator 分隔符
     * @return 拆分后的字符串集合
     */
    public static List<String> split(String value, String separator) {
        List<String> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        if (StrUtil.isEmpty(separator)) {
            result.add(value);
            return result;
        }

        String[] values = value.split(Pattern.quote(separator), -1);
        for (String item : values) {
            result.add(item);
        }
        return result;
    }

    /**
     * 使用指定分隔符拆分字符串，并去除每项首尾空白。
     *
     * @param value     字符串
     * @param separator 分隔符
     * @return 拆分后的字符串集合
     */
    public static List<String> splitTrim(String value, String separator) {
        List<String> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        if (StrUtil.isEmpty(separator)) {
            result.add(StrUtil.trim(value));
            return result;
        }

        String[] values = value.split(Pattern.quote(separator), -1);
        for (String item : values) {
            result.add(StrUtil.trim(item));
        }
        return result;
    }

    /**
     * 使用指定分隔符拆分字符串，去除每项首尾空白并忽略空白项。
     *
     * @param value     字符串
     * @param separator 分隔符
     * @return 拆分后的字符串集合
     */
    public static List<String> splitTrimIgnoreBlank(String value, String separator) {
        List<String> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        if (StrUtil.isEmpty(separator)) {
            String trimValue = StrUtil.trim(value);
            if (StrUtil.isNotBlank(trimValue)) {
                result.add(trimValue);
            }
            return result;
        }

        String[] values = value.split(Pattern.quote(separator), -1);
        for (String item : values) {
            String trimValue = StrUtil.trim(item);
            if (StrUtil.isNotBlank(trimValue)) {
                result.add(trimValue);
            }
        }
        return result;
    }

    // ============================== 字符串校验 ==============================

    /**
     * 判断字符串是否为数字。
     *
     * @param value 字符串
     * @return 是否为数字
     */
    public static boolean isNumber(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^-?\\d+(\\.\\d+)?$", value);
    }

    /**
     * 判断字符串是否为整数。
     *
     * @param value 字符串
     * @return 是否为整数
     */
    public static boolean isInteger(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^-?\\d+$", value);
    }

    /**
     * 判断字符串是否为正整数。
     *
     * @param value 字符串
     * @return 是否为正整数
     */
    public static boolean isPositiveInteger(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^[1-9]\\d*$", value);
    }

    /**
     * 判断字符串是否全部由数字组成。
     *
     * @param value 字符串
     * @return 是否全部由数字组成
     */
    public static boolean isDigits(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^\\d+$", value);
    }

    /**
     * 判断字符串是否全部由英文字母组成。
     *
     * @param value 字符串
     * @return 是否全部由英文字母组成
     */
    public static boolean isLetters(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^[a-zA-Z]+$", value);
    }

    /**
     * 判断字符串是否全部由英文字母或数字组成。
     *
     * @param value 字符串
     * @return 是否全部由英文字母或数字组成
     */
    public static boolean isLettersOrNumbers(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^[a-zA-Z0-9]+$", value);
    }

    /**
     * 判断字符串是否包含中文字符。
     *
     * @param value 字符串
     * @return 是否包含中文字符
     */
    public static boolean containsChinese(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.contains("[\\u4e00-\\u9fa5]", value);
    }

    /**
     * 判断字符串是否全部由中文字符组成。
     *
     * @param value 字符串
     * @return 是否全部由中文字符组成
     */
    public static boolean isChinese(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^[\\u4e00-\\u9fa5]+$", value);
    }

    // ============================== 字符串清理 ==============================

    /**
     * 移除字符串中的特殊字符，仅保留中文、英文字母、数字、下划线和中划线。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String removeSpecialChar(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9_\\-]", EMPTY);
    }

    /**
     * 仅保留字符串中的数字。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String retainNumber(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\D", EMPTY);
    }

    /**
     * 仅保留字符串中的英文字母。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String retainLetter(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[^a-zA-Z]", EMPTY);
    }

    /**
     * 仅保留字符串中的中文字符。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String retainChinese(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[^\\u4e00-\\u9fa5]", EMPTY);
    }

    /**
     * 仅保留字符串中的中文、英文字母和数字。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String retainChineseLetterNumber(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", EMPTY);
    }

    // ============================== 脱敏处理 ==============================

    /**
     * 对字符串进行通用脱敏处理。
     *
     * @param value        字符串
     * @param prefixLength 保留前缀长度
     * @param suffixLength 保留后缀长度
     * @return 脱敏后的字符串
     */
    public static String mask(String value, int prefixLength, int suffixLength) {
        return mask(value, prefixLength, suffixLength, '*');
    }

    /**
     * 使用指定掩码字符对字符串进行通用脱敏处理。
     *
     * @param value        字符串
     * @param prefixLength 保留前缀长度
     * @param suffixLength 保留后缀长度
     * @param maskChar     掩码字符
     * @return 脱敏后的字符串
     */
    public static String mask(String value, int prefixLength, int suffixLength, char maskChar) {
        if (value == null) {
            return null;
        }

        int length = value.length();
        int prefix = Math.max(0, prefixLength);
        int suffix = Math.max(0, suffixLength);

        if (prefix + suffix >= length) {
            return String.valueOf(maskChar).repeat(length);
        }

        String prefixValue = value.substring(0, prefix);
        String suffixValue = suffix == 0 ? EMPTY : value.substring(length - suffix);
        String maskValue = String.valueOf(maskChar).repeat(length - prefix - suffix);
        return prefixValue + maskValue + suffixValue;
    }

    /**
     * 对手机号进行脱敏处理。
     *
     * @param mobile 手机号
     * @return 脱敏后的手机号
     */
    public static String desensitizedMobile(String mobile) {
        return StrUtil.isBlank(mobile) ? EMPTY : DesensitizedUtil.mobilePhone(mobile);
    }

    /**
     * 对邮箱进行脱敏处理。
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String desensitizedEmail(String email) {
        return StrUtil.isBlank(email) ? EMPTY : DesensitizedUtil.email(email);
    }

    /**
     * 对中文姓名进行脱敏处理。
     *
     * @param name 中文姓名
     * @return 脱敏后的中文姓名
     */
    public static String desensitizedChineseName(String name) {
        return StrUtil.isBlank(name) ? EMPTY : DesensitizedUtil.chineseName(name);
    }

    /**
     * 对身份证号进行脱敏处理。
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String desensitizedIdCard(String idCard) {
        return StrUtil.isBlank(idCard) ? EMPTY : DesensitizedUtil.idCardNum(idCard, 6, 4);
    }

    /**
     * 对银行卡号进行脱敏处理。
     *
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String desensitizedBankCard(String bankCard) {
        return StrUtil.isBlank(bankCard) ? EMPTY : DesensitizedUtil.bankCard(bankCard);
    }

    // ============================== 雪花ID ==============================

    /**
     * 高性能雪花 ID（long 类型）。
     *
     * @return 全局唯一 ID
     */
    public static long snowflakeIdFast() {
        return SNOWFLAKE.nextId();
    }

    /**
     * 高性能雪花 ID（String 类型）。
     *
     * @return 全局唯一 ID（字符串）
     */
    public static String snowflakeIdStrFast() {
        return SNOWFLAKE.nextIdStr();
    }

    /**
     * 高性能雪花 ID（Mac版本 long 类型）。
     *
     * 基于本机 Mac 信息自动生成 workerId，适用于本地开发和单机服务。
     *
     * @return 全局唯一 ID
     * @author Ateng
     * @since 2026-06-13
     */
    public static long snowflakeIdFastMac() {
        return macSnowflake().nextId();
    }

    /**
     * 高性能雪花 ID（Mac版本 String 类型）。
     *
     * 基于本机 Mac 信息自动生成 workerId，适用于本地开发和单机服务。
     *
     * @return 全局唯一 ID（字符串）
     * @author Ateng
     * @since 2026-06-13
     */
    public static String snowflakeIdStrFastMac() {
        return macSnowflake().nextIdStr();
    }

    /**
     * 基于 Mac 信息生成 Snowflake 实例（自动推导 workerId）
     * <p>
     * 适用于本地开发、测试环境、小规模单机部署场景。
     * workerId 通过 MAC 地址 / 主机名 / IP hash 生成，并限制在 0~31。
     *
     * @return Snowflake 实例
     */
    public static Snowflake macSnowflake() {

        StringBuilder sb = new StringBuilder();

        String mac = NetUtil.getLocalMacAddress();
        if (ObjectUtil.isNotEmpty(mac)) {
            sb.append(mac);
        }

        String hostName = NetUtil.getLocalHostName();
        if (ObjectUtil.isNotEmpty(hostName)) {
            sb.append(hostName);
        }

        String ip = NetUtil.getLocalhostStr();
        if (ObjectUtil.isNotEmpty(ip)) {
            sb.append(ip);
        }

        int workerId = Math.abs(sb.toString().hashCode()) % 32;

        return IdUtil.getSnowflake(workerId, 1L);
    }

    // ============================== 随机字符串 ==============================

    /**
     * 生成带横线的 UUID。
     *
     * @return UUID 字符串
     */
    public static String uuid() {
        return IdUtil.fastUUID();
    }

    /**
     * 生成不带横线的 UUID。
     *
     * @return 简化 UUID 字符串
     */
    public static String simpleUuid() {
        return IdUtil.fastSimpleUUID();
    }

    /**
     * 生成指定长度的随机数字字符串。
     *
     * @param length 长度
     * @return 随机数字字符串
     */
    public static String randomNumbers(int length) {
        return randomString(RANDOM_NUMBERS, length);
    }

    /**
     * 生成指定长度的随机字母字符串。
     *
     * @param length 长度
     * @return 随机字母字符串
     */
    public static String randomLetters(int length) {
        return randomString(RANDOM_LETTERS, length);
    }

    /**
     * 生成指定长度的随机字母数字字符串。
     *
     * @param length 长度
     * @return 随机字母数字字符串
     */
    public static String randomLettersNumbers(int length) {
        return randomString(RANDOM_LETTERS_NUMBERS, length);
    }

    /**
     * 基于指定字符池生成随机字符串。
     *
     * @param baseString 字符池
     * @param length     长度
     * @return 随机字符串
     */
    public static String randomString(String baseString, int length) {
        if (length <= 0) {
            return EMPTY;
        }
        if (StrUtil.isEmpty(baseString)) {
            return EMPTY;
        }

        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RandomUtil.randomInt(0, baseString.length());
            builder.append(baseString.charAt(index));
        }
        return builder.toString();
    }

    // ============================== 对象判空 ==============================

    /**
     * 判断对象是否为 null。
     *
     * @param value 对象
     * @return 是否为 null
     */
    public static boolean isNull(Object value) {
        return ObjectUtil.isNull(value);
    }

    /**
     * 判断对象是否不为 null。
     *
     * @param value 对象
     * @return 是否不为 null
     */
    public static boolean isNotNull(Object value) {
        return ObjectUtil.isNotNull(value);
    }

    /**
     * 判断对象是否为空。
     *
     * @param value 对象
     * @return 是否为空
     */
    public static boolean isObjectEmpty(Object value) {
        return ObjectUtil.isEmpty(value);
    }

    /**
     * 判断对象是否非空。
     *
     * @param value 对象
     * @return 是否非空
     */
    public static boolean isObjectNotEmpty(Object value) {
        return ObjectUtil.isNotEmpty(value);
    }

    /**
     * 当对象为 null 时返回默认值。
     *
     * @param value        对象
     * @param defaultValue 默认值
     * @param <T>          对象类型
     * @return 原对象或默认值
     */
    public static <T> T defaultObjectIfNull(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * 当对象为空时返回默认值。
     *
     * @param value        对象
     * @param defaultValue 默认值
     * @param <T>          对象类型
     * @return 原对象或默认值
     */
    public static <T> T defaultObjectIfEmpty(T value, T defaultValue) {
        return ObjectUtil.isEmpty(value) ? defaultValue : value;
    }

    /**
     * 返回第一个非 null 对象。
     *
     * @param values 对象数组
     * @param <T>    对象类型
     * @return 第一个非 null 对象
     */
    @SafeVarargs
    public static <T> T firstNotNull(T... values) {
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
     * 判断目标对象是否等于任意一个候选对象。
     *
     * @param target 目标对象
     * @param values 候选对象数组
     * @return 是否匹配任意候选对象
     */
    public static boolean equalsAny(Object target, Object... values) {
        if (values == null || values.length == 0) {
            return false;
        }
        for (Object value : values) {
            if (Objects.equals(target, value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断目标对象是否不等于所有候选对象。
     *
     * @param target 目标对象
     * @param values 候选对象数组
     * @return 是否不匹配所有候选对象
     */
    public static boolean notEqualsAny(Object target, Object... values) {
        return !equalsAny(target, values);
    }

    // ============================== 类型转换 ==============================

    /**
     * 将对象转换为字符串。
     *
     * @param value 对象
     * @return 字符串
     */
    public static String toStr(Object value) {
        return Convert.toStr(value);
    }

    /**
     * 将对象转换为字符串，转换失败时返回默认值。
     *
     * @param value        对象
     * @param defaultValue 默认值
     * @return 字符串
     */
    public static String toStr(Object value, String defaultValue) {
        return Convert.toStr(value, defaultValue);
    }

    /**
     * 将对象转换为 Integer。
     *
     * @param value 对象
     * @return Integer 值
     */
    public static Integer toInt(Object value) {
        return Convert.toInt(value);
    }

    /**
     * 将对象转换为 Integer，转换失败时返回默认值。
     *
     * @param value        对象
     * @param defaultValue 默认值
     * @return Integer 值
     */
    public static Integer toInt(Object value, Integer defaultValue) {
        return Convert.toInt(value, defaultValue);
    }

    /**
     * 将对象转换为 Long。
     *
     * @param value 对象
     * @return Long 值
     */
    public static Long toLong(Object value) {
        return Convert.toLong(value);
    }

    /**
     * 将对象转换为 Long，转换失败时返回默认值。
     *
     * @param value        对象
     * @param defaultValue 默认值
     * @return Long 值
     */
    public static Long toLong(Object value, Long defaultValue) {
        return Convert.toLong(value, defaultValue);
    }

    /**
     * 将对象转换为 Double。
     *
     * @param value 对象
     * @return Double 值
     */
    public static Double toDouble(Object value) {
        return Convert.toDouble(value);
    }

    /**
     * 将对象转换为 Double，转换失败时返回默认值。
     *
     * @param value        对象
     * @param defaultValue 默认值
     * @return Double 值
     */
    public static Double toDouble(Object value, Double defaultValue) {
        return Convert.toDouble(value, defaultValue);
    }

    /**
     * 将对象转换为 Boolean。
     *
     * @param value 对象
     * @return Boolean 值
     */
    public static Boolean toBool(Object value) {
        return Convert.toBool(value);
    }

    /**
     * 将对象转换为 Boolean，转换失败时返回默认值。
     *
     * @param value        对象
     * @param defaultValue 默认值
     * @return Boolean 值
     */
    public static Boolean toBool(Object value, Boolean defaultValue) {
        return Convert.toBool(value, defaultValue);
    }

    /**
     * 将对象转换为 BigDecimal。
     *
     * @param value 对象
     * @return BigDecimal 值
     */
    public static BigDecimal toBigDecimal(Object value) {
        return Convert.toBigDecimal(value);
    }

    /**
     * 将对象转换为 BigDecimal，转换失败时返回默认值。
     *
     * @param value        对象
     * @param defaultValue 默认值
     * @return BigDecimal 值
     */
    public static BigDecimal toBigDecimal(Object value, BigDecimal defaultValue) {
        return Convert.toBigDecimal(value, defaultValue);
    }

    /**
     * 将对象转换为指定类型。
     *
     * @param value       对象
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 转换后的对象
     */
    public static <T> T convert(Object value, Class<T> targetClass) {
        if (value == null || targetClass == null) {
            return null;
        }
        return Convert.convert(targetClass, value);
    }

    /**
     * 将对象转换为指定类型，转换失败时返回默认值。
     *
     * @param value        对象
     * @param targetClass  目标类型
     * @param defaultValue 默认值
     * @param <T>          目标类型
     * @return 转换后的对象
     */
    public static <T> T convert(Object value, Class<T> targetClass, T defaultValue) {
        if (value == null || targetClass == null) {
            return defaultValue;
        }
        try {
            return Convert.convert(targetClass, value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 将集合元素批量转换为指定类型。
     *
     * @param values      原集合
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 转换后的集合
     */
    public static <T> List<T> convertList(Collection<?> values, Class<T> targetClass) {
        List<T> result = new ArrayList<>();
        if (values == null || values.isEmpty() || targetClass == null) {
            return result;
        }

        for (Object value : values) {
            result.add(convert(value, targetClass));
        }
        return result;
    }

    /**
     * 判断对象是否为指定类型。
     *
     * @param value       对象
     * @param targetClass 目标类型
     * @return 是否为指定类型
     */
    public static boolean isInstanceOf(Object value, Class<?> targetClass) {
        if (value == null || targetClass == null) {
            return false;
        }
        return targetClass.isInstance(value);
    }

    /**
     * 将对象安全转换为指定类型。
     *
     * @param value       对象
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 转换后的对象
     */
    public static <T> T safeCast(Object value, Class<T> targetClass) {
        if (value == null || targetClass == null || !targetClass.isInstance(value)) {
            return null;
        }
        return targetClass.cast(value);
    }

    /**
     * 将对象安全转换为指定类型，转换失败时返回默认值。
     *
     * @param value        对象
     * @param targetClass  目标类型
     * @param defaultValue 默认值
     * @param <T>          目标类型
     * @return 转换后的对象
     */
    public static <T> T safeCast(Object value, Class<T> targetClass, T defaultValue) {
        T result = safeCast(value, targetClass);
        return result == null ? defaultValue : result;
    }

    // ============================== Bean 复制 ==============================

    /**
     * 将源对象复制为指定类型的新对象。
     *
     * @param source      源对象
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 复制后的目标对象
     */
    public static <T> T copyBean(Object source, Class<T> targetClass) {
        if (source == null || targetClass == null) {
            return null;
        }
        return BeanUtil.copyProperties(source, targetClass);
    }

    /**
     * 将源对象复制为指定类型的新对象，并忽略指定属性。
     *
     * @param source           源对象
     * @param targetClass      目标类型
     * @param ignoreProperties 忽略属性数组
     * @param <T>              目标类型
     * @return 复制后的目标对象
     */
    public static <T> T copyBean(Object source, Class<T> targetClass, String... ignoreProperties) {
        if (source == null || targetClass == null) {
            return null;
        }
        return BeanUtil.copyProperties(source, targetClass, ignoreProperties);
    }

    /**
     * 将源对象复制到目标对象。
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyBean(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        BeanUtil.copyProperties(source, target);
    }

    /**
     * 将源对象复制到目标对象，并忽略指定属性。
     *
     * @param source           源对象
     * @param target           目标对象
     * @param ignoreProperties 忽略属性数组
     */
    public static void copyBean(Object source, Object target, String... ignoreProperties) {
        if (source == null || target == null) {
            return;
        }
        BeanUtil.copyProperties(source, target, ignoreProperties);
    }

    /**
     * 将源对象复制为指定类型的新对象，并忽略 null 属性。
     *
     * @param source      源对象
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 复制后的目标对象
     */
    public static <T> T copyBeanIgnoreNull(Object source, Class<T> targetClass) {
        return copyBeanIgnoreNull(source, targetClass, new String[0]);
    }

    /**
     * 将源对象复制为指定类型的新对象，忽略 null 属性和指定属性。
     *
     * @param source           源对象
     * @param targetClass      目标类型
     * @param ignoreProperties 忽略属性数组
     * @param <T>              目标类型
     * @return 复制后的目标对象
     */
    public static <T> T copyBeanIgnoreNull(Object source, Class<T> targetClass, String... ignoreProperties) {
        if (source == null || targetClass == null) {
            return null;
        }

        T target = newInstance(targetClass);
        copyBeanIgnoreNull(source, target, ignoreProperties);
        return target;
    }

    /**
     * 将源对象复制到目标对象，并忽略 null 属性。
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyBeanIgnoreNull(Object source, Object target) {
        copyBeanIgnoreNull(source, target, new String[0]);
    }

    /**
     * 将源对象复制到目标对象，忽略 null 属性和指定属性。
     *
     * @param source           源对象
     * @param target           目标对象
     * @param ignoreProperties 忽略属性数组
     */
    public static void copyBeanIgnoreNull(Object source, Object target, String... ignoreProperties) {
        if (source == null || target == null) {
            return;
        }

        CopyOptions copyOptions = CopyOptions.create()
                .setIgnoreNullValue(true)
                .setIgnoreError(true);

        if (ignoreProperties != null && ignoreProperties.length > 0) {
            copyOptions.setIgnoreProperties(ignoreProperties);
        }

        BeanUtil.copyProperties(source, target, copyOptions);
    }

    /**
     * 将源对象集合复制为指定类型的新集合。
     *
     * @param sources     源对象集合
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 复制后的目标对象集合
     */
    public static <T> List<T> copyBeanList(Collection<?> sources, Class<T> targetClass) {
        List<T> result = new ArrayList<>();
        if (sources == null || sources.isEmpty() || targetClass == null) {
            return result;
        }

        for (Object source : sources) {
            if (source != null) {
                result.add(copyBean(source, targetClass));
            }
        }
        return result;
    }

    /**
     * 将源对象集合复制为指定类型的新集合，并忽略 null 属性。
     *
     * @param sources     源对象集合
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 复制后的目标对象集合
     */
    public static <T> List<T> copyBeanListIgnoreNull(Collection<?> sources, Class<T> targetClass) {
        List<T> result = new ArrayList<>();
        if (sources == null || sources.isEmpty() || targetClass == null) {
            return result;
        }

        for (Object source : sources) {
            if (source != null) {
                result.add(copyBeanIgnoreNull(source, targetClass));
            }
        }
        return result;
    }

    // ============================== Bean 与 Map 转换 ==============================

    /**
     * 将 Bean 转换为 Map。
     *
     * @param bean Bean 对象
     * @return Map 对象
     */
    public static Map<String, Object> beanToMap(Object bean) {
        if (bean == null) {
            return new LinkedHashMap<>();
        }
        return BeanUtil.beanToMap(bean);
    }

    /**
     * 将 Bean 转换为 Map，并控制是否忽略 null 值。
     *
     * @param bean            Bean 对象
     * @param ignoreNullValue 是否忽略 null 值
     * @return Map 对象
     */
    public static Map<String, Object> beanToMap(Object bean, boolean ignoreNullValue) {
        if (bean == null) {
            return new LinkedHashMap<>();
        }
        return BeanUtil.beanToMap(bean, false, ignoreNullValue);
    }

    /**
     * 将 Bean 转换为下划线风格 key 的 Map。
     *
     * @param bean Bean 对象
     * @return Map 对象
     */
    public static Map<String, Object> beanToUnderlineMap(Object bean) {
        if (bean == null) {
            return new LinkedHashMap<>();
        }
        return BeanUtil.beanToMap(bean, true, false);
    }

    /**
     * 将 Bean 转换为下划线风格 key 的 Map，并控制是否忽略 null 值。
     *
     * @param bean            Bean 对象
     * @param ignoreNullValue 是否忽略 null 值
     * @return Map 对象
     */
    public static Map<String, Object> beanToUnderlineMap(Object bean, boolean ignoreNullValue) {
        if (bean == null) {
            return new LinkedHashMap<>();
        }
        return BeanUtil.beanToMap(bean, true, ignoreNullValue);
    }

    /**
     * 将 Map 转换为指定类型的 Bean。
     *
     * @param map         Map 对象
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return Bean 对象
     */
    public static <T> T mapToBean(Map<?, ?> map, Class<T> targetClass) {
        if (map == null || map.isEmpty() || targetClass == null) {
            return null;
        }
        return BeanUtil.mapToBean(map, targetClass, false);
    }

    /**
     * 将下划线风格 key 的 Map 转换为指定类型的 Bean。
     *
     * @param map         Map 对象
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return Bean 对象
     */
    public static <T> T underlineMapToBean(Map<?, ?> map, Class<T> targetClass) {
        if (map == null || map.isEmpty() || targetClass == null) {
            return null;
        }
        return BeanUtil.mapToBean(map, targetClass, true);
    }

    /**
     * 将 Map 填充到已有 Bean 对象。
     *
     * @param map    Map 对象
     * @param target 目标 Bean
     */
    public static void fillBeanWithMap(Map<?, ?> map, Object target) {
        if (map == null || map.isEmpty() || target == null) {
            return;
        }
        BeanUtil.fillBeanWithMap(map, target, false);
    }

    /**
     * 将下划线风格 key 的 Map 填充到已有 Bean 对象。
     *
     * @param map    Map 对象
     * @param target 目标 Bean
     */
    public static void fillBeanWithUnderlineMap(Map<?, ?> map, Object target) {
        if (map == null || map.isEmpty() || target == null) {
            return;
        }
        BeanUtil.fillBeanWithMap(map, target, true);
    }

    /**
     * 将 Map 的 key 转换为字符串类型。
     *
     * @param map 原 Map
     * @return key 为字符串的 Map
     */
    public static Map<String, Object> toStringKeyMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return result;
        }

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(Objects.toString(entry.getKey(), EMPTY), entry.getValue());
        }
        return result;
    }

    /**
     * 创建 HashMap。
     *
     * @param <K> key 类型
     * @param <V> value 类型
     * @return HashMap 对象
     */
    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<>();
    }

    /**
     * 创建 LinkedHashMap。
     *
     * @param <K> key 类型
     * @param <V> value 类型
     * @return LinkedHashMap 对象
     */
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap() {
        return new LinkedHashMap<>();
    }

    // ============================== 字段反射处理 ==============================

    /**
     * 判断类型是否存在指定字段。
     *
     * @param targetClass 目标类型
     * @param fieldName   字段名称
     * @return 是否存在指定字段
     */
    public static boolean hasField(Class<?> targetClass, String fieldName) {
        if (targetClass == null || StrUtil.isBlank(fieldName)) {
            return false;
        }
        return ReflectUtil.getField(targetClass, fieldName) != null;
    }

    /**
     * 获取指定类型的字段对象。
     *
     * @param targetClass 目标类型
     * @param fieldName   字段名称
     * @return 字段对象
     */
    public static Field getField(Class<?> targetClass, String fieldName) {
        if (targetClass == null || StrUtil.isBlank(fieldName)) {
            return null;
        }
        return ReflectUtil.getField(targetClass, fieldName);
    }

    /**
     * 获取指定类型的字段类型。
     *
     * @param targetClass 目标类型
     * @param fieldName   字段名称
     * @return 字段类型
     */
    public static Class<?> getFieldType(Class<?> targetClass, String fieldName) {
        Field field = getField(targetClass, fieldName);
        return field == null ? null : field.getType();
    }

    /**
     * 获取对象指定字段的值。
     *
     * @param target    目标对象
     * @param fieldName 字段名称
     * @return 字段值
     */
    public static Object getFieldValue(Object target, String fieldName) {
        if (target == null || StrUtil.isBlank(fieldName)) {
            return null;
        }
        return ReflectUtil.getFieldValue(target, fieldName);
    }

    /**
     * 获取对象指定字段的值，并转换为指定类型。
     *
     * @param target      目标对象
     * @param fieldName   字段名称
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 转换后的字段值
     */
    public static <T> T getFieldValue(Object target, String fieldName, Class<T> targetClass) {
        Object fieldValue = getFieldValue(target, fieldName);
        return convert(fieldValue, targetClass);
    }

    /**
     * 设置对象指定字段的值。
     *
     * @param target     目标对象
     * @param fieldName  字段名称
     * @param fieldValue 字段值
     */
    public static void setFieldValue(Object target, String fieldName, Object fieldValue) {
        if (target == null || StrUtil.isBlank(fieldName)) {
            return;
        }
        ReflectUtil.setFieldValue(target, fieldName, fieldValue);
    }

    /**
     * 调用对象的指定方法。
     *
     * @param target     目标对象
     * @param methodName 方法名称
     * @param args       方法参数
     * @return 方法返回值
     */
    public static Object invokeMethod(Object target, String methodName, Object... args) {
        if (target == null || StrUtil.isBlank(methodName)) {
            return null;
        }
        return ReflectUtil.invoke(target, methodName, args);
    }

    // ============================== 枚举处理 ==============================

    /**
     * 根据枚举名称获取枚举对象。
     *
     * @param enumClass 枚举类型
     * @param name      枚举名称
     * @param <E>       枚举类型
     * @return 枚举对象
     */
    public static <E extends Enum<E>> E enumByName(Class<E> enumClass, String name) {
        if (enumClass == null || StrUtil.isBlank(name)) {
            return null;
        }

        for (E enumItem : enumClass.getEnumConstants()) {
            if (Objects.equals(enumItem.name(), name)) {
                return enumItem;
            }
        }
        return null;
    }

    /**
     * 根据枚举名称忽略大小写获取枚举对象。
     *
     * @param enumClass 枚举类型
     * @param name      枚举名称
     * @param <E>       枚举类型
     * @return 枚举对象
     */
    public static <E extends Enum<E>> E enumByNameIgnoreCase(Class<E> enumClass, String name) {
        if (enumClass == null || StrUtil.isBlank(name)) {
            return null;
        }

        for (E enumItem : enumClass.getEnumConstants()) {
            if (enumItem.name().equalsIgnoreCase(name)) {
                return enumItem;
            }
        }
        return null;
    }

    /**
     * 根据枚举字段值获取枚举对象。
     *
     * @param enumClass  枚举类型
     * @param fieldName  字段名称
     * @param fieldValue 字段值
     * @param <E>        枚举类型
     * @return 枚举对象
     */
    public static <E extends Enum<E>> E enumByFieldValue(Class<E> enumClass, String fieldName, Object fieldValue) {
        if (enumClass == null || StrUtil.isBlank(fieldName)) {
            return null;
        }

        for (E enumItem : enumClass.getEnumConstants()) {
            Object value = getFieldValue(enumItem, fieldName);
            if (Objects.equals(value, fieldValue)) {
                return enumItem;
            }
        }
        return null;
    }

    /**
     * 判断枚举类型中是否存在指定名称。
     *
     * @param enumClass 枚举类型
     * @param name      枚举名称
     * @param <E>       枚举类型
     * @return 是否存在
     */
    public static <E extends Enum<E>> boolean isEnumName(Class<E> enumClass, String name) {
        return enumByName(enumClass, name) != null;
    }

    /**
     * 获取枚举名称。
     *
     * @param enumValue 枚举值
     * @return 枚举名称
     */
    public static String enumName(Enum<?> enumValue) {
        return enumValue == null ? null : enumValue.name();
    }

    /**
     * 获取枚举序号。
     *
     * @param enumValue 枚举值
     * @return 枚举序号
     */
    public static Integer enumOrdinal(Enum<?> enumValue) {
        return enumValue == null ? null : enumValue.ordinal();
    }

    // ============================== 集合判空 ==============================

    /**
     * 判断集合是否为空。
     *
     * @param values 集合
     * @return 是否为空
     */
    public static boolean isCollEmpty(Collection<?> values) {
        return CollUtil.isEmpty(values);
    }

    /**
     * 判断集合是否非空。
     *
     * @param values 集合
     * @return 是否非空
     */
    public static boolean isCollNotEmpty(Collection<?> values) {
        return CollUtil.isNotEmpty(values);
    }

    /**
     * 判断多个集合中是否存在空集合。
     *
     * @param values 集合数组
     * @return 是否存在空集合
     */
    public static boolean isAnyCollEmpty(Collection<?>... values) {
        if (values == null || values.length == 0) {
            return true;
        }
        for (Collection<?> value : values) {
            if (CollUtil.isEmpty(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断多个集合是否全部为空。
     *
     * @param values 集合数组
     * @return 是否全部为空
     */
    public static boolean isAllCollEmpty(Collection<?>... values) {
        if (values == null || values.length == 0) {
            return true;
        }
        for (Collection<?> value : values) {
            if (CollUtil.isNotEmpty(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 当 List 为 null 时返回新的空 List。
     *
     * @param values List 集合
     * @param <T>    元素类型
     * @return 非 null 的 List
     */
    public static <T> List<T> emptyListIfNull(List<T> values) {
        return values == null ? new ArrayList<>() : values;
    }

    /**
     * 当 Set 为 null 时返回新的空 Set。
     *
     * @param values Set 集合
     * @param <T>    元素类型
     * @return 非 null 的 Set
     */
    public static <T> Set<T> emptySetIfNull(Set<T> values) {
        return values == null ? new LinkedHashSet<>() : values;
    }

    /**
     * 当集合为空时返回默认集合。
     *
     * @param values        集合
     * @param defaultValues 默认集合
     * @param <T>           集合类型
     * @return 原集合或默认集合
     */
    public static <T extends Collection<?>> T defaultCollIfEmpty(T values, T defaultValues) {
        return CollUtil.isEmpty(values) ? defaultValues : values;
    }

    // ============================== 集合创建与转换 ==============================

    /**
     * 创建 ArrayList 集合。
     *
     * @param <T> 元素类型
     * @return ArrayList 集合
     */
    public static <T> List<T> newArrayList() {
        return new ArrayList<>();
    }

    /**
     * 根据已有集合创建 ArrayList 集合。
     *
     * @param values 原集合
     * @param <T>    元素类型
     * @return ArrayList 集合
     */
    public static <T> List<T> newArrayList(Collection<T> values) {
        if (CollUtil.isEmpty(values)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(values);
    }

    /**
     * 创建 LinkedHashSet 集合。
     *
     * @param <T> 元素类型
     * @return LinkedHashSet 集合
     */
    public static <T> Set<T> newLinkedHashSet() {
        return new LinkedHashSet<>();
    }

    /**
     * 根据已有集合创建 LinkedHashSet 集合。
     *
     * @param values 原集合
     * @param <T>    元素类型
     * @return LinkedHashSet 集合
     */
    public static <T> Set<T> newLinkedHashSet(Collection<T> values) {
        if (CollUtil.isEmpty(values)) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(values);
    }

    /**
     * 将数组转换为 List 集合。
     *
     * @param values 数组
     * @param <T>    元素类型
     * @return List 集合
     */
    @SafeVarargs
    public static <T> List<T> toList(T... values) {
        if (values == null || values.length == 0) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(values));
    }

    /**
     * 将数组转换为 LinkedHashSet 集合。
     *
     * @param values 数组
     * @param <T>    元素类型
     * @return Set 集合
     */
    @SafeVarargs
    public static <T> Set<T> toSet(T... values) {
        if (values == null || values.length == 0) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    /**
     * 合并多个 List
     *
     * @param lists 多个集合
     * @param <T> 元素类型
     * @return 合并后的 List
     */
    @SafeVarargs
    public static <T> List<T> mergeList(List<T>... lists) {

        List<T> result = new ArrayList<>();
        if (lists == null || lists.length == 0) {
            return result;
        }

        for (List<T> list : lists) {
            if (CollUtil.isEmpty(list)) {
                continue;
            }
            result.addAll(list);
        }

        return result;
    }

    /**
     * 合并 List 并去重（保持插入顺序）
     *
     * @param lists 多个集合
     * @param <T> 元素类型
     * @return 去重后的 List
     */
    @SafeVarargs
    public static <T> List<T> mergeListDistinct(List<T>... lists) {

        if (lists == null || lists.length == 0) {
            return new ArrayList<>();
        }

        Set<T> set = new LinkedHashSet<>();

        for (List<T> list : lists) {
            if (CollUtil.isEmpty(list)) {
                continue;
            }
            set.addAll(list);
        }

        return new ArrayList<>(set);
    }

    /**
     * 合并多个 Set
     *
     * @param sets 多个集合
     * @param <T> 元素类型
     * @return 合并后的 Set
     */
    @SafeVarargs
    public static <T> Set<T> mergeSet(Set<T>... sets) {

        Set<T> result = new LinkedHashSet<>();
        if (sets == null || sets.length == 0) {
            return result;
        }

        for (Set<T> set : sets) {
            if (CollUtil.isEmpty(set)) {
                continue;
            }
            result.addAll(set);
        }

        return result;
    }

    /**
     * 安全合并两个 List
     *
     * @param list1 集合1
     * @param list2 集合2
     * @param <T> 元素类型
     * @return 合并后的 List
     */
    public static <T> List<T> mergeSafe(List<T> list1, List<T> list2) {

        List<T> result = new ArrayList<>();

        if (CollUtil.isNotEmpty(list1)) {
            result.addAll(list1);
        }

        if (CollUtil.isNotEmpty(list2)) {
            result.addAll(list2);
        }

        return result;
    }

    /**
     * 合并 List 并排序
     *
     * @param comparator 排序规则
     * @param lists 多个集合
     * @param <T> 元素类型
     * @return 排序后的 List
     */
    @SafeVarargs
    public static <T> List<T> mergeListSorted(Comparator<T> comparator, List<T>... lists) {

        List<T> result = mergeList(lists);

        if (comparator != null) {
            result.sort(comparator);
        }

        return result;
    }

    /**
     * 按 key 合并集合（后者覆盖前者）
     *
     * @param lists 多个集合
     * @param keyMapper key映射函数
     * @param <T> 元素类型
     * @param <K> key类型
     * @return Map结构（合并结果）
     */
    @SafeVarargs
    public static <T, K> Map<K, T> mergeByKey(Function<T, K> keyMapper, List<T>... lists) {

        Map<K, T> result = new LinkedHashMap<>();

        if (lists == null || keyMapper == null) {
            return result;
        }

        for (List<T> list : lists) {
            if (CollUtil.isEmpty(list)) {
                continue;
            }

            for (T value : list) {
                result.put(keyMapper.apply(value), value);
            }
        }

        return result;
    }

    /**
     * 将集合转换为 List 集合。
     *
     * @param values 集合
     * @param <T>    元素类型
     * @return List 集合
     */
    public static <T> List<T> collToList(Collection<T> values) {
        if (CollUtil.isEmpty(values)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(values);
    }

    /**
     * 将集合转换为 LinkedHashSet 集合。
     *
     * @param values 集合
     * @param <T>    元素类型
     * @return Set 集合
     */
    public static <T> Set<T> collToSet(Collection<T> values) {
        if (CollUtil.isEmpty(values)) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(values);
    }

    /**
     * 将集合转换为不可变 List 集合。
     *
     * @param values 集合
     * @param <T>    元素类型
     * @return 不可变 List 集合
     */
    public static <T> List<T> toUnmodifiableList(Collection<T> values) {
        if (CollUtil.isEmpty(values)) {
            return List.of();
        }
        return List.copyOf(values);
    }

    /**
     * 将集合转换为不可变 Set 集合。
     *
     * @param values 集合
     * @param <T>    元素类型
     * @return 不可变 Set 集合
     */
    public static <T> Set<T> toUnmodifiableSet(Collection<T> values) {
        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }
        return Set.copyOf(values);
    }

    /**
     * 集合交集（基于 Hutool）
     *
     * @param list1 集合1
     * @param list2 集合2
     * @param <T> 元素类型
     * @return 交集结果
     */
    public static <T> Collection<T> intersection(Collection<T> list1, Collection<T> list2) {

        if (CollUtil.isEmpty(list1) || CollUtil.isEmpty(list2)) {
            return new ArrayList<>();
        }

        return CollUtil.intersection(list1, list2);
    }

    /**
     * 集合差集（list1 - list2）
     *
     * @param list1 集合1
     * @param list2 集合2
     * @param <T> 元素类型
     * @return 差集结果
     */
    public static <T> Collection<T> diff(Collection<T> list1, Collection<T> list2) {

        if (CollUtil.isEmpty(list1)) {
            return new ArrayList<>();
        }

        if (CollUtil.isEmpty(list2)) {
            return new ArrayList<>(list1);
        }

        return CollUtil.subtract(list1, list2);
    }

    /**
     * 集合并集（自动去重）
     *
     * @param list1 集合1
     * @param list2 集合2
     * @param <T> 元素类型
     * @return 并集结果
     */
    public static <T> List<T> union(Collection<T> list1, Collection<T> list2) {

        if (CollUtil.isEmpty(list1) && CollUtil.isEmpty(list2)) {
            return new ArrayList<>();
        }

        List<T> result = new ArrayList<>();

        if (CollUtil.isNotEmpty(list1)) {
            result.addAll(list1);
        }

        if (CollUtil.isNotEmpty(list2)) {
            result.addAll(list2);
        }

        return CollUtil.distinct(result);
    }

    /**
     * 集合分片（按指定大小切分）
     *
     * @param list 原集合
     * @param size 每片大小
     * @param <T> 元素类型
     * @return 分片后的 List
     */
    public static <T> List<List<T>> chunk(Collection<T> list, int size) {

        if (CollUtil.isEmpty(list) || size <= 0) {
            return new ArrayList<>();
        }

        return ListUtil.partition(new ArrayList<>(list), size);
    }

    /**
     * 嵌套集合扁平化
     *
     * @param nested 嵌套集合
     * @param <T> 元素类型
     * @return 扁平化结果
     */
    public static <T> List<T> flatten(Collection<? extends Collection<T>> nested) {

        List<T> result = new ArrayList<>();

        if (CollUtil.isEmpty(nested)) {
            return result;
        }

        for (Collection<T> collection : nested) {
            if (CollUtil.isEmpty(collection)) {
                continue;
            }
            result.addAll(collection);
        }

        return result;
    }

    /**
     * 获取排序后的 TopN
     *
     * @param list 原集合
     * @param comparator 排序规则
     * @param n 数量
     * @param <T> 元素类型
     * @return TopN结果
     */
    public static <T> List<T> topN(Collection<T> list, Comparator<T> comparator, int n) {

        if (CollUtil.isEmpty(list) || comparator == null || n <= 0) {
            return new ArrayList<>();
        }

        List<T> result = new ArrayList<>(list);
        result.sort(comparator);

        if (result.size() <= n) {
            return result;
        }

        return CollUtil.sub(result, 0, n);
    }

    // ============================== 集合过滤与映射 ==============================

    /**
     * 根据条件过滤集合。
     *
     * @param values    原集合
     * @param predicate 过滤条件
     * @param <T>       元素类型
     * @return 过滤后的 List 集合
     */
    public static <T> List<T> filter(Collection<T> values, Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        if (CollUtil.isEmpty(values) || predicate == null) {
            return result;
        }

        for (T value : values) {
            if (predicate.test(value)) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 过滤集合中的 null 元素。
     *
     * @param values 原集合
     * @param <T>    元素类型
     * @return 过滤后的 List 集合
     */
    public static <T> List<T> filterNotNull(Collection<T> values) {
        List<T> result = new ArrayList<>();
        if (CollUtil.isEmpty(values)) {
            return result;
        }

        for (T value : values) {
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 过滤集合中的空元素。
     *
     * @param values 原集合
     * @param <T>    元素类型
     * @return 过滤后的 List 集合
     */
    public static <T> List<T> filterNotEmpty(Collection<T> values) {
        List<T> result = new ArrayList<>();
        if (CollUtil.isEmpty(values)) {
            return result;
        }

        for (T value : values) {
            if (ObjectUtil.isNotEmpty(value)) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 将集合元素映射为另一种类型。
     *
     * @param values 原集合
     * @param mapper 映射函数
     * @param <T>    原元素类型
     * @param <R>    目标元素类型
     * @return 映射后的 List 集合
     */
    public static <T, R> List<R> map(Collection<T> values, Function<T, R> mapper) {
        List<R> result = new ArrayList<>();
        if (CollUtil.isEmpty(values) || mapper == null) {
            return result;
        }

        for (T value : values) {
            result.add(mapper.apply(value));
        }
        return result;
    }

    /**
     * 将集合元素映射为另一种类型，并忽略映射后的 null 值。
     *
     * @param values 原集合
     * @param mapper 映射函数
     * @param <T>    原元素类型
     * @param <R>    目标元素类型
     * @return 映射后的 List 集合
     */
    public static <T, R> List<R> mapNotNull(Collection<T> values, Function<T, R> mapper) {
        List<R> result = new ArrayList<>();
        if (CollUtil.isEmpty(values) || mapper == null) {
            return result;
        }

        for (T value : values) {
            R mappedValue = mapper.apply(value);
            if (mappedValue != null) {
                result.add(mappedValue);
            }
        }
        return result;
    }

    /**
     * 将集合元素扁平映射为 List 集合。
     *
     * @param values 原集合
     * @param mapper 扁平映射函数
     * @param <T>    原元素类型
     * @param <R>    目标元素类型
     * @return 扁平化后的 List 集合
     */
    public static <T, R> List<R> flatMap(Collection<T> values, Function<T, Collection<R>> mapper) {
        List<R> result = new ArrayList<>();
        if (CollUtil.isEmpty(values) || mapper == null) {
            return result;
        }

        for (T value : values) {
            Collection<R> mappedValues = mapper.apply(value);
            if (CollUtil.isNotEmpty(mappedValues)) {
                result.addAll(mappedValues);
            }
        }
        return result;
    }

    // ============================== 集合查找 ==============================

    /**
     * 获取集合第一个元素。
     *
     * @param values 集合
     * @param <T>    元素类型
     * @return 第一个元素
     */
    public static <T> T first(Collection<T> values) {
        if (CollUtil.isEmpty(values)) {
            return null;
        }
        return values.iterator().next();
    }

    /**
     * 根据条件获取集合第一个匹配元素。
     *
     * @param values    集合
     * @param predicate 匹配条件
     * @param <T>       元素类型
     * @return 第一个匹配元素
     */
    public static <T> T first(Collection<T> values, Predicate<T> predicate) {
        if (CollUtil.isEmpty(values) || predicate == null) {
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
     * 获取 List 最后一个元素。
     *
     * @param values List 集合
     * @param <T>    元素类型
     * @return 最后一个元素
     */
    public static <T> T last(List<T> values) {
        if (CollUtil.isEmpty(values)) {
            return null;
        }
        return values.get(values.size() - 1);
    }

    /**
     * 判断集合中是否存在满足条件的元素。
     *
     * @param values    集合
     * @param predicate 匹配条件
     * @param <T>       元素类型
     * @return 是否存在
     */
    public static <T> boolean anyMatch(Collection<T> values, Predicate<T> predicate) {
        if (CollUtil.isEmpty(values) || predicate == null) {
            return false;
        }

        for (T value : values) {
            if (predicate.test(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断集合中是否所有元素都满足条件。
     *
     * @param values    集合
     * @param predicate 匹配条件
     * @param <T>       元素类型
     * @return 是否全部满足
     */
    public static <T> boolean allMatch(Collection<T> values, Predicate<T> predicate) {
        if (CollUtil.isEmpty(values) || predicate == null) {
            return false;
        }

        for (T value : values) {
            if (!predicate.test(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断集合中是否没有元素满足条件。
     *
     * @param values    集合
     * @param predicate 匹配条件
     * @param <T>       元素类型
     * @return 是否全部不满足
     */
    public static <T> boolean noneMatch(Collection<T> values, Predicate<T> predicate) {
        if (CollUtil.isEmpty(values) || predicate == null) {
            return true;
        }

        for (T value : values) {
            if (predicate.test(value)) {
                return false;
            }
        }
        return true;
    }

    // ============================== 集合去重 ==============================

    /**
     * 对集合进行去重。
     *
     * @param values 原集合
     * @param <T>    元素类型
     * @return 去重后的 List 集合
     */
    public static <T> List<T> distinct(Collection<T> values) {
        if (CollUtil.isEmpty(values)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    /**
     * 根据指定字段或规则对集合进行去重。
     *
     * @param values    原集合
     * @param keyMapper 去重 key 映射函数
     * @param <T>       元素类型
     * @param <K>       key 类型
     * @return 去重后的 List 集合
     */
    public static <T, K> List<T> distinctBy(Collection<T> values, Function<T, K> keyMapper) {
        List<T> result = new ArrayList<>();
        if (CollUtil.isEmpty(values) || keyMapper == null) {
            return result;
        }

        Set<K> keySet = new LinkedHashSet<>();
        for (T value : values) {
            K key = keyMapper.apply(value);
            if (keySet.add(key)) {
                result.add(value);
            }
        }
        return result;
    }

    // ============================== 集合排序 ==============================

    /**
     * 根据比较器对集合排序。
     *
     * @param values     原集合
     * @param comparator 比较器
     * @param <T>        元素类型
     * @return 排序后的 List 集合
     */
    public static <T> List<T> sort(Collection<T> values, Comparator<T> comparator) {
        List<T> result = newArrayList(values);
        if (CollUtil.isEmpty(result) || comparator == null) {
            return result;
        }

        result.sort(comparator);
        return result;
    }

    /**
     * 对集合进行自然升序排序。
     *
     * @param values 原集合
     * @param <T>    元素类型
     * @return 排序后的 List 集合
     */
    public static <T extends Comparable<? super T>> List<T> sortAsc(Collection<T> values) {
        List<T> result = newArrayList(values);
        Collections.sort(result);
        return result;
    }

    /**
     * 对集合进行自然降序排序。
     *
     * @param values 原集合
     * @param <T>    元素类型
     * @return 排序后的 List 集合
     */
    public static <T extends Comparable<? super T>> List<T> sortDesc(Collection<T> values) {
        List<T> result = newArrayList(values);
        result.sort(Collections.reverseOrder());
        return result;
    }

    /**
     * 反转 List 集合。
     *
     * @param values 原 List 集合
     * @param <T>    元素类型
     * @return 反转后的 List 集合
     */
    public static <T> List<T> reverse(List<T> values) {
        List<T> result = newArrayList(values);
        Collections.reverse(result);
        return result;
    }

    // ============================== 集合分组与转 Map ==============================

    /**
     * 根据指定 key 对集合进行分组。
     *
     * @param values    原集合
     * @param keyMapper 分组 key 映射函数
     * @param <T>       元素类型
     * @param <K>       key 类型
     * @return 分组后的 Map
     */
    public static <T, K> Map<K, List<T>> groupBy(Collection<T> values, Function<T, K> keyMapper) {
        Map<K, List<T>> result = new LinkedHashMap<>();
        if (CollUtil.isEmpty(values) || keyMapper == null) {
            return result;
        }

        for (T value : values) {
            K key = keyMapper.apply(value);
            result.computeIfAbsent(key, item -> new ArrayList<>()).add(value);
        }
        return result;
    }

    /**
     * 根据指定 key 对集合进行分组，并统计每组数量。
     *
     * @param values    原集合
     * @param keyMapper 分组 key 映射函数
     * @param <T>       元素类型
     * @param <K>       key 类型
     * @return 分组数量 Map
     */
    public static <T, K> Map<K, Long> groupCountBy(Collection<T> values, Function<T, K> keyMapper) {
        Map<K, Long> result = new LinkedHashMap<>();
        if (CollUtil.isEmpty(values) || keyMapper == null) {
            return result;
        }

        for (T value : values) {
            K key = keyMapper.apply(value);
            result.put(key, result.getOrDefault(key, 0L) + 1L);
        }
        return result;
    }

    /**
     * 根据 key 分组，并对 value 进行字段映射转换。
     *
     * @param values    原集合
     * @param keyMapper 分组 key
     * @param valueMapper 值转换函数
     * @param <T> 原类型
     * @param <K> key类型
     * @param <R> value类型
     * @return 分组后的 Map
     */
    public static <T, K, R> Map<K, List<R>> groupByMapping(
            Collection<T> values,
            Function<T, K> keyMapper,
            Function<T, R> valueMapper) {

        Map<K, List<R>> result = new LinkedHashMap<>();
        if (CollUtil.isEmpty(values) || keyMapper == null || valueMapper == null) {
            return result;
        }

        for (T value : values) {
            K key = keyMapper.apply(value);
            R mapped = valueMapper.apply(value);

            result.computeIfAbsent(key, k -> new ArrayList<>()).add(mapped);
        }

        return result;
    }

    /**
     * 根据 key 分组，并过滤元素。
     *
     * @param values 原集合
     * @param keyMapper 分组key
     * @param filter 过滤条件
     * @param <T> 类型
     * @param <K> key类型
     * @return 分组结果
     */
    public static <T, K> Map<K, List<T>> groupByFilter(
            Collection<T> values,
            Function<T, K> keyMapper,
            Predicate<T> filter) {

        Map<K, List<T>> result = new LinkedHashMap<>();
        if (CollUtil.isEmpty(values) || keyMapper == null) {
            return result;
        }

        for (T value : values) {
            if (filter != null && !filter.test(value)) {
                continue;
            }

            K key = keyMapper.apply(value);
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        return result;
    }

    /**
     * 分组并做数值聚合（sum）
     *
     * @param values 原集合
     * @param keyMapper 分组key
     * @param valueMapper 数值映射
     * @param <T> 类型
     * @param <K> key类型
     * @return 聚合结果
     */
    public static <T, K> Map<K, Double> groupBySum(
            Collection<T> values,
            Function<T, K> keyMapper,
            Function<T, Double> valueMapper) {

        Map<K, Double> result = new LinkedHashMap<>();
        if (CollUtil.isEmpty(values) || keyMapper == null || valueMapper == null) {
            return result;
        }

        for (T value : values) {
            K key = keyMapper.apply(value);
            Double v = valueMapper.apply(value);
            if (v == null) {
                v = 0D;
            }

            result.put(key, result.getOrDefault(key, 0D) + v);
        }

        return result;
    }

    /**
     * 分组并获取最大值元素
     *
     * @param values 原集合
     * @param keyMapper key
     * @param comparator 比较器
     * @param <T> 类型
     * @param <K> key类型
     * @return 每组最大值元素
     */
    public static <T, K> Map<K, T> groupByMax(
            Collection<T> values,
            Function<T, K> keyMapper,
            Comparator<T> comparator) {

        Map<K, T> result = new LinkedHashMap<>();
        if (CollUtil.isEmpty(values) || keyMapper == null || comparator == null) {
            return result;
        }

        for (T value : values) {
            K key = keyMapper.apply(value);

            result.merge(key, value, (oldVal, newVal) ->
                    comparator.compare(oldVal, newVal) >= 0 ? oldVal : newVal);
        }

        return result;
    }

    /**
     * 二级分组（key1 -> key2 -> list）
     *
     * @param values 原集合
     * @param key1 一级key
     * @param key2 二级key
     * @param <T> 类型
     * @param <K1> 一级key
     * @param <K2> 二级key
     * @return 嵌套分组结果
     */
    public static <T, K1, K2> Map<K1, Map<K2, List<T>>> groupBy2Level(
            Collection<T> values,
            Function<T, K1> key1,
            Function<T, K2> key2) {

        Map<K1, Map<K2, List<T>>> result = new LinkedHashMap<>();
        if (CollUtil.isEmpty(values)) {
            return result;
        }

        for (T value : values) {
            K1 k1 = key1.apply(value);
            K2 k2 = key2.apply(value);

            result
                    .computeIfAbsent(k1, k -> new LinkedHashMap<>())
                    .computeIfAbsent(k2, k -> new ArrayList<>())
                    .add(value);
        }

        return result;
    }

    /**
     * 将集合转换为 Map，value 为集合元素本身，重复 key 保留第一条。
     *
     * @param values    原集合
     * @param keyMapper key 映射函数
     * @param <T>       元素类型
     * @param <K>       key 类型
     * @return 转换后的 Map
     */
    public static <T, K> Map<K, T> toMap(Collection<T> values, Function<T, K> keyMapper) {
        Map<K, T> result = new LinkedHashMap<>();
        if (CollUtil.isEmpty(values) || keyMapper == null) {
            return result;
        }

        for (T value : values) {
            K key = keyMapper.apply(value);
            result.putIfAbsent(key, value);
        }
        return result;
    }

    /**
     * 将集合转换为 Map，重复 key 保留第一条。
     *
     * @param values      原集合
     * @param keyMapper   key 映射函数
     * @param valueMapper value 映射函数
     * @param <T>         元素类型
     * @param <K>         key 类型
     * @param <V>         value 类型
     * @return 转换后的 Map
     */
    public static <T, K, V> Map<K, V> toMap(Collection<T> values, Function<T, K> keyMapper, Function<T, V> valueMapper) {
        Map<K, V> result = new LinkedHashMap<>();
        if (CollUtil.isEmpty(values) || keyMapper == null || valueMapper == null) {
            return result;
        }

        for (T value : values) {
            K key = keyMapper.apply(value);
            V mapValue = valueMapper.apply(value);
            result.putIfAbsent(key, mapValue);
        }
        return result;
    }

    /**
     * 将集合转换为 Map（支持过滤 + 转换）。
     *
     * @param values      原集合
     * @param keyMapper   key 映射函数
     * @param valueMapper value 映射函数
     * @param filter      过滤条件
     * @param <T>         元素类型
     * @param <K>         key 类型
     * @param <V>         value 类型
     * @return Map
     */
    public static <T, K, V> Map<K, V> toMap(
            Collection<T> values,
            Function<T, K> keyMapper,
            Function<T, V> valueMapper,
            Predicate<T> filter) {

        Map<K, V> result = new LinkedHashMap<>();
        if (CollUtil.isEmpty(values) || keyMapper == null || valueMapper == null) {
            return result;
        }

        for (T value : values) {
            if (filter != null && !filter.test(value)) {
                continue;
            }

            result.putIfAbsent(keyMapper.apply(value), valueMapper.apply(value));
        }

        return result;
    }

    /**
     * 高性能 toMap（直接覆盖，无判断）
     *
     * @param values    原集合
     * @param keyMapper key
     * @param valueMapper value
     * @param <T> 类型
     * @param <K> key类型
     * @param <V> value类型
     * @return Map
     */
    public static <T, K, V> Map<K, V> toMapFast(
            Collection<T> values,
            Function<T, K> keyMapper,
            Function<T, V> valueMapper) {

        Map<K, V> result = new LinkedHashMap<>();
        if (CollUtil.isEmpty(values) || keyMapper == null || valueMapper == null) {
            return result;
        }

        for (T value : values) {
            result.put(keyMapper.apply(value), valueMapper.apply(value));
        }

        return result;
    }

    /**
     * 转换为 Map（value 自动去重）
     *
     * @param values 原集合
     * @param keyMapper key
     * @param <T> 类型
     * @param <K> key类型
     * @return Map
     */
    public static <T, K> Map<K, T> toMapUnique(Collection<T> values, Function<T, K> keyMapper) {

        Map<K, T> result = new LinkedHashMap<>();
        if (CollUtil.isEmpty(values) || keyMapper == null) {
            return result;
        }

        for (T value : values) {
            result.put(keyMapper.apply(value), value);
        }

        return result;
    }

    /**
     * 将集合转换为 Map，重复 key 使用后一条覆盖前一条。
     *
     * @param values      原集合
     * @param keyMapper   key 映射函数
     * @param valueMapper value 映射函数
     * @param <T>         元素类型
     * @param <K>         key 类型
     * @param <V>         value 类型
     * @return 转换后的 Map
     */
    public static <T, K, V> Map<K, V> toMapOverwrite(Collection<T> values, Function<T, K> keyMapper, Function<T, V> valueMapper) {
        Map<K, V> result = new LinkedHashMap<>();
        if (CollUtil.isEmpty(values) || keyMapper == null || valueMapper == null) {
            return result;
        }

        for (T value : values) {
            result.put(keyMapper.apply(value), valueMapper.apply(value));
        }
        return result;
    }

    /**
     * 将集合转换为 Map，value 为 List（相同 key 自动归集）。
     *
     * @param values    原集合
     * @param keyMapper key 映射函数
     * @param <T>       元素类型
     * @param <K>       key 类型
     * @return 分组 Map
     */
    public static <T, K> Map<K, List<T>> toMapList(Collection<T> values, Function<T, K> keyMapper) {

        Map<K, List<T>> result = new LinkedHashMap<>();
        if (CollUtil.isEmpty(values) || keyMapper == null) {
            return result;
        }

        for (T value : values) {
            K key = keyMapper.apply(value);
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        return result;
    }

    /**
     * 将集合转换为 Map，支持自定义冲突处理策略。
     *
     * @param values       原集合
     * @param keyMapper    key 映射函数
     * @param valueMapper  value 映射函数
     * @param mergeFunction 冲突合并策略（旧值、新值）
     * @param <T>          元素类型
     * @param <K>          key 类型
     * @param <V>          value 类型
     * @return 转换后的 Map
     */
    public static <T, K, V> Map<K, V> toMap(
            Collection<T> values,
            Function<T, K> keyMapper,
            Function<T, V> valueMapper,
            BinaryOperator<V> mergeFunction) {

        Map<K, V> result = new LinkedHashMap<>();
        if (CollUtil.isEmpty(values) || keyMapper == null || valueMapper == null) {
            return result;
        }

        for (T value : values) {
            K key = keyMapper.apply(value);
            V val = valueMapper.apply(value);

            if (mergeFunction == null) {
                result.putIfAbsent(key, val);
            } else {
                result.merge(key, val, mergeFunction);
            }
        }

        return result;
    }

    // ============================== 集合分页与拆分 ==============================

    /**
     * 安全截取 List 集合。
     *
     * @param values    原 List 集合
     * @param fromIndex 开始索引
     * @param toIndex   结束索引
     * @param <T>       元素类型
     * @return 截取后的 List 集合
     */
    public static <T> List<T> subList(List<T> values, int fromIndex, int toIndex) {
        if (CollUtil.isEmpty(values)) {
            return new ArrayList<>();
        }

        int size = values.size();
        int start = Math.max(0, fromIndex);
        int end = Math.min(size, toIndex);
        if (start >= end) {
            return new ArrayList<>();
        }

        return new ArrayList<>(values.subList(start, end));
    }

    /**
     * 对集合进行内存分页。
     *
     * @param values   原集合
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @param <T>      元素类型
     * @return 分页后的 List 集合
     */
    public static <T> List<T> page(Collection<T> values, int pageNum, int pageSize) {
        if (CollUtil.isEmpty(values) || pageSize <= 0) {
            return new ArrayList<>();
        }

        List<T> list = newArrayList(values);
        int currentPage = Math.max(pageNum, 1);
        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = fromIndex + pageSize;
        return subList(list, fromIndex, toIndex);
    }

    /**
     * 按指定大小拆分集合。
     *
     * @param values 原集合
     * @param size   每组大小
     * @param <T>    元素类型
     * @return 拆分后的集合
     */
    public static <T> List<List<T>> splitList(Collection<T> values, int size) {
        List<List<T>> result = new ArrayList<>();
        if (CollUtil.isEmpty(values) || size <= 0) {
            return result;
        }

        List<T> list = newArrayList(values);
        for (int i = 0; i < list.size(); i += size) {
            result.add(subList(list, i, i + size));
        }
        return result;
    }

    // ============================== Map 判空 ==============================

    /**
     * 判断 Map 是否为空。
     *
     * @param values Map 对象
     * @return 是否为空
     */
    public static boolean isMapEmpty(Map<?, ?> values) {
        return MapUtil.isEmpty(values);
    }

    /**
     * 判断 Map 是否非空。
     *
     * @param values Map 对象
     * @return 是否非空
     */
    public static boolean isMapNotEmpty(Map<?, ?> values) {
        return MapUtil.isNotEmpty(values);
    }

    /**
     * 当 Map 为 null 时返回新的空 Map。
     *
     * @param values Map 对象
     * @param <K>    key 类型
     * @param <V>    value 类型
     * @return 非 null 的 Map
     */
    public static <K, V> Map<K, V> emptyMapIfNull(Map<K, V> values) {
        return values == null ? new LinkedHashMap<>() : values;
    }

    /**
     * 当 Map 为空时返回默认 Map。
     *
     * @param values        Map 对象
     * @param defaultValues 默认 Map
     * @param <K>           key 类型
     * @param <V>           value 类型
     * @return 原 Map 或默认 Map
     */
    public static <K, V> Map<K, V> defaultMapIfEmpty(Map<K, V> values, Map<K, V> defaultValues) {
        return MapUtil.isEmpty(values) ? defaultValues : values;
    }

    // ============================== Map 取值 ==============================

    /**
     * 获取 Map 中指定 key 的值。
     *
     * @param values Map 对象
     * @param key    key
     * @param <K>    key 类型
     * @param <V>    value 类型
     * @return value 值
     */
    public static <K, V> V mapGet(Map<K, V> values, K key) {
        if (MapUtil.isEmpty(values)) {
            return null;
        }
        return values.get(key);
    }

    /**
     * 获取 Map 中指定 key 的值，值不存在时返回默认值。
     *
     * @param values       Map 对象
     * @param key          key
     * @param defaultValue 默认值
     * @param <K>          key 类型
     * @param <V>          value 类型
     * @return value 值
     */
    public static <K, V> V mapGet(Map<K, V> values, K key, V defaultValue) {
        if (MapUtil.isEmpty(values)) {
            return defaultValue;
        }
        return values.getOrDefault(key, defaultValue);
    }

    /**
     * 获取 Map 中指定 key 的字符串值。
     *
     * @param values Map 对象
     * @param key    key
     * @return 字符串值
     */
    public static String mapGetStr(Map<?, ?> values, Object key) {
        if (MapUtil.isEmpty(values)) {
            return null;
        }
        return Convert.toStr(values.get(key));
    }

    /**
     * 获取 Map 中指定 key 的字符串值，值不存在时返回默认值。
     *
     * @param values       Map 对象
     * @param key          key
     * @param defaultValue 默认值
     * @return 字符串值
     */
    public static String mapGetStr(Map<?, ?> values, Object key, String defaultValue) {
        if (MapUtil.isEmpty(values)) {
            return defaultValue;
        }
        return Convert.toStr(values.get(key), defaultValue);
    }

    /**
     * 获取 Map 中指定 key 的 Integer 值。
     *
     * @param values Map 对象
     * @param key    key
     * @return Integer 值
     */
    public static Integer mapGetInt(Map<?, ?> values, Object key) {
        if (MapUtil.isEmpty(values)) {
            return null;
        }
        return Convert.toInt(values.get(key));
    }

    /**
     * 获取 Map 中指定 key 的 Long 值。
     *
     * @param values Map 对象
     * @param key    key
     * @return Long 值
     */
    public static Long mapGetLong(Map<?, ?> values, Object key) {
        if (MapUtil.isEmpty(values)) {
            return null;
        }
        return Convert.toLong(values.get(key));
    }

    /**
     * 获取 Map 中指定 key 的 Boolean 值。
     *
     * @param values Map 对象
     * @param key    key
     * @return Boolean 值
     */
    public static Boolean mapGetBool(Map<?, ?> values, Object key) {
        if (MapUtil.isEmpty(values)) {
            return null;
        }
        return Convert.toBool(values.get(key));
    }

    /**
     * 获取 Map 中指定 key 的 BigDecimal 值。
     *
     * @param values Map 对象
     * @param key    key
     * @return BigDecimal 值
     */
    public static BigDecimal mapGetBigDecimal(Map<?, ?> values, Object key) {
        if (MapUtil.isEmpty(values)) {
            return null;
        }
        return Convert.toBigDecimal(values.get(key));
    }

    // ============================== Map 判断 ==============================

    /**
     * 判断 Map 是否包含指定 key。
     *
     * @param values Map 对象
     * @param key    key
     * @return 是否包含
     */
    public static boolean containsKey(Map<?, ?> values, Object key) {
        return MapUtil.isNotEmpty(values) && values.containsKey(key);
    }

    /**
     * 判断 Map 是否包含任意一个 key。
     *
     * @param values Map 对象
     * @param keys   key 数组
     * @return 是否包含任意一个 key
     */
    public static boolean containsAnyKey(Map<?, ?> values, Object... keys) {
        if (MapUtil.isEmpty(values) || keys == null || keys.length == 0) {
            return false;
        }

        for (Object key : keys) {
            if (values.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断 Map 是否包含所有 key。
     *
     * @param values Map 对象
     * @param keys   key 数组
     * @return 是否包含所有 key
     */
    public static boolean containsAllKeys(Map<?, ?> values, Object... keys) {
        if (MapUtil.isEmpty(values) || keys == null || keys.length == 0) {
            return false;
        }

        for (Object key : keys) {
            if (!values.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    // ============================== Map 写入与清理 ==============================

    /**
     * 当 value 不为 null 时写入 Map。
     *
     * @param values Map 对象
     * @param key    key
     * @param value  value
     * @param <K>    key 类型
     * @param <V>    value 类型
     * @return Map 对象
     */
    public static <K, V> Map<K, V> putIfNotNull(Map<K, V> values, K key, V value) {
        if (values == null) {
            return new LinkedHashMap<>();
        }
        if (value != null) {
            values.put(key, value);
        }
        return values;
    }

    /**
     * 当字符串 value 非空白时写入 Map。
     *
     * @param values Map 对象
     * @param key    key
     * @param value  字符串 value
     * @param <K>    key 类型
     * @return Map 对象
     */
    public static <K> Map<K, Object> putIfNotBlank(Map<K, Object> values, K key, CharSequence value) {
        if (values == null) {
            values = new LinkedHashMap<>();
        }
        if (StrUtil.isNotBlank(value)) {
            values.put(key, value.toString());
        }
        return values;
    }

    /**
     * 移除 Map 中 value 为 null 的元素。
     *
     * @param values 原 Map
     * @param <K>    key 类型
     * @param <V>    value 类型
     * @return 清理后的 Map
     */
    public static <K, V> Map<K, V> removeNullValue(Map<K, V> values) {
        Map<K, V> result = new LinkedHashMap<>();
        if (MapUtil.isEmpty(values)) {
            return result;
        }

        for (Map.Entry<K, V> entry : values.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 移除 Map 中 value 为空的元素。
     *
     * @param values 原 Map
     * @param <K>    key 类型
     * @param <V>    value 类型
     * @return 清理后的 Map
     */
    public static <K, V> Map<K, V> removeEmptyValue(Map<K, V> values) {
        Map<K, V> result = new LinkedHashMap<>();
        if (MapUtil.isEmpty(values)) {
            return result;
        }

        for (Map.Entry<K, V> entry : values.entrySet()) {
            if (ObjectUtil.isNotEmpty(entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 移除 Map 中字符串 value 为空白的元素。
     *
     * @param values 原 Map
     * @param <K>    key 类型
     * @return 清理后的 Map
     */
    public static <K> Map<K, Object> removeBlankValue(Map<K, ?> values) {
        Map<K, Object> result = new LinkedHashMap<>();
        if (MapUtil.isEmpty(values)) {
            return result;
        }

        for (Map.Entry<K, ?> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof CharSequence charSequence && StrUtil.isBlank(charSequence)) {
                continue;
            }
            result.put(entry.getKey(), value);
        }
        return result;
    }

    /**
     * 根据条件过滤 Map。
     *
     * @param values    原 Map
     * @param predicate 过滤条件
     * @param <K>       key 类型
     * @param <V>       value 类型
     * @return 过滤后的 Map
     */
    public static <K, V> Map<K, V> filterMap(Map<K, V> values, Predicate<Map.Entry<K, V>> predicate) {
        Map<K, V> result = new LinkedHashMap<>();
        if (MapUtil.isEmpty(values) || predicate == null) {
            return result;
        }

        for (Map.Entry<K, V> entry : values.entrySet()) {
            if (predicate.test(entry)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    // ============================== Map 转换与排序 ==============================

    /**
     * 获取 Map 的 key 列表。
     *
     * @param values Map 对象
     * @param <K>    key 类型
     * @return key 列表
     */
    public static <K> List<K> mapKeyList(Map<K, ?> values) {
        if (MapUtil.isEmpty(values)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(values.keySet());
    }

    /**
     * 获取 Map 的 value 列表。
     *
     * @param values Map 对象
     * @param <V>    value 类型
     * @return value 列表
     */
    public static <V> List<V> mapValueList(Map<?, V> values) {
        if (MapUtil.isEmpty(values)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(values.values());
    }

    /**
     * 按 key 升序排序 Map。
     *
     * @param values 原 Map
     * @param <K>    key 类型
     * @param <V>    value 类型
     * @return 排序后的 Map
     */
    public static <K extends Comparable<? super K>, V> Map<K, V> sortMapByKeyAsc(Map<K, V> values) {
        Map<K, V> result = new TreeMap<>();
        if (MapUtil.isEmpty(values)) {
            return result;
        }

        result.putAll(values);
        return result;
    }

    /**
     * 按 key 降序排序 Map。
     *
     * @param values 原 Map
     * @param <K>    key 类型
     * @param <V>    value 类型
     * @return 排序后的 Map
     */
    public static <K extends Comparable<? super K>, V> Map<K, V> sortMapByKeyDesc(Map<K, V> values) {
        Map<K, V> result = new TreeMap<>(Collections.reverseOrder());
        if (MapUtil.isEmpty(values)) {
            return result;
        }

        result.putAll(values);
        return result;
    }

    /**
     * 合并两个 Map，后一个 Map 不覆盖前一个 Map 的同名 key。
     *
     * @param first  第一个 Map
     * @param second 第二个 Map
     * @param <K>    key 类型
     * @param <V>    value 类型
     * @return 合并后的 Map
     */
    public static <K, V> Map<K, V> mergeMap(Map<K, V> first, Map<K, V> second) {
        return mergeMap(first, second, false);
    }

    /**
     * 合并两个 Map，并控制后一个 Map 是否覆盖前一个 Map 的同名 key。
     *
     * @param first     第一个 Map
     * @param second    第二个 Map
     * @param overwrite 是否覆盖同名 key
     * @param <K>       key 类型
     * @param <V>       value 类型
     * @return 合并后的 Map
     */
    public static <K, V> Map<K, V> mergeMap(Map<K, V> first, Map<K, V> second, boolean overwrite) {
        Map<K, V> result = new LinkedHashMap<>();
        if (MapUtil.isNotEmpty(first)) {
            result.putAll(first);
        }
        if (MapUtil.isEmpty(second)) {
            return result;
        }

        for (Map.Entry<K, V> entry : second.entrySet()) {
            if (overwrite || !result.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    // ============================== 当前时间 ==============================

    /**
     * 获取当前日期时间。
     *
     * @return 当前日期时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前日期。
     *
     * @return 当前日期
     */
    public static LocalDate today() {
        return LocalDate.now();
    }

    /**
     * 获取当前时间。
     *
     * @return 当前时间
     */
    public static LocalTime currentTime() {
        return LocalTime.now();
    }

    /**
     * 获取当前 Date 对象。
     *
     * @return 当前 Date 对象
     */
    public static Date currentDate() {
        return DateUtil.date();
    }

    /**
     * 获取当前毫秒时间戳。
     *
     * @return 当前毫秒时间戳
     */
    public static long currentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前秒级时间戳。
     *
     * @return 当前秒级时间戳
     */
    public static long currentSecondTimestamp() {
        return Instant.now().getEpochSecond();
    }

    /**
     * 获取系统默认时区。
     *
     * @return 系统默认时区
     */
    public static ZoneId systemZoneId() {
        return ZoneId.systemDefault();
    }

    // ============================== 日期时间格式化 ==============================

    /**
     * 将 LocalDate 格式化为 yyyy-MM-dd 字符串。
     *
     * @param value 日期
     * @return 日期字符串
     */
    public static String formatDate(LocalDate value) {
        return value == null ? null : value.format(DATE_FORMATTER);
    }

    /**
     * 将 LocalTime 格式化为 HH:mm:ss 字符串。
     *
     * @param value 时间
     * @return 时间字符串
     */
    public static String formatTime(LocalTime value) {
        return value == null ? null : value.format(TIME_FORMATTER);
    }

    /**
     * 将 LocalDateTime 格式化为 yyyy-MM-dd HH:mm:ss 字符串。
     *
     * @param value 日期时间
     * @return 日期时间字符串
     */
    public static String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    /**
     * 将 LocalDateTime 格式化为 yyyy-MM-dd HH:mm:ss.SSS 字符串。
     *
     * @param value 日期时间
     * @return 日期时间字符串
     */
    public static String formatDateTimeMilli(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_MILLI_FORMATTER);
    }

    /**
     * 使用指定格式格式化 LocalDateTime。
     *
     * @param value   日期时间
     * @param pattern 日期时间格式
     * @return 日期时间字符串
     */
    public static String formatDateTime(LocalDateTime value, String pattern) {
        if (value == null || StrUtil.isBlank(pattern)) {
            return null;
        }
        return value.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 使用指定格式格式化 LocalDate。
     *
     * @param value   日期
     * @param pattern 日期格式
     * @return 日期字符串
     */
    public static String formatDate(LocalDate value, String pattern) {
        if (value == null || StrUtil.isBlank(pattern)) {
            return null;
        }
        return value.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 使用指定格式格式化 LocalTime。
     *
     * @param value   时间
     * @param pattern 时间格式
     * @return 时间字符串
     */
    public static String formatTime(LocalTime value, String pattern) {
        if (value == null || StrUtil.isBlank(pattern)) {
            return null;
        }
        return value.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 将 Date 格式化为 yyyy-MM-dd 字符串。
     *
     * @param value 日期
     * @return 日期字符串
     */
    public static String formatDate(Date value) {
        return value == null ? null : DateUtil.format(value, DATE_PATTERN);
    }

    /**
     * 将 Date 格式化为 yyyy-MM-dd HH:mm:ss 字符串。
     *
     * @param value 日期时间
     * @return 日期时间字符串
     */
    public static String formatDateTime(Date value) {
        return value == null ? null : DateUtil.format(value, DATE_TIME_PATTERN);
    }

    /**
     * 使用指定格式格式化 Date。
     *
     * @param value   日期时间
     * @param pattern 日期时间格式
     * @return 日期时间字符串
     */
    public static String formatDate(Date value, String pattern) {
        if (value == null || StrUtil.isBlank(pattern)) {
            return null;
        }
        return DateUtil.format(value, pattern);
    }

    // ============================== 日期时间解析 ==============================

    /**
     * 将 yyyy-MM-dd 字符串解析为 LocalDate。
     *
     * @param value 日期字符串
     * @return LocalDate 对象
     */
    public static LocalDate parseLocalDate(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return LocalDate.parse(value, DATE_FORMATTER);
    }

    /**
     * 使用指定格式将字符串解析为 LocalDate。
     *
     * @param value   日期字符串
     * @param pattern 日期格式
     * @return LocalDate 对象
     */
    public static LocalDate parseLocalDate(String value, String pattern) {
        if (StrUtil.isBlank(value) || StrUtil.isBlank(pattern)) {
            return null;
        }
        return LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 将 HH:mm:ss 字符串解析为 LocalTime。
     *
     * @param value 时间字符串
     * @return LocalTime 对象
     */
    public static LocalTime parseLocalTime(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return LocalTime.parse(value, TIME_FORMATTER);
    }

    /**
     * 使用指定格式将字符串解析为 LocalTime。
     *
     * @param value   时间字符串
     * @param pattern 时间格式
     * @return LocalTime 对象
     */
    public static LocalTime parseLocalTime(String value, String pattern) {
        if (StrUtil.isBlank(value) || StrUtil.isBlank(pattern)) {
            return null;
        }
        return LocalTime.parse(value, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 将 yyyy-MM-dd HH:mm:ss 字符串解析为 LocalDateTime。
     *
     * @param value 日期时间字符串
     * @return LocalDateTime 对象
     */
    public static LocalDateTime parseLocalDateTime(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }

    /**
     * 使用指定格式将字符串解析为 LocalDateTime。
     *
     * @param value   日期时间字符串
     * @param pattern 日期时间格式
     * @return LocalDateTime 对象
     */
    public static LocalDateTime parseLocalDateTime(String value, String pattern) {
        if (StrUtil.isBlank(value) || StrUtil.isBlank(pattern)) {
            return null;
        }
        return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 使用 Hutool 将字符串解析为 Date。
     *
     * @param value 日期时间字符串
     * @return Date 对象
     */
    public static Date parseDate(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return DateUtil.parse(value);
    }

    /**
     * 使用指定格式将字符串解析为 Date。
     *
     * @param value   日期时间字符串
     * @param pattern 日期时间格式
     * @return Date 对象
     */
    public static Date parseDate(String value, String pattern) {
        if (StrUtil.isBlank(value) || StrUtil.isBlank(pattern)) {
            return null;
        }
        return DateUtil.parse(value, pattern);
    }

    /**
     * 将 yyyy-MM-dd 字符串解析为 Date。
     *
     * @param value 日期字符串
     * @return Date 对象
     */
    public static Date parseDateOnly(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return DateUtil.parse(value, DATE_PATTERN);
    }

    /**
     * 将 yyyy-MM-dd HH:mm:ss 字符串解析为 Date。
     *
     * @param value 日期时间字符串
     * @return Date 对象
     */
    public static Date parseDateTime(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return DateUtil.parse(value, DATE_TIME_PATTERN);
    }

    // ============================== Date 与 java.time 转换 ==============================

    /**
     * 将 Date 转换为 LocalDateTime。
     *
     * @param value Date 对象
     * @return LocalDateTime 对象
     */
    public static LocalDateTime toLocalDateTime(Date value) {
        if (value == null) {
            return null;
        }
        return LocalDateTime.ofInstant(value.toInstant(), systemZoneId());
    }

    /**
     * 将 Date 转换为 LocalDate。
     *
     * @param value Date 对象
     * @return LocalDate 对象
     */
    public static LocalDate toLocalDate(Date value) {
        LocalDateTime localDateTime = toLocalDateTime(value);
        return localDateTime == null ? null : localDateTime.toLocalDate();
    }

    /**
     * 将 Date 转换为 LocalTime。
     *
     * @param value Date 对象
     * @return LocalTime 对象
     */
    public static LocalTime toLocalTime(Date value) {
        LocalDateTime localDateTime = toLocalDateTime(value);
        return localDateTime == null ? null : localDateTime.toLocalTime();
    }

    /**
     * 将 LocalDateTime 转换为 Date。
     *
     * @param value LocalDateTime 对象
     * @return Date 对象
     */
    public static Date toDate(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return Date.from(value.atZone(systemZoneId()).toInstant());
    }

    /**
     * 将 LocalDate 转换为 Date。
     *
     * @param value LocalDate 对象
     * @return Date 对象
     */
    public static Date toDate(LocalDate value) {
        if (value == null) {
            return null;
        }
        return Date.from(value.atStartOfDay(systemZoneId()).toInstant());
    }

    /**
     * 将 LocalDate 和 LocalTime 转换为 Date。
     *
     * @param date 日期
     * @param time 时间
     * @return Date 对象
     */
    public static Date toDate(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            return null;
        }
        return toDate(LocalDateTime.of(date, time));
    }

    /**
     * 将 LocalDate 和 LocalTime 转换为 LocalDateTime。
     *
     * @param date 日期
     * @param time 时间
     * @return LocalDateTime 对象
     */
    public static LocalDateTime toLocalDateTime(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            return null;
        }
        return LocalDateTime.of(date, time);
    }

    // ============================== 时间戳转换 ==============================

    /**
     * 将毫秒时间戳转换为 LocalDateTime。
     *
     * @param timestamp 毫秒时间戳
     * @return LocalDateTime 对象
     */
    public static LocalDateTime timestampToLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), systemZoneId());
    }

    /**
     * 将秒级时间戳转换为 LocalDateTime。
     *
     * @param secondTimestamp 秒级时间戳
     * @return LocalDateTime 对象
     */
    public static LocalDateTime secondTimestampToLocalDateTime(long secondTimestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(secondTimestamp), systemZoneId());
    }

    /**
     * 将 LocalDateTime 转换为毫秒时间戳。
     *
     * @param value 日期时间
     * @return 毫秒时间戳
     */
    public static Long toTimestamp(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(systemZoneId()).toInstant().toEpochMilli();
    }

    /**
     * 将 LocalDateTime 转换为秒级时间戳。
     *
     * @param value 日期时间
     * @return 秒级时间戳
     */
    public static Long toSecondTimestamp(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(systemZoneId()).toEpochSecond();
    }

    /**
     * 将 Date 转换为毫秒时间戳。
     *
     * @param value Date 对象
     * @return 毫秒时间戳
     */
    public static Long toTimestamp(Date value) {
        return value == null ? null : value.getTime();
    }

    /**
     * 将 Date 转换为秒级时间戳。
     *
     * @param value Date 对象
     * @return 秒级时间戳
     */
    public static Long toSecondTimestamp(Date value) {
        return value == null ? null : value.getTime() / 1000;
    }

    /**
     * 将毫秒时间戳转换为 Date。
     *
     * @param timestamp 毫秒时间戳
     * @return Date 对象
     */
    public static Date timestampToDate(long timestamp) {
        return new Date(timestamp);
    }

    /**
     * 将秒级时间戳转换为 Date。
     *
     * @param secondTimestamp 秒级时间戳
     * @return Date 对象
     */
    public static Date secondTimestampToDate(long secondTimestamp) {
        return new Date(secondTimestamp * 1000);
    }

    // ============================== 日期时间起止范围 ==============================

    /**
     * 获取指定日期的开始时间。
     *
     * @param value 日期
     * @return 日期开始时间
     */
    public static LocalDateTime startOfDay(LocalDate value) {
        return value == null ? null : value.atStartOfDay();
    }

    /**
     * 获取指定日期时间所在天的开始时间。
     *
     * @param value 日期时间
     * @return 当天开始时间
     */
    public static LocalDateTime startOfDay(LocalDateTime value) {
        return value == null ? null : value.toLocalDate().atStartOfDay();
    }

    /**
     * 获取指定日期的结束时间。
     *
     * @param value 日期
     * @return 日期结束时间
     */
    public static LocalDateTime endOfDay(LocalDate value) {
        return value == null ? null : value.atTime(LocalTime.MAX);
    }

    /**
     * 获取指定日期时间所在天的结束时间。
     *
     * @param value 日期时间
     * @return 当天结束时间
     */
    public static LocalDateTime endOfDay(LocalDateTime value) {
        return value == null ? null : value.toLocalDate().atTime(LocalTime.MAX);
    }

    /**
     * 获取指定 Date 所在天的开始时间。
     *
     * @param value Date 对象
     * @return 当天开始时间
     */
    public static Date startOfDay(Date value) {
        return value == null ? null : DateUtil.beginOfDay(value);
    }

    /**
     * 获取指定 Date 所在天的结束时间。
     *
     * @param value Date 对象
     * @return 当天结束时间
     */
    public static Date endOfDay(Date value) {
        return value == null ? null : DateUtil.endOfDay(value);
    }

    /**
     * 获取今天的开始时间。
     *
     * @return 今天开始时间
     */
    public static LocalDateTime startOfToday() {
        return startOfDay(today());
    }

    /**
     * 获取今天的结束时间。
     *
     * @return 今天结束时间
     */
    public static LocalDateTime endOfToday() {
        return endOfDay(today());
    }

    /**
     * 获取指定日期所在周的开始时间，周一作为一周开始。
     *
     * @param value 日期
     * @return 本周开始时间
     */
    public static LocalDateTime startOfWeek(LocalDate value) {
        if (value == null) {
            return null;
        }
        return value.with(DayOfWeek.MONDAY).atStartOfDay();
    }

    /**
     * 获取指定日期所在周的结束时间，周日作为一周结束。
     *
     * @param value 日期
     * @return 本周结束时间
     */
    public static LocalDateTime endOfWeek(LocalDate value) {
        if (value == null) {
            return null;
        }
        return value.with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);
    }

    /**
     * 获取指定日期所在月的开始时间。
     *
     * @param value 日期
     * @return 本月开始时间
     */
    public static LocalDateTime startOfMonth(LocalDate value) {
        if (value == null) {
            return null;
        }
        return value.withDayOfMonth(1).atStartOfDay();
    }

    /**
     * 获取指定日期所在月的结束时间。
     *
     * @param value 日期
     * @return 本月结束时间
     */
    public static LocalDateTime endOfMonth(LocalDate value) {
        if (value == null) {
            return null;
        }
        return YearMonth.from(value).atEndOfMonth().atTime(LocalTime.MAX);
    }

    /**
     * 获取指定日期所在年的开始时间。
     *
     * @param value 日期
     * @return 本年开始时间
     */
    public static LocalDateTime startOfYear(LocalDate value) {
        if (value == null) {
            return null;
        }
        return LocalDate.of(value.getYear(), 1, 1).atStartOfDay();
    }

    /**
     * 获取指定日期所在年的结束时间。
     *
     * @param value 日期
     * @return 本年结束时间
     */
    public static LocalDateTime endOfYear(LocalDate value) {
        if (value == null) {
            return null;
        }
        return LocalDate.of(value.getYear(), 12, 31).atTime(LocalTime.MAX);
    }

    /**
     * 获取本周开始时间，周一作为一周开始。
     *
     * @return 本周开始时间
     */
    public static LocalDateTime startOfCurrentWeek() {
        return startOfWeek(today());
    }

    /**
     * 获取本周结束时间，周日作为一周结束。
     *
     * @return 本周结束时间
     */
    public static LocalDateTime endOfCurrentWeek() {
        return endOfWeek(today());
    }

    /**
     * 获取本月开始时间。
     *
     * @return 本月开始时间
     */
    public static LocalDateTime startOfCurrentMonth() {
        return startOfMonth(today());
    }

    /**
     * 获取本月结束时间。
     *
     * @return 本月结束时间
     */
    public static LocalDateTime endOfCurrentMonth() {
        return endOfMonth(today());
    }

    /**
     * 获取本年开始时间。
     *
     * @return 本年开始时间
     */
    public static LocalDateTime startOfCurrentYear() {
        return startOfYear(today());
    }

    /**
     * 获取本年结束时间。
     *
     * @return 本年结束时间
     */
    public static LocalDateTime endOfCurrentYear() {
        return endOfYear(today());
    }

    // ============================== 日期时间加减 ==============================

    /**
     * 给日期时间增加指定年数。
     *
     * @param value 日期时间
     * @param years 年数
     * @return 计算后的日期时间
     */
    public static LocalDateTime plusYears(LocalDateTime value, long years) {
        return value == null ? null : value.plusYears(years);
    }

    /**
     * 给日期时间减少指定年数。
     *
     * @param value 日期时间
     * @param years 年数
     * @return 计算后的日期时间
     */
    public static LocalDateTime minusYears(LocalDateTime value, long years) {
        return value == null ? null : value.minusYears(years);
    }

    /**
     * 给日期时间增加指定月数。
     *
     * @param value  日期时间
     * @param months 月数
     * @return 计算后的日期时间
     */
    public static LocalDateTime plusMonths(LocalDateTime value, long months) {
        return value == null ? null : value.plusMonths(months);
    }

    /**
     * 给日期时间减少指定月数。
     *
     * @param value  日期时间
     * @param months 月数
     * @return 计算后的日期时间
     */
    public static LocalDateTime minusMonths(LocalDateTime value, long months) {
        return value == null ? null : value.minusMonths(months);
    }

    /**
     * 给日期时间增加指定天数。
     *
     * @param value 日期时间
     * @param days  天数
     * @return 计算后的日期时间
     */
    public static LocalDateTime plusDays(LocalDateTime value, long days) {
        return value == null ? null : value.plusDays(days);
    }

    /**
     * 给日期时间减少指定天数。
     *
     * @param value 日期时间
     * @param days  天数
     * @return 计算后的日期时间
     */
    public static LocalDateTime minusDays(LocalDateTime value, long days) {
        return value == null ? null : value.minusDays(days);
    }

    /**
     * 给日期时间增加指定小时数。
     *
     * @param value 日期时间
     * @param hours 小时数
     * @return 计算后的日期时间
     */
    public static LocalDateTime plusHours(LocalDateTime value, long hours) {
        return value == null ? null : value.plusHours(hours);
    }

    /**
     * 给日期时间减少指定小时数。
     *
     * @param value 日期时间
     * @param hours 小时数
     * @return 计算后的日期时间
     */
    public static LocalDateTime minusHours(LocalDateTime value, long hours) {
        return value == null ? null : value.minusHours(hours);
    }

    /**
     * 给日期时间增加指定分钟数。
     *
     * @param value   日期时间
     * @param minutes 分钟数
     * @return 计算后的日期时间
     */
    public static LocalDateTime plusMinutes(LocalDateTime value, long minutes) {
        return value == null ? null : value.plusMinutes(minutes);
    }

    /**
     * 给日期时间减少指定分钟数。
     *
     * @param value   日期时间
     * @param minutes 分钟数
     * @return 计算后的日期时间
     */
    public static LocalDateTime minusMinutes(LocalDateTime value, long minutes) {
        return value == null ? null : value.minusMinutes(minutes);
    }

    /**
     * 给日期时间增加指定秒数。
     *
     * @param value   日期时间
     * @param seconds 秒数
     * @return 计算后的日期时间
     */
    public static LocalDateTime plusSeconds(LocalDateTime value, long seconds) {
        return value == null ? null : value.plusSeconds(seconds);
    }

    /**
     * 给日期时间减少指定秒数。
     *
     * @param value   日期时间
     * @param seconds 秒数
     * @return 计算后的日期时间
     */
    public static LocalDateTime minusSeconds(LocalDateTime value, long seconds) {
        return value == null ? null : value.minusSeconds(seconds);
    }

    /**
     * 给 Date 增加指定天数。
     *
     * @param value Date 对象
     * @param days  天数
     * @return 计算后的 Date 对象
     */
    public static Date offsetDay(Date value, int days) {
        if (value == null) {
            return null;
        }
        return DateUtil.offsetDay(value, days);
    }

    /**
     * 给 Date 增加指定小时数。
     *
     * @param value Date 对象
     * @param hours 小时数
     * @return 计算后的 Date 对象
     */
    public static Date offsetHour(Date value, int hours) {
        if (value == null) {
            return null;
        }
        return DateUtil.offsetHour(value, hours);
    }

    /**
     * 给 Date 增加指定分钟数。
     *
     * @param value   Date 对象
     * @param minutes 分钟数
     * @return 计算后的 Date 对象
     */
    public static Date offsetMinute(Date value, int minutes) {
        if (value == null) {
            return null;
        }
        return DateUtil.offsetMinute(value, minutes);
    }

    // ============================== 日期时间比较 ==============================

    /**
     * 判断第一个日期时间是否早于第二个日期时间。
     *
     * @param first  第一个日期时间
     * @param second 第二个日期时间
     * @return 是否早于
     */
    public static boolean isBefore(LocalDateTime first, LocalDateTime second) {
        return first != null && second != null && first.isBefore(second);
    }

    /**
     * 判断第一个日期时间是否晚于第二个日期时间。
     *
     * @param first  第一个日期时间
     * @param second 第二个日期时间
     * @return 是否晚于
     */
    public static boolean isAfter(LocalDateTime first, LocalDateTime second) {
        return first != null && second != null && first.isAfter(second);
    }

    /**
     * 判断日期时间是否在指定范围内。
     *
     * @param value 日期时间
     * @param start 开始日期时间
     * @param end   结束日期时间
     * @return 是否在范围内
     */
    public static boolean isBetween(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        if (value == null || start == null || end == null) {
            return false;
        }
        return !value.isBefore(start) && !value.isAfter(end);
    }

    /**
     * 判断日期是否为今天。
     *
     * @param value 日期
     * @return 是否为今天
     */
    public static boolean isToday(LocalDate value) {
        return value != null && Objects.equals(value, today());
    }

    /**
     * 判断日期时间是否为今天。
     *
     * @param value 日期时间
     * @return 是否为今天
     */
    public static boolean isToday(LocalDateTime value) {
        return value != null && isToday(value.toLocalDate());
    }

    /**
     * 判断 Date 是否为今天。
     *
     * @param value Date 对象
     * @return 是否为今天
     */
    public static boolean isToday(Date value) {
        return value != null && DateUtil.isSameDay(value, currentDate());
    }

    /**
     * 判断两个日期是否为同一天。
     *
     * @param first  第一个日期
     * @param second 第二个日期
     * @return 是否为同一天
     */
    public static boolean isSameDay(LocalDate first, LocalDate second) {
        return first != null && second != null && Objects.equals(first, second);
    }

    /**
     * 判断两个日期时间是否为同一天。
     *
     * @param first  第一个日期时间
     * @param second 第二个日期时间
     * @return 是否为同一天
     */
    public static boolean isSameDay(LocalDateTime first, LocalDateTime second) {
        return first != null && second != null && Objects.equals(first.toLocalDate(), second.toLocalDate());
    }

    /**
     * 判断两个 Date 是否为同一天。
     *
     * @param first  第一个 Date
     * @param second 第二个 Date
     * @return 是否为同一天
     */
    public static boolean isSameDay(Date first, Date second) {
        return first != null && second != null && DateUtil.isSameDay(first, second);
    }

    /**
     * 获取较早的日期时间。
     *
     * @param first  第一个日期时间
     * @param second 第二个日期时间
     * @return 较早的日期时间
     */
    public static LocalDateTime minDateTime(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isBefore(second) ? first : second;
    }

    /**
     * 获取较晚的日期时间。
     *
     * @param first  第一个日期时间
     * @param second 第二个日期时间
     * @return 较晚的日期时间
     */
    public static LocalDateTime maxDateTime(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

    // ============================== 日期时间差值 ==============================

    /**
     * 计算两个日期相差天数。
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return 相差天数
     */
    public static Long betweenDays(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return null;
        }
        return Duration.between(start.atStartOfDay(), end.atStartOfDay()).toDays();
    }

    /**
     * 计算两个日期时间相差天数。
     *
     * @param start 开始日期时间
     * @param end   结束日期时间
     * @return 相差天数
     */
    public static Long betweenDays(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        return Duration.between(start, end).toDays();
    }

    /**
     * 计算两个日期时间相差小时数。
     *
     * @param start 开始日期时间
     * @param end   结束日期时间
     * @return 相差小时数
     */
    public static Long betweenHours(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        return Duration.between(start, end).toHours();
    }

    /**
     * 计算两个日期时间相差分钟数。
     *
     * @param start 开始日期时间
     * @param end   结束日期时间
     * @return 相差分钟数
     */
    public static Long betweenMinutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        return Duration.between(start, end).toMinutes();
    }

    /**
     * 计算两个日期时间相差秒数。
     *
     * @param start 开始日期时间
     * @param end   结束日期时间
     * @return 相差秒数
     */
    public static Long betweenSeconds(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        return Duration.between(start, end).toSeconds();
    }

    /**
     * 计算两个 Date 相差天数。
     *
     * @param start 开始 Date
     * @param end   结束 Date
     * @return 相差天数
     */
    public static Long betweenDays(Date start, Date end) {
        if (start == null || end == null) {
            return null;
        }
        return DateUtil.betweenDay(start, end, false);
    }

    /**
     * 计算两个 Date 相差小时数。
     *
     * @param start 开始 Date
     * @param end   结束 Date
     * @return 相差小时数
     */
    public static Long betweenHours(Date start, Date end) {
        if (start == null || end == null) {
            return null;
        }
        return DateUtil.between(start, end, DateUnit.HOUR, false);
    }

    /**
     * 计算两个 Date 相差分钟数。
     *
     * @param start 开始 Date
     * @param end   结束 Date
     * @return 相差分钟数
     */
    public static Long betweenMinutes(Date start, Date end) {
        if (start == null || end == null) {
            return null;
        }
        return DateUtil.between(start, end, DateUnit.MINUTE, false);
    }

    /**
     * 计算两个 Date 相差秒数。
     *
     * @param start 开始 Date
     * @param end   结束 Date
     * @return 相差秒数
     */
    public static Long betweenSeconds(Date start, Date end) {
        if (start == null || end == null) {
            return null;
        }
        return DateUtil.between(start, end, DateUnit.SECOND, false);
    }

    // ============================== 年龄与月份处理 ==============================

    /**
     * 根据生日计算年龄。
     *
     * @param birthday 生日
     * @return 年龄
     */
    public static Integer age(LocalDate birthday) {
        if (birthday == null) {
            return null;
        }
        return birthday.until(today()).getYears();
    }

    /**
     * 根据生日 Date 计算年龄。
     *
     * @param birthday 生日
     * @return 年龄
     */
    public static Integer age(Date birthday) {
        if (birthday == null) {
            return null;
        }
        return DateUtil.ageOfNow(birthday);
    }

    /**
     * 获取指定日期所在月份的天数。
     *
     * @param value 日期
     * @return 月份天数
     */
    public static Integer lengthOfMonth(LocalDate value) {
        return value == null ? null : value.lengthOfMonth();
    }

    /**
     * 获取指定日期所在年份的天数。
     *
     * @param value 日期
     * @return 年份天数
     */
    public static Integer lengthOfYear(LocalDate value) {
        return value == null ? null : value.lengthOfYear();
    }

    /**
     * 判断指定日期所在年份是否为闰年。
     *
     * @param value 日期
     * @return 是否为闰年
     */
    public static Boolean isLeapYear(LocalDate value) {
        return value == null ? null : value.isLeapYear();
    }

    // ============================== 日期集合生成 ==============================

    /**
     * 生成两个日期之间的日期集合，包含开始日期和结束日期。
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return 日期集合
     */
    public static List<LocalDate> listDaysBetween(LocalDate start, LocalDate end) {
        List<LocalDate> result = new ArrayList<>();
        if (start == null || end == null || start.isAfter(end)) {
            return result;
        }

        LocalDate current = start;
        while (!current.isAfter(end)) {
            result.add(current);
            current = current.plusDays(1);
        }
        return result;
    }

    /**
     * 生成两个日期时间之间的日期集合，包含开始日期和结束日期。
     *
     * @param start 开始日期时间
     * @param end   结束日期时间
     * @return 日期集合
     */
    public static List<LocalDate> listDaysBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return new ArrayList<>();
        }
        return listDaysBetween(start.toLocalDate(), end.toLocalDate());
    }

    /**
     * 生成指定月份的所有日期集合。
     *
     * @param year  年份
     * @param month 月份
     * @return 日期集合
     */
    public static List<LocalDate> listDaysOfMonth(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return listDaysBetween(yearMonth.atDay(1), yearMonth.atEndOfMonth());
    }

    // ============================== Hutool DateTime 兼容 ==============================

    /**
     * 将 Date 转换为 Hutool DateTime。
     *
     * @param value Date 对象
     * @return DateTime 对象
     */
    public static DateTime toDateTime(Date value) {
        return value == null ? null : DateUtil.date(value);
    }

    /**
     * 获取当前 Hutool DateTime。
     *
     * @return 当前 DateTime 对象
     */
    public static DateTime currentDateTime() {
        return DateUtil.date();
    }

    /**
     * 格式化当前日期时间。
     *
     * @return 当前日期时间字符串
     */
    public static String nowStr() {
        return DateUtil.now();
    }

    /**
     * 格式化当前日期。
     *
     * @return 当前日期字符串
     */
    public static String todayStr() {
        return DateUtil.today();
    }

    // ============================== 数字判断 ==============================

    /**
     * 判断对象是否可以转换为数字。
     *
     * @param value 对象
     * @return 是否为数字
     */
    public static boolean isNumeric(Object value) {
        if (value == null) {
            return false;
        }
        return NumberUtil.isNumber(value.toString());
    }

    /**
     * 判断数字是否为 0。
     *
     * @param value 数字
     * @return 是否为 0
     */
    public static boolean isZero(Number value) {
        if (value == null) {
            return false;
        }
        return BigDecimal.ZERO.compareTo(toBigDecimalOrZero(value)) == 0;
    }

    /**
     * 判断数字是否不为 0。
     *
     * @param value 数字
     * @return 是否不为 0
     */
    public static boolean isNotZero(Number value) {
        return !isZero(value);
    }

    /**
     * 判断数字是否大于 0。
     *
     * @param value 数字
     * @return 是否大于 0
     */
    public static boolean isPositive(Number value) {
        if (value == null) {
            return false;
        }
        return toBigDecimalOrZero(value).compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判断数字是否大于等于 0。
     *
     * @param value 数字
     * @return 是否大于等于 0
     */
    public static boolean isPositiveOrZero(Number value) {
        if (value == null) {
            return false;
        }
        return toBigDecimalOrZero(value).compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * 判断数字是否小于 0。
     *
     * @param value 数字
     * @return 是否小于 0
     */
    public static boolean isNegative(Number value) {
        if (value == null) {
            return false;
        }
        return toBigDecimalOrZero(value).compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * 判断数字是否小于等于 0。
     *
     * @param value 数字
     * @return 是否小于等于 0
     */
    public static boolean isNegativeOrZero(Number value) {
        if (value == null) {
            return false;
        }
        return toBigDecimalOrZero(value).compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * 判断数字是否在指定范围内，包含边界值。
     *
     * @param value 数字
     * @param min   最小值
     * @param max   最大值
     * @return 是否在范围内
     */
    public static boolean isBetween(Number value, Number min, Number max) {
        if (value == null || min == null || max == null) {
            return false;
        }

        BigDecimal valueDecimal = toBigDecimalOrZero(value);
        BigDecimal minDecimal = toBigDecimalOrZero(min);
        BigDecimal maxDecimal = toBigDecimalOrZero(max);
        return valueDecimal.compareTo(minDecimal) >= 0 && valueDecimal.compareTo(maxDecimal) <= 0;
    }

    // ============================== 数字转换 ==============================

    /**
     * 将对象转换为 BigDecimal，转换失败时返回 0。
     *
     * @param value 对象
     * @return BigDecimal 值
     */
    public static BigDecimal toBigDecimalOrZero(Object value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null ? BigDecimal.ZERO : decimal;
    }

    /**
     * 将对象转换为 BigDecimal，转换失败时返回默认值。
     *
     * @param value        对象
     * @param defaultValue 默认值
     * @return BigDecimal 值
     */
    public static BigDecimal toBigDecimalOrDefault(Object value, BigDecimal defaultValue) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null ? defaultValue : decimal;
    }

    /**
     * 将对象转换为金额 BigDecimal，默认保留 2 位小数。
     *
     * @param value 对象
     * @return 金额 BigDecimal
     */
    public static BigDecimal toAmount(Object value) {
        return toAmount(value, DEFAULT_AMOUNT_SCALE);
    }

    /**
     * 将对象转换为金额 BigDecimal，并保留指定小数位。
     *
     * @param value 对象
     * @param scale 小数位
     * @return 金额 BigDecimal
     */
    public static BigDecimal toAmount(Object value, int scale) {
        return round(toBigDecimalOrZero(value), scale);
    }

    /**
     * 将对象转换为金额 BigDecimal，并保留指定小数位和舍入模式。
     *
     * @param value        对象
     * @param scale        小数位
     * @param roundingMode 舍入模式
     * @return 金额 BigDecimal
     */
    public static BigDecimal toAmount(Object value, int scale, RoundingMode roundingMode) {
        return round(toBigDecimalOrZero(value), scale, roundingMode);
    }

    /**
     * 将金额元转换为分。
     *
     * @param yuan 金额元
     * @return 金额分
     */
    public static Long yuanToFen(Number yuan) {
        if (yuan == null) {
            return null;
        }
        return toBigDecimalOrZero(yuan)
                .multiply(ONE_HUNDRED)
                .setScale(0, DEFAULT_ROUNDING_MODE)
                .longValue();
    }

    /**
     * 将金额分转换为元，默认保留 2 位小数。
     *
     * @param fen 金额分
     * @return 金额元
     */
    public static BigDecimal fenToYuan(Number fen) {
        return fenToYuan(fen, DEFAULT_AMOUNT_SCALE);
    }

    /**
     * 将金额分转换为元，并保留指定小数位。
     *
     * @param fen   金额分
     * @param scale 小数位
     * @return 金额元
     */
    public static BigDecimal fenToYuan(Number fen, int scale) {
        if (fen == null) {
            return null;
        }
        return divide(fen, ONE_HUNDRED, scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 将数字限制在指定范围内。
     *
     * @param value 数字
     * @param min   最小值
     * @param max   最大值
     * @return 限制后的数字
     */
    public static BigDecimal limitBetween(Number value, Number min, Number max) {
        BigDecimal valueDecimal = toBigDecimalOrZero(value);
        BigDecimal minDecimal = toBigDecimalOrZero(min);
        BigDecimal maxDecimal = toBigDecimalOrZero(max);

        if (valueDecimal.compareTo(minDecimal) < 0) {
            return minDecimal;
        }
        if (valueDecimal.compareTo(maxDecimal) > 0) {
            return maxDecimal;
        }
        return valueDecimal;
    }

    // ============================== 数字比较 ==============================

    /**
     * 比较两个数字大小。
     *
     * @param first  第一个数字
     * @param second 第二个数字
     * @return 比较结果
     */
    public static int compare(Number first, Number second) {
        return toBigDecimalOrZero(first).compareTo(toBigDecimalOrZero(second));
    }

    /**
     * 判断第一个数字是否大于第二个数字。
     *
     * @param first  第一个数字
     * @param second 第二个数字
     * @return 是否大于
     */
    public static boolean greaterThan(Number first, Number second) {
        return compare(first, second) > 0;
    }

    /**
     * 判断第一个数字是否大于等于第二个数字。
     *
     * @param first  第一个数字
     * @param second 第二个数字
     * @return 是否大于等于
     */
    public static boolean greaterThanOrEqual(Number first, Number second) {
        return compare(first, second) >= 0;
    }

    /**
     * 判断第一个数字是否小于第二个数字。
     *
     * @param first  第一个数字
     * @param second 第二个数字
     * @return 是否小于
     */
    public static boolean lessThan(Number first, Number second) {
        return compare(first, second) < 0;
    }

    /**
     * 判断第一个数字是否小于等于第二个数字。
     *
     * @param first  第一个数字
     * @param second 第二个数字
     * @return 是否小于等于
     */
    public static boolean lessThanOrEqual(Number first, Number second) {
        return compare(first, second) <= 0;
    }

    /**
     * 获取两个数字中的较小值。
     *
     * @param first  第一个数字
     * @param second 第二个数字
     * @return 较小值
     */
    public static BigDecimal min(Number first, Number second) {
        BigDecimal firstDecimal = toBigDecimalOrZero(first);
        BigDecimal secondDecimal = toBigDecimalOrZero(second);
        return firstDecimal.compareTo(secondDecimal) <= 0 ? firstDecimal : secondDecimal;
    }

    /**
     * 获取两个数字中的较大值。
     *
     * @param first  第一个数字
     * @param second 第二个数字
     * @return 较大值
     */
    public static BigDecimal max(Number first, Number second) {
        BigDecimal firstDecimal = toBigDecimalOrZero(first);
        BigDecimal secondDecimal = toBigDecimalOrZero(second);
        return firstDecimal.compareTo(secondDecimal) >= 0 ? firstDecimal : secondDecimal;
    }

    // ============================== 数字计算 ==============================

    /**
     * 对多个数字进行加法计算。
     *
     * @param values 数字数组
     * @return 计算结果
     */
    public static BigDecimal add(Number... values) {
        if (values == null || values.length == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal result = BigDecimal.ZERO;
        for (Number value : values) {
            result = result.add(toBigDecimalOrZero(value));
        }
        return result;
    }

    /**
     * 对多个数字进行减法计算。
     *
     * @param first  第一个数字
     * @param values 后续数字数组
     * @return 计算结果
     */
    public static BigDecimal subtract(Number first, Number... values) {
        BigDecimal result = toBigDecimalOrZero(first);
        if (values == null || values.length == 0) {
            return result;
        }

        for (Number value : values) {
            result = result.subtract(toBigDecimalOrZero(value));
        }
        return result;
    }

    /**
     * 对多个数字进行乘法计算。
     *
     * @param values 数字数组
     * @return 计算结果
     */
    public static BigDecimal multiply(Number... values) {
        if (values == null || values.length == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal result = BigDecimal.ONE;
        for (Number value : values) {
            result = result.multiply(toBigDecimalOrZero(value));
        }
        return result;
    }

    /**
     * 对两个数字进行除法计算，默认保留 2 位小数。
     *
     * @param dividend 被除数
     * @param divisor  除数
     * @return 计算结果
     */
    public static BigDecimal divide(Number dividend, Number divisor) {
        return divide(dividend, divisor, DEFAULT_AMOUNT_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 对两个数字进行除法计算，并保留指定小数位。
     *
     * @param dividend 被除数
     * @param divisor  除数
     * @param scale    小数位
     * @return 计算结果
     */
    public static BigDecimal divide(Number dividend, Number divisor, int scale) {
        return divide(dividend, divisor, scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 对两个数字进行除法计算，并指定小数位和舍入模式。
     *
     * @param dividend     被除数
     * @param divisor      除数
     * @param scale        小数位
     * @param roundingMode 舍入模式
     * @return 计算结果
     */
    public static BigDecimal divide(Number dividend, Number divisor, int scale, RoundingMode roundingMode) {
        BigDecimal divisorDecimal = toBigDecimalOrZero(divisor);
        if (BigDecimal.ZERO.compareTo(divisorDecimal) == 0) {
            throw new ArithmeticException("除数不能为0");
        }

        RoundingMode mode = roundingMode == null ? DEFAULT_ROUNDING_MODE : roundingMode;
        return toBigDecimalOrZero(dividend).divide(divisorDecimal, Math.max(scale, 0), mode);
    }

    /**
     * 安全除法计算，除数为 0 时返回默认值。
     *
     * @param dividend     被除数
     * @param divisor      除数
     * @param defaultValue 默认值
     * @return 计算结果
     */
    public static BigDecimal safeDivide(Number dividend, Number divisor, BigDecimal defaultValue) {
        BigDecimal divisorDecimal = toBigDecimalOrZero(divisor);
        if (BigDecimal.ZERO.compareTo(divisorDecimal) == 0) {
            return defaultValue;
        }
        return divide(dividend, divisorDecimal);
    }

    /**
     * 安全除法计算，除数为 0 时返回默认值。
     *
     * @param dividend     被除数
     * @param divisor      除数
     * @param scale        小数位
     * @param roundingMode 舍入模式
     * @param defaultValue 默认值
     * @return 计算结果
     */
    public static BigDecimal safeDivide(Number dividend, Number divisor, int scale, RoundingMode roundingMode, BigDecimal defaultValue) {
        BigDecimal divisorDecimal = toBigDecimalOrZero(divisor);
        if (BigDecimal.ZERO.compareTo(divisorDecimal) == 0) {
            return defaultValue;
        }
        return divide(dividend, divisorDecimal, scale, roundingMode);
    }

    /**
     * 计算数字绝对值。
     *
     * @param value 数字
     * @return 绝对值
     */
    public static BigDecimal abs(Number value) {
        return toBigDecimalOrZero(value).abs();
    }

    /**
     * 计算数字相反数。
     *
     * @param value 数字
     * @return 相反数
     */
    public static BigDecimal negate(Number value) {
        return toBigDecimalOrZero(value).negate();
    }

    // ============================== 精度处理 ==============================

    /**
     * 对数字进行四舍五入，默认保留 2 位小数。
     *
     * @param value 数字
     * @return 处理后的数字
     */
    public static BigDecimal round(Number value) {
        return round(value, DEFAULT_AMOUNT_SCALE);
    }

    /**
     * 对数字进行四舍五入，并保留指定小数位。
     *
     * @param value 数字
     * @param scale 小数位
     * @return 处理后的数字
     */
    public static BigDecimal round(Number value, int scale) {
        return round(value, scale, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 对数字进行精度处理，并指定小数位和舍入模式。
     *
     * @param value        数字
     * @param scale        小数位
     * @param roundingMode 舍入模式
     * @return 处理后的数字
     */
    public static BigDecimal round(Number value, int scale, RoundingMode roundingMode) {
        RoundingMode mode = roundingMode == null ? DEFAULT_ROUNDING_MODE : roundingMode;
        return toBigDecimalOrZero(value).setScale(Math.max(scale, 0), mode);
    }

    /**
     * 去除 BigDecimal 末尾多余的 0。
     *
     * @param value 数字
     * @return 处理后的数字字符串
     */
    public static String stripTrailingZeros(Number value) {
        return toBigDecimalOrZero(value).stripTrailingZeros().toPlainString();
    }

    /**
     * 向上取整。
     *
     * @param value 数字
     * @return 取整结果
     */
    public static BigDecimal ceil(Number value) {
        return round(value, 0, RoundingMode.CEILING);
    }

    /**
     * 向下取整。
     *
     * @param value 数字
     * @return 取整结果
     */
    public static BigDecimal floor(Number value) {
        return round(value, 0, RoundingMode.FLOOR);
    }

    // ============================== 金额计算 ==============================

    /**
     * 金额加法计算，默认保留 2 位小数。
     *
     * @param values 金额数组
     * @return 金额结果
     */
    public static BigDecimal addAmount(Number... values) {
        return round(add(values), DEFAULT_AMOUNT_SCALE);
    }

    /**
     * 金额减法计算，默认保留 2 位小数。
     *
     * @param first  第一个金额
     * @param values 后续金额数组
     * @return 金额结果
     */
    public static BigDecimal subtractAmount(Number first, Number... values) {
        return round(subtract(first, values), DEFAULT_AMOUNT_SCALE);
    }

    /**
     * 金额乘法计算，默认保留 2 位小数。
     *
     * @param values 金额数组
     * @return 金额结果
     */
    public static BigDecimal multiplyAmount(Number... values) {
        return round(multiply(values), DEFAULT_AMOUNT_SCALE);
    }

    /**
     * 金额除法计算，默认保留 2 位小数。
     *
     * @param dividend 被除数金额
     * @param divisor  除数
     * @return 金额结果
     */
    public static BigDecimal divideAmount(Number dividend, Number divisor) {
        return divide(dividend, divisor, DEFAULT_AMOUNT_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 计算折扣后金额。
     *
     * @param amount       原金额
     * @param discountRate 折扣率
     * @return 折扣后金额
     */
    public static BigDecimal discountAmount(Number amount, Number discountRate) {
        return round(multiply(amount, discountRate), DEFAULT_AMOUNT_SCALE);
    }

    /**
     * 计算税额。
     *
     * @param amount  原金额
     * @param taxRate 税率
     * @return 税额
     */
    public static BigDecimal taxAmount(Number amount, Number taxRate) {
        return round(multiply(amount, taxRate), DEFAULT_AMOUNT_SCALE);
    }

    /**
     * 计算含税金额。
     *
     * @param amount  原金额
     * @param taxRate 税率
     * @return 含税金额
     */
    public static BigDecimal amountWithTax(Number amount, Number taxRate) {
        BigDecimal tax = taxAmount(amount, taxRate);
        return addAmount(amount, tax);
    }

    /**
     * 计算不含税金额。
     *
     * @param amountWithTax 含税金额
     * @param taxRate       税率
     * @return 不含税金额
     */
    public static BigDecimal amountWithoutTax(Number amountWithTax, Number taxRate) {
        BigDecimal divisor = add(BigDecimal.ONE, taxRate);
        return divide(amountWithTax, divisor, DEFAULT_AMOUNT_SCALE, DEFAULT_ROUNDING_MODE);
    }

    // ============================== 百分比与比例 ==============================

    /**
     * 计算比例，默认保留 4 位小数。
     *
     * @param numerator   分子
     * @param denominator 分母
     * @return 比例
     */
    public static BigDecimal ratio(Number numerator, Number denominator) {
        return safeDivide(numerator, denominator, 4, DEFAULT_ROUNDING_MODE, BigDecimal.ZERO);
    }

    /**
     * 计算比例，并保留指定小数位。
     *
     * @param numerator   分子
     * @param denominator 分母
     * @param scale       小数位
     * @return 比例
     */
    public static BigDecimal ratio(Number numerator, Number denominator, int scale) {
        return safeDivide(numerator, denominator, scale, DEFAULT_ROUNDING_MODE, BigDecimal.ZERO);
    }

    /**
     * 计算百分比数值，默认保留 2 位小数。
     *
     * @param numerator   分子
     * @param denominator 分母
     * @return 百分比数值
     */
    public static BigDecimal percent(Number numerator, Number denominator) {
        return percent(numerator, denominator, DEFAULT_PERCENT_SCALE);
    }

    /**
     * 计算百分比数值，并保留指定小数位。
     *
     * @param numerator   分子
     * @param denominator 分母
     * @param scale       小数位
     * @return 百分比数值
     */
    public static BigDecimal percent(Number numerator, Number denominator, int scale) {
        BigDecimal ratio = ratio(numerator, denominator, scale + 2);
        return round(ratio.multiply(ONE_HUNDRED), scale);
    }

    /**
     * 计算百分比字符串，默认保留 2 位小数。
     *
     * @param numerator   分子
     * @param denominator 分母
     * @return 百分比字符串
     */
    public static String percentStr(Number numerator, Number denominator) {
        return percentStr(numerator, denominator, DEFAULT_PERCENT_SCALE);
    }

    /**
     * 计算百分比字符串，并保留指定小数位。
     *
     * @param numerator   分子
     * @param denominator 分母
     * @param scale       小数位
     * @return 百分比字符串
     */
    public static String percentStr(Number numerator, Number denominator, int scale) {
        return formatNumber(percent(numerator, denominator, scale), scale) + "%";
    }

    // ============================== 数字格式化 ==============================

    /**
     * 格式化数字，默认保留 2 位小数。
     *
     * @param value 数字
     * @return 数字字符串
     */
    public static String formatNumber(Number value) {
        return formatNumber(value, DEFAULT_AMOUNT_SCALE);
    }

    /**
     * 格式化数字，并保留指定小数位。
     *
     * @param value 数字
     * @param scale 小数位
     * @return 数字字符串
     */
    public static String formatNumber(Number value, int scale) {
        return round(value, scale).toPlainString();
    }

    /**
     * 格式化金额，默认保留 2 位小数。
     *
     * @param value 金额
     * @return 金额字符串
     */
    public static String formatAmount(Number value) {
        return formatAmount(value, DEFAULT_AMOUNT_SCALE);
    }

    /**
     * 格式化金额，并保留指定小数位。
     *
     * @param value 金额
     * @param scale 小数位
     * @return 金额字符串
     */
    public static String formatAmount(Number value, int scale) {
        return formatNumber(value, scale);
    }

    /**
     * 格式化金额为千分位格式，默认保留 2 位小数。
     *
     * @param value 金额
     * @return 千分位金额字符串
     */
    public static String formatAmountWithComma(Number value) {
        return formatAmountWithComma(value, DEFAULT_AMOUNT_SCALE);
    }

    /**
     * 格式化金额为千分位格式，并保留指定小数位。
     *
     * @param value 金额
     * @param scale 小数位
     * @return 千分位金额字符串
     */
    public static String formatAmountWithComma(Number value, int scale) {
        StringBuilder pattern = new StringBuilder("#,##0");
        int safeScale = Math.max(scale, 0);
        if (safeScale > 0) {
            pattern.append(".");
            pattern.append("0".repeat(safeScale));
        }

        DecimalFormat decimalFormat = new DecimalFormat(pattern.toString());
        decimalFormat.setRoundingMode(DEFAULT_ROUNDING_MODE);
        return decimalFormat.format(toBigDecimalOrZero(value));
    }

    /**
     * 将金额分转换为元字符串，默认保留 2 位小数。
     *
     * @param fen 金额分
     * @return 金额元字符串
     */
    public static String fenToYuanStr(Number fen) {
        return fenToYuanStr(fen, DEFAULT_AMOUNT_SCALE);
    }

    /**
     * 将金额分转换为元字符串，并保留指定小数位。
     *
     * @param fen   金额分
     * @param scale 小数位
     * @return 金额元字符串
     */
    public static String fenToYuanStr(Number fen, int scale) {
        BigDecimal yuan = fenToYuan(fen, scale);
        return yuan == null ? null : yuan.toPlainString();
    }

    // ============================== 随机数字 ==============================

    /**
     * 生成指定范围内的随机整数，包含最小值和最大值。
     *
     * @param minInclusive 最小值
     * @param maxInclusive 最大值
     * @return 随机整数
     */
    public static int randomInt(int minInclusive, int maxInclusive) {
        if (minInclusive > maxInclusive) {
            throw new IllegalArgumentException("最小值不能大于最大值");
        }
        if (minInclusive == maxInclusive) {
            return minInclusive;
        }
        return RandomUtil.randomInt(minInclusive, maxInclusive + 1);
    }

    /**
     * 生成指定范围内的随机 Long，包含最小值和最大值。
     *
     * @param minInclusive 最小值
     * @param maxInclusive 最大值
     * @return 随机 Long
     */
    public static long randomLong(long minInclusive, long maxInclusive) {
        if (minInclusive > maxInclusive) {
            throw new IllegalArgumentException("最小值不能大于最大值");
        }
        if (minInclusive == maxInclusive) {
            return minInclusive;
        }
        return RandomUtil.randomLong(minInclusive, maxInclusive + 1);
    }

    /**
     * 生成指定范围内的随机金额，默认保留 2 位小数。
     *
     * @param min 最小金额
     * @param max 最大金额
     * @return 随机金额
     */
    public static BigDecimal randomAmount(Number min, Number max) {
        return randomAmount(min, max, DEFAULT_AMOUNT_SCALE);
    }

    /**
     * 生成指定范围内的随机金额，并保留指定小数位。
     *
     * @param min   最小金额
     * @param max   最大金额
     * @param scale 小数位
     * @return 随机金额
     */
    public static BigDecimal randomAmount(Number min, Number max, int scale) {
        BigDecimal minDecimal = toBigDecimalOrZero(min);
        BigDecimal maxDecimal = toBigDecimalOrZero(max);
        if (minDecimal.compareTo(maxDecimal) > 0) {
            throw new IllegalArgumentException("最小金额不能大于最大金额");
        }
        if (minDecimal.compareTo(maxDecimal) == 0) {
            return round(minDecimal, scale);
        }

        double randomValue = RandomUtil.randomDouble(minDecimal.doubleValue(), maxDecimal.doubleValue());
        return round(BigDecimal.valueOf(randomValue), scale);
    }

    // ============================== JSON 创建 ==============================

    /**
     * 创建空 JSONObject。
     *
     * @return JSONObject 对象
     */
    public static JSONObject newJsonObject() {
        return JSONUtil.createObj();
    }

    /**
     * 创建空 JSONArray。
     *
     * @return JSONArray 对象
     */
    public static JSONArray newJsonArray() {
        return JSONUtil.createArray();
    }

    /**
     * 创建忽略 null 值的 JSON 配置。
     *
     * @return JSON 配置
     */
    public static JSONConfig jsonConfigIgnoreNull() {
        return JSONConfig.create().setIgnoreNullValue(true);
    }

    /**
     * 创建保留 null 值的 JSON 配置。
     *
     * @return JSON 配置
     */
    public static JSONConfig jsonConfigKeepNull() {
        return JSONConfig.create().setIgnoreNullValue(false);
    }

    // ============================== JSON 判断 ==============================

    /**
     * 判断字符串是否为 JSON 类型字符串。
     *
     * @param value 字符串
     * @return 是否为 JSON 类型字符串
     */
    public static boolean isJson(String value) {
        return StrUtil.isNotBlank(value) && JSONUtil.isTypeJSON(value);
    }

    /**
     * 判断字符串是否为 JSON 对象类型字符串。
     *
     * @param value 字符串
     * @return 是否为 JSON 对象类型字符串
     */
    public static boolean isJsonObject(String value) {
        return StrUtil.isNotBlank(value) && JSONUtil.isTypeJSONObject(value);
    }

    /**
     * 判断字符串是否为 JSON 数组类型字符串。
     *
     * @param value 字符串
     * @return 是否为 JSON 数组类型字符串
     */
    public static boolean isJsonArray(String value) {
        return StrUtil.isNotBlank(value) && JSONUtil.isTypeJSONArray(value);
    }

    /**
     * 严格判断字符串是否为合法 JSON。
     *
     * @param value 字符串
     * @return 是否为合法 JSON
     */
    public static boolean isValidJson(String value) {
        if (StrUtil.isBlank(value) || !JSONUtil.isTypeJSON(value)) {
            return false;
        }

        try {
            JSONUtil.parse(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 严格判断字符串是否为合法 JSON 对象。
     *
     * @param value 字符串
     * @return 是否为合法 JSON 对象
     */
    public static boolean isValidJsonObject(String value) {
        if (StrUtil.isBlank(value) || !JSONUtil.isTypeJSONObject(value)) {
            return false;
        }

        try {
            JSONUtil.parseObj(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 严格判断字符串是否为合法 JSON 数组。
     *
     * @param value 字符串
     * @return 是否为合法 JSON 数组
     */
    public static boolean isValidJsonArray(String value) {
        if (StrUtil.isBlank(value) || !JSONUtil.isTypeJSONArray(value)) {
            return false;
        }

        try {
            JSONUtil.parseArray(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断对象是否为 JSON 空值。
     *
     * @param value 对象
     * @return 是否为 JSON 空值
     */
    public static boolean isJsonNull(Object value) {
        return JSONUtil.isNull(value);
    }

    /**
     * 判断对象是否不是 JSON 空值。
     *
     * @param value 对象
     * @return 是否不是 JSON 空值
     */
    public static boolean isNotJsonNull(Object value) {
        return !JSONUtil.isNull(value);
    }

    // ============================== JSON 序列化 ==============================

    /**
     * 将对象转换为 JSON 字符串。
     *
     * @param value 对象
     * @return JSON 字符串
     */
    public static String toJsonStr(Object value) {
        if (value == null) {
            return null;
        }
        return JSONUtil.toJsonStr(value);
    }

    /**
     * 将对象转换为 JSON 字符串，并使用指定 JSON 配置。
     *
     * @param value      对象
     * @param jsonConfig JSON 配置
     * @return JSON 字符串
     */
    public static String toJsonStr(Object value, JSONConfig jsonConfig) {
        if (value == null) {
            return null;
        }
        return JSONUtil.toJsonStr(value, jsonConfig);
    }

    /**
     * 将对象转换为 JSON 字符串，并忽略 null 值。
     *
     * @param value 对象
     * @return JSON 字符串
     */
    public static String toJsonStrIgnoreNull(Object value) {
        if (value == null) {
            return null;
        }
        return JSONUtil.toJsonStr(value, jsonConfigIgnoreNull());
    }

    /**
     * 将对象转换为格式化后的 JSON 字符串。
     *
     * @param value 对象
     * @return 格式化后的 JSON 字符串
     */
    public static String toJsonPrettyStr(Object value) {
        if (value == null) {
            return null;
        }
        return JSONUtil.toJsonPrettyStr(value);
    }

    /**
     * 将 JSON 对象转换为指定缩进级别的 JSON 字符串。
     *
     * @param json         JSON 对象
     * @param indentFactor 缩进级别
     * @return JSON 字符串
     */
    public static String toJsonStr(JSON json, int indentFactor) {
        if (json == null) {
            return null;
        }
        return JSONUtil.toJsonStr(json, Math.max(indentFactor, 0));
    }

    /**
     * 压缩 JSON 字符串。
     *
     * @param jsonStr JSON 字符串
     * @return 压缩后的 JSON 字符串
     */
    public static String compactJson(String jsonStr) {
        if (StrUtil.isBlank(jsonStr)) {
            return EMPTY;
        }

        try {
            JSON json = JSONUtil.parse(jsonStr);
            return JSONUtil.toJsonStr(json);
        } catch (Exception e) {
            return jsonStr;
        }
    }

    /**
     * 格式化 JSON 字符串。
     *
     * @param jsonStr JSON 字符串
     * @return 格式化后的 JSON 字符串
     */
    public static String formatJson(String jsonStr) {
        if (StrUtil.isBlank(jsonStr)) {
            return EMPTY;
        }
        return JSONUtil.formatJsonStr(jsonStr);
    }

    // ============================== JSON 解析 ==============================

    /**
     * 将对象解析为 JSON 对象。
     *
     * @param value 对象
     * @return JSON 对象
     */
    public static JSON parseJson(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return JSONUtil.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将对象解析为 JSONObject。
     *
     * @param value 对象
     * @return JSONObject 对象
     */
    public static JSONObject parseJsonObject(Object value) {
        if (value == null) {
            return JSONUtil.createObj();
        }

        try {
            return JSONUtil.parseObj(value);
        } catch (Exception e) {
            return JSONUtil.createObj();
        }
    }

    /**
     * 将字符串解析为 JSONObject。
     *
     * @param jsonStr JSON 字符串
     * @return JSONObject 对象
     */
    public static JSONObject parseJsonObject(String jsonStr) {
        if (StrUtil.isBlank(jsonStr)) {
            return JSONUtil.createObj();
        }

        try {
            return JSONUtil.parseObj(jsonStr);
        } catch (Exception e) {
            return JSONUtil.createObj();
        }
    }

    /**
     * 将对象解析为 JSONArray。
     *
     * @param value 对象
     * @return JSONArray 对象
     */
    public static JSONArray parseJsonArray(Object value) {
        if (value == null) {
            return JSONUtil.createArray();
        }

        try {
            return JSONUtil.parseArray(value);
        } catch (Exception e) {
            return JSONUtil.createArray();
        }
    }

    /**
     * 将字符串解析为 JSONArray。
     *
     * @param jsonStr JSON 字符串
     * @return JSONArray 对象
     */
    public static JSONArray parseJsonArray(String jsonStr) {
        if (StrUtil.isBlank(jsonStr)) {
            return JSONUtil.createArray();
        }

        try {
            return JSONUtil.parseArray(jsonStr);
        } catch (Exception e) {
            return JSONUtil.createArray();
        }
    }

    /**
     * 将对象解析为忽略 null 值的 JSONObject。
     *
     * @param value 对象
     * @return JSONObject 对象
     */
    public static JSONObject parseJsonObjectIgnoreNull(Object value) {
        if (value == null) {
            return JSONUtil.createObj();
        }

        try {
            return JSONUtil.parseObj(value, jsonConfigIgnoreNull());
        } catch (Exception e) {
            return JSONUtil.createObj();
        }
    }

    /**
     * 将对象解析为忽略 null 值的 JSONArray。
     *
     * @param value 对象
     * @return JSONArray 对象
     */
    public static JSONArray parseJsonArrayIgnoreNull(Object value) {
        if (value == null) {
            return JSONUtil.createArray();
        }

        try {
            return JSONUtil.parseArray(value, jsonConfigIgnoreNull());
        } catch (Exception e) {
            return JSONUtil.createArray();
        }
    }

    // ============================== JSON 转 Bean ==============================

    /**
     * 将 JSON 字符串转换为指定类型对象。
     *
     * @param jsonStr     JSON 字符串
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 目标对象
     */
    public static <T> T jsonToBean(String jsonStr, Class<T> targetClass) {
        if (StrUtil.isBlank(jsonStr) || targetClass == null) {
            return null;
        }

        try {
            return JSONUtil.toBean(jsonStr, targetClass);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 JSON 字符串转换为指定类型对象，并使用指定 JSON 配置。
     *
     * @param jsonStr     JSON 字符串
     * @param jsonConfig  JSON 配置
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 目标对象
     */
    public static <T> T jsonToBean(String jsonStr, JSONConfig jsonConfig, Class<T> targetClass) {
        if (StrUtil.isBlank(jsonStr) || targetClass == null) {
            return null;
        }

        try {
            return JSONUtil.toBean(jsonStr, jsonConfig, targetClass);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 JSONObject 转换为指定类型对象。
     *
     * @param jsonObject  JSONObject 对象
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 目标对象
     */
    public static <T> T jsonToBean(JSONObject jsonObject, Class<T> targetClass) {
        if (jsonObject == null || targetClass == null) {
            return null;
        }

        try {
            return JSONUtil.toBean(jsonObject, targetClass);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 JSON 字符串转换为指定泛型对象。
     *
     * @param jsonStr       JSON 字符串
     * @param typeReference 泛型类型引用
     * @param <T>           目标类型
     * @return 目标对象
     */
    public static <T> T jsonToBean(String jsonStr, TypeReference<T> typeReference) {
        if (StrUtil.isBlank(jsonStr) || typeReference == null) {
            return null;
        }

        try {
            return JSONUtil.toBean(jsonStr, typeReference, true);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 JSON 对象转换为指定泛型对象。
     *
     * @param json          JSON 对象
     * @param typeReference 泛型类型引用
     * @param <T>           目标类型
     * @return 目标对象
     */
    public static <T> T jsonToBean(JSON json, TypeReference<T> typeReference) {
        if (json == null || typeReference == null) {
            return null;
        }

        try {
            return JSONUtil.toBean(json, typeReference, true);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 JSON 字符串转换为指定 Type 对象。
     *
     * @param jsonStr JSON 字符串
     * @param type    目标 Type
     * @param <T>     目标类型
     * @return 目标对象
     */
    public static <T> T jsonToBean(String jsonStr, Type type) {
        if (StrUtil.isBlank(jsonStr) || type == null) {
            return null;
        }

        try {
            return JSONUtil.toBean(jsonStr, type, true);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 JSON 对象转换为指定 Type 对象。
     *
     * @param json JSON 对象
     * @param type 目标 Type
     * @param <T>  目标类型
     * @return 目标对象
     */
    public static <T> T jsonToBean(JSON json, Type type) {
        if (json == null || type == null) {
            return null;
        }

        try {
            return JSONUtil.toBean(json, type, true);
        } catch (Exception e) {
            return null;
        }
    }

    // ============================== JSON 转集合 ==============================

    /**
     * 将 JSON 数组字符串转换为指定类型 List。
     *
     * @param jsonArrayStr JSON 数组字符串
     * @param elementClass 元素类型
     * @param <T>          元素类型
     * @return List 集合
     */
    public static <T> List<T> jsonToList(String jsonArrayStr, Class<T> elementClass) {
        if (StrUtil.isBlank(jsonArrayStr) || elementClass == null) {
            return new ArrayList<>();
        }

        try {
            return JSONUtil.toList(jsonArrayStr, elementClass);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 将 JSONArray 转换为指定类型 List。
     *
     * @param jsonArray    JSONArray 对象
     * @param elementClass 元素类型
     * @param <T>          元素类型
     * @return List 集合
     */
    public static <T> List<T> jsonToList(JSONArray jsonArray, Class<T> elementClass) {
        if (jsonArray == null || elementClass == null) {
            return new ArrayList<>();
        }

        try {
            return JSONUtil.toList(jsonArray, elementClass);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 将 JSON 数组字符串转换为字符串 List。
     *
     * @param jsonArrayStr JSON 数组字符串
     * @return 字符串 List
     */
    public static List<String> jsonToStringList(String jsonArrayStr) {
        return jsonToList(jsonArrayStr, String.class);
    }

    /**
     * 将 JSON 数组字符串转换为 Integer List。
     *
     * @param jsonArrayStr JSON 数组字符串
     * @return Integer List
     */
    public static List<Integer> jsonToIntegerList(String jsonArrayStr) {
        return jsonToList(jsonArrayStr, Integer.class);
    }

    /**
     * 将 JSON 数组字符串转换为 Long List。
     *
     * @param jsonArrayStr JSON 数组字符串
     * @return Long List
     */
    public static List<Long> jsonToLongList(String jsonArrayStr) {
        return jsonToList(jsonArrayStr, Long.class);
    }

    /**
     * 将 JSON 数组字符串转换为 BigDecimal List。
     *
     * @param jsonArrayStr JSON 数组字符串
     * @return BigDecimal List
     */
    public static List<BigDecimal> jsonToBigDecimalList(String jsonArrayStr) {
        return jsonToList(jsonArrayStr, BigDecimal.class);
    }

    // ============================== JSON 转 Map ==============================

    /**
     * 将 JSON 字符串转换为 Map。
     *
     * @param jsonStr JSON 字符串
     * @return Map 对象
     */
    public static Map<String, Object> jsonToMap(String jsonStr) {
        JSONObject jsonObject = parseJsonObject(jsonStr);
        return jsonObjectToMap(jsonObject);
    }

    /**
     * 将 JSONObject 转换为 Map。
     *
     * @param jsonObject JSONObject 对象
     * @return Map 对象
     */
    public static Map<String, Object> jsonObjectToMap(JSONObject jsonObject) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (jsonObject == null || jsonObject.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            result.put(entry.getKey(), normalizeJsonValue(entry.getValue()));
        }
        return result;
    }

    /**
     * 将 JSON 数组字符串转换为 Map List。
     *
     * @param jsonArrayStr JSON 数组字符串
     * @return Map List
     */
    public static List<Map<String, Object>> jsonToMapList(String jsonArrayStr) {
        List<Map<String, Object>> result = new ArrayList<>();
        JSONArray jsonArray = parseJsonArray(jsonArrayStr);
        if (jsonArray.isEmpty()) {
            return result;
        }

        for (Object item : jsonArray) {
            if (item instanceof JSONObject jsonObject) {
                result.add(jsonObjectToMap(jsonObject));
            } else if (item instanceof Map<?, ?> map) {
                result.add(toStringKeyMap(map));
            }
        }
        return result;
    }

    // ============================== JSON 字段读取 ==============================

    /**
     * 根据路径表达式读取 JSON 字段值。
     *
     * @param json       JSON 对象
     * @param expression 路径表达式
     * @return 字段值
     */
    public static Object jsonGet(JSON json, String expression) {
        if (json == null || StrUtil.isBlank(expression)) {
            return null;
        }

        try {
            return JSONUtil.getByPath(json, expression);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据路径表达式读取 JSON 字段值，读取失败时返回默认值。
     *
     * @param json         JSON 对象
     * @param expression   路径表达式
     * @param defaultValue 默认值
     * @param <T>          字段类型
     * @return 字段值
     */
    public static <T> T jsonGet(JSON json, String expression, T defaultValue) {
        if (json == null || StrUtil.isBlank(expression)) {
            return defaultValue;
        }

        try {
            return JSONUtil.getByPath(json, expression, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 根据路径表达式读取 JSON 字符串中的字段值。
     *
     * @param jsonStr    JSON 字符串
     * @param expression 路径表达式
     * @return 字段值
     */
    public static Object jsonGet(String jsonStr, String expression) {
        JSON json = parseJson(jsonStr);
        return jsonGet(json, expression);
    }

    /**
     * 根据路径表达式读取 JSON 字符串中的字段值，读取失败时返回默认值。
     *
     * @param jsonStr      JSON 字符串
     * @param expression   路径表达式
     * @param defaultValue 默认值
     * @param <T>          字段类型
     * @return 字段值
     */
    public static <T> T jsonGet(String jsonStr, String expression, T defaultValue) {
        JSON json = parseJson(jsonStr);
        return jsonGet(json, expression, defaultValue);
    }

    /**
     * 根据路径表达式读取 JSON 字符串字段。
     *
     * @param json       JSON 对象
     * @param expression 路径表达式
     * @return 字符串字段值
     */
    public static String jsonGetStr(JSON json, String expression) {
        Object value = jsonGet(json, expression);
        return Convert.toStr(value);
    }

    /**
     * 根据路径表达式读取 JSON 字符串字段，读取失败时返回默认值。
     *
     * @param json         JSON 对象
     * @param expression   路径表达式
     * @param defaultValue 默认值
     * @return 字符串字段值
     */
    public static String jsonGetStr(JSON json, String expression, String defaultValue) {
        Object value = jsonGet(json, expression);
        return Convert.toStr(value, defaultValue);
    }

    /**
     * 根据路径表达式读取 JSON Integer 字段。
     *
     * @param json       JSON 对象
     * @param expression 路径表达式
     * @return Integer 字段值
     */
    public static Integer jsonGetInt(JSON json, String expression) {
        Object value = jsonGet(json, expression);
        return Convert.toInt(value);
    }

    /**
     * 根据路径表达式读取 JSON Integer 字段，读取失败时返回默认值。
     *
     * @param json         JSON 对象
     * @param expression   路径表达式
     * @param defaultValue 默认值
     * @return Integer 字段值
     */
    public static Integer jsonGetInt(JSON json, String expression, Integer defaultValue) {
        Object value = jsonGet(json, expression);
        return Convert.toInt(value, defaultValue);
    }

    /**
     * 根据路径表达式读取 JSON Long 字段。
     *
     * @param json       JSON 对象
     * @param expression 路径表达式
     * @return Long 字段值
     */
    public static Long jsonGetLong(JSON json, String expression) {
        Object value = jsonGet(json, expression);
        return Convert.toLong(value);
    }

    /**
     * 根据路径表达式读取 JSON Long 字段，读取失败时返回默认值。
     *
     * @param json         JSON 对象
     * @param expression   路径表达式
     * @param defaultValue 默认值
     * @return Long 字段值
     */
    public static Long jsonGetLong(JSON json, String expression, Long defaultValue) {
        Object value = jsonGet(json, expression);
        return Convert.toLong(value, defaultValue);
    }

    /**
     * 根据路径表达式读取 JSON Boolean 字段。
     *
     * @param json       JSON 对象
     * @param expression 路径表达式
     * @return Boolean 字段值
     */
    public static Boolean jsonGetBool(JSON json, String expression) {
        Object value = jsonGet(json, expression);
        return Convert.toBool(value);
    }

    /**
     * 根据路径表达式读取 JSON Boolean 字段，读取失败时返回默认值。
     *
     * @param json         JSON 对象
     * @param expression   路径表达式
     * @param defaultValue 默认值
     * @return Boolean 字段值
     */
    public static Boolean jsonGetBool(JSON json, String expression, Boolean defaultValue) {
        Object value = jsonGet(json, expression);
        return Convert.toBool(value, defaultValue);
    }

    /**
     * 根据路径表达式读取 JSON BigDecimal 字段。
     *
     * @param json       JSON 对象
     * @param expression 路径表达式
     * @return BigDecimal 字段值
     */
    public static BigDecimal jsonGetBigDecimal(JSON json, String expression) {
        Object value = jsonGet(json, expression);
        return Convert.toBigDecimal(value);
    }

    /**
     * 根据路径表达式读取 JSON BigDecimal 字段，读取失败时返回默认值。
     *
     * @param json         JSON 对象
     * @param expression   路径表达式
     * @param defaultValue 默认值
     * @return BigDecimal 字段值
     */
    public static BigDecimal jsonGetBigDecimal(JSON json, String expression, BigDecimal defaultValue) {
        Object value = jsonGet(json, expression);
        return Convert.toBigDecimal(value, defaultValue);
    }

    /**
     * 根据路径表达式读取 JSON 对象字段。
     *
     * @param json       JSON 对象
     * @param expression 路径表达式
     * @return JSONObject 字段值
     */
    public static JSONObject jsonGetObject(JSON json, String expression) {
        Object value = jsonGet(json, expression);
        if (value == null) {
            return JSONUtil.createObj();
        }
        return parseJsonObject(value);
    }

    /**
     * 根据路径表达式读取 JSON 数组字段。
     *
     * @param json       JSON 对象
     * @param expression 路径表达式
     * @return JSONArray 字段值
     */
    public static JSONArray jsonGetArray(JSON json, String expression) {
        Object value = jsonGet(json, expression);
        if (value == null) {
            return JSONUtil.createArray();
        }
        return parseJsonArray(value);
    }

    // ============================== JSON 字段写入 ==============================

    /**
     * 向 JSONObject 写入字段。
     *
     * @param jsonObject JSONObject 对象
     * @param key        key
     * @param value      value
     * @return JSONObject 对象
     */
    public static JSONObject jsonPut(JSONObject jsonObject, String key, Object value) {
        JSONObject target = jsonObject == null ? JSONUtil.createObj() : jsonObject;
        if (StrUtil.isBlank(key)) {
            return target;
        }

        target.set(key, value);
        return target;
    }

    /**
     * 当 value 不为 null 时向 JSONObject 写入字段。
     *
     * @param jsonObject JSONObject 对象
     * @param key        key
     * @param value      value
     * @return JSONObject 对象
     */
    public static JSONObject jsonPutIfNotNull(JSONObject jsonObject, String key, Object value) {
        JSONObject target = jsonObject == null ? JSONUtil.createObj() : jsonObject;
        if (StrUtil.isBlank(key) || value == null) {
            return target;
        }

        target.set(key, value);
        return target;
    }

    /**
     * 当字符串 value 非空白时向 JSONObject 写入字段。
     *
     * @param jsonObject JSONObject 对象
     * @param key        key
     * @param value      字符串 value
     * @return JSONObject 对象
     */
    public static JSONObject jsonPutIfNotBlank(JSONObject jsonObject, String key, CharSequence value) {
        JSONObject target = jsonObject == null ? JSONUtil.createObj() : jsonObject;
        if (StrUtil.isBlank(key) || StrUtil.isBlank(value)) {
            return target;
        }

        target.set(key, value.toString());
        return target;
    }

    /**
     * 根据路径表达式向 JSON 对象写入字段。
     *
     * @param json       JSON 对象
     * @param expression 路径表达式
     * @param value      字段值
     */
    public static void jsonPutByPath(JSON json, String expression, Object value) {
        if (json == null || StrUtil.isBlank(expression)) {
            return;
        }

        JSONUtil.putByPath(json, expression, value);
    }

    /**
     * 向 JSONArray 追加元素。
     *
     * @param jsonArray JSONArray 对象
     * @param value     元素值
     * @return JSONArray 对象
     */
    public static JSONArray jsonArrayAdd(JSONArray jsonArray, Object value) {
        JSONArray target = jsonArray == null ? JSONUtil.createArray() : jsonArray;
        target.add(value);
        return target;
    }

    /**
     * 向 JSONArray 批量追加元素。
     *
     * @param jsonArray JSONArray 对象
     * @param values    元素集合
     * @return JSONArray 对象
     */
    public static JSONArray jsonArrayAddAll(JSONArray jsonArray, Collection<?> values) {
        JSONArray target = jsonArray == null ? JSONUtil.createArray() : jsonArray;
        if (CollUtil.isEmpty(values)) {
            return target;
        }

        for (Object value : values) {
            target.add(value);
        }
        return target;
    }

    // ============================== JSON 字符串处理 ==============================

    /**
     * 对字符串进行 JSON 引号转义。
     *
     * @param value 字符串
     * @return 转义后的字符串
     */
    public static String jsonQuote(String value) {
        return value == null ? null : JSONUtil.quote(value);
    }

    /**
     * 对字符串中的不可见字符进行 JSON 转义。
     *
     * @param value 字符串
     * @return 转义后的字符串
     */
    public static String jsonEscape(String value) {
        return value == null ? null : JSONUtil.escape(value);
    }

    /**
     * 将 XML 字符串转换为 JSONObject。
     *
     * @param xml XML 字符串
     * @return JSONObject 对象
     */
    public static JSONObject xmlToJson(String xml) {
        if (StrUtil.isBlank(xml)) {
            return JSONUtil.createObj();
        }

        try {
            return JSONUtil.xmlToJson(xml);
        } catch (Exception e) {
            return JSONUtil.createObj();
        }
    }

    /**
     * 将 JSON 对象转换为 XML 字符串。
     *
     * @param json JSON 对象
     * @return XML 字符串
     */
    public static String jsonToXml(JSON json) {
        if (json == null) {
            return null;
        }

        try {
            return JSONUtil.toXmlStr(json);
        } catch (Exception e) {
            return null;
        }
    }

    // ============================== JSON 内部辅助方法 ==============================

    /**
     * 规范化 JSON 字段值。
     *
     * @param value 字段值
     * @return 规范化后的字段值
     */
    private static Object normalizeJsonValue(Object value) {
        if (JSONUtil.isNull(value)) {
            return null;
        }

        if (value instanceof JSONObject jsonObject) {
            return jsonObjectToMap(jsonObject);
        }

        if (value instanceof JSONArray jsonArray) {
            List<Object> list = new ArrayList<>();
            for (Object item : jsonArray) {
                list.add(normalizeJsonValue(item));
            }
            return list;
        }

        return value;
    }

    // ============================== 内部辅助方法 ==============================

    /**
     * 通过无参构造方法创建对象实例。
     *
     * @param targetClass 目标类型
     * @param <T>         目标类型
     * @return 对象实例
     */
    private static <T> T newInstance(Class<T> targetClass) {
        if (targetClass == null) {
            throw new IllegalArgumentException("目标类型不能为空");
        }

        try {
            Constructor<T> constructor = targetClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("目标类型必须提供可访问的无参构造方法：" + targetClass.getName(), e);
        }
    }

    // ============================== 文件判断 ==============================

    /**
     * 判断文件或目录是否存在。
     *
     * @param path 文件路径
     * @return 是否存在
     */
    public static boolean fileExists(String path) {
        return StrUtil.isNotBlank(path) && FileUtil.exist(path);
    }

    /**
     * 判断文件或目录是否存在。
     *
     * @param file 文件对象
     * @return 是否存在
     */
    public static boolean fileExists(File file) {
        return file != null && FileUtil.exist(file);
    }

    /**
     * 判断路径是否为文件。
     *
     * @param path 文件路径
     * @return 是否为文件
     */
    public static boolean isFile(String path) {
        return StrUtil.isNotBlank(path) && FileUtil.isFile(path);
    }

    /**
     * 判断对象是否为文件。
     *
     * @param file 文件对象
     * @return 是否为文件
     */
    public static boolean isFile(File file) {
        return file != null && FileUtil.isFile(file);
    }

    /**
     * 判断路径是否为目录。
     *
     * @param path 目录路径
     * @return 是否为目录
     */
    public static boolean isDirectory(String path) {
        return StrUtil.isNotBlank(path) && FileUtil.isDirectory(path);
    }

    /**
     * 判断对象是否为目录。
     *
     * @param file 文件对象
     * @return 是否为目录
     */
    public static boolean isDirectory(File file) {
        return file != null && FileUtil.isDirectory(file);
    }

    /**
     * 判断文件是否为空文件。
     *
     * @param file 文件对象
     * @return 是否为空文件
     */
    public static boolean isEmptyFile(File file) {
        return file != null && FileUtil.isFile(file) && file.length() == 0;
    }

    /**
     * 判断目录是否为空目录。
     *
     * @param dir 目录对象
     * @return 是否为空目录
     */
    public static boolean isEmptyDirectory(File dir) {
        if (dir == null || !FileUtil.isDirectory(dir)) {
            return false;
        }

        File[] files = dir.listFiles();
        return files == null || files.length == 0;
    }

    /**
     * 判断文件是否可读。
     *
     * @param file 文件对象
     * @return 是否可读
     */
    public static boolean isReadable(File file) {
        return file != null && file.exists() && file.canRead();
    }

    /**
     * 判断文件是否可写。
     *
     * @param file 文件对象
     * @return 是否可写
     */
    public static boolean isWritable(File file) {
        return file != null && file.exists() && file.canWrite();
    }

    // ============================== 文件对象与路径处理 ==============================

    /**
     * 根据路径创建 File 对象。
     *
     * @param path 文件路径
     * @return File 对象
     */
    public static File toFile(String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return FileUtil.file(path);
    }

    /**
     * 根据路径创建 Path 对象。
     *
     * @param path 文件路径
     * @return Path 对象
     */
    public static Path toPath(String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return Path.of(path);
    }

    /**
     * 获取文件绝对路径。
     *
     * @param path 文件路径
     * @return 绝对路径
     */
    public static String getAbsolutePath(String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return FileUtil.getAbsolutePath(path);
    }

    /**
     * 获取文件绝对路径。
     *
     * @param file 文件对象
     * @return 绝对路径
     */
    public static String getAbsolutePath(File file) {
        if (file == null) {
            return null;
        }
        return FileUtil.getAbsolutePath(file);
    }

    /**
     * 获取规范化路径。
     *
     * @param path 文件路径
     * @return 规范化路径
     */
    public static String normalizePath(String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return FileUtil.normalize(path);
    }

    /**
     * 拼接多个路径片段。
     *
     * @param paths 路径片段数组
     * @return 拼接后的路径
     */
    public static String pathJoin(String... paths) {
        if (paths == null || paths.length == 0) {
            return EMPTY;
        }
        return FileUtil.file(paths).getPath();
    }

    /**
     * 获取父级目录路径。
     *
     * @param path 文件路径
     * @return 父级目录路径
     */
    public static String getParentPath(String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }

        File parentFile = FileUtil.file(path).getParentFile();
        return parentFile == null ? null : parentFile.getPath();
    }

    /**
     * 获取父级目录对象。
     *
     * @param file 文件对象
     * @return 父级目录对象
     */
    public static File getParentFile(File file) {
        return file == null ? null : file.getParentFile();
    }

    /**
     * 获取文件名称。
     *
     * @param path 文件路径
     * @return 文件名称
     */
    public static String getFileName(String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return FileUtil.getName(path);
    }

    /**
     * 获取文件名称。
     *
     * @param file 文件对象
     * @return 文件名称
     */
    public static String getFileName(File file) {
        return file == null ? null : FileUtil.getName(file);
    }

    /**
     * 获取文件主名称，不包含扩展名。
     *
     * @param path 文件路径
     * @return 文件主名称
     */
    public static String getMainName(String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return FileUtil.mainName(path);
    }

    /**
     * 获取文件主名称，不包含扩展名。
     *
     * @param file 文件对象
     * @return 文件主名称
     */
    public static String getMainName(File file) {
        return file == null ? null : FileUtil.mainName(file);
    }

    /**
     * 获取文件扩展名。
     *
     * @param path 文件路径
     * @return 文件扩展名
     */
    public static String getExtName(String path) {
        if (StrUtil.isBlank(path)) {
            return EMPTY;
        }
        return FileUtil.extName(path);
    }

    /**
     * 获取文件扩展名。
     *
     * @param file 文件对象
     * @return 文件扩展名
     */
    public static String getExtName(File file) {
        return file == null ? EMPTY : FileUtil.extName(file);
    }

    /**
     * 判断文件扩展名是否匹配。
     *
     * @param path     文件路径
     * @param extNames 扩展名数组
     * @return 是否匹配
     */
    public static boolean isExtName(String path, String... extNames) {
        if (StrUtil.isBlank(path) || extNames == null || extNames.length == 0) {
            return false;
        }

        String extName = getExtName(path);
        for (String item : extNames) {
            if (StrUtil.equalsIgnoreCase(extName, removePrefix(item, "."))) {
                return true;
            }
        }
        return false;
    }

    // ============================== 文件创建 ==============================

    /**
     * 创建目录，目录已存在时直接返回。
     *
     * @param path 目录路径
     * @return 目录对象
     */
    public static File mkdir(String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return FileUtil.mkdir(path);
    }

    /**
     * 创建目录，目录已存在时直接返回。
     *
     * @param dir 目录对象
     * @return 目录对象
     */
    public static File mkdir(File dir) {
        if (dir == null) {
            return null;
        }
        return FileUtil.mkdir(dir);
    }

    /**
     * 创建文件，父级目录不存在时自动创建。
     *
     * @param path 文件路径
     * @return 文件对象
     */
    public static File touchFile(String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return FileUtil.touch(path);
    }

    /**
     * 创建文件，父级目录不存在时自动创建。
     *
     * @param file 文件对象
     * @return 文件对象
     */
    public static File touchFile(File file) {
        if (file == null) {
            return null;
        }
        return FileUtil.touch(file);
    }

    /**
     * 创建父级目录。
     *
     * @param file 文件对象
     * @return 父级目录对象
     */
    public static File mkdirParent(File file) {
        if (file == null) {
            return null;
        }

        File parentFile = file.getParentFile();
        if (parentFile != null) {
            FileUtil.mkdir(parentFile);
        }
        return parentFile;
    }

    /**
     * 创建临时文件。
     *
     * @param prefix 文件名前缀
     * @param suffix 文件名后缀
     * @return 临时文件对象
     */
    public static File createTempFile(String prefix, String suffix) {
        return FileUtil.createTempFile(defaultIfBlank(prefix, "tmp"), defaultIfBlank(suffix, ".tmp"), true);
    }

    /**
     * 在指定目录下创建临时文件。
     *
     * @param prefix 文件名前缀
     * @param suffix 文件名后缀
     * @param dir    临时文件目录
     * @return 临时文件对象
     */
    public static File createTempFile(String prefix, String suffix, File dir) {
        return FileUtil.createTempFile(defaultIfBlank(prefix, "tmp"), defaultIfBlank(suffix, ".tmp"), dir, true);
    }

    /**
     * 创建临时目录。
     *
     * @param prefix 目录名前缀
     * @return 临时目录对象
     */
    public static File createTempDir(String prefix) {
        File tempFile = createTempFile(defaultIfBlank(prefix, "tmp"), EMPTY);
        if (tempFile != null && tempFile.exists()) {
            FileUtil.del(tempFile);
            FileUtil.mkdir(tempFile);
        }
        return tempFile;
    }

    // ============================== 文件读写 ==============================

    /**
     * 使用 UTF-8 读取文本文件。
     *
     * @param path 文件路径
     * @return 文件内容
     */
    public static String readUtf8String(String path) {
        if (StrUtil.isBlank(path) || !FileUtil.exist(path)) {
            return null;
        }
        return FileUtil.readUtf8String(path);
    }

    /**
     * 使用 UTF-8 读取文本文件。
     *
     * @param file 文件对象
     * @return 文件内容
     */
    public static String readUtf8String(File file) {
        if (file == null || !FileUtil.exist(file)) {
            return null;
        }
        return FileUtil.readUtf8String(file);
    }

    /**
     * 使用指定字符集读取文本文件。
     *
     * @param path    文件路径
     * @param charset 字符集
     * @return 文件内容
     */
    public static String readString(String path, Charset charset) {
        if (StrUtil.isBlank(path) || !FileUtil.exist(path)) {
            return null;
        }
        return FileUtil.readString(path, charset == null ? DEFAULT_CHARSET : charset);
    }

    /**
     * 使用指定字符集读取文本文件。
     *
     * @param file    文件对象
     * @param charset 字符集
     * @return 文件内容
     */
    public static String readString(File file, Charset charset) {
        if (file == null || !FileUtil.exist(file)) {
            return null;
        }
        return FileUtil.readString(file, charset == null ? DEFAULT_CHARSET : charset);
    }

    /**
     * 使用 UTF-8 按行读取文本文件。
     *
     * @param path 文件路径
     * @return 文本行集合
     */
    public static List<String> readUtf8Lines(String path) {
        if (StrUtil.isBlank(path) || !FileUtil.exist(path)) {
            return new ArrayList<>();
        }
        return FileUtil.readUtf8Lines(path);
    }

    /**
     * 使用 UTF-8 按行读取文本文件。
     *
     * @param file 文件对象
     * @return 文本行集合
     */
    public static List<String> readUtf8Lines(File file) {
        if (file == null || !FileUtil.exist(file)) {
            return new ArrayList<>();
        }
        return FileUtil.readUtf8Lines(file);
    }

    /**
     * 使用指定字符集按行读取文本文件。
     *
     * @param path    文件路径
     * @param charset 字符集
     * @return 文本行集合
     */
    public static List<String> readLines(String path, Charset charset) {
        if (StrUtil.isBlank(path) || !FileUtil.exist(path)) {
            return new ArrayList<>();
        }
        return FileUtil.readLines(path, charset == null ? DEFAULT_CHARSET : charset);
    }

    /**
     * 使用 UTF-8 写入文本文件，文件已存在时覆盖。
     *
     * @param content 文件内容
     * @param path    文件路径
     * @return 写入后的文件对象
     */
    public static File writeUtf8String(String content, String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return FileUtil.writeUtf8String(defaultIfNull(content, EMPTY), path);
    }

    /**
     * 使用 UTF-8 写入文本文件，文件已存在时覆盖。
     *
     * @param content 文件内容
     * @param file    文件对象
     * @return 写入后的文件对象
     */
    public static File writeUtf8String(String content, File file) {
        if (file == null) {
            return null;
        }
        return FileUtil.writeUtf8String(defaultIfNull(content, EMPTY), file);
    }

    /**
     * 使用指定字符集写入文本文件，文件已存在时覆盖。
     *
     * @param content 文件内容
     * @param path    文件路径
     * @param charset 字符集
     * @return 写入后的文件对象
     */
    public static File writeString(String content, String path, Charset charset) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return FileUtil.writeString(defaultIfNull(content, EMPTY), path, charset == null ? DEFAULT_CHARSET : charset);
    }

    /**
     * 使用 UTF-8 追加文本内容。
     *
     * @param content 文件内容
     * @param path    文件路径
     * @return 写入后的文件对象
     */
    public static File appendUtf8String(String content, String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return FileUtil.appendUtf8String(defaultIfNull(content, EMPTY), path);
    }

    /**
     * 使用 UTF-8 追加文本内容。
     *
     * @param content 文件内容
     * @param file    文件对象
     * @return 写入后的文件对象
     */
    public static File appendUtf8String(String content, File file) {
        if (file == null) {
            return null;
        }
        return FileUtil.appendUtf8String(defaultIfNull(content, EMPTY), file);
    }

    /**
     * 使用指定字符集追加文本内容。
     *
     * @param content 文件内容
     * @param path    文件路径
     * @param charset 字符集
     * @return 写入后的文件对象
     */
    public static File appendString(String content, String path, Charset charset) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return FileUtil.appendString(defaultIfNull(content, EMPTY), path, charset == null ? DEFAULT_CHARSET : charset);
    }

    /**
     * 使用 UTF-8 写入文本行，文件已存在时覆盖。
     *
     * @param lines 文本行集合
     * @param path  文件路径
     * @return 写入后的文件对象
     */
    public static File writeUtf8Lines(Collection<String> lines, String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return FileUtil.writeUtf8Lines(lines == null ? new ArrayList<>() : lines, path);
    }

    /**
     * 使用 UTF-8 追加文本行。
     *
     * @param lines 文本行集合
     * @param path  文件路径
     * @return 写入后的文件对象
     */
    public static File appendUtf8Lines(Collection<String> lines, String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return FileUtil.appendUtf8Lines(lines == null ? new ArrayList<>() : lines, path);
    }

    // ============================== 字节与流处理 ==============================

    /**
     * 读取文件字节数组。
     *
     * @param path 文件路径
     * @return 字节数组
     */
    public static byte[] readBytes(String path) {
        if (StrUtil.isBlank(path) || !FileUtil.exist(path)) {
            return new byte[0];
        }
        return FileUtil.readBytes(path);
    }

    /**
     * 读取文件字节数组。
     *
     * @param file 文件对象
     * @return 字节数组
     */
    public static byte[] readBytes(File file) {
        if (file == null || !FileUtil.exist(file)) {
            return new byte[0];
        }
        return FileUtil.readBytes(file);
    }

    /**
     * 写入字节数组，文件已存在时覆盖。
     *
     * @param bytes 字节数组
     * @param path  文件路径
     * @return 写入后的文件对象
     */
    public static File writeBytes(byte[] bytes, String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return FileUtil.writeBytes(bytes == null ? new byte[0] : bytes, path);
    }

    /**
     * 写入字节数组，文件已存在时覆盖。
     *
     * @param bytes 字节数组
     * @param file  文件对象
     * @return 写入后的文件对象
     */
    public static File writeBytes(byte[] bytes, File file) {
        if (file == null) {
            return null;
        }
        return FileUtil.writeBytes(bytes == null ? new byte[0] : bytes, file);
    }

    /**
     * 将字符串转换为 UTF-8 输入流。
     *
     * @param value 字符串
     * @return 输入流
     */
    public static InputStream toInputStream(String value) {
        return IoUtil.toStream(defaultIfNull(value, EMPTY), DEFAULT_CHARSET);
    }

    /**
     * 将字符串转换为指定字符集输入流。
     *
     * @param value   字符串
     * @param charset 字符集
     * @return 输入流
     */
    public static InputStream toInputStream(String value, Charset charset) {
        return IoUtil.toStream(defaultIfNull(value, EMPTY), charset == null ? DEFAULT_CHARSET : charset);
    }

    /**
     * 将字节数组转换为输入流。
     *
     * @param bytes 字节数组
     * @return 输入流
     */
    public static InputStream toInputStream(byte[] bytes) {
        return new ByteArrayInputStream(bytes == null ? new byte[0] : bytes);
    }

    /**
     * 将输入流读取为 UTF-8 字符串。
     *
     * @param inputStream 输入流
     * @return 字符串
     */
    public static String readUtf8(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        return IoUtil.read(inputStream, DEFAULT_CHARSET);
    }

    /**
     * 将输入流读取为指定字符集字符串。
     *
     * @param inputStream 输入流
     * @param charset     字符集
     * @return 字符串
     */
    public static String read(InputStream inputStream, Charset charset) {
        if (inputStream == null) {
            return null;
        }
        return IoUtil.read(inputStream, charset == null ? DEFAULT_CHARSET : charset);
    }

    /**
     * 将输入流读取为字节数组。
     *
     * @param inputStream 输入流
     * @return 字节数组
     */
    public static byte[] readBytes(InputStream inputStream) {
        if (inputStream == null) {
            return new byte[0];
        }
        return IoUtil.readBytes(inputStream);
    }

    /**
     * 复制输入流到输出流。
     *
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @return 复制字节数
     */
    public static long copyStream(InputStream inputStream, OutputStream outputStream) {
        if (inputStream == null || outputStream == null) {
            return 0L;
        }
        return IoUtil.copy(inputStream, outputStream);
    }

    /**
     * 复制输入流到输出流，并指定缓冲区大小。
     *
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @param bufferSize   缓冲区大小
     * @return 复制字节数
     */
    public static long copyStream(InputStream inputStream, OutputStream outputStream, int bufferSize) {
        if (inputStream == null || outputStream == null) {
            return 0L;
        }
        return IoUtil.copy(inputStream, outputStream, Math.max(bufferSize, DEFAULT_IO_BUFFER_SIZE));
    }

    /**
     * 将输入流复制为字节数组。
     *
     * @param inputStream 输入流
     * @return 字节数组
     */
    public static byte[] copyToBytes(InputStream inputStream) {
        if (inputStream == null) {
            return new byte[0];
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IoUtil.copy(inputStream, outputStream);
        return outputStream.toByteArray();
    }

    /**
     * 安静关闭资源。
     *
     * @param closeable 可关闭资源
     */
    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (Exception ignored) {
            // 忽略关闭异常
        }
    }

    // ============================== 文件复制移动删除 ==============================

    /**
     * 复制文件或目录。
     *
     * @param sourcePath 源路径
     * @param targetPath 目标路径
     * @param override   是否覆盖
     * @return 目标文件对象
     */
    public static File copyFile(String sourcePath, String targetPath, boolean override) {
        if (StrUtil.isBlank(sourcePath) || StrUtil.isBlank(targetPath)) {
            return null;
        }
        return FileUtil.copy(sourcePath, targetPath, override);
    }

    /**
     * 复制文件或目录。
     *
     * @param sourceFile 源文件
     * @param targetFile 目标文件
     * @param override   是否覆盖
     * @return 目标文件对象
     */
    public static File copyFile(File sourceFile, File targetFile, boolean override) {
        if (sourceFile == null || targetFile == null) {
            return null;
        }
        return FileUtil.copy(sourceFile, targetFile, override);
    }

    /**
     * 使用 NIO 复制文件。
     *
     * @param sourcePath 源路径
     * @param targetPath 目标路径
     * @param override   是否覆盖
     * @return 目标路径
     */
    public static Path copyFileByNio(Path sourcePath, Path targetPath, boolean override) {
        if (sourcePath == null || targetPath == null) {
            return null;
        }

        try {
            Path parent = targetPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (override) {
                return Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return Files.copy(sourcePath, targetPath);
        } catch (Exception e) {
            throw new IllegalStateException("文件复制失败：" + sourcePath + " -> " + targetPath, e);
        }
    }

    /**
     * 移动文件或目录。
     *
     * @param sourceFile 源文件
     * @param targetFile 目标文件
     * @param override   是否覆盖
     * @return 目标文件对象
     */
    public static File moveFile(File sourceFile, File targetFile, boolean override) {
        if (sourceFile == null || targetFile == null) {
            return null;
        }

        File parentFile = targetFile.getParentFile();
        if (parentFile != null) {
            FileUtil.mkdir(parentFile);
        }

        FileUtil.move(sourceFile, targetFile, override);
        return targetFile;
    }

    /**
     * 使用 NIO 移动文件。
     *
     * @param sourcePath 源路径
     * @param targetPath 目标路径
     * @param override   是否覆盖
     * @return 目标路径
     */
    public static Path moveFileByNio(Path sourcePath, Path targetPath, boolean override) {
        if (sourcePath == null || targetPath == null) {
            return null;
        }

        try {
            Path parent = targetPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (override) {
                return Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return Files.move(sourcePath, targetPath);
        } catch (Exception e) {
            throw new IllegalStateException("文件移动失败：" + sourcePath + " -> " + targetPath, e);
        }
    }

    /**
     * 重命名文件。
     *
     * @param file       文件对象
     * @param newName    新文件名
     * @param isOverride 是否覆盖
     * @return 重命名后的文件对象
     */
    public static File renameFile(File file, String newName, boolean isOverride) {
        if (file == null || StrUtil.isBlank(newName)) {
            return null;
        }
        return FileUtil.rename(file, newName, isOverride);
    }

    /**
     * 删除文件或目录。
     *
     * @param path 文件路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String path) {
        return StrUtil.isNotBlank(path) && FileUtil.del(path);
    }

    /**
     * 删除文件或目录。
     *
     * @param file 文件对象
     * @return 是否删除成功
     */
    public static boolean deleteFile(File file) {
        return file != null && FileUtil.del(file);
    }

    /**
     * 清空目录内容。
     *
     * @param dir 目录对象
     * @return 是否清空成功
     */
    public static boolean cleanDirectory(File dir) {
        if (dir == null || !FileUtil.isDirectory(dir)) {
            return false;
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return true;
        }

        boolean success = true;
        for (File file : files) {
            success = FileUtil.del(file) && success;
        }
        return success;
    }

    // ============================== 文件大小 ==============================

    /**
     * 获取文件或目录大小。
     *
     * @param path 文件路径
     * @return 字节大小
     */
    public static long fileSize(String path) {
        if (StrUtil.isBlank(path) || !FileUtil.exist(path)) {
            return 0L;
        }
        return FileUtil.size(FileUtil.file(path));
    }

    /**
     * 获取文件或目录大小。
     *
     * @param file 文件对象
     * @return 字节大小
     */
    public static long fileSize(File file) {
        if (file == null || !FileUtil.exist(file)) {
            return 0L;
        }
        return FileUtil.size(file);
    }

    /**
     * 格式化文件大小。
     *
     * @param size 文件字节数
     * @return 格式化后的文件大小
     */
    public static String formatFileSize(long size) {
        return FileUtil.readableFileSize(Math.max(size, 0L));
    }

    /**
     * 获取文件大小并格式化。
     *
     * @param file 文件对象
     * @return 格式化后的文件大小
     */
    public static String formatFileSize(File file) {
        return formatFileSize(fileSize(file));
    }

    /**
     * 获取文件大小并格式化。
     *
     * @param path 文件路径
     * @return 格式化后的文件大小
     */
    public static String formatFileSize(String path) {
        return formatFileSize(fileSize(path));
    }

    // ============================== 文件列表 ==============================

    /**
     * 获取目录下的文件和目录集合。
     *
     * @param dirPath 目录路径
     * @return 文件集合
     */
    public static List<File> listFiles(String dirPath) {
        if (StrUtil.isBlank(dirPath)) {
            return new ArrayList<>();
        }
        return listFiles(FileUtil.file(dirPath));
    }

    /**
     * 获取目录下的文件和目录集合。
     *
     * @param dir 目录对象
     * @return 文件集合
     */
    public static List<File> listFiles(File dir) {
        List<File> result = new ArrayList<>();
        if (dir == null || !FileUtil.isDirectory(dir)) {
            return result;
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return result;
        }

        result.addAll(Arrays.asList(files));
        return result;
    }

    /**
     * 获取目录下的普通文件集合。
     *
     * @param dir 目录对象
     * @return 普通文件集合
     */
    public static List<File> listOnlyFiles(File dir) {
        List<File> result = new ArrayList<>();
        if (dir == null || !FileUtil.isDirectory(dir)) {
            return result;
        }

        File[] files = dir.listFiles(File::isFile);
        if (files == null || files.length == 0) {
            return result;
        }

        result.addAll(Arrays.asList(files));
        return result;
    }

    /**
     * 获取目录下的子目录集合。
     *
     * @param dir 目录对象
     * @return 子目录集合
     */
    public static List<File> listOnlyDirectories(File dir) {
        List<File> result = new ArrayList<>();
        if (dir == null || !FileUtil.isDirectory(dir)) {
            return result;
        }

        File[] files = dir.listFiles(File::isDirectory);
        if (files == null || files.length == 0) {
            return result;
        }

        result.addAll(Arrays.asList(files));
        return result;
    }

    /**
     * 获取目录下指定扩展名的文件集合。
     *
     * @param dir      目录对象
     * @param extNames 扩展名数组
     * @return 文件集合
     */
    public static List<File> listFilesByExt(File dir, String... extNames) {
        List<File> result = new ArrayList<>();
        if (dir == null || !FileUtil.isDirectory(dir) || extNames == null || extNames.length == 0) {
            return result;
        }

        File[] files = dir.listFiles(file -> file.isFile() && isExtName(file.getName(), extNames));
        if (files == null || files.length == 0) {
            return result;
        }

        result.addAll(Arrays.asList(files));
        return result;
    }

    /**
     * 递归获取目录下所有文件。
     *
     * @param dir 目录对象
     * @return 文件集合
     */
    public static List<File> loopFiles(File dir) {
        if (dir == null || !FileUtil.isDirectory(dir)) {
            return new ArrayList<>();
        }
        return FileUtil.loopFiles(dir);
    }

    /**
     * 递归获取目录下指定扩展名的所有文件。
     *
     * @param dir      目录对象
     * @param extNames 扩展名数组
     * @return 文件集合
     */
    public static List<File> loopFilesByExt(File dir, String... extNames) {
        List<File> files = loopFiles(dir);
        if (CollUtil.isEmpty(files) || extNames == null || extNames.length == 0) {
            return files;
        }

        return filter(files, file -> file != null && isExtName(file.getName(), extNames));
    }

    // ============================== 文件类型判断 ==============================

    /**
     * 根据文件内容获取文件类型。
     *
     * @param file 文件对象
     * @return 文件类型
     */
    public static String getFileType(File file) {
        if (file == null || !FileUtil.isFile(file)) {
            return null;
        }

        try {
            return FileTypeUtil.getType(file);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断文件是否为图片类型。
     *
     * @param file 文件对象
     * @return 是否为图片类型
     */
    public static boolean isImageFile(File file) {
        String extName = getExtName(file);
        return equalsAny(toLowerCase(extName), "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg");
    }

    /**
     * 判断文件是否为文档类型。
     *
     * @param file 文件对象
     * @return 是否为文档类型
     */
    public static boolean isDocumentFile(File file) {
        String extName = getExtName(file);
        return equalsAny(toLowerCase(extName), "txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "md", "csv");
    }

    /**
     * 判断文件是否为压缩包类型。
     *
     * @param file 文件对象
     * @return 是否为压缩包类型
     */
    public static boolean isArchiveFile(File file) {
        String extName = getExtName(file);
        return equalsAny(toLowerCase(extName), "zip", "rar", "7z", "tar", "gz", "bz2", "xz");
    }

    /**
     * 判断文件是否为视频类型。
     *
     * @param file 文件对象
     * @return 是否为视频类型
     */
    public static boolean isVideoFile(File file) {
        String extName = getExtName(file);
        return equalsAny(toLowerCase(extName), "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm");
    }

    /**
     * 判断文件是否为音频类型。
     *
     * @param file 文件对象
     * @return 是否为音频类型
     */
    public static boolean isAudioFile(File file) {
        String extName = getExtName(file);
        return equalsAny(toLowerCase(extName), "mp3", "wav", "aac", "flac", "ogg", "m4a");
    }

    // ============================== 文件名称处理 ==============================

    /**
     * 生成唯一文件名。
     *
     * @param originalName 原文件名
     * @return 唯一文件名
     */
    public static String uniqueFileName(String originalName) {
        String extName = getExtName(originalName);
        String uuid = simpleUuid();
        if (StrUtil.isBlank(extName)) {
            return uuid;
        }
        return uuid + "." + extName;
    }

    /**
     * 生成带时间戳的文件名。
     *
     * @param originalName 原文件名
     * @return 带时间戳的文件名
     */
    public static String timestampFileName(String originalName) {
        String mainName = defaultIfBlank(getMainName(originalName), "file");
        String extName = getExtName(originalName);
        String timestamp = String.valueOf(currentTimestamp());

        if (StrUtil.isBlank(extName)) {
            return mainName + "_" + timestamp;
        }
        return mainName + "_" + timestamp + "." + extName;
    }

    /**
     * 清理文件名中的非法字符。
     *
     * @param fileName 文件名
     * @return 清理后的文件名
     */
    public static String cleanFileName(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return EMPTY;
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 限制文件名长度。
     *
     * @param fileName  文件名
     * @param maxLength 最大长度
     * @return 限制后的文件名
     */
    public static String limitFileName(String fileName, int maxLength) {
        if (StrUtil.isBlank(fileName)) {
            return EMPTY;
        }
        if (maxLength <= 0 || fileName.length() <= maxLength) {
            return fileName;
        }

        String mainName = getMainName(fileName);
        String extName = getExtName(fileName);
        if (StrUtil.isBlank(extName)) {
            return truncate(fileName, maxLength);
        }

        int extLength = extName.length() + 1;
        int mainMaxLength = maxLength - extLength;
        if (mainMaxLength <= 0) {
            return truncate(fileName, maxLength);
        }
        return truncate(mainName, mainMaxLength) + "." + extName;
    }

    // ============================== MD5 摘要 ==============================

    /**
     * 计算字符串 MD5 摘要。
     *
     * @param value 字符串
     * @return MD5 摘要字符串
     */
    public static String md5(String value) {
        if (value == null) {
            return null;
        }
        return SecureUtil.md5(value);
    }

    /**
     * 计算文件 MD5 摘要。
     *
     * @param file 文件对象
     * @return MD5 摘要字符串
     */
    public static String md5(File file) {
        if (file == null || !FileUtil.isFile(file)) {
            return null;
        }
        return SecureUtil.md5(file);
    }

    /**
     * 计算字节数组 MD5 摘要。
     *
     * @param bytes 字节数组
     * @return MD5 摘要字符串
     */
    public static String md5(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return new Digester(DigestAlgorithm.MD5).digestHex(bytes);
    }

    /**
     * 判断字符串与 MD5 摘要是否匹配。
     *
     * @param value 字符串
     * @param md5   MD5 摘要字符串
     * @return 是否匹配
     */
    public static boolean md5Equals(String value, String md5) {
        if (value == null || StrUtil.isBlank(md5)) {
            return false;
        }
        return StrUtil.equalsIgnoreCase(md5(value), md5);
    }

    /**
     * 判断文件与 MD5 摘要是否匹配。
     *
     * @param file 文件对象
     * @param md5  MD5 摘要字符串
     * @return 是否匹配
     */
    public static boolean md5Equals(File file, String md5) {
        if (file == null || StrUtil.isBlank(md5)) {
            return false;
        }
        return StrUtil.equalsIgnoreCase(md5(file), md5);
    }

    // ============================== SHA 摘要 ==============================

    /**
     * 计算字符串 SHA-1 摘要。
     *
     * @param value 字符串
     * @return SHA-1 摘要字符串
     */
    public static String sha1(String value) {
        if (value == null) {
            return null;
        }
        return new Digester(DigestAlgorithm.SHA1).digestHex(value, DEFAULT_CHARSET);
    }

    /**
     * 计算字节数组 SHA-1 摘要。
     *
     * @param bytes 字节数组
     * @return SHA-1 摘要字符串
     */
    public static String sha1(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return new Digester(DigestAlgorithm.SHA1).digestHex(bytes);
    }

    /**
     * 计算文件 SHA-1 摘要。
     *
     * @param file 文件对象
     * @return SHA-1 摘要字符串
     */
    public static String sha1(File file) {
        if (file == null || !FileUtil.isFile(file)) {
            return null;
        }
        return new Digester(DigestAlgorithm.SHA1).digestHex(file);
    }

    /**
     * 计算字符串 SHA-256 摘要。
     *
     * @param value 字符串
     * @return SHA-256 摘要字符串
     */
    public static String sha256(String value) {
        if (value == null) {
            return null;
        }
        return new Digester(DigestAlgorithm.SHA256).digestHex(value, DEFAULT_CHARSET);
    }

    /**
     * 计算字节数组 SHA-256 摘要。
     *
     * @param bytes 字节数组
     * @return SHA-256 摘要字符串
     */
    public static String sha256(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return new Digester(DigestAlgorithm.SHA256).digestHex(bytes);
    }

    /**
     * 计算文件 SHA-256 摘要。
     *
     * @param file 文件对象
     * @return SHA-256 摘要字符串
     */
    public static String sha256(File file) {
        if (file == null || !FileUtil.isFile(file)) {
            return null;
        }
        return new Digester(DigestAlgorithm.SHA256).digestHex(file);
    }

    /**
     * 计算字符串 SHA-384 摘要。
     *
     * @param value 字符串
     * @return SHA-384 摘要字符串
     */
    public static String sha384(String value) {
        if (value == null) {
            return null;
        }
        return new Digester(DigestAlgorithm.SHA384).digestHex(value, DEFAULT_CHARSET);
    }

    /**
     * 计算字节数组 SHA-384 摘要。
     *
     * @param bytes 字节数组
     * @return SHA-384 摘要字符串
     */
    public static String sha384(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return new Digester(DigestAlgorithm.SHA384).digestHex(bytes);
    }

    /**
     * 计算文件 SHA-384 摘要。
     *
     * @param file 文件对象
     * @return SHA-384 摘要字符串
     */
    public static String sha384(File file) {
        if (file == null || !FileUtil.isFile(file)) {
            return null;
        }
        return new Digester(DigestAlgorithm.SHA384).digestHex(file);
    }

    /**
     * 计算字符串 SHA-512 摘要。
     *
     * @param value 字符串
     * @return SHA-512 摘要字符串
     */
    public static String sha512(String value) {
        if (value == null) {
            return null;
        }
        return new Digester(DigestAlgorithm.SHA512).digestHex(value, DEFAULT_CHARSET);
    }

    /**
     * 计算字节数组 SHA-512 摘要。
     *
     * @param bytes 字节数组
     * @return SHA-512 摘要字符串
     */
    public static String sha512(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return new Digester(DigestAlgorithm.SHA512).digestHex(bytes);
    }

    /**
     * 计算文件 SHA-512 摘要。
     *
     * @param file 文件对象
     * @return SHA-512 摘要字符串
     */
    public static String sha512(File file) {
        if (file == null || !FileUtil.isFile(file)) {
            return null;
        }
        return new Digester(DigestAlgorithm.SHA512).digestHex(file);
    }

    // ============================== Base64 编码 ==============================

    /**
     * 将字符串进行 Base64 编码。
     *
     * @param value 字符串
     * @return Base64 字符串
     */
    public static String base64Encode(String value) {
        if (value == null) {
            return null;
        }
        return Base64.encode(value, DEFAULT_CHARSET);
    }

    /**
     * 将字符串按指定字符集进行 Base64 编码。
     *
     * @param value   字符串
     * @param charset 字符集
     * @return Base64 字符串
     */
    public static String base64Encode(String value, Charset charset) {
        if (value == null) {
            return null;
        }
        return Base64.encode(value, charset == null ? DEFAULT_CHARSET : charset);
    }

    /**
     * 将字节数组进行 Base64 编码。
     *
     * @param bytes 字节数组
     * @return Base64 字符串
     */
    public static String base64Encode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return Base64.encode(bytes);
    }

    /**
     * 将文件内容进行 Base64 编码。
     *
     * @param file 文件对象
     * @return Base64 字符串
     */
    public static String base64Encode(File file) {
        if (file == null || !FileUtil.isFile(file)) {
            return null;
        }
        return Base64.encode(FileUtil.readBytes(file));
    }

    /**
     * 将 Base64 字符串解码为 UTF-8 字符串。
     *
     * @param base64 Base64 字符串
     * @return 解码后的字符串
     */
    public static String base64DecodeStr(String base64) {
        if (StrUtil.isBlank(base64)) {
            return EMPTY;
        }
        return Base64.decodeStr(base64, DEFAULT_CHARSET);
    }

    /**
     * 将 Base64 字符串按指定字符集解码为字符串。
     *
     * @param base64  Base64 字符串
     * @param charset 字符集
     * @return 解码后的字符串
     */
    public static String base64DecodeStr(String base64, Charset charset) {
        if (StrUtil.isBlank(base64)) {
            return EMPTY;
        }
        return Base64.decodeStr(base64, charset == null ? DEFAULT_CHARSET : charset);
    }

    /**
     * 将 Base64 字符串解码为字节数组。
     *
     * @param base64 Base64 字符串
     * @return 字节数组
     */
    public static byte[] base64Decode(String base64) {
        if (StrUtil.isBlank(base64)) {
            return new byte[0];
        }
        return Base64.decode(base64);
    }

    /**
     * 将 Base64 字符串解码并写入文件。
     *
     * @param base64 Base64 字符串
     * @param file   文件对象
     * @return 写入后的文件对象
     */
    public static File base64DecodeToFile(String base64, File file) {
        if (StrUtil.isBlank(base64) || file == null) {
            return null;
        }
        return FileUtil.writeBytes(Base64.decode(base64), file);
    }

    /**
     * 判断字符串是否为 Base64 编码字符串。
     *
     * @param value 字符串
     * @return 是否为 Base64 编码字符串
     */
    public static boolean isBase64(String value) {
        return StrUtil.isNotBlank(value) && Base64.isBase64(value);
    }

    // ============================== Hex 编码 ==============================

    /**
     * 将字符串转换为 Hex 字符串。
     *
     * @param value 字符串
     * @return Hex 字符串
     */
    public static String hexEncode(String value) {
        if (value == null) {
            return null;
        }
        return HexUtil.encodeHexStr(value.getBytes(DEFAULT_CHARSET));
    }

    /**
     * 将字符串按指定字符集转换为 Hex 字符串。
     *
     * @param value   字符串
     * @param charset 字符集
     * @return Hex 字符串
     */
    public static String hexEncode(String value, Charset charset) {
        if (value == null) {
            return null;
        }
        Charset actualCharset = charset == null ? DEFAULT_CHARSET : charset;
        return HexUtil.encodeHexStr(value.getBytes(actualCharset));
    }

    /**
     * 将字节数组转换为 Hex 字符串。
     *
     * @param bytes 字节数组
     * @return Hex 字符串
     */
    public static String hexEncode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return HexUtil.encodeHexStr(bytes);
    }

    /**
     * 将 Hex 字符串解码为 UTF-8 字符串。
     *
     * @param hex Hex 字符串
     * @return 字符串
     */
    public static String hexDecodeStr(String hex) {
        if (StrUtil.isBlank(hex)) {
            return EMPTY;
        }
        return new String(HexUtil.decodeHex(hex), DEFAULT_CHARSET);
    }

    /**
     * 将 Hex 字符串按指定字符集解码为字符串。
     *
     * @param hex     Hex 字符串
     * @param charset 字符集
     * @return 字符串
     */
    public static String hexDecodeStr(String hex, Charset charset) {
        if (StrUtil.isBlank(hex)) {
            return EMPTY;
        }
        Charset actualCharset = charset == null ? DEFAULT_CHARSET : charset;
        return new String(HexUtil.decodeHex(hex), actualCharset);
    }

    /**
     * 将 Hex 字符串解码为字节数组。
     *
     * @param hex Hex 字符串
     * @return 字节数组
     */
    public static byte[] hexDecode(String hex) {
        if (StrUtil.isBlank(hex)) {
            return new byte[0];
        }
        return HexUtil.decodeHex(hex);
    }

    /**
     * 判断字符串是否为 Hex 字符串。
     *
     * @param value 字符串
     * @return 是否为 Hex 字符串
     */
    public static boolean isHex(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^[0-9a-fA-F]+$", value) && value.length() % 2 == 0;
    }

    // ============================== URL 编码 ==============================

    /**
     * 对 URL 字符串进行 UTF-8 编码。
     *
     * @param value URL 字符串
     * @return 编码后的字符串
     */
    public static String urlEncode(String value) {
        if (value == null) {
            return null;
        }
        return URLUtil.encode(value, DEFAULT_CHARSET);
    }

    /**
     * 对 URL 字符串按指定字符集编码。
     *
     * @param value   URL 字符串
     * @param charset 字符集
     * @return 编码后的字符串
     */
    public static String urlEncode(String value, Charset charset) {
        if (value == null) {
            return null;
        }
        return URLUtil.encode(value, charset == null ? DEFAULT_CHARSET : charset);
    }

    /**
     * 对 URL 字符串进行 UTF-8 解码。
     *
     * @param value URL 字符串
     * @return 解码后的字符串
     */
    public static String urlDecode(String value) {
        if (value == null) {
            return null;
        }
        return URLUtil.decode(value, DEFAULT_CHARSET);
    }

    /**
     * 对 URL 字符串按指定字符集解码。
     *
     * @param value   URL 字符串
     * @param charset 字符集
     * @return 解码后的字符串
     */
    public static String urlDecode(String value, Charset charset) {
        if (value == null) {
            return null;
        }
        return URLUtil.decode(value, charset == null ? DEFAULT_CHARSET : charset);
    }

    // ============================== AES 密钥 ==============================

    /**
     * 生成 AES-128 密钥字节数组。
     *
     * @return AES-128 密钥字节数组
     */
    public static byte[] generateAes128Key() {
        return generateAesKey(128);
    }

    /**
     * 生成 AES-192 密钥字节数组。
     *
     * @return AES-192 密钥字节数组
     */
    public static byte[] generateAes192Key() {
        return generateAesKey(192);
    }

    /**
     * 生成 AES-256 密钥字节数组。
     *
     * @return AES-256 密钥字节数组
     */
    public static byte[] generateAes256Key() {
        return generateAesKey(256);
    }

    /**
     * 生成指定长度的 AES 密钥字节数组。
     *
     * @param bitLength 密钥位数，支持 128、192、256
     * @return AES 密钥字节数组
     */
    public static byte[] generateAesKey(int bitLength) {
        if (bitLength != 128 && bitLength != 192 && bitLength != 256) {
            throw new IllegalArgumentException("AES密钥长度仅支持128、192、256位");
        }

        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(bitLength, new SecureRandom());
            return keyGenerator.generateKey().getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("AES密钥生成失败", e);
        }
    }

    /**
     * 生成 AES-128 密钥 Base64 字符串。
     *
     * @return AES-128 密钥 Base64 字符串
     */
    public static String generateAes128KeyBase64() {
        return base64Encode(generateAes128Key());
    }

    /**
     * 生成 AES-192 密钥 Base64 字符串。
     *
     * @return AES-192 密钥 Base64 字符串
     */
    public static String generateAes192KeyBase64() {
        return base64Encode(generateAes192Key());
    }

    /**
     * 生成 AES-256 密钥 Base64 字符串。
     *
     * @return AES-256 密钥 Base64 字符串
     */
    public static String generateAes256KeyBase64() {
        return base64Encode(generateAes256Key());
    }

    /**
     * 根据字符串生成固定长度 AES-128 密钥。
     *
     * @param key 密钥字符串
     * @return AES-128 密钥字节数组
     */
    public static byte[] aes128Key(String key) {
        return normalizeAesKey(key, AES_KEY_128_LENGTH);
    }

    /**
     * 根据字符串生成固定长度 AES-192 密钥。
     *
     * @param key 密钥字符串
     * @return AES-192 密钥字节数组
     */
    public static byte[] aes192Key(String key) {
        return normalizeAesKey(key, AES_KEY_192_LENGTH);
    }

    /**
     * 根据字符串生成固定长度 AES-256 密钥。
     *
     * @param key 密钥字符串
     * @return AES-256 密钥字节数组
     */
    public static byte[] aes256Key(String key) {
        return normalizeAesKey(key, AES_KEY_256_LENGTH);
    }

    /**
     * 校验 AES 密钥长度是否合法。
     *
     * @param key 密钥字节数组
     * @return 是否合法
     */
    public static boolean isValidAesKey(byte[] key) {
        if (key == null) {
            return false;
        }
        return key.length == AES_KEY_128_LENGTH || key.length == AES_KEY_192_LENGTH || key.length == AES_KEY_256_LENGTH;
    }

    // ============================== AES 加密解密 ==============================

    /**
     * 使用 AES 加密字符串并输出 Base64 字符串。
     *
     * @param content 明文内容
     * @param key     AES 密钥字节数组
     * @return Base64 密文
     */
    public static String aesEncryptBase64(String content, byte[] key) {
        if (content == null) {
            return null;
        }
        AES aes = createAes(key);
        return aes.encryptBase64(content, DEFAULT_CHARSET);
    }

    /**
     * 使用 AES 加密字符串并输出 Hex 字符串。
     *
     * @param content 明文内容
     * @param key     AES 密钥字节数组
     * @return Hex 密文
     */
    public static String aesEncryptHex(String content, byte[] key) {
        if (content == null) {
            return null;
        }
        AES aes = createAes(key);
        return aes.encryptHex(content, DEFAULT_CHARSET);
    }

    /**
     * 使用 AES 加密字节数组。
     *
     * @param content 明文字节数组
     * @param key     AES 密钥字节数组
     * @return 密文字节数组
     */
    public static byte[] aesEncrypt(byte[] content, byte[] key) {
        if (content == null) {
            return new byte[0];
        }
        AES aes = createAes(key);
        return aes.encrypt(content);
    }

    /**
     * 解密 AES Base64 密文为字符串。
     *
     * @param base64CipherText Base64 密文
     * @param key              AES 密钥字节数组
     * @return 明文字符串
     */
    public static String aesDecryptBase64(String base64CipherText, byte[] key) {
        if (StrUtil.isBlank(base64CipherText)) {
            return EMPTY;
        }
        AES aes = createAes(key);
        return aes.decryptStr(Base64.decode(base64CipherText), DEFAULT_CHARSET);
    }

    /**
     * 解密 AES Hex 密文为字符串。
     *
     * @param hexCipherText Hex 密文
     * @param key           AES 密钥字节数组
     * @return 明文字符串
     */
    public static String aesDecryptHex(String hexCipherText, byte[] key) {
        if (StrUtil.isBlank(hexCipherText)) {
            return EMPTY;
        }
        AES aes = createAes(key);
        return aes.decryptStr(HexUtil.decodeHex(hexCipherText), DEFAULT_CHARSET);
    }

    /**
     * 解密 AES 密文字节数组。
     *
     * @param cipherBytes 密文字节数组
     * @param key         AES 密钥字节数组
     * @return 明文字节数组
     */
    public static byte[] aesDecrypt(byte[] cipherBytes, byte[] key) {
        if (cipherBytes == null) {
            return new byte[0];
        }
        AES aes = createAes(key);
        return aes.decrypt(cipherBytes);
    }

    /**
     * 使用字符串密钥加密内容并输出 Base64 密文。
     *
     * @param content 明文内容
     * @param key     密钥字符串
     * @return Base64 密文
     */
    public static String aesEncryptBase64(String content, String key) {
        return aesEncryptBase64(content, aes128Key(key));
    }

    /**
     * 使用字符串密钥解密 Base64 密文。
     *
     * @param base64CipherText Base64 密文
     * @param key              密钥字符串
     * @return 明文内容
     */
    public static String aesDecryptBase64(String base64CipherText, String key) {
        return aesDecryptBase64(base64CipherText, aes128Key(key));
    }

    /**
     * 使用字符串密钥加密内容并输出 Hex 密文。
     *
     * @param content 明文内容
     * @param key     密钥字符串
     * @return Hex 密文
     */
    public static String aesEncryptHex(String content, String key) {
        return aesEncryptHex(content, aes128Key(key));
    }

    /**
     * 使用字符串密钥解密 Hex 密文。
     *
     * @param hexCipherText Hex 密文
     * @param key           密钥字符串
     * @return 明文内容
     */
    public static String aesDecryptHex(String hexCipherText, String key) {
        return aesDecryptHex(hexCipherText, aes128Key(key));
    }

    // ============================== UUID 与随机编码 ==============================

    /**
     * 生成无横线 UUID。
     *
     * @return 无横线 UUID
     */
    public static String uuidSimple() {
        return IdUtil.fastSimpleUUID();
    }

    /**
     * 生成带横线 UUID。
     *
     * @return 带横线 UUID
     */
    public static String uuidWithDash() {
        return IdUtil.fastUUID();
    }

    /**
     * 生成随机 Token。
     *
     * @param length 长度
     * @return 随机 Token
     */
    public static String randomToken(int length) {
        return randomLettersNumbers(length);
    }

    /**
     * 生成随机数字验证码。
     *
     * @param length 长度
     * @return 随机数字验证码
     */
    public static String randomCode(int length) {
        return randomNumbers(length);
    }

    /**
     * 生成随机盐值。
     *
     * @param length 长度
     * @return 随机盐值
     */
    public static String randomSalt(int length) {
        return randomLettersNumbers(length);
    }

    // ============================== 加密编码内部辅助方法 ==============================

    /**
     * 创建 AES 加密器。
     *
     * @param key AES 密钥字节数组
     * @return AES 加密器
     */
    private static AES createAes(byte[] key) {
        if (!isValidAesKey(key)) {
            throw new IllegalArgumentException("AES密钥长度必须为16、24或32字节");
        }
        return SecureUtil.aes(key);
    }

    /**
     * 根据字符串归一化生成 AES 密钥。
     *
     * @param key    密钥字符串
     * @param length 密钥字节长度
     * @return AES 密钥字节数组
     */
    private static byte[] normalizeAesKey(String key, int length) {
        if (StrUtil.isBlank(key)) {
            throw new IllegalArgumentException("AES密钥字符串不能为空");
        }
        if (length != AES_KEY_128_LENGTH && length != AES_KEY_192_LENGTH && length != AES_KEY_256_LENGTH) {
            throw new IllegalArgumentException("AES密钥长度必须为16、24或32字节");
        }

        String sha256 = sha256(key);
        byte[] source = sha256.getBytes(DEFAULT_CHARSET);
        byte[] result = new byte[length];
        System.arraycopy(source, 0, result, 0, length);
        return result;
    }

    // ============================== 请求基础信息 ==============================

    /**
     * 获取请求方法。
     *
     * @param request HTTP 请求对象
     * @return 请求方法
     */
    public static String getRequestMethod(HttpServletRequest request) {
        return request == null ? null : request.getMethod();
    }

    /**
     * 判断是否为 GET 请求。
     *
     * @param request HTTP 请求对象
     * @return 是否为 GET 请求
     */
    public static boolean isGetRequest(HttpServletRequest request) {
        return StrUtil.equalsIgnoreCase(getRequestMethod(request), "GET");
    }

    /**
     * 判断是否为 POST 请求。
     *
     * @param request HTTP 请求对象
     * @return 是否为 POST 请求
     */
    public static boolean isPostRequest(HttpServletRequest request) {
        return StrUtil.equalsIgnoreCase(getRequestMethod(request), "POST");
    }

    /**
     * 判断是否为 PUT 请求。
     *
     * @param request HTTP 请求对象
     * @return 是否为 PUT 请求
     */
    public static boolean isPutRequest(HttpServletRequest request) {
        return StrUtil.equalsIgnoreCase(getRequestMethod(request), "PUT");
    }

    /**
     * 判断是否为 DELETE 请求。
     *
     * @param request HTTP 请求对象
     * @return 是否为 DELETE 请求
     */
    public static boolean isDeleteRequest(HttpServletRequest request) {
        return StrUtil.equalsIgnoreCase(getRequestMethod(request), "DELETE");
    }

    /**
     * 判断是否为 PATCH 请求。
     *
     * @param request HTTP 请求对象
     * @return 是否为 PATCH 请求
     */
    public static boolean isPatchRequest(HttpServletRequest request) {
        return StrUtil.equalsIgnoreCase(getRequestMethod(request), "PATCH");
    }

    /**
     * 获取请求 URI。
     *
     * @param request HTTP 请求对象
     * @return 请求 URI
     */
    public static String getRequestUri(HttpServletRequest request) {
        return request == null ? null : request.getRequestURI();
    }

    /**
     * 获取请求 URL。
     *
     * @param request HTTP 请求对象
     * @return 请求 URL
     */
    public static String getRequestUrl(HttpServletRequest request) {
        return request == null ? null : request.getRequestURL().toString();
    }

    /**
     * 获取完整请求 URL，包含 query string。
     *
     * @param request HTTP 请求对象
     * @return 完整请求 URL
     */
    public static String getFullRequestUrl(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String url = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        if (StrUtil.isBlank(queryString)) {
            return url;
        }
        return url + "?" + queryString;
    }

    /**
     * 获取请求上下文路径。
     *
     * @param request HTTP 请求对象
     * @return 上下文路径
     */
    public static String getContextPath(HttpServletRequest request) {
        return request == null ? null : request.getContextPath();
    }

    /**
     * 获取请求来源地址。
     *
     * @param request HTTP 请求对象
     * @return 来源地址
     */
    public static String getReferer(HttpServletRequest request) {
        return getHeader(request, "Referer");
    }

    /**
     * 判断是否为 Ajax 请求。
     *
     * @param request HTTP 请求对象
     * @return 是否为 Ajax 请求
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        return StrUtil.equalsIgnoreCase(getHeader(request, "X-Requested-With"), "XMLHttpRequest");
    }

    /**
     * 判断是否为 HTTPS 请求。
     *
     * @param request HTTP 请求对象
     * @return 是否为 HTTPS 请求
     */
    public static boolean isHttpsRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        String forwardedProto = getHeader(request, "X-Forwarded-Proto");
        if (StrUtil.isNotBlank(forwardedProto)) {
            return StrUtil.equalsIgnoreCase(forwardedProto, "https");
        }
        return request.isSecure() || StrUtil.equalsIgnoreCase(request.getScheme(), "https");
    }

    // ============================== 请求参数处理 ==============================

    /**
     * 获取请求参数。
     *
     * @param request HTTP 请求对象
     * @param name    参数名称
     * @return 参数值
     */
    public static String getParam(HttpServletRequest request, String name) {
        if (request == null || StrUtil.isBlank(name)) {
            return null;
        }
        return request.getParameter(name);
    }

    /**
     * 获取请求参数，参数不存在时返回默认值。
     *
     * @param request      HTTP 请求对象
     * @param name         参数名称
     * @param defaultValue 默认值
     * @return 参数值
     */
    public static String getParam(HttpServletRequest request, String name, String defaultValue) {
        return defaultIfBlank(getParam(request, name), defaultValue);
    }

    /**
     * 获取 Integer 请求参数。
     *
     * @param request HTTP 请求对象
     * @param name    参数名称
     * @return Integer 参数值
     */
    public static Integer getParamInt(HttpServletRequest request, String name) {
        return toInt(getParam(request, name));
    }

    /**
     * 获取 Integer 请求参数，参数不存在或转换失败时返回默认值。
     *
     * @param request      HTTP 请求对象
     * @param name         参数名称
     * @param defaultValue 默认值
     * @return Integer 参数值
     */
    public static Integer getParamInt(HttpServletRequest request, String name, Integer defaultValue) {
        return toInt(getParam(request, name), defaultValue);
    }

    /**
     * 获取 Long 请求参数。
     *
     * @param request HTTP 请求对象
     * @param name    参数名称
     * @return Long 参数值
     */
    public static Long getParamLong(HttpServletRequest request, String name) {
        return toLong(getParam(request, name));
    }

    /**
     * 获取 Long 请求参数，参数不存在或转换失败时返回默认值。
     *
     * @param request      HTTP 请求对象
     * @param name         参数名称
     * @param defaultValue 默认值
     * @return Long 参数值
     */
    public static Long getParamLong(HttpServletRequest request, String name, Long defaultValue) {
        return toLong(getParam(request, name), defaultValue);
    }

    /**
     * 获取 Boolean 请求参数。
     *
     * @param request HTTP 请求对象
     * @param name    参数名称
     * @return Boolean 参数值
     */
    public static Boolean getParamBool(HttpServletRequest request, String name) {
        return toBool(getParam(request, name));
    }

    /**
     * 获取 Boolean 请求参数，参数不存在或转换失败时返回默认值。
     *
     * @param request      HTTP 请求对象
     * @param name         参数名称
     * @param defaultValue 默认值
     * @return Boolean 参数值
     */
    public static Boolean getParamBool(HttpServletRequest request, String name, Boolean defaultValue) {
        return toBool(getParam(request, name), defaultValue);
    }

    /**
     * 获取 BigDecimal 请求参数。
     *
     * @param request HTTP 请求对象
     * @param name    参数名称
     * @return BigDecimal 参数值
     */
    public static BigDecimal getParamBigDecimal(HttpServletRequest request, String name) {
        return toBigDecimal(getParam(request, name));
    }

    /**
     * 获取请求参数数组。
     *
     * @param request HTTP 请求对象
     * @param name    参数名称
     * @return 参数数组
     */
    public static String[] getParamValues(HttpServletRequest request, String name) {
        if (request == null || StrUtil.isBlank(name)) {
            return new String[0];
        }

        String[] values = request.getParameterValues(name);
        return values == null ? new String[0] : values;
    }

    /**
     * 获取请求参数 Map。
     *
     * @param request HTTP 请求对象
     * @return 参数 Map
     */
    public static Map<String, String> getParamMap(HttpServletRequest request) {
        Map<String, String> result = new LinkedHashMap<>();
        if (request == null || MapUtil.isEmpty(request.getParameterMap())) {
            return result;
        }

        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String[] values = entry.getValue();
            result.put(entry.getKey(), values == null || values.length == 0 ? null : values[0]);
        }
        return result;
    }

    /**
     * 获取请求参数多值 Map。
     *
     * @param request HTTP 请求对象
     * @return 参数多值 Map
     */
    public static Map<String, List<String>> getParamListMap(HttpServletRequest request) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (request == null || MapUtil.isEmpty(request.getParameterMap())) {
            return result;
        }

        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String[] values = entry.getValue();
            result.put(entry.getKey(), values == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(values)));
        }
        return result;
    }

    // ============================== Header 处理 ==============================

    /**
     * 获取请求 Header。
     *
     * @param request HTTP 请求对象
     * @param name    Header 名称
     * @return Header 值
     */
    public static String getHeader(HttpServletRequest request, String name) {
        if (request == null || StrUtil.isBlank(name)) {
            return null;
        }
        return request.getHeader(name);
    }

    /**
     * 获取请求 Header，Header 不存在时返回默认值。
     *
     * @param request      HTTP 请求对象
     * @param name         Header 名称
     * @param defaultValue 默认值
     * @return Header 值
     */
    public static String getHeader(HttpServletRequest request, String name, String defaultValue) {
        return defaultIfBlank(getHeader(request, name), defaultValue);
    }

    /**
     * 获取所有请求 Header。
     *
     * @param request HTTP 请求对象
     * @return Header Map
     */
    public static Map<String, String> getHeaderMap(HttpServletRequest request) {
        Map<String, String> result = new LinkedHashMap<>();
        if (request == null) {
            return result;
        }

        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return result;
        }

        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            result.put(name, request.getHeader(name));
        }
        return result;
    }

    /**
     * 判断请求是否包含指定 Header。
     *
     * @param request HTTP 请求对象
     * @param name    Header 名称
     * @return 是否包含
     */
    public static boolean hasHeader(HttpServletRequest request, String name) {
        return StrUtil.isNotBlank(getHeader(request, name));
    }

    /**
     * 获取 Authorization Header。
     *
     * @param request HTTP 请求对象
     * @return Authorization Header
     */
    public static String getAuthorization(HttpServletRequest request) {
        return getHeader(request, "Authorization");
    }

    /**
     * 获取 Bearer Token。
     *
     * @param request HTTP 请求对象
     * @return Bearer Token
     */
    public static String getBearerToken(HttpServletRequest request) {
        String authorization = getAuthorization(request);
        if (StrUtil.isBlank(authorization)) {
            return null;
        }

        String bearerPrefix = "Bearer ";
        if (authorization.regionMatches(true, 0, bearerPrefix, 0, bearerPrefix.length())) {
            return authorization.substring(bearerPrefix.length()).trim();
        }
        return null;
    }

    /**
     * 获取 Token，优先从 Authorization Bearer 中读取，其次从指定 Header 中读取。
     *
     * @param request    HTTP 请求对象
     * @param headerName Token Header 名称
     * @return Token
     */
    public static String getToken(HttpServletRequest request, String headerName) {
        String bearerToken = getBearerToken(request);
        if (StrUtil.isNotBlank(bearerToken)) {
            return bearerToken;
        }
        return getHeader(request, defaultIfBlank(headerName, "token"));
    }

    /**
     * 获取内容类型。
     *
     * @param request HTTP 请求对象
     * @return 内容类型
     */
    public static String getContentType(HttpServletRequest request) {
        return request == null ? null : request.getContentType();
    }

    /**
     * 判断请求内容类型是否为 JSON。
     *
     * @param request HTTP 请求对象
     * @return 是否为 JSON 请求
     */
    public static boolean isJsonRequest(HttpServletRequest request) {
        String contentType = getContentType(request);
        return StrUtil.isNotBlank(contentType) && StrUtil.containsIgnoreCase(contentType, "application/json");
    }

    /**
     * 判断请求内容类型是否为表单。
     *
     * @param request HTTP 请求对象
     * @return 是否为表单请求
     */
    public static boolean isFormRequest(HttpServletRequest request) {
        String contentType = getContentType(request);
        return StrUtil.isNotBlank(contentType)
                && (StrUtil.containsIgnoreCase(contentType, "application/x-www-form-urlencoded")
                || StrUtil.containsIgnoreCase(contentType, "multipart/form-data"));
    }

    // ============================== Cookie 处理 ==============================

    /**
     * 获取请求 Cookie 数组。
     *
     * @param request HTTP 请求对象
     * @return Cookie 数组
     */
    public static Cookie[] getCookies(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return new Cookie[0];
        }
        return request.getCookies();
    }

    /**
     * 获取指定名称的 Cookie。
     *
     * @param request HTTP 请求对象
     * @param name    Cookie 名称
     * @return Cookie 对象
     */
    public static Cookie getCookie(HttpServletRequest request, String name) {
        if (request == null || StrUtil.isBlank(name)) {
            return null;
        }

        for (Cookie cookie : getCookies(request)) {
            if (cookie != null && StrUtil.equals(cookie.getName(), name)) {
                return cookie;
            }
        }
        return null;
    }

    /**
     * 获取指定名称的 Cookie 值。
     *
     * @param request HTTP 请求对象
     * @param name    Cookie 名称
     * @return Cookie 值
     */
    public static String getCookieValue(HttpServletRequest request, String name) {
        Cookie cookie = getCookie(request, name);
        return cookie == null ? null : cookie.getValue();
    }

    /**
     * 添加 Cookie 到响应。
     *
     * @param response HTTP 响应对象
     * @param name     Cookie 名称
     * @param value    Cookie 值
     * @param maxAge   有效时间，单位秒
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        if (response == null || StrUtil.isBlank(name)) {
            return;
        }

        Cookie cookie = new Cookie(name, defaultIfNull(value, EMPTY));
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    /**
     * 添加 HttpOnly Cookie 到响应。
     *
     * @param response HTTP 响应对象
     * @param name     Cookie 名称
     * @param value    Cookie 值
     * @param maxAge   有效时间，单位秒
     */
    public static void addHttpOnlyCookie(HttpServletResponse response, String name, String value, int maxAge) {
        if (response == null || StrUtil.isBlank(name)) {
            return;
        }

        Cookie cookie = new Cookie(name, defaultIfNull(value, EMPTY));
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    /**
     * 删除 Cookie。
     *
     * @param response HTTP 响应对象
     * @param name     Cookie 名称
     */
    public static void removeCookie(HttpServletResponse response, String name) {
        addCookie(response, name, EMPTY, 0);
    }

    // ============================== IP 地址处理 ==============================

    /**
     * 获取客户端 IP 地址。
     *
     * @param request HTTP 请求对象
     * @return 客户端 IP 地址
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String ip = firstNotBlank(
                getHeader(request, "X-Forwarded-For"),
                getHeader(request, "X-Real-IP"),
                getHeader(request, "Proxy-Client-IP"),
                getHeader(request, "WL-Proxy-Client-IP"),
                getHeader(request, "HTTP_CLIENT_IP"),
                getHeader(request, "HTTP_X_FORWARDED_FOR"),
                request.getRemoteAddr()
        );

        if (StrUtil.isBlank(ip)) {
            return null;
        }

        if (StrUtil.contains(ip, ",")) {
            return splitTrimIgnoreBlank(ip, ",").stream()
                    .filter(item -> StrUtil.isNotBlank(item) && !"unknown".equalsIgnoreCase(item))
                    .findFirst()
                    .orElse(null);
        }

        return "unknown".equalsIgnoreCase(ip) ? null : ip;
    }

    /**
     * 获取服务端 IP 地址。
     *
     * @param request HTTP 请求对象
     * @return 服务端 IP 地址
     */
    public static String getServerIp(HttpServletRequest request) {
        return request == null ? null : request.getLocalAddr();
    }

    /**
     * 获取客户端端口。
     *
     * @param request HTTP 请求对象
     * @return 客户端端口
     */
    public static Integer getClientPort(HttpServletRequest request) {
        return request == null ? null : request.getRemotePort();
    }

    /**
     * 获取服务端端口。
     *
     * @param request HTTP 请求对象
     * @return 服务端端口
     */
    public static Integer getServerPort(HttpServletRequest request) {
        return request == null ? null : request.getServerPort();
    }

    /**
     * 判断 IP 是否为内网 IP。
     *
     * @param ip IP 地址
     * @return 是否为内网 IP
     */
    public static boolean isInnerIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }

        try {
            return NetUtil.isInnerIP(ip);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断客户端 IP 是否为内网 IP。
     *
     * @param request HTTP 请求对象
     * @return 是否为内网 IP
     */
    public static boolean isInnerClientIp(HttpServletRequest request) {
        return isInnerIp(getClientIp(request));
    }

    /**
     * 判断字符串是否为 IPv4 地址。
     *
     * @param ip IP 地址
     * @return 是否为 IPv4 地址
     */
    public static boolean isIpv4(String ip) {
        return StrUtil.isNotBlank(ip) && Validator.isIpv4(ip);
    }

    /**
     * 判断字符串是否为 IPv6 地址。
     *
     * @param ip IP 地址
     * @return 是否为 IPv6 地址
     */
    public static boolean isIpv6(String ip) {
        return StrUtil.isNotBlank(ip) && Validator.isIpv6(ip);
    }

    // ============================== User-Agent 处理 ==============================

    /**
     * 获取 User-Agent 字符串。
     *
     * @param request HTTP 请求对象
     * @return User-Agent 字符串
     */
    public static String getUserAgent(HttpServletRequest request) {
        return getHeader(request, "User-Agent");
    }

    /**
     * 解析 User-Agent。
     *
     * @param userAgent User-Agent 字符串
     * @return UserAgent 对象
     */
    public static UserAgent parseUserAgent(String userAgent) {
        if (StrUtil.isBlank(userAgent)) {
            return null;
        }
        return UserAgentUtil.parse(userAgent);
    }

    /**
     * 解析请求中的 User-Agent。
     *
     * @param request HTTP 请求对象
     * @return UserAgent 对象
     */
    public static UserAgent parseUserAgent(HttpServletRequest request) {
        return parseUserAgent(getUserAgent(request));
    }

    /**
     * 获取浏览器名称。
     *
     * @param request HTTP 请求对象
     * @return 浏览器名称
     */
    public static String getBrowserName(HttpServletRequest request) {
        UserAgent userAgent = parseUserAgent(request);
        return userAgent == null || userAgent.getBrowser() == null ? null : userAgent.getBrowser().getName();
    }

    /**
     * 获取浏览器版本。
     *
     * @param request HTTP 请求对象
     * @return 浏览器版本
     */
    public static String getBrowserVersion(HttpServletRequest request) {
        UserAgent userAgent = parseUserAgent(request);
        return userAgent == null ? null : userAgent.getVersion();
    }

    /**
     * 获取操作系统名称。
     *
     * @param request HTTP 请求对象
     * @return 操作系统名称
     */
    public static String getOsName(HttpServletRequest request) {
        UserAgent userAgent = parseUserAgent(request);
        return userAgent == null || userAgent.getOs() == null ? null : userAgent.getOs().getName();
    }

    /**
     * 获取平台名称。
     *
     * @param request HTTP 请求对象
     * @return 平台名称
     */
    public static String getPlatformName(HttpServletRequest request) {
        UserAgent userAgent = parseUserAgent(request);
        return userAgent == null || userAgent.getPlatform() == null ? null : userAgent.getPlatform().getName();
    }

    /**
     * 判断是否为移动端请求。
     *
     * @param request HTTP 请求对象
     * @return 是否为移动端请求
     */
    public static boolean isMobileRequest(HttpServletRequest request) {
        UserAgent userAgent = parseUserAgent(request);
        return userAgent != null && userAgent.isMobile();
    }

    /**
     * 判断是否为 PC 端请求。
     *
     * @param request HTTP 请求对象
     * @return 是否为 PC 端请求
     */
    public static boolean isPcRequest(HttpServletRequest request) {
        UserAgent userAgent = parseUserAgent(request);
        return userAgent != null && !userAgent.isMobile();
    }

    // ============================== URL 与 Query 参数 ==============================

    /**
     * 判断字符串是否为 URL。
     *
     * @param url URL 字符串
     * @return 是否为 URL
     */
    public static boolean isUrl(String url) {
        return StrUtil.isNotBlank(url) && Validator.isUrl(url);
    }

    /**
     * 获取 URL 中的协议。
     *
     * @param url URL 字符串
     * @return URL 协议
     */
    public static String getUrlScheme(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }

        try {
            return URI.create(url).getScheme();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 URL 中的主机名。
     *
     * @param url URL 字符串
     * @return 主机名
     */
    public static String getUrlHost(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }

        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 URL 中的路径。
     *
     * @param url URL 字符串
     * @return URL 路径
     */
    public static String getUrlPath(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }

        try {
            return URI.create(url).getPath();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 URL 中的 Query 字符串。
     *
     * @param url URL 字符串
     * @return Query 字符串
     */
    public static String getUrlQuery(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }

        try {
            return URI.create(url).getRawQuery();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 Map 转换为 Query 字符串。
     *
     * @param params 参数 Map
     * @return Query 字符串
     */
    public static String toQueryString(Map<String, ?> params) {
        if (MapUtil.isEmpty(params)) {
            return EMPTY;
        }

        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            if (StrUtil.isBlank(entry.getKey()) || entry.getValue() == null) {
                continue;
            }

            String key = urlEncode(entry.getKey());
            Object value = entry.getValue();

            if (value instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    if (item != null) {
                        pairs.add(key + "=" + urlEncode(item.toString()));
                    }
                }
            } else if (value.getClass().isArray()) {
                Object[] array = (Object[]) value;
                for (Object item : array) {
                    if (item != null) {
                        pairs.add(key + "=" + urlEncode(item.toString()));
                    }
                }
            } else {
                pairs.add(key + "=" + urlEncode(value.toString()));
            }
        }
        return join("&", pairs);
    }

    /**
     * 将 Query 字符串解析为 Map。
     *
     * @param query Query 字符串
     * @return 参数 Map
     */
    public static Map<String, String> parseQueryString(String query) {
        Map<String, String> result = new LinkedHashMap<>();
        if (StrUtil.isBlank(query)) {
            return result;
        }

        String actualQuery = removePrefix(query, "?");
        List<String> pairs = splitTrimIgnoreBlank(actualQuery, "&");
        for (String pair : pairs) {
            int index = pair.indexOf("=");
            if (index < 0) {
                result.put(urlDecode(pair), EMPTY);
                continue;
            }

            String key = urlDecode(pair.substring(0, index));
            String value = urlDecode(pair.substring(index + 1));
            result.put(key, value);
        }
        return result;
    }

    /**
     * 将 Query 字符串解析为多值 Map。
     *
     * @param query Query 字符串
     * @return 多值参数 Map
     */
    public static Map<String, List<String>> parseQueryStringListMap(String query) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (StrUtil.isBlank(query)) {
            return result;
        }

        String actualQuery = removePrefix(query, "?");
        List<String> pairs = splitTrimIgnoreBlank(actualQuery, "&");
        for (String pair : pairs) {
            int index = pair.indexOf("=");
            String key;
            String value;

            if (index < 0) {
                key = urlDecode(pair);
                value = EMPTY;
            } else {
                key = urlDecode(pair.substring(0, index));
                value = urlDecode(pair.substring(index + 1));
            }

            result.computeIfAbsent(key, item -> new ArrayList<>()).add(value);
        }
        return result;
    }

    /**
     * 向 URL 追加 Query 参数。
     *
     * @param url   URL 字符串
     * @param key   参数名
     * @param value 参数值
     * @return 追加后的 URL
     */
    public static String appendQueryParam(String url, String key, Object value) {
        if (StrUtil.isBlank(url) || StrUtil.isBlank(key) || value == null) {
            return url;
        }

        String queryString = urlEncode(key) + "=" + urlEncode(value.toString());
        return url + (url.contains("?") ? "&" : "?") + queryString;
    }

    /**
     * 向 URL 追加多个 Query 参数。
     *
     * @param url    URL 字符串
     * @param params 参数 Map
     * @return 追加后的 URL
     */
    public static String appendQueryParams(String url, Map<String, ?> params) {
        if (StrUtil.isBlank(url) || MapUtil.isEmpty(params)) {
            return url;
        }

        String queryString = toQueryString(params);
        if (StrUtil.isBlank(queryString)) {
            return url;
        }
        return url + (url.contains("?") ? "&" : "?") + queryString;
    }

    /**
     * 从 URL 中读取指定 Query 参数。
     *
     * @param url URL 字符串
     * @param key 参数名
     * @return 参数值
     */
    public static String getQueryParam(String url, String key) {
        if (StrUtil.isBlank(url) || StrUtil.isBlank(key)) {
            return null;
        }

        return parseQueryString(getUrlQuery(url)).get(key);
    }

    // ============================== 响应处理 ==============================

    /**
     * 设置响应 JSON 内容类型。
     *
     * @param response HTTP 响应对象
     */
    public static void setJsonResponse(HttpServletResponse response) {
        if (response == null) {
            return;
        }

        response.setCharacterEncoding(DEFAULT_CHARSET.name());
        response.setContentType("application/json;charset=" + DEFAULT_CHARSET.name());
    }

    /**
     * 设置响应纯文本内容类型。
     *
     * @param response HTTP 响应对象
     */
    public static void setTextResponse(HttpServletResponse response) {
        if (response == null) {
            return;
        }

        response.setCharacterEncoding(DEFAULT_CHARSET.name());
        response.setContentType("text/plain;charset=" + DEFAULT_CHARSET.name());
    }

    /**
     * 设置响应 HTML 内容类型。
     *
     * @param response HTTP 响应对象
     */
    public static void setHtmlResponse(HttpServletResponse response) {
        if (response == null) {
            return;
        }

        response.setCharacterEncoding(DEFAULT_CHARSET.name());
        response.setContentType("text/html;charset=" + DEFAULT_CHARSET.name());
    }

    /**
     * 设置响应状态码。
     *
     * @param response HTTP 响应对象
     * @param status   状态码
     */
    public static void setResponseStatus(HttpServletResponse response, int status) {
        if (response == null) {
            return;
        }
        response.setStatus(status);
    }

    /**
     * 向响应写入字符串。
     *
     * @param response HTTP 响应对象
     * @param content  响应内容
     */
    public static void writeResponse(HttpServletResponse response, String content) {
        if (response == null) {
            return;
        }

        try {
            response.getWriter().write(defaultIfNull(content, EMPTY));
            response.getWriter().flush();
        } catch (IOException e) {
            throw new IllegalStateException("响应内容写入失败", e);
        }
    }

    /**
     * 向响应写入 JSON 内容。
     *
     * @param response HTTP 响应对象
     * @param value    响应对象
     */
    public static void writeJsonResponse(HttpServletResponse response, Object value) {
        if (response == null) {
            return;
        }

        setJsonResponse(response);
        writeResponse(response, toJsonStr(value));
    }

    /**
     * 向响应写入字节数组。
     *
     * @param response HTTP 响应对象
     * @param bytes    字节数组
     */
    public static void writeBytesResponse(HttpServletResponse response, byte[] bytes) {
        if (response == null) {
            return;
        }

        try {
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(bytes == null ? new byte[0] : bytes);
            outputStream.flush();
        } catch (IOException e) {
            throw new IllegalStateException("响应字节写入失败", e);
        }
    }

    /**
     * 设置文件下载响应头。
     *
     * @param response HTTP 响应对象
     * @param fileName 下载文件名
     */
    public static void setDownloadHeader(HttpServletResponse response, String fileName) {
        if (response == null) {
            return;
        }

        String actualFileName = defaultIfBlank(fileName, "download");
        String encodedFileName = urlEncode(actualFileName).replace("+", "%20");

        response.setCharacterEncoding(DEFAULT_CHARSET.name());
        response.setContentType("application/octet-stream;charset=" + DEFAULT_CHARSET.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    /**
     * 向响应写入下载文件。
     *
     * @param response HTTP 响应对象
     * @param file     文件对象
     */
    public static void writeDownloadFile(HttpServletResponse response, File file) {
        if (response == null || file == null || !FileUtil.isFile(file)) {
            return;
        }

        setDownloadHeader(response, file.getName());
        response.setContentLengthLong(file.length());

        try (InputStream inputStream = Files.newInputStream(file.toPath());
             ServletOutputStream outputStream = response.getOutputStream()) {
            IoUtil.copy(inputStream, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            throw new IllegalStateException("下载文件写入失败：" + file.getAbsolutePath(), e);
        }
    }

    /**
     * 向响应写入下载字节数组。
     *
     * @param response HTTP 响应对象
     * @param fileName 下载文件名
     * @param bytes    字节数组
     */
    public static void writeDownloadBytes(HttpServletResponse response, String fileName, byte[] bytes) {
        if (response == null) {
            return;
        }

        byte[] actualBytes = bytes == null ? new byte[0] : bytes;
        setDownloadHeader(response, fileName);
        response.setContentLengthLong(actualBytes.length);
        writeBytesResponse(response, actualBytes);
    }

    // ============================== Web 内部辅助方法 ==============================

    /**
     * 对象数组转换为字符串集合。
     *
     * @param array 对象数组
     * @return 字符串集合
     */
    private static List<String> objectArrayToStringList(Object[] array) {
        List<String> result = new ArrayList<>();
        if (array == null || array.length == 0) {
            return result;
        }

        for (Object item : array) {
            if (item != null) {
                result.add(item.toString());
            }
        }
        return result;
    }

    // ============================== 基础参数校验 ==============================

    /**
     * 判断对象是否为有效参数。
     *
     * @param value 对象
     * @return 是否为有效参数
     */
    public static boolean isValidObject(Object value) {
        return ObjectUtil.isNotEmpty(value);
    }

    /**
     * 判断对象是否为无效参数。
     *
     * @param value 对象
     * @return 是否为无效参数
     */
    public static boolean isInvalidObject(Object value) {
        return ObjectUtil.isEmpty(value);
    }

    /**
     * 判断字符串是否为有效参数。
     *
     * @param value 字符串
     * @return 是否为有效参数
     */
    public static boolean isValidStr(CharSequence value) {
        return StrUtil.isNotBlank(value);
    }

    /**
     * 判断字符串是否为无效参数。
     *
     * @param value 字符串
     * @return 是否为无效参数
     */
    public static boolean isInvalidStr(CharSequence value) {
        return StrUtil.isBlank(value);
    }

    /**
     * 判断集合是否为有效参数。
     *
     * @param values 集合
     * @return 是否为有效参数
     */
    public static boolean isValidCollection(Collection<?> values) {
        return CollUtil.isNotEmpty(values);
    }

    /**
     * 判断集合是否为无效参数。
     *
     * @param values 集合
     * @return 是否为无效参数
     */
    public static boolean isInvalidCollection(Collection<?> values) {
        return CollUtil.isEmpty(values);
    }

    /**
     * 判断 Map 是否为有效参数。
     *
     * @param values Map 对象
     * @return 是否为有效参数
     */
    public static boolean isValidMap(Map<?, ?> values) {
        return MapUtil.isNotEmpty(values);
    }

    /**
     * 判断 Map 是否为无效参数。
     *
     * @param values Map 对象
     * @return 是否为无效参数
     */
    public static boolean isInvalidMap(Map<?, ?> values) {
        return MapUtil.isEmpty(values);
    }

    // ============================== 常用格式校验 ==============================

    /**
     * 校验手机号格式。
     *
     * @param value 手机号
     * @return 是否为手机号
     */
    public static boolean isValidMobile(String value) {
        return StrUtil.isNotBlank(value) && Validator.isMobile(value);
    }

    /**
     * 校验邮箱格式。
     *
     * @param value 邮箱
     * @return 是否为邮箱
     */
    public static boolean isValidEmail(String value) {
        return StrUtil.isNotBlank(value) && Validator.isEmail(value);
    }

    /**
     * 校验身份证号格式。
     *
     * @param value 身份证号
     * @return 是否为身份证号
     */
    public static boolean isValidIdCard(String value) {
        return StrUtil.isNotBlank(value) && Validator.isCitizenId(value);
    }

    /**
     * 校验 URL 格式。
     *
     * @param value URL 字符串
     * @return 是否为 URL
     */
    public static boolean isValidUrl(String value) {
        return StrUtil.isNotBlank(value) && Validator.isUrl(value);
    }

    /**
     * 校验 IPv4 地址格式。
     *
     * @param value IPv4 地址
     * @return 是否为 IPv4 地址
     */
    public static boolean isValidIpv4(String value) {
        return StrUtil.isNotBlank(value) && Validator.isIpv4(value);
    }

    /**
     * 校验 IPv6 地址格式。
     *
     * @param value IPv6 地址
     * @return 是否为 IPv6 地址
     */
    public static boolean isValidIpv6(String value) {
        return StrUtil.isNotBlank(value) && Validator.isIpv6(value);
    }

    /**
     * 校验 IP 地址格式。
     *
     * @param value IP 地址
     * @return 是否为 IP 地址
     */
    public static boolean isValidIp(String value) {
        return isValidIpv4(value) || isValidIpv6(value);
    }

    /**
     * 校验 MAC 地址格式。
     *
     * @param value MAC 地址
     * @return 是否为 MAC 地址
     */
    public static boolean isValidMac(String value) {
        return StrUtil.isNotBlank(value)
                && ReUtil.isMatch("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$", value);
    }

    /**
     * 校验域名格式。
     *
     * @param value 域名
     * @return 是否为域名
     */
    public static boolean isValidDomain(String value) {
        return StrUtil.isNotBlank(value)
                && ReUtil.isMatch("^(?=.{1,253}$)([a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$", value);
    }

    /**
     * 校验邮政编码格式。
     *
     * @param value 邮政编码
     * @return 是否为邮政编码
     */
    public static boolean isValidPostalCode(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^\\d{6}$", value);
    }

    /**
     * 校验统一社会信用代码格式。
     *
     * @param value 统一社会信用代码
     * @return 是否为统一社会信用代码
     */
    public static boolean isValidCreditCode(String value) {
        return StrUtil.isNotBlank(value)
                && ReUtil.isMatch("^[0-9A-HJ-NPQRTUWXY]{2}\\d{6}[0-9A-HJ-NPQRTUWXY]{10}$", value);
    }

    /**
     * 校验银行卡号格式。
     *
     * @param value 银行卡号
     * @return 是否为银行卡号
     */
    public static boolean isValidBankCard(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }

        String cardNo = cleanBlank(value);
        if (!ReUtil.isMatch("^\\d{12,19}$", cardNo)) {
            return false;
        }
        return checkLuhn(cardNo);
    }

    /**
     * 校验车牌号格式。
     *
     * @param value 车牌号
     * @return 是否为车牌号
     */
    public static boolean isValidPlateNumber(String value) {
        return StrUtil.isNotBlank(value)
                && ReUtil.isMatch("^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z][A-Z0-9挂学警港澳]{5,6}$", value);
    }

    // ============================== 数字参数校验 ==============================

    /**
     * 校验字符串是否为有效数字。
     *
     * @param value 字符串
     * @return 是否为有效数字
     */
    public static boolean isValidNumber(String value) {
        return StrUtil.isNotBlank(value) && NumberUtil.isNumber(value);
    }

    /**
     * 校验字符串是否为有效整数。
     *
     * @param value 字符串
     * @return 是否为有效整数
     */
    public static boolean isValidInteger(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^-?\\d+$", value);
    }

    /**
     * 校验字符串是否为有效正整数。
     *
     * @param value 字符串
     * @return 是否为有效正整数
     */
    public static boolean isValidPositiveInteger(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^[1-9]\\d*$", value);
    }

    /**
     * 校验字符串是否为有效非负整数。
     *
     * @param value 字符串
     * @return 是否为有效非负整数
     */
    public static boolean isValidNonNegativeInteger(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^(0|[1-9]\\d*)$", value);
    }

    /**
     * 校验字符串是否为有效小数。
     *
     * @param value 字符串
     * @return 是否为有效小数
     */
    public static boolean isValidDecimal(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^-?\\d+\\.\\d+$", value);
    }

    /**
     * 校验字符串是否为有效金额，最多保留 2 位小数。
     *
     * @param value 金额字符串
     * @return 是否为有效金额
     */
    public static boolean isValidAmount(String value) {
        return isValidAmount(value, DEFAULT_AMOUNT_SCALE);
    }

    /**
     * 校验字符串是否为有效金额，并限制最大小数位。
     *
     * @param value    金额字符串
     * @param maxScale 最大小数位
     * @return 是否为有效金额
     */
    public static boolean isValidAmount(String value, int maxScale) {
        if (StrUtil.isBlank(value) || maxScale < 0) {
            return false;
        }

        String regex = "^(0|[1-9]\\d*)(\\.\\d{1," + maxScale + "})?$";
        return ReUtil.isMatch(regex, value);
    }

    /**
     * 校验数字是否在指定范围内，包含边界值。
     *
     * @param value 数字
     * @param min   最小值
     * @param max   最大值
     * @return 是否在范围内
     */
    public static boolean isValidNumberRange(Number value, Number min, Number max) {
        return isBetween(value, min, max);
    }

    /**
     * 校验数字是否大于指定值。
     *
     * @param value 数字
     * @param min   最小值
     * @return 是否大于指定值
     */
    public static boolean isGreaterThan(Number value, Number min) {
        if (value == null || min == null) {
            return false;
        }
        return greaterThan(value, min);
    }

    /**
     * 校验数字是否大于等于指定值。
     *
     * @param value 数字
     * @param min   最小值
     * @return 是否大于等于指定值
     */
    public static boolean isGreaterThanOrEqual(Number value, Number min) {
        if (value == null || min == null) {
            return false;
        }
        return greaterThanOrEqual(value, min);
    }

    /**
     * 校验数字是否小于指定值。
     *
     * @param value 数字
     * @param max   最大值
     * @return 是否小于指定值
     */
    public static boolean isLessThan(Number value, Number max) {
        if (value == null || max == null) {
            return false;
        }
        return lessThan(value, max);
    }

    /**
     * 校验数字是否小于等于指定值。
     *
     * @param value 数字
     * @param max   最大值
     * @return 是否小于等于指定值
     */
    public static boolean isLessThanOrEqual(Number value, Number max) {
        if (value == null || max == null) {
            return false;
        }
        return lessThanOrEqual(value, max);
    }

    /**
     * 校验端口号是否有效。
     *
     * @param port 端口号
     * @return 是否为有效端口号
     */
    public static boolean isValidPort(Integer port) {
        return port != null && port >= 1 && port <= 65535;
    }

    /**
     * 校验端口号字符串是否有效。
     *
     * @param port 端口号字符串
     * @return 是否为有效端口号
     */
    public static boolean isValidPort(String port) {
        return isValidPositiveInteger(port) && isValidPort(toInt(port));
    }

    // ============================== 字符串长度校验 ==============================

    /**
     * 校验字符串长度是否等于指定长度。
     *
     * @param value  字符串
     * @param length 长度
     * @return 是否等于指定长度
     */
    public static boolean isLengthEqual(String value, int length) {
        return value != null && value.length() == length;
    }

    /**
     * 校验字符串长度是否大于等于指定长度。
     *
     * @param value     字符串
     * @param minLength 最小长度
     * @return 是否大于等于指定长度
     */
    public static boolean isMinLength(String value, int minLength) {
        return value != null && value.length() >= minLength;
    }

    /**
     * 校验字符串长度是否小于等于指定长度。
     *
     * @param value     字符串
     * @param maxLength 最大长度
     * @return 是否小于等于指定长度
     */
    public static boolean isMaxLength(String value, int maxLength) {
        return value != null && value.length() <= maxLength;
    }

    /**
     * 校验字符串长度是否在指定范围内，包含边界值。
     *
     * @param value     字符串
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @return 是否在指定范围内
     */
    public static boolean isLengthBetween(String value, int minLength, int maxLength) {
        if (value == null || minLength < 0 || maxLength < minLength) {
            return false;
        }

        int length = value.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * 校验字符串字节长度是否在指定范围内，使用 UTF-8 编码。
     *
     * @param value     字符串
     * @param minLength 最小字节长度
     * @param maxLength 最大字节长度
     * @return 是否在指定范围内
     */
    public static boolean isByteLengthBetween(String value, int minLength, int maxLength) {
        return isByteLengthBetween(value, minLength, maxLength, DEFAULT_CHARSET);
    }

    /**
     * 校验字符串字节长度是否在指定范围内。
     *
     * @param value     字符串
     * @param minLength 最小字节长度
     * @param maxLength 最大字节长度
     * @param charset   字符集
     * @return 是否在指定范围内
     */
    public static boolean isByteLengthBetween(String value, int minLength, int maxLength, Charset charset) {
        if (value == null || minLength < 0 || maxLength < minLength) {
            return false;
        }

        Charset actualCharset = charset == null ? DEFAULT_CHARSET : charset;
        int length = value.getBytes(actualCharset).length;
        return length >= minLength && length <= maxLength;
    }

    // ============================== 字符内容校验 ==============================

    /**
     * 校验字符串是否只包含中文。
     *
     * @param value 字符串
     * @return 是否只包含中文
     */
    public static boolean isValidChinese(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^[\\u4e00-\\u9fa5]+$", value);
    }

    /**
     * 校验字符串是否只包含英文字母。
     *
     * @param value 字符串
     * @return 是否只包含英文字母
     */
    public static boolean isValidLetters(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^[a-zA-Z]+$", value);
    }

    /**
     * 校验字符串是否只包含数字。
     *
     * @param value 字符串
     * @return 是否只包含数字
     */
    public static boolean isValidDigits(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^\\d+$", value);
    }

    /**
     * 校验字符串是否只包含英文字母和数字。
     *
     * @param value 字符串
     * @return 是否只包含英文字母和数字
     */
    public static boolean isValidLettersOrNumbers(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^[a-zA-Z0-9]+$", value);
    }

    /**
     * 校验字符串是否只包含中文、英文字母和数字。
     *
     * @param value 字符串
     * @return 是否只包含中文、英文字母和数字
     */
    public static boolean isValidChineseLettersNumbers(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^[\\u4e00-\\u9fa5a-zA-Z0-9]+$", value);
    }

    /**
     * 校验字符串是否只包含英文字母、数字和下划线。
     *
     * @param value 字符串
     * @return 是否只包含英文字母、数字和下划线
     */
    public static boolean isValidGeneralCode(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^\\w+$", value);
    }

    /**
     * 校验字符串是否不包含特殊字符。
     *
     * @param value 字符串
     * @return 是否不包含特殊字符
     */
    public static boolean isWithoutSpecialChar(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.isMatch("^[\\u4e00-\\u9fa5a-zA-Z0-9_\\-]+$", value);
    }

    /**
     * 校验字符串是否包含中文。
     *
     * @param value 字符串
     * @return 是否包含中文
     */
    public static boolean hasChinese(String value) {
        return StrUtil.isNotBlank(value) && ReUtil.contains("[\\u4e00-\\u9fa5]", value);
    }

    /**
     * 校验字符串是否包含空白字符。
     *
     * @param value 字符串
     * @return 是否包含空白字符
     */
    public static boolean hasWhitespace(String value) {
        return StrUtil.isNotEmpty(value) && ReUtil.contains("\\s", value);
    }

    // ============================== 账号密码校验 ==============================

    /**
     * 校验用户名格式，支持字母开头，包含字母、数字、下划线，长度 4 到 32 位。
     *
     * @param value 用户名
     * @return 是否为有效用户名
     */
    public static boolean isValidUsername(String value) {
        return isValidUsername(value, 4, 32);
    }

    /**
     * 校验用户名格式，支持字母开头，包含字母、数字、下划线。
     *
     * @param value     用户名
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @return 是否为有效用户名
     */
    public static boolean isValidUsername(String value, int minLength, int maxLength) {
        if (!isLengthBetween(value, minLength, maxLength)) {
            return false;
        }
        return ReUtil.isMatch("^[a-zA-Z][a-zA-Z0-9_]*$", value);
    }

    /**
     * 校验密码强度，要求同时包含字母和数字，长度 8 到 32 位。
     *
     * @param value 密码
     * @return 是否为有效密码
     */
    public static boolean isValidPassword(String value) {
        return isValidPassword(value, 8, 32);
    }

    /**
     * 校验密码强度，要求同时包含字母和数字。
     *
     * @param value     密码
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @return 是否为有效密码
     */
    public static boolean isValidPassword(String value, int minLength, int maxLength) {
        if (!isLengthBetween(value, minLength, maxLength)) {
            return false;
        }
        return ReUtil.contains("[a-zA-Z]", value) && ReUtil.contains("\\d", value);
    }

    /**
     * 校验强密码，要求包含大小写字母、数字和特殊字符。
     *
     * @param value 密码
     * @return 是否为强密码
     */
    public static boolean isStrongPassword(String value) {
        if (!isLengthBetween(value, 8, 64)) {
            return false;
        }

        return ReUtil.contains("[a-z]", value)
                && ReUtil.contains("[A-Z]", value)
                && ReUtil.contains("\\d", value)
                && ReUtil.contains("[^a-zA-Z0-9]", value);
    }

    // ============================== 正则校验 ==============================

    /**
     * 使用正则表达式校验字符串。
     *
     * @param value 字符串
     * @param regex 正则表达式
     * @return 是否匹配
     */
    public static boolean isMatch(String value, String regex) {
        if (value == null || StrUtil.isBlank(regex)) {
            return false;
        }

        try {
            return ReUtil.isMatch(regex, value);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断字符串是否包含正则表达式匹配内容。
     *
     * @param value 字符串
     * @param regex 正则表达式
     * @return 是否包含匹配内容
     */
    public static boolean containsMatch(String value, String regex) {
        if (value == null || StrUtil.isBlank(regex)) {
            return false;
        }

        try {
            return ReUtil.contains(regex, value);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 校验正则表达式是否合法。
     *
     * @param regex 正则表达式
     * @return 是否合法
     */
    public static boolean isValidRegex(String regex) {
        if (StrUtil.isBlank(regex)) {
            return false;
        }

        try {
            Pattern.compile(regex);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ============================== 日期参数校验 ==============================

    /**
     * 校验字符串是否为 yyyy-MM-dd 日期格式。
     *
     * @param value 日期字符串
     * @return 是否为日期格式
     */
    public static boolean isValidDate(String value) {
        return isValidDate(value, DATE_PATTERN);
    }

    /**
     * 校验字符串是否为指定日期格式。
     *
     * @param value   日期字符串
     * @param pattern 日期格式
     * @return 是否为日期格式
     */
    public static boolean isValidDate(String value, String pattern) {
        if (StrUtil.isBlank(value) || StrUtil.isBlank(pattern)) {
            return false;
        }

        try {
            parseLocalDate(value, pattern);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 校验字符串是否为 yyyy-MM-dd HH:mm:ss 日期时间格式。
     *
     * @param value 日期时间字符串
     * @return 是否为日期时间格式
     */
    public static boolean isValidDateTime(String value) {
        return isValidDateTime(value, DATE_TIME_PATTERN);
    }

    /**
     * 校验字符串是否为指定日期时间格式。
     *
     * @param value   日期时间字符串
     * @param pattern 日期时间格式
     * @return 是否为日期时间格式
     */
    public static boolean isValidDateTime(String value, String pattern) {
        if (StrUtil.isBlank(value) || StrUtil.isBlank(pattern)) {
            return false;
        }

        try {
            parseLocalDateTime(value, pattern);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 校验日期是否在指定范围内，包含边界值。
     *
     * @param value 日期
     * @param start 开始日期
     * @param end   结束日期
     * @return 是否在范围内
     */
    public static boolean isValidDateRange(LocalDate value, LocalDate start, LocalDate end) {
        if (value == null || start == null || end == null) {
            return false;
        }
        return !value.isBefore(start) && !value.isAfter(end);
    }

    /**
     * 校验日期时间是否在指定范围内，包含边界值。
     *
     * @param value 日期时间
     * @param start 开始日期时间
     * @param end   结束日期时间
     * @return 是否在范围内
     */
    public static boolean isValidDateTimeRange(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return isBetween(value, start, end);
    }

    /**
     * 校验日期是否为过去日期。
     *
     * @param value 日期
     * @return 是否为过去日期
     */
    public static boolean isPastDate(LocalDate value) {
        return value != null && value.isBefore(today());
    }

    /**
     * 校验日期是否为未来日期。
     *
     * @param value 日期
     * @return 是否为未来日期
     */
    public static boolean isFutureDate(LocalDate value) {
        return value != null && value.isAfter(today());
    }

    /**
     * 校验日期时间是否为过去时间。
     *
     * @param value 日期时间
     * @return 是否为过去时间
     */
    public static boolean isPastDateTime(LocalDateTime value) {
        return value != null && value.isBefore(now());
    }

    /**
     * 校验日期时间是否为未来时间。
     *
     * @param value 日期时间
     * @return 是否为未来时间
     */
    public static boolean isFutureDateTime(LocalDateTime value) {
        return value != null && value.isAfter(now());
    }

    // ============================== 集合与数组参数校验 ==============================

    /**
     * 校验集合大小是否等于指定大小。
     *
     * @param values 集合
     * @param size   大小
     * @return 是否等于指定大小
     */
    public static boolean isSizeEqual(Collection<?> values, int size) {
        return values != null && values.size() == size;
    }

    /**
     * 校验集合大小是否大于等于指定大小。
     *
     * @param values  集合
     * @param minSize 最小大小
     * @return 是否大于等于指定大小
     */
    public static boolean isMinSize(Collection<?> values, int minSize) {
        return values != null && values.size() >= minSize;
    }

    /**
     * 校验集合大小是否小于等于指定大小。
     *
     * @param values  集合
     * @param maxSize 最大大小
     * @return 是否小于等于指定大小
     */
    public static boolean isMaxSize(Collection<?> values, int maxSize) {
        return values != null && values.size() <= maxSize;
    }

    /**
     * 校验集合大小是否在指定范围内。
     *
     * @param values  集合
     * @param minSize 最小大小
     * @param maxSize 最大大小
     * @return 是否在指定范围内
     */
    public static boolean isSizeBetween(Collection<?> values, int minSize, int maxSize) {
        if (values == null || minSize < 0 || maxSize < minSize) {
            return false;
        }

        int size = values.size();
        return size >= minSize && size <= maxSize;
    }

    /**
     * 校验数组大小是否在指定范围内。
     *
     * @param values  数组
     * @param minSize 最小大小
     * @param maxSize 最大大小
     * @return 是否在指定范围内
     */
    public static boolean isArraySizeBetween(Object[] values, int minSize, int maxSize) {
        if (values == null || minSize < 0 || maxSize < minSize) {
            return false;
        }

        int size = values.length;
        return size >= minSize && size <= maxSize;
    }

    /**
     * 校验集合是否包含指定元素。
     *
     * @param values 集合
     * @param value  元素
     * @return 是否包含
     */
    public static boolean containsElement(Collection<?> values, Object value) {
        return CollUtil.isNotEmpty(values) && values.contains(value);
    }

    /**
     * 校验集合是否包含所有指定元素。
     *
     * @param values  集合
     * @param targets 目标元素集合
     * @return 是否包含所有目标元素
     */
    public static boolean containsAllElements(Collection<?> values, Collection<?> targets) {
        return CollUtil.isNotEmpty(values) && CollUtil.isNotEmpty(targets) && values.containsAll(targets);
    }

    // ============================== 枚举参数校验 ==============================

    /**
     * 校验字符串是否为指定枚举名称。
     *
     * @param enumClass 枚举类型
     * @param name      枚举名称
     * @param <E>       枚举类型
     * @return 是否为指定枚举名称
     */
    public static <E extends Enum<E>> boolean isValidEnumName(Class<E> enumClass, String name) {
        return enumByName(enumClass, name) != null;
    }

    /**
     * 忽略大小写校验字符串是否为指定枚举名称。
     *
     * @param enumClass 枚举类型
     * @param name      枚举名称
     * @param <E>       枚举类型
     * @return 是否为指定枚举名称
     */
    public static <E extends Enum<E>> boolean isValidEnumNameIgnoreCase(Class<E> enumClass, String name) {
        return enumByNameIgnoreCase(enumClass, name) != null;
    }

    /**
     * 校验字段值是否匹配指定枚举字段。
     *
     * @param enumClass  枚举类型
     * @param fieldName  字段名称
     * @param fieldValue 字段值
     * @param <E>        枚举类型
     * @return 是否匹配
     */
    public static <E extends Enum<E>> boolean isValidEnumFieldValue(Class<E> enumClass, String fieldName, Object fieldValue) {
        return enumByFieldValue(enumClass, fieldName, fieldValue) != null;
    }

    // ============================== 文件参数校验 ==============================

    /**
     * 校验文件路径是否存在。
     *
     * @param path 文件路径
     * @return 是否存在
     */
    public static boolean isValidFileExists(String path) {
        return fileExists(path);
    }

    /**
     * 校验文件对象是否存在。
     *
     * @param file 文件对象
     * @return 是否存在
     */
    public static boolean isValidFileExists(File file) {
        return fileExists(file);
    }

    /**
     * 校验文件大小是否小于等于指定字节数。
     *
     * @param file         文件对象
     * @param maxSizeBytes 最大字节数
     * @return 是否小于等于指定字节数
     */
    public static boolean isValidFileSize(File file, long maxSizeBytes) {
        return file != null && FileUtil.isFile(file) && file.length() <= maxSizeBytes;
    }

    /**
     * 校验文件扩展名是否允许。
     *
     * @param file            文件对象
     * @param allowedExtNames 允许的扩展名数组
     * @return 是否允许
     */
    public static boolean isValidFileExt(File file, String... allowedExtNames) {
        if (file == null || allowedExtNames == null || allowedExtNames.length == 0) {
            return false;
        }
        return isExtName(file.getName(), allowedExtNames);
    }

    /**
     * 校验文件名是否合法。
     *
     * @param fileName 文件名
     * @return 是否合法
     */
    public static boolean isValidFileName(String fileName) {
        return StrUtil.isNotBlank(fileName)
                && !ReUtil.contains("[\\\\/:*?\"<>|]", fileName)
                && !StrUtil.equals(fileName, ".")
                && !StrUtil.equals(fileName, "..");
    }

    // ============================== 参数校验内部辅助方法 ==============================

    /**
     * 使用 Luhn 算法校验数字字符串。
     *
     * @param value 数字字符串
     * @return 是否通过校验
     */
    private static boolean checkLuhn(String value) {
        if (StrUtil.isBlank(value) || !ReUtil.isMatch("^\\d+$", value)) {
            return false;
        }

        int sum = 0;
        boolean doubleDigit = false;
        for (int i = value.length() - 1; i >= 0; i--) {
            int digit = value.charAt(i) - '0';
            if (doubleDigit) {
                digit = digit * 2;
                if (digit > 9) {
                    digit = digit - 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }

    // ============================== 基础断言 ==============================

    /**
     * 断言条件为 true。
     *
     * @param expression 条件表达式
     * @param message    异常消息
     */
    public static void assertTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(defaultIfBlank(message, "断言条件必须为true"));
        }
    }

    /**
     * 断言条件为 true。
     *
     * @param expression 条件表达式
     * @param message    异常消息模板
     * @param args       模板参数
     */
    public static void assertTrue(boolean expression, String message, Object... args) {
        if (!expression) {
            throw new IllegalArgumentException(resolveAssertMessage(message, args));
        }
    }

    /**
     * 断言条件为 false。
     *
     * @param expression 条件表达式
     * @param message    异常消息
     */
    public static void assertFalse(boolean expression, String message) {
        if (expression) {
            throw new IllegalArgumentException(defaultIfBlank(message, "断言条件必须为false"));
        }
    }

    /**
     * 断言条件为 false。
     *
     * @param expression 条件表达式
     * @param message    异常消息模板
     * @param args       模板参数
     */
    public static void assertFalse(boolean expression, String message, Object... args) {
        if (expression) {
            throw new IllegalArgumentException(resolveAssertMessage(message, args));
        }
    }

    /**
     * 断言对象为 null。
     *
     * @param value   对象
     * @param message 异常消息
     */
    public static void assertNull(Object value, String message) {
        if (value != null) {
            throw new IllegalArgumentException(defaultIfBlank(message, "对象必须为null"));
        }
    }

    /**
     * 断言对象不为 null，并返回原对象。
     *
     * @param value   对象
     * @param message 异常消息
     * @param <T>     对象类型
     * @return 原对象
     */
    public static <T> T assertNotNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(defaultIfBlank(message, "对象不能为null"));
        }
        return value;
    }

    /**
     * 断言对象不为空，并返回原对象。
     *
     * @param value   对象
     * @param message 异常消息
     * @param <T>     对象类型
     * @return 原对象
     */
    public static <T> T assertNotEmpty(T value, String message) {
        if (ObjectUtil.isEmpty(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "对象不能为空"));
        }
        return value;
    }

    /**
     * 断言对象为空。
     *
     * @param value   对象
     * @param message 异常消息
     */
    public static void assertEmpty(Object value, String message) {
        if (ObjectUtil.isNotEmpty(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "对象必须为空"));
        }
    }

    // ============================== 字符串断言 ==============================

    /**
     * 断言字符串非空白，并返回原字符串。
     *
     * @param value   字符串
     * @param message 异常消息
     * @return 原字符串
     */
    public static String assertNotBlank(String value, String message) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "字符串不能为空白"));
        }
        return value;
    }

    /**
     * 断言字符串非空白，并返回原字符串。
     *
     * @param value   字符串
     * @param message 异常消息模板
     * @param args    模板参数
     * @return 原字符串
     */
    public static String assertNotBlank(String value, String message, Object... args) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(resolveAssertMessage(message, args));
        }
        return value;
    }

    /**
     * 断言字符串为空白。
     *
     * @param value   字符串
     * @param message 异常消息
     */
    public static void assertBlank(String value, String message) {
        if (StrUtil.isNotBlank(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "字符串必须为空白"));
        }
    }

    /**
     * 断言字符串非空，并返回原字符串。
     *
     * @param value   字符串
     * @param message 异常消息
     * @return 原字符串
     */
    public static String assertNotEmpty(String value, String message) {
        if (StrUtil.isEmpty(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "字符串不能为空"));
        }
        return value;
    }

    /**
     * 断言字符串为空。
     *
     * @param value   字符串
     * @param message 异常消息
     */
    public static void assertEmpty(String value, String message) {
        if (StrUtil.isNotEmpty(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "字符串必须为空"));
        }
    }

    /**
     * 断言字符串长度在指定范围内，并返回原字符串。
     *
     * @param value     字符串
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @param message   异常消息
     * @return 原字符串
     */
    public static String assertLengthBetween(String value, int minLength, int maxLength, String message) {
        if (!isLengthBetween(value, minLength, maxLength)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "字符串长度不在允许范围内"));
        }
        return value;
    }

    /**
     * 断言字符串最大长度，并返回原字符串。
     *
     * @param value     字符串
     * @param maxLength 最大长度
     * @param message   异常消息
     * @return 原字符串
     */
    public static String assertMaxLength(String value, int maxLength, String message) {
        if (!isMaxLength(value, maxLength)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "字符串长度超过限制"));
        }
        return value;
    }

    /**
     * 断言字符串最小长度，并返回原字符串。
     *
     * @param value     字符串
     * @param minLength 最小长度
     * @param message   异常消息
     * @return 原字符串
     */
    public static String assertMinLength(String value, int minLength, String message) {
        if (!isMinLength(value, minLength)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "字符串长度不足"));
        }
        return value;
    }

    /**
     * 断言字符串匹配正则表达式，并返回原字符串。
     *
     * @param value   字符串
     * @param regex   正则表达式
     * @param message 异常消息
     * @return 原字符串
     */
    public static String assertMatch(String value, String regex, String message) {
        if (!isMatch(value, regex)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "字符串格式不正确"));
        }
        return value;
    }

    // ============================== 集合与 Map 断言 ==============================

    /**
     * 断言集合非空，并返回原集合。
     *
     * @param values  集合
     * @param message 异常消息
     * @param <T>     集合类型
     * @return 原集合
     */
    public static <T extends Collection<?>> T assertCollNotEmpty(T values, String message) {
        if (CollUtil.isEmpty(values)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "集合不能为空"));
        }
        return values;
    }

    /**
     * 断言集合为空。
     *
     * @param values  集合
     * @param message 异常消息
     */
    public static void assertCollEmpty(Collection<?> values, String message) {
        if (CollUtil.isNotEmpty(values)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "集合必须为空"));
        }
    }

    /**
     * 断言集合大小在指定范围内，并返回原集合。
     *
     * @param values  集合
     * @param minSize 最小大小
     * @param maxSize 最大大小
     * @param message 异常消息
     * @param <T>     集合类型
     * @return 原集合
     */
    public static <T extends Collection<?>> T assertCollSizeBetween(T values, int minSize, int maxSize, String message) {
        if (!isSizeBetween(values, minSize, maxSize)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "集合大小不在允许范围内"));
        }
        return values;
    }

    /**
     * 断言集合包含指定元素，并返回原集合。
     *
     * @param values  集合
     * @param value   元素
     * @param message 异常消息
     * @param <T>     集合类型
     * @return 原集合
     */
    public static <T extends Collection<?>> T assertCollContains(T values, Object value, String message) {
        if (!containsElement(values, value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "集合不包含指定元素"));
        }
        return values;
    }

    /**
     * 断言 Map 非空，并返回原 Map。
     *
     * @param values  Map 对象
     * @param message 异常消息
     * @param <T>     Map 类型
     * @return 原 Map
     */
    public static <T extends Map<?, ?>> T assertMapNotEmpty(T values, String message) {
        if (MapUtil.isEmpty(values)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "Map不能为空"));
        }
        return values;
    }

    /**
     * 断言 Map 为空。
     *
     * @param values  Map 对象
     * @param message 异常消息
     */
    public static void assertMapEmpty(Map<?, ?> values, String message) {
        if (MapUtil.isNotEmpty(values)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "Map必须为空"));
        }
    }

    /**
     * 断言 Map 包含指定 key，并返回原 Map。
     *
     * @param values  Map 对象
     * @param key     key
     * @param message 异常消息
     * @param <T>     Map 类型
     * @return 原 Map
     */
    public static <T extends Map<?, ?>> T assertMapContainsKey(T values, Object key, String message) {
        if (!containsKey(values, key)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "Map不包含指定key"));
        }
        return values;
    }

    // ============================== 数字断言 ==============================

    /**
     * 断言数字不为 null，并返回原数字。
     *
     * @param value   数字
     * @param message 异常消息
     * @param <T>     数字类型
     * @return 原数字
     */
    public static <T extends Number> T assertNumberNotNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(defaultIfBlank(message, "数字不能为空"));
        }
        return value;
    }

    /**
     * 断言数字大于指定值，并返回原数字。
     *
     * @param value   数字
     * @param min     最小值
     * @param message 异常消息
     * @param <T>     数字类型
     * @return 原数字
     */
    public static <T extends Number> T assertGreaterThan(T value, Number min, String message) {
        if (!isGreaterThan(value, min)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "数字必须大于指定值"));
        }
        return value;
    }

    /**
     * 断言数字大于等于指定值，并返回原数字。
     *
     * @param value   数字
     * @param min     最小值
     * @param message 异常消息
     * @param <T>     数字类型
     * @return 原数字
     */
    public static <T extends Number> T assertGreaterThanOrEqual(T value, Number min, String message) {
        if (!isGreaterThanOrEqual(value, min)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "数字必须大于等于指定值"));
        }
        return value;
    }

    /**
     * 断言数字小于指定值，并返回原数字。
     *
     * @param value   数字
     * @param max     最大值
     * @param message 异常消息
     * @param <T>     数字类型
     * @return 原数字
     */
    public static <T extends Number> T assertLessThan(T value, Number max, String message) {
        if (!isLessThan(value, max)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "数字必须小于指定值"));
        }
        return value;
    }

    /**
     * 断言数字小于等于指定值，并返回原数字。
     *
     * @param value   数字
     * @param max     最大值
     * @param message 异常消息
     * @param <T>     数字类型
     * @return 原数字
     */
    public static <T extends Number> T assertLessThanOrEqual(T value, Number max, String message) {
        if (!isLessThanOrEqual(value, max)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "数字必须小于等于指定值"));
        }
        return value;
    }

    /**
     * 断言数字在指定范围内，并返回原数字。
     *
     * @param value   数字
     * @param min     最小值
     * @param max     最大值
     * @param message 异常消息
     * @param <T>     数字类型
     * @return 原数字
     */
    public static <T extends Number> T assertNumberBetween(T value, Number min, Number max, String message) {
        if (!isValidNumberRange(value, min, max)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "数字不在允许范围内"));
        }
        return value;
    }

    /**
     * 断言数字为正数，并返回原数字。
     *
     * @param value   数字
     * @param message 异常消息
     * @param <T>     数字类型
     * @return 原数字
     */
    public static <T extends Number> T assertPositive(T value, String message) {
        if (!isPositive(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "数字必须为正数"));
        }
        return value;
    }

    /**
     * 断言数字为非负数，并返回原数字。
     *
     * @param value   数字
     * @param message 异常消息
     * @param <T>     数字类型
     * @return 原数字
     */
    public static <T extends Number> T assertPositiveOrZero(T value, String message) {
        if (!isPositiveOrZero(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "数字必须为非负数"));
        }
        return value;
    }

    /**
     * 断言数字不为 0，并返回原数字。
     *
     * @param value   数字
     * @param message 异常消息
     * @param <T>     数字类型
     * @return 原数字
     */
    public static <T extends Number> T assertNotZero(T value, String message) {
        if (isZero(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "数字不能为0"));
        }
        return value;
    }

    // ============================== 常用格式断言 ==============================

    /**
     * 断言手机号格式正确，并返回原字符串。
     *
     * @param value   手机号
     * @param message 异常消息
     * @return 原手机号
     */
    public static String assertMobile(String value, String message) {
        if (!isValidMobile(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "手机号格式不正确"));
        }
        return value;
    }

    /**
     * 断言邮箱格式正确，并返回原字符串。
     *
     * @param value   邮箱
     * @param message 异常消息
     * @return 原邮箱
     */
    public static String assertEmail(String value, String message) {
        if (!isValidEmail(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "邮箱格式不正确"));
        }
        return value;
    }

    /**
     * 断言身份证号格式正确，并返回原字符串。
     *
     * @param value   身份证号
     * @param message 异常消息
     * @return 原身份证号
     */
    public static String assertIdCard(String value, String message) {
        if (!isValidIdCard(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "身份证号格式不正确"));
        }
        return value;
    }

    /**
     * 断言 URL 格式正确，并返回原字符串。
     *
     * @param value   URL 字符串
     * @param message 异常消息
     * @return 原 URL 字符串
     */
    public static String assertUrl(String value, String message) {
        if (!isValidUrl(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "URL格式不正确"));
        }
        return value;
    }

    /**
     * 断言 IP 地址格式正确，并返回原字符串。
     *
     * @param value   IP 地址
     * @param message 异常消息
     * @return 原 IP 地址
     */
    public static String assertIp(String value, String message) {
        if (!isValidIp(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "IP地址格式不正确"));
        }
        return value;
    }

    /**
     * 断言金额格式正确，并返回原字符串。
     *
     * @param value   金额字符串
     * @param message 异常消息
     * @return 原金额字符串
     */
    public static String assertAmount(String value, String message) {
        if (!isValidAmount(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "金额格式不正确"));
        }
        return value;
    }

    /**
     * 断言端口号有效，并返回原端口号。
     *
     * @param port    端口号
     * @param message 异常消息
     * @return 原端口号
     */
    public static Integer assertPort(Integer port, String message) {
        if (!isValidPort(port)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "端口号不合法"));
        }
        return port;
    }

    // ============================== 日期时间断言 ==============================

    /**
     * 断言日期字符串格式正确，并返回原字符串。
     *
     * @param value   日期字符串
     * @param message 异常消息
     * @return 原日期字符串
     */
    public static String assertDate(String value, String message) {
        if (!isValidDate(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "日期格式不正确"));
        }
        return value;
    }

    /**
     * 断言日期字符串格式正确，并返回原字符串。
     *
     * @param value   日期字符串
     * @param pattern 日期格式
     * @param message 异常消息
     * @return 原日期字符串
     */
    public static String assertDate(String value, String pattern, String message) {
        if (!isValidDate(value, pattern)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "日期格式不正确"));
        }
        return value;
    }

    /**
     * 断言日期时间字符串格式正确，并返回原字符串。
     *
     * @param value   日期时间字符串
     * @param message 异常消息
     * @return 原日期时间字符串
     */
    public static String assertDateTime(String value, String message) {
        if (!isValidDateTime(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "日期时间格式不正确"));
        }
        return value;
    }

    /**
     * 断言日期时间字符串格式正确，并返回原字符串。
     *
     * @param value   日期时间字符串
     * @param pattern 日期时间格式
     * @param message 异常消息
     * @return 原日期时间字符串
     */
    public static String assertDateTime(String value, String pattern, String message) {
        if (!isValidDateTime(value, pattern)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "日期时间格式不正确"));
        }
        return value;
    }

    /**
     * 断言日期在指定范围内，并返回原日期。
     *
     * @param value   日期
     * @param start   开始日期
     * @param end     结束日期
     * @param message 异常消息
     * @return 原日期
     */
    public static LocalDate assertDateRange(LocalDate value, LocalDate start, LocalDate end, String message) {
        if (!isValidDateRange(value, start, end)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "日期不在允许范围内"));
        }
        return value;
    }

    /**
     * 断言日期时间在指定范围内，并返回原日期时间。
     *
     * @param value   日期时间
     * @param start   开始日期时间
     * @param end     结束日期时间
     * @param message 异常消息
     * @return 原日期时间
     */
    public static LocalDateTime assertDateTimeRange(LocalDateTime value, LocalDateTime start, LocalDateTime end, String message) {
        if (!isValidDateTimeRange(value, start, end)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "日期时间不在允许范围内"));
        }
        return value;
    }

    /**
     * 断言日期为过去日期，并返回原日期。
     *
     * @param value   日期
     * @param message 异常消息
     * @return 原日期
     */
    public static LocalDate assertPastDate(LocalDate value, String message) {
        if (!isPastDate(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "日期必须为过去日期"));
        }
        return value;
    }

    /**
     * 断言日期为未来日期，并返回原日期。
     *
     * @param value   日期
     * @param message 异常消息
     * @return 原日期
     */
    public static LocalDate assertFutureDate(LocalDate value, String message) {
        if (!isFutureDate(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "日期必须为未来日期"));
        }
        return value;
    }

    // ============================== 枚举与文件断言 ==============================

    /**
     * 断言枚举名称有效，并返回枚举对象。
     *
     * @param enumClass 枚举类型
     * @param name      枚举名称
     * @param message   异常消息
     * @param <E>       枚举类型
     * @return 枚举对象
     */
    public static <E extends Enum<E>> E assertEnumName(Class<E> enumClass, String name, String message) {
        E enumValue = enumByName(enumClass, name);
        if (enumValue == null) {
            throw new IllegalArgumentException(defaultIfBlank(message, "枚举名称不合法"));
        }
        return enumValue;
    }

    /**
     * 断言枚举名称有效，忽略大小写，并返回枚举对象。
     *
     * @param enumClass 枚举类型
     * @param name      枚举名称
     * @param message   异常消息
     * @param <E>       枚举类型
     * @return 枚举对象
     */
    public static <E extends Enum<E>> E assertEnumNameIgnoreCase(Class<E> enumClass, String name, String message) {
        E enumValue = enumByNameIgnoreCase(enumClass, name);
        if (enumValue == null) {
            throw new IllegalArgumentException(defaultIfBlank(message, "枚举名称不合法"));
        }
        return enumValue;
    }

    /**
     * 断言文件存在，并返回文件对象。
     *
     * @param file    文件对象
     * @param message 异常消息
     * @return 文件对象
     */
    public static File assertFileExists(File file, String message) {
        if (!fileExists(file)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "文件不存在"));
        }
        return file;
    }

    /**
     * 断言路径存在，并返回文件对象。
     *
     * @param path    文件路径
     * @param message 异常消息
     * @return 文件对象
     */
    public static File assertFileExists(String path, String message) {
        File file = toFile(path);
        return assertFileExists(file, message);
    }

    /**
     * 断言对象为普通文件，并返回文件对象。
     *
     * @param file    文件对象
     * @param message 异常消息
     * @return 文件对象
     */
    public static File assertIsFile(File file, String message) {
        if (!isFile(file)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "目标不是有效文件"));
        }
        return file;
    }

    /**
     * 断言对象为目录，并返回目录对象。
     *
     * @param dir     目录对象
     * @param message 异常消息
     * @return 目录对象
     */
    public static File assertIsDirectory(File dir, String message) {
        if (!isDirectory(dir)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "目标不是有效目录"));
        }
        return dir;
    }

    /**
     * 断言文件扩展名合法，并返回文件对象。
     *
     * @param file     文件对象
     * @param message  异常消息
     * @param extNames 允许的扩展名数组
     * @return 文件对象
     */
    public static File assertFileExt(File file, String message, String... extNames) {
        if (!isValidFileExt(file, extNames)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "文件扩展名不允许"));
        }
        return file;
    }

    // ============================== 状态断言 ==============================

    /**
     * 断言状态为 true。
     *
     * @param expression 状态表达式
     * @param message    异常消息
     */
    public static void assertState(boolean expression, String message) {
        if (!expression) {
            throw new IllegalStateException(defaultIfBlank(message, "当前状态不允许执行该操作"));
        }
    }

    /**
     * 断言状态为 true。
     *
     * @param expression 状态表达式
     * @param message    异常消息模板
     * @param args       模板参数
     */
    public static void assertState(boolean expression, String message, Object... args) {
        if (!expression) {
            throw new IllegalStateException(resolveAssertMessage(message, args));
        }
    }

    /**
     * 断言状态为 false。
     *
     * @param expression 状态表达式
     * @param message    异常消息
     */
    public static void assertStateFalse(boolean expression, String message) {
        if (expression) {
            throw new IllegalStateException(defaultIfBlank(message, "当前状态不允许执行该操作"));
        }
    }

    /**
     * 断言对象状态不为 null，并返回原对象。
     *
     * @param value   对象
     * @param message 异常消息
     * @param <T>     对象类型
     * @return 原对象
     */
    public static <T> T assertStateNotNull(T value, String message) {
        if (value == null) {
            throw new IllegalStateException(defaultIfBlank(message, "状态对象不能为空"));
        }
        return value;
    }

    /**
     * 断言对象状态非空，并返回原对象。
     *
     * @param value   对象
     * @param message 异常消息
     * @param <T>     对象类型
     * @return 原对象
     */
    public static <T> T assertStateNotEmpty(T value, String message) {
        if (ObjectUtil.isEmpty(value)) {
            throw new IllegalStateException(defaultIfBlank(message, "状态对象不能为空"));
        }
        return value;
    }

    // ============================== 自定义异常抛出 ==============================

    /**
     * 条件成立时抛出指定运行时异常。
     *
     * @param expression 条件表达式
     * @param exception  运行时异常
     */
    public static void throwIf(boolean expression, RuntimeException exception) {
        if (expression) {
            throw exception == null ? new IllegalStateException("操作异常") : exception;
        }
    }

    /**
     * 条件成立时通过异常提供器抛出运行时异常。
     *
     * @param expression        条件表达式
     * @param exceptionSupplier 异常提供器
     */
    public static void throwIf(boolean expression, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!expression) {
            return;
        }

        RuntimeException exception = exceptionSupplier == null ? null : exceptionSupplier.get();
        throw exception == null ? new IllegalStateException("操作异常") : exception;
    }

    /**
     * 条件不成立时抛出指定运行时异常。
     *
     * @param expression 条件表达式
     * @param exception  运行时异常
     */
    public static void throwIfFalse(boolean expression, RuntimeException exception) {
        throwIf(!expression, exception);
    }

    /**
     * 对象为 null 时抛出指定运行时异常，并返回原对象。
     *
     * @param value     对象
     * @param exception 运行时异常
     * @param <T>       对象类型
     * @return 原对象
     */
    public static <T> T throwIfNull(T value, RuntimeException exception) {
        if (value == null) {
            throw exception == null ? new IllegalArgumentException("对象不能为空") : exception;
        }
        return value;
    }

    /**
     * 字符串为空白时抛出指定运行时异常，并返回原字符串。
     *
     * @param value     字符串
     * @param exception 运行时异常
     * @return 原字符串
     */
    public static String throwIfBlank(String value, RuntimeException exception) {
        if (StrUtil.isBlank(value)) {
            throw exception == null ? new IllegalArgumentException("字符串不能为空白") : exception;
        }
        return value;
    }

    /**
     * 集合为空时抛出指定运行时异常，并返回原集合。
     *
     * @param values    集合
     * @param exception 运行时异常
     * @param <T>       集合类型
     * @return 原集合
     */
    public static <T extends Collection<?>> T throwIfCollEmpty(T values, RuntimeException exception) {
        if (CollUtil.isEmpty(values)) {
            throw exception == null ? new IllegalArgumentException("集合不能为空") : exception;
        }
        return values;
    }

    /**
     * Map 为空时抛出指定运行时异常，并返回原 Map。
     *
     * @param values    Map 对象
     * @param exception 运行时异常
     * @param <T>       Map 类型
     * @return 原 Map
     */
    public static <T extends Map<?, ?>> T throwIfMapEmpty(T values, RuntimeException exception) {
        if (MapUtil.isEmpty(values)) {
            throw exception == null ? new IllegalArgumentException("Map不能为空") : exception;
        }
        return values;
    }

    // ============================== 异常构造 ==============================

    /**
     * 创建参数异常。
     *
     * @param message 异常消息
     * @return 参数异常
     */
    public static IllegalArgumentException illegalArgument(String message) {
        return new IllegalArgumentException(defaultIfBlank(message, "参数异常"));
    }

    /**
     * 创建参数异常。
     *
     * @param message 异常消息
     * @param cause   原始异常
     * @return 参数异常
     */
    public static IllegalArgumentException illegalArgument(String message, Throwable cause) {
        return new IllegalArgumentException(defaultIfBlank(message, "参数异常"), cause);
    }

    /**
     * 创建状态异常。
     *
     * @param message 异常消息
     * @return 状态异常
     */
    public static IllegalStateException illegalState(String message) {
        return new IllegalStateException(defaultIfBlank(message, "状态异常"));
    }

    /**
     * 创建状态异常。
     *
     * @param message 异常消息
     * @param cause   原始异常
     * @return 状态异常
     */
    public static IllegalStateException illegalState(String message, Throwable cause) {
        return new IllegalStateException(defaultIfBlank(message, "状态异常"), cause);
    }

    /**
     * 创建不支持操作异常。
     *
     * @param message 异常消息
     * @return 不支持操作异常
     */
    public static UnsupportedOperationException unsupportedOperation(String message) {
        return new UnsupportedOperationException(defaultIfBlank(message, "不支持当前操作"));
    }

    /**
     * 将 Throwable 包装为 RuntimeException。
     *
     * @param throwable 异常对象
     * @return 运行时异常
     */
    public static RuntimeException toRuntimeException(Throwable throwable) {
        if (throwable == null) {
            return new RuntimeException("未知异常");
        }
        if (throwable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException(throwable.getMessage(), throwable);
    }

    /**
     * 将 Throwable 包装为 RuntimeException，并指定异常消息。
     *
     * @param throwable 异常对象
     * @param message   异常消息
     * @return 运行时异常
     */
    public static RuntimeException toRuntimeException(Throwable throwable, String message) {
        if (throwable == null) {
            return new RuntimeException(defaultIfBlank(message, "未知异常"));
        }
        if (throwable instanceof RuntimeException runtimeException && StrUtil.isBlank(message)) {
            return runtimeException;
        }
        return new RuntimeException(defaultIfBlank(message, throwable.getMessage()), throwable);
    }

    // ============================== 异常信息提取 ==============================

    /**
     * 获取异常消息。
     *
     * @param throwable 异常对象
     * @return 异常消息
     */
    public static String getExceptionMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        return throwable.getMessage();
    }

    /**
     * 获取异常简短消息。
     *
     * @param throwable 异常对象
     * @return 异常简短消息
     */
    public static String getSimpleExceptionMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        String message = throwable.getMessage();
        if (StrUtil.isBlank(message)) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getClass().getSimpleName() + ": " + message;
    }

    /**
     * 获取异常根因。
     *
     * @param throwable 异常对象
     * @return 根因异常
     */
    public static Throwable getRootCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        return ExceptionUtil.getRootCause(throwable);
    }

    /**
     * 获取异常根因消息。
     *
     * @param throwable 异常对象
     * @return 根因消息
     */
    public static String getRootCauseMessage(Throwable throwable) {
        Throwable rootCause = getRootCause(throwable);
        return rootCause == null ? null : getSimpleExceptionMessage(rootCause);
    }

    /**
     * 获取异常堆栈字符串。
     *
     * @param throwable 异常对象
     * @return 异常堆栈字符串
     */
    public static String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return EMPTY;
        }
        return ExceptionUtil.stacktraceToString(throwable);
    }

    /**
     * 获取异常堆栈字符串，并限制最大长度。
     *
     * @param throwable 异常对象
     * @param limit     最大长度
     * @return 异常堆栈字符串
     */
    public static String getStackTrace(Throwable throwable, int limit) {
        if (throwable == null) {
            return EMPTY;
        }

        String stackTrace = getStackTrace(throwable);
        if (limit <= 0 || stackTrace.length() <= limit) {
            return stackTrace;
        }
        return stackTrace.substring(0, limit);
    }

    /**
     * 判断异常链中是否包含指定异常类型。
     *
     * @param throwable      异常对象
     * @param exceptionClass 异常类型
     * @return 是否包含指定异常类型
     */
    public static boolean containsException(Throwable throwable, Class<? extends Throwable> exceptionClass) {
        if (throwable == null || exceptionClass == null) {
            return false;
        }

        Throwable current = throwable;
        while (current != null) {
            if (exceptionClass.isAssignableFrom(current.getClass())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 查找异常链中的指定异常类型。
     *
     * @param throwable      异常对象
     * @param exceptionClass 异常类型
     * @param <T>            异常类型
     * @return 匹配的异常对象
     */
    public static <T extends Throwable> T findException(Throwable throwable, Class<T> exceptionClass) {
        if (throwable == null || exceptionClass == null) {
            return null;
        }

        Throwable current = throwable;
        while (current != null) {
            if (exceptionClass.isAssignableFrom(current.getClass())) {
                return exceptionClass.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    // ============================== 异常执行包装 ==============================

    /**
     * 安全执行无返回值操作，异常时包装为 RuntimeException。
     *
     * @param runnable 可执行操作
     */
    public static void runUnchecked(CheckedRunnable runnable) {
        if (runnable == null) {
            return;
        }

        try {
            runnable.run();
        } catch (Exception e) {
            throw toRuntimeException(e);
        }
    }

    /**
     * 安全执行无返回值操作，异常时包装为 RuntimeException。
     *
     * @param runnable 可执行操作
     * @param message  异常消息
     */
    public static void runUnchecked(CheckedRunnable runnable, String message) {
        if (runnable == null) {
            return;
        }

        try {
            runnable.run();
        } catch (Exception e) {
            throw toRuntimeException(e, message);
        }
    }

    /**
     * 安全执行有返回值操作，异常时包装为 RuntimeException。
     *
     * @param supplier 可执行操作
     * @param <T>      返回值类型
     * @return 操作结果
     */
    public static <T> T callUnchecked(CheckedSupplier<T> supplier) {
        if (supplier == null) {
            return null;
        }

        try {
            return supplier.get();
        } catch (Exception e) {
            throw toRuntimeException(e);
        }
    }

    /**
     * 安全执行有返回值操作，异常时返回默认值。
     *
     * @param supplier     可执行操作
     * @param defaultValue 默认值
     * @param <T>          返回值类型
     * @return 操作结果或默认值
     */
    public static <T> T callOrDefault(CheckedSupplier<T> supplier, T defaultValue) {
        if (supplier == null) {
            return defaultValue;
        }

        try {
            return supplier.get();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 安全执行无返回值操作，吞掉异常。
     *
     * @param runnable 可执行操作
     * @return 是否执行成功
     */
    public static boolean runQuietly(CheckedRunnable runnable) {
        if (runnable == null) {
            return true;
        }

        try {
            runnable.run();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ============================== 异常断言内部辅助方法 ==============================

    /**
     * 解析断言异常消息。
     *
     * @param message 异常消息模板
     * @param args    模板参数
     * @return 异常消息
     */
    private static String resolveAssertMessage(String message, Object... args) {
        if (StrUtil.isBlank(message)) {
            return "参数断言失败";
        }
        if (args == null || args.length == 0) {
            return message;
        }
        return StrUtil.format(message, args);
    }

    /**
     * 可抛出受检异常的无返回值操作。
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @FunctionalInterface
    public interface CheckedRunnable {

        /**
         * 执行操作。
         *
         * @throws Exception 执行异常
         */
        void run() throws Exception;

    }

    /**
     * 可抛出受检异常的有返回值操作。
     *
     * @param <T> 返回值类型
     * @author Ateng
     * @since 2026-04-29
     */
    @FunctionalInterface
    public interface CheckedSupplier<T> {

        /**
         * 执行操作并返回结果。
         *
         * @return 操作结果
         * @throws Exception 执行异常
         */
        T get() throws Exception;

    }

    // ============================== 系统属性 ==============================

    /**
     * 获取系统属性。
     *
     * @param key 属性名
     * @return 属性值
     */
    public static String getSystemProperty(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        return System.getProperty(key);
    }

    /**
     * 获取系统属性，属性不存在时返回默认值。
     *
     * @param key          属性名
     * @param defaultValue 默认值
     * @return 属性值
     */
    public static String getSystemProperty(String key, String defaultValue) {
        return defaultIfBlank(getSystemProperty(key), defaultValue);
    }

    /**
     * 设置系统属性。
     *
     * @param key   属性名
     * @param value 属性值
     * @return 上一个属性值
     */
    public static String setSystemProperty(String key, String value) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        return System.setProperty(key, defaultIfNull(value, EMPTY));
    }

    /**
     * 清除系统属性。
     *
     * @param key 属性名
     * @return 被清除的属性值
     */
    public static String clearSystemProperty(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        return System.clearProperty(key);
    }

    /**
     * 判断系统属性是否存在。
     *
     * @param key 属性名
     * @return 是否存在
     */
    public static boolean hasSystemProperty(String key) {
        return StrUtil.isNotBlank(getSystemProperty(key));
    }

    /**
     * 获取系统属性对象。
     *
     * @return 系统属性对象
     */
    public static Properties getSystemProperties() {
        return System.getProperties();
    }

    /**
     * 获取系统属性 Map。
     *
     * @return 系统属性 Map
     */
    public static Map<String, String> getSystemPropertyMap() {
        Map<String, String> result = new LinkedHashMap<>();
        Properties properties = System.getProperties();
        for (String name : properties.stringPropertyNames()) {
            result.put(name, properties.getProperty(name));
        }
        return result;
    }

    /**
     * 获取布尔类型系统属性。
     *
     * @param key 属性名
     * @return 布尔值
     */
    public static Boolean getSystemPropertyBool(String key) {
        return toBool(getSystemProperty(key));
    }

    /**
     * 获取布尔类型系统属性，属性不存在或转换失败时返回默认值。
     *
     * @param key          属性名
     * @param defaultValue 默认值
     * @return 布尔值
     */
    public static Boolean getSystemPropertyBool(String key, Boolean defaultValue) {
        return toBool(getSystemProperty(key), defaultValue);
    }

    /**
     * 获取 Integer 类型系统属性。
     *
     * @param key 属性名
     * @return Integer 值
     */
    public static Integer getSystemPropertyInt(String key) {
        return toInt(getSystemProperty(key));
    }

    /**
     * 获取 Integer 类型系统属性，属性不存在或转换失败时返回默认值。
     *
     * @param key          属性名
     * @param defaultValue 默认值
     * @return Integer 值
     */
    public static Integer getSystemPropertyInt(String key, Integer defaultValue) {
        return toInt(getSystemProperty(key), defaultValue);
    }

    /**
     * 获取 Long 类型系统属性。
     *
     * @param key 属性名
     * @return Long 值
     */
    public static Long getSystemPropertyLong(String key) {
        return toLong(getSystemProperty(key));
    }

    /**
     * 获取 Long 类型系统属性，属性不存在或转换失败时返回默认值。
     *
     * @param key          属性名
     * @param defaultValue 默认值
     * @return Long 值
     */
    public static Long getSystemPropertyLong(String key, Long defaultValue) {
        return toLong(getSystemProperty(key), defaultValue);
    }

    // ============================== 环境变量 ==============================

    /**
     * 获取环境变量。
     *
     * @param key 环境变量名
     * @return 环境变量值
     */
    public static String getEnv(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        return System.getenv(key);
    }

    /**
     * 获取环境变量，变量不存在时返回默认值。
     *
     * @param key          环境变量名
     * @param defaultValue 默认值
     * @return 环境变量值
     */
    public static String getEnv(String key, String defaultValue) {
        return defaultIfBlank(getEnv(key), defaultValue);
    }

    /**
     * 判断环境变量是否存在。
     *
     * @param key 环境变量名
     * @return 是否存在
     */
    public static boolean hasEnv(String key) {
        return StrUtil.isNotBlank(getEnv(key));
    }

    /**
     * 获取环境变量 Map。
     *
     * @return 环境变量 Map
     */
    public static Map<String, String> getEnvMap() {
        return new LinkedHashMap<>(System.getenv());
    }

    /**
     * 获取必填环境变量。
     *
     * @param key 环境变量名
     * @return 环境变量值
     */
    public static String getRequiredEnv(String key) {
        String value = getEnv(key);
        if (StrUtil.isBlank(value)) {
            throw new IllegalStateException("环境变量不存在：" + key);
        }
        return value;
    }

    /**
     * 获取环境变量或系统属性，优先读取环境变量。
     *
     * @param envKey      环境变量名
     * @param propertyKey 系统属性名
     * @return 配置值
     */
    public static String getEnvOrProperty(String envKey, String propertyKey) {
        String envValue = getEnv(envKey);
        if (StrUtil.isNotBlank(envValue)) {
            return envValue;
        }
        return getSystemProperty(propertyKey);
    }

    /**
     * 获取环境变量或系统属性，优先读取环境变量。
     *
     * @param envKey       环境变量名
     * @param propertyKey  系统属性名
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static String getEnvOrProperty(String envKey, String propertyKey, String defaultValue) {
        return defaultIfBlank(getEnvOrProperty(envKey, propertyKey), defaultValue);
    }

    // ============================== 操作系统信息 ==============================

    /**
     * 获取操作系统名称。
     *
     * @return 操作系统名称
     */
    public static String getOsName() {
        return getSystemProperty("os.name");
    }

    /**
     * 获取操作系统版本。
     *
     * @return 操作系统版本
     */
    public static String getOsVersion() {
        return getSystemProperty("os.version");
    }

    /**
     * 获取操作系统架构。
     *
     * @return 操作系统架构
     */
    public static String getOsArch() {
        return getSystemProperty("os.arch");
    }

    /**
     * 判断当前系统是否为 Windows。
     *
     * @return 是否为 Windows
     */
    public static boolean isWindows() {
        return StrUtil.containsIgnoreCase(getOsName(), "windows");
    }

    /**
     * 判断当前系统是否为 Linux。
     *
     * @return 是否为 Linux
     */
    public static boolean isLinux() {
        return StrUtil.containsIgnoreCase(getOsName(), "linux");
    }

    /**
     * 判断当前系统是否为 Mac。
     *
     * @return 是否为 Mac
     */
    public static boolean isMac() {
        String osName = getOsName();
        return StrUtil.containsIgnoreCase(osName, "mac") || StrUtil.containsIgnoreCase(osName, "darwin");
    }

    /**
     * 判断当前系统是否为 Unix 类系统。
     *
     * @return 是否为 Unix 类系统
     */
    public static boolean isUnixLike() {
        String osName = toLowerCase(defaultIfNull(getOsName(), EMPTY));
        return StrUtil.containsAny(osName, "linux", "unix", "aix", "freebsd", "openbsd", "netbsd", "sunos", "mac", "darwin");
    }

    /**
     * 获取系统文件分隔符。
     *
     * @return 文件分隔符
     */
    public static String getFileSeparator() {
        return getSystemProperty("file.separator");
    }

    /**
     * 获取系统路径分隔符。
     *
     * @return 路径分隔符
     */
    public static String getPathSeparator() {
        return getSystemProperty("path.separator");
    }

    /**
     * 获取系统换行符。
     *
     * @return 换行符
     */
    public static String getLineSeparator() {
        return getSystemProperty("line.separator");
    }

    /**
     * 获取默认字符集名称。
     *
     * @return 默认字符集名称
     */
    public static String getDefaultCharsetName() {
        return Charset.defaultCharset().name();
    }

    /**
     * 获取系统默认时区 ID。
     *
     * @return 时区 ID
     */
    public static String getSystemZoneId() {
        return ZoneId.systemDefault().getId();
    }

    /**
     * 获取系统可用处理器数量。
     *
     * @return 可用处理器数量
     */
    public static int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    // ============================== 用户与路径信息 ==============================

    /**
     * 获取当前用户名称。
     *
     * @return 当前用户名称
     */
    public static String getUserName() {
        return getSystemProperty("user.name");
    }

    /**
     * 获取用户主目录。
     *
     * @return 用户主目录
     */
    public static String getUserHome() {
        return getSystemProperty("user.home");
    }

    /**
     * 获取用户主目录文件对象。
     *
     * @return 用户主目录文件对象
     */
    public static File getUserHomeFile() {
        return toFile(getUserHome());
    }

    /**
     * 获取当前工作目录。
     *
     * @return 当前工作目录
     */
    public static String getUserDir() {
        return getSystemProperty("user.dir");
    }

    /**
     * 获取当前工作目录文件对象。
     *
     * @return 当前工作目录文件对象
     */
    public static File getUserDirFile() {
        return toFile(getUserDir());
    }

    /**
     * 获取 Java 临时目录。
     *
     * @return Java 临时目录
     */
    public static String getJavaTempDir() {
        return getSystemProperty("java.io.tmpdir");
    }

    /**
     * 获取 Java 临时目录文件对象。
     *
     * @return Java 临时目录文件对象
     */
    public static File getJavaTempDirFile() {
        return toFile(getJavaTempDir());
    }

    /**
     * 基于用户主目录拼接路径。
     *
     * @param paths 路径片段
     * @return 拼接后的路径
     */
    public static String userHomePath(String... paths) {
        return joinBasePath(getUserHome(), paths);
    }

    /**
     * 基于当前工作目录拼接路径。
     *
     * @param paths 路径片段
     * @return 拼接后的路径
     */
    public static String userDirPath(String... paths) {
        return joinBasePath(getUserDir(), paths);
    }

    /**
     * 基于临时目录拼接路径。
     *
     * @param paths 路径片段
     * @return 拼接后的路径
     */
    public static String tempDirPath(String... paths) {
        return joinBasePath(getJavaTempDir(), paths);
    }

    /**
     * 判断当前工作目录是否包含指定文件。
     *
     * @param paths 文件路径片段
     * @return 是否存在
     */
    public static boolean existsInUserDir(String... paths) {
        return fileExists(userDirPath(paths));
    }

    /**
     * 判断用户主目录是否包含指定文件。
     *
     * @param paths 文件路径片段
     * @return 是否存在
     */
    public static boolean existsInUserHome(String... paths) {
        return fileExists(userHomePath(paths));
    }

    // ============================== Java 与 JVM 信息 ==============================

    /**
     * 获取 Java 版本。
     *
     * @return Java 版本
     */
    public static String getJavaVersion() {
        return getSystemProperty("java.version");
    }

    /**
     * 获取 Java 主版本号。
     *
     * @return Java 主版本号
     */
    public static Integer getJavaMajorVersion() {
        String version = getJavaVersion();
        if (StrUtil.isBlank(version)) {
            return null;
        }

        if (version.startsWith("1.")) {
            return toInt(sub(version, 2, 3));
        }

        String major = splitTrimIgnoreBlank(version, ".").stream().findFirst().orElse(null);
        return toInt(major);
    }

    /**
     * 判断当前 Java 版本是否大于等于指定主版本。
     *
     * @param majorVersion 主版本号
     * @return 是否大于等于
     */
    public static boolean isJavaVersionAtLeast(int majorVersion) {
        Integer currentVersion = getJavaMajorVersion();
        return currentVersion != null && currentVersion >= majorVersion;
    }

    /**
     * 获取 Java 安装目录。
     *
     * @return Java 安装目录
     */
    public static String getJavaHome() {
        return getSystemProperty("java.home");
    }

    /**
     * 获取 Java 供应商。
     *
     * @return Java 供应商
     */
    public static String getJavaVendor() {
        return getSystemProperty("java.vendor");
    }

    /**
     * 获取 JVM 名称。
     *
     * @return JVM 名称
     */
    public static String getJvmName() {
        return getSystemProperty("java.vm.name");
    }

    /**
     * 获取 JVM 版本。
     *
     * @return JVM 版本
     */
    public static String getJvmVersion() {
        return getSystemProperty("java.vm.version");
    }

    /**
     * 获取 JVM 供应商。
     *
     * @return JVM 供应商
     */
    public static String getJvmVendor() {
        return getSystemProperty("java.vm.vendor");
    }

    /**
     * 获取 Java 类路径。
     *
     * @return Java 类路径
     */
    public static String getClassPath() {
        return getSystemProperty("java.class.path");
    }

    /**
     * 获取 Java 库路径。
     *
     * @return Java 库路径
     */
    public static String getLibraryPath() {
        return getSystemProperty("java.library.path");
    }

    /**
     * 获取 JVM 运行时 Bean。
     *
     * @return JVM 运行时 Bean
     */
    public static RuntimeMXBean getRuntimeMxBean() {
        return ManagementFactory.getRuntimeMXBean();
    }

    /**
     * 获取 JVM 启动时间戳。
     *
     * @return JVM 启动时间戳
     */
    public static long getJvmStartTime() {
        return getRuntimeMxBean().getStartTime();
    }

    /**
     * 获取 JVM 运行时长，单位毫秒。
     *
     * @return JVM 运行时长
     */
    public static long getJvmUptime() {
        return getRuntimeMxBean().getUptime();
    }

    /**
     * 获取 JVM 启动参数。
     *
     * @return JVM 启动参数集合
     */
    public static List<String> getJvmInputArguments() {
        return new ArrayList<>(getRuntimeMxBean().getInputArguments());
    }

    /**
     * 获取 JVM 名称标识。
     *
     * @return JVM 名称标识
     */
    public static String getJvmRuntimeName() {
        return getRuntimeMxBean().getName();
    }

    // ============================== 进程信息 ==============================

    /**
     * 获取当前进程 ID。
     *
     * @return 当前进程 ID
     */
    public static long getPid() {
        return ProcessHandle.current().pid();
    }

    /**
     * 获取当前进程命令。
     *
     * @return 当前进程命令
     */
    public static String getProcessCommand() {
        return ProcessHandle.current().info().command().orElse(null);
    }

    /**
     * 获取当前进程命令行。
     *
     * @return 当前进程命令行
     */
    public static String getProcessCommandLine() {
        return ProcessHandle.current().info().commandLine().orElse(null);
    }

    /**
     * 获取当前进程启动用户。
     *
     * @return 当前进程启动用户
     */
    public static String getProcessUser() {
        return ProcessHandle.current().info().user().orElse(getUserName());
    }

    /**
     * 判断当前进程是否存活。
     *
     * @return 是否存活
     */
    public static boolean isCurrentProcessAlive() {
        return ProcessHandle.current().isAlive();
    }

    // ============================== 内存信息 ==============================

    /**
     * 获取最大可用内存字节数。
     *
     * @return 最大可用内存字节数
     */
    public static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    /**
     * 获取 JVM 当前总内存字节数。
     *
     * @return 当前总内存字节数
     */
    public static long getTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    /**
     * 获取 JVM 当前空闲内存字节数。
     *
     * @return 当前空闲内存字节数
     */
    public static long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    /**
     * 获取 JVM 当前已用内存字节数。
     *
     * @return 当前已用内存字节数
     */
    public static long getUsedMemory() {
        return getTotalMemory() - getFreeMemory();
    }

    /**
     * 获取 JVM 当前可用内存字节数。
     *
     * @return 当前可用内存字节数
     */
    public static long getUsableMemory() {
        return getMaxMemory() - getUsedMemory();
    }

    /**
     * 获取已用内存 MB。
     *
     * @return 已用内存 MB
     */
    public static BigDecimal getUsedMemoryMb() {
        return divide(getUsedMemory(), BYTES_PER_MB, 2);
    }

    /**
     * 获取最大内存 MB。
     *
     * @return 最大内存 MB
     */
    public static BigDecimal getMaxMemoryMb() {
        return divide(getMaxMemory(), BYTES_PER_MB, 2);
    }

    /**
     * 获取内存使用率百分比。
     *
     * @return 内存使用率百分比
     */
    public static BigDecimal getMemoryUsagePercent() {
        return percent(getUsedMemory(), getMaxMemory());
    }

    /**
     * 获取内存信息 Map。
     *
     * @return 内存信息 Map
     */
    public static Map<String, Object> getMemoryInfoMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("maxMemory", getMaxMemory());
        result.put("totalMemory", getTotalMemory());
        result.put("freeMemory", getFreeMemory());
        result.put("usedMemory", getUsedMemory());
        result.put("usableMemory", getUsableMemory());
        result.put("usedMemoryMb", getUsedMemoryMb());
        result.put("maxMemoryMb", getMaxMemoryMb());
        result.put("memoryUsagePercent", getMemoryUsagePercent());
        return result;
    }

    /**
     * 格式化最大可用内存。
     *
     * @return 最大可用内存
     */
    public static String formatMaxMemory() {
        return formatFileSize(getMaxMemory());
    }

    /**
     * 格式化当前总内存。
     *
     * @return 当前总内存
     */
    public static String formatTotalMemory() {
        return formatFileSize(getTotalMemory());
    }

    /**
     * 格式化当前空闲内存。
     *
     * @return 当前空闲内存
     */
    public static String formatFreeMemory() {
        return formatFileSize(getFreeMemory());
    }

    /**
     * 格式化当前已用内存。
     *
     * @return 当前已用内存
     */
    public static String formatUsedMemory() {
        return formatFileSize(getUsedMemory());
    }

    /**
     * 主动触发 GC。
     */
    public static void gc() {
        System.gc();
    }

    // ============================== 主机与网络信息 ==============================

    /**
     * 获取本地主机对象。
     *
     * @return 本地主机对象
     */
    public static InetAddress getLocalHost() {
        try {
            return InetAddress.getLocalHost();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取本地主机名称。
     *
     * @return 本地主机名称
     */
    public static String getLocalHostName() {
        InetAddress localHost = getLocalHost();
        return localHost == null ? null : localHost.getHostName();
    }

    /**
     * 获取本地主机地址。
     *
     * @return 本地主机地址
     */
    public static String getLocalHostAddress() {
        InetAddress localHost = getLocalHost();
        return localHost == null ? null : localHost.getHostAddress();
    }

    /**
     * 获取所有网卡 IP 地址。
     *
     * @return IP 地址集合
     */
    public static List<String> getLocalIpList() {
        List<String> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            if (networkInterfaces == null) {
                return result;
            }

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (networkInterface == null || !networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }

                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress == null || inetAddress.isLoopbackAddress()) {
                        continue;
                    }
                    result.add(inetAddress.getHostAddress());
                }
            }
        } catch (Exception e) {
            return result;
        }
        return result;
    }

    /**
     * 获取第一个本地 IPv4 地址。
     *
     * @return 本地 IPv4 地址
     */
    public static String getFirstLocalIpv4() {
        for (String ip : getLocalIpList()) {
            if (isValidIpv4(ip)) {
                return ip;
            }
        }
        return null;
    }

    /**
     * 获取第一个本地 IPv6 地址。
     *
     * @return 本地 IPv6 地址
     */
    public static String getFirstLocalIpv6() {
        for (String ip : getLocalIpList()) {
            if (isValidIpv6(ip)) {
                return ip;
            }
        }
        return null;
    }

    /**
     * 判断当前主机是否可解析。
     *
     * @return 是否可解析
     */
    public static boolean isLocalHostAvailable() {
        return getLocalHost() != null;
    }

    // ============================== Spring Profile 环境 ==============================

    /**
     * 获取当前激活的 Spring Profile 字符串。
     *
     * @return Spring Profile 字符串
     */
    public static String getActiveProfile() {
        String activeProfile = firstNotBlank(
                getSystemProperty(SPRING_PROFILES_ACTIVE_PROPERTY),
                getEnv(SPRING_PROFILES_ACTIVE_ENV),
                getSystemProperty(SPRING_PROFILES_DEFAULT_PROPERTY),
                getEnv(SPRING_PROFILES_DEFAULT_ENV)
        );
        return blankToNull(activeProfile);
    }

    /**
     * 获取当前激活的 Spring Profile 集合。
     *
     * @return Spring Profile 集合
     */
    public static List<String> getActiveProfiles() {
        String activeProfile = getActiveProfile();
        if (StrUtil.isBlank(activeProfile)) {
            return new ArrayList<>();
        }
        return splitTrimIgnoreBlank(activeProfile, ",");
    }

    /**
     * 判断是否激活指定 Spring Profile。
     *
     * @param profile Spring Profile
     * @return 是否激活
     */
    public static boolean isProfileActive(String profile) {
        if (StrUtil.isBlank(profile)) {
            return false;
        }

        for (String activeProfile : getActiveProfiles()) {
            if (StrUtil.equalsIgnoreCase(activeProfile, profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为 local 环境。
     *
     * @return 是否为 local 环境
     */
    public static boolean isLocalProfile() {
        return isProfileActive("local");
    }

    /**
     * 判断是否为 dev 环境。
     *
     * @return 是否为 dev 环境
     */
    public static boolean isDevProfile() {
        return isProfileActive("dev");
    }

    /**
     * 判断是否为 test 环境。
     *
     * @return 是否为 test 环境
     */
    public static boolean isTestProfile() {
        return isProfileActive("test");
    }

    /**
     * 判断是否为 sit 环境。
     *
     * @return 是否为 sit 环境
     */
    public static boolean isSitProfile() {
        return isProfileActive("sit");
    }

    /**
     * 判断是否为 uat 环境。
     *
     * @return 是否为 uat 环境
     */
    public static boolean isUatProfile() {
        return isProfileActive("uat");
    }

    /**
     * 判断是否为 prod 环境。
     *
     * @return 是否为 prod 环境
     */
    public static boolean isProdProfile() {
        return isProfileActive("prod") || isProfileActive("production");
    }

    /**
     * 判断是否为非生产环境。
     *
     * @return 是否为非生产环境
     */
    public static boolean isNotProdProfile() {
        return !isProdProfile();
    }

    // ============================== 运行环境汇总 ==============================

    /**
     * 获取系统基础信息 Map。
     *
     * @return 系统基础信息 Map
     */
    public static Map<String, Object> getSystemInfoMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("osName", getOsName());
        result.put("osVersion", getOsVersion());
        result.put("osArch", getOsArch());
        result.put("javaVersion", getJavaVersion());
        result.put("javaMajorVersion", getJavaMajorVersion());
        result.put("javaHome", getJavaHome());
        result.put("javaVendor", getJavaVendor());
        result.put("jvmName", getJvmName());
        result.put("jvmVersion", getJvmVersion());
        result.put("jvmVendor", getJvmVendor());
        result.put("userName", getUserName());
        result.put("userHome", getUserHome());
        result.put("userDir", getUserDir());
        result.put("javaTempDir", getJavaTempDir());
        result.put("zoneId", getSystemZoneId());
        result.put("defaultCharset", getDefaultCharsetName());
        result.put("availableProcessors", getAvailableProcessors());
        result.put("pid", getPid());
        result.put("activeProfile", getActiveProfile());
        result.put("hostName", getLocalHostName());
        result.put("hostAddress", getLocalHostAddress());
        return result;
    }

    /**
     * 获取运行时信息 Map。
     *
     * @return 运行时信息 Map
     */
    public static Map<String, Object> getRuntimeInfoMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pid", getPid());
        result.put("processUser", getProcessUser());
        result.put("processCommand", getProcessCommand());
        result.put("processCommandLine", getProcessCommandLine());
        result.put("jvmRuntimeName", getJvmRuntimeName());
        result.put("jvmStartTime", getJvmStartTime());
        result.put("jvmUptime", getJvmUptime());
        result.put("jvmInputArguments", getJvmInputArguments());
        result.put("memory", getMemoryInfoMap());
        return result;
    }

    /**
     * 获取环境信息 Map。
     *
     * @return 环境信息 Map
     */
    public static Map<String, Object> getEnvironmentInfoMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("system", getSystemInfoMap());
        result.put("runtime", getRuntimeInfoMap());
        result.put("memory", getMemoryInfoMap());
        result.put("localIpList", getLocalIpList());
        result.put("activeProfiles", getActiveProfiles());
        return result;
    }

    /**
     * 获取环境信息 JSON 字符串。
     *
     * @return 环境信息 JSON 字符串
     */
    public static String getEnvironmentInfoJson() {
        return toJsonStr(getEnvironmentInfoMap());
    }

    /**
     * 获取格式化后的环境信息 JSON 字符串。
     *
     * @return 格式化后的环境信息 JSON 字符串
     */
    public static String getEnvironmentInfoPrettyJson() {
        return toJsonPrettyStr(getEnvironmentInfoMap());
    }

    // ============================== 系统环境内部辅助方法 ==============================

    /**
     * 基于基础路径拼接路径。
     *
     * @param basePath 基础路径
     * @param paths    路径片段
     * @return 拼接后的路径
     */
    private static String joinBasePath(String basePath, String... paths) {
        if (StrUtil.isBlank(basePath)) {
            return EMPTY;
        }
        if (paths == null || paths.length == 0) {
            return basePath;
        }

        String[] allPaths = new String[paths.length + 1];
        allPaths[0] = basePath;
        System.arraycopy(paths, 0, allPaths, 1, paths.length);
        return pathJoin(allPaths);
    }
}