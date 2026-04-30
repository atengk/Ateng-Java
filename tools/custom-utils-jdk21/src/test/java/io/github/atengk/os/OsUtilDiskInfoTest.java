package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilDiskInfoTest {

    @Test
    void shouldGetDiskInfo() {
        String path = OsUtil.getTempDir();
        assertFalse(OsUtil.getRootDirs().isEmpty());
        assertTrue(OsUtil.getDiskTotalSpace(path) >= 0);
        assertTrue(OsUtil.getDiskFreeSpace(path) >= 0);
        assertTrue(OsUtil.getDiskUsableSpace(path) >= 0);
        assertTrue(OsUtil.getDiskUsedSpace(path) >= 0);
        assertTrue(OsUtil.getDiskUsageRate(path) >= 0D);
    }

    @Test
    void shouldHandlePathOperations() throws Exception {
        Path dir = Files.createTempDirectory("os-util-disk-");
        assertTrue(OsUtil.isDiskSpaceEnough(dir.toString(), 0));
        assertTrue(OsUtil.isAbsolutePath(OsUtil.normalizePath(dir.toString())));
        assertFalse(OsUtil.joinPath("a", "b", "c").isBlank());
        assertFalse(OsUtil.toSystemPath("a/b/c").isBlank());
    }

    @Test
    void shouldRejectInvalidDiskArguments() {
        assertThrows(IllegalArgumentException.class, () -> OsUtil.getDiskTotalSpace(" "));
        assertThrows(IllegalArgumentException.class, () -> OsUtil.isDiskSpaceEnough(OsUtil.getTempDir(), -1));
        assertThrows(IllegalArgumentException.class, () -> OsUtil.normalizePath(" "));
        assertThrows(IllegalArgumentException.class, () -> OsUtil.joinPath());
    }
}
