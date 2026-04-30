package io.github.atengk.zip;

import io.github.atengk.utils.zip.ZipUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilFormatDetectTest {

    @TempDir
    Path tempDir;

    @Test
    void detectFormatShouldUseExtensionAndHeader() throws Exception {
        Path source = ZipUtilTestSupport.text(tempDir, "a.txt", "a");
        Path zip = tempDir.resolve("a.zip");
        ZipUtil.zip(source, zip);

        assertEquals(ZipUtil.ArchiveFormat.ZIP, ZipUtil.detectFormat(zip));
        assertEquals(ZipUtil.ArchiveFormat.ZIP, ZipUtil.detectFormat(new ByteArrayInputStream(new byte[]{'P', 'K', 3, 4})));
        assertTrue(ZipUtil.isZip(zip));
        assertTrue(ZipUtil.isArchive(zip));
        assertTrue(ZipUtil.isCompressed(zip));
    }

    @Test
    void compressorFormatShouldReturnExpectedValue() throws Exception {
        Path source = ZipUtilTestSupport.text(tempDir, "a.txt", "a");
        Path gz = tempDir.resolve("a.gz");
        ZipUtil.gzip(source, gz);

        assertTrue(ZipUtil.isGzip(gz));
        assertEquals(ZipUtil.CompressorFormat.GZIP, ZipUtil.getCompressorFormat(gz));
    }

    @Test
    void unknownBytesShouldReturnUnknown() {
        assertEquals(ZipUtil.ArchiveFormat.UNKNOWN, ZipUtil.detectFormat(new ByteArrayInputStream(new byte[]{1, 2, 3})));
    }
}
