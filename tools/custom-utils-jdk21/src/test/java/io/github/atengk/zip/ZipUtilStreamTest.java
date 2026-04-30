package io.github.atengk.zip;

import io.github.atengk.utils.zip.ZipUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilStreamTest {

    @TempDir
    Path tempDir;

    @Test
    void zipToStreamAndUnzipBytesShouldWork() throws Exception {
        Path source = ZipUtilTestSupport.sampleDir(tempDir);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ZipUtil.zipToStream(source, out);
        ZipUtil.unzipBytes(out.toByteArray(), tempDir.resolve("out"));

        assertEquals("A", ZipUtilTestSupport.read(tempDir.resolve("out/source/a.txt")));
    }

    @Test
    void zipDynamicEntriesShouldWork() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ZipUtil.zipTextToStream(Map.of("a.txt", "A", "b.txt", "B"), out);
        ZipUtil.unzip(new ByteArrayInputStream(out.toByteArray()), tempDir.resolve("text-out"));

        assertEquals("A", ZipUtilTestSupport.read(tempDir.resolve("text-out/a.txt")));
        assertEquals("B", ZipUtilTestSupport.read(tempDir.resolve("text-out/b.txt")));
    }

    @Test
    void extractFromStreamShouldExtractProvidedFormat() throws Exception {
        Path file = ZipUtilTestSupport.text(tempDir, "input.txt", "stream");
        Path zip = tempDir.resolve("input.zip");
        ZipUtil.zip(file, zip);

        try (var in = Files.newInputStream(zip)) {
            ZipUtil.extractFromStream(in, tempDir.resolve("stream-out"), ZipUtil.ArchiveFormat.ZIP);
        }

        assertEquals("stream", ZipUtilTestSupport.read(tempDir.resolve("stream-out/input.txt")));
    }
}
