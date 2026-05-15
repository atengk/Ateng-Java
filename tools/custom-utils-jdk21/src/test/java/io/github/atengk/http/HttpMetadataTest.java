package io.github.atengk.http;

import io.github.atengk.utils.http.HttpUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpMetadataTest {

    @Test
    void shouldResolveMetadataAndFileName() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.context("/meta.txt", exchange -> {
                exchange.getResponseHeaders().add("Content-Length", "4");
                exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"report.txt\"");
                TestHttpServer.send(exchange, 200, "text/plain", "data");
            }).start();

            HttpUtil.RemoteMetadata metadata = HttpUtil.metadata(server.url("/meta.txt"));
            assertEquals(200, metadata.statusCode());
            assertEquals("report.txt", HttpUtil.getFileName(server.url("/meta.txt"), Map.of("Content-Disposition", List.of("attachment; filename=\"report.txt\""))));
        }
    }
}
