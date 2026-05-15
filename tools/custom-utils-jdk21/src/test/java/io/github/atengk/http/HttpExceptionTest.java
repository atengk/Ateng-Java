package io.github.atengk.http;

import io.github.atengk.utils.http.HttpUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpExceptionTest {

    @Test
    void shouldExposeCustomExceptions() {
        HttpUtil.HttpStatusException statusException = new HttpUtil.HttpStatusException("bad", 500, "err".getBytes());
        assertEquals(500, statusException.statusCode());
        assertArrayEquals("err".getBytes(), statusException.responseBody());

        assertThrows(HttpUtil.HttpSecurityException.class, () -> HttpUtil.validateUrlBySecurity("not-url", HttpUtil.SecurityOptions.defaults()));
    }
}
