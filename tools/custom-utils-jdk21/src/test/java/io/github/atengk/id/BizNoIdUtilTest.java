package io.github.atengk.id;

import io.github.atengk.utils.id.IdUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BizNoIdUtilTest {

    @Test
    void shouldGenerateCommonBizNo() {
        String value = IdUtil.bizNo("ORD");
        assertTrue(value.startsWith("ORD"));
        assertTrue(IdUtil.isBizNo(value));
    }

    @Test
    void shouldGenerateBizNoWithPatternAndRandomLength() {
        String value = IdUtil.bizNo("BIZ", "yyyyMMdd", 4);
        assertTrue(value.matches("^BIZ\\d{8}[A-Za-z0-9]{4}$"));
        assertEquals(15, value.length());
    }

    @Test
    void shouldGenerateBusinessShortcutNumbers() {
        assertTrue(IdUtil.orderNo().startsWith("ORD"));
        assertTrue(IdUtil.payNo().startsWith("PAY"));
        assertTrue(IdUtil.refundNo().startsWith("REF"));
        assertTrue(IdUtil.tradeNo().startsWith("TRD"));
        assertTrue(IdUtil.serialNo("SER").startsWith("SER"));
        assertTrue(IdUtil.batchNo("BAT").startsWith("BAT"));
    }

    @Test
    void shouldFormatAndSplitBizNo() {
        String value = IdUtil.formatBizNo("ORD", "20260430", "ABC123");
        Map<String, String> parts = IdUtil.splitBizNo(value);
        assertEquals("ORD", parts.get("prefix"));
        assertEquals("20260430", parts.get("date"));
        assertEquals("ABC123", parts.get("sequence"));
    }

    @Test
    void shouldRejectInvalidBizNoArguments() {
        assertThrows(IllegalArgumentException.class, () -> IdUtil.bizNo(null));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.bizNo("ORD", 0));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.bizNo("ORD", "yyyyMMdd]", 4));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.splitBizNo("ORDABC"));
    }
}
