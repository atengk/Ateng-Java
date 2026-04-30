package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilValidateTest {

    @Test
    void validateCommonSecurityValues() {
        assertDoesNotThrow(() -> SecurityUtil.requireNotBlank("a", "name"));
        assertDoesNotThrow(() -> SecurityUtil.validateKeyLength(new byte[3], 2, 3));
        assertDoesNotThrow(() -> SecurityUtil.validateIv(new byte[12], 12));
        assertDoesNotThrow(() -> SecurityUtil.validateAlgorithm("SHA-256"));
        assertTrue(SecurityUtil.isSupportedAlgorithm("SHA-256"));
        assertFalse(SecurityUtil.isSupportedAlgorithm("NO-SUCH-ALG"));
    }

    @Test
    void validateUrlsIpAndFilename() {
        assertTrue(SecurityUtil.isSafeFilename("a.txt"));
        assertFalse(SecurityUtil.isSafeFilename("../a.txt"));
        assertTrue(SecurityUtil.isSafeRedirectUrl("/home"));
        assertTrue(SecurityUtil.isSafeRedirectUrl("https://example.com/home"));
        assertFalse(SecurityUtil.isSafeRedirectUrl("//evil.com"));
        assertTrue(SecurityUtil.isPrivateIp("192.168.1.1"));
        assertTrue(SecurityUtil.isLoopbackIp("127.0.0.1"));
        assertFalse(SecurityUtil.isPrivateIp("8.8.8.8"));
    }

    @Test
    void validationShouldRejectBadValues() {
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.requireNotBlank(" ", "name"));
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.validateKeyLength(new byte[1], 2));
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.validateAlgorithm("NO-SUCH-ALG"));
    }
}
