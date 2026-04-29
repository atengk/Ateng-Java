package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilPageQuantityTest {

    @Test
    void shouldHandlePageValues() {
        assertEquals(1, NumberUtil.safePageNum(null));
        assertEquals(1, NumberUtil.safePageNum(0));
        assertEquals(10, NumberUtil.safePageSize(null));
        assertEquals(500, NumberUtil.safePageSize(999));
        assertEquals(50, NumberUtil.safePageSize(999, 50));
        assertEquals(20L, NumberUtil.offset(3, 10));
        assertEquals(3, NumberUtil.totalPage(21L, 10));
        assertTrue(NumberUtil.hasNextPage(2, 10, 21L));
    }

    @Test
    void shouldHandleQuantityValues() {
        assertEquals(BigDecimal.ZERO, NumberUtil.safeQuantity(-1));
        assertEquals(new BigDecimal("3"), NumberUtil.safeQuantity(3));
        assertTrue(NumberUtil.checkQuantity(0));
        assertFalse(NumberUtil.checkQuantity(null));
        assertEquals(new BigDecimal("5"), NumberUtil.limitQuantity(10, 5));
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.limitQuantity(1, -1));
    }
}
