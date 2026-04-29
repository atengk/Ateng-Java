package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilDirectoryTraversalTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldListAndWalkDirectory() throws Exception {
        Files.createDirectories(tempDir.resolve("a/b"));
        Files.writeString(tempDir.resolve("a/file.txt"), "x");
        Files.writeString(tempDir.resolve("a/b/file.log"), "x");
        assertEquals(1, FileUtil.listDirs(tempDir.resolve("a")).size());
        assertEquals(1, FileUtil.listFiles(tempDir.resolve("a")).size());
        assertTrue(FileUtil.walk(tempDir.resolve("a")).size() >= 3);
        assertEquals(2, FileUtil.walkFiles(tempDir.resolve("a")).size());
    }

    @Test
    void shouldFindAndFilterFiles() throws Exception {
        Files.writeString(tempDir.resolve("a.txt"), "a");
        Files.writeString(tempDir.resolve("b.log"), "b");
        assertEquals(1, FileUtil.findByExtName(tempDir, java.util.List.of("txt")).size());
        assertEquals(1, FileUtil.findByName(tempDir, "b.log").size());
        assertEquals(2, FileUtil.countFiles(tempDir));
    }

    @Test
    void shouldForEachFile() throws Exception {
        Files.writeString(tempDir.resolve("a.txt"), "a");
        Files.writeString(tempDir.resolve("b.txt"), "b");
        AtomicInteger count = new AtomicInteger();
        FileUtil.forEachFile(tempDir, path -> count.incrementAndGet());
        assertEquals(2, count.get());
    }
}
