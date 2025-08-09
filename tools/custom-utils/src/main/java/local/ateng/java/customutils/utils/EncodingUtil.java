package local.ateng.java.customutils.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.Base64;

/**
 * 编码工具类
 * 提供常用的编码解码处理方法（如 URL 编码/解码、Base64 编码/解码、字符集转换等）
 *
 * @author Ateng
 * @since 2025-07-30
 */
public final class EncodingUtil {

    /**
     * 禁止实例化工具类
     */
    private EncodingUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * URL 编码，默认使用 UTF-8 编码
     *
     * @param input 需要编码的字符串
     * @return 编码后的字符串，编码失败返回原字符串
     */
    public static String urlEncode(String input) {
        return urlEncode(input, StandardCharsets.UTF_8.name());
    }

    /**
     * 对输入字符串进行 URL 编码
     *
     * <p>该方法使用指定的字符集对输入字符串进行 URL 编码。
     * 编码过程中，空格会被编码为 "%20" 而非默认的 "+"，以更符合 URL 标准。
     * 如果输入为 null，直接返回 null；如果指定字符集无效或编码失败，返回原字符串。</p>
     *
     * @param input   需要编码的字符串，允许为 null
     * @param charset 编码字符集名称，例如 "UTF-8"；若为 null 或无效，则默认使用 UTF-8
     * @return 编码后的字符串；若编码失败或输入为 null，则返回原字符串或 null
     */
    public static String urlEncode(String input, String charset) {
        // 空格编码替换字符
        final String spaceEncoded = "%20";
        // URLEncoder默认空格编码字符
        final String spaceEncodedByURLEncoder = "+";

        if (input == null) {
            return null;
        }

        Charset charsetToUse;
        try {
            charsetToUse = (charset == null) ? StandardCharsets.UTF_8 : Charset.forName(charset);
        } catch (Exception e) {
            charsetToUse = StandardCharsets.UTF_8;
        }

        try {
            String encoded = URLEncoder.encode(input, charsetToUse.name());
            // 将 URLEncoder 编码中默认的 '+' 替换为更标准的 '%20'
            return encoded.replace(spaceEncodedByURLEncoder, spaceEncoded);
        } catch (UnsupportedEncodingException e) {
            // 编码失败，返回原字符串
            return input;
        }
    }

    /**
     * URL 解码，默认使用 UTF-8 编码
     *
     * @param input 需要解码的字符串
     * @return 解码后的字符串，解码失败返回原字符串
     */
    public static String urlDecode(String input) {
        return urlDecode(input, "UTF-8");
    }

    /**
     * URL 解码
     *
     * @param input   需要解码的字符串
     * @param charset 解码字符集，如 UTF-8
     * @return 解码后的字符串，解码失败返回原字符串
     */
    public static String urlDecode(String input, String charset) {
        if (input == null) {
            return null;
        }
        try {
            return URLDecoder.decode(input, charset);
        } catch (UnsupportedEncodingException e) {
            // 解码失败，返回原字符串
            return input;
        }
    }

    /**
     * Base64 编码，使用标准编码器
     *
     * @param input 字节数组
     * @return Base64 编码后的字符串，输入为 null 返回 null
     */
    public static String base64Encode(byte[] input) {
        if (input == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(input);
    }

    /**
     * Base64 解码，使用标准解码器
     *
     * @param input Base64 编码的字符串
     * @return 解码后的字节数组，输入为 null 或解码失败返回 null
     */
    public static byte[] base64Decode(String input) {
        if (input == null) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(input);
        } catch (IllegalArgumentException e) {
            // 解码失败，返回 null
            return null;
        }
    }

    /**
     * 将字符串从一种字符集编码转换为另一种字符集编码
     *
     * @param input         原字符串
     * @param srcCharset    源字符集名称，如 UTF-8
     * @param targetCharset 目标字符集名称，如 ISO-8859-1
     * @return 转换编码后的字符串，转换失败返回原字符串
     */
    public static String convertCharset(String input, String srcCharset, String targetCharset) {
        if (input == null) {
            return null;
        }
        try {
            byte[] bytes = input.getBytes(srcCharset);
            return new String(bytes, targetCharset);
        } catch (UnsupportedEncodingException e) {
            // 转换失败，返回原字符串
            return input;
        }
    }

    /**
     * 判断字符串是否是有效的指定字符集编码
     *
     * @param input   字符串
     * @param charset 字符集名称
     * @return 是有效编码返回 true，否则返回 false
     */
    public static boolean isValidEncoding(String input, String charset) {
        if (input == null || charset == null) {
            return false;
        }
        Charset cs;
        try {
            cs = Charset.forName(charset);
        } catch (Exception e) {
            return false;
        }
        byte[] bytes = input.getBytes(cs);
        String decoded = new String(bytes, cs);
        return input.equals(decoded);
    }

    /**
     * Base64 URL 安全编码（替换了 + 和 /，并且不带填充符）
     *
     * @param input 字节数组
     * @return URL 安全的 Base64 编码字符串，输入为 null 返回 null
     */
    public static String base64UrlEncode(byte[] input) {
        if (input == null) {
            return null;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }

    /**
     * Base64 URL 安全解码
     *
     * @param input URL 安全的 Base64 编码字符串
     * @return 解码后的字节数组，输入为 null 或解码失败返回 null
     */
    public static byte[] base64UrlDecode(String input) {
        if (input == null) {
            return null;
        }
        try {
            return Base64.getUrlDecoder().decode(input);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 将字符串编码为指定字符集的字节数组
     *
     * @param input   输入字符串
     * @param charset 字符集名称，如 UTF-8
     * @return 字节数组，编码失败返回 null
     */
    public static byte[] getBytes(String input, String charset) {
        if (input == null) {
            return null;
        }
        try {
            return input.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    /**
     * 使用 CharsetEncoder 安全编码字符串，遇到非法字符抛异常
     *
     * @param input   输入字符串
     * @param charset 字符集名称
     * @return 编码后的字节数组，编码失败返回 null
     */
    public static byte[] safeEncode(String input, String charset) {
        if (input == null || charset == null) {
            return null;
        }
        CharsetEncoder encoder = Charset.forName(charset)
                .newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            ByteBuffer byteBuffer = encoder.encode(CharBuffer.wrap(input));
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            return bytes;
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    /**
     * 使用 CharsetDecoder 安全解码字节数组，遇到非法字符抛异常
     *
     * @param bytes   字节数组
     * @param charset 字符集名称
     * @return 解码后的字符串，解码失败返回 null
     */
    public static String safeDecode(byte[] bytes, String charset) {
        if (bytes == null || charset == null) {
            return null;
        }
        CharsetDecoder decoder = Charset.forName(charset)
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            CharBuffer charBuffer = decoder.decode(ByteBuffer.wrap(bytes));
            return charBuffer.toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    /**
     * 判断字符串是否为有效的 Base64 编码字符串（标准或 URL 安全）
     *
     * @param input 输入字符串
     * @return 是有效 Base64 编码返回 true，否则 false
     */
    public static boolean isBase64(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        // Base64 字符集（包括 URL 安全字符集）
        String base64Pattern = "^[A-Za-z0-9+/]*={0,2}$";
        String base64UrlPattern = "^[A-Za-z0-9_-]*={0,2}$";

        return input.matches(base64Pattern) || input.matches(base64UrlPattern);
    }

    /**
     * 简单判断字符串是否可能是 URL 编码字符串（是否包含 %xx 格式）
     *
     * @param input 输入字符串
     * @return 包含 %xx 格式返回 true，否则 false
     */
    public static boolean isUrlEncoded(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return input.matches(".*%[0-9a-fA-F]{2}.*");
    }

}
