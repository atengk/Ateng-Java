package io.github.atengk.spring;

import io.github.atengk.utils.spring.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServletContextAccessTest {

    @AfterEach
    void tearDown() {
        ServletTestSupport.clear();
    }

    @Test
    void shouldAccessServletObjects() {
        ServletTestSupport.ServletPair pair = ServletTestSupport.bind();
        pair.request().setSession(pair.request().getSession(true));

        assertNotNull(SpringUtil.getRequestAttributes());
        assertNotNull(SpringUtil.getServletRequestAttributes());
        assertSame(pair.request(), SpringUtil.getRequest());
        assertSame(pair.response(), SpringUtil.getResponse());
        assertNotNull(SpringUtil.getSession());
        assertNotNull(SpringUtil.getSession(true));
        assertNotNull(SpringUtil.getServletContext());
    }

    @Test
    void shouldReturnNullWithoutRequest() {
        assertNull(SpringUtil.getRequest());
        assertNull(SpringUtil.getResponse());
        assertNull(SpringUtil.getSession());
        assertThrows(IllegalStateException.class, SpringUtil::requireRequest);
        assertThrows(IllegalStateException.class, SpringUtil::requireResponse);
    }
}
