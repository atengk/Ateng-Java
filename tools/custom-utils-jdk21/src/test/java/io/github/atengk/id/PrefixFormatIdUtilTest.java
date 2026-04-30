package io.github.atengk.id;

import io.github.atengk.utils.id.IdUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrefixFormatIdUtilTest {

    @Test
    void shouldHandlePrefix() {
        assertEquals("ORD123", IdUtil.withPrefix("ORD", "123"));
        assertTrue(IdUtil.hasPrefix("ORD", "ORD123"));
        assertEquals("123", IdUtil.removePrefix("ORD", "ORD123"));
        assertEquals("PAY123", IdUtil.removePrefix("ORD", "PAY123"));
    }

    @Test
    void shouldFormatWithDateAndBizNo() {
        String value = IdUtil.formatWithDate("ORD", "yyyyMMdd", "ABC");
        assertTrue(value.matches("^ORD\\d{8}ABC$"));
        assertEquals("ORD20260430ABC", IdUtil.formatBizNo("ORD", "20260430", "ABC"));
    }

    @Test
    void shouldSplitBizNoAndPad() {
        Map<String, String> parts = IdUtil.splitBizNo("ORD20260430ABC");
        assertEquals("ORD", parts.get("prefix"));
        assertEquals("20260430", parts.get("date"));
        assertEquals("ABC", parts.get("sequence"));
        assertEquals("000123", IdUtil.padLeft(123, 6));
        assertEquals("00123", IdUtil.padLeft("123", 5));
        assertEquals("12300", IdUtil.padRight("123", 5));
        assertEquals("123456", IdUtil.padLeft("123456", 3));
    }

    @Test
    void shouldRejectInvalidPrefixAndFormatArguments() {
        assertThrows(IllegalArgumentException.class, () -> IdUtil.withPrefix("", "1"));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.withPrefix("前缀", "1"));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.formatWithDate("ORD", "yyyy]", "ABC"));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.formatBizNo("ORD", "", "ABC"));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.padLeft("1", 0));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.padRight("1", 0));
    }
}
