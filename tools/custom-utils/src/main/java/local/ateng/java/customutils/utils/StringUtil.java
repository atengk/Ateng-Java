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
     * 使用指定分隔符拼接字符串数组
     *
     * @param delimiter 分隔符
     * @param elements  元素数组
     * @return 拼接结果
     */
    public static String join(String delimiter, String... elements) {
        if (elements == null || elements.length == 0) {
            return "";
        }
        return String.join(delimiter, elements);
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
        return join(DEFAULT_DELIMITER, elements);
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
     * 使用 SLF4J 占位符方式格式化字符串
     * <p>
     * 示例：
     * <pre>
     *     String msg = StringUtil.format("参数1={}, 参数2={}, 参数3={}", "A", "B", "C");
     *     输出：参数1=A, 参数2=B, 参数3=C
     * </pre>
     *
     * @param template 含 {} 占位符的字符串模板
     * @param args     替换占位符的参数列表
     * @return 格式化后的字符串
     */
    public static String format(String template, Object... args) {
        if (template == null || args == null || args.length == 0) {
            return template;
        }

        StringBuilder sb = new StringBuilder(template.length() + args.length * 10);
        int templateLength = template.length();
        int argIndex = 0;
        int cursor = 0;

        while (cursor < templateLength) {
            int placeholderIndex = template.indexOf("{}", cursor);
            if (placeholderIndex == -1 || argIndex >= args.length) {
                // 没有更多占位符或参数已用完，追加剩余部分
                sb.append(template.substring(cursor));
                break;
            }

            sb.append(template, cursor, placeholderIndex);
            sb.append(args[argIndex] != null ? args[argIndex].toString() : "null");

            // 跳过"{}"
            cursor = placeholderIndex + 2;
            argIndex++;
        }

        return sb.toString();
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


}
