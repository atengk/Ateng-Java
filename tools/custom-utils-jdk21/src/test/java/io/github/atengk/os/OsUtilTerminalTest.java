package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilTerminalTest {

    @Test
    void shouldGetTerminalInfo() {
        assertDoesNotThrow(OsUtil::isConsoleAvailable);
        assertDoesNotThrow(OsUtil::isTerminal);
        assertDoesNotThrow(OsUtil::isAnsiSupported);
        assertNotNull(OsUtil.getShell());
    }

    @Test
    void shouldIdentifyShellTypeWithoutException() {
        assertDoesNotThrow(OsUtil::isBash);
        assertDoesNotThrow(OsUtil::isZsh);
        assertDoesNotThrow(OsUtil::isPowerShell);
        assertDoesNotThrow(OsUtil::isCmd);
    }

    @Test
    void shouldGetTerminalSizeBoundary() {
        assertTrue(OsUtil.getTerminalWidth() >= -1);
        assertTrue(OsUtil.getTerminalHeight() >= -1);
    }
}
