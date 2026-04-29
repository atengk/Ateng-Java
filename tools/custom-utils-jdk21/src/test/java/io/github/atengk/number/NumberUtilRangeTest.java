package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilRangeTest {

    @Test
    void shouldClampAndLimit() {
        assertEquals(new BigDecimal("10"), NumberUtil.clamp(11, 1, 10));
        assertEquals(new BigDecimal("1"), NumberUtil.limit(null, 1, 10));
        assertEquals(new BigDecimal("1"), NumberUtil.minLimit(-1, 1));
        assertEquals(new BigDecimal("10"), NumberUtil.maxLimit(20, 10));
    }

    @Test
    void shouldCheckRangeState() {
        assertTrue(NumberUtil.isInRange(5, 1, 10));
        assertFalse(NumberUtil.isOutOfRange(5, 1, 10));
        assertTrue(NumberUtil.lessThanMin(0, 1));
        assertTrue(NumberUtil.greaterThanMax(11, 10));
        assertEquals(new BigDecimal("9"), NumberUtil.defaultIfOutOfRange(11, 1, 10, 9));
    }

    @Test
    void shouldThrowForInvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.clamp(1, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.minLimit(1, null));
    }
}
