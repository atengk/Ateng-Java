package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilMoneyTest {

    @Test
    void shouldCalculateMoney() {
        assertEquals(new BigDecimal("1.24"), NumberUtil.money(new BigDecimal("1.235")));
        assertEquals(new BigDecimal("3.30"), NumberUtil.addMoney(new BigDecimal("1.10"), new BigDecimal("2.20")));
        assertEquals(new BigDecimal("1.10"), NumberUtil.subMoney(new BigDecimal("3.30"), new BigDecimal("2.20")));
        assertEquals(new BigDecimal("9.99"), NumberUtil.mulMoney(new BigDecimal("3.33"), 3));
        assertEquals(new BigDecimal("3.33"), NumberUtil.divMoney(new BigDecimal("10.00"), 3));
        assertEquals(new BigDecimal("1.24"), NumberUtil.roundMoney(new BigDecimal("1.235")));
    }

    @Test
    void shouldConvertFenAndYuan() {
        assertEquals(new BigDecimal("12.34"), NumberUtil.fenToYuan(1234));
        assertEquals(1234L, NumberUtil.yuanToFen(new BigDecimal("12.34")));
        assertEquals("1,234.50", NumberUtil.formatMoney(new BigDecimal("1234.5")));
    }

    @Test
    void shouldCheckValidMoney() {
        assertTrue(NumberUtil.isValidMoney(new BigDecimal("0.01")));
        assertTrue(NumberUtil.isValidMoney(new BigDecimal("0")));
        assertFalse(NumberUtil.isValidMoney(new BigDecimal("-0.01")));
        assertFalse(NumberUtil.isValidMoney(new BigDecimal("0.001")));
        assertThrows(ArithmeticException.class, () -> NumberUtil.divMoney(1, 0));
    }
}
