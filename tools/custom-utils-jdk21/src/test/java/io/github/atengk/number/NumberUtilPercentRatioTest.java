package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilPercentRatioTest {

    @Test
    void shouldCalculatePercentAndRatio() {
        assertEquals(new BigDecimal("25.00"), NumberUtil.percent(1, 4));
        assertEquals("25.00%", NumberUtil.percentText(1, 4));
        assertEquals(new BigDecimal("0.250000"), NumberUtil.ratio(1, 4));
        assertEquals(new BigDecimal("0.250000"), NumberUtil.rate(1, 4));
    }

    @Test
    void shouldCalculateBusinessRates() {
        assertEquals(new BigDecimal("25.00"), NumberUtil.growthRate(125, 100));
        assertEquals(new BigDecimal("25.00"), NumberUtil.decreaseRate(75, 100));
        assertEquals(new BigDecimal("80.00"), NumberUtil.discount(80, 100));
        assertEquals(new BigDecimal("25.00"), NumberUtil.markupRate(125, 100));
        assertEquals(new BigDecimal("-20.00"), NumberUtil.changeRate(80, 100));
    }

    @Test
    void shouldHandleZeroDenominator() {
        assertEquals(BigDecimal.ZERO, NumberUtil.percent(1, 0));
        assertEquals(new BigDecimal("-1"), NumberUtil.ratio(1, 0, 2, new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.percent(1, 2, -1));
    }
}
