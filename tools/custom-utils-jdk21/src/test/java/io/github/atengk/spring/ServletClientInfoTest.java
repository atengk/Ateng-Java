package io.github.atengk.spring;

import io.github.atengk.utils.spring.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServletClientInfoTest {

    @AfterEach
    void tearDown() {
        ServletTestSupport.clear();
    }

    @Test
    void shouldReadClientInfo() {
        ServletTestSupport.ServletPair pair = ServletTestSupport.bind();
        pair.request().addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        pair.request().addHeader("X-Requested-With", "XMLHttpRequest");
        pair.request().setRemoteAddr("127.0.0.2");
        pair.request().setRemoteHost("client.local");
        pair.request().setRemotePort(55000);
        pair.request().setScheme("https");
        pair.request().setServerName("api.example.com");
        pair.request().setServerPort(443);
        pair.request().setProtocol("HTTP/2");
        pair.request().setSecure(true);

        assertEquals("10.0.0.1", SpringUtil.getClientIp());
        assertEquals(55000, SpringUtil.getClientPort());
        assertEquals("127.0.0.2", SpringUtil.getRemoteAddr());
        assertEquals("client.local", SpringUtil.getRemoteHost());
        assertEquals("https", SpringUtil.getScheme());
        assertEquals("api.example.com", SpringUtil.getServerName());
        assertEquals(443, SpringUtil.getServerPort());
        assertEquals("HTTP/2", SpringUtil.getProtocol());
        assertTrue(SpringUtil.isAjaxRequest());
        assertTrue(SpringUtil.isSecureRequest());
    }

    @Test
    void shouldReturnNullWithoutRequest() {
        assertNull(SpringUtil.getClientIp());
        assertNull(SpringUtil.getRemoteAddr());
        assertFalse(SpringUtil.isAjaxRequest());
        assertFalse(SpringUtil.isSecureRequest());
    }
}
