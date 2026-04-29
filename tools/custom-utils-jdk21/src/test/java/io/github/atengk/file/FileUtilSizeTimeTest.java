package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilSizeTimeTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCalculateSize() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("dir"));
        Files.writeString(dir.resolve("a.txt"), "12345");
        assertEquals(5, FileUtil.sizeOfDir(dir));
        assertEquals(5, FileUtil.sizeOf(dir));
        assertEquals("1.00 KB", FileUtil.readableSize(1024));
    }

    @Test
    void shouldHandleTimes() throws Exception {
        Path file = Files.writeString(tempDir.resolve("time.txt"), "x");
        Instant old = Instant.now().minus(Duration.ofDays(1));
        FileUtil.setLastModifiedTime(file, old);
        assertTrue(FileUtil.isModifiedBefore(file, Instant.now()));
        assertTrue(FileUtil.isExpired(file, Duration.ofMillis(1)));
        assertNotNull(FileUtil.creationTime(file));
        assertNotNull(FileUtil.lastAccessTime(file));
    }

    @Test
    void shouldRejectNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> FileUtil.readableSize(-1));
    }
}
