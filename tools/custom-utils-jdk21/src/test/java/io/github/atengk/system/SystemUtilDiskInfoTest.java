package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilDiskInfoTest {

    @Test
    void shouldGetDiskInfo() {
        String path = SystemUtil.getTempDir();
        assertFalse(SystemUtil.getRootDirs().isEmpty());
        assertTrue(SystemUtil.getDiskTotalSpace(path) >= 0);
        assertTrue(SystemUtil.getDiskFreeSpace(path) >= 0);
        assertTrue(SystemUtil.getDiskUsableSpace(path) >= 0);
        assertTrue(SystemUtil.getDiskUsedSpace(path) >= 0);
        assertTrue(SystemUtil.getDiskUsageRate(path) >= 0D);
    }

    @Test
    void shouldHandlePathOperations() throws Exception {
        Path dir = Files.createTempDirectory("system-util-disk-");
        assertTrue(SystemUtil.isDiskSpaceEnough(dir.toString(), 0));
        assertTrue(SystemUtil.isAbsolutePath(SystemUtil.normalizePath(dir.toString())));
        assertFalse(SystemUtil.joinPath("a", "b", "c").isBlank());
        assertFalse(SystemUtil.toSystemPath("a/b/c").isBlank());
    }

    @Test
    void shouldRejectInvalidDiskArguments() {
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.getDiskTotalSpace(" "));
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.isDiskSpaceEnough(SystemUtil.getTempDir(), -1));
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.normalizePath(" "));
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.joinPath());
    }
}
