package io.github.atengk.zip;

import io.github.atengk.exception.ZipUtilException;
import io.github.atengk.utils.ZipUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilBasicZipTest {

    @TempDir
    Path tempDir;

    @Test
    void zipDirectoryShouldCreateZipAndEntries() throws Exception {
        Path source = ZipUtilTestSupport.sampleDir(tempDir);
        Path zip = tempDir.resolve("basic.zip");

        ZipUtil.zip(source, zip);

        assertTrue(Files.isRegularFile(zip));
        assertTrue(ZipUtil.containsEntry(zip, "source/a.txt"));
        assertTrue(ZipUtil.containsEntry(zip, "source/nested/b.txt"));
    }

    @Test
    void zipMultipleFilesShouldCreateEntries() throws Exception {
        Path one = ZipUtilTestSupport.text(tempDir, "one.txt", "1");
        Path two = ZipUtilTestSupport.text(tempDir, "two.txt", "2");
        Path zip = tempDir.resolve("multi.zip");

        ZipUtil.zip(List.of(one, two), zip);

        assertEquals(2, ZipUtil.listFileEntries(zip).size());
    }

    @Test
    void zipEmptySourcesShouldThrow() {
        Path zip = tempDir.resolve("empty.zip");
        assertThrows(IllegalArgumentException.class, () -> ZipUtil.zip(List.of(), zip));
    }

    @Test
    void zipMissingSourceShouldThrow() {
        assertThrows(ZipUtilException.class, () -> ZipUtil.zip(tempDir.resolve("missing"), tempDir.resolve("x.zip")));
    }
}
