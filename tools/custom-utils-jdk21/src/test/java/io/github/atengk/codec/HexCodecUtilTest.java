package io.github.atengk.codec;

import io.github.atengk.utils.codec.CodecUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HexCodecUtilTest {

    @Test
    void shouldEncodeAndDecodeHex() {
        byte[] data = {0x0F, 0x10, (byte) 0xFF};
        assertEquals("0f10ff", CodecUtil.hexEncode(data));
        assertEquals("0F10FF", CodecUtil.hexEncodeUpper(data));
        assertArrayEquals(data, CodecUtil.hexDecode("0f10ff"));
    }

    @Test
    void shouldConvertStringAndHex() {
        String hex = CodecUtil.stringToHex("中文", StandardCharsets.UTF_8);
        assertEquals("中文", CodecUtil.hexToString(hex, StandardCharsets.UTF_8));
    }

    @Test
    void shouldCleanFormatAndValidateHex() {
        assertEquals("0A0B0C", CodecUtil.cleanHex("0A:0B-0C"));
        assertEquals("0A 0B 0C", CodecUtil.formatHex("0A0B0C", " "));
        assertTrue(CodecUtil.isHex("0A:0B-0C"));
        assertFalse(CodecUtil.isHex("0X"));
    }

    @Test
    void shouldHandleNullAndInvalidHex() {
        assertNull(CodecUtil.hexEncode(null));
        assertNull(CodecUtil.hexDecode(null));
        assertThrows(IllegalArgumentException.class, () -> CodecUtil.hexDecode("abc"));
        assertThrows(IllegalArgumentException.class, () -> CodecUtil.hexDecode("zz"));
    }
}
