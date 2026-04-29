package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilRoundingTest {

    @Test
    void shouldRoundNumbers() {
        assertEquals(new BigDecimal("1.24"), NumberUtil.round(new BigDecimal("1.235"), 2));
        assertEquals(new BigDecimal("1.23"), NumberUtil.round(new BigDecimal("1.239"), 2, RoundingMode.DOWN));
        assertEquals(new BigDecimal("1.24"), NumberUtil.roundHalfUp(new BigDecimal("1.235"), 2));
        assertEquals(new BigDecimal("1.23"), NumberUtil.roundDown(new BigDecimal("1.239"), 2));
        assertEquals(new BigDecimal("1.24"), NumberUtil.roundUp(new BigDecimal("1.231"), 2));
    }

    @Test
    void shouldHandleIntegerAndScaleOperations() {
        assertEquals(new BigDecimal("1"), NumberUtil.floor(new BigDecimal("1.9")));
        assertEquals(new BigDecimal("2"), NumberUtil.ceil(new BigDecimal("1.1")));
        assertEquals(new BigDecimal("1.23"), NumberUtil.truncate(new BigDecimal("1.239"), 2));
        assertEquals(new BigDecimal("1000"), NumberUtil.stripTrailingZeros(new BigDecimal("1000.000")));
        assertEquals(2, NumberUtil.scale(new BigDecimal("1.2300")));
        assertEquals(new BigDecimal("1.20"), NumberUtil.setScale(new BigDecimal("1.2"), 2));
    }

    @Test
    void shouldThrowForInvalidScale() {
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.round(1, -1));
    }
}
