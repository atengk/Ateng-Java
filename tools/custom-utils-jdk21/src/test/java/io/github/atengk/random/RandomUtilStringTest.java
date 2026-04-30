package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilStringTest {

    @Test
    void shouldGenerateStringsByType() {
        assertEquals(8, RandomUtil.randomString(8).length());
        assertTrue(RandomUtil.randomLetters(8).matches("[a-zA-Z]{8}"));
        assertTrue(RandomUtil.randomLowerLetters(8).matches("[a-z]{8}"));
        assertTrue(RandomUtil.randomUpperLetters(8).matches("[A-Z]{8}"));
        assertTrue(RandomUtil.randomNumbers(8).matches("\\d{8}"));
        assertTrue(RandomUtil.randomAlphaNumeric(8).matches("[a-zA-Z0-9]{8}"));
    }

    @Test
    void shouldGenerateAsciiChineseAndPatternString() {
        assertEquals(5, RandomUtil.randomAscii(5).length());
        assertEquals(3, RandomUtil.randomChinese(3).length());

        String value = RandomUtil.randomByPattern("USER-@@-##-$$-**");
        assertTrue(value.matches("USER-[A-Z]{2}-\\d{2}-[a-z]{2}-[a-zA-Z0-9]{2}"));
    }

    @Test
    void shouldHandleZeroLength() {
        assertEquals("", RandomUtil.randomString(0));
        assertEquals("", RandomUtil.randomNumbers(0));
    }

    @Test
    void shouldRejectInvalidStringArguments() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomString("", 1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomString("abc", -1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomAscii(-1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomChinese(-1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomByPattern(" "));
    }
}
