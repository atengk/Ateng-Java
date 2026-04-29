package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileUtilAssertionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRequireCommonConditions() throws Exception {
        Path file = Files.writeString(tempDir.resolve("a.txt"), "a");
        assertEquals(file, FileUtil.requireExists(file));
        assertEquals(file, FileUtil.requireFile(file));
        assertEquals(file, FileUtil.requireReadable(file));
        assertEquals(file, FileUtil.requireWritable(file));
        assertEquals(file, FileUtil.requireExtName(file, List.of("txt")));
        assertEquals(file, FileUtil.requireSizeLimit(file, 1));
        assertEquals(file, FileUtil.requireNonEmptyFile(file));
    }

    @Test
    void shouldRequireDirectoryAndSafePath() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("dir"));
        assertEquals(dir, FileUtil.requireDir(dir));
        Path target = dir.resolve("a.txt");
        assertEquals(target.toAbsolutePath().normalize(), FileUtil.requireSafePath(dir, target));
        assertEquals(target, FileUtil.requireNotExists(target));
    }

    @Test
    void shouldThrowForFailedRequirements() throws Exception {
        Path empty = Files.writeString(tempDir.resolve("empty.txt"), "");
        assertThrows(IllegalArgumentException.class, () -> FileUtil.requireNonEmptyFile(empty));
        assertThrows(IllegalArgumentException.class, () -> FileUtil.requireExists(tempDir.resolve("missing.txt")));
        assertThrows(IllegalArgumentException.class, () -> FileUtil.requireExtName(empty, List.of("pdf")));
    }
}
