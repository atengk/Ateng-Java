package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilCodeTest {

    @Test
    void shouldGenerateVerificationCodes() {
        assertTrue(RandomUtil.randomCode(6).matches("\\d{6}"));
        assertTrue(RandomUtil.randomNumberCode(6).matches("\\d{6}"));
        assertTrue(RandomUtil.randomLetterCode(6).matches("[a-zA-Z]{6}"));
        assertEquals(6, RandomUtil.randomMixedCode(6).length());
        assertEquals(8, RandomUtil.randomShortCode().length());
    }

    @Test
    void shouldGenerateBusinessCodes() {
        assertEquals(10, RandomUtil.randomInviteCode(10).length());
        assertEquals(10, RandomUtil.randomShareCode(10).length());
        assertEquals(12, RandomUtil.randomShortCode(12).length());
    }

    @Test
    void shouldHandleBoundaryAndException() {
        assertEquals("", RandomUtil.randomCode(0));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomCode(-1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomInviteCode(-1));
    }
}
