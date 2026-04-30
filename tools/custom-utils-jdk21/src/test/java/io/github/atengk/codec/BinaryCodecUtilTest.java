package io.github.atengk.codec;

import io.github.atengk.utils.codec.CodecUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BinaryCodecUtilTest {

    @Test
    void shouldConvertBytesAndBinaryString() {
        byte[] bytes = {65, 66};
        String binary = CodecUtil.bytesToBinaryString(bytes);
        assertEquals("0100000101000010", binary);
        assertArrayEquals(bytes, CodecUtil.binaryStringToBytes(binary));
    }

    @Test
    void shouldConvertBetweenHexAndBase64() {
        String hex = CodecUtil.stringToHex("hello");
        String base64 = CodecUtil.hexToBase64(hex);
        assertEquals(hex, CodecUtil.base64ToHex(base64));
    }

    @Test
    void shouldConvertBytesAliases() {
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(data, CodecUtil.base64ToBytes(CodecUtil.bytesToBase64(data)));
        assertArrayEquals(data, CodecUtil.hexToBytes(CodecUtil.bytesToHex(data)));
        assertEquals("abc", CodecUtil.base64ToString(CodecUtil.stringToBase64("abc")));
    }

    @Test
    void shouldHandleNullAndEmptyBinary() {
        assertNull(CodecUtil.bytesToBinaryString(null));
        assertNull(CodecUtil.binaryStringToBytes(null));
        assertArrayEquals(new byte[0], CodecUtil.binaryStringToBytes("  "));
    }

    @Test
    void shouldRejectInvalidBinary() {
        assertThrows(IllegalArgumentException.class, () -> CodecUtil.binaryStringToBytes("010"));
        assertThrows(IllegalArgumentException.class, () -> CodecUtil.binaryStringToBytes("0100000X"));
    }
}
