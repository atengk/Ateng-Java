package io.github.atengk.http;

import io.github.atengk.utils.http.HttpUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class HttpLoggingAuditTest {

    @Test
    void shouldMaskSensitiveHeadersAndAllowLogDisabledRequest() throws Exception {
        assertNotNull(Logger.getLogger(HttpUtil.class.getName()));
        assertEquals("******", HttpUtil.maskHeaders(Map.of("Cookie", "sid=1"), Set.of("cookie")).get("Cookie"));

        try (TestHttpServer server = new TestHttpServer()) {
            server.text("/log", "ok").start();
            HttpUtil.RequestOptions options = HttpUtil.defaultRequestOptions().withLogEnabled(false);
            assertEquals("ok", HttpUtil.get(server.url("/log"), options).bodyAsString());
        }
    }
}
