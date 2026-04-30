package io.github.atengk.codec;

import io.github.atengk.utils.codec.CodecUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnicodeCodecUtilTest {

    @Test
    void shouldEncodeAndDecodeUnicode() {
        String encoded = CodecUtil.unicodeEncode("中A");
        assertEquals("\\u4E2D\\u0041", encoded);
        assertEquals("中A", CodecUtil.unicodeDecode(encoded));
    }

    @Test
    void shouldEscapeOnlyNonAscii() {
        assertEquals("A\\u4E2D", CodecUtil.unicodeEncodeNonAscii("A中"));
        assertEquals("A中", CodecUtil.unescapeUnicode("A\\u4E2D"));
    }

    @Test
    void shouldDetectUnicodeEscape() {
        assertTrue(CodecUtil.containsUnicodeEscape("A\\u4E2D"));
        assertFalse(CodecUtil.containsUnicodeEscape("A中"));
    }

    @Test
    void shouldDecodeQuietlyWhenInvalid() {
        assertEquals("\\u4E", CodecUtil.unicodeDecodeQuietly("\\u4E"));
        assertThrows(IllegalArgumentException.class, () -> CodecUtil.unicodeDecodeStrict("\\u4E"));
    }

    @Test
    void shouldHandleNull() {
        assertNull(CodecUtil.unicodeEncode(null));
        assertNull(CodecUtil.unicodeDecode(null));
    }
}
