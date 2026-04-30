package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilMemoryInfoTest {

    @Test
    void shouldGetCpuAndMemoryInfo() {
        assertTrue(SystemUtil.getAvailableProcessors() > 0);
        assertTrue(SystemUtil.getJvmMaxMemory() > 0);
        assertTrue(SystemUtil.getJvmTotalMemory() > 0);
        assertTrue(SystemUtil.getJvmFreeMemory() >= 0);
        assertTrue(SystemUtil.getJvmUsedMemory() >= 0);
    }

    @Test
    void shouldGetMemoryUsage() {
        assertNotNull(SystemUtil.getJvmMemoryUsage());
        assertNotNull(SystemUtil.getHeapMemoryUsage());
        assertNotNull(SystemUtil.getNonHeapMemoryUsage());
        double rate = SystemUtil.getMemoryUsageRate();
        assertTrue(rate >= 0D && rate <= 1D);
    }

    @Test
    void shouldGetSystemLoadAverage() {
        assertTrue(SystemUtil.getSystemLoadAverage() >= -1D);
    }
}
