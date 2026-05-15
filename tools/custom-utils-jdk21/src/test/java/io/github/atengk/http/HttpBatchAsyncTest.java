package io.github.atengk.http;

import io.github.atengk.utils.http.HttpUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HttpBatchAsyncTest {

    @Test
    void shouldRunAsyncAndBatchRequests() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.text("/a", "a").text("/b", "b").start();
            assertEquals("a", HttpUtil.getAsync(server.url("/a")).join().bodyAsString());

            List<HttpUtil.HttpResult<byte[]>> results = HttpUtil.batchGet(List.of(server.url("/a"), server.url("/b")), 2);
            assertEquals(2, results.size());
            assertEquals(List.of("a", "b"), results.stream().map(HttpUtil.HttpResult::bodyAsString).toList());
        }
    }
}
