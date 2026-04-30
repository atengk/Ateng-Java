package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilAppPathTest {

    @Test
    void shouldGetApplicationPaths() {
        assertFalse(OsUtil.getWorkingDir().isBlank());
        assertFalse(OsUtil.getAppDir().isBlank());
        assertFalse(OsUtil.getClassPathRoot().isBlank());
        assertNotNull(OsUtil.getJarPath());
        assertNotNull(OsUtil.getJarDir());
    }

    @Test
    void shouldGetDefaultApplicationDirs() {
        assertFalse(OsUtil.getConfigDir().isBlank());
        assertFalse(OsUtil.getLogDir().isBlank());
        assertFalse(OsUtil.getDataDir().isBlank());
        assertFalse(OsUtil.getCacheDir().isBlank());
    }

    @Test
    void shouldGetResourcePathBoundary() {
        assertTrue(OsUtil.getResourcePath("resource-not-exists.txt").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> OsUtil.getResourcePath(" "));
    }
}
