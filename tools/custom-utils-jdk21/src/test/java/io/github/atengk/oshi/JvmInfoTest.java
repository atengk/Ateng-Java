package io.github.atengk.oshi;

import io.github.atengk.utils.oshi.OshiUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JvmInfoTest {

    @Test
    void normalJvmInfoShouldReturnData() {
        assertNotNull(OshiUtil.getJvmInfo());
        assertNotNull(OshiUtil.getJvmMemoryInfo());
        assertNotNull(OshiUtil.getJvmThreadInfo());
        assertNotNull(OshiUtil.getJvmGcInfo());
        assertNotNull(OshiUtil.snapshotJvm());
    }

    @Test
    void currentJvmProcessMetricsShouldBeSafe() {
        assertTrue(OshiUtil.getJvmUptime() >= 0);
        assertNotNull(OshiUtil.getJavaVersion());
        assertNotNull(OshiUtil.getJavaHome());
        assertTrue(OshiUtil.getCurrentProcessCpuLoad() >= 0);
        assertTrue(OshiUtil.getCurrentProcessMemoryUsage() >= 0);
    }

    @Test
    void jvmMemoryBoundaryShouldBeNonNegativeWhereApplicable() {
        OshiUtil.JvmMemoryInfo info = OshiUtil.getJvmMemoryInfo();
        assertTrue(info.heapUsed() >= 0);
        assertTrue(info.nonHeapUsed() >= 0);
        assertTrue(info.availableProcessors() > 0);
    }
}
