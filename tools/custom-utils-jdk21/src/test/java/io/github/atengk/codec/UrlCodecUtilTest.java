package io.github.atengk.codec;

import io.github.atengk.utils.codec.CodecUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class UrlCodecUtilTest {

    @Test
    void shouldEncodeAndDecodeUrlByUtf8() {
        String encoded = CodecUtil.urlEncode("中文 value");
        assertEquals("%E4%B8%AD%E6%96%87+value", encoded);
        assertEquals("中文 value", CodecUtil.urlDecode(encoded));
    }

    @Test
    void shouldEncodeAndDecodeWithCustomCharset() {
        String encoded = CodecUtil.urlEncode("中文", StandardCharsets.UTF_8);
        assertEquals("中文", CodecUtil.urlDecode(encoded, StandardCharsets.UTF_8));
    }

    @Test
    void shouldEncodeQueryAndFormParam() {
        assertEquals("a+b", CodecUtil.encodeQueryParam("a b"));
        assertEquals("a b", CodecUtil.decodeQueryParam("a+b"));
        assertEquals("a+b", CodecUtil.encodeFormParam("a b"));
        assertEquals("a b", CodecUtil.decodeFormParam("a+b"));
    }

    @Test
    void shouldEncodeAndDecodePathSegment() {
        String encoded = CodecUtil.encodePathSegment("a/b 中文");
        assertEquals("a%2Fb%20%E4%B8%AD%E6%96%87", encoded);
        assertEquals("a/b 中文", CodecUtil.decodePathSegment(encoded));
    }

    @Test
    void shouldHandleNullSafely() {
        assertNull(CodecUtil.urlEncode(null));
        assertNull(CodecUtil.urlDecode(null));
        assertNull(CodecUtil.encodePathSegment(null));
        assertNull(CodecUtil.decodePathSegment(null));
    }

    @Test
    void shouldDetectUrlEncodedString() {
        assertTrue(CodecUtil.isUrlEncoded("%E4%B8%AD"));
        assertFalse(CodecUtil.isUrlEncoded("中文"));
        assertFalse(CodecUtil.isUrlEncoded("%E4%B8%"));
    }

    @Test
    void shouldDecodeSafelyWhenFormatInvalid() {
        assertEquals("%E4%B8%", CodecUtil.safeUrlDecode("%E4%B8%"));
        assertEquals("a+b", CodecUtil.safeUrlEncode("a b"));
    }

    @Test
    void shouldThrowWhenPathPercentFormatInvalid() {
        assertThrows(IllegalArgumentException.class, () -> CodecUtil.decodePathSegment("abc%"));
    }
}
