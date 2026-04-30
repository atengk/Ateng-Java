package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilTerminalTest {

    @Test
    void shouldGetTerminalInfo() {
        assertDoesNotThrow(SystemUtil::isConsoleAvailable);
        assertDoesNotThrow(SystemUtil::isTerminal);
        assertDoesNotThrow(SystemUtil::isAnsiSupported);
        assertNotNull(SystemUtil.getShell());
    }

    @Test
    void shouldIdentifyShellTypeWithoutException() {
        assertDoesNotThrow(SystemUtil::isBash);
        assertDoesNotThrow(SystemUtil::isZsh);
        assertDoesNotThrow(SystemUtil::isPowerShell);
        assertDoesNotThrow(SystemUtil::isCmd);
    }

    @Test
    void shouldGetTerminalSizeBoundary() {
        assertTrue(SystemUtil.getTerminalWidth() >= -1);
        assertTrue(SystemUtil.getTerminalHeight() >= -1);
    }
}
