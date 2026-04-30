package io.github.atengk.id;

import io.github.atengk.utils.id.IdUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomCodeIdUtilTest {

    @Test
    void shouldGenerateRandomCodes() {
        assertTrue(IdUtil.randomCode(6).matches("^\\d{6}$"));
        assertTrue(IdUtil.randomNumber(6).matches("^\\d{6}$"));
        assertTrue(IdUtil.randomLetter(6).matches("^[A-Za-z]{6}$"));
        assertTrue(IdUtil.randomUpperLetter(6).matches("^[A-Z]{6}$"));
        assertTrue(IdUtil.randomLowerLetter(6).matches("^[a-z]{6}$"));
        assertTrue(IdUtil.randomMix(6).matches("^[A-Za-z0-9]{6}$"));
    }

    @Test
    void shouldGenerateReadableAndCommonCodes() {
        String readable = IdUtil.randomReadable(20);
        assertEquals(20, readable.length());
        assertFalse(readable.contains("0"));
        assertFalse(readable.contains("O"));
        assertFalse(readable.contains("1"));
        assertFalse(readable.contains("l"));
        assertTrue(IdUtil.smsCode().matches("^\\d{6}$"));
        assertTrue(IdUtil.emailCode().matches("^[A-Za-z0-9]{6}$"));
    }

    @Test
    void shouldRejectInvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> IdUtil.randomCode(0));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.randomNumber(-1));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.randomReadable(0));
    }
}
