package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileUtilUploadDownloadTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldBuildAndSaveUploadFile() throws Exception {
        Path uploadRoot = FileUtil.getUploadPath(tempDir.resolve("upload"));
        Path target = FileUtil.buildUploadPath(uploadRoot, "a.txt");
        FileUtil.saveUploadFile(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)), target, true);
        assertEquals("hello", Files.readString(target));
        assertEquals(target, FileUtil.checkUploadFile(target, 10, List.of("txt")));
    }

    @Test
    void shouldResolveDownloadSafely() throws Exception {
        Path file = Files.writeString(tempDir.resolve("download.txt"), "data");
        assertEquals(file.toAbsolutePath().normalize(), FileUtil.resolveDownloadFile(tempDir, "download.txt"));
        assertEquals(file.toAbsolutePath().normalize(), FileUtil.checkDownloadFile(tempDir, file));
        assertEquals("download.txt", FileUtil.getDownloadFileName(file));
        assertEquals("a%20b.txt", FileUtil.encodeDownloadFileName("a b.txt"));
    }

    @Test
    void shouldRejectUnsafeDownloadPath() {
        assertThrows(IllegalArgumentException.class, () -> FileUtil.resolveDownloadFile(tempDir, "..", "evil.txt"));
    }
}
