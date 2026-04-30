package io.github.atengk.zip;

import io.github.atengk.utils.zip.ZipUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilProgressTest {

    @TempDir
    Path tempDir;

    @Test
    void zipAndUnzipWithProgressShouldCallListener() throws Exception {
        Path source = ZipUtilTestSupport.sampleDir(tempDir);
        Path zip = tempDir.resolve("progress.zip");
        AtomicInteger calls = new AtomicInteger();
        ZipUtil.ProgressListener listener = new ZipUtil.ProgressListener() {
            @Override
            public void onProgress(String entryName, long processedBytes, long totalBytes, int percent) {
                calls.incrementAndGet();
            }
        };

        ZipUtil.zipWithProgress(source, zip, listener);
        ZipUtil.unzipWithProgress(zip, tempDir.resolve("out"), listener);

        assertTrue(calls.get() > 0);
    }

    @Test
    void estimateWorkloadShouldReturnFileSize() throws Exception {
        Path file = ZipUtilTestSupport.text(tempDir, "size.txt", "12345");
        assertEquals(5, ZipUtil.estimateWorkload(java.util.List.of(file)));
    }

    @Test
    void taskControlShouldHandleMissingTaskGracefully() {
        assertEquals(OptionalInt.empty(), ZipUtil.getProgress("missing"));
        assertDoesNotThrow(() -> ZipUtil.cancelTask("missing"));
        assertDoesNotThrow(() -> ZipUtil.pauseTask("missing"));
        assertDoesNotThrow(() -> ZipUtil.resumeTask("missing"));
    }
}
