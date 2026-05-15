package io.github.atengk.http;

import io.github.atengk.utils.http.HttpUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class HttpRetryAndTimeoutTest {

    @Test
    void shouldRetryIdempotentRequestByStatusCode() throws Exception {
        AtomicInteger count = new AtomicInteger();
        try (TestHttpServer server = new TestHttpServer()) {
            server.context("/retry", exchange -> {
                int current = count.incrementAndGet();
                TestHttpServer.send(exchange, current < 2 ? 500 : 200, "text/plain; charset=UTF-8", current < 2 ? "bad" : "ok");
            }).start();

            HttpUtil.RetryOptions retry = HttpUtil.RetryOptions.defaults().withMaxRetries(2).withInterval(Duration.ofMillis(1));
            HttpUtil.RequestOptions options = HttpUtil.defaultRequestOptions().withRetryOptions(retry);
            assertEquals("ok", HttpUtil.get(server.url("/retry"), options).bodyAsString());
            assertEquals(2, count.get());
        }
    }

    @Test
    void shouldThrowWhenResponseTooLarge() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.text("/large", "abcdef").start();
            HttpUtil.RequestOptions options = HttpUtil.defaultRequestOptions().withMaxResponseBytes(2);
            assertThrows(HttpUtil.HttpResponseTooLargeException.class, () -> HttpUtil.get(server.url("/large"), options));
        }
    }
}
