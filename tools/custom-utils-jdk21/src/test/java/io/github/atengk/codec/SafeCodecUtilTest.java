package io.github.atengk.codec;

import io.github.atengk.utils.codec.CodecUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafeCodecUtilTest {

    @Test
    void shouldHandleBlankAndEmptyConversion() {
        assertTrue(CodecUtil.isBlank("  "));
        assertNull(CodecUtil.emptyToNull("  "));
        assertEquals("", CodecUtil.nullToEmpty(null));
        assertEquals("abc", CodecUtil.requireText("abc", "不能为空"));
    }

    @Test
    void shouldRejectRequiredText() {
        assertThrows(IllegalArgumentException.class, () -> CodecUtil.requireText(" ", "不能为空"));
    }

    @Test
    void shouldSafeEncodeAndDecode() {
        assertEquals("ok", CodecUtil.safeEncode(() -> "ok", "default"));
        assertEquals("default", CodecUtil.safeDecode(() -> {
            throw new IllegalArgumentException("bad");
        }, "default"));
        assertEquals("default", CodecUtil.safeDecode(null, "default"));
    }

    @Test
    void shouldEncodeAndDecodeQuietly() {
        assertEquals("abc", CodecUtil.decodeQuietly("YWJj", CodecUtil::base64DecodeToString));
        assertEquals("%%%", CodecUtil.decodeQuietly("%%%", CodecUtil::base64DecodeToString));
        assertEquals("abc", CodecUtil.encodeQuietly("abc", null));
    }
}
