package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilPredicateTest {

    @Test
    void shouldCheckNumberText() {
        assertTrue(NumberUtil.isNumber("1,234.56"));
        assertFalse(NumberUtil.isNumber("abc"));
        assertTrue(NumberUtil.isInteger("100.0"));
        assertFalse(NumberUtil.isInteger("100.1"));
        assertTrue(NumberUtil.isDecimal("100.1"));
    }

    @Test
    void shouldCheckSignAndZero() {
        assertTrue(NumberUtil.isPositive(1));
        assertTrue(NumberUtil.isNegative(-1));
        assertTrue(NumberUtil.isZero(0.0D));
        assertTrue(NumberUtil.isNotZero(0.1D));
        assertTrue(NumberUtil.isPositiveOrZero(0));
        assertTrue(NumberUtil.isNegativeOrZero(-1));
        assertFalse(NumberUtil.isPositive(null));
    }

    @Test
    void shouldCheckOddEvenAndDoubleState() {
        assertTrue(NumberUtil.isEven(2));
        assertTrue(NumberUtil.isOdd(-3));
        assertFalse(NumberUtil.isEven(2.5));
        assertTrue(NumberUtil.isFinite(1.0D));
        assertFalse(NumberUtil.isFinite(Double.NaN));
        assertTrue(NumberUtil.isNaN(Double.NaN));
    }
}
