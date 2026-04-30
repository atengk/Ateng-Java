package io.github.atengk.spring;

import io.github.atengk.utils.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ServletRequestBodyTest {

    @AfterEach
    void tearDown() {
        ServletTestSupport.clear();
    }

    @Test
    void shouldReadBody() throws Exception {
        ServletTestSupport.ServletPair pair = ServletTestSupport.bind();
        pair.request().setContent("hello".getBytes(StandardCharsets.UTF_8));

        assertEquals("hello", SpringUtil.getRequestBody());

        ServletTestSupport.clear();
        pair = ServletTestSupport.bind();
        pair.request().setContent("stream".getBytes(StandardCharsets.UTF_8));
        assertNotNull(SpringUtil.getInputStream());

        ServletTestSupport.clear();
        pair = ServletTestSupport.bind();
        pair.request().setContent("reader".getBytes(StandardCharsets.UTF_8));
        assertNotNull(SpringUtil.getReader());
    }

    @Test
    void shouldReturnNullWithoutRequest() {
        assertNull(SpringUtil.getRequestBodyAsString());
        assertNull(SpringUtil.getRequestBodyAsBytes());
        assertNull(SpringUtil.getInputStream());
        assertNull(SpringUtil.getReader());
    }
}
