package io.github.atengk.http;

import io.github.atengk.utils.http.HttpUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpConfigurationOptionsTest {

    @Test
    void shouldBuildRequestDownloadAndUploadOptions() throws Exception {
        HttpUtil.RequestOptions requestOptions = HttpUtil.defaultRequestOptions()
                .withTimeout(Duration.ofSeconds(1))
                .withHeader("X-App", "test")
                .withMaxResponseBytes(1024);
        assertEquals("test", requestOptions.headers().get("X-App"));

        HttpUtil.DownloadOptions downloadOptions = HttpUtil.downloadOptions("http://example.com/a.txt", java.nio.file.Path.of("/tmp"))
                .headers(Map.of("Referer", "http://example.com"))
                .fileName("a.txt")
                .overwriteStrategy(HttpUtil.OverwriteStrategy.OVERWRITE)
                .build();
        assertEquals("a.txt", downloadOptions.fileName());

        HttpUtil.UploadOptions uploadOptions = HttpUtil.uploadOptions("http://example.com/upload")
                .header("X-App", "test")
                .formField("name", "Ateng")
                .build();
        assertEquals("Ateng", uploadOptions.formFields().get("name"));
    }
}
