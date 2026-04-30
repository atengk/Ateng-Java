package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilFileSecurityTest {

    @TempDir
    Path tempDir;

    @Test
    void fileSecurityShouldValidateCommonFields() throws Exception {
        Path file = tempDir.resolve("demo.txt");
        Files.writeString(file, "hello");
        assertTrue(SecurityUtil.validateFileExtension("demo.TXT", Set.of("txt")));
        assertTrue(SecurityUtil.validateFileSize(10, 10));
        assertEquals("bad_name.txt", SecurityUtil.sanitizeFilename("../bad:name.txt"));
        assertTrue(SecurityUtil.isSafePath(tempDir, Path.of("a/b.txt")));
        assertFalse(SecurityUtil.isSafePath(tempDir, Path.of("../bad.txt")));
        assertEquals("68656c6c6f", SecurityUtil.checkMagicNumber(file));
        assertNotNull(SecurityUtil.detectMimeType(file));
    }

    @Test
    void executableShouldBeDetectedByName() throws Exception {
        Path script = tempDir.resolve("run.sh");
        Files.writeString(script, "echo ok");
        assertTrue(SecurityUtil.isExecutableFile(script));
        assertFalse(SecurityUtil.isExecutableFile(tempDir.resolve("missing.txt")));
    }

    @Test
    void fileSecurityShouldRejectInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.validateFileSize(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.sanitizeFilename(".."));
        assertFalse(SecurityUtil.validateFileExtension("demo", Set.of("txt")));
    }
}
