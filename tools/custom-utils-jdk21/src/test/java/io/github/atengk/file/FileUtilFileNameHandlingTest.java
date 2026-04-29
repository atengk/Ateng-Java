package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilFileNameHandlingTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSanitizeAndNormalizeFileName() {
        assertEquals("bad_name.txt", FileUtil.sanitizeFileName("bad/name.txt"));
        assertEquals("a_b.txt", FileUtil.normalizeFileName("a b.txt"));
        assertEquals("file", FileUtil.getSafeFileName("..", "file"));
    }

    @Test
    void shouldGenerateNames() throws Exception {
        assertEquals("report.txt", FileUtil.generateFileName("report", ".txt"));
        assertTrue(FileUtil.generateTimestampFileName("log").endsWith(".log"));
        Files.writeString(tempDir.resolve("a.txt"), "x");
        Path unique = FileUtil.generateUniqueFileName(tempDir, "a.txt");
        assertEquals("a(1).txt", unique.getFileName().toString());
    }

    @Test
    void shouldAddSuffixAndTruncate() {
        assertEquals("pre-a.txt", FileUtil.addPrefix("a.txt", "pre-"));
        assertEquals("a-bak.txt", FileUtil.addSuffix("a.txt", "-bak"));
        assertEquals(8, FileUtil.truncateFileName("abcdef.txt", 8).length());
        assertThrows(IllegalArgumentException.class, () -> FileUtil.truncateFileName("a.txt", 0));
    }
}
