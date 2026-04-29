package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilComparisonTest {

    @Test
    void shouldCompareNumbers() {
        assertEquals(0, NumberUtil.compare(new BigDecimal("1.0"), 1));
        assertTrue(NumberUtil.eq(1, 1.0D));
        assertTrue(NumberUtil.ne(1, 2));
        assertTrue(NumberUtil.gt(2, 1));
        assertTrue(NumberUtil.ge(2, 2));
        assertTrue(NumberUtil.lt(1, 2));
        assertTrue(NumberUtil.le(2, 2));
    }

    @Test
    void shouldCheckBetweenAndExtremes() {
        assertTrue(NumberUtil.between(5, 1, 10));
        assertFalse(NumberUtil.between(1, 1, 10, false));
        assertEquals(new BigDecimal("10"), NumberUtil.max(1, 10, null, 3));
        assertEquals(new BigDecimal("1"), NumberUtil.min(1, 10, null, 3));
    }

    @Test
    void shouldThrowWhenCompareInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.compare(null, 1));
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.between(1, 10, 1));
    }
}
