package io.github.atengk.oshi;

import io.github.atengk.utils.oshi.OshiUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemOverviewTest {

    @Test
    void normalOverviewShouldReturnData() {
        OshiUtil.SystemOverview overview = OshiUtil.getSystemOverview();
        assertNotNull(overview.osInfo());
        assertNotNull(overview.hostSummary());
        assertTrue(overview.uptime() >= 0);
        assertNotNull(OshiUtil.snapshotSystem());
    }

    @Test
    void healthStatusShouldBeSafe() {
        OshiUtil.HealthStatus status = OshiUtil.getHealthStatus();
        assertNotNull(status.warnings());
        assertNotNull(status.criticalItems());
        assertDoesNotThrow(OshiUtil::isSystemHealthy);
    }

    @Test
    void invalidThresholdInHealthRuleShouldThrow() {
        OshiUtil.HealthRule rule = new OshiUtil.HealthRule(-1, 80, 80, null, false);
        assertThrows(IllegalArgumentException.class, () -> OshiUtil.checkSystemHealth(rule));
    }
}
