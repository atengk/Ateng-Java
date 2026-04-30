package io.github.atengk.codec;

import io.github.atengk.utils.codec.CodecUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class Base64CodecUtilTest {

    @Test
    void shouldEncodeAndDecodeBase64String() {
        String encoded = CodecUtil.base64Encode("中文abc");
        assertEquals("中文abc", CodecUtil.base64DecodeToString(encoded));
    }

    @Test
    void shouldEncodeAndDecodeBase64Bytes() {
        byte[] data = {1, 2, 3, 4};
        String encoded = CodecUtil.base64Encode(data);
        assertArrayEquals(data, CodecUtil.base64Decode(encoded));
    }

    @Test
    void shouldUseCustomCharset() {
        String encoded = CodecUtil.base64Encode("中文", StandardCharsets.UTF_8);
        assertEquals("中文", CodecUtil.base64DecodeToString(encoded, StandardCharsets.UTF_8));
    }

    @Test
    void shouldEncodeAndDecodeUrlSafeBase64() {
        String encoded = CodecUtil.base64UrlEncode("a?b=中文");
        assertFalse(encoded.contains("+"));
        assertFalse(encoded.contains("/"));
        assertEquals("a?b=中文", CodecUtil.base64UrlDecodeToString(encoded));
    }

    @Test
    void shouldEncodeAndDecodeMimeBase64() {
        byte[] data = "hello mime".getBytes(StandardCharsets.UTF_8);
        String encoded = CodecUtil.base64MimeEncode(data);
        assertArrayEquals(data, CodecUtil.base64MimeDecode(encoded));
    }

    @Test
    void shouldDetectBase64() {
        String encoded = CodecUtil.base64Encode("hello");
        assertTrue(CodecUtil.isBase64(encoded));
        assertFalse(CodecUtil.isBase64("not base64!"));
        assertTrue(CodecUtil.isBase64UrlSafe(CodecUtil.base64UrlEncode("hello")));
    }

    @Test
    void shouldHandleNullAndInvalidValues() {
        assertNull(CodecUtil.base64Encode((String) null));
        assertNull(CodecUtil.base64Decode(null));
        assertThrows(IllegalArgumentException.class, () -> CodecUtil.base64Decode("%%%"));
    }
}
