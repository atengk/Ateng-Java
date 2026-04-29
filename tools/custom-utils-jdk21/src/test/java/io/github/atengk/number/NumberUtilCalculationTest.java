package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilCalculationTest {

    @Test
    void shouldCalculateBasicOperations() {
        assertEquals(new BigDecimal("3"), NumberUtil.add(1, 2));
        assertEquals(new BigDecimal("6"), NumberUtil.add(1, 2, 3));
        assertEquals(new BigDecimal("3"), NumberUtil.sub(5, 2));
        assertEquals(new BigDecimal("6"), NumberUtil.mul(2, 3));
        assertEquals(new BigDecimal("24"), NumberUtil.mul(2, 3, 4));
    }

    @Test
    void shouldCalculateDivideAndOtherOperations() {
        assertEquals(new BigDecimal("0.33"), NumberUtil.div(1, 3, 2));
        assertEquals(new BigDecimal("0.34"), NumberUtil.div(1, 3, 2, RoundingMode.UP));
        assertEquals(new BigDecimal("-1"), NumberUtil.safeDiv(1, 0, new BigDecimal("-1")));
        assertEquals(new BigDecimal("1"), NumberUtil.remainder(10, 3));
        assertEquals(new BigDecimal("3"), NumberUtil.abs(-3));
        assertEquals(new BigDecimal("-3"), NumberUtil.negate(3));
        assertEquals(new BigDecimal("8"), NumberUtil.pow(2, 3));
    }

    @Test
    void shouldThrowForInvalidCalculation() {
        assertThrows(ArithmeticException.class, () -> NumberUtil.div(1, 0));
        assertThrows(ArithmeticException.class, () -> NumberUtil.remainder(1, 0));
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.pow(2, -1));
    }
}
