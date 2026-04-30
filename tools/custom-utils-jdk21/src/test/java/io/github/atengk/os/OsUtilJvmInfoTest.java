package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilJvmInfoTest {

    @Test
    void shouldGetJvmInfo() {
        assertFalse(OsUtil.getJavaVersion().isBlank());
        assertFalse(OsUtil.getJavaVendor().isBlank());
        assertFalse(OsUtil.getJavaHome().isBlank());
        assertFalse(OsUtil.getJvmName().isBlank());
        assertFalse(OsUtil.getJvmVersion().isBlank());
        assertFalse(OsUtil.getJvmVendor().isBlank());
    }

    @Test
    void shouldSupportJdk21Features() {
        assertTrue(OsUtil.isJava21OrLater());
        assertTrue(OsUtil.isVirtualThreadSupported());
    }

    @Test
    void shouldReturnClasspathAndInputArgs() {
        assertNotNull(OsUtil.getClassPath());
        assertNotNull(OsUtil.getLibraryPath());
        assertNotNull(OsUtil.getJvmInputArgs());
    }
}
