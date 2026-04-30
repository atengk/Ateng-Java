package io.github.atengk.codec;

import io.github.atengk.utils.codec.CodecUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CharsetCodecUtilTest {

    @Test
    void shouldConvertStringAndBytes() {
        byte[] bytes = CodecUtil.toBytes("中文");
        assertEquals("中文", CodecUtil.toString(bytes));
    }

    @Test
    void shouldUseCustomCharsetAndDefaultCharset() {
        byte[] bytes = CodecUtil.toBytes("abc", StandardCharsets.ISO_8859_1);
        assertEquals("abc", CodecUtil.toString(bytes, StandardCharsets.ISO_8859_1));
        assertEquals(StandardCharsets.UTF_8, CodecUtil.defaultCharset());
        assertEquals(StandardCharsets.UTF_8, CodecUtil.normalizeCharset(null));
    }

    @Test
    void shouldGetCharsetWithFallback() {
        assertEquals(StandardCharsets.UTF_8, CodecUtil.getCharset("UTF-8"));
        assertEquals(StandardCharsets.UTF_8, CodecUtil.getCharset("invalid-charset"));
    }

    @Test
    void shouldCheckUtf8Bytes() {
        assertTrue(CodecUtil.isUtf8("中文".getBytes(StandardCharsets.UTF_8)));
        assertFalse(CodecUtil.isUtf8(new byte[]{(byte) 0xC3, 0x28}));
    }

    @Test
    void shouldHandleNull() {
        assertNull(CodecUtil.toBytes(null));
        assertNull(CodecUtil.toString(null));
        assertNull(CodecUtil.convertCharset(null, StandardCharsets.UTF_8, StandardCharsets.UTF_8));
    }
}
