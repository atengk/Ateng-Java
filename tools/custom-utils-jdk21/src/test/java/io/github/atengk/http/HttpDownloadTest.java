package io.github.atengk.http;

import io.github.atengk.utils.http.HttpUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class HttpDownloadTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldDownloadFileWithDigestAndProgress() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.text("/file.txt", "hello download").start();
            AtomicLong progress = new AtomicLong();

            HttpUtil.DownloadResult result = HttpUtil.download(HttpUtil.downloadOptions(server.url("/file.txt"), tempDir)
                    .fileName("file.txt")
                    .calculateMd5(true)
                    .calculateSha256(true)
                    .progressListener(event -> progress.set(event.transferred()))
                    .build());

            assertTrue(result.success());
            assertEquals("hello download", Files.readString(result.filePath()));
            assertNotNull(result.md5());
            assertNotNull(result.sha256());
            assertTrue(progress.get() > 0);
        }
    }

    @Test
    void shouldDownloadBytesAndRejectLargeFile() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.text("/bytes", "abc").start();
            assertArrayEquals("abc".getBytes(), HttpUtil.downloadBytes(server.url("/bytes")));

            HttpUtil.DownloadOptions options = HttpUtil.downloadOptions(server.url("/bytes"), tempDir)
                    .fileName("small.txt")
                    .maxFileSize(1)
                    .build();
            assertThrows(HttpUtil.HttpResponseTooLargeException.class, () -> HttpUtil.download(options));
        }
    }
}
