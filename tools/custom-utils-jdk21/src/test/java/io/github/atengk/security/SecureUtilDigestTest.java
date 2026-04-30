package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilDigestTest {

    @TempDir
    Path tempDir;

    @Test
    void digestShouldReturnKnownValues() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", SecurityUtil.sha256Hex(""));
        assertEquals(64, SecurityUtil.sha256Hex("abc").length());
        assertEquals(128, SecurityUtil.sha512Hex("abc").length());
        assertEquals(40, SecurityUtil.sha1Hex("abc").length());
        assertEquals(32, SecurityUtil.md5Hex("abc").length());
        assertTrue(SecurityUtil.verifyDigest("abc".getBytes(), SecurityUtil.sha256Hex("abc"), "SHA-256"));
    }

    @Test
    void fileDigestShouldWork() throws Exception {
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "abc");
        assertEquals(SecurityUtil.sha256Hex("abc"), SecurityUtil.sha256File(file));
        assertTrue(SecurityUtil.verifyFileDigest(file, SecurityUtil.sha256Hex("abc"), "SHA-256"));
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.digest("abc".getBytes(), "NOPE"));
    }

    @Test
    void constantTimeEqualsShouldHandleNull() {
        assertTrue(SecurityUtil.constantTimeEquals("a", "a"));
        assertFalse(SecurityUtil.constantTimeEquals("a", "b"));
        assertFalse(SecurityUtil.constantTimeEquals(null, "b"));
    }
}
