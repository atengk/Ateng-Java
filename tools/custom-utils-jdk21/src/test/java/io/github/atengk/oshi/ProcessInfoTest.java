package io.github.atengk.oshi;

import io.github.atengk.utils.oshi.OshiUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessInfoTest {

    @Test
    void normalProcessInfoShouldReturnData() {
        assertTrue(OshiUtil.getCurrentPid() > 0);
        assertNotNull(OshiUtil.getCurrentProcess());
        assertTrue(OshiUtil.getProcessCount() >= 0);
        assertTrue(OshiUtil.getThreadCount() >= 0);
        assertNotNull(OshiUtil.snapshotProcesses());
    }

    @Test
    void processListLimitShouldWork() {
        assertNotNull(OshiUtil.listProcesses(0));
        assertTrue(OshiUtil.listProcesses(1).size() <= 1);
        assertTrue(OshiUtil.listTopCpuProcesses(1).size() <= 1);
        assertTrue(OshiUtil.listTopMemoryProcesses(1).size() <= 1);
    }

    @Test
    void invalidProcessArgumentsShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> OshiUtil.listProcesses(-1));
        assertThrows(IllegalArgumentException.class, () -> OshiUtil.getProcess(0));
        assertThrows(IllegalArgumentException.class, () -> OshiUtil.listTopCpuProcesses(0));
        assertThrows(IllegalArgumentException.class, () -> OshiUtil.findProcessesByName(" "));
        assertFalse(OshiUtil.isProcessRunning(-1));
    }
}
