package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilAppPathTest {

    @Test
    void shouldGetApplicationPaths() {
        assertFalse(SystemUtil.getWorkingDir().isBlank());
        assertFalse(SystemUtil.getAppDir().isBlank());
        assertFalse(SystemUtil.getClassPathRoot().isBlank());
        assertNotNull(SystemUtil.getJarPath());
        assertNotNull(SystemUtil.getJarDir());
    }

    @Test
    void shouldGetDefaultApplicationDirs() {
        assertFalse(SystemUtil.getConfigDir().isBlank());
        assertFalse(SystemUtil.getLogDir().isBlank());
        assertFalse(SystemUtil.getDataDir().isBlank());
        assertFalse(SystemUtil.getCacheDir().isBlank());
    }

    @Test
    void shouldGetResourcePathBoundary() {
        assertTrue(SystemUtil.getResourcePath("resource-not-exists.txt").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.getResourcePath(" "));
    }
}
