package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilFileDeleteTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCleanAndDeleteRecursively() throws Exception {
        Path dir = tempDir.resolve("dir");
        Files.createDirectories(dir.resolve("sub"));
        Files.writeString(dir.resolve("sub/a.txt"), "a");
        FileUtil.cleanDir(dir);
        assertTrue(FileUtil.isEmptyDir(dir));
        FileUtil.deleteRecursively(dir);
        assertFalse(Files.exists(dir));
    }

    @Test
    void shouldDeleteFileAndEmptyDir() throws Exception {
        Path file = Files.writeString(tempDir.resolve("a.txt"), "a");
        FileUtil.deleteFile(file);
        assertFalse(Files.exists(file));
        Path dir = Files.createDirectory(tempDir.resolve("empty"));
        assertTrue(FileUtil.deleteEmptyDir(dir));
    }

    @Test
    void shouldDeleteQuietlyAndRejectNonEmptyDirDelete() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("non-empty"));
        Files.writeString(dir.resolve("a.txt"), "a");
        assertFalse(FileUtil.deleteQuietly(dir));
        assertThrows(Exception.class, () -> FileUtil.deleteEmptyDir(dir));
    }
}
