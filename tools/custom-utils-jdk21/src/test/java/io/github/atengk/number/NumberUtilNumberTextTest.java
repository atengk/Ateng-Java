package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilNumberTextTest {

    @Test
    void shouldCleanAndNormalizeText() {
        assertEquals("1234.56", NumberUtil.cleanNumber(" 1,234.56 "));
        assertEquals("1234", NumberUtil.removeComma("1,234"));
        assertEquals("1234.56", NumberUtil.normalizeNumber("¥ 1,234.56"));
        assertTrue(NumberUtil.isNumericText("￥1,234.56"));
    }

    @Test
    void shouldParseText() {
        assertEquals(new BigDecimal("1234.56"), NumberUtil.parseNumberText("1,234.56"));
        assertEquals(0, new BigDecimal("0.25").compareTo(NumberUtil.parsePercentText("25%")));
        assertEquals(new BigDecimal("99.90"), NumberUtil.parseMoneyText("￥99.90"));
    }

    @Test
    void shouldExtractNumbers() {
        assertTrue(NumberUtil.containsNumber("库存：100件"));
        assertEquals(new BigDecimal("100"), NumberUtil.extractNumber("库存：100件"));
        assertEquals(List.of(new BigDecimal("100"), new BigDecimal("2.5")), NumberUtil.extractNumbers("库存100件，重量2.5kg"));
        assertNull(NumberUtil.extractNumber("abc"));
    }
}
