package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilFileCheckTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCheckFileAndDirectoryStatus() throws Exception {
        Path file = Files.writeString(tempDir.resolve("a.txt"), "");
        Path dir = Files.createDirectory(tempDir.resolve("dir"));
        assertTrue(FileUtil.exists(file));
        assertTrue(FileUtil.isFile(file));
        assertTrue(FileUtil.isDir(dir));
        assertTrue(FileUtil.isEmptyFile(file));
        assertTrue(FileUtil.isEmptyDir(dir));
        assertFalse(FileUtil.notExists(file));
    }

    @Test
    void shouldValidateFileName() {
        assertTrue(FileUtil.isValidFileName("report.txt"));
        assertFalse(FileUtil.isValidFileName("bad/name.txt"));
        assertFalse(FileUtil.isValidFileName(".."));
    }

    @Test
    void shouldFailWhenCheckingWrongType() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("dir"));
        assertThrows(IllegalArgumentException.class, () -> FileUtil.checkIsFile(dir));
        assertThrows(IllegalArgumentException.class, () -> FileUtil.checkExists(tempDir.resolve("missing")));
    }
}
