package io.github.atengk.http;

import io.github.atengk.utils.http.HttpUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HttpUploadTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldUploadFileWithFormFields() throws Exception {
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "hello upload", StandardCharsets.UTF_8);

        try (TestHttpServer server = new TestHttpServer()) {
            server.context("/upload", exchange -> {
                try {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    TestHttpServer.send(exchange, 200, "text/plain; charset=UTF-8", body.contains("hello upload") && body.contains("bizId") ? "ok" : "fail");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();

            HttpUtil.UploadOptions options = HttpUtil.uploadOptions(server.url("/upload"))
                    .formField("bizId", "1001")
                    .addFile("file", file, "a.txt", "text/plain")
                    .build();
            assertEquals("ok", HttpUtil.upload(options).bodyAsString());
        }
    }
}
