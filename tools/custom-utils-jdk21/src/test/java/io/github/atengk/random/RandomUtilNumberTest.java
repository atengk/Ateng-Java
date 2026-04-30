package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilNumberTest {

    @Test
    void shouldGenerateNumbersWithinRange() {
        int intValue = RandomUtil.randomInt(1, 3);
        assertTrue(intValue >= 1 && intValue <= 3);

        long longValue = RandomUtil.randomLong(10L, 12L);
        assertTrue(longValue >= 10L && longValue <= 12L);

        double doubleValue = RandomUtil.randomDouble(1.0, 2.0);
        assertTrue(doubleValue >= 1.0 && doubleValue < 2.0);
    }

    @Test
    void shouldHandleBoundaryValues() {
        assertEquals(5, RandomUtil.randomInt(5, 5));
        assertEquals(9L, RandomUtil.randomLong(9L, 9L));
        assertEquals(0, RandomUtil.randomInt(1));

        BigDecimal decimal = RandomUtil.randomDecimal(1.0, 2.0, 2);
        assertEquals(2, decimal.scale());
    }

    @Test
    void shouldGenerateEvenAndOddNumbers() {
        assertEquals(0, RandomUtil.randomEven(0, 0));
        assertEquals(1, RandomUtil.randomOdd(1, 1));
        assertEquals(0, RandomUtil.randomEven(1, 2) % 2);
        assertNotEquals(0, RandomUtil.randomOdd(2, 3) % 2);
    }

    @Test
    void shouldRejectInvalidNumberArguments() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomInt(0));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomLong(0));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomInt(3, 1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomDouble(2.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomDecimal(-1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomEven(1, 1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomOdd(2, 2));
    }
}
