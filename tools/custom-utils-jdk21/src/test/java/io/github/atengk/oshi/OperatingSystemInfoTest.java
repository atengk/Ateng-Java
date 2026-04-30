package io.github.atengk.oshi;

import io.github.atengk.utils.oshi.OshiUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperatingSystemInfoTest {

    @Test
    void normalOsInfoShouldReturnData() {
        OshiUtil.OsInfo info = OshiUtil.getOsInfo();
        assertNotNull(info.family());
        assertTrue(info.bitness() >= 0);
        assertNotNull(OshiUtil.getBootTime());
        assertNotNull(OshiUtil.listSessions());
    }

    @Test
    void platformDetectionShouldBeBooleanCompatible() {
        assertDoesNotThrow(OshiUtil::isWindows);
        assertDoesNotThrow(OshiUtil::isLinux);
        assertDoesNotThrow(OshiUtil::isMac);
        assertNotNull(OshiUtil.getCurrentUser());
    }

    @Test
    void architectureShouldNotBeNull() {
        assertNotNull(OshiUtil.getSystemArchitecture());
    }
}
