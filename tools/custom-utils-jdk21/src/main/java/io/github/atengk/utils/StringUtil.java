package io.github.atengk.utils;

/**
 * 字符串工具类。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class StringUtil {

    private StringUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 判断字符串是否为 null 或空字符串。
     *
     * @param value 字符串
     * @return 是否为空
     */
    public static boolean isEmpty(CharSequence value) {
        return value == null || value.isEmpty();
    }

    /**
     * 判断字符串是否不为 null 且不是空字符串。
     *
     * @param value 字符串
     * @return 是否不为空
     */
    public static boolean isNotEmpty(CharSequence value) {
        return !isEmpty(value);
    }

    /**
     * 判断字符串是否为 null、空字符串或空白字符串。
     *
     * @param value 字符串
     * @return 是否为空白
     */
    public static boolean isBlank(CharSequence value) {
        if (value == null) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否不为 null 且包含非空白字符。
     *
     * @param value 字符串
     * @return 是否非空白
     */
    public static boolean isNotBlank(CharSequence value) {
        return !isBlank(value);
    }

    /**
     * 判断字符串是否包含有效文本。
     *
     * @param value 字符串
     * @return 是否包含有效文本
     */
    public static boolean hasText(CharSequence value) {
        return isNotBlank(value);
    }

    /**
     * 判断所有字符串是否都为空白。
     *
     * @param values 字符串数组
     * @return 是否全部为空白
     */
    public static boolean isAllBlank(CharSequence... values) {
        if (values == null || values.length == 0) {
            return true;
        }
        for (CharSequence value : values) {
            if (isNotBlank(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断任意字符串是否为空白。
     *
     * @param values 字符串数组
     * @return 是否存在空白字符串
     */
    public static boolean isAnyBlank(CharSequence... values) {
        if (values == null || values.length == 0) {
            return false;
        }
        for (CharSequence value : values) {
            if (isBlank(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断所有字符串是否都为空。
     *
     * @param values 字符串数组
     * @return 是否全部为空
     */
    public static boolean isAllEmpty(CharSequence... values) {
        if (values == null || values.length == 0) {
            return true;
        }
        for (CharSequence value : values) {
            if (isNotEmpty(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断任意字符串是否为空。
     *
     * @param values 字符串数组
     * @return 是否存在空字符串
     */
    public static boolean isAnyEmpty(CharSequence... values) {
        if (values == null || values.length == 0) {
            return false;
        }
        for (CharSequence value : values) {
            if (isEmpty(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 如果对象为 null，则返回默认值。
     *
     * @param value        原始对象
     * @param defaultValue 默认值
     * @param <T>          对象类型
     * @return 原始对象或默认值
     */
    public static <T> T defaultIfNull(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * 如果字符串为空，则返回默认值。
     *
     * @param value        原始字符串
     * @param defaultValue 默认值
     * @return 原始字符串或默认值
     */
    public static String defaultIfEmpty(String value, String defaultValue) {
        return isEmpty(value) ? defaultValue : value;
    }

    /**
     * 如果字符串为空白，则返回默认值。
     *
     * @param value        原始字符串
     * @param defaultValue 默认值
     * @return 原始字符串或默认值
     */
    public static String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    /**
     * 如果字符串为空，则返回 null。
     *
     * @param value 原始字符串
     * @return 原始字符串或 null
     */
    public static String emptyToNull(String value) {
        return isEmpty(value) ? null : value;
    }

    /**
     * 如果字符串为空白，则返回 null。
     *
     * @param value 原始字符串
     * @return 原始字符串或 null
     */
    public static String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    /**
     * 如果字符串为 null，则返回空字符串。
     *
     * @param value 原始字符串
     * @return 原始字符串或空字符串
     */
    public static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 如果字符串为 null，则返回默认值。
     *
     * @param value        原始字符串
     * @param defaultValue 默认值
     * @return 原始字符串或默认值
     */
    public static String nullToDefault(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * 去除字符串前后的普通空白字符。
     *
     * @param value 原始字符串
     * @return 处理后的字符串
     */
    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * 去除字符串前后的普通空白字符，null 返回空字符串。
     *
     * @param value 原始字符串
     * @return 处理后的字符串
     */
    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 去除字符串前后的普通空白字符，处理后为空则返回 null。
     *
     * @param value 原始字符串
     * @return 处理后的字符串或 null
     */
    public static String trimToNull(String value) {
        return emptyToNull(trim(value));
    }

    /**
     * 去除字符串前后的 Unicode 空白字符。
     *
     * @param value 原始字符串
     * @return 处理后的字符串
     */
    public static String strip(String value) {
        return value == null ? null : value.strip();
    }

    /**
     * 去除字符串前后的 Unicode 空白字符，null 返回空字符串。
     *
     * @param value 原始字符串
     * @return 处理后的字符串
     */
    public static String stripToEmpty(String value) {
        return value == null ? "" : value.strip();
    }

    /**
     * 去除字符串前后的 Unicode 空白字符，处理后为空白则返回 null。
     *
     * @param value 原始字符串
     * @return 处理后的字符串或 null
     */
    public static String stripToNull(String value) {
        return blankToNull(strip(value));
    }

    /**
     * 移除字符串中的所有空白字符。
     *
     * @param value 原始字符串
     * @return 处理后的字符串
     */
    public static String removeAllWhitespace(String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        value.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .forEach(builder::appendCodePoint);
        return builder.toString();
    }

    /**
     * 归一化空白字符，将连续空白压缩为一个半角空格，并去除前后空白。
     *
     * @param value 原始字符串
     * @return 处理后的字符串
     */
    public static String normalizeWhitespace(String value) {
        if (value == null) {
            return null;
        }

        String stripped = value.strip();
        if (stripped.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(stripped.length());
        boolean previousWhitespace = false;

        for (int i = 0; i < stripped.length(); ) {
            int codePoint = stripped.codePointAt(i);
            if (Character.isWhitespace(codePoint)) {
                if (!previousWhitespace) {
                    builder.append(' ');
                    previousWhitespace = true;
                }
            } else {
                builder.appendCodePoint(codePoint);
                previousWhitespace = false;
            }
            i += Character.charCount(codePoint);
        }

        return builder.toString();
    }

    /**
     * 清理不可见格式字符，例如零宽字符、BOM、软连字符。
     *
     * @param value 原始字符串
     * @return 处理后的字符串
     */
    public static String cleanInvisibleChars(String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (!isInvisibleFormatChar(codePoint)) {
                builder.appendCodePoint(codePoint);
            }
            i += Character.charCount(codePoint);
        }

        return builder.toString();
    }

    /**
     * 清理控制字符，保留换行、回车和制表符。
     *
     * @param value 原始字符串
     * @return 处理后的字符串
     */
    public static String cleanControlChars(String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (!isRemovableControlChar(codePoint)) {
                builder.appendCodePoint(codePoint);
            }
            i += Character.charCount(codePoint);
        }

        return builder.toString();
    }

    /**
     * 判断是否为需要清理的不可见格式字符。
     *
     * @param codePoint Unicode 码点
     * @return 是否为不可见格式字符
     */
    private static boolean isInvisibleFormatChar(int codePoint) {
        return Character.getType(codePoint) == Character.FORMAT
                || codePoint == '\uFEFF'
                || codePoint == '\u00AD';
    }

    /**
     * 判断是否为需要移除的控制字符。
     *
     * @param codePoint Unicode 码点
     * @return 是否为需要移除的控制字符
     */
    private static boolean isRemovableControlChar(int codePoint) {
        return Character.isISOControl(codePoint)
                && codePoint != '\n'
                && codePoint != '\r'
                && codePoint != '\t';
    }

    /**
     * 安全比较两个字符串是否相等。
     *
     * @param value1 字符串1
     * @param value2 字符串2
     * @return 是否相等
     */
    public static boolean equals(CharSequence value1, CharSequence value2) {
        if (value1 == value2) {
            return true;
        }
        if (value1 == null || value2 == null) {
            return false;
        }
        if (value1.length() != value2.length()) {
            return false;
        }
        for (int i = 0; i < value1.length(); i++) {
            if (value1.charAt(i) != value2.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 安全比较两个字符串是否相等，忽略大小写。
     *
     * @param value1 字符串1
     * @param value2 字符串2
     * @return 是否相等
     */
    public static boolean equalsIgnoreCase(CharSequence value1, CharSequence value2) {
        if (value1 == value2) {
            return true;
        }
        if (value1 == null || value2 == null) {
            return false;
        }
        return value1.toString().equalsIgnoreCase(value2.toString());
    }

    /**
     * 判断字符串是否等于任意候选字符串。
     *
     * @param value      字符串
     * @param candidates 候选字符串数组
     * @return 是否匹配任意候选字符串
     */
    public static boolean equalsAny(CharSequence value, CharSequence... candidates) {
        if (candidates == null || candidates.length == 0) {
            return false;
        }
        for (CharSequence candidate : candidates) {
            if (equals(value, candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断字符串是否等于任意候选字符串，忽略大小写。
     *
     * @param value      字符串
     * @param candidates 候选字符串数组
     * @return 是否匹配任意候选字符串
     */
    public static boolean equalsAnyIgnoreCase(CharSequence value, CharSequence... candidates) {
        if (candidates == null || candidates.length == 0) {
            return false;
        }
        for (CharSequence candidate : candidates) {
            if (equalsIgnoreCase(value, candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 安全比较两个字符串大小，null 小于非 null。
     *
     * @param value1 字符串1
     * @param value2 字符串2
     * @return 比较结果
     */
    public static int compare(String value1, String value2) {
        return compare(value1, value2, true);
    }

    /**
     * 安全比较两个字符串大小。
     *
     * @param value1     字符串1
     * @param value2     字符串2
     * @param nullIsLess null 是否小于非 null
     * @return 比较结果
     */
    public static int compare(String value1, String value2, boolean nullIsLess) {
        if (value1 == value2) {
            return 0;
        }
        if (value1 == null) {
            return nullIsLess ? -1 : 1;
        }
        if (value2 == null) {
            return nullIsLess ? 1 : -1;
        }
        return value1.compareTo(value2);
    }

    /**
     * 安全比较两个字符串大小，忽略大小写，null 小于非 null。
     *
     * @param value1 字符串1
     * @param value2 字符串2
     * @return 比较结果
     */
    public static int compareIgnoreCase(String value1, String value2) {
        return compareIgnoreCase(value1, value2, true);
    }

    /**
     * 安全比较两个字符串大小，忽略大小写。
     *
     * @param value1     字符串1
     * @param value2     字符串2
     * @param nullIsLess null 是否小于非 null
     * @return 比较结果
     */
    public static int compareIgnoreCase(String value1, String value2, boolean nullIsLess) {
        if (value1 == value2) {
            return 0;
        }
        if (value1 == null) {
            return nullIsLess ? -1 : 1;
        }
        if (value2 == null) {
            return nullIsLess ? 1 : -1;
        }
        return value1.compareToIgnoreCase(value2);
    }

    /**
     * 归一化空白后比较两个字符串是否相等。
     *
     * @param value1 字符串1
     * @param value2 字符串2
     * @return 是否相等
     */
    public static boolean equalsNormalized(String value1, String value2) {
        return equals(normalizeWhitespace(value1), normalizeWhitespace(value2));
    }

    /**
     * 去除前后空白后比较两个字符串是否相等。
     *
     * @param value1 字符串1
     * @param value2 字符串2
     * @return 是否相等
     */
    public static boolean equalsTrimmed(String value1, String value2) {
        return equals(trim(value1), trim(value2));
    }

    /**
     * 判断字符串是否包含指定内容。
     *
     * @param value  字符串
     * @param search 查询内容
     * @return 是否包含
     */
    public static boolean contains(CharSequence value, CharSequence search) {
        if (value == null || search == null) {
            return false;
        }
        return value.toString().contains(search);
    }

    /**
     * 判断字符串是否包含指定内容，忽略大小写。
     *
     * @param value  字符串
     * @param search 查询内容
     * @return 是否包含
     */
    public static boolean containsIgnoreCase(CharSequence value, CharSequence search) {
        if (value == null || search == null) {
            return false;
        }
        return indexOfIgnoreCase(value, search) >= 0;
    }

    /**
     * 判断字符串是否包含任意查询内容。
     *
     * @param value    字符串
     * @param searches 查询内容数组
     * @return 是否包含任意查询内容
     */
    public static boolean containsAny(CharSequence value, CharSequence... searches) {
        if (value == null || searches == null || searches.length == 0) {
            return false;
        }
        for (CharSequence search : searches) {
            if (contains(value, search)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断字符串是否包含任意查询内容，忽略大小写。
     *
     * @param value    字符串
     * @param searches 查询内容数组
     * @return 是否包含任意查询内容
     */
    public static boolean containsAnyIgnoreCase(CharSequence value, CharSequence... searches) {
        if (value == null || searches == null || searches.length == 0) {
            return false;
        }
        for (CharSequence search : searches) {
            if (containsIgnoreCase(value, search)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断字符串是否包含全部查询内容。
     *
     * @param value    字符串
     * @param searches 查询内容数组
     * @return 是否包含全部查询内容
     */
    public static boolean containsAll(CharSequence value, CharSequence... searches) {
        if (value == null || searches == null || searches.length == 0) {
            return false;
        }
        for (CharSequence search : searches) {
            if (!contains(value, search)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否不包含任何查询内容。
     *
     * @param value    字符串
     * @param searches 查询内容数组
     * @return 是否不包含任何查询内容
     */
    public static boolean containsNone(CharSequence value, CharSequence... searches) {
        if (value == null) {
            return true;
        }
        if (searches == null || searches.length == 0) {
            return true;
        }
        for (CharSequence search : searches) {
            if (contains(value, search)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否包含空白字符。
     *
     * @param value 字符串
     * @return 是否包含空白字符
     */
    public static boolean containsWhitespace(CharSequence value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断字符串是否包含中文字符。
     *
     * @param value 字符串
     * @return 是否包含中文字符
     */
    public static boolean containsChinese(CharSequence value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); ) {
            int codePoint = Character.codePointAt(value, i);
            if (isChineseCodePoint(codePoint)) {
                return true;
            }
            i += Character.charCount(codePoint);
        }
        return false;
    }

    /**
     * 判断字符串是否包含 Emoji 字符。
     *
     * @param value 字符串
     * @return 是否包含 Emoji 字符
     */
    public static boolean containsEmoji(CharSequence value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); ) {
            int codePoint = Character.codePointAt(value, i);
            if (isEmojiCodePoint(codePoint)) {
                return true;
            }
            i += Character.charCount(codePoint);
        }
        return false;
    }

    /**
     * 判断字符串是否以指定前缀开头。
     *
     * @param value  字符串
     * @param prefix 前缀
     * @return 是否以指定前缀开头
     */
    public static boolean startsWith(CharSequence value, CharSequence prefix) {
        if (value == null || prefix == null) {
            return false;
        }
        if (prefix.length() > value.length()) {
            return false;
        }
        for (int i = 0; i < prefix.length(); i++) {
            if (value.charAt(i) != prefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否以指定前缀开头，忽略大小写。
     *
     * @param value  字符串
     * @param prefix 前缀
     * @return 是否以指定前缀开头
     */
    public static boolean startsWithIgnoreCase(CharSequence value, CharSequence prefix) {
        if (value == null || prefix == null) {
            return false;
        }
        if (prefix.length() > value.length()) {
            return false;
        }
        return value.toString().regionMatches(true, 0, prefix.toString(), 0, prefix.length());
    }

    /**
     * 判断字符串是否以任意前缀开头。
     *
     * @param value    字符串
     * @param prefixes 前缀数组
     * @return 是否以任意前缀开头
     */
    public static boolean startsWithAny(CharSequence value, CharSequence... prefixes) {
        if (value == null || prefixes == null || prefixes.length == 0) {
            return false;
        }
        for (CharSequence prefix : prefixes) {
            if (startsWith(value, prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断字符串是否以指定后缀结尾。
     *
     * @param value  字符串
     * @param suffix 后缀
     * @return 是否以指定后缀结尾
     */
    public static boolean endsWith(CharSequence value, CharSequence suffix) {
        if (value == null || suffix == null) {
            return false;
        }
        if (suffix.length() > value.length()) {
            return false;
        }

        int valueOffset = value.length() - suffix.length();
        for (int i = 0; i < suffix.length(); i++) {
            if (value.charAt(valueOffset + i) != suffix.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否以指定后缀结尾，忽略大小写。
     *
     * @param value  字符串
     * @param suffix 后缀
     * @return 是否以指定后缀结尾
     */
    public static boolean endsWithIgnoreCase(CharSequence value, CharSequence suffix) {
        if (value == null || suffix == null) {
            return false;
        }
        if (suffix.length() > value.length()) {
            return false;
        }
        int start = value.length() - suffix.length();
        return value.toString().regionMatches(true, start, suffix.toString(), 0, suffix.length());
    }

    /**
     * 判断字符串是否以任意后缀结尾。
     *
     * @param value    字符串
     * @param suffixes 后缀数组
     * @return 是否以任意后缀结尾
     */
    public static boolean endsWithAny(CharSequence value, CharSequence... suffixes) {
        if (value == null || suffixes == null || suffixes.length == 0) {
            return false;
        }
        for (CharSequence suffix : suffixes) {
            if (endsWith(value, suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断字符串是否包含指定前缀。
     *
     * @param value  字符串
     * @param prefix 前缀
     * @return 是否包含指定前缀
     */
    public static boolean hasPrefix(CharSequence value, CharSequence prefix) {
        return startsWith(value, prefix);
    }

    /**
     * 判断字符串是否包含指定后缀。
     *
     * @param value  字符串
     * @param suffix 后缀
     * @return 是否包含指定后缀
     */
    public static boolean hasSuffix(CharSequence value, CharSequence suffix) {
        return endsWith(value, suffix);
    }

    /**
     * 判断码点是否为中文字符。
     *
     * @param codePoint Unicode 码点
     * @return 是否为中文字符
     */
    private static boolean isChineseCodePoint(int codePoint) {
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        return script == Character.UnicodeScript.HAN;
    }

    /**
     * 判断码点是否为常见 Emoji 字符。
     *
     * @param codePoint Unicode 码点
     * @return 是否为常见 Emoji 字符
     */
    private static boolean isEmojiCodePoint(int codePoint) {
        return (codePoint >= 0x1F300 && codePoint <= 0x1FAFF)
                || (codePoint >= 0x2600 && codePoint <= 0x27BF)
                || (codePoint >= 0x2300 && codePoint <= 0x23FF);
    }

    /**
     * 查找字符串首次出现的位置。
     *
     * @param value  字符串
     * @param search 查询内容
     * @return 首次出现的位置，不存在返回 -1
     */
    public static int indexOf(CharSequence value, CharSequence search) {
        if (value == null || search == null) {
            return -1;
        }
        return value.toString().indexOf(search.toString());
    }

    /**
     * 从指定位置开始查找字符串首次出现的位置。
     *
     * @param value     字符串
     * @param search    查询内容
     * @param fromIndex 开始位置
     * @return 首次出现的位置，不存在返回 -1
     */
    public static int indexOf(CharSequence value, CharSequence search, int fromIndex) {
        if (value == null || search == null) {
            return -1;
        }
        return value.toString().indexOf(search.toString(), Math.max(fromIndex, 0));
    }

    /**
     * 查找字符串首次出现的位置，忽略大小写。
     *
     * @param value  字符串
     * @param search 查询内容
     * @return 首次出现的位置，不存在返回 -1
     */
    public static int indexOfIgnoreCase(CharSequence value, CharSequence search) {
        return indexOfIgnoreCase(value, search, 0);
    }

    /**
     * 从指定位置开始查找字符串首次出现的位置，忽略大小写。
     *
     * @param value     字符串
     * @param search    查询内容
     * @param fromIndex 开始位置
     * @return 首次出现的位置，不存在返回 -1
     */
    public static int indexOfIgnoreCase(CharSequence value, CharSequence search, int fromIndex) {
        if (value == null || search == null) {
            return -1;
        }

        int searchLength = search.length();
        int valueLength = value.length();
        int start = Math.max(fromIndex, 0);

        if (searchLength == 0) {
            return start <= valueLength ? start : valueLength;
        }
        if (searchLength > valueLength || start > valueLength - searchLength) {
            return -1;
        }

        String source = value.toString();
        String target = search.toString();
        for (int i = start; i <= valueLength - searchLength; i++) {
            if (source.regionMatches(true, i, target, 0, searchLength)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 查找字符串最后一次出现的位置。
     *
     * @param value  字符串
     * @param search 查询内容
     * @return 最后一次出现的位置，不存在返回 -1
     */
    public static int lastIndexOf(CharSequence value, CharSequence search) {
        if (value == null || search == null) {
            return -1;
        }
        return value.toString().lastIndexOf(search.toString());
    }

    /**
     * 从指定位置向前查找字符串最后一次出现的位置。
     *
     * @param value     字符串
     * @param search    查询内容
     * @param fromIndex 开始位置
     * @return 最后一次出现的位置，不存在返回 -1
     */
    public static int lastIndexOf(CharSequence value, CharSequence search, int fromIndex) {
        if (value == null || search == null) {
            return -1;
        }
        return value.toString().lastIndexOf(search.toString(), fromIndex);
    }

    /**
     * 查找字符串最后一次出现的位置，忽略大小写。
     *
     * @param value  字符串
     * @param search 查询内容
     * @return 最后一次出现的位置，不存在返回 -1
     */
    public static int lastIndexOfIgnoreCase(CharSequence value, CharSequence search) {
        if (value == null || search == null) {
            return -1;
        }
        return lastIndexOfIgnoreCase(value, search, value.length());
    }

    /**
     * 从指定位置向前查找字符串最后一次出现的位置，忽略大小写。
     *
     * @param value     字符串
     * @param search    查询内容
     * @param fromIndex 开始位置
     * @return 最后一次出现的位置，不存在返回 -1
     */
    public static int lastIndexOfIgnoreCase(CharSequence value, CharSequence search, int fromIndex) {
        if (value == null || search == null) {
            return -1;
        }

        int searchLength = search.length();
        int valueLength = value.length();

        if (searchLength == 0) {
            return Math.min(Math.max(fromIndex, 0), valueLength);
        }
        if (searchLength > valueLength) {
            return -1;
        }

        int start = Math.min(fromIndex, valueLength - searchLength);
        if (start < 0) {
            return -1;
        }

        String source = value.toString();
        String target = search.toString();
        for (int i = start; i >= 0; i--) {
            if (source.regionMatches(true, i, target, 0, searchLength)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 查找字符串第 ordinal 次出现的位置。
     *
     * @param value   字符串
     * @param search  查询内容
     * @param ordinal 出现次数，从 1 开始
     * @return 指定次数出现的位置，不存在返回 -1
     */
    public static int ordinalIndexOf(CharSequence value, CharSequence search, int ordinal) {
        if (value == null || search == null || ordinal <= 0) {
            return -1;
        }
        if (search.isEmpty()) {
            return 0;
        }

        int found = 0;
        int index = -1;
        do {
            index = indexOf(value, search, index + 1);
            if (index < 0) {
                return -1;
            }
            found++;
        } while (found < ordinal);

        return index;
    }

    /**
     * 统计查询内容出现次数。
     *
     * @param value  字符串
     * @param search 查询内容
     * @return 出现次数
     */
    public static int countMatches(CharSequence value, CharSequence search) {
        if (isEmpty(value) || isEmpty(search)) {
            return 0;
        }

        int count = 0;
        int index = 0;
        int searchLength = search.length();

        while ((index = indexOf(value, search, index)) >= 0) {
            count++;
            index += searchLength;
        }

        return count;
    }

    /**
     * 查找任意查询内容首次出现的位置。
     *
     * @param value    字符串
     * @param searches 查询内容数组
     * @return 首次出现的位置，不存在返回 -1
     */
    public static int firstIndexOfAny(CharSequence value, CharSequence... searches) {
        if (value == null || searches == null || searches.length == 0) {
            return -1;
        }

        int result = -1;
        for (CharSequence search : searches) {
            int index = indexOf(value, search);
            if (index >= 0 && (result < 0 || index < result)) {
                result = index;
            }
        }

        return result;
    }

    /**
     * 查找任意查询内容最后一次出现的位置。
     *
     * @param value    字符串
     * @param searches 查询内容数组
     * @return 最后一次出现的位置，不存在返回 -1
     */
    public static int lastIndexOfAny(CharSequence value, CharSequence... searches) {
        if (value == null || searches == null || searches.length == 0) {
            return -1;
        }

        int result = -1;
        for (CharSequence search : searches) {
            int index = lastIndexOf(value, search);
            if (index > result) {
                result = index;
            }
        }

        return result;
    }

    /**
     * 安全截取字符串。
     *
     * @param value 字符串
     * @param start 开始位置，包含
     * @param end   结束位置，不包含
     * @return 截取后的字符串
     */
    public static String substring(String value, int start, int end) {
        return safeSubstring(value, start, end);
    }

    /**
     * 截取查询内容之前的字符串。
     *
     * @param value     字符串
     * @param separator 分隔符
     * @return 截取后的字符串
     */
    public static String substringBefore(String value, String separator) {
        if (value == null || separator == null) {
            return value;
        }
        if (separator.isEmpty()) {
            return "";
        }

        int index = value.indexOf(separator);
        return index < 0 ? value : value.substring(0, index);
    }

    /**
     * 截取查询内容之后的字符串。
     *
     * @param value     字符串
     * @param separator 分隔符
     * @return 截取后的字符串
     */
    public static String substringAfter(String value, String separator) {
        if (value == null) {
            return null;
        }
        if (separator == null) {
            return "";
        }

        int index = value.indexOf(separator);
        return index < 0 ? "" : value.substring(index + separator.length());
    }

    /**
     * 截取最后一个查询内容之前的字符串。
     *
     * @param value     字符串
     * @param separator 分隔符
     * @return 截取后的字符串
     */
    public static String substringBeforeLast(String value, String separator) {
        if (value == null || separator == null || separator.isEmpty()) {
            return value;
        }

        int index = value.lastIndexOf(separator);
        return index < 0 ? value : value.substring(0, index);
    }

    /**
     * 截取最后一个查询内容之后的字符串。
     *
     * @param value     字符串
     * @param separator 分隔符
     * @return 截取后的字符串
     */
    public static String substringAfterLast(String value, String separator) {
        if (value == null) {
            return null;
        }
        if (separator == null || separator.isEmpty()) {
            return "";
        }

        int index = value.lastIndexOf(separator);
        return index < 0 || index == value.length() - separator.length()
                ? ""
                : value.substring(index + separator.length());
    }

    /**
     * 从左侧截取指定长度字符串。
     *
     * @param value  字符串
     * @param length 长度
     * @return 截取后的字符串
     */
    public static String left(String value, int length) {
        if (value == null) {
            return null;
        }
        if (length <= 0) {
            return "";
        }
        return value.length() <= length ? value : value.substring(0, length);
    }

    /**
     * 从右侧截取指定长度字符串。
     *
     * @param value  字符串
     * @param length 长度
     * @return 截取后的字符串
     */
    public static String right(String value, int length) {
        if (value == null) {
            return null;
        }
        if (length <= 0) {
            return "";
        }
        return value.length() <= length ? value : value.substring(value.length() - length);
    }

    /**
     * 从指定位置开始截取指定长度字符串。
     *
     * @param value  字符串
     * @param start  开始位置
     * @param length 长度
     * @return 截取后的字符串
     */
    public static String mid(String value, int start, int length) {
        if (value == null) {
            return null;
        }
        if (length <= 0) {
            return "";
        }

        int safeStart = Math.max(start, 0);
        if (safeStart >= value.length()) {
            return "";
        }

        int safeEnd = Math.min(safeStart + length, value.length());
        return value.substring(safeStart, safeEnd);
    }

    /**
     * 按最大长度裁剪字符串。
     *
     * @param value     字符串
     * @param maxLength 最大长度
     * @return 裁剪后的字符串
     */
    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (maxLength <= 0) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * 按最大长度裁剪字符串，并追加后缀。
     *
     * @param value     字符串
     * @param maxLength 最大长度
     * @param suffix    后缀
     * @return 裁剪后的字符串
     */
    public static String truncateWithSuffix(String value, int maxLength, String suffix) {
        if (value == null) {
            return null;
        }
        if (maxLength <= 0) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }

        String safeSuffix = nullToEmpty(suffix);
        if (safeSuffix.length() >= maxLength) {
            return safeSuffix.substring(0, maxLength);
        }

        return value.substring(0, maxLength - safeSuffix.length()) + safeSuffix;
    }

    /**
     * 安全截取字符串，自动修正越界下标。
     *
     * @param value 字符串
     * @param start 开始位置，包含
     * @param end   结束位置，不包含
     * @return 截取后的字符串
     */
    public static String safeSubstring(String value, int start, int end) {
        if (value == null) {
            return null;
        }

        int length = value.length();
        int safeStart = Math.max(start, 0);
        int safeEnd = Math.min(end, length);

        if (safeStart >= safeEnd) {
            return "";
        }

        return value.substring(safeStart, safeEnd);
    }

    /**
     * 替换字符串中的指定内容。
     *
     * @param value       字符串
     * @param search      查询内容
     * @param replacement 替换内容
     * @return 替换后的字符串
     */
    public static String replace(String value, String search, String replacement) {
        if (value == null || isEmpty(search)) {
            return value;
        }
        return value.replace(search, nullToEmpty(replacement));
    }

    /**
     * 替换字符串中的指定内容，忽略大小写。
     *
     * @param value       字符串
     * @param search      查询内容
     * @param replacement 替换内容
     * @return 替换后的字符串
     */
    public static String replaceIgnoreCase(String value, String search, String replacement) {
        if (value == null || isEmpty(search)) {
            return value;
        }

        String safeReplacement = nullToEmpty(replacement);
        StringBuilder builder = new StringBuilder(value.length());
        int searchLength = search.length();
        int start = 0;
        int index;

        while ((index = indexOfIgnoreCase(value, search, start)) >= 0) {
            builder.append(value, start, index);
            builder.append(safeReplacement);
            start = index + searchLength;
        }

        builder.append(value, start, value.length());
        return builder.toString();
    }

    /**
     * 替换第一次出现的指定内容。
     *
     * @param value       字符串
     * @param search      查询内容
     * @param replacement 替换内容
     * @return 替换后的字符串
     */
    public static String replaceFirst(String value, String search, String replacement) {
        if (value == null || isEmpty(search)) {
            return value;
        }

        int index = value.indexOf(search);
        if (index < 0) {
            return value;
        }

        return value.substring(0, index) + nullToEmpty(replacement) + value.substring(index + search.length());
    }

    /**
     * 替换最后一次出现的指定内容。
     *
     * @param value       字符串
     * @param search      查询内容
     * @param replacement 替换内容
     * @return 替换后的字符串
     */
    public static String replaceLast(String value, String search, String replacement) {
        if (value == null || isEmpty(search)) {
            return value;
        }

        int index = value.lastIndexOf(search);
        if (index < 0) {
            return value;
        }

        return value.substring(0, index) + nullToEmpty(replacement) + value.substring(index + search.length());
    }

    /**
     * 按映射批量替换字符串内容。
     *
     * @param value        字符串
     * @param replacements 替换映射
     * @return 替换后的字符串
     */
    public static String replaceEach(String value, java.util.Map<String, String> replacements) {
        if (value == null || replacements == null || replacements.isEmpty()) {
            return value;
        }

        String result = value;
        for (java.util.Map.Entry<String, String> entry : replacements.entrySet()) {
            result = replace(result, entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * 删除字符串中的指定内容。
     *
     * @param value  字符串
     * @param remove 删除内容
     * @return 删除后的字符串
     */
    public static String remove(String value, String remove) {
        return replace(value, remove, "");
    }

    /**
     * 删除字符串中的指定内容，忽略大小写。
     *
     * @param value  字符串
     * @param remove 删除内容
     * @return 删除后的字符串
     */
    public static String removeIgnoreCase(String value, String remove) {
        return replaceIgnoreCase(value, remove, "");
    }

    /**
     * 删除字符串前缀。
     *
     * @param value  字符串
     * @param prefix 前缀
     * @return 删除后的字符串
     */
    public static String removePrefix(String value, String prefix) {
        return removeStart(value, prefix);
    }

    /**
     * 删除字符串后缀。
     *
     * @param value  字符串
     * @param suffix 后缀
     * @return 删除后的字符串
     */
    public static String removeSuffix(String value, String suffix) {
        return removeEnd(value, suffix);
    }

    /**
     * 删除字符串开头的指定内容。
     *
     * @param value  字符串
     * @param remove 删除内容
     * @return 删除后的字符串
     */
    public static String removeStart(String value, String remove) {
        if (value == null || isEmpty(remove)) {
            return value;
        }
        return startsWith(value, remove) ? value.substring(remove.length()) : value;
    }

    /**
     * 删除字符串结尾的指定内容。
     *
     * @param value  字符串
     * @param remove 删除内容
     * @return 删除后的字符串
     */
    public static String removeEnd(String value, String remove) {
        if (value == null || isEmpty(remove)) {
            return value;
        }
        return endsWith(value, remove) ? value.substring(0, value.length() - remove.length()) : value;
    }

    /**
     * 删除字符串开头的指定内容，忽略大小写。
     *
     * @param value  字符串
     * @param remove 删除内容
     * @return 删除后的字符串
     */
    public static String removeStartIgnoreCase(String value, String remove) {
        if (value == null || isEmpty(remove)) {
            return value;
        }
        return startsWithIgnoreCase(value, remove) ? value.substring(remove.length()) : value;
    }

    /**
     * 删除字符串结尾的指定内容，忽略大小写。
     *
     * @param value  字符串
     * @param remove 删除内容
     * @return 删除后的字符串
     */
    public static String removeEndIgnoreCase(String value, String remove) {
        if (value == null || isEmpty(remove)) {
            return value;
        }
        return endsWithIgnoreCase(value, remove) ? value.substring(0, value.length() - remove.length()) : value;
    }

    /**
     * 按分隔符分割字符串。
     *
     * @param value     字符串
     * @param delimiter 分隔符
     * @return 分割后的数组
     */
    public static String[] split(String value, String delimiter) {
        if (value == null) {
            return new String[0];
        }
        if (delimiter == null || delimiter.isEmpty()) {
            return new String[]{value};
        }
        return value.split(java.util.regex.Pattern.quote(delimiter), -1);
    }

    /**
     * 按分隔符分割字符串并转换为列表。
     *
     * @param value     字符串
     * @param delimiter 分隔符
     * @return 分割后的列表
     */
    public static java.util.List<String> splitToList(String value, String delimiter) {
        return new java.util.ArrayList<>(java.util.Arrays.asList(split(value, delimiter)));
    }

    /**
     * 按分隔符分割字符串，并去除每个元素前后的空白字符。
     *
     * @param value     字符串
     * @param delimiter 分隔符
     * @return 分割后的数组
     */
    public static String[] splitTrim(String value, String delimiter) {
        String[] array = split(value, delimiter);
        for (int i = 0; i < array.length; i++) {
            array[i] = trimToEmpty(array[i]);
        }
        return array;
    }

    /**
     * 按分隔符分割字符串，并去除每个元素前后的空白字符后转换为列表。
     *
     * @param value     字符串
     * @param delimiter 分隔符
     * @return 分割后的列表
     */
    public static java.util.List<String> splitTrimToList(String value, String delimiter) {
        return new java.util.ArrayList<>(java.util.Arrays.asList(splitTrim(value, delimiter)));
    }

    /**
     * 按固定长度分割字符串。
     *
     * @param value  字符串
     * @param length 每段长度
     * @return 分割后的列表
     */
    public static java.util.List<String> splitByLength(String value, int length) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (value == null || value.isEmpty() || length <= 0) {
            return result;
        }

        for (int start = 0; start < value.length(); start += length) {
            int end = Math.min(start + length, value.length());
            result.add(value.substring(start, end));
        }

        return result;
    }

    /**
     * 按行分割字符串，兼容 Windows、Linux 和 macOS 换行符。
     *
     * @param value 字符串
     * @return 行列表
     */
    public static java.util.List<String> splitLines(String value) {
        if (value == null) {
            return new java.util.ArrayList<>();
        }
        return new java.util.ArrayList<>(java.util.Arrays.asList(value.split("\\R", -1)));
    }

    /**
     * 按第一个分隔符分割字符串。
     *
     * @param value     字符串
     * @param delimiter 分隔符
     * @return 长度为 2 的数组，未找到分隔符时第二个元素为空字符串
     */
    public static String[] splitFirst(String value, String delimiter) {
        if (value == null) {
            return new String[]{"", ""};
        }
        if (delimiter == null || delimiter.isEmpty()) {
            return new String[]{value, ""};
        }

        int index = value.indexOf(delimiter);
        if (index < 0) {
            return new String[]{value, ""};
        }

        return new String[]{
                value.substring(0, index),
                value.substring(index + delimiter.length())
        };
    }

    /**
     * 按最后一个分隔符分割字符串。
     *
     * @param value     字符串
     * @param delimiter 分隔符
     * @return 长度为 2 的数组，未找到分隔符时第二个元素为空字符串
     */
    public static String[] splitLast(String value, String delimiter) {
        if (value == null) {
            return new String[]{"", ""};
        }
        if (delimiter == null || delimiter.isEmpty()) {
            return new String[]{value, ""};
        }

        int index = value.lastIndexOf(delimiter);
        if (index < 0) {
            return new String[]{value, ""};
        }

        return new String[]{
                value.substring(0, index),
                value.substring(index + delimiter.length())
        };
    }

    /**
     * 按分隔符分割字符串并转换为有序集合。
     *
     * @param value     字符串
     * @param delimiter 分隔符
     * @return 分割后的有序集合
     */
    public static java.util.Set<String> splitToSet(String value, String delimiter) {
        return new java.util.LinkedHashSet<>(splitToList(value, delimiter));
    }

    /**
     * 按分隔符分割字符串并转换为数组。
     *
     * @param value     字符串
     * @param delimiter 分隔符
     * @return 分割后的数组
     */
    public static String[] splitToArray(String value, String delimiter) {
        return split(value, delimiter);
    }

    /**
     * 拼接数组元素。
     *
     * @param delimiter 分隔符
     * @param values    元素数组
     * @return 拼接后的字符串
     */
    public static String join(String delimiter, Object... values) {
        if (values == null || values.length == 0) {
            return "";
        }
        return join(java.util.Arrays.asList(values), delimiter);
    }

    /**
     * 拼接集合元素。
     *
     * @param values    元素集合
     * @param delimiter 分隔符
     * @return 拼接后的字符串
     */
    public static String join(java.lang.Iterable<?> values, String delimiter) {
        if (values == null) {
            return "";
        }

        String safeDelimiter = nullToEmpty(delimiter);
        StringBuilder builder = new StringBuilder();
        java.util.Iterator<?> iterator = values.iterator();

        while (iterator.hasNext()) {
            Object value = iterator.next();
            builder.append(value == null ? "null" : value);
            if (iterator.hasNext()) {
                builder.append(safeDelimiter);
            }
        }

        return builder.toString();
    }

    /**
     * 拼接数组元素，忽略 null。
     *
     * @param delimiter 分隔符
     * @param values    元素数组
     * @return 拼接后的字符串
     */
    public static String joinIgnoreNull(String delimiter, Object... values) {
        if (values == null || values.length == 0) {
            return "";
        }

        java.util.List<Object> result = new java.util.ArrayList<>();
        for (Object value : values) {
            if (value != null) {
                result.add(value);
            }
        }

        return join(result, delimiter);
    }

    /**
     * 拼接数组元素，忽略空字符串。
     *
     * @param delimiter 分隔符
     * @param values    元素数组
     * @return 拼接后的字符串
     */
    public static String joinIgnoreEmpty(String delimiter, String... values) {
        if (values == null || values.length == 0) {
            return "";
        }

        java.util.List<String> result = new java.util.ArrayList<>();
        for (String value : values) {
            if (isNotEmpty(value)) {
                result.add(value);
            }
        }

        return join(result, delimiter);
    }

    /**
     * 拼接数组元素，忽略空白字符串。
     *
     * @param delimiter 分隔符
     * @param values    元素数组
     * @return 拼接后的字符串
     */
    public static String joinIgnoreBlank(String delimiter, String... values) {
        if (values == null || values.length == 0) {
            return "";
        }

        java.util.List<String> result = new java.util.ArrayList<>();
        for (String value : values) {
            if (isNotBlank(value)) {
                result.add(value);
            }
        }

        return join(result, delimiter);
    }

    /**
     * 为每个非 null 元素添加前缀后拼接。
     *
     * @param values    元素集合
     * @param delimiter 分隔符
     * @param prefix    前缀
     * @return 拼接后的字符串
     */
    public static String joinWithPrefix(java.lang.Iterable<?> values, String delimiter, String prefix) {
        if (values == null) {
            return "";
        }

        java.util.List<String> result = new java.util.ArrayList<>();
        String safePrefix = nullToEmpty(prefix);

        for (Object value : values) {
            if (value != null) {
                result.add(safePrefix + value);
            }
        }

        return join(result, delimiter);
    }

    /**
     * 为每个非 null 元素添加后缀后拼接。
     *
     * @param values    元素集合
     * @param delimiter 分隔符
     * @param suffix    后缀
     * @return 拼接后的字符串
     */
    public static String joinWithSuffix(java.lang.Iterable<?> values, String delimiter, String suffix) {
        if (values == null) {
            return "";
        }

        java.util.List<String> result = new java.util.ArrayList<>();
        String safeSuffix = nullToEmpty(suffix);

        for (Object value : values) {
            if (value != null) {
                result.add(value + safeSuffix);
            }
        }

        return join(result, delimiter);
    }

    /**
     * 拼接 Map 键值对。
     *
     * @param map            Map 数据
     * @param entryDelimiter 键值对分隔符
     * @param keyValueSymbol 键值连接符
     * @return 拼接后的字符串
     */
    public static String joinMap(java.util.Map<?, ?> map, String entryDelimiter, String keyValueSymbol) {
        if (map == null || map.isEmpty()) {
            return "";
        }

        String safeEntryDelimiter = nullToEmpty(entryDelimiter);
        String safeKeyValueSymbol = nullToEmpty(keyValueSymbol);
        StringBuilder builder = new StringBuilder();

        java.util.Iterator<? extends java.util.Map.Entry<?, ?>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            java.util.Map.Entry<?, ?> entry = iterator.next();
            builder.append(entry.getKey()).append(safeKeyValueSymbol).append(entry.getValue());
            if (iterator.hasNext()) {
                builder.append(safeEntryDelimiter);
            }
        }

        return builder.toString();
    }

    /**
     * 直接拼接数组元素，null 按空字符串处理。
     *
     * @param values 元素数组
     * @return 拼接后的字符串
     */
    public static String concat(Object... values) {
        if (values == null || values.length == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (Object value : values) {
            if (value != null) {
                builder.append(value);
            }
        }

        return builder.toString();
    }

    /**
     * 直接拼接非空白字符串。
     *
     * @param values 字符串数组
     * @return 拼接后的字符串
     */
    public static String concatIfNotBlank(String... values) {
        if (values == null || values.length == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (isNotBlank(value)) {
                builder.append(value);
            }
        }

        return builder.toString();
    }

    /**
     * 使用 {} 占位符顺序格式化字符串。
     *
     * @param template 模板字符串
     * @param args     参数数组
     * @return 格式化后的字符串
     */
    public static String format(String template, Object... args) {
        return formatIndexed(template, args);
    }

    /**
     * 使用 {} 占位符顺序格式化字符串。
     *
     * @param template 模板字符串
     * @param args     参数数组
     * @return 格式化后的字符串
     */
    public static String formatIndexed(String template, Object... args) {
        if (template == null) {
            return null;
        }
        if (args == null || args.length == 0) {
            return template;
        }

        StringBuilder builder = new StringBuilder(template.length() + args.length * 16);
        int argIndex = 0;

        for (int i = 0; i < template.length(); i++) {
            char current = template.charAt(i);
            if (current == '\\' && i + 2 < template.length()
                    && template.charAt(i + 1) == '{'
                    && template.charAt(i + 2) == '}') {
                builder.append("{}");
                i += 2;
                continue;
            }

            if (current == '{' && i + 1 < template.length() && template.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    builder.append(args[argIndex++]);
                } else {
                    builder.append("{}");
                }
                i++;
                continue;
            }

            builder.append(current);
        }

        return builder.toString();
    }

    /**
     * 使用 {0}、{1} 格式占位符格式化字符串。
     *
     * @param template 模板字符串
     * @param args     参数数组
     * @return 格式化后的字符串
     */
    public static String formatNumbered(String template, Object... args) {
        if (template == null) {
            return null;
        }
        if (args == null || args.length == 0) {
            return template;
        }

        String result = template;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", String.valueOf(args[i]));
        }

        return result;
    }

    /**
     * 使用 {name} 命名占位符格式化字符串。
     *
     * @param template 模板字符串
     * @param values   参数映射
     * @return 格式化后的字符串
     */
    public static String formatNamed(String template, java.util.Map<String, ?> values) {
        return templateReplaceIgnoreMissing(template, values, "{", "}");
    }

    /**
     * 使用 Map 格式化字符串。
     *
     * @param template 模板字符串
     * @param values   参数映射
     * @return 格式化后的字符串
     */
    public static String formatByMap(String template, java.util.Map<String, ?> values) {
        return formatNamed(template, values);
    }

    /**
     * 存在参数时格式化字符串。
     *
     * @param template 模板字符串
     * @param args     参数数组
     * @return 格式化后的字符串
     */
    public static String formatIfArgsPresent(String template, Object... args) {
        if (args == null || args.length == 0) {
            return template;
        }
        return format(template, args);
    }

    /**
     * 使用 MessageFormat 格式化字符串。
     *
     * @param template 模板字符串
     * @param args     参数数组
     * @return 格式化后的字符串
     */
    public static String messageFormat(String template, Object... args) {
        if (template == null) {
            return null;
        }
        return java.text.MessageFormat.format(template, args == null ? new Object[0] : args);
    }

    /**
     * 使用 String.format 格式化字符串。
     *
     * @param template 模板字符串
     * @param args     参数数组
     * @return 格式化后的字符串
     */
    public static String printfFormat(String template, Object... args) {
        if (template == null) {
            return null;
        }
        return String.format(template, args == null ? new Object[0] : args);
    }

    /**
     * 使用默认 ${name} 模板占位符替换字符串，缺失值替换为空字符串。
     *
     * @param template 模板字符串
     * @param values   参数映射
     * @return 替换后的字符串
     */
    public static String templateReplace(String template, java.util.Map<String, ?> values) {
        return templateReplace(template, values, "${", "}");
    }

    /**
     * 使用指定模板占位符替换字符串，缺失值替换为空字符串。
     *
     * @param template 模板字符串
     * @param values   参数映射
     * @param prefix   占位符前缀
     * @param suffix   占位符后缀
     * @return 替换后的字符串
     */
    public static String templateReplace(String template, java.util.Map<String, ?> values, String prefix, String suffix) {
        return doTemplateReplace(template, values, prefix, suffix, false);
    }

    /**
     * 使用默认 ${name} 模板占位符替换字符串，缺失值保留原占位符。
     *
     * @param template 模板字符串
     * @param values   参数映射
     * @return 替换后的字符串
     */
    public static String templateReplaceIgnoreMissing(String template, java.util.Map<String, ?> values) {
        return templateReplaceIgnoreMissing(template, values, "${", "}");
    }

    /**
     * 使用指定模板占位符替换字符串，缺失值保留原占位符。
     *
     * @param template 模板字符串
     * @param values   参数映射
     * @param prefix   占位符前缀
     * @param suffix   占位符后缀
     * @return 替换后的字符串
     */
    public static String templateReplaceIgnoreMissing(String template, java.util.Map<String, ?> values, String prefix, String suffix) {
        return doTemplateReplace(template, values, prefix, suffix, true);
    }

    /**
     * 执行模板占位符替换。
     *
     * @param template      模板字符串
     * @param values        参数映射
     * @param prefix        占位符前缀
     * @param suffix        占位符后缀
     * @param ignoreMissing 是否忽略缺失值
     * @return 替换后的字符串
     */
    private static String doTemplateReplace(String template, java.util.Map<String, ?> values, String prefix, String suffix, boolean ignoreMissing) {
        if (template == null) {
            return null;
        }
        if (values == null || values.isEmpty()) {
            return ignoreMissing ? template : removeTemplatePlaceholders(template, prefix, suffix);
        }
        if (isEmpty(prefix) || isEmpty(suffix)) {
            return template;
        }

        StringBuilder builder = new StringBuilder(template.length());
        int start = 0;

        while (start < template.length()) {
            int prefixIndex = template.indexOf(prefix, start);
            if (prefixIndex < 0) {
                builder.append(template, start, template.length());
                break;
            }

            int suffixIndex = template.indexOf(suffix, prefixIndex + prefix.length());
            if (suffixIndex < 0) {
                builder.append(template, start, template.length());
                break;
            }

            builder.append(template, start, prefixIndex);

            String key = template.substring(prefixIndex + prefix.length(), suffixIndex);
            if (values.containsKey(key)) {
                builder.append(values.get(key));
            } else if (ignoreMissing) {
                builder.append(template, prefixIndex, suffixIndex + suffix.length());
            }

            start = suffixIndex + suffix.length();
        }

        return builder.toString();
    }

    /**
     * 移除模板占位符。
     *
     * @param template 模板字符串
     * @param prefix   占位符前缀
     * @param suffix   占位符后缀
     * @return 移除占位符后的字符串
     */
    private static String removeTemplatePlaceholders(String template, String prefix, String suffix) {
        if (isEmpty(prefix) || isEmpty(suffix)) {
            return template;
        }

        StringBuilder builder = new StringBuilder(template.length());
        int start = 0;

        while (start < template.length()) {
            int prefixIndex = template.indexOf(prefix, start);
            if (prefixIndex < 0) {
                builder.append(template, start, template.length());
                break;
            }

            int suffixIndex = template.indexOf(suffix, prefixIndex + prefix.length());
            if (suffixIndex < 0) {
                builder.append(template, start, template.length());
                break;
            }

            builder.append(template, start, prefixIndex);
            start = suffixIndex + suffix.length();
        }

        return builder.toString();
    }

    /**
     * 将字符串转换为大写。
     *
     * @param value 字符串
     * @return 大写字符串
     */
    public static String upperCase(String value) {
        return value == null ? null : value.toUpperCase(java.util.Locale.ROOT);
    }

    /**
     * 将字符串转换为小写。
     *
     * @param value 字符串
     * @return 小写字符串
     */
    public static String lowerCase(String value) {
        return value == null ? null : value.toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * 将字符串首字符转换为大写。
     *
     * @param value 字符串
     * @return 首字符大写后的字符串
     */
    public static String capitalize(String value) {
        if (isEmpty(value)) {
            return value;
        }

        int firstCodePoint = value.codePointAt(0);
        int upperCodePoint = Character.toTitleCase(firstCodePoint);
        if (firstCodePoint == upperCodePoint) {
            return value;
        }

        int firstCharCount = Character.charCount(firstCodePoint);
        return new StringBuilder(value.length())
                .appendCodePoint(upperCodePoint)
                .append(value.substring(firstCharCount))
                .toString();
    }

    /**
     * 将字符串首字符转换为小写。
     *
     * @param value 字符串
     * @return 首字符小写后的字符串
     */
    public static String uncapitalize(String value) {
        if (isEmpty(value)) {
            return value;
        }

        int firstCodePoint = value.codePointAt(0);
        int lowerCodePoint = Character.toLowerCase(firstCodePoint);
        if (firstCodePoint == lowerCodePoint) {
            return value;
        }

        int firstCharCount = Character.charCount(firstCodePoint);
        return new StringBuilder(value.length())
                .appendCodePoint(lowerCodePoint)
                .append(value.substring(firstCharCount))
                .toString();
    }

    /**
     * 反转字符串中字母大小写。
     *
     * @param value 字符串
     * @return 大小写反转后的字符串
     */
    public static String swapCase(String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (Character.isUpperCase(codePoint)) {
                builder.appendCodePoint(Character.toLowerCase(codePoint));
            } else if (Character.isLowerCase(codePoint)) {
                builder.appendCodePoint(Character.toUpperCase(codePoint));
            } else {
                builder.appendCodePoint(codePoint);
            }
            i += Character.charCount(codePoint);
        }

        return builder.toString();
    }

    /**
     * 将字符串首字符转换为大写。
     *
     * @param value 字符串
     * @return 首字符大写后的字符串
     */
    public static String firstCharUpper(String value) {
        return capitalize(value);
    }

    /**
     * 将字符串首字符转换为小写。
     *
     * @param value 字符串
     * @return 首字符小写后的字符串
     */
    public static String firstCharLower(String value) {
        return uncapitalize(value);
    }

    /**
     * 将字符串转换为标题格式。
     *
     * @param value 字符串
     * @return 标题格式字符串
     */
    public static String toTitleCase(String value) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            return value;
        }

        StringBuilder builder = new StringBuilder(value.length());
        boolean nextUpper = true;

        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (Character.isWhitespace(codePoint)) {
                builder.appendCodePoint(codePoint);
                nextUpper = true;
            } else if (nextUpper) {
                builder.appendCodePoint(Character.toTitleCase(codePoint));
                nextUpper = false;
            } else {
                builder.appendCodePoint(Character.toLowerCase(codePoint));
            }
            i += Character.charCount(codePoint);
        }

        return builder.toString();
    }

    /**
     * 将字符串转换为小驼峰命名。
     *
     * @param value 字符串
     * @return 小驼峰命名字符串
     */
    public static String toCamelCase(String value) {
        java.util.List<String> words = splitToWords(value);
        if (words.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        builder.append(words.getFirst().toLowerCase(java.util.Locale.ROOT));

        for (int i = 1; i < words.size(); i++) {
            builder.append(capitalize(words.get(i).toLowerCase(java.util.Locale.ROOT)));
        }

        return builder.toString();
    }

    /**
     * 将字符串转换为大驼峰命名。
     *
     * @param value 字符串
     * @return 大驼峰命名字符串
     */
    public static String toPascalCase(String value) {
        java.util.List<String> words = splitToWords(value);
        if (words.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (String word : words) {
            builder.append(capitalize(word.toLowerCase(java.util.Locale.ROOT)));
        }

        return builder.toString();
    }

    /**
     * 将字符串转换为下划线命名。
     *
     * @param value 字符串
     * @return 下划线命名字符串
     */
    public static String toSnakeCase(String value) {
        return joinWords(splitToWords(value), "_", false);
    }

    /**
     * 将字符串转换为短横线命名。
     *
     * @param value 字符串
     * @return 短横线命名字符串
     */
    public static String toKebabCase(String value) {
        return joinWords(splitToWords(value), "-", false);
    }

    /**
     * 将驼峰命名转换为下划线命名。
     *
     * @param value 字符串
     * @return 下划线命名字符串
     */
    public static String camelToSnake(String value) {
        return toSnakeCase(value);
    }

    /**
     * 将下划线命名转换为小驼峰命名。
     *
     * @param value 字符串
     * @return 小驼峰命名字符串
     */
    public static String snakeToCamel(String value) {
        return toCamelCase(value);
    }

    /**
     * 将驼峰命名转换为短横线命名。
     *
     * @param value 字符串
     * @return 短横线命名字符串
     */
    public static String camelToKebab(String value) {
        return toKebabCase(value);
    }

    /**
     * 将短横线命名转换为小驼峰命名。
     *
     * @param value 字符串
     * @return 小驼峰命名字符串
     */
    public static String kebabToCamel(String value) {
        return toCamelCase(value);
    }

    /**
     * 重复字符串。
     *
     * @param value 字符串
     * @param count 重复次数
     * @return 重复后的字符串
     */
    public static String repeat(String value, int count) {
        if (value == null) {
            return null;
        }
        if (count <= 0 || value.isEmpty()) {
            return "";
        }
        return value.repeat(count);
    }

    /**
     * 左侧填充字符串。
     *
     * @param value  字符串
     * @param length 目标长度
     * @param pad    填充字符串
     * @return 填充后的字符串
     */
    public static String leftPad(String value, int length, String pad) {
        String safeValue = nullToEmpty(value);
        if (safeValue.length() >= length) {
            return safeValue;
        }

        String safePad = isEmpty(pad) ? " " : pad;
        return buildPadding(length - safeValue.length(), safePad) + safeValue;
    }

    /**
     * 右侧填充字符串。
     *
     * @param value  字符串
     * @param length 目标长度
     * @param pad    填充字符串
     * @return 填充后的字符串
     */
    public static String rightPad(String value, int length, String pad) {
        String safeValue = nullToEmpty(value);
        if (safeValue.length() >= length) {
            return safeValue;
        }

        String safePad = isEmpty(pad) ? " " : pad;
        return safeValue + buildPadding(length - safeValue.length(), safePad);
    }

    /**
     * 居中填充字符串。
     *
     * @param value  字符串
     * @param length 目标长度
     * @param pad    填充字符串
     * @return 填充后的字符串
     */
    public static String center(String value, int length, String pad) {
        String safeValue = nullToEmpty(value);
        if (safeValue.length() >= length) {
            return safeValue;
        }

        String safePad = isEmpty(pad) ? " " : pad;
        int totalPadLength = length - safeValue.length();
        int leftPadLength = totalPadLength / 2;
        int rightPadLength = totalPadLength - leftPadLength;

        return buildPadding(leftPadLength, safePad) + safeValue + buildPadding(rightPadLength, safePad);
    }

    /**
     * 字符串开头填充。
     *
     * @param value  字符串
     * @param length 目标长度
     * @param pad    填充字符串
     * @return 填充后的字符串
     */
    public static String padStart(String value, int length, String pad) {
        return leftPad(value, length, pad);
    }

    /**
     * 字符串结尾填充。
     *
     * @param value  字符串
     * @param length 目标长度
     * @param pad    填充字符串
     * @return 填充后的字符串
     */
    public static String padEnd(String value, int length, String pad) {
        return rightPad(value, length, pad);
    }

    /**
     * 左侧补零。
     *
     * @param value  字符串
     * @param length 目标长度
     * @return 补零后的字符串
     */
    public static String zeroPad(String value, int length) {
        return leftPad(value, length, "0");
    }

    /**
     * 固定字符串长度，超出则裁剪，不足则右侧补空格。
     *
     * @param value  字符串
     * @param length 目标长度
     * @return 固定长度字符串
     */
    public static String fixedLength(String value, int length) {
        if (length <= 0) {
            return "";
        }

        String safeValue = nullToEmpty(value);
        if (safeValue.length() > length) {
            return safeValue.substring(0, length);
        }

        return rightPad(safeValue, length, " ");
    }

    /**
     * 左对齐字符串，右侧补空格。
     *
     * @param value  字符串
     * @param length 目标长度
     * @return 左对齐字符串
     */
    public static String alignLeft(String value, int length) {
        return rightPad(value, length, " ");
    }

    /**
     * 右对齐字符串，左侧补空格。
     *
     * @param value  字符串
     * @param length 目标长度
     * @return 右对齐字符串
     */
    public static String alignRight(String value, int length) {
        return leftPad(value, length, " ");
    }

    /**
     * 将字符串拆分为命名单词。
     *
     * @param value 字符串
     * @return 单词列表
     */
    private static java.util.List<String> splitToWords(String value) {
        java.util.List<String> words = new java.util.ArrayList<>();
        if (isBlank(value)) {
            return words;
        }

        StringBuilder current = new StringBuilder();

        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);

            if (isWordSeparator(codePoint)) {
                addWord(words, current);
                i += Character.charCount(codePoint);
                continue;
            }

            if (current.length() > 0 && isCamelBoundary(current, codePoint, value, i)) {
                addWord(words, current);
            }

            current.appendCodePoint(codePoint);
            i += Character.charCount(codePoint);
        }

        addWord(words, current);
        return words;
    }

    /**
     * 判断是否为单词分隔符。
     *
     * @param codePoint Unicode 码点
     * @return 是否为单词分隔符
     */
    private static boolean isWordSeparator(int codePoint) {
        return codePoint == '_'
                || codePoint == '-'
                || codePoint == '.'
                || codePoint == '/'
                || codePoint == '\\'
                || Character.isWhitespace(codePoint);
    }

    /**
     * 判断当前位置是否为驼峰边界。
     *
     * @param current   当前单词
     * @param codePoint 当前码点
     * @param value     原始字符串
     * @param index     当前下标
     * @return 是否为驼峰边界
     */
    private static boolean isCamelBoundary(StringBuilder current, int codePoint, String value, int index) {
        int previousCodePoint = current.codePointBefore(current.length());

        if (Character.isLowerCase(previousCodePoint) && Character.isUpperCase(codePoint)) {
            return true;
        }

        if (Character.isLetter(previousCodePoint) && Character.isDigit(codePoint)) {
            return true;
        }

        if (Character.isDigit(previousCodePoint) && Character.isLetter(codePoint)) {
            return true;
        }

        if (Character.isUpperCase(previousCodePoint) && Character.isUpperCase(codePoint)) {
            int nextIndex = index + Character.charCount(codePoint);
            if (nextIndex < value.length()) {
                int nextCodePoint = value.codePointAt(nextIndex);
                return Character.isLowerCase(nextCodePoint);
            }
        }

        return false;
    }

    /**
     * 添加命名单词。
     *
     * @param words   单词列表
     * @param current 当前单词
     */
    private static void addWord(java.util.List<String> words, StringBuilder current) {
        if (!current.isEmpty()) {
            words.add(current.toString());
            current.setLength(0);
        }
    }

    /**
     * 拼接命名单词。
     *
     * @param words       单词列表
     * @param delimiter   分隔符
     * @param preserveRaw 是否保留原始大小写
     * @return 拼接后的字符串
     */
    private static String joinWords(java.util.List<String> words, String delimiter, boolean preserveRaw) {
        if (words == null || words.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (!builder.isEmpty()) {
                builder.append(delimiter);
            }
            builder.append(preserveRaw ? word : word.toLowerCase(java.util.Locale.ROOT));
        }

        return builder.toString();
    }

    /**
     * 构建指定长度的填充字符串。
     *
     * @param length 填充长度
     * @param pad    填充字符串
     * @return 填充字符串
     */
    private static String buildPadding(int length, String pad) {
        if (length <= 0) {
            return "";
        }

        String safePad = isEmpty(pad) ? " " : pad;
        StringBuilder builder = new StringBuilder(length);

        while (builder.length() < length) {
            builder.append(safePad);
        }

        if (builder.length() > length) {
            builder.setLength(length);
        }

        return builder.toString();
    }

    private static final String REGEX_INTEGER = "^[+-]?\\d+$";

    private static final String REGEX_DECIMAL = "^[+-]?(?:\\d+\\.\\d+|\\d+\\.|\\.\\d+)$";

    private static final String REGEX_NUMBER = "^[+-]?(?:\\d+|\\d+\\.\\d+|\\d+\\.|\\.\\d+)$";

    private static final String REGEX_ALPHA = "^[A-Za-z]+$";

    private static final String REGEX_ALPHA_NUMERIC = "^[A-Za-z0-9]+$";

    private static final String REGEX_EMAIL = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$";

    private static final String REGEX_MOBILE = "^1[3-9]\\d{9}$";

    private static final String REGEX_PHONE = "^(?:\\d{3,4}-?)?\\d{7,8}(?:-\\d{1,6})?$";

    private static final String REGEX_POST_CODE = "^\\d{6}$";

    private static final String REGEX_UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";

    private static final String REGEX_IPV4 = "^(?:(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)$";

    private static final int[] ID_CARD_WEIGHT = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

    private static final char[] ID_CARD_CHECK_CODE = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    /**
     * 判断字符串是否完整匹配正则表达式。
     *
     * @param value 字符串
     * @param regex 正则表达式
     * @return 是否匹配
     */
    public static boolean matches(String value, String regex) {
        if (value == null || isEmpty(regex)) {
            return false;
        }
        return java.util.regex.Pattern.matches(regex, value);
    }

    /**
     * 判断字符串中是否存在匹配正则表达式的内容。
     *
     * @param value 字符串
     * @param regex 正则表达式
     * @return 是否存在匹配内容
     */
    public static boolean find(String value, String regex) {
        if (value == null || isEmpty(regex)) {
            return false;
        }
        return java.util.regex.Pattern.compile(regex).matcher(value).find();
    }

    /**
     * 查找字符串中所有匹配正则表达式的内容。
     *
     * @param value 字符串
     * @param regex 正则表达式
     * @return 匹配内容列表
     */
    public static java.util.List<String> findAll(String value, String regex) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (value == null || isEmpty(regex)) {
            return result;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(value);
        while (matcher.find()) {
            result.add(matcher.group());
        }

        return result;
    }

    /**
     * 使用正则表达式替换字符串内容。
     *
     * @param value       字符串
     * @param regex       正则表达式
     * @param replacement 替换内容
     * @return 替换后的字符串
     */
    public static String replaceByRegex(String value, String regex, String replacement) {
        if (value == null || isEmpty(regex)) {
            return value;
        }
        return java.util.regex.Pattern.compile(regex).matcher(value).replaceAll(nullToEmpty(replacement));
    }

    /**
     * 使用正则表达式删除字符串内容。
     *
     * @param value 字符串
     * @param regex 正则表达式
     * @return 删除后的字符串
     */
    public static String removeByRegex(String value, String regex) {
        return replaceByRegex(value, regex, "");
    }

    /**
     * 使用正则表达式分割字符串。
     *
     * @param value 字符串
     * @param regex 正则表达式
     * @return 分割后的数组
     */
    public static String[] splitByRegex(String value, String regex) {
        if (value == null) {
            return new String[0];
        }
        if (isEmpty(regex)) {
            return new String[]{value};
        }
        return java.util.regex.Pattern.compile(regex).split(value, -1);
    }

    /**
     * 判断字符串是否完整匹配正则表达式。
     *
     * @param value 字符串
     * @param regex 正则表达式
     * @return 是否匹配
     */
    public static boolean isRegexMatch(String value, String regex) {
        return matches(value, regex);
    }

    /**
     * 获取第一个正则匹配内容。
     *
     * @param value 字符串
     * @param regex 正则表达式
     * @return 第一个匹配内容
     */
    public static String getFirstMatch(String value, String regex) {
        if (value == null || isEmpty(regex)) {
            return null;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(value);
        return matcher.find() ? matcher.group() : null;
    }

    /**
     * 获取第一个正则匹配分组内容。
     *
     * @param value      字符串
     * @param regex      正则表达式
     * @param groupIndex 分组下标
     * @return 分组内容
     */
    public static String getMatchGroup(String value, String regex, int groupIndex) {
        if (value == null || isEmpty(regex) || groupIndex < 0) {
            return null;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(value);
        if (!matcher.find() || groupIndex > matcher.groupCount()) {
            return null;
        }

        return matcher.group(groupIndex);
    }

    /**
     * 判断字符串是否为数字。
     *
     * @param value 字符串
     * @return 是否为数字
     */
    public static boolean isNumeric(String value) {
        return isNotBlank(value) && matches(value, REGEX_NUMBER);
    }

    /**
     * 判断字符串是否为整数。
     *
     * @param value 字符串
     * @return 是否为整数
     */
    public static boolean isInteger(String value) {
        return isNotBlank(value) && matches(value, REGEX_INTEGER);
    }

    /**
     * 判断字符串是否为 Long 范围内的整数。
     *
     * @param value 字符串
     * @return 是否为 Long
     */
    public static boolean isLong(String value) {
        if (!isInteger(value)) {
            return false;
        }
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为小数。
     *
     * @param value 字符串
     * @return 是否为小数
     */
    public static boolean isDecimal(String value) {
        return isNotBlank(value) && matches(value, REGEX_DECIMAL);
    }

    /**
     * 判断字符串是否为正数。
     *
     * @param value 字符串
     * @return 是否为正数
     */
    public static boolean isPositiveNumber(String value) {
        if (!isNumeric(value)) {
            return false;
        }
        return new java.math.BigDecimal(value).compareTo(java.math.BigDecimal.ZERO) > 0;
    }

    /**
     * 判断字符串是否为负数。
     *
     * @param value 字符串
     * @return 是否为负数
     */
    public static boolean isNegativeNumber(String value) {
        if (!isNumeric(value)) {
            return false;
        }
        return new java.math.BigDecimal(value).compareTo(java.math.BigDecimal.ZERO) < 0;
    }

    /**
     * 判断字符串是否只包含英文字母。
     *
     * @param value 字符串
     * @return 是否只包含英文字母
     */
    public static boolean isAlpha(String value) {
        return isNotEmpty(value) && matches(value, REGEX_ALPHA);
    }

    /**
     * 判断字符串是否只包含英文字母和数字。
     *
     * @param value 字符串
     * @return 是否只包含英文字母和数字
     */
    public static boolean isAlphaNumeric(String value) {
        return isNotEmpty(value) && matches(value, REGEX_ALPHA_NUMERIC);
    }

    /**
     * 判断字符串是否只包含 ASCII 字符。
     *
     * @param value 字符串
     * @return 是否只包含 ASCII 字符
     */
    public static boolean isAscii(String value) {
        if (value == null) {
            return false;
        }

        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) > 127) {
                return false;
            }
        }

        return true;
    }

    /**
     * 判断字符串中的字母是否全部为小写。
     *
     * @param value 字符串
     * @return 是否全部为小写
     */
    public static boolean isLowerCase(String value) {
        if (isEmpty(value)) {
            return false;
        }

        boolean hasLetter = false;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (Character.isLetter(codePoint)) {
                hasLetter = true;
                if (!Character.isLowerCase(codePoint)) {
                    return false;
                }
            }
            i += Character.charCount(codePoint);
        }

        return hasLetter;
    }

    /**
     * 判断字符串中的字母是否全部为大写。
     *
     * @param value 字符串
     * @return 是否全部为大写
     */
    public static boolean isUpperCase(String value) {
        if (isEmpty(value)) {
            return false;
        }

        boolean hasLetter = false;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (Character.isLetter(codePoint)) {
                hasLetter = true;
                if (!Character.isUpperCase(codePoint)) {
                    return false;
                }
            }
            i += Character.charCount(codePoint);
        }

        return hasLetter;
    }

    /**
     * 判断字符串是否为邮箱。
     *
     * @param value 字符串
     * @return 是否为邮箱
     */
    public static boolean isEmail(String value) {
        return isNotBlank(value) && value.length() <= 254 && matches(value, REGEX_EMAIL);
    }

    /**
     * 判断字符串是否为中国大陆手机号。
     *
     * @param value 字符串
     * @return 是否为手机号
     */
    public static boolean isMobile(String value) {
        return isNotBlank(value) && matches(value, REGEX_MOBILE);
    }

    /**
     * 判断字符串是否为固定电话。
     *
     * @param value 字符串
     * @return 是否为固定电话
     */
    public static boolean isPhone(String value) {
        return isNotBlank(value) && matches(value, REGEX_PHONE);
    }

    /**
     * 判断字符串是否为 URL。
     *
     * @param value 字符串
     * @return 是否为 URL
     */
    public static boolean isUrl(String value) {
        if (isBlank(value)) {
            return false;
        }

        try {
            java.net.URI uri = java.net.URI.create(value);
            return isNotBlank(uri.getScheme()) && isNotBlank(uri.getHost());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为 HTTP 或 HTTPS URL。
     *
     * @param value 字符串
     * @return 是否为 HTTP 或 HTTPS URL
     */
    public static boolean isHttpUrl(String value) {
        if (!isUrl(value)) {
            return false;
        }

        String scheme = java.net.URI.create(value).getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    /**
     * 判断字符串是否为 IPv4 地址。
     *
     * @param value 字符串
     * @return 是否为 IPv4 地址
     */
    public static boolean isIpv4(String value) {
        return isNotBlank(value) && matches(value, REGEX_IPV4);
    }

    /**
     * 判断字符串是否为 IPv6 地址。
     *
     * @param value 字符串
     * @return 是否为 IPv6 地址
     */
    public static boolean isIpv6(String value) {
        if (isBlank(value)) {
            return false;
        }

        String candidate = value;
        if (startsWith(candidate, "[") && endsWith(candidate, "]")) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }

        if (!candidate.contains(":")) {
            return false;
        }

        try {
            java.net.InetAddress address = java.net.InetAddress.getByName(candidate);
            return address instanceof java.net.Inet6Address;
        } catch (java.net.UnknownHostException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为中国大陆居民身份证号。
     *
     * @param value 字符串
     * @return 是否为身份证号
     */
    public static boolean isIdCard(String value) {
        if (isBlank(value)) {
            return false;
        }

        String idCard = value.trim().toUpperCase(java.util.Locale.ROOT);
        if (!matches(idCard, "^\\d{17}[0-9X]$")) {
            return false;
        }

        String birthday = idCard.substring(6, 14);
        if (!isValidDate(birthday, "yyyyMMdd")) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += Character.digit(idCard.charAt(i), 10) * ID_CARD_WEIGHT[i];
        }

        char expected = ID_CARD_CHECK_CODE[sum % 11];
        return expected == idCard.charAt(17);
    }

    /**
     * 判断字符串是否为银行卡号。
     *
     * @param value 字符串
     * @return 是否为银行卡号
     */
    public static boolean isBankCard(String value) {
        if (isBlank(value)) {
            return false;
        }

        String cardNo = removeAllWhitespace(value);
        if (!matches(cardNo, "^\\d{12,30}$")) {
            return false;
        }

        int sum = 0;
        boolean doubleDigit = false;

        for (int i = cardNo.length() - 1; i >= 0; i--) {
            int digit = cardNo.charAt(i) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return sum % 10 == 0;
    }

    /**
     * 判断字符串是否为中国大陆邮政编码。
     *
     * @param value 字符串
     * @return 是否为邮政编码
     */
    public static boolean isPostCode(String value) {
        return isNotBlank(value) && matches(value, REGEX_POST_CODE);
    }

    /**
     * 判断字符串是否为 UUID。
     *
     * @param value 字符串
     * @return 是否为 UUID
     */
    public static boolean isUuid(String value) {
        return isNotBlank(value) && matches(value, REGEX_UUID);
    }

    /**
     * 判断字符串是否为有效日期。
     *
     * @param value   日期字符串
     * @param pattern 日期格式
     * @return 是否为有效日期
     */
    private static boolean isValidDate(String value, String pattern) {
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                    .ofPattern(pattern)
                    .withResolverStyle(java.time.format.ResolverStyle.STRICT);
            java.time.LocalDate.parse(value, formatter);
            return true;
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }

    /**
     * 按指定范围脱敏字符串。
     *
     * @param value 字符串
     * @param start 开始位置，包含
     * @param end   结束位置，不包含
     * @return 脱敏后的字符串
     */
    public static String mask(String value, int start, int end) {
        return mask(value, start, end, "*");
    }

    /**
     * 按指定范围脱敏字符串。
     *
     * @param value 字符串
     * @param start 开始位置，包含
     * @param end   结束位置，不包含
     * @param mask  脱敏字符
     * @return 脱敏后的字符串
     */
    public static String mask(String value, int start, int end, String mask) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }

        int length = value.length();
        int safeStart = Math.max(start, 0);
        int safeEnd = Math.min(end, length);

        if (safeStart >= safeEnd) {
            return value;
        }

        String safeMask = isEmpty(mask) ? "*" : mask;
        return value.substring(0, safeStart)
                + buildMask(safeEnd - safeStart, safeMask)
                + value.substring(safeEnd);
    }

    /**
     * 从左侧开始脱敏指定长度。
     *
     * @param value      字符串
     * @param maskLength 脱敏长度
     * @return 脱敏后的字符串
     */
    public static String maskLeft(String value, int maskLength) {
        if (value == null) {
            return null;
        }
        return mask(value, 0, Math.min(maskLength, value.length()));
    }

    /**
     * 从右侧开始脱敏指定长度。
     *
     * @param value      字符串
     * @param maskLength 脱敏长度
     * @return 脱敏后的字符串
     */
    public static String maskRight(String value, int maskLength) {
        if (value == null) {
            return null;
        }
        int safeMaskLength = Math.max(maskLength, 0);
        return mask(value, Math.max(value.length() - safeMaskLength, 0), value.length());
    }

    /**
     * 保留左右两侧指定长度，中间部分脱敏。
     *
     * @param value       字符串
     * @param leftLength  左侧保留长度
     * @param rightLength 右侧保留长度
     * @return 脱敏后的字符串
     */
    public static String maskMiddle(String value, int leftLength, int rightLength) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }

        int length = value.length();
        int safeLeftLength = Math.max(leftLength, 0);
        int safeRightLength = Math.max(rightLength, 0);

        if (safeLeftLength + safeRightLength >= length) {
            return value;
        }

        return mask(value, safeLeftLength, length - safeRightLength);
    }

    /**
     * 脱敏手机号。
     *
     * @param value 手机号
     * @return 脱敏后的手机号
     */
    public static String maskMobile(String value) {
        if (isBlank(value)) {
            return value;
        }

        String mobile = value.trim();
        if (mobile.length() <= 7) {
            return maskMiddle(mobile, 3, 1);
        }

        return maskMiddle(mobile, 3, 4);
    }

    /**
     * 脱敏邮箱。
     *
     * @param value 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String value) {
        if (isBlank(value)) {
            return value;
        }

        String email = value.trim();
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return maskMiddle(email, 1, 1);
        }

        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        String maskedName = name.length() <= 2 ? maskRight(name, Math.max(name.length() - 1, 0)) : maskMiddle(name, 1, 1);

        return maskedName + domain;
    }

    /**
     * 脱敏身份证号。
     *
     * @param value 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String value) {
        if (isBlank(value)) {
            return value;
        }

        String idCard = value.trim();
        if (idCard.length() <= 8) {
            return maskMiddle(idCard, 2, 2);
        }

        return maskMiddle(idCard, 6, 4);
    }

    /**
     * 脱敏银行卡号。
     *
     * @param value 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String maskBankCard(String value) {
        if (isBlank(value)) {
            return value;
        }

        String cardNo = removeAllWhitespace(value);
        if (cardNo.length() <= 8) {
            return maskMiddle(cardNo, 2, 2);
        }

        return maskMiddle(cardNo, 4, 4);
    }

    /**
     * 脱敏姓名。
     *
     * @param value 姓名
     * @return 脱敏后的姓名
     */
    public static String maskName(String value) {
        if (isBlank(value)) {
            return value;
        }

        String name = value.trim();
        int codePointCount = name.codePointCount(0, name.length());

        if (codePointCount <= 1) {
            return name;
        }
        if (codePointCount == 2) {
            int firstEnd = name.offsetByCodePoints(0, 1);
            return name.substring(0, firstEnd) + "*";
        }

        int firstEnd = name.offsetByCodePoints(0, 1);
        int lastStart = name.offsetByCodePoints(0, codePointCount - 1);
        return name.substring(0, firstEnd) + buildMask(codePointCount - 2, "*") + name.substring(lastStart);
    }

    /**
     * 脱敏地址。
     *
     * @param value 地址
     * @return 脱敏后的地址
     */
    public static String maskAddress(String value) {
        if (isBlank(value)) {
            return value;
        }

        String address = value.trim();
        int codePointCount = address.codePointCount(0, address.length());

        if (codePointCount <= 6) {
            return maskMiddle(address, 2, 1);
        }

        int start = address.offsetByCodePoints(0, Math.min(6, codePointCount));
        return address.substring(0, start) + buildMask(Math.max(codePointCount - 6, 3), "*");
    }

    /**
     * 使用 UTF-8 编码获取字节数组。
     *
     * @param value 字符串
     * @return 字节数组
     */
    public static byte[] getBytes(String value) {
        return getBytes(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 使用指定字符集获取字节数组。
     *
     * @param value   字符串
     * @param charset 字符集
     * @return 字节数组
     */
    public static byte[] getBytes(String value, java.nio.charset.Charset charset) {
        if (value == null) {
            return new byte[0];
        }
        return value.getBytes(charset == null ? java.nio.charset.StandardCharsets.UTF_8 : charset);
    }

    /**
     * 使用 UTF-8 编码将字节数组转换为字符串。
     *
     * @param bytes 字节数组
     * @return 字符串
     */
    public static String newString(byte[] bytes) {
        return newString(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 使用指定字符集将字节数组转换为字符串。
     *
     * @param bytes   字节数组
     * @param charset 字符集
     * @return 字符串
     */
    public static String newString(byte[] bytes, java.nio.charset.Charset charset) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        return new String(bytes, charset == null ? java.nio.charset.StandardCharsets.UTF_8 : charset);
    }

    /**
     * 使用 UTF-8 编码进行 Base64 编码。
     *
     * @param value 字符串
     * @return Base64 字符串
     */
    public static String base64Encode(String value) {
        if (value == null) {
            return null;
        }
        return java.util.Base64.getEncoder().encodeToString(getBytes(value));
    }

    /**
     * 对字节数组进行 Base64 编码。
     *
     * @param bytes 字节数组
     * @return Base64 字符串
     */
    public static String base64Encode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return java.util.Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 使用 UTF-8 编码进行 Base64 解码。
     *
     * @param value Base64 字符串
     * @return 解码后的字符串
     */
    public static String base64Decode(String value) {
        if (value == null) {
            return null;
        }
        return newString(java.util.Base64.getDecoder().decode(value));
    }

    /**
     * Base64 解码为字节数组。
     *
     * @param value Base64 字符串
     * @return 解码后的字节数组
     */
    public static byte[] base64DecodeToBytes(String value) {
        if (value == null) {
            return new byte[0];
        }
        return java.util.Base64.getDecoder().decode(value);
    }

    /**
     * 使用 UTF-8 进行 URL 编码。
     *
     * @param value 字符串
     * @return URL 编码后的字符串
     */
    public static String urlEncode(String value) {
        return urlEncode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 使用指定字符集进行 URL 编码。
     *
     * @param value   字符串
     * @param charset 字符集
     * @return URL 编码后的字符串
     */
    public static String urlEncode(String value, java.nio.charset.Charset charset) {
        if (value == null) {
            return null;
        }
        return java.net.URLEncoder.encode(value, charset == null ? java.nio.charset.StandardCharsets.UTF_8 : charset);
    }

    /**
     * 使用 UTF-8 进行 URL 解码。
     *
     * @param value 字符串
     * @return URL 解码后的字符串
     */
    public static String urlDecode(String value) {
        return urlDecode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 使用指定字符集进行 URL 解码。
     *
     * @param value   字符串
     * @param charset 字符集
     * @return URL 解码后的字符串
     */
    public static String urlDecode(String value, java.nio.charset.Charset charset) {
        if (value == null) {
            return null;
        }
        return java.net.URLDecoder.decode(value, charset == null ? java.nio.charset.StandardCharsets.UTF_8 : charset);
    }

    /**
     * HTML 转义。
     *
     * @param value 字符串
     * @return 转义后的字符串
     */
    public static String htmlEscape(String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '&' -> builder.append("&amp;");
                case '<' -> builder.append("&lt;");
                case '>' -> builder.append("&gt;");
                case '"' -> builder.append("&quot;");
                case '\'' -> builder.append("&#39;");
                case '/' -> builder.append("&#47;");
                default -> builder.append(current);
            }
        }

        return builder.toString();
    }

    /**
     * HTML 反转义。
     *
     * @param value 字符串
     * @return 反转义后的字符串
     */
    public static String htmlUnescape(String value) {
        if (value == null) {
            return null;
        }

        return value
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&#x27;", "'")
                .replace("&#47;", "/")
                .replace("&#x2F;", "/")
                .replace("&amp;", "&");
    }

    /**
     * Unicode 编码。
     *
     * @param value 字符串
     * @return Unicode 编码后的字符串
     */
    public static String unicodeEncode(String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length() * 6);
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current <= 127) {
                builder.append(current);
            } else {
                builder.append("\\u");
                String hex = Integer.toHexString(current).toUpperCase(java.util.Locale.ROOT);
                builder.append("0".repeat(4 - hex.length())).append(hex);
            }
        }

        return builder.toString();
    }

    /**
     * Unicode 解码。
     *
     * @param value Unicode 编码字符串
     * @return 解码后的字符串
     */
    public static String unicodeDecode(String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); ) {
            if (i + 5 < value.length()
                    && value.charAt(i) == '\\'
                    && value.charAt(i + 1) == 'u'
                    && isHex(value, i + 2, i + 6)) {
                String hex = value.substring(i + 2, i + 6);
                builder.append((char) Integer.parseInt(hex, 16));
                i += 6;
            } else {
                builder.append(value.charAt(i));
                i++;
            }
        }

        return builder.toString();
    }

    /**
     * 获取字符串长度。
     *
     * @param value 字符串
     * @return 字符串长度
     */
    public static int length(CharSequence value) {
        return value == null ? 0 : value.length();
    }

    /**
     * 获取 UTF-8 字节长度。
     *
     * @param value 字符串
     * @return 字节长度
     */
    public static int byteLength(String value) {
        return byteLength(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 获取指定字符集字节长度。
     *
     * @param value   字符串
     * @param charset 字符集
     * @return 字节长度
     */
    public static int byteLength(String value, java.nio.charset.Charset charset) {
        if (value == null) {
            return 0;
        }
        return value.getBytes(charset == null ? java.nio.charset.StandardCharsets.UTF_8 : charset).length;
    }

    /**
     * 获取 Unicode 码点数量。
     *
     * @param value 字符串
     * @return Unicode 码点数量
     */
    public static int charLength(String value) {
        return value == null ? 0 : value.codePointCount(0, value.length());
    }

    /**
     * 统计指定字符出现次数。
     *
     * @param value  字符串
     * @param target 目标字符
     * @return 出现次数
     */
    public static int countChars(String value, char target) {
        if (value == null || value.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == target) {
                count++;
            }
        }

        return count;
    }

    /**
     * 统计中文字符数量。
     *
     * @param value 字符串
     * @return 中文字符数量
     */
    public static int countChinese(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (isChineseCodePoint(codePoint)) {
                count++;
            }
            i += Character.charCount(codePoint);
        }

        return count;
    }

    /**
     * 统计字母数量。
     *
     * @param value 字符串
     * @return 字母数量
     */
    public static int countLetters(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (Character.isLetter(codePoint)) {
                count++;
            }
            i += Character.charCount(codePoint);
        }

        return count;
    }

    /**
     * 统计数字数量。
     *
     * @param value 字符串
     * @return 数字数量
     */
    public static int countDigits(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (Character.isDigit(codePoint)) {
                count++;
            }
            i += Character.charCount(codePoint);
        }

        return count;
    }

    /**
     * 统计大写字母数量。
     *
     * @param value 字符串
     * @return 大写字母数量
     */
    public static int countUpperCase(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (Character.isUpperCase(codePoint)) {
                count++;
            }
            i += Character.charCount(codePoint);
        }

        return count;
    }

    /**
     * 统计小写字母数量。
     *
     * @param value 字符串
     * @return 小写字母数量
     */
    public static int countLowerCase(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (Character.isLowerCase(codePoint)) {
                count++;
            }
            i += Character.charCount(codePoint);
        }

        return count;
    }

    /**
     * 统计文本行数。
     *
     * @param value 字符串
     * @return 行数
     */
    public static int countLines(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        return value.split("\\R", -1).length;
    }

    /**
     * 构建脱敏字符串。
     *
     * @param length 脱敏长度
     * @param mask   脱敏字符
     * @return 脱敏字符串
     */
    private static String buildMask(int length, String mask) {
        if (length <= 0) {
            return "";
        }
        String safeMask = isEmpty(mask) ? "*" : mask;
        return buildPadding(length, safeMask);
    }

    /**
     * 判断指定范围是否为十六进制字符。
     *
     * @param value 字符串
     * @param start 开始位置，包含
     * @param end   结束位置，不包含
     * @return 是否为十六进制字符
     */
    private static boolean isHex(String value, int start, int end) {
        if (value == null || start < 0 || end > value.length() || start >= end) {
            return false;
        }

        for (int i = start; i < end; i++) {
            char current = value.charAt(i);
            boolean hex = (current >= '0' && current <= '9')
                    || (current >= 'a' && current <= 'f')
                    || (current >= 'A' && current <= 'F');
            if (!hex) {
                return false;
            }
        }

        return true;
    }

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    private static final String RANDOM_NUMERIC_CHARS = "0123456789";

    private static final String RANDOM_ALPHABETIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final String RANDOM_ALPHA_NUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final String RANDOM_UPPER_CASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String RANDOM_LOWER_CASE_CHARS = "abcdefghijklmnopqrstuvwxyz";

    /**
     * 仅保留数字字符。
     *
     * @param value 字符串
     * @return 过滤后的字符串
     */
    public static String retainDigits(String value) {
        return filterByPredicate(value, Character::isDigit);
    }

    /**
     * 仅保留字母字符。
     *
     * @param value 字符串
     * @return 过滤后的字符串
     */
    public static String retainLetters(String value) {
        return filterByPredicate(value, Character::isLetter);
    }

    /**
     * 仅保留字母和数字字符。
     *
     * @param value 字符串
     * @return 过滤后的字符串
     */
    public static String retainAlphaNumeric(String value) {
        return filterByPredicate(value, Character::isLetterOrDigit);
    }

    /**
     * 移除所有数字字符。
     *
     * @param value 字符串
     * @return 过滤后的字符串
     */
    public static String removeDigits(String value) {
        if (value == null) {
            return null;
        }

        return filterByPredicate(value, codePoint -> !Character.isDigit(codePoint));
    }

    /**
     * 移除所有字母字符。
     *
     * @param value 字符串
     * @return 过滤后的字符串
     */
    public static String removeLetters(String value) {
        if (value == null) {
            return null;
        }

        return filterByPredicate(value, codePoint -> !Character.isLetter(codePoint));
    }

    /**
     * 移除特殊字符，仅保留字母和数字。
     *
     * @param value 字符串
     * @return 过滤后的字符串
     */
    public static String removeSpecialChars(String value) {
        return retainAlphaNumeric(value);
    }

    /**
     * 按码点过滤字符串。
     *
     * @param value     字符串
     * @param predicate 过滤条件
     * @return 过滤后的字符串
     */
    public static String filterByPredicate(String value, java.util.function.IntPredicate predicate) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }
        if (predicate == null) {
            return value;
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (predicate.test(codePoint)) {
                builder.appendCodePoint(codePoint);
            }
            i += Character.charCount(codePoint);
        }

        return builder.toString();
    }

    /**
     * 移除常见 Emoji 字符。
     *
     * @param value 字符串
     * @return 移除 Emoji 后的字符串
     */
    public static String removeEmoji(String value) {
        if (value == null) {
            return null;
        }

        return filterByPredicate(value, codePoint -> !isEmojiCodePoint(codePoint));
    }

    /**
     * 生成随机数字字符串。
     *
     * @param length 长度
     * @return 随机字符串
     */
    public static String randomNumeric(int length) {
        return randomString(length, RANDOM_NUMERIC_CHARS);
    }

    /**
     * 生成随机字母字符串。
     *
     * @param length 长度
     * @return 随机字符串
     */
    public static String randomAlphabetic(int length) {
        return randomString(length, RANDOM_ALPHABETIC_CHARS);
    }

    /**
     * 生成随机字母数字字符串。
     *
     * @param length 长度
     * @return 随机字符串
     */
    public static String randomAlphaNumeric(int length) {
        return randomString(length, RANDOM_ALPHA_NUMERIC_CHARS);
    }

    /**
     * 从指定候选字符中生成随机字符串。
     *
     * @param length         长度
     * @param candidateChars 候选字符
     * @return 随机字符串
     */
    public static String randomString(int length, String candidateChars) {
        if (length <= 0) {
            return "";
        }
        if (isEmpty(candidateChars)) {
            throw new IllegalArgumentException("候选字符不能为空");
        }

        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(candidateChars.charAt(SECURE_RANDOM.nextInt(candidateChars.length())));
        }

        return builder.toString();
    }

    /**
     * 生成随机大写字母字符串。
     *
     * @param length 长度
     * @return 随机字符串
     */
    public static String randomUpperCase(int length) {
        return randomString(length, RANDOM_UPPER_CASE_CHARS);
    }

    /**
     * 生成随机小写字母字符串。
     *
     * @param length 长度
     * @return 随机字符串
     */
    public static String randomLowerCase(int length) {
        return randomString(length, RANDOM_LOWER_CASE_CHARS);
    }

    /**
     * 生成随机数字验证码。
     *
     * @param length 长度
     * @return 随机验证码
     */
    public static String randomCode(int length) {
        return randomNumeric(length);
    }

    /**
     * 生成随机 UUID 字符串。
     *
     * @return UUID 字符串
     */
    public static String randomUuid() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * 缩略字符串，默认使用省略号。
     *
     * @param value    字符串
     * @param maxWidth 最大展示宽度
     * @return 缩略后的字符串
     */
    public static String abbreviate(String value, int maxWidth) {
        return abbreviate(value, maxWidth, "...");
    }

    /**
     * 缩略字符串。
     *
     * @param value    字符串
     * @param maxWidth 最大展示宽度
     * @param suffix   后缀
     * @return 缩略后的字符串
     */
    public static String abbreviate(String value, int maxWidth, String suffix) {
        if (value == null) {
            return null;
        }
        if (maxWidth <= 0) {
            return "";
        }

        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= maxWidth) {
            return value;
        }

        String safeSuffix = nullToEmpty(suffix);
        int suffixLength = safeSuffix.codePointCount(0, safeSuffix.length());
        if (suffixLength >= maxWidth) {
            return substringByCodePoints(safeSuffix, 0, maxWidth);
        }

        return substringByCodePoints(value, 0, maxWidth - suffixLength) + safeSuffix;
    }

    /**
     * 中间缩略字符串。
     *
     * @param value    字符串
     * @param maxWidth 最大展示宽度
     * @return 缩略后的字符串
     */
    public static String abbreviateMiddle(String value, int maxWidth) {
        return abbreviateMiddle(value, maxWidth, "...");
    }

    /**
     * 中间缩略字符串。
     *
     * @param value    字符串
     * @param maxWidth 最大展示宽度
     * @param middle   中间占位符
     * @return 缩略后的字符串
     */
    public static String abbreviateMiddle(String value, int maxWidth, String middle) {
        if (value == null) {
            return null;
        }
        if (maxWidth <= 0) {
            return "";
        }

        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= maxWidth) {
            return value;
        }

        String safeMiddle = nullToEmpty(middle);
        int middleLength = safeMiddle.codePointCount(0, safeMiddle.length());
        if (middleLength >= maxWidth) {
            return substringByCodePoints(safeMiddle, 0, maxWidth);
        }

        int remainLength = maxWidth - middleLength;
        int leftLength = remainLength / 2 + remainLength % 2;
        int rightLength = remainLength / 2;

        return substringByCodePoints(value, 0, leftLength)
                + safeMiddle
                + substringByCodePoints(value, codePointCount - rightLength, codePointCount);
    }

    /**
     * 使用省略号限制字符串展示长度。
     *
     * @param value    字符串
     * @param maxWidth 最大展示宽度
     * @return 处理后的字符串
     */
    public static String ellipsis(String value, int maxWidth) {
        return abbreviate(value, maxWidth, "...");
    }

    /**
     * 生成文本预览。
     *
     * @param value    字符串
     * @param maxWidth 最大展示宽度
     * @return 预览文本
     */
    public static String preview(String value, int maxWidth) {
        if (value == null) {
            return null;
        }

        String normalized = normalizeWhitespace(value);
        return abbreviate(normalized, maxWidth, "...");
    }

    /**
     * 限制字符串长度，超出直接截断。
     *
     * @param value    字符串
     * @param maxWidth 最大长度
     * @return 限制长度后的字符串
     */
    public static String limitLength(String value, int maxWidth) {
        if (value == null) {
            return null;
        }
        if (maxWidth <= 0) {
            return "";
        }

        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= maxWidth) {
            return value;
        }

        return substringByCodePoints(value, 0, maxWidth);
    }

    /**
     * 生成单行文本预览。
     *
     * @param value    字符串
     * @param maxWidth 最大展示宽度
     * @return 单行预览文本
     */
    public static String linePreview(String value, int maxWidth) {
        if (value == null) {
            return null;
        }

        String line = value.replaceAll("\\R+", " ");
        return preview(line, maxWidth);
    }

    /**
     * 使用相同包装字符包裹字符串。
     *
     * @param value 字符串
     * @param wrap  包装字符
     * @return 包裹后的字符串
     */
    public static String wrap(String value, String wrap) {
        return wrap(value, wrap, wrap);
    }

    /**
     * 使用指定前后缀包裹字符串。
     *
     * @param value  字符串
     * @param prefix 前缀
     * @param suffix 后缀
     * @return 包裹后的字符串
     */
    public static String wrap(String value, String prefix, String suffix) {
        if (value == null) {
            return null;
        }

        return nullToEmpty(prefix) + value + nullToEmpty(suffix);
    }

    /**
     * 移除相同包装字符。
     *
     * @param value 字符串
     * @param wrap  包装字符
     * @return 移除包装后的字符串
     */
    public static String unwrap(String value, String wrap) {
        return unwrap(value, wrap, wrap);
    }

    /**
     * 移除指定前后缀包装字符。
     *
     * @param value  字符串
     * @param prefix 前缀
     * @param suffix 后缀
     * @return 移除包装后的字符串
     */
    public static String unwrap(String value, String prefix, String suffix) {
        if (value == null) {
            return null;
        }

        String safePrefix = nullToEmpty(prefix);
        String safeSuffix = nullToEmpty(suffix);

        if (isEmpty(safePrefix) && isEmpty(safeSuffix)) {
            return value;
        }

        boolean matchPrefix = isEmpty(safePrefix) || startsWith(value, safePrefix);
        boolean matchSuffix = isEmpty(safeSuffix) || endsWith(value, safeSuffix);

        if (!matchPrefix || !matchSuffix) {
            return value;
        }

        int start = safePrefix.length();
        int end = value.length() - safeSuffix.length();

        if (start > end) {
            return value;
        }

        return value.substring(start, end);
    }

    /**
     * 按 Unicode 码点截取字符串。
     *
     * @param value          字符串
     * @param startCodePoint 开始码点位置，包含
     * @param endCodePoint   结束码点位置，不包含
     * @return 截取后的字符串
     */
    private static String substringByCodePoints(String value, int startCodePoint, int endCodePoint) {
        if (value == null) {
            return null;
        }

        int codePointCount = value.codePointCount(0, value.length());
        int safeStart = Math.max(startCodePoint, 0);
        int safeEnd = Math.min(endCodePoint, codePointCount);

        if (safeStart >= safeEnd) {
            return "";
        }

        int startIndex = value.offsetByCodePoints(0, safeStart);
        int endIndex = value.offsetByCodePoints(0, safeEnd);
        return value.substring(startIndex, endIndex);
    }

    /**
     * 归一化换行符为 \n。
     *
     * @param value 字符串
     * @return 归一化后的字符串
     */
    public static String normalizeLineSeparator(String value) {
        return normalizeLineSeparator(value, "\n");
    }

    /**
     * 归一化换行符为指定换行符。
     *
     * @param value         字符串
     * @param lineSeparator 换行符
     * @return 归一化后的字符串
     */
    public static String normalizeLineSeparator(String value, String lineSeparator) {
        if (value == null) {
            return null;
        }

        String safeLineSeparator = lineSeparator == null ? "\n" : lineSeparator;
        return value.replaceAll("\\R", java.util.regex.Matcher.quoteReplacement(safeLineSeparator));
    }

    /**
     * 移除空行，不移除仅包含空白字符的行。
     *
     * @param value 字符串
     * @return 移除空行后的字符串
     */
    public static String removeEmptyLines(String value) {
        return removeEmptyLines(value, "\n");
    }

    /**
     * 移除空行，不移除仅包含空白字符的行。
     *
     * @param value         字符串
     * @param lineSeparator 换行符
     * @return 移除空行后的字符串
     */
    public static String removeEmptyLines(String value, String lineSeparator) {
        if (value == null) {
            return null;
        }

        java.util.List<String> lines = splitLines(value);
        java.util.List<String> result = new java.util.ArrayList<>();

        for (String line : lines) {
            if (line != null && !line.isEmpty()) {
                result.add(line);
            }
        }

        return join(result, lineSeparator == null ? "\n" : lineSeparator);
    }

    /**
     * 移除空白行。
     *
     * @param value 字符串
     * @return 移除空白行后的字符串
     */
    public static String removeBlankLines(String value) {
        return removeBlankLines(value, "\n");
    }

    /**
     * 移除空白行。
     *
     * @param value         字符串
     * @param lineSeparator 换行符
     * @return 移除空白行后的字符串
     */
    public static String removeBlankLines(String value, String lineSeparator) {
        if (value == null) {
            return null;
        }

        java.util.List<String> lines = splitLines(value);
        java.util.List<String> result = new java.util.ArrayList<>();

        for (String line : lines) {
            if (isNotBlank(line)) {
                result.add(line);
            }
        }

        return join(result, lineSeparator == null ? "\n" : lineSeparator);
    }

    /**
     * 去除每一行前后的空白字符。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String trimLines(String value) {
        return trimLines(value, "\n");
    }

    /**
     * 去除每一行前后的空白字符。
     *
     * @param value         字符串
     * @param lineSeparator 换行符
     * @return 处理后的字符串
     */
    public static String trimLines(String value, String lineSeparator) {
        if (value == null) {
            return null;
        }

        java.util.List<String> lines = splitLines(value);
        java.util.List<String> result = new java.util.ArrayList<>(lines.size());

        for (String line : lines) {
            result.add(trimToEmpty(line));
        }

        return join(result, lineSeparator == null ? "\n" : lineSeparator);
    }

    /**
     * 为每一行添加缩进。
     *
     * @param value      字符串
     * @param indentText 缩进内容
     * @return 添加缩进后的字符串
     */
    public static String indentLines(String value, String indentText) {
        if (value == null) {
            return null;
        }

        return prefixLines(value, nullToEmpty(indentText));
    }

    /**
     * 为每一行添加前缀。
     *
     * @param value  字符串
     * @param prefix 前缀
     * @return 添加前缀后的字符串
     */
    public static String prefixLines(String value, String prefix) {
        return prefixLines(value, prefix, "\n");
    }

    /**
     * 为每一行添加前缀。
     *
     * @param value         字符串
     * @param prefix        前缀
     * @param lineSeparator 换行符
     * @return 添加前缀后的字符串
     */
    public static String prefixLines(String value, String prefix, String lineSeparator) {
        if (value == null) {
            return null;
        }

        java.util.List<String> lines = splitLines(value);
        java.util.List<String> result = new java.util.ArrayList<>(lines.size());
        String safePrefix = nullToEmpty(prefix);

        for (String line : lines) {
            result.add(safePrefix + line);
        }

        return join(result, lineSeparator == null ? "\n" : lineSeparator);
    }

    /**
     * 为每一行添加后缀。
     *
     * @param value  字符串
     * @param suffix 后缀
     * @return 添加后缀后的字符串
     */
    public static String suffixLines(String value, String suffix) {
        return suffixLines(value, suffix, "\n");
    }

    /**
     * 为每一行添加后缀。
     *
     * @param value         字符串
     * @param suffix        后缀
     * @param lineSeparator 换行符
     * @return 添加后缀后的字符串
     */
    public static String suffixLines(String value, String suffix, String lineSeparator) {
        if (value == null) {
            return null;
        }

        java.util.List<String> lines = splitLines(value);
        java.util.List<String> result = new java.util.ArrayList<>(lines.size());
        String safeSuffix = nullToEmpty(suffix);

        for (String line : lines) {
            result.add(line + safeSuffix);
        }

        return join(result, lineSeparator == null ? "\n" : lineSeparator);
    }

    /**
     * 统计文本行数。
     *
     * @param value 字符串
     * @return 行数
     */
    public static int lineCount(String value) {
        return countLines(value);
    }

    /**
     * 获取文件名。
     *
     * @param path 路径
     * @return 文件名
     */
    public static String getFileName(String path) {
        if (isBlank(path)) {
            return path;
        }

        String normalized = path.replace('\\', '/');
        String cleanPath = removeEndSlash(normalized);
        int index = cleanPath.lastIndexOf('/');

        return index < 0 ? cleanPath : cleanPath.substring(index + 1);
    }

    /**
     * 获取不带扩展名的文件名。
     *
     * @param path 路径
     * @return 不带扩展名的文件名
     */
    public static String getFileNameWithoutExtension(String path) {
        String fileName = getFileName(path);
        if (isBlank(fileName)) {
            return fileName;
        }

        int index = fileName.lastIndexOf('.');
        if (index <= 0) {
            return fileName;
        }

        return fileName.substring(0, index);
    }

    /**
     * 获取文件扩展名，不包含点号。
     *
     * @param path 路径
     * @return 文件扩展名
     */
    public static String getFileExtension(String path) {
        String fileName = getFileName(path);
        if (isBlank(fileName)) {
            return "";
        }

        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(index + 1);
    }

    /**
     * 修改文件扩展名。
     *
     * @param path         路径
     * @param newExtension 新扩展名
     * @return 修改后的路径
     */
    public static String changeExtension(String path, String newExtension) {
        if (path == null) {
            return null;
        }

        String safeExtension = nullToEmpty(newExtension);
        if (startsWith(safeExtension, ".")) {
            safeExtension = safeExtension.substring(1);
        }

        int slashIndex = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        int dotIndex = path.lastIndexOf('.');

        boolean hasExtension = dotIndex > slashIndex;
        String basePath = hasExtension ? path.substring(0, dotIndex) : path;

        return isEmpty(safeExtension) ? basePath : basePath + "." + safeExtension;
    }

    /**
     * 归一化路径分隔符并清理冗余路径片段。
     *
     * @param path 路径
     * @return 归一化后的路径
     */
    public static String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        if (path.isEmpty()) {
            return "";
        }

        String value = path.replace('\\', '/');
        String prefix = "";
        String remain = value;

        int schemeIndex = remain.indexOf("://");
        if (schemeIndex > 0) {
            prefix = remain.substring(0, schemeIndex + 3);
            remain = remain.substring(schemeIndex + 3);
        } else if (startsWith(remain, "//")) {
            prefix = "//";
            remain = remain.substring(2);
        } else if (remain.length() >= 2 && Character.isLetter(remain.charAt(0)) && remain.charAt(1) == ':') {
            prefix = remain.substring(0, 2);
            remain = remain.substring(2);
        }

        boolean absolute = startsWith(remain, "/");
        String[] segments = remain.split("/+");
        java.util.Deque<String> stack = new java.util.ArrayDeque<>();

        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }

            if ("..".equals(segment)) {
                if (!stack.isEmpty() && !"..".equals(stack.peekLast())) {
                    stack.removeLast();
                } else if (!absolute) {
                    stack.addLast(segment);
                }
            } else {
                stack.addLast(segment);
            }
        }

        String normalized = join(stack, "/");

        if (absolute) {
            normalized = "/" + normalized;
        }

        if (normalized.isEmpty() && absolute) {
            normalized = "/";
        }

        return prefix + normalized;
    }

    /**
     * 确保字符串以斜杠开头。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String ensureStartSlash(String value) {
        if (isEmpty(value)) {
            return "/";
        }
        return startsWith(value, "/") ? value : "/" + value;
    }

    /**
     * 确保字符串以斜杠结尾。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String ensureEndSlash(String value) {
        if (isEmpty(value)) {
            return "/";
        }
        return endsWith(value, "/") ? value : value + "/";
    }

    /**
     * 移除字符串结尾的斜杠。
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String removeEndSlash(String value) {
        if (value == null) {
            return null;
        }

        String result = value;
        while (result.length() > 1 && (result.endsWith("/") || result.endsWith("\\"))) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    /**
     * 转义 SQL LIKE 特殊字符。
     *
     * @param value 字符串
     * @return 转义后的字符串
     */
    public static String escapeSqlLike(String value) {
        return escapeSqlLike(value, '\\');
    }

    /**
     * 转义 SQL LIKE 特殊字符。
     *
     * @param value      字符串
     * @param escapeChar 转义字符
     * @return 转义后的字符串
     */
    public static String escapeSqlLike(String value, char escapeChar) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == escapeChar || current == '%' || current == '_') {
                builder.append(escapeChar);
            }
            builder.append(current);
        }

        return builder.toString();
    }

    /**
     * JSON 字符串转义。
     *
     * @param value 字符串
     * @return 转义后的字符串
     */
    public static String escapeJson(String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);

            switch (codePoint) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (codePoint < 0x20) {
                        appendUnicodeEscape(builder, codePoint);
                    } else {
                        builder.appendCodePoint(codePoint);
                    }
                }
            }

            i += Character.charCount(codePoint);
        }

        return builder.toString();
    }

    /**
     * JSON 字符串反转义。
     *
     * @param value 字符串
     * @return 反转义后的字符串
     */
    public static String unescapeJson(String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current != '\\' || i == value.length() - 1) {
                builder.append(current);
                continue;
            }

            char next = value.charAt(++i);
            switch (next) {
                case '"' -> builder.append('"');
                case '\\' -> builder.append('\\');
                case '/' -> builder.append('/');
                case 'b' -> builder.append('\b');
                case 'f' -> builder.append('\f');
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case 'u' -> {
                    if (i + 4 < value.length() && isHex(value, i + 1, i + 5)) {
                        String hex = value.substring(i + 1, i + 5);
                        builder.append((char) Integer.parseInt(hex, 16));
                        i += 4;
                    } else {
                        builder.append("\\u");
                    }
                }
                default -> builder.append(next);
            }
        }

        return builder.toString();
    }

    /**
     * 使用双引号包裹字符串。
     *
     * @param value 字符串
     * @return 包裹后的字符串
     */
    public static String quote(String value) {
        return doubleQuote(value);
    }

    /**
     * 移除字符串两端成对引号。
     *
     * @param value 字符串
     * @return 移除引号后的字符串
     */
    public static String unquote(String value) {
        if (value == null) {
            return null;
        }

        String result = value;
        if (result.length() >= 2) {
            char first = result.charAt(0);
            char last = result.charAt(result.length() - 1);

            if ((first == '"' && last == '"')
                    || (first == '\'' && last == '\'')
                    || (first == '`' && last == '`')) {
                return result.substring(1, result.length() - 1);
            }
        }

        return result;
    }

    /**
     * 使用单引号包裹字符串。
     *
     * @param value 字符串
     * @return 包裹后的字符串
     */
    public static String singleQuote(String value) {
        return "'" + nullToEmpty(value).replace("'", "\\'") + "'";
    }

    /**
     * 使用双引号包裹字符串。
     *
     * @param value 字符串
     * @return 包裹后的字符串
     */
    public static String doubleQuote(String value) {
        return "\"" + escapeJson(nullToEmpty(value)) + "\"";
    }

    /**
     * 移除字符串中的所有引号。
     *
     * @param value 字符串
     * @return 移除引号后的字符串
     */
    public static String removeQuotes(String value) {
        if (value == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current != '"' && current != '\'' && current != '`') {
                builder.append(current);
            }
        }

        return builder.toString();
    }

    /**
     * 追加 Unicode 转义内容。
     *
     * @param builder   字符串构建器
     * @param codePoint Unicode 码点
     */
    private static void appendUnicodeEscape(StringBuilder builder, int codePoint) {
        String hex = Integer.toHexString(codePoint).toUpperCase(java.util.Locale.ROOT);
        builder.append("\\u");
        builder.append("0".repeat(Math.max(0, 4 - hex.length())));
        builder.append(hex);
    }

    /**
     * 安全转换为字符串。
     *
     * @param value 对象
     * @return 字符串
     */
    public static String safeToString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 转换为字符串，null 返回 null。
     *
     * @param value 对象
     * @return 字符串
     */
    public static String toStringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 转换为字符串，null 返回空字符串。
     *
     * @param value 对象
     * @return 字符串
     */
    public static String toStringOrEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 安全解析 Integer。
     *
     * @param value 字符串
     * @return Integer 值，解析失败返回 null
     */
    public static Integer parseInt(String value) {
        return parseInt(value, null);
    }

    /**
     * 安全解析 Integer。
     *
     * @param value        字符串
     * @param defaultValue 默认值
     * @return Integer 值
     */
    public static Integer parseInt(String value, Integer defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }

        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 安全解析 Long。
     *
     * @param value 字符串
     * @return Long 值，解析失败返回 null
     */
    public static Long parseLong(String value) {
        return parseLong(value, null);
    }

    /**
     * 安全解析 Long。
     *
     * @param value        字符串
     * @param defaultValue 默认值
     * @return Long 值
     */
    public static Long parseLong(String value, Long defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }

        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 安全解析 Double。
     *
     * @param value 字符串
     * @return Double 值，解析失败返回 null
     */
    public static Double parseDouble(String value) {
        return parseDouble(value, null);
    }

    /**
     * 安全解析 Double。
     *
     * @param value        字符串
     * @param defaultValue 默认值
     * @return Double 值
     */
    public static Double parseDouble(String value, Double defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }

        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 安全解析 Boolean。
     *
     * @param value 字符串
     * @return Boolean 值，解析失败返回 null
     */
    public static Boolean parseBoolean(String value) {
        return parseBoolean(value, null);
    }

    /**
     * 安全解析 Boolean。
     *
     * @param value        字符串
     * @param defaultValue 默认值
     * @return Boolean 值
     */
    public static Boolean parseBoolean(String value, Boolean defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }

        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "true", "1", "yes", "y", "on" -> Boolean.TRUE;
            case "false", "0", "no", "n", "off" -> Boolean.FALSE;
            default -> defaultValue;
        };
    }

    /**
     * 安全解析 BigDecimal。
     *
     * @param value 字符串
     * @return BigDecimal 值，解析失败返回 null
     */
    public static java.math.BigDecimal parseBigDecimal(String value) {
        return parseBigDecimal(value, null);
    }

    /**
     * 安全解析 BigDecimal。
     *
     * @param value        字符串
     * @param defaultValue 默认值
     * @return BigDecimal 值
     */
    public static java.math.BigDecimal parseBigDecimal(String value, java.math.BigDecimal defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }

        try {
            return new java.math.BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 反转字符串。
     *
     * @param value 字符串
     * @return 反转后的字符串
     */
    public static String reverse(String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }

        int[] codePoints = value.codePoints().toArray();
        StringBuilder builder = new StringBuilder(value.length());

        for (int i = codePoints.length - 1; i >= 0; i--) {
            builder.appendCodePoint(codePoints[i]);
        }

        return builder.toString();
    }

    /**
     * 判断字符串是否为回文。
     *
     * @param value 字符串
     * @return 是否为回文
     */
    public static boolean isPalindrome(String value) {
        if (value == null) {
            return false;
        }

        String normalized = removeAllWhitespace(value);
        if (normalized == null) {
            return false;
        }

        normalized = normalized.toLowerCase(java.util.Locale.ROOT);
        return equals(normalized, reverse(normalized));
    }

    /**
     * 计算 Levenshtein 编辑距离。
     *
     * @param value1 字符串1
     * @param value2 字符串2
     * @return 编辑距离
     */
    public static int levenshteinDistance(String value1, String value2) {
        String source = nullToEmpty(value1);
        String target = nullToEmpty(value2);

        int[] sourceCodePoints = source.codePoints().toArray();
        int[] targetCodePoints = target.codePoints().toArray();

        int sourceLength = sourceCodePoints.length;
        int targetLength = targetCodePoints.length;

        if (sourceLength == 0) {
            return targetLength;
        }
        if (targetLength == 0) {
            return sourceLength;
        }

        int[] previous = new int[targetLength + 1];
        int[] current = new int[targetLength + 1];

        for (int j = 0; j <= targetLength; j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= sourceLength; i++) {
            current[0] = i;

            for (int j = 1; j <= targetLength; j++) {
                int cost = sourceCodePoints[i - 1] == targetCodePoints[j - 1] ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost
                );
            }

            int[] temp = previous;
            previous = current;
            current = temp;
        }

        return previous[targetLength];
    }

    /**
     * 计算字符串相似度，返回 0 到 1 之间的值。
     *
     * @param value1 字符串1
     * @param value2 字符串2
     * @return 相似度
     */
    public static double similarity(String value1, String value2) {
        String source = nullToEmpty(value1);
        String target = nullToEmpty(value2);

        if (source.isEmpty() && target.isEmpty()) {
            return 1D;
        }

        int maxLength = Math.max(
                source.codePointCount(0, source.length()),
                target.codePointCount(0, target.length())
        );

        if (maxLength == 0) {
            return 1D;
        }

        int distance = levenshteinDistance(source, target);
        return Math.max(0D, 1D - ((double) distance / maxLength));
    }

    /**
     * 获取公共前缀。
     *
     * @param value1 字符串1
     * @param value2 字符串2
     * @return 公共前缀
     */
    public static String commonPrefix(String value1, String value2) {
        if (value1 == null || value2 == null) {
            return "";
        }

        int maxLength = Math.min(value1.length(), value2.length());
        int index = 0;

        while (index < maxLength && value1.charAt(index) == value2.charAt(index)) {
            index++;
        }

        if (index > 0 && Character.isHighSurrogate(value1.charAt(index - 1))) {
            index--;
        }

        return value1.substring(0, index);
    }

    /**
     * 获取公共后缀。
     *
     * @param value1 字符串1
     * @param value2 字符串2
     * @return 公共后缀
     */
    public static String commonSuffix(String value1, String value2) {
        if (value1 == null || value2 == null) {
            return "";
        }

        int index1 = value1.length() - 1;
        int index2 = value2.length() - 1;
        int count = 0;

        while (index1 >= 0 && index2 >= 0 && value1.charAt(index1) == value2.charAt(index2)) {
            index1--;
            index2--;
            count++;
        }

        int start = value1.length() - count;
        if (start < value1.length() && Character.isLowSurrogate(value1.charAt(start))) {
            start++;
        }

        return value1.substring(start);
    }

    /**
     * 获取最长公共子串。
     *
     * @param value1 字符串1
     * @param value2 字符串2
     * @return 最长公共子串
     */
    public static String longestCommonSubstring(String value1, String value2) {
        if (isEmpty(value1) || isEmpty(value2)) {
            return "";
        }

        int[] source = value1.codePoints().toArray();
        int[] target = value2.codePoints().toArray();

        int[][] dp = new int[source.length + 1][target.length + 1];
        int maxLength = 0;
        int endIndex = 0;

        for (int i = 1; i <= source.length; i++) {
            for (int j = 1; j <= target.length; j++) {
                if (source[i - 1] == target[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    if (dp[i][j] > maxLength) {
                        maxLength = dp[i][j];
                        endIndex = i;
                    }
                }
            }
        }

        if (maxLength == 0) {
            return "";
        }

        return buildStringByCodePoints(source, endIndex - maxLength, endIndex);
    }

    /**
     * 要求对象不为 null。
     *
     * @param value   对象
     * @param message 异常信息
     * @param <T>     对象类型
     * @return 原始对象
     */
    public static <T> T requireNotNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(defaultIfBlank(message, "参数不能为 null"));
        }
        return value;
    }

    /**
     * 要求字符串不为空白。
     *
     * @param value   字符串
     * @param message 异常信息
     * @return 原始字符串
     */
    public static String requireNotBlank(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(defaultIfBlank(message, "字符串不能为空白"));
        }
        return value;
    }

    /**
     * 判断下标是否有效。
     *
     * @param value 字符串
     * @param index 下标
     * @return 是否有效
     */
    public static boolean isIndexValid(CharSequence value, int index) {
        return value != null && index >= 0 && index < value.length();
    }

    /**
     * 判断范围是否有效。
     *
     * @param value 字符串
     * @param start 开始位置，包含
     * @param end   结束位置，不包含
     * @return 是否有效
     */
    public static boolean isRangeValid(CharSequence value, int start, int end) {
        return value != null && start >= 0 && end >= start && end <= value.length();
    }

    /**
     * 安全转换为字符数组。
     *
     * @param value 字符串
     * @return 字符数组
     */
    public static char[] toCharArray(String value) {
        return value == null ? new char[0] : value.toCharArray();
    }

    /**
     * 判断字符是否为中文字符。
     *
     * @param value 字符
     * @return 是否为中文字符
     */
    public static boolean isCharChinese(char value) {
        return isChineseCodePoint(value);
    }

    /**
     * 判断字符是否为 ASCII 字符。
     *
     * @param value 字符
     * @return 是否为 ASCII 字符
     */
    public static boolean isCharAscii(char value) {
        return value <= 127;
    }

    /**
     * 判断字符是否为常见 Emoji 字符。
     *
     * @param value 字符
     * @return 是否为常见 Emoji 字符
     */
    public static boolean isCharEmoji(char value) {
        return isEmojiCodePoint(value);
    }

    /**
     * 按码点构建字符串。
     *
     * @param codePoints 码点数组
     * @param start      开始位置，包含
     * @param end        结束位置，不包含
     * @return 字符串
     */
    private static String buildStringByCodePoints(int[] codePoints, int start, int end) {
        if (codePoints == null || codePoints.length == 0) {
            return "";
        }

        int safeStart = Math.max(start, 0);
        int safeEnd = Math.min(end, codePoints.length);

        if (safeStart >= safeEnd) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = safeStart; i < safeEnd; i++) {
            builder.appendCodePoint(codePoints[i]);
        }

        return builder.toString();
    }
}