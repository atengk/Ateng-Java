package io.github.atengk.zip;

import io.github.atengk.exception.ZipUtilException;
import io.github.atengk.utils.ZipUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilBasicUnzipTest {

    @TempDir
    Path tempDir;

    @Test
    void unzipShouldExtractFiles() throws Exception {
        Path source = ZipUtilTestSupport.sampleDir(tempDir);
        Path zip = tempDir.resolve("basic.zip");
        Path out = tempDir.resolve("out");
        ZipUtil.zip(source, zip);

        ZipUtil.unzip(zip, out);

        assertEquals("A", ZipUtilTestSupport.read(out.resolve("source/a.txt")));
        assertEquals("B", ZipUtilTestSupport.read(out.resolve("source/nested/b.txt")));
    }

    @Test
    void unzipEntryShouldExtractSingleFile() throws Exception {
        Path source = ZipUtilTestSupport.sampleDir(tempDir);
        Path zip = tempDir.resolve("entry.zip");
        Path target = tempDir.resolve("single.txt");
        ZipUtil.zip(source, zip);

        ZipUtil.unzipEntry(zip, "source/a.txt", target);

        assertEquals("A", ZipUtilTestSupport.read(target));
    }

    @Test
    void unzipEntriesShouldExtractSelectedFiles() throws Exception {
        Path source = ZipUtilTestSupport.sampleDir(tempDir);
        Path zip = tempDir.resolve("selected.zip");
        Path out = tempDir.resolve("selected");
        ZipUtil.zip(source, zip);

        ZipUtil.unzipEntries(zip, Set.of("source/a.txt"), out);

        assertTrue(Files.exists(out.resolve("source/a.txt")));
        assertFalse(Files.exists(out.resolve("source/nested/b.txt")));
    }

    @Test
    void unzipMissingFileShouldThrow() {
        assertThrows(ZipUtilException.class, () -> ZipUtil.unzip(tempDir.resolve("missing.zip"), tempDir.resolve("out")));
    }
}
