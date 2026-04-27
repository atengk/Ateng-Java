package local.ateng.java.customutils.utils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 字符串工具类
 * 提供常用字符串处理方法
 *
 * @author Ateng
 * @since 2025-07-26
 */
public final class StringUtil {

    /**
     * 默认分隔符，逗号
     */
    public static final String DEFAULT_DELIMITER = ",";
    private static final String EMPTY = "";
    private static final String NULL_TEXT = "null";
    private static final char PLACEHOLDER_START = '{';
    private static final char PLACEHOLDER_END = '}';
    private static final char ESCAPE_CHAR = '\\';

    /**
     * 禁止实例化工具类
     */
    private StringUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 判断字符串是否为 null 或空串（""）
     *
     * @param str 输入字符串
     * @return 为 null 或空串返回 true，否则返回 false
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 判断字符串是否不为 null 且不为空串
     *
     * @param str 输入字符串
     * @return 非 null 且不为空串返回 true，否则返回 false
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 判断字符串是否为 null、空串或全是空白字符
     *
     * @param str 输入字符串
     * @return 为 null、空串或仅包含空白字符返回 true
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 判断字符串是否不为 null、空串或仅包含空白字符
     *
     * @param str 输入字符串
     * @return 非空白字符串返回 true
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * 去除字符串左右两端的空白字符，如果为 null 返回 null
     *
     * @param str 输入字符串
     * @return 去除空白后的字符串，若输入为 null 返回 null
     */
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    /**
     * 去除字符串两侧指定的字符串（子串）。
     *
     * <p>与 {@link String#trim()} 不同，该方法可去除任意指定的子串。
     * 当 {@code str} 为 null 或空字符串时，将默认去除两端的空格。</p>
     *
     * <p>示例：
     * <pre>
     * trim("##hello##", "##") → "hello"
     * trim("--abc--", "--")   → "abc"
     * trim("  xyz  ", null)   → "xyz"
     * trim("", "##")          → ""
     * </pre>
     *
     * @param str 原始字符串，可为 null
     * @param trimStr 要去除的子串；如果为 null 或空，则默认去除空格
     * @return 去除后的字符串；若输入为 null 则返回 null
     */
    public static String trim(String str, String trimStr) {
        if (str == null) {
            return null;
        }

        if (str.isEmpty()) {
            // 空字符串时直接去除空格
            return str.trim();
        }

        // 如果 trimStr 为空，则默认去除空格
        if (trimStr == null || trimStr.isEmpty()) {
            return str.trim();
        }

        String result = str;
        while (result.startsWith(trimStr)) {
            result = result.substring(trimStr.length());
        }
        while (result.endsWith(trimStr)) {
            result = result.substring(0, result.length() - trimStr.length());
        }
        // 额外再去除空格，保证干净
        return result.trim();
    }

    /**
     * 判断字符串是否为纯数字（可用于 ID、手机号等）
     *
     * @param str 输入字符串
     * @return 是数字返回 true，否则返回 false
     */
    public static boolean isNumeric(String str) {
        if (isBlank(str)) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否为有效的 UUID（标准 36 位格式）
     *
     * @param str 输入字符串
     * @return 是 UUID 返回 true，否则 false
     */
    public static boolean isUUID(String str) {
        if (isBlank(str)) {
            return false;
        }
        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 用指定字符将原字符串左侧填充到指定长度
     *
     * @param str     原始字符串
     * @param length  目标长度
     * @param padChar 填充字符
     * @return 填充后的字符串
     */
    public static String leftPad(String str, int length, char padChar) {
        if (str == null) {
            return null;
        }
        if (str.length() >= length) {
            return str;
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = str.length(); i < length; i++) {
            sb.append(padChar);
        }
        sb.append(str);
        return sb.toString();
    }

    /**
     * 用指定字符将原字符串右侧填充到指定长度
     *
     * @param str     原始字符串
     * @param length  目标长度
     * @param padChar 填充字符
     * @return 填充后的字符串
     */
    public static String rightPad(String str, int length, char padChar) {
        if (str == null) {
            return null;
        }
        if (str.length() >= length) {
            return str;
        }
        StringBuilder sb = new StringBuilder(length);
        sb.append(str);
        for (int i = str.length(); i < length; i++) {
            sb.append(padChar);
        }
        return sb.toString();
    }

    /**
     * 安全地比较两个字符串（允许 null）
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @return 相同返回 true，否则 false
     */
    public static boolean equals(String str1, String str2) {
        return Objects.equals(str1, str2);
    }

    /**
     * 忽略大小写比较两个字符串（允许 null）
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @return 相同返回 true，否则 false
     */
    public static boolean equalsIgnoreCase(String str1, String str2) {
        return str1 == null ? str2 == null : str1.equalsIgnoreCase(str2);
    }

    /**
     * 截取字符串（安全处理下标）
     *
     * @param str   原始字符串
     * @param start 起始索引（包含）
     * @param end   结束索引（不包含）
     * @return 截取后的字符串，若原始字符串为 null 返回 null
     */
    public static String substring(String str, int start, int end) {
        if (str == null) {
            return null;
        }
        int length = str.length();
        if (start < 0) {
            start = 0;
        }
        if (end > length) {
            end = length;
        }
        if (start > end) {
            return "";
        }
        return str.substring(start, end);
    }

    /**
     * 将字符串按分隔符分割成数组（空字符串或 null 返回空数组）
     *
     * @param str       原始字符串
     * @param delimiter 分隔符
     * @return 分割后的字符串数组
     */
    public static String[] split(String str, String delimiter) {
        return split(str, delimiter, true);
    }

    /**
     * 将字符串按分隔符分割成数组
     * <p>支持自动去掉首尾空格，并可选择是否忽略空元素</p>
     *
     * @param str         原始字符串
     * @param delimiter   分隔符
     * @param ignoreEmpty 是否忽略空元素
     * @return 分割后的数组，空字符串或 null 返回空数组
     */
    public static String[] split(String str, String delimiter, boolean ignoreEmpty) {
        if (str == null || str.trim().isEmpty() || delimiter == null) {
            return new String[0];
        }
        String[] parts = str.split(Pattern.quote(delimiter));
        if (!ignoreEmpty) {
            return parts;
        }
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    /**
     * 将字符串按分隔符拆分为列表
     *
     * @param str       原始字符串
     * @param delimiter 分隔符（如 ","）
     * @return 拆分后的 List，空字符串返回空列表
     */
    public static List<String> splitToList(String str, String delimiter) {
        // 复用核心方法：默认忽略空元素与空白片段
        return splitToList(str, delimiter, true);
    }

    /**
     * 将字符串按分隔符拆分为列表
     *
     * @param str 原始字符串
     * @return 拆分后的 List，空字符串返回空列表
     */
    public static List<String> splitToList(String str) {
        // 复用核心方法：默认忽略空元素与空白片段
        return splitToList(str, DEFAULT_DELIMITER, true);
    }

    /**
     * 将字符串按分隔符拆分为列表
     * <p>
     * 支持选择是否忽略空元素；当 {@code ignoreEmpty} 为 {@code true} 时，会对分段进行 {@code trim()}，
     * 并过滤掉空字符串（包括仅包含空白字符的片段）。当为 {@code false} 时，将保留经 {@code trim()} 后的结果，
     * 即可能包含空字符串。
     * </p>
     *
     * @param str         原始字符串
     * @param delimiter   分隔符（如 ","）
     * @param ignoreEmpty 是否忽略空元素（true=忽略空与空白片段，false=保留）
     * @return 拆分后的 List，空字符串或 null 返回空列表
     */
    public static List<String> splitToList(String str, String delimiter, boolean ignoreEmpty) {
        // 空安全：原始字符串或分隔符为空时，返回空列表
        if (str == null || str.isEmpty() || delimiter == null) {
            return Collections.emptyList();
        }

        // 使用 -1 的 limit 以保留尾部空段（当 ignoreEmpty=false 时有意义）
        // 同时用 Pattern.quote 保证分隔符按字面量处理，避免正则元字符干扰
        String[] parts = str.split(Pattern.quote(delimiter), -1);

        // 预估容量：最多等于 parts.length
        List<String> result = new ArrayList<>(parts.length);

        // 遍历分段：统一 trim；根据 ignoreEmpty 控制是否过滤空白
        for (String part : parts) {
            String val = part == null ? "" : part.trim();
            if (ignoreEmpty) {
                // 忽略空与仅空白的片段
                if (val.isEmpty()) {
                    continue;
                }
                result.add(val);
            } else {
                // 保留（可能为空字符串）——此时仍为 trim 后的值，避免意外空白
                result.add(val);
            }
        }
        return result;
    }

    /**
     * 将字符串按分隔符拆分为 Set，自动去重
     *
     * @param str       原始字符串
     * @param delimiter 分隔符
     * @return 拆分后的 Set
     */
    public static Set<String> splitToSet(String str, String delimiter) {
        if (str == null || str.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(splitToList(str, delimiter));
    }

    /**
     * 按指定分隔符分割字符串，并转换为指定类型的列表
     * <p>
     * 支持选择是否忽略空元素；当 {@code ignoreEmpty} 为 {@code true} 时，会对分段进行 {@code trim()}，
     * 并过滤掉空字符串（包括仅包含空白字符的片段）。当为 {@code false} 时，将保留经 {@code trim()} 后的结果，
     * 即可能包含空字符串。转换过程中若发生异常或转换结果为 {@code null}，该元素将被跳过。
     * </p>
     *
     * @param <T>         目标类型
     * @param str         原始字符串
     * @param delimiter   分隔符
     * @param converter   转换函数，将字符串转为 T 类型
     * @param ignoreEmpty 是否忽略空元素（true=忽略空与空白片段，false=保留）
     * @param ignoreError 是否忽略转换异常（true=忽略并跳过错误数据，false=抛出异常）
     * @return 转换成功的 T 类型列表，字符串为空或无有效元素时返回空列表
     */
    public static <T> List<T> splitToList(String str,
                                          String delimiter,
                                          Function<String, T> converter,
                                          boolean ignoreEmpty,
                                          boolean ignoreError) {
        if (str == null || str.isEmpty() || delimiter == null || converter == null) {
            return Collections.emptyList();
        }

        // 使用 -1 保留尾部空段（在 ignoreEmpty=false 时有意义）
        String[] parts = str.split(Pattern.quote(delimiter), -1);
        List<T> result = new ArrayList<>(parts.length);

        for (String part : parts) {
            String trimmed = part == null ? "" : part.trim();

            if (ignoreEmpty && trimmed.isEmpty()) {
                // 忽略空与仅空白片段
                continue;
            }

            try {
                T value = converter.apply(trimmed);
                if (value != null) {
                    result.add(value);
                }
            } catch (Exception e) {
                if (!ignoreError) {
                    throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
                }
                // ignoreError = true → 跳过错误数据
            }
        }
        return result;
    }

    /**
     * 按指定分隔符分割字符串，并转换为指定类型的列表
     *
     * @param <T>       目标类型
     * @param str       原始字符串
     * @param delimiter 分隔符
     * @param converter 转换函数，将字符串转为 T 类型
     * @return 转换成功的 T 类型列表，字符串为空或无有效元素时返回空列表
     */
    public static <T> List<T> splitToList(String str, String delimiter, Function<String, T> converter) {
        return splitToList(str, delimiter, converter, true, false);
    }

    /**
     * 按默认分隔符分割字符串，并转换为指定类型的列表
     *
     * @param <T>       目标类型
     * @param str       原始字符串
     * @param converter 转换函数，将字符串转为 T 类型
     * @return 转换成功的 T 类型列表，字符串为空或无有效元素时返回空列表
     */
    public static <T> List<T> splitToList(String str, Function<String, T> converter) {
        return splitToList(str, DEFAULT_DELIMITER, converter);
    }

    /**
     * 按指定分隔符拆分字符串并转换为 Integer 类型列表
     *
     * @param str       需要拆分的字符串
     * @param delimiter 分隔符字符串
     * @return Integer 类型列表，转换失败的元素会被跳过
     */
    public static List<Integer> splitToIntegerList(String str, String delimiter) {
        return splitToList(str, delimiter, Integer::parseInt);
    }

    /**
     * 按指定分隔符拆分字符串并转换为 Long 类型列表
     *
     * @param str       需要拆分的字符串
     * @param delimiter 分隔符字符串
     * @return Long 类型列表，转换失败的元素会被跳过
     */
    public static List<Long> splitToLongList(String str, String delimiter) {
        return splitToList(str, delimiter, Long::parseLong);
    }

    /**
     * 按指定分隔符拆分字符串并转换为 Double 类型列表
     *
     * @param str       需要拆分的字符串
     * @param delimiter 分隔符字符串
     * @return Double 类型列表，转换失败的元素会被跳过
     */
    public static List<Double> splitToDoubleList(String str, String delimiter) {
        return splitToList(str, delimiter, Double::parseDouble);
    }

    /**
     * 按指定分隔符拆分字符串并转换为 Boolean 类型列表
     *
     * <p>转换规则为忽略大小写的 true/false，非 true 字符串均为 false。</p>
     *
     * @param str       需要拆分的字符串
     * @param delimiter 分隔符字符串
     * @return Boolean 类型列表，转换失败的元素会被跳过
     */
    public static List<Boolean> splitToBooleanList(String str, String delimiter) {
        return splitToList(str, delimiter, s -> Boolean.parseBoolean(s.toLowerCase()));
    }

    /**
     * 按指定分隔符拆分字符串并转换为去除空白字符串的列表
     *
     * @param str       需要拆分的字符串
     * @param delimiter 分隔符字符串
     * @return 非空字符串列表
     */
    public static List<String> splitToStringList(String str, String delimiter) {
        return splitToList(str, delimiter, s -> s);
    }

    // ======================== 基础替换（Regex / Simple） ========================

    /**
     * 替换第一个匹配的子串（正则语义，等价于 {@link String#replaceFirst(String, String)}）。
     * <p><b>注意：</b>本方法把 {@code target} 当作正则表达式解析；{@code replacement}
     * 中的 {@code $}、{@code \} 等也遵循正则替换规则（如需安全替换请用 {@link #replaceSafeFirst(String, String, String)}）。</p>
     *
     * @param str         原始字符串（null 将直接返回 null）
     * @param target      要替换的<strong>正则</strong>模式
     * @param replacement 替换内容（按正则替换语义解释）
     * @return 替换后的字符串；若任一参数为 null，则返回原始 {@code str}
     * @since 2025-08-15
     */
    public static String replaceFirst(String str, String target, String replacement) {
        if (str == null || target == null || replacement == null) {
            return str;
        }
        return str.replaceFirst(target, replacement);
    }

    /**
     * 替换所有匹配的子串（正则语义，等价于 {@link String#replaceAll(String, String)}）。
     * <p><b>注意：</b>本方法把 {@code target} 当作正则表达式解析；{@code replacement}
     * 中的 {@code $}、{@code \} 等也遵循正则替换规则（如需安全替换请用 {@link #replaceSafeAll(String, String, String)}）。</p>
     *
     * @param str         原始字符串（null 将直接返回 null）
     * @param target      要替换的<strong>正则</strong>模式
     * @param replacement 替换内容（按正则替换语义解释）
     * @return 替换后的字符串；若任一参数为 null，则返回原始 {@code str}
     * @since 2025-08-15
     */
    public static String replaceAllRegex(String str, String target, String replacement) {
        if (str == null || target == null || replacement == null) {
            return str;
        }
        return str.replaceAll(target, replacement);
    }

    /**
     * 替换所有匹配的子串（<b>简单模式</b>，不使用正则；等价于 {@link String#replace(CharSequence, CharSequence)}）。
     * <p>当你确认 {@code target} 只是普通文本（非正则）时，推荐使用该方法，效率更直接且无正则歧义。</p>
     *
     * @param str         原始字符串（null 将直接返回 null）
     * @param target      要替换的普通文本（非正则）
     * @param replacement 替换内容（普通文本，不解析 $、\）
     * @return 替换后的字符串；若任一参数为 null，则返回原始 {@code str}
     * @since 2025-08-15
     */
    public static String replaceAllSimple(String str, String target, String replacement) {
        if (str == null || target == null || replacement == null) {
            return str;
        }
        return str.replace(target, replacement);
    }

    /**
     * 批量替换字符串中的内容。
     * <p>根据传入的 Map，将字符串中出现的 key 替换为对应的 value。</p>
     *
     * <pre>
     * 示例：
     * String text = "Hello ${name}, welcome to ${place}!";
     * Map<String, Object> map = new HashMap<>();
     * map.put("${name}", "Tony");
     * map.put("${place}", "Beijing");
     *
     * String result = replaceByMap(text, map);
     * // result = "Hello Tony, welcome to Beijing!"
     * </pre>
     *
     * @param text   原始字符串
     * @param values 替换规则，key 为要替换的内容，value 为替换结果
     * @return 替换后的字符串；如果 text 或 values 为空则返回原字符串
     */
    public static String replaceByMap(String text, Map<String, Object> values) {
        if (text == null || values == null || values.isEmpty()) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key == null) {
                continue;
            }
            // 确保特殊字符不会被当成正则处理
            String regex = Pattern.quote(key);
            result = result.replaceAll(regex, value == null ? "" : value.toString());
        }
        return result;
    }

    // ======================== 安全替换（自动转义 target & replacement） ========================

    /**
     * 安全地替换第一个匹配：对 {@code target} 做 {@link Pattern#quote(String)}，对 {@code replacement}
     * 做 {@link Matcher#quoteReplacement(String)}，避免正则与替换串的特殊含义（如 {@code . * $ \}）。
     *
     * @param str         原始字符串（null 将直接返回 null）
     * @param target      要替换的文本（将被安全转义为字面量）
     * @param replacement 替换内容（将被安全转义为字面量）
     * @return 替换后的字符串；若任一参数为 null，则返回原始 {@code str}
     * @since 2025-08-15
     */
    public static String replaceSafeFirst(String str, String target, String replacement) {
        return replaceSafeInternal(str, target, replacement, false);
    }

    /**
     * 安全地替换所有匹配：对 {@code target} 做 {@link Pattern#quote(String)}，对 {@code replacement}
     * 做 {@link Matcher#quoteReplacement(String)}，避免正则与替换串的特殊含义（如 {@code . * $ \}）。
     *
     * @param str         原始字符串（null 将直接返回 null）
     * @param target      要替换的文本（将被安全转义为字面量）
     * @param replacement 替换内容（将被安全转义为字面量）
     * @return 替换后的字符串；若任一参数为 null，则返回原始 {@code str}
     * @since 2025-08-15
     */
    public static String replaceSafeAll(String str, String target, String replacement) {
        return replaceSafeInternal(str, target, replacement, true);
    }

    /**
     * 安全替换（内部复用）：封装了正则与替换串的双重转义。
     *
     * @param str         原始字符串
     * @param target      目标文本（按字面量处理）
     * @param replacement 替换文本（按字面量处理）
     * @param replaceAll  {@code true} 替换全部；{@code false} 仅替换第一个
     * @return 替换结果或原样返回
     */
    private static String replaceSafeInternal(String str, String target, String replacement, boolean replaceAll) {
        if (str == null || target == null || replacement == null) {
            return str;
        }
        String quotedTarget = Pattern.quote(target);
        String quotedReplacement = Matcher.quoteReplacement(replacement);
        return replaceAll ? str.replaceAll(quotedTarget, quotedReplacement)
                : str.replaceFirst(quotedTarget, quotedReplacement);
    }

    // ======================== 安全替换（忽略大小写） ========================

    /**
     * 安全地替换第一个匹配（忽略大小写）：目标按字面量匹配（安全转义），不区分大小写。
     *
     * @param str         原始字符串（null 将直接返回 null）
     * @param target      要替换的文本（按字面量处理，不区分大小写）
     * @param replacement 替换内容（按字面量处理，自动转义 $、\）
     * @return 替换后的字符串；若任一参数为 null，则返回原始 {@code str}
     * @since 2025-08-15
     */
    public static String replaceSafeIgnoreCaseFirst(String str, String target, String replacement) {
        return replaceSafeIgnoreCaseInternal(str, target, replacement, false);
    }

    /**
     * 安全地替换所有匹配（忽略大小写）：目标按字面量匹配（安全转义），不区分大小写。
     *
     * @param str         原始字符串（null 将直接返回 null）
     * @param target      要替换的文本（按字面量处理，不区分大小写）
     * @param replacement 替换内容（按字面量处理，自动转义 $、\）
     * @return 替换后的字符串；若任一参数为 null，则返回原始 {@code str}
     * @since 2025-08-15
     */
    public static String replaceSafeIgnoreCaseAll(String str, String target, String replacement) {
        return replaceSafeIgnoreCaseInternal(str, target, replacement, true);
    }

    /**
     * 安全忽略大小写替换（内部复用）。
     *
     * @param str         原始字符串
     * @param target      目标文本（字面量匹配）
     * @param replacement 替换文本（字面量替换）
     * @param replaceAll  {@code true} 替换全部；{@code false} 仅替换第一个
     * @return 替换结果或原样返回
     */
    private static String replaceSafeIgnoreCaseInternal(String str, String target, String replacement, boolean replaceAll) {
        if (str == null || target == null || replacement == null) {
            return str;
        }
        // (?i) 忽略大小写
        String regex = "(?i)" + Pattern.quote(target);
        String quotedReplacement = Matcher.quoteReplacement(replacement);
        return replaceAll ? str.replaceAll(regex, quotedReplacement)
                : str.replaceFirst(regex, quotedReplacement);
    }

    // ======================== 安全替换（限制次数） ========================

    /**
     * 安全地按次数限制进行替换：目标与替换文本均按字面量处理，内部使用正则匹配并逐次替换。
     * <p>
     * 用于“只替换前 N 次”的场景。若 {@code limit} ≤ 0，原样返回 {@code str}。
     * </p>
     *
     * @param str         原始字符串（null 将直接返回 null）
     * @param target      要替换的文本（按字面量处理）
     * @param replacement 替换内容（按字面量处理，自动转义 $、\）
     * @param limit       替换次数上限（≤ 0 表示不替换；&gt; 匹配数时等价于替换全部）
     * @return 替换后的字符串；若任一参数为 null 或 {@code limit} ≤ 0，则返回原始 {@code str}
     * @since 2025-08-15
     */
    public static String replaceSafeLimit(String str, String target, String replacement, int limit) {
        if (str == null || target == null || replacement == null || limit <= 0) {
            return str;
        }
        String quotedTarget = Pattern.quote(target);
        String quotedReplacement = Matcher.quoteReplacement(replacement);

        Matcher matcher = Pattern.compile(quotedTarget).matcher(str);
        StringBuffer sb = new StringBuffer();
        int count = 0;
        while (matcher.find()) {
            if (++count > limit) {
                break; // 超出限制，停止追加替换，后续保持原样
            }
            matcher.appendReplacement(sb, quotedReplacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 移除字符串中所有空格（含空格、制表符、换行等）
     *
     * @param str 原始字符串
     * @return 移除空白字符后的字符串
     */
    public static String remove(String str) {
        if (isBlank(str)) {
            return "";
        }
        return str.replaceAll("\\s+", "");
    }

    /**
     * 移除字符串中所有指定字符
     *
     * @param str        原始字符串
     * @param charsToDel 要删除的字符（如",."）
     * @return 删除后的字符串
     */
    public static String remove(String str, String charsToDel) {
        if (str == null || charsToDel == null) {
            return str;
        }
        String regex = "[" + Pattern.quote(charsToDel) + "]";
        return str.replaceAll(regex, "");
    }

    /**
     * 判断字符串是否以指定前缀开头（忽略 null）
     *
     * @param str    原始字符串
     * @param prefix 前缀
     * @return 是前缀返回 true
     */
    public static boolean startsWith(String str, String prefix) {
        return str != null && prefix != null && str.startsWith(prefix);
    }

    /**
     * 判断字符串是否以指定后缀结尾（忽略 null）
     *
     * @param str    原始字符串
     * @param suffix 后缀
     * @return 是后缀返回 true
     */
    public static boolean endsWith(String str, String suffix) {
        return str != null && suffix != null && str.endsWith(suffix);
    }

    /**
     * 使用指定分隔符拼接集合中的字符串元素
     *
     * @param delimiter 分隔符
     * @param elements  字符串集合
     * @return 拼接结果，集合为空返回空字符串
     */
    public static String join(String delimiter, java.util.Collection<String> elements) {
        if (elements == null || elements.isEmpty()) {
            return "";
        }
        return String.join(delimiter, elements);
    }

    /**
     * 使用指定分隔符拼接对象数组，每个对象调用 toString() 方法
     *
     * @param delimiter 分隔符
     * @param elements  对象数组
     * @return 拼接结果，对象为 null 会被转换为 "null"
     */
    public static String joinObjects(String delimiter, Object... elements) {
        if (elements == null || elements.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            sb.append(elements[i] == null ? "null" : elements[i].toString());
        }
        return sb.toString();
    }

    /**
     * 使用指定分隔符拼接集合中的对象元素，每个对象调用 toString() 方法
     *
     * @param delimiter 分隔符
     * @param elements  对象集合
     * @return 拼接结果，集合为空返回空字符串
     */
    public static String joinObjects(String delimiter, java.util.Collection<?> elements) {
        if (elements == null || elements.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object obj : elements) {
            if (!first) {
                sb.append(delimiter);
            } else {
                first = false;
            }
            sb.append(obj == null ? "null" : obj.toString());
        }
        return sb.toString();
    }

    /**
     * 使用默认分隔符拼接字符串数组
     *
     * @param elements 字符串数组
     * @return 拼接结果，数组为空返回空字符串
     */
    public static String join(String... elements) {
        if (elements == null || elements.length == 0) {
            return "";
        }
        return String.join(DEFAULT_DELIMITER, elements);
    }

    /**
     * 使用默认分隔符拼接字符串集合
     *
     * @param elements 字符串集合
     * @return 拼接结果，集合为空返回空字符串
     */
    public static String join(java.util.Collection<String> elements) {
        return join(DEFAULT_DELIMITER, elements);
    }

    /**
     * 使用默认分隔符拼接对象数组
     *
     * @param elements 对象数组
     * @return 拼接结果，数组为空返回空字符串
     */
    public static String joinObjects(Object... elements) {
        return joinObjects(DEFAULT_DELIMITER, elements);
    }

    /**
     * 使用默认分隔符拼接对象集合
     *
     * @param elements 对象集合
     * @return 拼接结果，集合为空返回空字符串
     */
    public static String joinObjects(java.util.Collection<?> elements) {
        return joinObjects(DEFAULT_DELIMITER, elements);
    }

    /**
     * 将指定字符串重复多次
     *
     * @param str   字符串
     * @param count 重复次数
     * @return 重复后的字符串
     */
    public static String repeat(String str, int count) {
        if (str == null || count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 将首字母转换为大写（英文）
     *
     * @param str 原始字符串
     * @return 首字母大写的字符串
     */
    public static String capitalizeFirst(String str) {
        if (isBlank(str)) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 将首字母转换为小写（英文）
     *
     * @param str 原始字符串
     * @return 首字母小写的字符串
     */
    public static String uncapitalizeFirst(String str) {
        if (isBlank(str)) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 将字符串全部转换为大写
     *
     * @param str 原始字符串
     * @return 全部大写字符串，null 返回 null
     */
    public static String toUpperCase(String str) {
        return str == null ? null : str.toUpperCase();
    }

    /**
     * 将字符串全部转换为小写
     *
     * @param str 原始字符串
     * @return 全部小写字符串，null 返回 null
     */
    public static String toLowerCase(String str) {
        return str == null ? null : str.toLowerCase();
    }

    /**
     * 将下划线命名转为驼峰命名（如 user_name -> userName）
     *
     * @param str 下划线字符串
     * @return 驼峰命名字符串
     */
    public static String toCamelCase(String str) {
        if (isBlank(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        boolean upperNext = false;
        for (char c : str.toCharArray()) {
            if (c == '_') {
                upperNext = true;
            } else if (upperNext) {
                sb.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 将驼峰命名转为下划线命名（如 userName -> user_name）
     *
     * @param str 驼峰命名字符串
     * @return 下划线命名字符串
     */
    public static String toSnakeCase(String str) {
        if (isBlank(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 将下划线命名转为大驼峰命名（PascalCase，如 user_name -> UserName）
     *
     * @param str 下划线字符串
     * @return 大驼峰命名字符串
     */
    public static String toPascalCase(String str) {
        if (isBlank(str)) {
            return str;
        }
        String camel = toCamelCase(str);
        // 首字母大写
        return capitalizeFirst(camel);
    }

    /**
     * Base64 编码
     *
     * @param input 输入字符串
     * @return 编码后的 Base64 字符串
     */
    public static String base64Encode(String input) {
        if (input == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64 解码
     *
     * @param base64 Base64 字符串
     * @return 解码后的原始字符串
     */
    public static String base64Decode(String base64) {
        if (base64 == null) {
            return null;
        }
        byte[] decoded = Base64.getDecoder().decode(base64);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    /**
     * HTML 字符转义（如 < 转为 &lt;）
     *
     * @param str 原始字符串
     * @return 转义后的字符串，当传入为空时返回原值
     */
    public static String escapeHtml(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length());
        for (char c : str.toCharArray()) {
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#x27;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * HTML 字符还原（如 &lt; 还原为 <）
     *
     * @param str 转义后的字符串
     * @return 还原后的字符串，当传入为空时返回原值
     */
    public static String unescapeHtml(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#x27;", "'")
                .replace("&amp;", "&");
    }

    /**
     * 手机号码脱敏，保留前三位和后四位，中间用四个星号替代
     *
     * @param phone 手机号字符串
     * @return 脱敏后的手机号，长度不为11时返回原始字符串
     */
    public static String maskPhone(String phone) {
        // 手机号固定长度
        final int phoneLength = 11;
        // 前面可见位数
        final int prefixVisible = 3;
        // 后面可见位数
        final int suffixVisible = 4;
        // 中间脱敏符号
        final String mask = "****";

        if (phone == null || phone.length() != phoneLength) {
            return phone;
        }

        return phone.substring(0, prefixVisible) + mask + phone.substring(phoneLength - suffixVisible);
    }

    /**
     * 身份证脱敏处理，保留前3位和后4位，中间用 * 号替代
     *
     * @param id 身份证号
     * @return 脱敏后的身份证号，长度不足最小限制则返回原始字符串
     */
    public static String maskIdCard(String id) {
        // 脱敏最小长度限制
        final int minLength = 8;
        // 前面可见长度
        final int prefixVisible = 3;
        // 后面可见长度
        final int suffixVisible = 4;

        if (id == null || id.length() < minLength) {
            return id;
        }
        int length = id.length();
        int maskLength = length - prefixVisible - suffixVisible;
        StringBuilder maskBuilder = new StringBuilder();
        for (int i = 0; i < maskLength; i++) {
            maskBuilder.append('*');
        }
        return id.substring(0, prefixVisible) + maskBuilder.toString() + id.substring(length - suffixVisible);
    }

    /**
     * 判断字符串中是否包含中文字符
     *
     * @param str 输入字符串
     * @return 包含中文字符返回 true
     */
    public static boolean containsChinese(String str) {
        if (isBlank(str)) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将全角字符转换为半角字符
     *
     * @param str 原始字符串
     * @return 半角字符串
     */
    public static String toHalfWidth(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            // 全角空格
            if (c == 12288) {
                sb.append(' ');
            } else if (c >= 65281 && c <= 65374) {
                sb.append((char) (c - 65248));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 将半角字符转换为全角字符
     *
     * @param str 原始字符串
     * @return 全角字符串
     */
    public static String toFullWidth(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (c == ' ') {
                sb.append((char) 12288);
            } else if (c >= 33 && c <= 126) {
                sb.append((char) (c + 65248));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * URL 编码（UTF-8）
     *
     * @param str 原始字符串
     * @return 编码后的字符串
     */
    public static String urlEncode(String str) {
        if (str == null) {
            return null;
        }
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return str;
        }
    }

    /**
     * URL 解码（UTF-8）
     *
     * @param str 编码字符串
     * @return 解码后的字符串
     */
    public static String urlDecode(String str) {
        if (str == null) {
            return null;
        }
        try {
            return URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return str;
        }
    }

    /**
     * 将字符串编码为 Unicode 编码格式（如：\\u4e2d\\u6587）
     *
     * @param str 原始字符串
     * @return Unicode 编码后的字符串
     */
    public static String toUnicode(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            sb.append(String.format("\\u%04x", (int) c));
        }
        return sb.toString();
    }

    /**
     * 将 Unicode 字符串解码为普通字符串（如：\\u4e2d\\u6587 -> 中文）
     *
     * @param unicodeStr Unicode 字符串
     * @return 解码后的字符串
     */
    public static String fromUnicode(String unicodeStr) {
        if (unicodeStr == null) {
            return null;
        }
        String regex = "\\\\u([0-9a-fA-F]{4})";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(unicodeStr);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String code = matcher.group(1);
            char ch = (char) Integer.parseInt(code, 16);
            matcher.appendReplacement(sb, Character.toString(ch));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 反转字符串（如 abc -> cba）
     *
     * @param str 原始字符串
     * @return 反转后的字符串
     */
    public static String reverse(String str) {
        if (str == null) {
            return null;
        }
        return new StringBuilder(str).reverse().toString();
    }

    /**
     * 去除指定前缀（忽略大小写）
     *
     * @param str    原始字符串
     * @param prefix 要移除的前缀
     * @return 结果字符串
     */
    public static String removePrefixIgnoreCase(String str, String prefix) {
        if (isBlank(str) || isBlank(prefix)) {
            return str;
        }
        if (str.toLowerCase().startsWith(prefix.toLowerCase())) {
            return str.substring(prefix.length());
        }
        return str;
    }

    /**
     * 去除指定后缀（忽略大小写）
     *
     * @param str    原始字符串
     * @param suffix 要移除的后缀
     * @return 结果字符串
     */
    public static String removeSuffixIgnoreCase(String str, String suffix) {
        if (isBlank(str) || isBlank(suffix)) {
            return str;
        }
        if (str.toLowerCase().endsWith(suffix.toLowerCase())) {
            return str.substring(0, str.length() - suffix.length());
        }
        return str;
    }

    /**
     * 对多行字符串进行缩进（每行前添加指定空格数量）
     *
     * @param str    原始多行字符串
     * @param spaces 空格数量
     * @return 缩进后的字符串
     */
    public static String indent(String str, int spaces) {
        if (str == null || spaces <= 0) {
            return str;
        }
        String indent = repeat(" ", spaces);
        return Arrays.stream(str.split("\r?\n"))
                .map(line -> indent + line)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 计算字符串长度（英文=1，中文=2）
     *
     * @param str 字符串
     * @return 估算的长度
     */
    public static int lengthConsideringChinese(String str) {
        if (str == null) {
            return 0;
        }
        int len = 0;
        for (char c : str.toCharArray()) {
            // 中文范围：\u4E00-\u9FFF，其他字符按1算
            len += (c >= '\u4E00' && c <= '\u9FFF') ? 2 : 1;
        }
        return len;
    }

    /**
     * 只保留字符串中的中文、英文、数字
     *
     * @param str 原始字符串
     * @return 过滤后的字符串
     */
    public static String retainChineseAlphabetNumber(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("[^a-zA-Z0-9\\u4E00-\\u9FFF]", "");
    }

    /**
     * 从驼峰命名字符串中提取首字母缩写（如 UserAccountName -> UAN）
     *
     * @param str 驼峰字符串
     * @return 首字母缩写（大写）
     */
    public static String acronymFromCamel(String str) {
        if (isBlank(str)) {
            return "";
        }
        StringBuilder acronym = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (i == 0 || Character.isUpperCase(c)) {
                acronym.append(Character.toUpperCase(c));
            }
        }
        return acronym.toString();
    }

    /**
     * 获取字符串的 MD5 值（32 位小写）
     *
     * @param str 原始字符串
     * @return MD5 哈希字符串
     */
    public static String md5(String str) {
        return hash(str, "MD5");
    }

    /**
     * 获取字符串的 SHA-1 值
     *
     * @param str 原始字符串
     * @return SHA-1 哈希字符串
     */
    public static String sha1(String str) {
        return hash(str, "SHA-1");
    }

    /**
     * 获取字符串的 SHA-256 值
     *
     * @param str 原始字符串
     * @return SHA-256 哈希字符串
     */
    public static String sha256(String str) {
        return hash(str, "SHA-256");
    }

    /**
     * 内部通用哈希实现（使用标准 JDK MessageDigest）
     *
     * @param str       输入字符串
     * @param algorithm 哈希算法（如 MD5、SHA-1、SHA-256）
     * @return 哈希值（小写十六进制）
     */
    private static String hash(String str, String algorithm) {
        if (str == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] bytes = md.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * 判断字符串是否完全匹配指定正则表达式
     *
     * @param str   原始字符串
     * @param regex 正则表达式
     * @return 匹配返回 true，否则 false
     */
    public static boolean matches(String str, String regex) {
        if (str == null || regex == null) {
            return false;
        }
        return Pattern.matches(regex, str);
    }

    /**
     * 验证邮箱格式（简单版）
     *
     * @param email 邮箱字符串
     * @return 格式正确返回 true
     */
    public static boolean isEmail(String email) {
        if (email == null) {
            return false;
        }
        return email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$");
    }

    /**
     * 验证中国大陆手机号格式（11位，以13-19开头）
     *
     * @param phone 手机号码
     * @return 格式正确返回 true
     */
    public static boolean isChinaMobilePhone(String phone) {
        if (phone == null) {
            return false;
        }
        return phone.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 验证身份证号码（15位或18位，最后一位可为X）
     *
     * @param idCard 身份证号
     * @return 格式正确返回 true
     */
    public static boolean isIdCard(String idCard) {
        if (idCard == null) {
            return false;
        }
        return idCard.matches("^(\\d{15}|\\d{17}[\\dXx])$");
    }

    /**
     * 验证IPv4地址格式
     *
     * @param ip IPv4 地址字符串
     * @return 格式正确返回 true
     */
    public static boolean isIPv4(String ip) {
        if (ip == null) {
            return false;
        }
        return ip.matches("^(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3}$");
    }

    /**
     * 验证 URL 格式（http/https，支持域名/IP、端口、路径、查询参数、锚点）
     *
     * @param url URL 字符串
     * @return 格式正确返回 true，否则返回 false
     */
    public static boolean isUrl(String url) {
        if (isEmpty(url)) {
            return false;
        }
        String regex =
                // 协议
                "^(https?://)"
                        + "(([\\w-]+\\.)+[\\w-]{2,}|"
                        // 域名或IP
                        + "((25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(?!$)|$)){4})"
                        // 端口
                        + "(:\\d{1,5})?"
                        // 路径、查询、锚点
                        + "(/[^\\s]*)?$";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url.trim());
        return matcher.matches();
    }

    /**
     * 判断 URL 是否安全
     * 1. 不能以危险协议（javascript:, data:, vbscript:）开头
     * 2. 不能包含 HTML/JS 注入字符
     * 3. 不能包含非 ASCII 可打印字符
     *
     * @param url URL 字符串
     * @return 安全返回 true，不安全返回 false
     */
    public static boolean isSafeUrl(String url) {
        if (isBlank(url)) {
            return false;
        }
        String cleaned = url.trim().toLowerCase();

        // 危险协议列表
        String[] dangerousProtocols = {"javascript:", "data:", "vbscript:"};
        // 注入字符正则
        String injectionPattern = ".*[<>\"'`()].*";
        // 非 ASCII 可打印字符正则
        String nonAsciiPrintablePattern = ".*[^\\x20-\\x7E].*";

        // 危险协议检测
        for (String protocol : dangerousProtocols) {
            if (cleaned.startsWith(protocol)) {
                return false;
            }
        }

        // 注入字符检测
        if (url.matches(injectionPattern)) {
            return false;
        }

        // 非 ASCII 可打印字符检测
        if (url.matches(nonAsciiPrintablePattern)) {
            return false;
        }

        return true;
    }


    /**
     * 修复并安全过滤 URL
     * 1. 去除首尾空格、换行、控制字符
     * 2. 阻止危险协议（javascript:, data:, vbscript:）
     * 3. 移除 HTML/JS 注入字符
     * 4. 如果缺少协议，补全为 http://
     * 5. 统一协议和域名部分为小写
     * 6. 清理路径中多余的连续斜杠
     *
     * @param url 原始 URL
     * @return 修复并安全过滤后的 URL，当传入为空或不安全时返回 null
     */
    public static String fixUrl(String url) {
        if (isBlank(url)) {
            return null;
        }

        // 正则表达式变量
        final String controlWhitespacePattern = "[\\p{Cntrl}\\s]+";
        final String injectionCharsPattern = "[<>\"'`()]";
        final String nonAsciiPrintablePattern = "[^\\x20-\\x7E]";
        final String httpUrlPattern = "^(?i)https?://.*";

        // 去掉控制字符和空白符，替换为单个空格并去首尾空格
        String fixed = url.replaceAll(controlWhitespacePattern, " ").trim();

        // 不安全 URL 直接拒绝
        if (!isSafeUrl(fixed)) {
            return null;
        }

        // 移除 HTML/JS 注入字符
        fixed = fixed.replaceAll(injectionCharsPattern, "");

        // 移除非 ASCII 可打印字符
        fixed = fixed.replaceAll(nonAsciiPrintablePattern, "");

        // 如果没有协议，补上 http://
        final String httpPrefix = "http://";
        if (!fixed.matches(httpUrlPattern)) {
            fixed = httpPrefix + fixed;
        }

        try {
            java.net.URL parsedUrl = new java.net.URL(fixed);
            String protocol = parsedUrl.getProtocol().toLowerCase();
            String host = parsedUrl.getHost().toLowerCase();
            int port = parsedUrl.getPort();
            String path = parsedUrl.getPath().replaceAll("/{2,}", "/");
            String query = parsedUrl.getQuery() != null ? "?" + parsedUrl.getQuery() : "";
            String ref = parsedUrl.getRef() != null ? "#" + parsedUrl.getRef() : "";

            // 组装安全规范化 URL
            StringBuilder sb = new StringBuilder();
            sb.append(protocol).append("://").append(host);
            if (port != -1 && port != parsedUrl.getDefaultPort()) {
                sb.append(":").append(port);
            }
            sb.append(path).append(query).append(ref);
            return sb.toString();
        } catch (Exception e) {
            // 解析失败，认为不安全
            return null;
        }
    }

    /**
     * 修正和规范路径字符串
     * 1. 去除前后空格
     * 2. 统一路径分隔符为 '/'
     * 3. 去除多余的连续 '/'
     * 4. 规范处理 '.' 和 '..'
     *
     * @param path 输入的路径字符串，可能是文件路径或URL路径
     * @return 规范后的干净路径字符串
     */
    public static String fixPath(String path) {
        // 魔法值常量
        final char backslashChar = '\\';
        final char slashChar = '/';
        final String multipleSlashPattern = "/+";
        final String currentDir = ".";
        final String parentDir = "..";
        final String emptyString = "";

        if (path == null || path.trim().isEmpty()) {
            return emptyString;
        }
        // 去除前后空格
        path = path.trim();
        // 统一反斜杠为正斜杠
        path = path.replace(backslashChar, slashChar);
        // 去除连续多余的斜杠，比如 /////
        path = path.replaceAll(multipleSlashPattern, String.valueOf(slashChar));

        // 处理相对路径 . 和 ..
        String[] parts = path.split(String.valueOf(slashChar));
        Deque<String> stack = new LinkedList<>();

        for (String part : parts) {
            if (part.equals(emptyString) || part.equals(currentDir)) {
                // 空或者当前目录，跳过
                continue;
            }
            if (part.equals(parentDir)) {
                // 上一级目录，弹出栈顶（如果存在）
                if (!stack.isEmpty()) {
                    stack.pollLast();
                }
            } else {
                // 正常目录，压入栈
                stack.offerLast(part);
            }
        }

        // 重新拼接路径
        StringBuilder cleanPath = new StringBuilder();
        for (String dir : stack) {
            cleanPath.append(slashChar).append(dir);
        }

        // 如果输入是绝对路径（以 '/' 开头），保留开头的 '/'
        // 否则去掉开头的 '/'，返回相对路径
        boolean isAbsolute = path.startsWith(String.valueOf(slashChar));
        if (cleanPath.length() == 0) {
            return isAbsolute ? String.valueOf(slashChar) : emptyString;
        }
        return isAbsolute ? cleanPath.toString() : cleanPath.substring(1);
    }

    /**
     * 判断字符串是否为合法的日期格式（yyyy-MM-dd）
     *
     * @param str 字符串
     * @return 是日期格式返回 true
     */
    public static boolean isDate(String str) {
        if (str == null) {
            return false;
        }
        return str.matches("^\\d{4}-\\d{2}-\\d{2}$");
    }

    /**
     * 判断字符串是否为合法时间格式（HH:mm:ss）
     *
     * @param str 字符串
     * @return 是时间格式返回 true
     */
    public static boolean isTime(String str) {
        if (str == null) {
            return false;
        }
        return str.matches("^([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$");
    }

    /**
     * 判断字符串是否为合法的邮政编码（中国6位数字）
     *
     * @param str 字符串
     * @return 是邮政编码返回 true
     */
    public static boolean isPostalCode(String str) {
        if (str == null) {
            return false;
        }
        return str.matches("^\\d{6}$");
    }

    /**
     * 判断字符串是否只包含字母（大小写均可）
     *
     * @param str 字符串
     * @return 只包含字母返回 true
     */
    public static boolean isAlpha(String str) {
        if (str == null) {
            return false;
        }
        return str.matches("^[a-zA-Z]+$");
    }

    /**
     * 判断字符串是否只包含字母和数字
     *
     * @param str 字符串
     * @return 只包含字母和数字返回 true
     */
    public static boolean isAlphanumeric(String str) {
        if (str == null) {
            return false;
        }
        return str.matches("^[a-zA-Z0-9]+$");
    }

    /**
     * 将字节数组转换为十六进制字符串（大写，空格分隔，每字节两位）
     *
     * @param bytes 字节数组
     * @return 十六进制字符串，格式如： "AB CD 12"，为空返回空字符串
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        // 去除最后一个空格
        return sb.toString().trim();
    }

    /**
     * 使用 SLF4J 风格的 {} 占位符格式化字符串。
     * 规则：
     * 1. {} 按顺序替换参数
     * 2. 参数不足时，剩余 {} 保持不变
     * 3. 参数多余时，多余参数自动忽略
     * 4. \{} 会被视为普通文本 {}
     *
     * 示例：
     * StringUtil.format("参数1={}, 参数2={}, 参数3={}", "A", "B", "C");
     * 结果：参数1=A, 参数2=B, 参数3=C
     *
     * @param template 含 {} 占位符的模板字符串
     * @param args     参数列表
     * @return 格式化后的字符串
     */
    public static String format(String template, Object... args) {
        if (template == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder(template.length() + (args == null ? 0 : args.length * 16));
        int argIndex = 0;
        int length = template.length();

        for (int i = 0; i < length; i++) {
            char current = template.charAt(i);

            if (current == ESCAPE_CHAR) {
                if (i + 1 < length) {
                    char next = template.charAt(i + 1);
                    if (next == PLACEHOLDER_START && i + 2 < length && template.charAt(i + 2) == PLACEHOLDER_END) {
                        builder.append(PLACEHOLDER_START).append(PLACEHOLDER_END);
                        i += 2;
                        continue;
                    }
                    builder.append(next);
                    i++;
                    continue;
                }
                builder.append(current);
                continue;
            }

            if (current == PLACEHOLDER_START
                    && i + 1 < length
                    && template.charAt(i + 1) == PLACEHOLDER_END) {
                if (args != null && argIndex < args.length) {
                    builder.append(toString(args[argIndex++]));
                } else {
                    builder.append(PLACEHOLDER_START).append(PLACEHOLDER_END);
                }
                i++;
                continue;
            }

            builder.append(current);
        }

        return builder.toString();
    }

    /**
     * 使用索引占位符格式化字符串。
     * 规则：
     * 1. {0}、{1}、{2} 按索引替换
     * 2. 索引不存在时保持原样
     * 3. 支持 \{0} 形式的转义
     *
     * 示例：
     * StringUtil.formatIndex("姓名={0}，年龄={1}", "张三", 18);
     * 结果：姓名=张三，年龄=18
     *
     * @param template 模板字符串
     * @param args     参数列表
     * @return 格式化后的字符串
     */
    public static String formatIndex(String template, Object... args) {
        if (template == null) {
            return null;
        }
        if (isEmpty(template)) {
            return EMPTY;
        }

        StringBuilder builder = new StringBuilder(template.length() + (args == null ? 0 : args.length * 16));
        int length = template.length();

        for (int i = 0; i < length; i++) {
            char current = template.charAt(i);

            if (current == ESCAPE_CHAR) {
                if (i + 1 < length) {
                    builder.append(template.charAt(i + 1));
                    i++;
                    continue;
                }
                builder.append(current);
                continue;
            }

            if (current == PLACEHOLDER_START) {
                int endIndex = template.indexOf(PLACEHOLDER_END, i + 1);
                if (endIndex > i + 1) {
                    String token = template.substring(i + 1, endIndex);
                    if (isInteger(token)) {
                        int index = Integer.parseInt(token);
                        if (args != null && index >= 0 && index < args.length) {
                            builder.append(toString(args[index]));
                        } else {
                            builder.append(template, i, endIndex + 1);
                        }
                        i = endIndex;
                        continue;
                    }
                }
            }

            builder.append(current);
        }

        return builder.toString();
    }

    /**
     * 使用命名占位符格式化字符串。
     * 规则：
     * 1. {name} 会从 Map 中读取对应值
     * 2. 未命中的占位符保持原样
     * 3. 支持 \{name} 形式的转义
     *
     * 示例：
     * StringUtil.formatNamed("用户={name}，状态={status}", map);
     *
     * @param template 模板字符串
     * @param values    命名参数
     * @return 格式化后的字符串
     */
    public static String formatNamed(String template, Map<String, ?> values) {
        if (template == null) {
            return null;
        }
        if (isEmpty(template)) {
            return EMPTY;
        }
        if (values == null || values.isEmpty()) {
            return template;
        }

        StringBuilder builder = new StringBuilder(template.length() + values.size() * 16);
        int length = template.length();

        for (int i = 0; i < length; i++) {
            char current = template.charAt(i);

            if (current == ESCAPE_CHAR) {
                if (i + 1 < length) {
                    char next = template.charAt(i + 1);
                    if (next == PLACEHOLDER_START) {
                        builder.append(PLACEHOLDER_START);
                        i++;
                        continue;
                    }
                    builder.append(next);
                    i++;
                    continue;
                }
                builder.append(current);
                continue;
            }

            if (current == PLACEHOLDER_START) {
                int endIndex = template.indexOf(PLACEHOLDER_END, i + 1);
                if (endIndex > i + 1) {
                    String key = template.substring(i + 1, endIndex);
                    if (values.containsKey(key)) {
                        builder.append(toString(values.get(key)));
                    } else {
                        builder.append(template, i, endIndex + 1);
                    }
                    i = endIndex;
                    continue;
                }
            }

            builder.append(current);
        }

        return builder.toString();
    }


    /**
     * 判断字符串是否为整数。
     *
     * @param str 字符串
     * @return true：是整数
     */
    public static boolean isInteger(String str) {
        if (isBlank(str)) {
            return false;
        }
        int start = 0;
        if (str.charAt(0) == '-' || str.charAt(0) == '+') {
            if (str.length() == 1) {
                return false;
            }
            start = 1;
        }
        for (int i = start; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 将对象安全转换为字符串，null 返回 "null"。
     *
     * @param value 对象
     * @return 字符串
     */
    private static String toString(Object value) {
        return value == null ? NULL_TEXT : String.valueOf(value);
    }

    /**
     * 构建 URL 字符串：先拼接 query（保留占位），再用 uriVariables 替换占位符。
     *
     * @param baseUrl      基础 URL（例如 "https://api.example.com/user/{id}/detail"）
     * @param queryParams  查询参数（支持 Collection / 数组 / 单值），若 value 为形如 "{name}" 的占位符则保留原样
     * @param uriVariables 模板变量映射（用于替换 {id}, {name} 等）
     * @param encode       是否对参数值与替换值进行 URL 编码（UTF-8）
     * @return 最终构建的 URL 字符串
     * @throws IllegalArgumentException 当 baseUrl 空或 encode==true 且最终 URL 非法时抛出
     */
    public static String buildUrl(String baseUrl,
                                  Map<String, ?> queryParams,
                                  Map<String, ?> uriVariables,
                                  boolean encode) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }

        StringBuilder sb = new StringBuilder(baseUrl);

        // ========== 1) 拼接 query 参数（保留形如 "{name}" 的占位符） ==========
        if (queryParams != null && !queryParams.isEmpty()) {
            boolean first = !baseUrl.contains("?");
            for (Map.Entry<String, ?> e : queryParams.entrySet()) {
                String key = e.getKey();
                Object val = e.getValue();
                if (key == null || val == null) {
                    continue;
                }

                // 把 value 统一展开为 List<String>
                List<String> values = new ArrayList<>();
                if (val instanceof Collection) {
                    for (Object o : (Collection<?>) val) {
                        if (o != null) {
                            values.add(String.valueOf(o));
                        }
                    }
                } else if (val.getClass().isArray()) {
                    int len = Array.getLength(val);
                    for (int i = 0; i < len; i++) {
                        Object o = Array.get(val, i);
                        if (o != null) {
                            values.add(String.valueOf(o));
                        }
                    }
                } else {
                    values.add(String.valueOf(val));
                }

                for (String v : values) {
                    if (v == null) {
                        continue;
                    }
                    sb.append(first ? '?' : '&');
                    first = false;

                    String k = encode ? safeEncode(key) : key;

                    // 如果 value 完全是占位符（全字符串为 {name}），则保留原样，后续统一替换
                    if (isWholePlaceholder(v)) {
                        sb.append(k).append("=").append(v);
                    } else {
                        String vv = encode ? safeEncode(v) : v;
                        sb.append(k).append("=").append(vv);
                    }
                }
            }
        }

        String urlWithPlaceholders = sb.toString();

        // ========== 2) 替换占位符 ==========
        if (uriVariables != null && !uriVariables.isEmpty()) {
            // 为避免替换顺序影响（例如 {a} 和 {ab}），按键长度降序替换更稳妥
            List<String> keys = new ArrayList<>();
            for (String k : uriVariables.keySet()) {
                if (k != null) {
                    keys.add(k);
                }
            }
            keys.sort((a, b) -> Integer.compare(b.length(), a.length()));

            String result = urlWithPlaceholders;
            for (String key : keys) {
                Object rawVal = uriVariables.get(key);
                if (rawVal == null) {
                    continue;
                }
                String replacement = String.valueOf(rawVal);
                replacement = encode ? safeEncode(replacement) : replacement;
                // 使用 String.replace 替换所有出现的 {key}
                result = result.replace("{" + key + "}", replacement);
            }
            urlWithPlaceholders = result;
        }

        // ========== 3) （可选）校验最终 URL ==========
        if (encode) {
            try {
                new URI(urlWithPlaceholders);
            } catch (URISyntaxException ex) {
                throw new IllegalArgumentException("Constructed URL is invalid: " + urlWithPlaceholders, ex);
            }
        } // 如果 encode == false，我们不强制校验（避免中文等未编码造成异常）

        return urlWithPlaceholders;
    }

    /**
     * 判断字符串是否是完整占位符（形如 "{xxx}"）。
     *
     * @param s 待判断的字符串
     * @return true 表示是占位符，false 表示不是
     */
    private static boolean isWholePlaceholder(String s) {
        if (s == null) {
            return false;
        }
        s = s.trim();
        return s.length() >= 3 && s.charAt(0) == '{' && s.charAt(s.length() - 1) == '}';
    }

    /**
     * 使用 UTF-8 对字符串进行 URL 编码。
     * <p>如果输入为 null，则返回 null。</p>
     *
     * @param input 待编码的字符串
     * @return 编码后的字符串
     */
    private static String safeEncode(String input) {
        if (input == null) {
            return null;
        }
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // 理论上不会发生
            return input;
        }
    }


    /**
     * 获取两个分隔符之间的内容（只返回第一个匹配）
     *
     * @param text            原始文本
     * @param startDelimiter  起始分隔符
     * @param endDelimiter    结束分隔符
     * @param includeDelimiter 是否输出包含分隔符本身
     * @return 截取的内容，如果不存在则返回 null
     */
    public static String substringBetween(String text,
                                          String startDelimiter,
                                          String endDelimiter,
                                          boolean includeDelimiter) {
        if (text == null || startDelimiter == null || endDelimiter == null) {
            return null;
        }

        int start = text.indexOf(startDelimiter);
        if (start < 0) {
            return null;
        }

        int end = text.indexOf(endDelimiter, start + startDelimiter.length());
        if (end < 0) {
            return null;
        }

        if (includeDelimiter) {
            return text.substring(start, end + endDelimiter.length());
        }

        return text.substring(start + startDelimiter.length(), end);
    }

    /**
     * 获取所有被分隔符包裹的内容（多个匹配）
     *
     * @param text             原始文本
     * @param startDelimiter   起始分隔符
     * @param endDelimiter     结束分隔符
     * @param includeDelimiter 是否输出包含分隔符本身
     * @return 所有匹配内容的列表，如果无匹配返回空集合
     */
    public static List<String> substringsBetween(String text,
                                                 String startDelimiter,
                                                 String endDelimiter,
                                                 boolean includeDelimiter) {
        if (text == null || startDelimiter == null || endDelimiter == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        int pos = 0;
        int start;
        int end;

        while (true) {
            start = text.indexOf(startDelimiter, pos);
            if (start < 0) {
                break;
            }

            end = text.indexOf(endDelimiter, start + startDelimiter.length());
            if (end < 0) {
                break;
            }

            String segment;
            if (includeDelimiter) {
                segment = text.substring(start, end + endDelimiter.length());
            } else {
                segment = text.substring(start + startDelimiter.length(), end);
            }

            result.add(segment);

            pos = end + endDelimiter.length();
        }

        return result;
    }

    /**
     * null 转为空字符串。
     *
     * @param str 原始字符串
     * @return 非 null 字符串
     */
    public static String nullToEmpty(String str) {
        return str == null ? EMPTY : str;
    }

    /**
     * 空字符串转为 null。
     *
     * @param str 原始字符串
     * @return null 或原字符串
     */
    public static String emptyToNull(String str) {
        return isEmpty(str) ? null : str;
    }

    /**
     * 空白字符串转为 null。
     *
     * @param str 原始字符串
     * @return null 或原字符串
     */
    public static String blankToNull(String str) {
        return isBlank(str) ? null : str;
    }

    /**
     * 当字符串为 null 时返回默认值。
     *
     * @param str          原始字符串
     * @param defaultValue 默认值
     * @return 原字符串或默认值
     */
    public static String defaultIfNull(String str, String defaultValue) {
        return str == null ? defaultValue : str;
    }

    /**
     * 当字符串为空时返回默认值。
     *
     * @param str          原始字符串
     * @param defaultValue 默认值
     * @return 原字符串或默认值
     */
    public static String defaultIfEmpty(String str, String defaultValue) {
        return isEmpty(str) ? defaultValue : str;
    }

    /**
     * 当字符串为空白时返回默认值。
     *
     * @param str          原始字符串
     * @param defaultValue 默认值
     * @return 原字符串或默认值
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }

    /**
     * 返回第一个非空白字符串。
     *
     * @param values 字符串数组
     * @return 第一个非空白字符串，不存在时返回空字符串
     */
    public static String firstNotBlank(String... values) {
        if (values == null || values.length == 0) {
            return EMPTY;
        }
        for (String value : values) {
            if (isNotBlank(value)) {
                return value;
            }
        }
        return EMPTY;
    }

    /**
     * 判断字符串是否包含指定内容。
     *
     * @param str    原始字符串
     * @param search 查找内容
     * @return 包含返回 true，否则返回 false
     */
    public static boolean contains(String str, String search) {
        return str != null && search != null && str.contains(search);
    }

    /**
     * 判断字符串是否包含指定内容，忽略大小写。
     *
     * @param str    原始字符串
     * @param search 查找内容
     * @return 包含返回 true，否则返回 false
     */
    public static boolean containsIgnoreCase(String str, String search) {
        return indexOfIgnoreCase(str, search) >= 0;
    }

    /**
     * 判断字符串是否以指定前缀开头，忽略大小写。
     *
     * @param str    原始字符串
     * @param prefix 前缀
     * @return 是指定前缀返回 true，否则返回 false
     */
    public static boolean startsWithIgnoreCase(String str, String prefix) {
        if (str == null || prefix == null || prefix.length() > str.length()) {
            return false;
        }
        return str.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * 判断字符串是否以指定后缀结尾，忽略大小写。
     *
     * @param str    原始字符串
     * @param suffix 后缀
     * @return 是指定后缀返回 true，否则返回 false
     */
    public static boolean endsWithIgnoreCase(String str, String suffix) {
        if (str == null || suffix == null || suffix.length() > str.length()) {
            return false;
        }
        int start = str.length() - suffix.length();
        return str.regionMatches(true, start, suffix, 0, suffix.length());
    }

    /**
     * 查找子串位置，忽略大小写。
     *
     * @param str    原始字符串
     * @param search 查找内容
     * @return 首次出现的位置，不存在返回 -1
     */
    public static int indexOfIgnoreCase(String str, String search) {
        return indexOfIgnoreCase(str, search, 0);
    }

    /**
     * 从指定位置查找子串位置，忽略大小写。
     *
     * @param str       原始字符串
     * @param search    查找内容
     * @param fromIndex 起始位置
     * @return 首次出现的位置，不存在返回 -1
     */
    public static int indexOfIgnoreCase(String str, String search, int fromIndex) {
        if (str == null || search == null) {
            return -1;
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (search.isEmpty()) {
            return Math.min(fromIndex, str.length());
        }

        int max = str.length() - search.length();
        if (fromIndex > max) {
            return -1;
        }

        for (int i = fromIndex; i <= max; i++) {
            if (str.regionMatches(true, i, search, 0, search.length())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 统计子串出现次数。
     *
     * @param str    原始字符串
     * @param search 查找内容
     * @return 出现次数
     */
    public static int countMatches(String str, String search) {
        if (isEmpty(str) || isEmpty(search)) {
            return 0;
        }

        int count = 0;
        int index = 0;
        while ((index = str.indexOf(search, index)) >= 0) {
            count++;
            index += search.length();
        }
        return count;
    }

    /**
     * 判断字符串是否等于任意候选值。
     *
     * @param str        原始字符串
     * @param candidates 候选值
     * @return 匹配任意一个返回 true
     */
    public static boolean equalsAny(String str, String... candidates) {
        if (candidates == null || candidates.length == 0) {
            return false;
        }
        for (String candidate : candidates) {
            if (Objects.equals(str, candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断字符串是否忽略大小写等于任意候选值。
     *
     * @param str        原始字符串
     * @param candidates 候选值
     * @return 匹配任意一个返回 true
     */
    public static boolean equalsAnyIgnoreCase(String str, String... candidates) {
        if (candidates == null || candidates.length == 0) {
            return false;
        }
        for (String candidate : candidates) {
            if (equalsIgnoreCase(str, candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 截取指定分隔符之前的内容。
     *
     * @param str       原始字符串
     * @param delimiter 分隔符
     * @return 分隔符之前的内容
     */
    public static String substringBefore(String str, String delimiter) {
        if (str == null || delimiter == null) {
            return str;
        }
        if (delimiter.isEmpty()) {
            return EMPTY;
        }
        int index = str.indexOf(delimiter);
        return index < 0 ? str : str.substring(0, index);
    }

    /**
     * 截取指定分隔符之后的内容。
     *
     * @param str       原始字符串
     * @param delimiter 分隔符
     * @return 分隔符之后的内容
     */
    public static String substringAfter(String str, String delimiter) {
        if (str == null) {
            return null;
        }
        if (delimiter == null) {
            return EMPTY;
        }
        if (delimiter.isEmpty()) {
            return str;
        }
        int index = str.indexOf(delimiter);
        return index < 0 ? EMPTY : str.substring(index + delimiter.length());
    }

    /**
     * 截取最后一个指定分隔符之前的内容。
     *
     * @param str       原始字符串
     * @param delimiter 分隔符
     * @return 最后一个分隔符之前的内容
     */
    public static String substringBeforeLast(String str, String delimiter) {
        if (str == null || delimiter == null) {
            return str;
        }
        if (delimiter.isEmpty()) {
            return str;
        }
        int index = str.lastIndexOf(delimiter);
        return index < 0 ? str : str.substring(0, index);
    }

    /**
     * 截取最后一个指定分隔符之后的内容。
     *
     * @param str       原始字符串
     * @param delimiter 分隔符
     * @return 最后一个分隔符之后的内容
     */
    public static String substringAfterLast(String str, String delimiter) {
        if (str == null) {
            return null;
        }
        if (delimiter == null || delimiter.isEmpty()) {
            return EMPTY;
        }
        int index = str.lastIndexOf(delimiter);
        return index < 0 || index == str.length() - delimiter.length()
                ? EMPTY
                : str.substring(index + delimiter.length());
    }

    /**
     * 从左侧截取指定长度。
     *
     * @param str    原始字符串
     * @param length 截取长度
     * @return 截取结果
     */
    public static String left(String str, int length) {
        if (str == null) {
            return null;
        }
        if (length <= 0) {
            return EMPTY;
        }
        return str.length() <= length ? str : str.substring(0, length);
    }

    /**
     * 从右侧截取指定长度。
     *
     * @param str    原始字符串
     * @param length 截取长度
     * @return 截取结果
     */
    public static String right(String str, int length) {
        if (str == null) {
            return null;
        }
        if (length <= 0) {
            return EMPTY;
        }
        return str.length() <= length ? str : str.substring(str.length() - length);
    }

    /**
     * 去除指定前缀，区分大小写。
     *
     * @param str    原始字符串
     * @param prefix 前缀
     * @return 去除后的字符串
     */
    public static String removePrefix(String str, String prefix) {
        if (str == null || prefix == null || prefix.isEmpty()) {
            return str;
        }
        return str.startsWith(prefix) ? str.substring(prefix.length()) : str;
    }

    /**
     * 去除指定后缀，区分大小写。
     *
     * @param str    原始字符串
     * @param suffix 后缀
     * @return 去除后的字符串
     */
    public static String removeSuffix(String str, String suffix) {
        if (str == null || suffix == null || suffix.isEmpty()) {
            return str;
        }
        return str.endsWith(suffix) ? str.substring(0, str.length() - suffix.length()) : str;
    }

    /**
     * 前缀不存在时自动追加前缀。
     *
     * @param str    原始字符串
     * @param prefix 前缀
     * @return 处理后的字符串
     */
    public static String prependIfMissing(String str, String prefix) {
        if (str == null || isEmpty(prefix)) {
            return str;
        }
        return str.startsWith(prefix) ? str : prefix + str;
    }

    /**
     * 后缀不存在时自动追加后缀。
     *
     * @param str    原始字符串
     * @param suffix 后缀
     * @return 处理后的字符串
     */
    public static String appendIfMissing(String str, String suffix) {
        if (str == null || isEmpty(suffix)) {
            return str;
        }
        return str.endsWith(suffix) ? str : str + suffix;
    }

    /**
     * 使用指定字符串包裹原字符串。
     *
     * @param str     原始字符串
     * @param wrapper 包裹字符串
     * @return 包裹后的字符串
     */
    public static String wrap(String str, String wrapper) {
        if (str == null || wrapper == null) {
            return str;
        }
        return wrapper + str + wrapper;
    }

    /**
     * 去除两侧相同的包裹字符串。
     *
     * @param str     原始字符串
     * @param wrapper 包裹字符串
     * @return 去除后的字符串
     */
    public static String unwrap(String str, String wrapper) {
        if (str == null || isEmpty(wrapper)) {
            return str;
        }
        if (str.startsWith(wrapper) && str.endsWith(wrapper) && str.length() >= wrapper.length() * 2) {
            return str.substring(wrapper.length(), str.length() - wrapper.length());
        }
        return str;
    }

    /**
     * 规范化空白字符，多个空白合并为一个半角空格。
     *
     * @param str 原始字符串
     * @return 规范化后的字符串
     */
    public static String normalizeWhitespace(String str) {
        if (str == null) {
            return null;
        }
        return str.trim().replaceAll("\\s+", " ");
    }

    /**
     * 移除换行符，保留其他内容。
     *
     * @param str 原始字符串
     * @return 移除换行后的字符串
     */
    public static String removeLineBreaks(String str) {
        if (str == null) {
            return null;
        }
        return str.replace("\r", "").replace("\n", "");
    }

    /**
     * 按任意换行符拆分为行列表。
     *
     * @param str         原始字符串
     * @param ignoreEmpty 是否忽略空行
     * @return 行列表
     */
    public static List<String> splitLines(String str, boolean ignoreEmpty) {
        if (str == null) {
            return Collections.emptyList();
        }

        String[] lines = str.split("\\R", -1);
        List<String> result = new ArrayList<>(lines.length);
        for (String line : lines) {
            if (ignoreEmpty && line.isEmpty()) {
                continue;
            }
            result.add(line);
        }
        return result;
    }

    /**
     * 缩略字符串，默认使用 ... 作为省略标记。
     *
     * @param str       原始字符串
     * @param maxLength 最大长度
     * @return 缩略后的字符串
     */
    public static String abbreviate(String str, int maxLength) {
        return abbreviate(str, maxLength, "...");
    }

    /**
     * 缩略字符串。
     *
     * @param str       原始字符串
     * @param maxLength 最大长度
     * @param marker    省略标记
     * @return 缩略后的字符串
     */
    public static String abbreviate(String str, int maxLength, String marker) {
        if (str == null) {
            return null;
        }
        if (maxLength <= 0) {
            return EMPTY;
        }
        if (str.length() <= maxLength) {
            return str;
        }

        marker = marker == null ? EMPTY : marker;
        if (marker.length() >= maxLength) {
            return str.substring(0, maxLength);
        }
        return str.substring(0, maxLength - marker.length()) + marker;
    }

    /**
     * 通用脱敏处理，默认使用 * 作为脱敏字符。
     *
     * @param str           原始字符串
     * @param prefixVisible 前面可见长度
     * @param suffixVisible 后面可见长度
     * @return 脱敏后的字符串
     */
    public static String mask(String str, int prefixVisible, int suffixVisible) {
        return mask(str, prefixVisible, suffixVisible, '*');
    }

    /**
     * 通用脱敏处理。
     *
     * @param str           原始字符串
     * @param prefixVisible 前面可见长度
     * @param suffixVisible 后面可见长度
     * @param maskChar      脱敏字符
     * @return 脱敏后的字符串
     */
    public static String mask(String str, int prefixVisible, int suffixVisible, char maskChar) {
        if (str == null) {
            return null;
        }

        int length = str.length();
        prefixVisible = Math.max(prefixVisible, 0);
        suffixVisible = Math.max(suffixVisible, 0);

        if (prefixVisible + suffixVisible >= length) {
            return str;
        }

        int maskLength = length - prefixVisible - suffixVisible;
        return str.substring(0, prefixVisible)
                + repeat(String.valueOf(maskChar), maskLength)
                + str.substring(length - suffixVisible);
    }

    /**
     * 提取字符串中的数字。
     *
     * @param str 原始字符串
     * @return 数字字符串
     */
    public static String extractDigits(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("\\D+", EMPTY);
    }

    /**
     * 提取字符串中的英文字母。
     *
     * @param str 原始字符串
     * @return 英文字母字符串
     */
    public static String extractLetters(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("[^a-zA-Z]+", EMPTY);
    }

    /**
     * 只保留中文字符。
     *
     * @param str 原始字符串
     * @return 中文字符串
     */
    public static String retainChinese(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("[^\\u4E00-\\u9FFF]+", EMPTY);
    }

    /**
     * 移除中文字符。
     *
     * @param str 原始字符串
     * @return 移除中文后的字符串
     */
    public static String removeChinese(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("[\\u4E00-\\u9FFF]+", EMPTY);
    }

    /**
     * 判断字符串是否只包含中文字符。
     *
     * @param str 原始字符串
     * @return 只包含中文返回 true，否则返回 false
     */
    public static boolean isChineseOnly(String str) {
        return isNotBlank(str) && str.matches("^[\\u4E00-\\u9FFF]+$");
    }

    /**
     * 判断字符串是否只包含 ASCII 可打印字符。
     *
     * @param str 原始字符串
     * @return 只包含 ASCII 可打印字符返回 true，否则返回 false
     */
    public static boolean isAsciiPrintable(String str) {
        if (str == null) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c < 32 || c > 126) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否为十进制数字，支持正负号和小数。
     *
     * @param str 原始字符串
     * @return 是十进制数字返回 true，否则返回 false
     */
    public static boolean isDecimal(String str) {
        if (isBlank(str)) {
            return false;
        }
        return str.matches("^[+-]?((\\d+\\.\\d+)|(\\d+)|(\\.\\d+))$");
    }

    /**
     * 判断字符串是否为正整数。
     *
     * @param str 原始字符串
     * @return 是正整数返回 true，否则返回 false
     */
    public static boolean isPositiveInteger(String str) {
        if (isBlank(str)) {
            return false;
        }
        return str.matches("^[1-9]\\d*$");
    }

    /**
     * 判断字符串是否为非负整数。
     *
     * @param str 原始字符串
     * @return 是非负整数返回 true，否则返回 false
     */
    public static boolean isNonNegativeInteger(String str) {
        if (isBlank(str)) {
            return false;
        }
        return str.matches("^(0|[1-9]\\d*)$");
    }

    /**
     * 判断字符串是否为金额格式，最多两位小数。
     *
     * @param str 原始字符串
     * @return 是金额格式返回 true，否则返回 false
     */
    public static boolean isMoney(String str) {
        if (isBlank(str)) {
            return false;
        }
        return str.matches("^[+-]?(0|[1-9]\\d*)(\\.\\d{1,2})?$");
    }

    /**
     * 查找第一个正则匹配内容。
     *
     * @param str   原始字符串
     * @param regex 正则表达式
     * @return 第一个匹配内容，不存在返回 null
     */
    public static String findFirst(String str, String regex) {
        return findFirst(str, regex, 0);
    }

    /**
     * 查找第一个正则匹配分组内容。
     *
     * @param str   原始字符串
     * @param regex 正则表达式
     * @param group 分组下标，0 表示完整匹配
     * @return 第一个匹配分组内容，不存在返回 null
     */
    public static String findFirst(String str, String regex, int group) {
        if (str == null || regex == null || group < 0) {
            return null;
        }

        Matcher matcher = Pattern.compile(regex).matcher(str);
        if (matcher.find() && group <= matcher.groupCount()) {
            return matcher.group(group);
        }
        return null;
    }

    /**
     * 查找所有正则匹配内容。
     *
     * @param str   原始字符串
     * @param regex 正则表达式
     * @return 匹配内容列表
     */
    public static List<String> findAll(String str, String regex) {
        return findAll(str, regex, 0);
    }

    /**
     * 查找所有正则匹配分组内容。
     *
     * @param str   原始字符串
     * @param regex 正则表达式
     * @param group 分组下标，0 表示完整匹配
     * @return 匹配内容列表
     */
    public static List<String> findAll(String str, String regex, int group) {
        if (str == null || regex == null || group < 0) {
            return Collections.emptyList();
        }

        Matcher matcher = Pattern.compile(regex).matcher(str);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            if (group <= matcher.groupCount()) {
                result.add(matcher.group(group));
            }
        }
        return result;
    }

    /**
     * 按中文长度规则从左侧截取，中文按 2 计算，其他字符按 1 计算。
     *
     * @param str       原始字符串
     * @param maxLength 最大长度
     * @return 截取后的字符串
     */
    public static String leftConsideringChineseLength(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (maxLength <= 0) {
            return EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        int length = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int charLength = isChineseChar(c) ? 2 : 1;
            if (length + charLength > maxLength) {
                break;
            }
            sb.append(c);
            length += charLength;
        }
        return sb.toString();
    }

    /**
     * 按中文长度规则缩略字符串，中文按 2 计算，其他字符按 1 计算。
     *
     * @param str       原始字符串
     * @param maxLength 最大长度
     * @return 缩略后的字符串
     */
    public static String abbreviateConsideringChineseLength(String str, int maxLength) {
        return abbreviateConsideringChineseLength(str, maxLength, "...");
    }

    /**
     * 按中文长度规则缩略字符串，中文按 2 计算，其他字符按 1 计算。
     *
     * @param str       原始字符串
     * @param maxLength 最大长度
     * @param marker    省略标记
     * @return 缩略后的字符串
     */
    public static String abbreviateConsideringChineseLength(String str, int maxLength, String marker) {
        if (str == null) {
            return null;
        }
        if (maxLength <= 0) {
            return EMPTY;
        }
        if (lengthConsideringChinese(str) <= maxLength) {
            return str;
        }

        marker = marker == null ? EMPTY : marker;
        int markerLength = lengthConsideringChinese(marker);
        if (markerLength >= maxLength) {
            return leftConsideringChineseLength(marker, maxLength);
        }

        return leftConsideringChineseLength(str, maxLength - markerLength) + marker;
    }

    /**
     * 判断字符是否为中文字符。
     *
     * @param c 字符
     * @return 是中文字符返回 true，否则返回 false
     */
    private static boolean isChineseChar(char c) {
        return Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN;
    }
}
