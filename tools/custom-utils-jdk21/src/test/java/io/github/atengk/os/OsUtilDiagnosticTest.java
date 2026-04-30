package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilDiagnosticTest {

    @Test
    void shouldGetCategorizedDiagnosticMaps() {
        assertFalse(OsUtil.getSystemInfo().isEmpty());
        assertFalse(OsUtil.getJvmInfo().isEmpty());
        assertFalse(OsUtil.getProcessInfo().isEmpty());
        assertFalse(OsUtil.getRuntimeInfo().isEmpty());
        assertFalse(OsUtil.getHostInfo().isEmpty());
        assertFalse(OsUtil.getNetworkInfo().isEmpty());
        assertFalse(OsUtil.getEnvironmentInfo().isEmpty());
        assertFalse(OsUtil.getDiskInfo(OsUtil.getTempDir()).isEmpty());
    }

    @Test
    void shouldGetSummaryAndJson() {
        assertTrue(OsUtil.getSystemSummary().contains("OS="));
        String json = OsUtil.toDiagnosticJson();
        assertTrue(json.startsWith("{"));
        assertTrue(json.contains("os"));
        assertFalse(OsUtil.toDiagnosticMap().isEmpty());
    }

    @Test
    void shouldPrintSystemInfoWithoutException() {
        assertDoesNotThrow(OsUtil::printSystemInfo);
        assertThrows(IllegalArgumentException.class, () -> OsUtil.getDiskInfo(" "));
    }
}
