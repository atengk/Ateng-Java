package io.github.atengk.file;

import io.github.atengk.utils.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilZipTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldZipAndUnzipDirectory() throws Exception {
        Path source = Files.createDirectories(tempDir.resolve("src/sub"));
        Files.writeString(source.resolve("a.txt"), "a");
        Path zip = tempDir.resolve("out.zip");
        FileUtil.zipDir(tempDir.resolve("src"), zip);
        assertTrue(FileUtil.checkZipSafe(zip, tempDir.resolve("unzip")));
        assertTrue(FileUtil.listZipEntries(zip).contains("sub/a.txt"));
        FileUtil.unzip(zip, tempDir.resolve("unzip"));
        assertEquals("a", Files.readString(tempDir.resolve("unzip/sub/a.txt")));
    }

    @Test
    void shouldZipFileAndExtractEntry() throws Exception {
        Path file = Files.writeString(tempDir.resolve("a.txt"), "abc");
        Path zip = tempDir.resolve("file.zip");
        FileUtil.zipFile(file, zip);
        Path extracted = tempDir.resolve("extracted.txt");
        FileUtil.extractZipEntry(zip, "a.txt", extracted);
        assertEquals("abc", Files.readString(extracted));
    }

    @Test
    void shouldPreventZipSlip() throws Exception {
        assertThrows(Exception.class, () -> FileUtil.preventZipSlip(tempDir, new ZipEntry("../evil.txt")));
    }
}
