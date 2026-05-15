package io.github.atengk.http;

import io.github.atengk.utils.http.HttpUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HttpHeaderCookieAuthTest {

    @Test
    void shouldCreateAuthCookieAndMaskedHeaders() throws Exception {
        assertEquals("Bearer abc", HttpUtil.bearerAuth("abc").get("Authorization"));
        assertTrue(HttpUtil.basicAuth("ateng", "123").get("Authorization").startsWith("Basic "));
        assertEquals("a=1; b=2", HttpUtil.cookieHeader(Map.of("a", "1", "b", "2")));

        Map<String, String> merged = HttpUtil.mergeHeaders(Map.of("A", "1"), Map.of("B", "2"));
        assertEquals("1", merged.get("A"));
        assertEquals("2", merged.get("B"));

        Map<String, String> masked = HttpUtil.maskHeaders(Map.of("Authorization", "secret", "X", "1"), Set.of("authorization"));
        assertEquals("******", masked.get("Authorization"));
        assertEquals("1", masked.get("X"));
    }

    @Test
    void shouldSendCustomHeaderAndCookie() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.context("/header", exchange -> {
                String token = exchange.getRequestHeaders().getFirst("Authorization");
                String cookie = exchange.getRequestHeaders().getFirst("Cookie");
                TestHttpServer.send(exchange, 200, "text/plain; charset=UTF-8", token + "|" + cookie);
            }).start();

            HttpUtil.RequestOptions options = HttpUtil.defaultRequestOptions()
                    .withHeaders(HttpUtil.mergeHeaders(HttpUtil.bearerAuth("abc"), Map.of("Cookie", HttpUtil.cookieHeader(Map.of("sid", "1")))));
            assertEquals("Bearer abc|sid=1", HttpUtil.get(server.url("/header"), options).bodyAsString());
        }
    }
}
