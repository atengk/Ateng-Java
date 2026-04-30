package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilOsInfoTest {

    @Test
    void shouldGetBasicOsInfo() {
        assertNotNull(OsUtil.getOsName());
        assertNotNull(OsUtil.getOsVersion());
        assertNotNull(OsUtil.getOsArch());
        assertFalse(OsUtil.getOsFamily().isBlank());
    }

    @Test
    void shouldIdentifyOnlyKnownMainOsFamily() {
        boolean matched = OsUtil.isWindows() || OsUtil.isLinux() || OsUtil.isMac() || OsUtil.isUnixLike()
                || "unknown".equals(OsUtil.getOsFamily());
        assertTrue(matched);
    }

    @Test
    void shouldDetectArchitectureBoundary() {
        assertTrue(OsUtil.is32Bit() || OsUtil.is64Bit() || OsUtil.isArm64() || OsUtil.isX64()
                || !OsUtil.getOsArch().isBlank());
    }
}
