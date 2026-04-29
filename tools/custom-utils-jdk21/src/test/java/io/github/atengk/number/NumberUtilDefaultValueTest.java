package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilDefaultValueTest {

    @Test
    void shouldHandleDefaultValues() {
        assertEquals(1, NumberUtil.defaultIfNull(null, 1));
        assertEquals(new BigDecimal("2"), NumberUtil.defaultIfInvalid("2", 0));
        assertEquals(9, NumberUtil.defaultIfInvalid("bad", 9));
    }

    @Test
    void shouldConvertNullToZeroOrEmpty() {
        assertEquals(0, NumberUtil.zeroIfNull((Integer) null));
        assertEquals(0L, NumberUtil.zeroIfNull((Long) null));
        assertEquals(BigDecimal.ZERO, NumberUtil.zeroIfNull((BigDecimal) null));
        assertEquals("", NumberUtil.emptyIfNull(null));
    }

    @Test
    void shouldReturnNullForSpecialValues() {
        assertNull(NumberUtil.nullIfZero(0));
        assertEquals(new BigDecimal("1"), NumberUtil.nullIfZero(1));
        assertNull(NumberUtil.nullIfNegative(-1));
        assertEquals(BigDecimal.ZERO, NumberUtil.nullIfNegative(0));
        assertNull(NumberUtil.nullIfInvalid("bad"));
    }
}
