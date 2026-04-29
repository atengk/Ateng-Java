package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilConversionTest {

    @Test
    void shouldConvertCommonValues() {
        assertEquals(123, NumberUtil.toInt("123"));
        assertEquals(123, NumberUtil.toInt("123.0"));
        assertEquals(123L, NumberUtil.toLong(new BigInteger("123")));
        assertEquals(12.5D, NumberUtil.toDouble("12.5"));
        assertEquals(new BigDecimal("1234.50"), NumberUtil.toBigDecimal("1,234.50"));
        assertEquals(new BigDecimal("1000"), NumberUtil.toNumber("1000"));
    }

    @Test
    void shouldReturnDefaultWhenConversionFails() {
        assertNull(NumberUtil.toInt("12.3"));
        assertEquals(9, NumberUtil.toInt("abc", 9));
        assertEquals(8L, NumberUtil.toLong("abc", 8L));
        assertEquals(1.2D, NumberUtil.toDouble("abc", 1.2D));
        assertEquals(BigDecimal.TEN, NumberUtil.toBigDecimal("abc", BigDecimal.TEN));
        assertNull(NumberUtil.toBigDecimal(Double.NaN));
        assertNull(NumberUtil.toBigDecimal(Double.POSITIVE_INFINITY));
    }

    @Test
    void shouldConvertNumberToString() {
        assertEquals("12.30", NumberUtil.toStr(new BigDecimal("12.30")));
        assertEquals("", NumberUtil.toStr(null, ""));
    }
}
