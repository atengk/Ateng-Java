package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilCommandTest {

    @Test
    void shouldExecuteCommand() {
        OsUtil.CommandResult result = OsUtil.execute("echo hello");
        assertEquals(0, result.getExitCode());
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("hello"));
        assertFalse(result.isTimeout());
        assertNotNull(result.getDuration());
    }

    @Test
    void shouldExecuteCommandWithListAndFindCommand() {
        assertTrue(OsUtil.hasCommand("java"));
        assertTrue(OsUtil.canExecute("java"));
        assertTrue(OsUtil.findCommand("java").isPresent());
        OsUtil.CommandResult result = OsUtil.execute(List.of("java", "-version"));
        assertTrue(result.isSuccess());
    }

    @Test
    void shouldExecuteScript() throws Exception {
        Path script = Files.createTempFile("os-util-script-", OsUtil.isWindows() ? ".bat" : ".sh");
        if (OsUtil.isWindows()) {
            Files.writeString(script, "@echo off\r\necho script-ok\r\n");
        } else {
            Files.writeString(script, "echo script-ok\n");
        }
        OsUtil.CommandResult result = OsUtil.executeScript(script.toString());
        assertTrue(result.getStdout().contains("script-ok"));
    }

    @Test
    void shouldHandleCommandBoundaryAndException() {
        assertThrows(IllegalArgumentException.class, () -> OsUtil.execute(" "));
        assertThrows(IllegalArgumentException.class, () -> OsUtil.execute(List.of()));
        assertThrows(IllegalArgumentException.class, () -> OsUtil.execute("echo hello", 0));
        assertTrue(OsUtil.buildShellCommand("echo hello").size() >= 3);
        assertFalse(OsUtil.findCommand("os-util-command-not-exists").isPresent());
    }
}
