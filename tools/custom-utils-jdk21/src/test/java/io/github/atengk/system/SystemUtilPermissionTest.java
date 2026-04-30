package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilPermissionTest {

    @Test
    void shouldCheckFilePermissions() throws Exception {
        Path file = Files.createTempFile("system-util-permission-", ".txt");
        assertTrue(SystemUtil.canRead(file.toString()));
        assertTrue(SystemUtil.canWrite(file.toString()));
        assertFalse(SystemUtil.canRead(" "));
        assertFalse(SystemUtil.canWrite(" "));
        assertFalse(SystemUtil.canExecuteFile(" "));
    }

    @Test
    void shouldCheckCommands() {
        assertTrue(SystemUtil.hasJava());
        assertTrue(SystemUtil.hasCommand("java"));
        assertDoesNotThrow(() -> SystemUtil.checkRequiredCommands("java"));
        assertThrows(IllegalStateException.class, () -> SystemUtil.checkRequiredCommands("system-util-command-not-exists"));
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.checkRequiredCommands());
        assertDoesNotThrow(SystemUtil::hasGit);
        assertDoesNotThrow(SystemUtil::hasDocker);
        assertDoesNotThrow(SystemUtil::hasCurl);
    }

    @Test
    void shouldCheckRequiredDirs() throws Exception {
        Path dir = Files.createTempDirectory("system-util-required-dir-");
        assertDoesNotThrow(() -> SystemUtil.checkRequiredDirs(dir.toString()));
        assertThrows(IllegalStateException.class, () -> SystemUtil.checkRequiredDirs(dir.resolve("missing").toString()));
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.checkRequiredDirs());
    }
}
