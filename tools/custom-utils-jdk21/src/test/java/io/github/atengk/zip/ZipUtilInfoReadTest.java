package io.github.atengk.zip;

import io.github.atengk.utils.ZipUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilInfoReadTest {

    @TempDir
    Path tempDir;

    @Test
    void infoMethodsShouldReadArchiveMetadata() throws Exception {
        Path source = ZipUtilTestSupport.sampleDir(tempDir);
        Path zip = tempDir.resolve("info.zip");
        ZipUtil.zip(source, zip);

        assertTrue(ZipUtil.countEntries(zip) >= 2);
        assertTrue(ZipUtil.getTotalUncompressedSize(zip) > 0);
        assertTrue(ZipUtil.getTotalCompressedSize(zip) >= 0);
        assertTrue(ZipUtil.getEntry(zip, "source/a.txt").isPresent());
        assertEquals(ZipUtil.ArchiveFormat.ZIP, ZipUtil.getArchiveInfo(zip).format());
    }

    @Test
    void findEntriesShouldReturnMatchedEntries() throws Exception {
        Path source = ZipUtilTestSupport.sampleDir(tempDir);
        Path zip = tempDir.resolve("find.zip");
        ZipUtil.zip(source, zip);

        assertEquals(1, ZipUtil.findEntries(zip, info -> info.name().endsWith("a.txt")).size());
    }
}
