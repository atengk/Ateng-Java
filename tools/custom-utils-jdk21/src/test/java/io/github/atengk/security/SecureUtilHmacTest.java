package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilHmacTest {

    @TempDir
    Path tempDir;

    @Test
    void hmacShouldSignAndVerify() {
        String sign = SecurityUtil.hmacSha256Hex("data", "secret");
        assertTrue(SecurityUtil.verifyHmacSha256("data", "secret", sign));
        assertFalse(SecurityUtil.verifyHmacSha256("data2", "secret", sign));
        assertEquals(128, SecurityUtil.hmacSha512Hex("data", "secret").length());
    }

    @Test
    void hmacFileShouldWork() throws Exception {
        Path file = tempDir.resolve("hmac.txt");
        Files.writeString(file, "hello");
        assertEquals(32, SecurityUtil.hmacFile(file, "k".getBytes(), "HmacSHA256").length);
    }

    @Test
    void signTextShouldFilterAndSort() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("b", "2");
        params.put("a", "1");
        params.put("empty", "");
        assertEquals("b=2&a=1", SecurityUtil.buildSignText(params));
        assertEquals("a=1&b=2", SecurityUtil.buildSortedSignText(params));
    }
}
