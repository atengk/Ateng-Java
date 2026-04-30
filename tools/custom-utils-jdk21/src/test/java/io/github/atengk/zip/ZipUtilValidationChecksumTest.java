package io.github.atengk.zip;

import io.github.atengk.utils.zip.exception.ZipUtilException;
import io.github.atengk.utils.zip.ZipUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilValidationChecksumTest {

    @TempDir
    Path tempDir;

    @Test
    void validateArchiveAndVerifyMethodsShouldWork() throws Exception {
        Path source = ZipUtilTestSupport.sampleDir(tempDir);
        Path zip = tempDir.resolve("validate.zip");
        ZipUtil.zip(source, zip);

        assertTrue(ZipUtil.testArchive(zip));
        assertTrue(ZipUtil.testZip(zip));
        assertTrue(ZipUtil.validateArchive(zip).valid());
        assertTrue(ZipUtil.validateZip(zip).valid());
        assertDoesNotThrow(() -> ZipUtil.verifyEntryExists(zip, "source/a.txt"));
        assertDoesNotThrow(() -> ZipUtil.verifyEntryCount(zip, ZipUtil.countEntries(zip)));
        assertThrows(ZipUtilException.class, () -> ZipUtil.verifyEntryExists(zip, "missing.txt"));
    }

    @Test
    void checksumMethodsShouldReturnExpectedLength() throws Exception {
        Path file = ZipUtilTestSupport.text(tempDir, "checksum.txt", "checksum");

        assertFalse(ZipUtil.crc32(file).isBlank());
        assertEquals(32, ZipUtil.md5(file).length());
        assertEquals(64, ZipUtil.sha256(file).length());
        assertDoesNotThrow(() -> ZipUtil.verifyChecksum(file, ZipUtil.sha256(file)));
        assertThrows(ZipUtilException.class, () -> ZipUtil.verifyChecksum(file, "bad"));
    }
}
