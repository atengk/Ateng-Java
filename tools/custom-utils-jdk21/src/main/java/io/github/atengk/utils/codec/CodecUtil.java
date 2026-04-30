package io.github.atengk.utils.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用编解码工具类。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class CodecUtil {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final char[] HEX_LOWER = "0123456789abcdef".toCharArray();
    private static final char[] HEX_UPPER = "0123456789ABCDEF".toCharArray();
    private static final String PATH_SEGMENT_ALLOWED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~!$&'()*+,;=:@";
    private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&(#\\d+|#x[0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]+);");
    private static final Pattern JSON_ESCAPE_PATTERN = Pattern.compile("\\\\([\\\"\\\\/bfnrt]|u[0-9a-fA-F]{4})");
    private static final Pattern URL_SAFE_BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+={0,2}$");

    private CodecUtil() {
        throw new UnsupportedOperationException("CodecUtil 是静态工具类，不能实例化");
    }

    /**
     * 使用 UTF-8 对字符串进行 URL 编码。
     *
     * @param value 待编码字符串
     * @return URL 编码结果，入参为 null 时返回 null
     */
    public static String urlEncode(String value) {
        return urlEncode(value, DEFAULT_CHARSET);
    }

    /**
     * 使用指定字符集对字符串进行 URL 编码。
     *
     * @param value   待编码字符串
     * @param charset 字符集，传入 null 时使用 UTF-8
     * @return URL 编码结果，入参为 null 时返回 null
     */
    public static String urlEncode(String value, Charset charset) {
        if (value == null) {
            return null;
        }
        return URLEncoder.encode(value, normalizeCharset(charset));
    }

    /**
     * 使用 UTF-8 对 URL 编码字符串进行解码。
     *
     * @param value 待解码字符串
     * @return URL 解码结果，入参为 null 时返回 null
     */
    public static String urlDecode(String value) {
        return urlDecode(value, DEFAULT_CHARSET);
    }

    /**
     * 使用指定字符集对 URL 编码字符串进行解码。
     *
     * @param value   待解码字符串
     * @param charset 字符集，传入 null 时使用 UTF-8
     * @return URL 解码结果，入参为 null 时返回 null
     */
    public static String urlDecode(String value, Charset charset) {
        if (value == null) {
            return null;
        }
        return URLDecoder.decode(value, normalizeCharset(charset));
    }

    /**
     * 使用 UTF-8 编码 URL 查询参数值。
     *
     * @param value 查询参数值
     * @return 编码后的查询参数值，入参为 null 时返回 null
     */
    public static String encodeQueryParam(String value) {
        return urlEncode(value, DEFAULT_CHARSET);
    }

    /**
     * 使用 UTF-8 解码 URL 查询参数值。
     *
     * @param value 查询参数值
     * @return 解码后的查询参数值，入参为 null 时返回 null
     */
    public static String decodeQueryParam(String value) {
        return urlDecode(value, DEFAULT_CHARSET);
    }

    /**
     * 使用 UTF-8 编码 URL 路径片段，斜杠会被编码。
     *
     * @param value 路径片段
     * @return 编码后的路径片段，入参为 null 时返回 null
     */
    public static String encodePathSegment(String value) {
        if (value == null) {
            return null;
        }
        return percentEncode(value, DEFAULT_CHARSET, PATH_SEGMENT_ALLOWED);
    }

    /**
     * 使用 UTF-8 解码 URL 路径片段。
     *
     * @param value 路径片段
     * @return 解码后的路径片段，入参为 null 时返回 null
     */
    public static String decodePathSegment(String value) {
        if (value == null) {
            return null;
        }
        return percentDecode(value, DEFAULT_CHARSET);
    }

    /**
     * 使用 UTF-8 编码表单参数值。
     *
     * @param value 表单参数值
     * @return 编码后的表单参数值，入参为 null 时返回 null
     */
    public static String encodeFormParam(String value) {
        return urlEncode(value, DEFAULT_CHARSET);
    }

    /**
     * 使用 UTF-8 解码表单参数值。
     *
     * @param value 表单参数值
     * @return 解码后的表单参数值，入参为 null 时返回 null
     */
    public static String decodeFormParam(String value) {
        return urlDecode(value, DEFAULT_CHARSET);
    }

    /**
     * 判断字符串是否疑似已经进行 URL 百分号编码。
     *
     * @param value 待判断字符串
     * @return 包含合法百分号编码时返回 true
     */
    public static boolean isUrlEncoded(String value) {
        if (isBlank(value)) {
            return false;
        }
        boolean foundPercent = false;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '%') {
                if (i + 2 >= value.length() || !isHexChar(value.charAt(i + 1)) || !isHexChar(value.charAt(i + 2))) {
                    return false;
                }
                foundPercent = true;
                i += 2;
            }
        }
        return foundPercent;
    }

    /**
     * 空值安全的 URL 编码，编码失败时返回原值。
     *
     * @param value 待编码字符串
     * @return 编码结果，入参为 null 时返回 null
     */
    public static String safeUrlEncode(String value) {
        if (value == null) {
            return null;
        }
        try {
            return urlEncode(value);
        } catch (RuntimeException ex) {
            return value;
        }
    }

    /**
     * 空值安全的 URL 解码，解码失败时返回原值。
     *
     * @param value 待解码字符串
     * @return 解码结果，入参为 null 时返回 null
     */
    public static String safeUrlDecode(String value) {
        if (value == null) {
            return null;
        }
        try {
            return urlDecode(value);
        } catch (RuntimeException ex) {
            return value;
        }
    }

    /**
     * 将参数 Map 构造成 query string，默认跳过 null 值。
     *
     * @param params 参数 Map
     * @return query string，不包含问号
     */
    public static String buildQueryString(Map<String, ?> params) {
        return buildQueryString(params, true);
    }

    /**
     * 将参数 Map 构造成 query string。
     *
     * @param params   参数 Map
     * @param skipNull 是否跳过 null 值
     * @return query string，不包含问号
     */
    public static String buildQueryString(Map<String, ?> params, boolean skipNull) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            String key = entry.getKey();
            if (isBlank(key)) {
                continue;
            }
            List<Object> values = toValueList(entry.getValue());
            for (Object rawValue : values) {
                if (rawValue == null && skipNull) {
                    continue;
                }
                String encodedKey = encodeQueryParam(key);
                String encodedValue = rawValue == null ? "" : encodeQueryParam(String.valueOf(rawValue));
                joiner.add(encodedKey + "=" + encodedValue);
            }
        }
        return joiner.toString();
    }

    /**
     * 将 query string 解析为 Map，重复参数会保留为多个值。
     *
     * @param queryString query string，可带问号
     * @return 参数 Map
     */
    public static Map<String, List<String>> parseQueryString(String queryString) {
        if (isBlank(queryString)) {
            return Collections.emptyMap();
        }
        String query = queryString.trim();
        if (query.startsWith("?")) {
            query = query.substring(1);
        }
        int fragmentIndex = query.indexOf('#');
        if (fragmentIndex >= 0) {
            query = query.substring(0, fragmentIndex);
        }
        if (query.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        String[] pairs = query.split("&", -1);
        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }
            int index = pair.indexOf('=');
            String rawKey = index >= 0 ? pair.substring(0, index) : pair;
            String rawValue = index >= 0 ? pair.substring(index + 1) : "";
            String key = decodeQueryParam(rawKey);
            String value = decodeQueryParam(rawValue);
            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
        }
        return result;
    }

    /**
     * 给 URL 追加单个 query 参数。
     *
     * @param url   URL
     * @param name  参数名
     * @param value 参数值，null 会追加为空值
     * @return 追加参数后的 URL
     */
    public static String appendQueryParam(String url, String name, Object value) {
        requireText(url, "URL 不能为空");
        requireText(name, "参数名不能为空");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put(name, value);
        return appendQueryParams(url, params);
    }

    /**
     * 给 URL 批量追加 query 参数。
     *
     * @param url    URL
     * @param params 参数 Map
     * @return 追加参数后的 URL
     */
    public static String appendQueryParams(String url, Map<String, ?> params) {
        requireText(url, "URL 不能为空");
        String query = buildQueryString(params, false);
        if (query.isEmpty()) {
            return url;
        }
        int fragmentIndex = url.indexOf('#');
        String fragment = fragmentIndex >= 0 ? url.substring(fragmentIndex) : "";
        String base = fragmentIndex >= 0 ? url.substring(0, fragmentIndex) : url;
        String delimiter = base.contains("?") ? (base.endsWith("?") || base.endsWith("&") ? "" : "&") : "?";
        return base + delimiter + query + fragment;
    }

    /**
     * 从 URL 中移除指定 query 参数。
     *
     * @param url  URL
     * @param name 参数名
     * @return 移除参数后的 URL
     */
    public static String removeQueryParam(String url, String name) {
        requireText(url, "URL 不能为空");
        requireText(name, "参数名不能为空");
        int fragmentIndex = url.indexOf('#');
        String fragment = fragmentIndex >= 0 ? url.substring(fragmentIndex) : "";
        String withoutFragment = fragmentIndex >= 0 ? url.substring(0, fragmentIndex) : url;
        int queryIndex = withoutFragment.indexOf('?');
        if (queryIndex < 0) {
            return url;
        }
        String base = withoutFragment.substring(0, queryIndex);
        String query = withoutFragment.substring(queryIndex + 1);
        Map<String, List<String>> params = new LinkedHashMap<>(parseQueryString(query));
        params.remove(name);
        String rebuilt = buildQueryString(params, false);
        return rebuilt.isEmpty() ? base + fragment : base + "?" + rebuilt + fragment;
    }

    /**
     * 从 URL 中获取指定 query 参数的第一个值。
     *
     * @param url  URL
     * @param name 参数名
     * @return 参数值，不存在时返回 null
     */
    public static String getQueryParam(String url, String name) {
        requireText(url, "URL 不能为空");
        requireText(name, "参数名不能为空");
        int queryIndex = url.indexOf('?');
        if (queryIndex < 0) {
            return null;
        }
        String query = url.substring(queryIndex + 1);
        List<String> values = parseQueryString(query).get(name);
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    /**
     * 判断 URL 是否包含指定 query 参数。
     *
     * @param url  URL
     * @param name 参数名
     * @return 包含时返回 true
     */
    public static boolean hasQueryParam(String url, String name) {
        return getQueryParam(url, name) != null;
    }

    /**
     * 规范化 query string，统一编码格式并移除多余问号和空片段。
     *
     * @param queryString query string
     * @return 规范化后的 query string
     */
    public static String normalizeQueryString(String queryString) {
        return buildQueryString(parseQueryString(queryString), false);
    }

    /**
     * 使用 UTF-8 对字符串进行标准 Base64 编码。
     *
     * @param value 待编码字符串
     * @return Base64 编码结果，入参为 null 时返回 null
     */
    public static String base64Encode(String value) {
        return base64Encode(value, DEFAULT_CHARSET);
    }

    /**
     * 使用指定字符集对字符串进行标准 Base64 编码。
     *
     * @param value   待编码字符串
     * @param charset 字符集，传入 null 时使用 UTF-8
     * @return Base64 编码结果，入参为 null 时返回 null
     */
    public static String base64Encode(String value, Charset charset) {
        if (value == null) {
            return null;
        }
        return base64Encode(value.getBytes(normalizeCharset(charset)));
    }

    /**
     * 对字节数组进行标准 Base64 编码。
     *
     * @param bytes 字节数组
     * @return Base64 编码结果，入参为 null 时返回 null
     */
    public static String base64Encode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 使用 UTF-8 将标准 Base64 字符串解码为字符串。
     *
     * @param value Base64 字符串
     * @return 解码后的字符串，入参为 null 时返回 null
     */
    public static String base64DecodeToString(String value) {
        return base64DecodeToString(value, DEFAULT_CHARSET);
    }

    /**
     * 使用指定字符集将标准 Base64 字符串解码为字符串。
     *
     * @param value   Base64 字符串
     * @param charset 字符集，传入 null 时使用 UTF-8
     * @return 解码后的字符串，入参为 null 时返回 null
     */
    public static String base64DecodeToString(String value, Charset charset) {
        byte[] bytes = base64Decode(value);
        return bytes == null ? null : new String(bytes, normalizeCharset(charset));
    }

    /**
     * 将标准 Base64 字符串解码为字节数组。
     *
     * @param value Base64 字符串
     * @return 解码后的字节数组，入参为 null 时返回 null
     */
    public static byte[] base64Decode(String value) {
        if (value == null) {
            return null;
        }
        return Base64.getDecoder().decode(value);
    }

    /**
     * 使用 UTF-8 对字符串进行 URL Safe Base64 编码。
     *
     * @param value 待编码字符串
     * @return URL Safe Base64 编码结果，入参为 null 时返回 null
     */
    public static String base64UrlEncode(String value) {
        if (value == null) {
            return null;
        }
        return base64UrlEncode(value.getBytes(DEFAULT_CHARSET));
    }

    /**
     * 对字节数组进行 URL Safe Base64 编码。
     *
     * @param bytes 字节数组
     * @return URL Safe Base64 编码结果，入参为 null 时返回 null
     */
    public static String base64UrlEncode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 使用 UTF-8 将 URL Safe Base64 字符串解码为字符串。
     *
     * @param value URL Safe Base64 字符串
     * @return 解码后的字符串，入参为 null 时返回 null
     */
    public static String base64UrlDecodeToString(String value) {
        byte[] bytes = base64UrlDecode(value);
        return bytes == null ? null : new String(bytes, DEFAULT_CHARSET);
    }

    /**
     * 将 URL Safe Base64 字符串解码为字节数组。
     *
     * @param value URL Safe Base64 字符串
     * @return 解码后的字节数组，入参为 null 时返回 null
     */
    public static byte[] base64UrlDecode(String value) {
        if (value == null) {
            return null;
        }
        return Base64.getUrlDecoder().decode(value);
    }

    /**
     * 对字节数组进行 MIME Base64 编码。
     *
     * @param bytes 字节数组
     * @return MIME Base64 编码结果，入参为 null 时返回 null
     */
    public static String base64MimeEncode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return Base64.getMimeEncoder().encodeToString(bytes);
    }

    /**
     * 将 MIME Base64 字符串解码为字节数组。
     *
     * @param value MIME Base64 字符串
     * @return 解码后的字节数组，入参为 null 时返回 null
     */
    public static byte[] base64MimeDecode(String value) {
        if (value == null) {
            return null;
        }
        return Base64.getMimeDecoder().decode(value);
    }

    /**
     * 判断字符串是否为合法标准 Base64。
     *
     * @param value 待判断字符串
     * @return 合法标准 Base64 时返回 true
     */
    public static boolean isBase64(String value) {
        if (isBlank(value)) {
            return false;
        }
        try {
            Base64.getDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * 判断字符串是否为合法 URL Safe Base64。
     *
     * @param value 待判断字符串
     * @return 合法 URL Safe Base64 时返回 true
     */
    public static boolean isBase64UrlSafe(String value) {
        if (isBlank(value) || !URL_SAFE_BASE64_PATTERN.matcher(value).matches()) {
            return false;
        }
        try {
            Base64.getUrlDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * 将字节数组转为小写十六进制字符串。
     *
     * @param bytes 字节数组
     * @return 小写十六进制字符串，入参为 null 时返回 null
     */
    public static String hexEncode(byte[] bytes) {
        return hexEncode(bytes, false);
    }

    /**
     * 将字节数组转为大写十六进制字符串。
     *
     * @param bytes 字节数组
     * @return 大写十六进制字符串，入参为 null 时返回 null
     */
    public static String hexEncodeUpper(byte[] bytes) {
        return hexEncode(bytes, true);
    }

    /**
     * 将十六进制字符串解码为字节数组。
     *
     * @param hex 十六进制字符串，可包含空格、冒号、短横线分隔符
     * @return 解码后的字节数组，入参为 null 时返回 null
     */
    public static byte[] hexDecode(String hex) {
        if (hex == null) {
            return null;
        }
        String clean = cleanHex(hex);
        if (clean.length() % 2 != 0) {
            throw new IllegalArgumentException("十六进制字符串长度必须为偶数");
        }
        byte[] result = new byte[clean.length() / 2];
        for (int i = 0; i < clean.length(); i += 2) {
            int high = Character.digit(clean.charAt(i), 16);
            int low = Character.digit(clean.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("十六进制字符串包含非法字符");
            }
            result[i / 2] = (byte) ((high << 4) + low);
        }
        return result;
    }

    /**
     * 使用 UTF-8 将字符串转为十六进制字符串。
     *
     * @param value 待转换字符串
     * @return 十六进制字符串，入参为 null 时返回 null
     */
    public static String stringToHex(String value) {
        return stringToHex(value, DEFAULT_CHARSET);
    }

    /**
     * 使用指定字符集将字符串转为十六进制字符串。
     *
     * @param value   待转换字符串
     * @param charset 字符集，传入 null 时使用 UTF-8
     * @return 十六进制字符串，入参为 null 时返回 null
     */
    public static String stringToHex(String value, Charset charset) {
        if (value == null) {
            return null;
        }
        return hexEncode(value.getBytes(normalizeCharset(charset)));
    }

    /**
     * 使用 UTF-8 将十六进制字符串转为普通字符串。
     *
     * @param hex 十六进制字符串
     * @return 普通字符串，入参为 null 时返回 null
     */
    public static String hexToString(String hex) {
        return hexToString(hex, DEFAULT_CHARSET);
    }

    /**
     * 使用指定字符集将十六进制字符串转为普通字符串。
     *
     * @param hex     十六进制字符串
     * @param charset 字符集，传入 null 时使用 UTF-8
     * @return 普通字符串，入参为 null 时返回 null
     */
    public static String hexToString(String hex, Charset charset) {
        byte[] bytes = hexDecode(hex);
        return bytes == null ? null : new String(bytes, normalizeCharset(charset));
    }

    /**
     * 判断字符串是否为合法十六进制字符串。
     *
     * @param value 待判断字符串
     * @return 合法十六进制字符串时返回 true
     */
    public static boolean isHex(String value) {
        if (isBlank(value)) {
            return false;
        }
        String clean = cleanHex(value);
        if (clean.isEmpty() || clean.length() % 2 != 0) {
            return false;
        }
        for (int i = 0; i < clean.length(); i++) {
            if (!isHexChar(clean.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 格式化十六进制字符串。
     *
     * @param hex       十六进制字符串
     * @param separator 字节分隔符，传入 null 时不添加分隔符
     * @return 格式化后的十六进制字符串，入参为 null 时返回 null
     */
    public static String formatHex(String hex, String separator) {
        if (hex == null) {
            return null;
        }
        String clean = cleanHex(hex);
        if (clean.length() % 2 != 0) {
            throw new IllegalArgumentException("十六进制字符串长度必须为偶数");
        }
        String actualSeparator = separator == null ? "" : separator;
        StringJoiner joiner = new StringJoiner(actualSeparator);
        for (int i = 0; i < clean.length(); i += 2) {
            joiner.add(clean.substring(i, i + 2));
        }
        return joiner.toString();
    }

    /**
     * 清理十六进制字符串中的常见分隔符。
     *
     * @param hex 十六进制字符串
     * @return 清理后的十六进制字符串，入参为 null 时返回 null
     */
    public static String cleanHex(String hex) {
        if (hex == null) {
            return null;
        }
        return hex.replace(" ", "").replace(":", "").replace("-", "").replace("_", "").trim();
    }

    /**
     * 将字符串中的每个字符转为 Unicode 转义。
     *
     * @param value 待编码字符串
     * @return Unicode 转义字符串，入参为 null 时返回 null
     */
    public static String unicodeEncode(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(value.length() * 6);
        for (int i = 0; i < value.length(); i++) {
            builder.append(String.format("\\u%04X", (int) value.charAt(i)));
        }
        return builder.toString();
    }

    /**
     * 将 Unicode 转义字符串还原为普通字符串。
     *
     * @param value Unicode 转义字符串
     * @return 普通字符串，入参为 null 时返回 null
     */
    public static String unicodeDecode(String value) {
        return unicodeDecodeStrict(value);
    }

    /**
     * 判断字符串是否包含 Unicode 转义片段。
     *
     * @param value 待判断字符串
     * @return 包含 Unicode 转义片段时返回 true
     */
    public static boolean containsUnicodeEscape(String value) {
        if (isBlank(value)) {
            return false;
        }
        for (int i = 0; i <= value.length() - 6; i++) {
            if (value.charAt(i) == '\\' && value.charAt(i + 1) == 'u') {
                boolean valid = true;
                for (int j = i + 2; j < i + 6; j++) {
                    if (!isHexChar(value.charAt(j))) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 只转义字符串中的非 ASCII 字符。
     *
     * @param value 待转义字符串
     * @return 转义后的字符串，入参为 null 时返回 null
     */
    public static String escapeUnicode(String value) {
        return unicodeEncodeNonAscii(value);
    }

    /**
     * 将 Unicode 转义字符串反转义为普通字符串。
     *
     * @param value Unicode 转义字符串
     * @return 普通字符串，入参为 null 时返回 null
     */
    public static String unescapeUnicode(String value) {
        return unicodeDecode(value);
    }

    /**
     * 只将非 ASCII 字符转为 Unicode 转义。
     *
     * @param value 待编码字符串
     * @return 编码后的字符串，入参为 null 时返回 null
     */
    public static String unicodeEncodeNonAscii(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c > 0x7F) {
                builder.append(String.format("\\u%04X", (int) c));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    /**
     * 严格解码 Unicode 转义字符串，非法格式会抛出异常。
     *
     * @param value Unicode 转义字符串
     * @return 解码后的字符串，入参为 null 时返回 null
     */
    public static String unicodeDecodeStrict(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length() && value.charAt(i + 1) == 'u') {
                if (i + 5 >= value.length()) {
                    throw new IllegalArgumentException("Unicode 转义格式不完整");
                }
                String hex = value.substring(i + 2, i + 6);
                if (!isHex(hex)) {
                    throw new IllegalArgumentException("Unicode 转义包含非法字符");
                }
                builder.append((char) Integer.parseInt(hex, 16));
                i += 5;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    /**
     * 宽松解码 Unicode 转义字符串，失败时返回原值。
     *
     * @param value Unicode 转义字符串
     * @return 解码后的字符串，失败时返回原值
     */
    public static String unicodeDecodeQuietly(String value) {
        if (value == null) {
            return null;
        }
        try {
            return unicodeDecodeStrict(value);
        } catch (RuntimeException ex) {
            return value;
        }
    }

    /**
     * HTML 特殊字符转义。
     *
     * @param value 待转义字符串
     * @return HTML 转义结果，入参为 null 时返回 null
     */
    public static String htmlEscape(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * HTML 特殊字符反转义。
     *
     * @param value 待反转义字符串
     * @return HTML 反转义结果，入参为 null 时返回 null
     */
    public static String htmlUnescape(String value) {
        if (value == null) {
            return null;
        }
        String decoded = decodeNumericEntities(value);
        return decoded.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&");
    }

    /**
     * XML 特殊字符转义。
     *
     * @param value 待转义字符串
     * @return XML 转义结果，入参为 null 时返回 null
     */
    public static String xmlEscape(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * XML 特殊字符反转义。
     *
     * @param value 待反转义字符串
     * @return XML 反转义结果，入参为 null 时返回 null
     */
    public static String xmlUnescape(String value) {
        if (value == null) {
            return null;
        }
        String decoded = decodeNumericEntities(value);
        return decoded.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }

    /**
     * HTML 属性值转义。
     *
     * @param value 属性值
     * @return 转义后的属性值，入参为 null 时返回 null
     */
    public static String escapeHtmlAttribute(String value) {
        return htmlEscape(value);
    }

    /**
     * XML 属性值转义。
     *
     * @param value 属性值
     * @return 转义后的属性值，入参为 null 时返回 null
     */
    public static String escapeXmlAttribute(String value) {
        return xmlEscape(value);
    }

    /**
     * 判断字符串是否包含 HTML 实体。
     *
     * @param value 待判断字符串
     * @return 包含 HTML 实体时返回 true
     */
    public static boolean containsHtmlEntity(String value) {
        return value != null && HTML_ENTITY_PATTERN.matcher(value).find();
    }

    /**
     * 去除 HTML 标签，仅保留文本内容。
     *
     * @param value HTML 字符串
     * @return 去除标签后的文本，入参为 null 时返回 null
     */
    public static String stripHtmlTags(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("(?is)<script[^>]*>.*?</script>", "")
                .replaceAll("(?is)<style[^>]*>.*?</style>", "")
                .replaceAll("(?is)<[^>]+>", "");
    }

    /**
     * 规范化 HTML 文本，去除标签、反转义实体并压缩空白符。
     *
     * @param value HTML 字符串
     * @return 规范化后的文本，入参为 null 时返回 null
     */
    public static String normalizeHtmlText(String value) {
        if (value == null) {
            return null;
        }
        return htmlUnescape(stripHtmlTags(value)).replaceAll("\\s+", " ").trim();
    }

    /**
     * JSON 字符串转义。
     *
     * @param value 待转义字符串
     * @return JSON 转义结果，入参为 null 时返回 null
     */
    public static String jsonEscape(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '/' -> builder.append("\\/");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04X", (int) c));
                    } else {
                        builder.append(c);
                    }
                }
            }
        }
        return builder.toString();
    }

    /**
     * JSON 字符串反转义。
     *
     * @param value 待反转义字符串
     * @return JSON 反转义结果，入参为 null 时返回 null
     */
    public static String jsonUnescape(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\') {
                builder.append(c);
                continue;
            }
            if (i + 1 >= value.length()) {
                throw new IllegalArgumentException("JSON 转义格式不完整");
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
                    if (i + 4 >= value.length()) {
                        throw new IllegalArgumentException("JSON Unicode 转义格式不完整");
                    }
                    String hex = value.substring(i + 1, i + 5);
                    if (!isHex(hex)) {
                        throw new IllegalArgumentException("JSON Unicode 转义包含非法字符");
                    }
                    builder.append((char) Integer.parseInt(hex, 16));
                    i += 4;
                }
                default -> throw new IllegalArgumentException("JSON 转义包含非法字符: " + next);
            }
        }
        return builder.toString();
    }

    /**
     * 转义可放入 JSON 字符串字段的内容。
     *
     * @param value 待转义字符串
     * @return 转义结果，入参为 null 时返回 null
     */
    public static String escapeJsonString(String value) {
        return jsonEscape(value);
    }

    /**
     * 还原 JSON 字符串字段内容。
     *
     * @param value 待反转义字符串
     * @return 反转义结果，入参为 null 时返回 null
     */
    public static String unescapeJsonString(String value) {
        return jsonUnescape(value);
    }

    /**
     * 判断字符串是否包含 JSON 转义序列。
     *
     * @param value 待判断字符串
     * @return 包含 JSON 转义序列时返回 true
     */
    public static boolean containsJsonEscape(String value) {
        return value != null && JSON_ESCAPE_PATTERN.matcher(value).find();
    }

    /**
     * 将文本处理为适合 JSON 输出的安全字符串，null 会转为空字符串。
     *
     * @param value 待处理文本
     * @return JSON 安全文本
     */
    public static String safeJsonText(String value) {
        return value == null ? "" : jsonEscape(value);
    }

    /**
     * 使用 UTF-8 将字符串转为字节数组。
     *
     * @param value 字符串
     * @return 字节数组，入参为 null 时返回 null
     */
    public static byte[] toBytes(String value) {
        return toBytes(value, DEFAULT_CHARSET);
    }

    /**
     * 使用指定字符集将字符串转为字节数组。
     *
     * @param value   字符串
     * @param charset 字符集，传入 null 时使用 UTF-8
     * @return 字节数组，入参为 null 时返回 null
     */
    public static byte[] toBytes(String value, Charset charset) {
        if (value == null) {
            return null;
        }
        return value.getBytes(normalizeCharset(charset));
    }

    /**
     * 使用 UTF-8 将字节数组转为字符串。
     *
     * @param bytes 字节数组
     * @return 字符串，入参为 null 时返回 null
     */
    public static String toString(byte[] bytes) {
        return toString(bytes, DEFAULT_CHARSET);
    }

    /**
     * 使用指定字符集将字节数组转为字符串。
     *
     * @param bytes   字节数组
     * @param charset 字符集，传入 null 时使用 UTF-8
     * @return 字符串，入参为 null 时返回 null
     */
    public static String toString(byte[] bytes, Charset charset) {
        if (bytes == null) {
            return null;
        }
        return new String(bytes, normalizeCharset(charset));
    }

    /**
     * 将字符串从源字符集转换为目标字符集表达。
     *
     * @param value         字符串
     * @param sourceCharset 源字符集，传入 null 时使用 UTF-8
     * @param targetCharset 目标字符集，传入 null 时使用 UTF-8
     * @return 转换后的字符串，入参为 null 时返回 null
     */
    public static String convertCharset(String value, Charset sourceCharset, Charset targetCharset) {
        if (value == null) {
            return null;
        }
        byte[] bytes = value.getBytes(normalizeCharset(sourceCharset));
        return new String(bytes, normalizeCharset(targetCharset));
    }

    /**
     * 判断字节数组是否为合法 UTF-8 数据。
     *
     * @param bytes 字节数组
     * @return 合法 UTF-8 时返回 true
     */
    public static boolean isUtf8(byte[] bytes) {
        if (bytes == null) {
            return false;
        }
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException ex) {
            return false;
        }
    }

    /**
     * 根据字符集名称获取 Charset，名称为空或非法时返回 UTF-8。
     *
     * @param charsetName 字符集名称
     * @return 字符集
     */
    public static Charset getCharset(String charsetName) {
        if (isBlank(charsetName)) {
            return DEFAULT_CHARSET;
        }
        try {
            return Charset.forName(charsetName.trim());
        } catch (RuntimeException ex) {
            return DEFAULT_CHARSET;
        }
    }

    /**
     * 返回工具类默认字符集 UTF-8。
     *
     * @return UTF-8 字符集
     */
    public static Charset defaultCharset() {
        return DEFAULT_CHARSET;
    }

    /**
     * 规范化字符集，传入 null 时返回 UTF-8。
     *
     * @param charset 字符集
     * @return 规范化后的字符集
     */
    public static Charset normalizeCharset(Charset charset) {
        return charset == null ? DEFAULT_CHARSET : charset;
    }

    /**
     * 将字节数组转为连续二进制字符串。
     *
     * @param bytes 字节数组
     * @return 二进制字符串，入参为 null 时返回 null
     */
    public static String bytesToBinaryString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(bytes.length * 8);
        for (byte item : bytes) {
            builder.append(String.format("%8s", Integer.toBinaryString(item & 0xFF)).replace(' ', '0'));
        }
        return builder.toString();
    }

    /**
     * 将连续二进制字符串转为字节数组。
     *
     * @param binary 二进制字符串，可包含空白符
     * @return 字节数组，入参为 null 时返回 null
     */
    public static byte[] binaryStringToBytes(String binary) {
        if (binary == null) {
            return null;
        }
        String clean = binary.replaceAll("\\s+", "");
        if (clean.isEmpty()) {
            return new byte[0];
        }
        if (clean.length() % 8 != 0) {
            throw new IllegalArgumentException("二进制字符串长度必须是 8 的倍数");
        }
        byte[] result = new byte[clean.length() / 8];
        for (int i = 0; i < clean.length(); i += 8) {
            String segment = clean.substring(i, i + 8);
            for (int j = 0; j < segment.length(); j++) {
                char c = segment.charAt(j);
                if (c != '0' && c != '1') {
                    throw new IllegalArgumentException("二进制字符串只能包含 0 和 1");
                }
            }
            result[i / 8] = (byte) Integer.parseInt(segment, 2);
        }
        return result;
    }

    /**
     * 将字节数组转为标准 Base64 字符串。
     *
     * @param bytes 字节数组
     * @return Base64 字符串，入参为 null 时返回 null
     */
    public static String bytesToBase64(byte[] bytes) {
        return base64Encode(bytes);
    }

    /**
     * 将标准 Base64 字符串转为字节数组。
     *
     * @param base64 Base64 字符串
     * @return 字节数组，入参为 null 时返回 null
     */
    public static byte[] base64ToBytes(String base64) {
        return base64Decode(base64);
    }

    /**
     * 将字节数组转为十六进制字符串。
     *
     * @param bytes 字节数组
     * @return 十六进制字符串，入参为 null 时返回 null
     */
    public static String bytesToHex(byte[] bytes) {
        return hexEncode(bytes);
    }

    /**
     * 将十六进制字符串转为字节数组。
     *
     * @param hex 十六进制字符串
     * @return 字节数组，入参为 null 时返回 null
     */
    public static byte[] hexToBytes(String hex) {
        return hexDecode(hex);
    }

    /**
     * 将十六进制字符串转换为标准 Base64 字符串。
     *
     * @param hex 十六进制字符串
     * @return Base64 字符串，入参为 null 时返回 null
     */
    public static String hexToBase64(String hex) {
        byte[] bytes = hexDecode(hex);
        return bytes == null ? null : base64Encode(bytes);
    }

    /**
     * 将标准 Base64 字符串转换为十六进制字符串。
     *
     * @param base64 Base64 字符串
     * @return 十六进制字符串，入参为 null 时返回 null
     */
    public static String base64ToHex(String base64) {
        byte[] bytes = base64Decode(base64);
        return bytes == null ? null : hexEncode(bytes);
    }

    /**
     * 使用 UTF-8 将字符串转为标准 Base64 字符串。
     *
     * @param value 字符串
     * @return Base64 字符串，入参为 null 时返回 null
     */
    public static String stringToBase64(String value) {
        return base64Encode(value);
    }

    /**
     * 使用 UTF-8 将标准 Base64 字符串还原为字符串。
     *
     * @param base64 Base64 字符串
     * @return 字符串，入参为 null 时返回 null
     */
    public static String base64ToString(String base64) {
        return base64DecodeToString(base64);
    }

    /**
     * 将文件内容读取为标准 Base64 字符串。
     *
     * @param path 文件路径
     * @return Base64 字符串
     * @throws IOException 读取文件失败时抛出
     */
    public static String fileToBase64(Path path) throws IOException {
        Objects.requireNonNull(path, "文件路径不能为空");
        return base64Encode(Files.readAllBytes(path));
    }

    /**
     * 将标准 Base64 字符串写入目标文件。
     *
     * @param base64     Base64 字符串
     * @param targetPath 目标文件路径
     * @throws IOException 写入文件失败时抛出
     */
    public static void base64ToFile(String base64, Path targetPath) throws IOException {
        Objects.requireNonNull(targetPath, "目标文件路径不能为空");
        byte[] bytes = base64Decode(base64);
        if (bytes == null) {
            throw new IllegalArgumentException("Base64 字符串不能为空");
        }
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(targetPath, bytes);
    }

    /**
     * 将输入流读取为标准 Base64 字符串。
     *
     * @param inputStream 输入流
     * @return Base64 字符串
     * @throws IOException 读取输入流失败时抛出
     */
    public static String inputStreamToBase64(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        return base64Encode(inputStream.readAllBytes());
    }

    /**
     * 将标准 Base64 字符串转为输入流。
     *
     * @param base64 Base64 字符串
     * @return 输入流，入参为 null 时返回空输入流
     */
    public static InputStream base64ToInputStream(String base64) {
        byte[] bytes = base64Decode(base64);
        return new ByteArrayInputStream(bytes == null ? new byte[0] : bytes);
    }

    /**
     * 将字节数组转换为 Data URI。
     *
     * @param bytes    字节数组
     * @param mimeType MIME 类型
     * @return Data URI，字节数组为 null 时返回 null
     */
    public static String bytesToDataUri(byte[] bytes, String mimeType) {
        if (bytes == null) {
            return null;
        }
        String type = isBlank(mimeType) ? "application/octet-stream" : mimeType.trim();
        return "data:" + type + ";base64," + base64Encode(bytes);
    }

    /**
     * 将 Base64 字符串转换为 Data URI。
     *
     * @param base64   Base64 字符串
     * @param mimeType MIME 类型
     * @return Data URI，Base64 字符串为 null 时返回 null
     */
    public static String base64ToDataUri(String base64, String mimeType) {
        if (base64 == null) {
            return null;
        }
        String type = isBlank(mimeType) ? "application/octet-stream" : mimeType.trim();
        return "data:" + type + ";base64," + base64;
    }

    /**
     * 从 Data URI 中提取 Base64 内容。
     *
     * @param dataUri Data URI
     * @return Base64 内容
     */
    public static String dataUriToBase64(String dataUri) {
        requireText(dataUri, "Data URI 不能为空");
        if (!isDataUri(dataUri)) {
            throw new IllegalArgumentException("非法 Data URI 格式");
        }
        int commaIndex = dataUri.indexOf(',');
        return dataUri.substring(commaIndex + 1);
    }

    /**
     * 获取 Data URI 中的 MIME 类型。
     *
     * @param dataUri Data URI
     * @return MIME 类型，没有显式类型时返回空字符串
     */
    public static String getDataUriMimeType(String dataUri) {
        requireText(dataUri, "Data URI 不能为空");
        if (!isDataUri(dataUri)) {
            throw new IllegalArgumentException("非法 Data URI 格式");
        }
        int start = "data:".length();
        int semicolonIndex = dataUri.indexOf(';', start);
        int commaIndex = dataUri.indexOf(',', start);
        int end = semicolonIndex >= 0 ? semicolonIndex : commaIndex;
        return end <= start ? "" : dataUri.substring(start, end);
    }

    /**
     * 判断字符串是否为 Data URI 格式。
     *
     * @param value 待判断字符串
     * @return 是 Data URI 时返回 true
     */
    public static boolean isDataUri(String value) {
        if (isBlank(value) || !value.startsWith("data:")) {
            return false;
        }
        int commaIndex = value.indexOf(',');
        if (commaIndex < 0) {
            return false;
        }
        String metadata = value.substring(5, commaIndex).toLowerCase();
        return metadata.isEmpty() || metadata.contains("base64") || metadata.contains("/");
    }

    /**
     * 判断字符串是否为空白。
     *
     * @param value 字符串
     * @return 为 null、空字符串或仅包含空白符时返回 true
     */
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 将空白字符串转换为 null。
     *
     * @param value 字符串
     * @return 非空白字符串或 null
     */
    public static String emptyToNull(String value) {
        return isBlank(value) ? null : value;
    }

    /**
     * 将 null 转为空字符串。
     *
     * @param value 字符串
     * @return 非 null 字符串
     */
    public static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 校验字符串不能为空白。
     *
     * @param value   字符串
     * @param message 异常提示
     * @return 原字符串
     */
    public static String requireText(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(isBlank(message) ? "字符串不能为空" : message);
        }
        return value;
    }

    /**
     * 安全执行解码逻辑，异常时返回默认值。
     *
     * @param supplier     解码逻辑
     * @param defaultValue 默认值
     * @return 解码结果或默认值
     */
    public static String safeDecode(Supplier<String> supplier, String defaultValue) {
        if (supplier == null) {
            return defaultValue;
        }
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }

    /**
     * 安全执行编码逻辑，异常时返回默认值。
     *
     * @param supplier     编码逻辑
     * @param defaultValue 默认值
     * @return 编码结果或默认值
     */
    public static String safeEncode(Supplier<String> supplier, String defaultValue) {
        return safeDecode(supplier, defaultValue);
    }

    /**
     * 静默执行解码函数，异常时返回原值。
     *
     * @param value   原始值
     * @param decoder 解码函数
     * @return 解码结果或原值
     */
    public static String decodeQuietly(String value, Function<String, String> decoder) {
        if (value == null || decoder == null) {
            return value;
        }
        try {
            return decoder.apply(value);
        } catch (RuntimeException ex) {
            return value;
        }
    }

    /**
     * 静默执行编码函数，异常时返回原值。
     *
     * @param value   原始值
     * @param encoder 编码函数
     * @return 编码结果或原值
     */
    public static String encodeQuietly(String value, Function<String, String> encoder) {
        return decodeQuietly(value, encoder);
    }

    /**
     * 按指定类型判断字符串是否已编码或已转义。
     *
     * @param value 字符串
     * @param type  编码类型
     * @return 符合指定编码类型时返回 true
     */
    public static boolean isEncoded(String value, EncodeType type) {
        Objects.requireNonNull(type, "编码类型不能为空");
        if (isBlank(value)) {
            return false;
        }
        return switch (type) {
            case URL, QUERY_PARAM, PATH_SEGMENT -> isUrlEncoded(value);
            case BASE64 -> isBase64(value);
            case BASE64_URL -> isBase64UrlSafe(value);
            case BASE64_MIME -> isBase64Mime(value);
            case HEX -> isHex(value);
            case UNICODE -> isUnicodeEscaped(value);
            case HTML, XML -> containsHtmlEntity(value);
            case JSON -> containsJsonEscape(value);
        };
    }

    /**
     * 校验字符串是否符合指定编码类型。
     *
     * @param value 字符串
     * @param type  编码类型
     */
    public static void validateEncoded(String value, EncodeType type) {
        if (!isEncoded(value, type)) {
            throw new IllegalArgumentException("字符串不符合指定编码类型: " + type);
        }
    }

    /**
     * 根据编码类型统一编码字符串。
     *
     * @param value 字符串
     * @param type  编码类型
     * @return 编码结果，入参为 null 时返回 null
     */
    public static String encode(String value, EncodeType type) {
        Objects.requireNonNull(type, "编码类型不能为空");
        if (value == null) {
            return null;
        }
        return switch (type) {
            case URL -> urlEncode(value);
            case QUERY_PARAM -> encodeQueryParam(value);
            case PATH_SEGMENT -> encodePathSegment(value);
            case BASE64 -> base64Encode(value);
            case BASE64_URL -> base64UrlEncode(value);
            case BASE64_MIME -> base64MimeEncode(value.getBytes(DEFAULT_CHARSET));
            case HEX -> stringToHex(value);
            case UNICODE -> unicodeEncode(value);
            case HTML -> htmlEscape(value);
            case XML -> xmlEscape(value);
            case JSON -> jsonEscape(value);
        };
    }

    /**
     * 根据编码类型统一解码字符串。
     *
     * @param value 字符串
     * @param type  编码类型
     * @return 解码结果，入参为 null 时返回 null
     */
    public static String decode(String value, EncodeType type) {
        Objects.requireNonNull(type, "编码类型不能为空");
        if (value == null) {
            return null;
        }
        return switch (type) {
            case URL -> urlDecode(value);
            case QUERY_PARAM -> decodeQueryParam(value);
            case PATH_SEGMENT -> decodePathSegment(value);
            case BASE64 -> base64DecodeToString(value);
            case BASE64_URL -> base64UrlDecodeToString(value);
            case BASE64_MIME -> new String(base64MimeDecode(value), DEFAULT_CHARSET);
            case HEX -> hexToString(value);
            case UNICODE -> unicodeDecode(value);
            case HTML -> htmlUnescape(value);
            case XML -> xmlUnescape(value);
            case JSON -> jsonUnescape(value);
        };
    }

    /**
     * 将字符串从一种编码格式转换为另一种编码格式。
     *
     * @param value      字符串
     * @param sourceType 源编码类型
     * @param targetType 目标编码类型
     * @return 转换结果，入参为 null 时返回 null
     */
    public static String convert(String value, EncodeType sourceType, EncodeType targetType) {
        Objects.requireNonNull(sourceType, "源编码类型不能为空");
        Objects.requireNonNull(targetType, "目标编码类型不能为空");
        if (value == null) {
            return null;
        }
        return encode(decode(value, sourceType), targetType);
    }

    private static String percentEncode(String value, Charset charset, String allowedChars) {
        StringBuilder builder = new StringBuilder(value.length());
        byte[] bytes = value.getBytes(charset);
        for (byte item : bytes) {
            int ch = item & 0xFF;
            if (ch < 128 && allowedChars.indexOf((char) ch) >= 0) {
                builder.append((char) ch);
            } else {
                builder.append('%');
                builder.append(HEX_UPPER[ch >>> 4]);
                builder.append(HEX_UPPER[ch & 0x0F]);
            }
        }
        return builder.toString();
    }

    private static String percentDecode(String value, Charset charset) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '%') {
                if (i + 2 >= value.length() || !isHexChar(value.charAt(i + 1)) || !isHexChar(value.charAt(i + 2))) {
                    throw new IllegalArgumentException("URL 百分号编码格式非法");
                }
                int high = Character.digit(value.charAt(i + 1), 16);
                int low = Character.digit(value.charAt(i + 2), 16);
                output.write((high << 4) + low);
                i += 2;
            } else {
                output.writeBytes(String.valueOf(c).getBytes(charset));
            }
        }
        return output.toString(charset);
    }

    private static List<Object> toValueList(Object value) {
        if (value == null) {
            return Collections.singletonList(null);
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> values = new ArrayList<>();
            iterable.forEach(values::add);
            return values;
        }
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        Class<?> valueClass = value.getClass();
        if (valueClass.isArray()) {
            int length = Array.getLength(value);
            List<Object> values = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                values.add(Array.get(value, i));
            }
            return values;
        }
        return Collections.singletonList(value);
    }

    private static String hexEncode(byte[] bytes, boolean upperCase) {
        if (bytes == null) {
            return null;
        }
        char[] table = upperCase ? HEX_UPPER : HEX_LOWER;
        char[] result = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            result[i * 2] = table[value >>> 4];
            result[i * 2 + 1] = table[value & 0x0F];
        }
        return new String(result);
    }

    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static String decodeNumericEntities(String value) {
        Matcher matcher = HTML_ENTITY_PATTERN.matcher(value);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String entity = matcher.group(1);
            if (entity.startsWith("#x") || entity.startsWith("#X")) {
                matcher.appendReplacement(builder, numericEntityToString(entity.substring(2), 16));
            } else if (entity.startsWith("#")) {
                matcher.appendReplacement(builder, numericEntityToString(entity.substring(1), 10));
            }
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private static String numericEntityToString(String value, int radix) {
        try {
            int codePoint = Integer.parseInt(value, radix);
            return Matcher.quoteReplacement(new String(Character.toChars(codePoint)));
        } catch (RuntimeException ex) {
            return Matcher.quoteReplacement("&#" + value + ";");
        }
    }

    private static boolean isUnicodeEscaped(String value) {
        if (!containsUnicodeEscape(value)) {
            return false;
        }
        try {
            unicodeDecodeStrict(value);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static boolean isBase64Mime(String value) {
        try {
            Base64.getMimeDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
