package io.github.atengk.http;

import io.github.atengk.utils.http.HttpUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpResponseParseTest {

    @Test
    void shouldParseResponseHeadersBodyAndContentType() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.context("/json", exchange -> {
                exchange.getResponseHeaders().add("X-Test", "ok");
                TestHttpServer.send(exchange, 200, "application/json; charset=UTF-8", "{\"ok\":true}");
            }).start();

            HttpUtil.RequestOptions options = HttpUtil.RequestOptions.defaults();
            HttpUtil.HttpResult<byte[]> result = HttpUtil.get(server.url("/json"), options);
            assertTrue(result.success());
            assertEquals("ok", result.firstHeader("X-Test").orElseThrow());
            assertEquals("{\"ok\":true}", result.bodyAsString());
            assertTrue(HttpUtil.isJson(result.firstHeader("Content-Type").orElseThrow()));
        }
    }

    @Test
    void shouldDetectCharsetAndBodyString() {
        assertEquals(StandardCharsets.UTF_8, HttpUtil.detectCharset("text/plain; charset=UTF-8"));
        assertEquals("你好", HttpUtil.bodyToString("你好".getBytes(StandardCharsets.UTF_8), Charset.forName("UTF-8")));
        assertEquals("1", HttpUtil.firstHeader(Map.of("Content-Length", List.of("1")), "content-length").orElseThrow());
    }
}
