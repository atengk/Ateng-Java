package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilJvmInfoTest {

    @Test
    void shouldGetJvmInfo() {
        assertFalse(SystemUtil.getJavaVersion().isBlank());
        assertFalse(SystemUtil.getJavaVendor().isBlank());
        assertFalse(SystemUtil.getJavaHome().isBlank());
        assertFalse(SystemUtil.getJvmName().isBlank());
        assertFalse(SystemUtil.getJvmVersion().isBlank());
        assertFalse(SystemUtil.getJvmVendor().isBlank());
    }

    @Test
    void shouldSupportJdk21Features() {
        assertTrue(SystemUtil.isJava21OrLater());
        assertTrue(SystemUtil.isVirtualThreadSupported());
    }

    @Test
    void shouldReturnClasspathAndInputArgs() {
        assertNotNull(SystemUtil.getClassPath());
        assertNotNull(SystemUtil.getLibraryPath());
        assertNotNull(SystemUtil.getJvmInputArgs());
    }
}
