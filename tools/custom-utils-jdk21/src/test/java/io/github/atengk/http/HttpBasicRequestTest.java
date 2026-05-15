package io.github.atengk.http;

import io.github.atengk.utils.http.HttpUtil;
import org.junit.jupiter.api.Test;

import java.net.http.HttpRequest;

import static org.junit.jupiter.api.Assertions.*;

class HttpBasicRequestTest {

    @Test
    void shouldSendGetPostPutPatchDeleteHeadOptionsAndCustomRequest() throws Exception {
        try (TestHttpServer server = new TestHttpServer()) {
            server.context("/api", exchange -> TestHttpServer.send(exchange, 200, "text/plain; charset=UTF-8", exchange.getRequestMethod()))
                    .start();

            assertEquals("GET", HttpUtil.get(server.url("/api")));
            assertEquals("POST", HttpUtil.postJson(server.url("/api"), "{}").bodyAsString());
            assertEquals("PUT", HttpUtil.put(server.url("/api"), "a").bodyAsString());
            assertEquals("PATCH", HttpUtil.patch(server.url("/api"), "a").bodyAsString());
            assertEquals("DELETE", HttpUtil.delete(server.url("/api")).bodyAsString());
            assertEquals(200, HttpUtil.head(server.url("/api")).statusCode());
            assertEquals("OPTIONS", HttpUtil.options(server.url("/api")).bodyAsString());
            assertEquals("REPORT", HttpUtil.request("REPORT", server.url("/api"), HttpRequest.BodyPublishers.noBody(), HttpUtil.defaultRequestOptions()).bodyAsString());
        }
    }
}
