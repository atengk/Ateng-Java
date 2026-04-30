package io.github.atengk.codec;

import io.github.atengk.utils.codec.CodecUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonCodecUtilTest {

    @Test
    void shouldEscapeAndUnescapeJson() {
        String text = "a\"b\\c\n中文";
        String escaped = CodecUtil.jsonEscape(text);
        assertEquals("a\\\"b\\\\c\\n中文", escaped);
        assertEquals(text, CodecUtil.jsonUnescape(escaped));
    }

    @Test
    void shouldEscapeAliases() {
        String escaped = CodecUtil.escapeJsonString("/path");
        assertEquals("\\/path", escaped);
        assertEquals("/path", CodecUtil.unescapeJsonString(escaped));
    }

    @Test
    void shouldDetectJsonEscapeAndSafeText() {
        assertTrue(CodecUtil.containsJsonEscape("a\\n"));
        assertFalse(CodecUtil.containsJsonEscape("abc"));
        assertEquals("", CodecUtil.safeJsonText(null));
    }

    @Test
    void shouldRejectInvalidJsonEscape() {
        assertThrows(IllegalArgumentException.class, () -> CodecUtil.jsonUnescape("\\x"));
        assertThrows(IllegalArgumentException.class, () -> CodecUtil.jsonUnescape("\\u12"));
    }

    @Test
    void shouldHandleNull() {
        assertNull(CodecUtil.jsonEscape(null));
        assertNull(CodecUtil.jsonUnescape(null));
    }
}
