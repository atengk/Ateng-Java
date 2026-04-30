package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilRandomTest {

    @Test
    void randomBytesShouldReturnExpectedLength() {
        assertEquals(16, SecurityUtil.randomBytes(16).length);
        assertEquals(32, SecurityUtil.randomHex(16).length());
        assertTrue(SecurityUtil.randomNumeric(6).matches("\\d{6}"));
        assertTrue(SecurityUtil.randomAlphaNumeric(12).matches("[A-Za-z0-9]{12}"));
    }

    @Test
    void uuidAndNonceShouldBeValid() {
        assertEquals(36, SecurityUtil.uuid().length());
        assertEquals(32, SecurityUtil.uuidWithoutDash().length());
        assertTrue(SecurityUtil.verifyNonce(SecurityUtil.nonce()));
        assertTrue(SecurityUtil.timestampNonce().contains("-"));
    }

    @Test
    void randomShouldRejectInvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.randomBytes(0));
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.randomNumeric(-1));
    }
}
