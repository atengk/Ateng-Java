package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilCopyMoveTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCopyAndMoveFile() throws Exception {
        Path source = Files.writeString(tempDir.resolve("source.txt"), "hello");
        Path copied = tempDir.resolve("target/copied.txt");
        FileUtil.copyFile(source, copied);
        assertEquals("hello", Files.readString(copied));
        Path moved = tempDir.resolve("target/moved.txt");
        FileUtil.moveFile(copied, moved);
        assertFalse(Files.exists(copied));
        assertTrue(Files.exists(moved));
    }

    @Test
    void shouldCopyDirectoryAndRename() throws Exception {
        Path sourceDir = Files.createDirectories(tempDir.resolve("src/sub"));
        Files.writeString(sourceDir.resolve("a.txt"), "a");
        Path targetDir = tempDir.resolve("copy");
        FileUtil.copyDir(tempDir.resolve("src"), targetDir);
        assertTrue(Files.exists(targetDir.resolve("sub/a.txt")));
        Path renamed = FileUtil.rename(targetDir.resolve("sub/a.txt"), "b.txt");
        assertEquals("b.txt", renamed.getFileName().toString());
    }

    @Test
    void shouldBackupWithTimestamp() throws Exception {
        Path file = Files.writeString(tempDir.resolve("config.yml"), "server: test");
        Path backup = FileUtil.backupWithTimestamp(file);
        assertTrue(Files.exists(backup));
        assertTrue(backup.getFileName().toString().startsWith("config-"));
    }
}
