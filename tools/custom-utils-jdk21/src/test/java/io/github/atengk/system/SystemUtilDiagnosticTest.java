package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilDiagnosticTest {

    @Test
    void shouldGetCategorizedDiagnosticMaps() {
        assertFalse(SystemUtil.getSystemInfo().isEmpty());
        assertFalse(SystemUtil.getJvmInfo().isEmpty());
        assertFalse(SystemUtil.getProcessInfo().isEmpty());
        assertFalse(SystemUtil.getRuntimeInfo().isEmpty());
        assertFalse(SystemUtil.getHostInfo().isEmpty());
        assertFalse(SystemUtil.getNetworkInfo().isEmpty());
        assertFalse(SystemUtil.getEnvironmentInfo().isEmpty());
        assertFalse(SystemUtil.getDiskInfo(SystemUtil.getTempDir()).isEmpty());
    }

    @Test
    void shouldGetSummaryAndJson() {
        assertTrue(SystemUtil.getSystemSummary().contains("OS="));
        String json = SystemUtil.toDiagnosticJson();
        assertTrue(json.startsWith("{"));
        assertTrue(json.contains("system"));
        assertFalse(SystemUtil.toDiagnosticMap().isEmpty());
    }

    @Test
    void shouldPrintSystemInfoWithoutException() {
        assertDoesNotThrow(SystemUtil::printSystemInfo);
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.getDiskInfo(" "));
    }
}
