package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilConstantsTest {

    @Test
    void shouldExposeCommonConstants() {
        assertEquals(BigDecimal.ZERO, NumberUtil.ZERO);
        assertEquals(BigDecimal.ONE, NumberUtil.ONE);
        assertEquals(BigDecimal.TEN, NumberUtil.TEN);
        assertEquals(new BigDecimal("100"), NumberUtil.HUNDRED);
        assertEquals(new BigDecimal("1000"), NumberUtil.THOUSAND);
        assertEquals(2, NumberUtil.DEFAULT_SCALE);
        assertEquals(6, NumberUtil.DEFAULT_DIVIDE_SCALE);
        assertEquals(RoundingMode.HALF_UP, NumberUtil.DEFAULT_ROUNDING_MODE);
        assertEquals(500, NumberUtil.MAX_PAGE_SIZE);
        assertEquals(1, NumberUtil.MIN_PAGE_NUM);
    }
}
