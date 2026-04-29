package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilFormatTest {

    @Test
    void shouldFormatNumbers() {
        assertEquals("1234.567", NumberUtil.format(1234.567));
        assertEquals("1,234.57", NumberUtil.format(1234.567, "#,##0.00"));
        assertEquals("1.20", NumberUtil.formatDecimal(1.2, 2));
        assertEquals("1,234.5", NumberUtil.formatThousands(1234.5));
    }

    @Test
    void shouldFormatPercentMoneyAndPlainString() {
        assertEquals("25.00%", NumberUtil.formatPercent(new java.math.BigDecimal("0.25")));
        assertEquals("1,234.50", NumberUtil.formatMoney(1234.5));
        assertEquals("1000", NumberUtil.removeTrailingZeros(new java.math.BigDecimal("1000.00")));
        assertEquals("1000000000000", NumberUtil.formatPlain(new java.math.BigDecimal("1E+12")));
        assertEquals("1000000000000", NumberUtil.toPlainString(new java.math.BigDecimal("1E+12")));
    }

    @Test
    void shouldHandleEmptyAndInvalidPattern() {
        assertEquals("", NumberUtil.format(null));
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.format(1, ""));
    }
}
