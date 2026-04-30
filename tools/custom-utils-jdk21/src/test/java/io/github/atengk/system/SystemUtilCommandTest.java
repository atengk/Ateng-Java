package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilCommandTest {

    @Test
    void shouldExecuteCommand() {
        SystemUtil.CommandResult result = SystemUtil.execute("echo hello");
        assertEquals(0, result.getExitCode());
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("hello"));
        assertFalse(result.isTimeout());
        assertNotNull(result.getDuration());
    }

    @Test
    void shouldExecuteCommandWithListAndFindCommand() {
        assertTrue(SystemUtil.hasCommand("java"));
        assertTrue(SystemUtil.canExecute("java"));
        assertTrue(SystemUtil.findCommand("java").isPresent());
        SystemUtil.CommandResult result = SystemUtil.execute(List.of("java", "-version"));
        assertTrue(result.isSuccess());
    }

    @Test
    void shouldExecuteScript() throws Exception {
        Path script = Files.createTempFile("system-util-script-", SystemUtil.isWindows() ? ".bat" : ".sh");
        if (SystemUtil.isWindows()) {
            Files.writeString(script, "@echo off\r\necho script-ok\r\n");
        } else {
            Files.writeString(script, "echo script-ok\n");
        }
        SystemUtil.CommandResult result = SystemUtil.executeScript(script.toString());
        assertTrue(result.getStdout().contains("script-ok"));
    }

    @Test
    void shouldHandleCommandBoundaryAndException() {
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.execute(" "));
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.execute(List.of()));
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.execute("echo hello", 0));
        assertTrue(SystemUtil.buildShellCommand("echo hello").size() >= 3);
        assertFalse(SystemUtil.findCommand("system-util-command-not-exists").isPresent());
    }
}
