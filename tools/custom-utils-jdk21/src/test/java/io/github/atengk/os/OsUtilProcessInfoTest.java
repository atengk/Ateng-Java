package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilProcessInfoTest {

    @Test
    void shouldGetCurrentProcessInfo() {
        assertTrue(OsUtil.getPid() > 0);
        assertTrue(OsUtil.isProcessAlive(OsUtil.getPid()));
        assertNotNull(OsUtil.getProcessName());
        assertNotNull(OsUtil.getProcessCommand());
        assertNotNull(OsUtil.getProcessCommandLine());
        assertNotNull(OsUtil.getProcessArgs());
    }

    @Test
    void shouldGetProcessTimeInfo() {
        assertNotNull(OsUtil.getProcessStartTime());
        Duration uptime = OsUtil.getProcessUptime();
        assertFalse(uptime.isNegative());
    }

    @Test
    void shouldHandleInvalidProcessOperation() {
        assertFalse(OsUtil.isProcessAlive(-1));
        assertThrows(IllegalArgumentException.class, () -> OsUtil.killProcess(-1));
        assertThrows(IllegalArgumentException.class, () -> OsUtil.killProcessTree(OsUtil.getPid()));
    }
}
