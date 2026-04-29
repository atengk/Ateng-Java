package io.github.atengk.zip;

import io.github.atengk.utils.ZipUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilBatchTest {

    @TempDir
    Path tempDir;

    @Test
    void zipEachUnzipEachAndExtractEachShouldWork() throws Exception {
        Path one = ZipUtilTestSupport.text(tempDir, "one.txt", "1");
        Path two = ZipUtilTestSupport.text(tempDir, "two.txt", "2");
        Path target = tempDir.resolve("batch");

        List<Path> zips = ZipUtil.zipEach(List.of(one, two), target);
        ZipUtil.unzipEach(zips, tempDir.resolve("unzip-each"));
        ZipUtil.extractEach(zips, tempDir.resolve("extract-each"));

        assertEquals(2, zips.size());
        assertTrue(Files.exists(tempDir.resolve("unzip-each/one/one.txt")));
        assertTrue(Files.exists(tempDir.resolve("extract-each/two/two.txt")));
    }

    @Test
    void zipByGroupAndMergeZipsShouldWork() throws Exception {
        Path one = ZipUtilTestSupport.text(tempDir, "g1.txt", "1");
        Path two = ZipUtilTestSupport.text(tempDir, "g2.txt", "2");

        Map<String, Path> grouped = ZipUtil.zipByGroup(Map.of("group-one", List.of(one), "group-two", List.of(two)), tempDir.resolve("groups"));
        Path merged = tempDir.resolve("merged.zip");
        ZipUtil.mergeZips(grouped.values(), merged);

        assertEquals(2, grouped.size());
        assertTrue(ZipUtil.containsEntry(merged, "g1.txt"));
        assertTrue(ZipUtil.containsEntry(merged, "g2.txt"));
    }

    @Test
    void splitBySizeAndArchiveOldFilesShouldWork() throws Exception {
        Path dir = tempDir.resolve("old");
        Path old = ZipUtilTestSupport.text(dir, "old.txt", "old");
        ZipUtilTestSupport.text(dir, "new.txt", "new");
        Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(Duration.ofDays(2))));

        List<Path> parts = ZipUtil.splitBySize(dir, tempDir.resolve("parts"), 3);
        Path archive = tempDir.resolve("old.zip");
        ZipUtil.archiveOldFiles(dir, Duration.ofDays(1), archive);

        assertFalse(parts.isEmpty());
        assertTrue(ZipUtil.containsEntry(archive, "old.txt"));
    }
}
