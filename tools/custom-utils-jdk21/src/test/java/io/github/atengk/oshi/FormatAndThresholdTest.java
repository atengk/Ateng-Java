package io.github.atengk.oshi;

import io.github.atengk.utils.oshi.OshiUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FormatAndThresholdTest {

    @Test
    void formatMethodsShouldHandleNormalValues() {
        assertEquals("1 KB", OshiUtil.formatBytes(1024));
        assertEquals("50%", OshiUtil.formatPercent(50));
        assertEquals("1 GHz", OshiUtil.formatFrequency(1_000_000_000L));
        assertEquals("1分钟1秒", OshiUtil.formatDuration(61));
        assertEquals(new BigDecimal("1.00"), OshiUtil.toMegabytes(1024L * 1024L));
        assertEquals(new BigDecimal("1.00"), OshiUtil.toGigabytes(1024L * 1024L * 1024L));
    }

    @Test
    void formatMethodsShouldHandleBoundaryValues() {
        assertEquals("0 B", OshiUtil.formatBytes(-1));
        assertEquals("0%", OshiUtil.formatPercent(Double.NaN));
        assertEquals("0 Hz", OshiUtil.formatFrequency(0));
        assertEquals("0秒", OshiUtil.formatDuration(-1));
        assertEquals(0.0, OshiUtil.safeDivide(1, 0));
    }

    @Test
    void thresholdMethodsShouldValidateArguments() {
        assertThrows(IllegalArgumentException.class, () -> OshiUtil.round(1.0, -1));
        assertThrows(IllegalArgumentException.class, () -> OshiUtil.isCpuOverloaded(101));
        assertThrows(IllegalArgumentException.class, () -> OshiUtil.isMemoryOverloaded(-1));
        assertThrows(NullPointerException.class, () -> OshiUtil.checkResourceThreshold(null));
        assertNotNull(OshiUtil.checkResourceThreshold(new OshiUtil.ResourceThreshold(100, 100, 100, null)));
        assertNotNull(OshiUtil.checkSystemHealth(new OshiUtil.HealthRule(100, 100, 100, null, false)));
        assertNotNull(OshiUtil.snapshotResource());
        assertNotNull(OshiUtil.snapshotAll());
    }
}
