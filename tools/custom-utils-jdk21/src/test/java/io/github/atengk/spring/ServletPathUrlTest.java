package io.github.atengk.spring;

import io.github.atengk.utils.spring.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServletPathUrlTest {

    @AfterEach
    void tearDown() {
        ServletTestSupport.clear();
    }

    @Test
    void shouldReadPathAndUrl() {
        ServletTestSupport.ServletPair pair = ServletTestSupport.bind();
        pair.request().setMethod("POST");
        pair.request().setScheme("https");
        pair.request().setServerName("example.com");
        pair.request().setServerPort(443);
        pair.request().setContextPath("/ctx");
        pair.request().setServletPath("/api");
        pair.request().setPathInfo("/users");
        pair.request().setRequestURI("/ctx/api/users");
        pair.request().setQueryString("a=1");

        assertEquals("/ctx/api/users", SpringUtil.getRequestUri());
        assertTrue(SpringUtil.getRequestUrl().endsWith("/ctx/api/users"));
        assertEquals("a=1", SpringUtil.getQueryString());
        assertTrue(SpringUtil.getFullUrl().endsWith("/ctx/api/users?a=1"));
        assertEquals("/ctx", SpringUtil.getContextPath());
        assertEquals("/api", SpringUtil.getServletPath());
        assertEquals("/users", SpringUtil.getPathInfo());
        assertEquals("POST", SpringUtil.getMethod());
        assertEquals("POST", SpringUtil.getRequestMethod());
        assertTrue(SpringUtil.isPost());
        assertFalse(SpringUtil.isGet());
        assertFalse(SpringUtil.isPut());
        assertFalse(SpringUtil.isDelete());
    }

    @Test
    void shouldDetectHttpMethods() {
        ServletTestSupport.ServletPair pair = ServletTestSupport.bind();
        pair.request().setMethod("GET");
        assertTrue(SpringUtil.isGet());
        pair.request().setMethod("PUT");
        assertTrue(SpringUtil.isPut());
        pair.request().setMethod("DELETE");
        assertTrue(SpringUtil.isDelete());
    }
}
