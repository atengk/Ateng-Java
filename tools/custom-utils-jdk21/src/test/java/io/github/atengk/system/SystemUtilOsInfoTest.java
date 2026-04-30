package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilOsInfoTest {

    @Test
    void shouldGetBasicOsInfo() {
        assertNotNull(SystemUtil.getOsName());
        assertNotNull(SystemUtil.getOsVersion());
        assertNotNull(SystemUtil.getOsArch());
        assertFalse(SystemUtil.getOsFamily().isBlank());
    }

    @Test
    void shouldIdentifyOnlyKnownMainOsFamily() {
        boolean matched = SystemUtil.isWindows() || SystemUtil.isLinux() || SystemUtil.isMac() || SystemUtil.isUnixLike()
                || "unknown".equals(SystemUtil.getOsFamily());
        assertTrue(matched);
    }

    @Test
    void shouldDetectArchitectureBoundary() {
        assertTrue(SystemUtil.is32Bit() || SystemUtil.is64Bit() || SystemUtil.isArm64() || SystemUtil.isX64()
                || !SystemUtil.getOsArch().isBlank());
    }
}
