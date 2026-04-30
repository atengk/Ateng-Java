package io.github.atengk.zip;

import io.github.atengk.utils.zip.exception.ZipUtilException;
import io.github.atengk.utils.zip.ZipUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilSecurityTest {

    @TempDir
    Path tempDir;

    @Test
    void validateEntryNameShouldRejectZipSlip() {
        assertThrows(ZipUtilException.class, () -> ZipUtil.validateEntryName("../evil.txt"));
        assertTrue(ZipUtil.isZipSlipEntry("../evil.txt"));
        assertEquals("evil.txt", ZipUtil.sanitizeEntryName("../evil.txt").replace("_/", ""));
    }

    @Test
    void safeResolveShouldKeepTargetInsideDirectory() {
        Path resolved = ZipUtil.safeResolve(tempDir.resolve("out"), "a/b.txt");
        assertTrue(resolved.normalize().startsWith(tempDir.resolve("out").toAbsolutePath().normalize()));
    }

    @Test
    void securityChecksShouldValidateArchive() throws Exception {
        Path source = ZipUtilTestSupport.text(tempDir, "a.txt", "a");
        Path zip = tempDir.resolve("safe.zip");
        ZipUtil.zip(source, zip);

        assertDoesNotThrow(() -> ZipUtil.checkMaxEntryCount(zip, 10));
        assertDoesNotThrow(() -> ZipUtil.checkMaxUncompressedSize(zip, 10));
        assertDoesNotThrow(() -> ZipUtil.checkCompressionRatio(zip, BigDecimal.valueOf(100)));
        assertDoesNotThrow(() -> ZipUtil.checkAllowedExtensions(zip, Set.of("txt")));
        assertThrows(ZipUtilException.class, () -> ZipUtil.checkBlockedExtensions(zip, Set.of("txt")));
    }

    @Test
    void sanitizeFileNameShouldReplaceUnsafeCharacters() {
        assertEquals("a_b_c.txt", ZipUtil.sanitizeFileName("a:b/c.txt"));
    }
}
