package io.github.atengk.spring;

import io.github.atengk.utils.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServletRequestHeaderTest {

    @AfterEach
    void tearDown() {
        ServletTestSupport.clear();
    }

    @Test
    void shouldReadHeaders() {
        ServletTestSupport.ServletPair pair = ServletTestSupport.bind();
        pair.request().addHeader("Authorization", "Bearer token-123");
        pair.request().addHeader("User-Agent", "JUnit");
        pair.request().addHeader("X-Test", "a");
        pair.request().addHeader("X-Test", "b");
        pair.request().addHeader("Referer", "https://example.com/from");
        pair.request().addHeader("Origin", "https://example.com");
        pair.request().addHeader("Host", "example.com");
        pair.request().setContentType("application/json");

        assertEquals("Bearer token-123", SpringUtil.getAuthorization());
        assertEquals("token-123", SpringUtil.getBearerToken());
        assertEquals("JUnit", SpringUtil.getUserAgent());
        assertEquals(List.of("a", "b"), SpringUtil.getHeaders("X-Test"));
        assertTrue(SpringUtil.getHeaderNames().contains("Authorization"));
        assertEquals("application/json", SpringUtil.getContentType());
        assertEquals("https://example.com/from", SpringUtil.getReferer());
        assertEquals("https://example.com", SpringUtil.getOrigin());
        assertEquals("example.com", SpringUtil.getHost());
    }

    @Test
    void shouldHandleHeaderEdges() {
        ServletTestSupport.bind();
        assertEquals("d", SpringUtil.getHeader("missing", "d"));
        assertNull(SpringUtil.getBearerToken());
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.getHeader(" "));
    }
}
