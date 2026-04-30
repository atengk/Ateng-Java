package io.github.atengk.zip;

import io.github.atengk.utils.zip.ZipUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilSplitZipTest {

    @TempDir
    Path tempDir;

    @Test
    void splitZipShouldCreatePartsAndExtract() throws Exception {
        Path file = tempDir.resolve("large.bin");
        byte[] data = new byte[200_000];
        new Random(1).nextBytes(data);
        Files.write(file, data);
        Path zip = tempDir.resolve("split.zip");
        Path out = tempDir.resolve("split-out");

        ZipUtil.zipSplit(file, zip, 65_536);

        assertTrue(ZipUtil.isSplitZip(zip));
        assertTrue(ZipUtil.validateSplitParts(zip));
        assertFalse(ZipUtil.listSplitParts(zip).isEmpty());
        ZipUtil.unzipSplit(zip, out);
        assertArrayEquals(data, Files.readAllBytes(out.resolve("large.bin")));
    }

    @Test
    void splitZipShouldRejectSmallSplitSize() throws Exception {
        Path file = ZipUtilTestSupport.text(tempDir, "a.txt", "a");
        assertThrows(IllegalArgumentException.class, () -> ZipUtil.zipSplit(file, tempDir.resolve("bad.zip"), 1));
    }
}
