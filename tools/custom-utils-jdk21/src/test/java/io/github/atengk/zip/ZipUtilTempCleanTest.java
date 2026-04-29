package io.github.atengk.zip;

import io.github.atengk.utils.ZipUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilTempCleanTest {

    @TempDir
    Path tempDir;

    @Test
    void tempZipAndTempDirShouldBeCreatedAndCleaned() throws Exception {
        Path zip = ZipUtil.createTempZip("test-");
        Path dir = ZipUtil.createTempDir("test-");

        assertTrue(Files.exists(zip));
        assertTrue(Files.isDirectory(dir));
        ZipUtil.cleanTemp(zip);
        ZipUtil.cleanTemp(dir);
        assertFalse(Files.exists(zip));
        assertFalse(Files.exists(dir));
    }

    @Test
    void zipToTempAndExtractToTempShouldWork() throws Exception {
        Path source = ZipUtilTestSupport.text(tempDir, "temp.txt", "temp");

        Path zip = ZipUtil.zipToTemp(source);
        Path out = ZipUtil.extractToTemp(zip);

        assertEquals("temp", ZipUtilTestSupport.read(out.resolve("temp.txt")));
        ZipUtil.cleanQuietly(zip);
        ZipUtil.cleanQuietly(out);
    }

    @Test
    void deleteDirectoryAndRollbackShouldBeIdempotent() throws Exception {
        Path dir = tempDir.resolve("delete");
        ZipUtilTestSupport.text(dir, "a.txt", "a");

        ZipUtil.deleteDirectory(dir);
        ZipUtil.rollbackOnFailure(dir);

        assertFalse(Files.exists(dir));
    }
}
