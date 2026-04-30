package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilBusinessNoTest {

    @Test
    void shouldGenerateBusinessNumbers() {
        assertTrue(RandomUtil.randomOrderNo().startsWith("ORD"));
        assertTrue(RandomUtil.randomOrderNo("SO").startsWith("SO"));
        assertTrue(RandomUtil.randomSerialNo("SN").startsWith("SN"));
        assertTrue(RandomUtil.randomBatchNo("BATCH").startsWith("BATCH"));
        assertTrue(RandomUtil.randomTradeNo().startsWith("TRD"));
        assertTrue(RandomUtil.randomTaskNo().startsWith("TASK"));
        assertTrue(RandomUtil.randomNo("NO", 4).matches("NO\\d{21}"));
    }

    @Test
    void shouldHandleBoundaryAndException() {
        assertTrue(RandomUtil.randomNo("N", 0).matches("N\\d{17}"));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomNo("", 1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomNo("N", -1));
    }
}
