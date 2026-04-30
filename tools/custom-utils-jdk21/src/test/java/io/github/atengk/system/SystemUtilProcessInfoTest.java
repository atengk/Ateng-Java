package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilProcessInfoTest {

    @Test
    void shouldGetCurrentProcessInfo() {
        assertTrue(SystemUtil.getPid() > 0);
        assertTrue(SystemUtil.isProcessAlive(SystemUtil.getPid()));
        assertNotNull(SystemUtil.getProcessName());
        assertNotNull(SystemUtil.getProcessCommand());
        assertNotNull(SystemUtil.getProcessCommandLine());
        assertNotNull(SystemUtil.getProcessArgs());
    }

    @Test
    void shouldGetProcessTimeInfo() {
        assertNotNull(SystemUtil.getProcessStartTime());
        Duration uptime = SystemUtil.getProcessUptime();
        assertFalse(uptime.isNegative());
    }

    @Test
    void shouldHandleInvalidProcessOperation() {
        assertFalse(SystemUtil.isProcessAlive(-1));
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.killProcess(-1));
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.killProcessTree(SystemUtil.getPid()));
    }
}
