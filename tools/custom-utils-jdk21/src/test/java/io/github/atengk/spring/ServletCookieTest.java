package io.github.atengk.spring;

import io.github.atengk.utils.spring.SpringUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServletCookieTest {

    @AfterEach
    void tearDown() {
        ServletTestSupport.clear();
    }

    @Test
    void shouldReadAndWriteCookies() {
        ServletTestSupport.ServletPair pair = ServletTestSupport.bind();
        pair.request().setCookies(new Cookie("token", "abc"));

        assertEquals(1, SpringUtil.getCookies().length);
        assertEquals("abc", SpringUtil.getCookieValue("token"));
        assertNull(SpringUtil.getCookie("missing"));

        SpringUtil.addCookie("a", "1");
        SpringUtil.addCookie("b", "2", 60);
        SpringUtil.addCookie("c", "3", "/app", 60);
        SpringUtil.removeCookie("a");
        assertTrue(pair.response().getCookies().length >= 4);
    }

    @Test
    void shouldRejectInvalidCookieName() {
        ServletTestSupport.bind();
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.getCookie(""));
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.addCookie(" ", "x"));
    }
}
