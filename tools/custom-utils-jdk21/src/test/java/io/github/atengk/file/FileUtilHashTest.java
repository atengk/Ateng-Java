package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilHashTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCalculateHashAndVerify() throws Exception {
        Path file = Files.writeString(tempDir.resolve("hash.txt"), "abc");
        assertEquals("900150983cd24fb0d6963f7d28e17f72", FileUtil.md5(file));
        assertTrue(FileUtil.verifyMd5(file, "900150983cd24fb0d6963f7d28e17f72"));
        assertTrue(FileUtil.verifySha256(file, FileUtil.sha256(file)));
        assertEquals(40, FileUtil.sha1(file).length());
    }

    @Test
    void shouldCompareContents() throws Exception {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "same");
        Path b = Files.writeString(tempDir.resolve("b.txt"), "same");
        Path c = Files.writeString(tempDir.resolve("c.txt"), "diff");
        assertTrue(FileUtil.sameContent(a, b));
        assertEquals(0, FileUtil.compareContent(a, b));
        assertNotEquals(0, FileUtil.compareContent(a, c));
    }

    @Test
    void shouldCalculateChecksum() throws Exception {
        Path file = Files.writeString(tempDir.resolve("sum.txt"), "abc");
        assertTrue(FileUtil.checksumCRC32(file) > 0);
        assertTrue(FileUtil.checksumAdler32(file) > 0);
        assertThrows(IllegalArgumentException.class, () -> FileUtil.hash(file, "NO_SUCH_ALGO"));
    }
}
