package local.ateng.java.customutils.utils;

import java.io.UnsupportedEncodingException;
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
        if (isBlank(str) || delimiter == null) {
            return new String[0];
        }
        return str.split(Pattern.quote(delimiter));
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
        List<T> result = new ArrayList<>();
        if (str == null || delimiter == null || converter == null) {
            return result;
        }
        String[] parts = str.split(Pattern.quote(delimiter));
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                T value = converter.apply(trimmed);
                if (value != null) {
                    result.add(value);
                }
            } catch (Exception e) {
                // 转换失败时跳过该元素，防止抛出异常
            }
        }
        return result;
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

    /**
     * 替换字符串中的指定内容（可处理 null）
     *
     * @param str         原始字符串
     * @param target      要替换的子串
     * @param replacement 替换后的内容
     * @return 替换后的结果字符串
     */
    public static String replace(String str, String target, String replacement) {
        if (str == null || target == null || replacement == null) {
            return str;
        }
        return str.replace(target, replacement);
    }

    /**
     * 移除字符串中所有空格（含空格、制表符、换行等）
     *
     * @param str 原始字符串
     * @return 移除空白字符后的字符串
     */
    public static String removeWhitespace(String str) {
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
    public static String removeChars(String str, String charsToDel) {
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
     * @return 转义后的字符串
     */
    public static String escapeHtml(String str) {
        if (str == null) {
            return null;
        }
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * 手机号码脱敏，保留前三位和后四位，中间用四个星号替代
     *
     * @param phone 手机号字符串
     * @return 脱敏后的手机号，长度不为11时返回原始字符串
     */
    public static String maskPhone(String phone) {
        final int PHONE_LENGTH = 11;       // 手机号固定长度
        final int PREFIX_VISIBLE = 3;      // 前面可见位数
        final int SUFFIX_VISIBLE = 4;      // 后面可见位数
        final String MASK = "****";        // 中间脱敏符号

        if (phone == null || phone.length() != PHONE_LENGTH) {
            return phone;
        }

        return phone.substring(0, PREFIX_VISIBLE) + MASK + phone.substring(PHONE_LENGTH - SUFFIX_VISIBLE);
    }

    /**
     * 身份证脱敏处理，保留前3位和后4位，中间用 * 号替代
     *
     * @param id 身份证号
     * @return 脱敏后的身份证号，长度不足最小限制则返回原始字符串
     */
    public static String maskIdCard(String id) {
        final int MIN_LENGTH = 8;  // 脱敏最小长度限制
        final int PREFIX_VISIBLE = 3; // 前面可见长度
        final int SUFFIX_VISIBLE = 4; // 后面可见长度

        if (id == null || id.length() < MIN_LENGTH) {
            return id;
        }
        int length = id.length();
        int maskLength = length - PREFIX_VISIBLE - SUFFIX_VISIBLE;
        StringBuilder maskBuilder = new StringBuilder();
        for (int i = 0; i < maskLength; i++) {
            maskBuilder.append('*');
        }
        return id.substring(0, PREFIX_VISIBLE) + maskBuilder.toString() + id.substring(length - SUFFIX_VISIBLE);
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
        Pattern pattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
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
     * 验证URL格式（http/https，简单校验）
     *
     * @param url URL 字符串
     * @return 格式正确返回 true
     */
    public static boolean isUrl(String url) {
        if (url == null) {
            return false;
        }
        return url.matches("^(https?://)?([\\w.-]+)(:[0-9]+)?(/[\\w./-]*)?$");
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

            cursor = placeholderIndex + 2; // 跳过"{}"
            argIndex++;
        }

        return sb.toString();
    }

}
