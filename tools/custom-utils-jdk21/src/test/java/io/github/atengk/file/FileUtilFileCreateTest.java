package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilFileCreateTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateFilesAndDirectories() throws Exception {
        Path dir = tempDir.resolve("a/b");
        FileUtil.mkdirs(dir);
        assertTrue(Files.isDirectory(dir));
        Path file = dir.resolve("test.txt");
        FileUtil.createFileIfAbsent(file);
        assertTrue(Files.isRegularFile(file));
        FileUtil.touch(file);
        assertTrue(Files.exists(file));
    }

    @Test
    void shouldCreateTempFileAndDir() throws Exception {
        Path file = FileUtil.createTempFile(tempDir, "a-", ".tmp");
        Path dir = FileUtil.createTempDir(tempDir, "d-");
        assertTrue(Files.isRegularFile(file));
        assertTrue(Files.isDirectory(dir));
    }

    @Test
    void shouldRejectCreatingDirectoryOverFile() throws Exception {
        Path file = Files.writeString(tempDir.resolve("exists"), "x");
        assertThrows(Exception.class, () -> FileUtil.ensureDir(file));
    }
}
