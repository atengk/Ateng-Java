package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilSeparatorTest {

    @Test
    void shouldGetSystemSeparatorsAndDirs() {
        assertEquals(System.lineSeparator(), OsUtil.getLineSeparator());
        assertEquals(File.separator, OsUtil.getFileSeparator());
        assertEquals(File.pathSeparator, OsUtil.getPathSeparator());
        assertFalse(OsUtil.getTempDir().isBlank());
        assertFalse(OsUtil.getUserHomeDir().isBlank());
        assertFalse(OsUtil.getUserDir().isBlank());
    }

    @Test
    void shouldBuildCommonUserDirs() {
        assertTrue(OsUtil.getDesktopDir().contains(OsUtil.getUserHomeDir()));
        assertTrue(OsUtil.getDownloadDir().contains(OsUtil.getUserHomeDir()));
    }

    @Test
    void shouldEnsureDirAndRejectBlankPath() throws Exception {
        Path dir = Files.createTempDirectory("os-util-dir-");
        Path target = dir.resolve("a/b/c");
        assertTrue(Files.isDirectory(OsUtil.ensureDir(target.toString())));
        assertThrows(IllegalArgumentException.class, () -> OsUtil.ensureDir(" "));
    }
}
