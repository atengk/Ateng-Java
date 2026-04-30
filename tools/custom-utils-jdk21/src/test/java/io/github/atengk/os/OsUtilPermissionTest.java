package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilPermissionTest {

    @Test
    void shouldCheckFilePermissions() throws Exception {
        Path file = Files.createTempFile("os-util-permission-", ".txt");
        assertTrue(OsUtil.canRead(file.toString()));
        assertTrue(OsUtil.canWrite(file.toString()));
        assertFalse(OsUtil.canRead(" "));
        assertFalse(OsUtil.canWrite(" "));
        assertFalse(OsUtil.canExecuteFile(" "));
    }

    @Test
    void shouldCheckCommands() {
        assertTrue(OsUtil.hasJava());
        assertTrue(OsUtil.hasCommand("java"));
        assertDoesNotThrow(() -> OsUtil.checkRequiredCommands("java"));
        assertThrows(IllegalStateException.class, () -> OsUtil.checkRequiredCommands("os-util-command-not-exists"));
        assertThrows(IllegalArgumentException.class, () -> OsUtil.checkRequiredCommands());
        assertDoesNotThrow(OsUtil::hasGit);
        assertDoesNotThrow(OsUtil::hasDocker);
        assertDoesNotThrow(OsUtil::hasCurl);
    }

    @Test
    void shouldCheckRequiredDirs() throws Exception {
        Path dir = Files.createTempDirectory("os-util-required-dir-");
        assertDoesNotThrow(() -> OsUtil.checkRequiredDirs(dir.toString()));
        assertThrows(IllegalStateException.class, () -> OsUtil.checkRequiredDirs(dir.resolve("missing").toString()));
        assertThrows(IllegalArgumentException.class, () -> OsUtil.checkRequiredDirs());
    }
}
