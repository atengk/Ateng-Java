package io.github.atengk.oshi;

import io.github.atengk.utils.oshi.OshiUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CpuInfoTest {

    @Test
    void normalCpuInfoShouldReturnData() {
        OshiUtil.CpuInfo info = OshiUtil.getCpuInfo();
        assertNotNull(info.name());
        assertTrue(info.logicalProcessorCount() >= 0);
        assertTrue(OshiUtil.getCpuLoadPercent() >= 0);
        assertNotNull(OshiUtil.snapshotCpu());
    }

    @Test
    void ticksShouldMatchProcessorTickTypes() {
        assertTrue(OshiUtil.getCpuTicks().length > 0);
        assertNotNull(OshiUtil.getPerCpuLoad());
    }

    @Test
    void invalidOldTicksShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> OshiUtil.calcCpuLoad(null));
        assertThrows(IllegalArgumentException.class, () -> OshiUtil.calcCpuLoad(new long[]{1L, 2L}));
    }
}
