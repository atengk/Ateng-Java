package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilMemoryInfoTest {

    @Test
    void shouldGetCpuAndMemoryInfo() {
        assertTrue(OsUtil.getAvailableProcessors() > 0);
        assertTrue(OsUtil.getJvmMaxMemory() > 0);
        assertTrue(OsUtil.getJvmTotalMemory() > 0);
        assertTrue(OsUtil.getJvmFreeMemory() >= 0);
        assertTrue(OsUtil.getJvmUsedMemory() >= 0);
    }

    @Test
    void shouldGetMemoryUsage() {
        assertNotNull(OsUtil.getJvmMemoryUsage());
        assertNotNull(OsUtil.getHeapMemoryUsage());
        assertNotNull(OsUtil.getNonHeapMemoryUsage());
        double rate = OsUtil.getMemoryUsageRate();
        assertTrue(rate >= 0D && rate <= 1D);
    }

    @Test
    void shouldGetSystemLoadAverage() {
        assertTrue(OsUtil.getSystemLoadAverage() >= -1D);
    }
}
