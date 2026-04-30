package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilSecurityLogTest {

    @Test
    void logMaskShouldWork() {
        assertEquals("abc****xyz", SecurityUtil.safeLogValue("abcdefgxyz"));
        assertTrue(SecurityUtil.maskAuthorization("Bearer abcdefghijklmn").startsWith("Bearer "));
        assertTrue(SecurityUtil.maskCookie("sid=abcdef; theme=dark").contains("sid="));
    }

    @Test
    void headerAndParamsShouldMaskSensitiveFields() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer abcdefghijklmn");
        assertNotEquals(headers.get("Authorization"), SecurityUtil.maskHeader(headers).get("Authorization"));
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("password", "123456");
        params.put("name", "Ateng");
        assertEquals("Ateng", SecurityUtil.maskRequestParams(params).get("name"));
        assertFalse(SecurityUtil.removeSensitiveFields(params).containsKey("password"));
    }

    @Test
    void sensitiveKeyAndExceptionMessageShouldWork() {
        assertTrue(SecurityUtil.containsSensitiveKey("access_token"));
        assertFalse(SecurityUtil.containsSensitiveKey("username"));
        assertEquals("password=******", SecurityUtil.sanitizeExceptionMessage("password=123456"));
    }
}
