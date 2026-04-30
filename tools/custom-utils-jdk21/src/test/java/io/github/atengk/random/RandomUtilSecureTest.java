package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilSecureTest {

    @Test
    void shouldGenerateSecureValues() {
        int value = RandomUtil.secureInt(10);
        assertTrue(value >= 0 && value < 10);
        assertDoesNotThrow(RandomUtil::secureLong);
        assertEquals(12, RandomUtil.secureString(12).length());
        assertTrue(RandomUtil.secureAlphaNumeric(12).matches("[a-zA-Z0-9]{12}"));
        assertEquals(16, RandomUtil.secureToken(16).length());
    }

    @Test
    void shouldGenerateEncodedSecureValues() {
        assertEquals(16, RandomUtil.secureHex(8).length());
        assertFalse(RandomUtil.secureBase64(8).isBlank());
        assertEquals(16, RandomUtil.secureSalt(8).length());
        assertEquals(12, RandomUtil.securePassword(12).length());
    }

    @Test
    void shouldHandleBoundaryAndException() {
        assertEquals("", RandomUtil.secureHex(0));
        assertEquals("", RandomUtil.secureBase64(0));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.secureInt(0));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.secureString(-1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.secureHex(-1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.securePassword(3));
    }
}
