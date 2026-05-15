package io.github.atengk.http;

import io.github.atengk.utils.http.HttpUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpCacheConditionalRequestTest {

    @Test
    void shouldCreateConditionalHeadersAndHandleNotModified() throws Exception {
        Map<String, String> headers = HttpUtil.conditionalHeaders("v1", "Fri, 15 May 2026 00:00:00 GMT");
        assertEquals("v1", headers.get("If-None-Match"));

        try (TestHttpServer server = new TestHttpServer()) {
            server.context("/cache", exchange -> {
                if ("v1".equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
                    exchange.getResponseHeaders().add("ETag", "v1");
                    TestHttpServer.send(exchange, 304, null, "");
                } else {
                    exchange.getResponseHeaders().add("ETag", "v1");
                    TestHttpServer.send(exchange, 200, "text/plain", "data");
                }
            }).start();

            HttpUtil.CacheResult result = HttpUtil.conditionalGet(server.url("/cache"), "v1", null);
            assertTrue(result.notModified());
            assertEquals(304, result.result().statusCode());
        }
    }
}
