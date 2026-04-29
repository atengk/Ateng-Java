package io.github.atengk.zip;

import io.github.atengk.utils.ZipUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilFilterMapperTest {

    @TempDir
    Path tempDir;

    @Test
    void zipWithFilterShouldIncludeOnlyMatchedFiles() throws Exception {
        Path root = tempDir.resolve("filter");
        ZipUtilTestSupport.text(root, "a.txt", "A");
        ZipUtilTestSupport.text(root, "b.log", "B");
        Path zip = tempDir.resolve("filter.zip");

        ZipUtil.zipWithFilter(root, zip, ZipUtil.includeExtensions(Set.of("txt")));

        assertTrue(ZipUtil.containsEntry(zip, "filter/a.txt"));
        assertFalse(ZipUtil.containsEntry(zip, "filter/b.log"));
    }

    @Test
    void unzipWithFilterShouldExtractOnlyMatchedEntries() throws Exception {
        Path root = ZipUtilTestSupport.sampleDir(tempDir);
        Path zip = tempDir.resolve("unzip-filter.zip");
        Path out = tempDir.resolve("out");
        ZipUtil.zip(root, zip);

        ZipUtil.unzipWithFilter(zip, out, info -> info.name().endsWith("a.txt"));

        assertTrue(Files.exists(out.resolve("source/a.txt")));
        assertFalse(Files.exists(out.resolve("source/nested/b.txt")));
    }

    @Test
    void zipWithMapperShouldChangeEntryName() throws Exception {
        Path file = ZipUtilTestSupport.text(tempDir, "map.txt", "map");
        Path zip = tempDir.resolve("map.zip");

        ZipUtil.zipWithMapper(file, zip, (source, entryName) -> "mapped/" + entryName);

        assertTrue(ZipUtil.containsEntry(zip, "mapped/map.txt"));
    }

    @Test
    void stripRootDirectoryShouldExtractWithoutRoot() throws Exception {
        Path root = ZipUtilTestSupport.sampleDir(tempDir);
        Path zip = tempDir.resolve("strip.zip");
        Path out = tempDir.resolve("strip-out");
        ZipUtil.zip(root, zip);

        ZipUtil.stripRootDirectory(zip, out);

        assertTrue(Files.exists(out.resolve("a.txt")));
    }
}
