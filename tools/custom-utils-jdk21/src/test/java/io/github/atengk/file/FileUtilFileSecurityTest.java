package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileUtilFileSecurityTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCheckSecurityRules() throws Exception {
        Path file = Files.writeString(tempDir.resolve("safe.txt"), "123");
        assertEquals(file.toAbsolutePath().normalize(), FileUtil.resolveSafePath(tempDir, "safe.txt"));
        assertEquals(file.toAbsolutePath().normalize(), FileUtil.checkPathSafe(tempDir, file));
        assertEquals(file, FileUtil.checkExtNameAllowed(file, List.of("txt")));
        assertEquals(file, FileUtil.checkSizeLimit(file, 10));
    }

    @Test
    void shouldRejectUnsafeRules() throws Exception {
        Path file = Files.writeString(tempDir.resolve("safe.txt"), "123");
        assertThrows(IllegalArgumentException.class, () -> FileUtil.resolveSafePath(tempDir, "..", "x.txt"));
        assertThrows(IllegalArgumentException.class, () -> FileUtil.checkExtNameAllowed(file, List.of("pdf")));
        assertThrows(IllegalArgumentException.class, () -> FileUtil.checkSizeLimit(file, 1));
    }

    @Test
    void shouldCheckReadableWritableAndNameSafety() throws Exception {
        Path file = Files.writeString(tempDir.resolve("a.txt"), "a");
        assertEquals(file, FileUtil.checkReadable(file));
        assertEquals(file, FileUtil.checkWritable(file));
        assertEquals("abc.txt", FileUtil.checkFileNameSafe("abc.txt"));
        assertThrows(IllegalArgumentException.class, () -> FileUtil.checkFileNameSafe("bad/name.txt"));
    }
}
