package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilRandomTest {

    @Test
    void shouldGenerateRandomNumbersInRange() {
        assertEquals(5, NumberUtil.randomInt(5, 5));
        assertEquals(9L, NumberUtil.randomLong(9L, 9L));
        double value = NumberUtil.randomDouble(1.0, 2.0);
        assertTrue(value >= 1.0 && value < 2.0);
        BigDecimal decimal = NumberUtil.randomBigDecimal(BigDecimal.ONE, BigDecimal.TEN, 2);
        assertTrue(decimal.compareTo(BigDecimal.ONE) >= 0 && decimal.compareTo(BigDecimal.TEN) <= 0);
    }

    @Test
    void shouldGenerateDigitsAndSignedRandom() {
        assertEquals(6, NumberUtil.randomDigits(6).length());
        assertEquals(6, NumberUtil.randomCode(6).length());
        assertEquals(6, NumberUtil.secureRandomCode(6).length());
        assertTrue(NumberUtil.randomPositiveInt(10) >= 1);
        assertTrue(NumberUtil.randomNegativeInt(-10) <= -1);
    }

    @Test
    void shouldThrowForInvalidRandomArguments() {
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.randomInt(2, 1));
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.randomDouble(2.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.randomDigits(0));
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.randomPositiveInt(0));
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.randomNegativeInt(0));
    }
}
