package io.github.atengk.oshi;

import io.github.atengk.utils.oshi.OshiUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeEnvironmentTest {

    @Test
    void normalRuntimeEnvironmentShouldReturnData() {
        assertNotNull(OshiUtil.getContainerInfo());
        assertNotNull(OshiUtil.snapshotRuntimeEnvironment());
        assertNotNull(OshiUtil.snapshotRuntime());
    }

    @Test
    void effectiveLimitsShouldBeSafe() {
        assertTrue(OshiUtil.getEffectiveMemoryLimit() >= 0);
        assertTrue(OshiUtil.getEffectiveCpuLimit() >= 0);
        assertDoesNotThrow(OshiUtil::isContainerEnvironment);
        assertDoesNotThrow(OshiUtil::isVirtualMachine);
    }

    @Test
    void cgroupLimitsMayBeUnavailableButShouldNotThrow() {
        assertDoesNotThrow(OshiUtil::getCgroupMemoryLimit);
        assertDoesNotThrow(OshiUtil::getCgroupCpuLimit);
    }
}
