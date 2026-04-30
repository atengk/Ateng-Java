package io.github.atengk.oshi;

import io.github.atengk.utils.oshi.OshiUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemoryInfoTest {

    @Test
    void normalMemoryInfoShouldReturnData() {
        OshiUtil.MemoryInfo info = OshiUtil.getMemoryInfo();
        assertTrue(info.total() >= 0);
        assertTrue(info.available() >= 0);
        assertTrue(info.usagePercent() >= 0 && info.usagePercent() <= 100);
        assertNotNull(OshiUtil.snapshotMemory());
    }

    @Test
    void swapInfoShouldBeSafe() {
        assertTrue(OshiUtil.getSwapTotal() >= 0);
        assertTrue(OshiUtil.getSwapUsed() >= 0);
        assertTrue(OshiUtil.getSwapUsage() >= 0);
    }

    @Test
    void safePercentShouldHandleBoundary() {
        assertEquals(0.0, OshiUtil.safePercent(1, 0));
        assertEquals(0.0, OshiUtil.safePercent(-1, 100));
        assertEquals(100.0, OshiUtil.safePercent(200, 100));
    }
}
