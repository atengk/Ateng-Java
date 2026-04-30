package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilPasswordTest {

    @Test
    void passwordHashShouldVerify() {
        String encoded = SecurityUtil.hashPassword("Az9!Secure");
        assertTrue(SecurityUtil.verifyPassword("Az9!Secure", encoded));
        assertFalse(SecurityUtil.verifyPassword("wrong", encoded));
        assertFalse(SecurityUtil.needRehash(encoded));
    }

    @Test
    void passwordStrengthShouldWork() {
        assertTrue(SecurityUtil.checkPasswordStrength("Az9!Secure") >= 4);
        assertTrue(SecurityUtil.isStrongPassword("Az9!Secure"));
        assertTrue(SecurityUtil.isWeakPassword("123456"));
        assertTrue(SecurityUtil.containsSequentialChars("abc9"));
        assertTrue(SecurityUtil.containsRepeatedChars("aaab"));
        assertEquals("******", SecurityUtil.maskPassword("abc"));
    }

    @Test
    void randomPasswordShouldContainEnoughChars() {
        String password = SecurityUtil.generateRandomPassword(16);
        assertEquals(16, password.length());
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.generateRandomPassword(7));
    }
}
