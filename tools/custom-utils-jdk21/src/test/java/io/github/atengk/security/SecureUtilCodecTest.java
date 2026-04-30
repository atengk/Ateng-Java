package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilCodecTest {

    @Test
    void utf8HexAndBase64ShouldRoundTrip() {
        String text = "安全工具";
        assertEquals(text, SecurityUtil.fromUtf8Bytes(SecurityUtil.toUtf8Bytes(text)));
        String hex = SecurityUtil.hexEncode(text.getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), SecurityUtil.hexDecode(hex));
        String base64 = SecurityUtil.base64Encode(text.getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), SecurityUtil.base64Decode(base64));
        String base64Url = SecurityUtil.base64UrlEncode(text.getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), SecurityUtil.base64UrlDecode(base64Url));
    }

    @Test
    void codecValidationShouldWork() {
        assertTrue(SecurityUtil.isHex("0a1b"));
        assertFalse(SecurityUtil.isHex("0a1"));
        assertTrue(SecurityUtil.isBase64(SecurityUtil.base64Encode("a".getBytes(StandardCharsets.UTF_8))));
        assertFalse(SecurityUtil.isBase64("%%%"));
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.hexDecode("xyz"));
    }
}
