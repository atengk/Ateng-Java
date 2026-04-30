package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilSeparatorTest {

    @Test
    void shouldGetSystemSeparatorsAndDirs() {
        assertEquals(System.lineSeparator(), SystemUtil.getLineSeparator());
        assertEquals(File.separator, SystemUtil.getFileSeparator());
        assertEquals(File.pathSeparator, SystemUtil.getPathSeparator());
        assertFalse(SystemUtil.getTempDir().isBlank());
        assertFalse(SystemUtil.getUserHomeDir().isBlank());
        assertFalse(SystemUtil.getUserDir().isBlank());
    }

    @Test
    void shouldBuildCommonUserDirs() {
        assertTrue(SystemUtil.getDesktopDir().contains(SystemUtil.getUserHomeDir()));
        assertTrue(SystemUtil.getDownloadDir().contains(SystemUtil.getUserHomeDir()));
    }

    @Test
    void shouldEnsureDirAndRejectBlankPath() throws Exception {
        Path dir = Files.createTempDirectory("system-util-dir-");
        Path target = dir.resolve("a/b/c");
        assertTrue(Files.isDirectory(SystemUtil.ensureDir(target.toString())));
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.ensureDir(" "));
    }
}
