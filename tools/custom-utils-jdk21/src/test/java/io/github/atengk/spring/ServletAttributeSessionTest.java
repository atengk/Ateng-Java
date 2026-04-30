package io.github.atengk.spring;

import io.github.atengk.utils.spring.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServletAttributeSessionTest {

    @AfterEach
    void tearDown() {
        ServletTestSupport.clear();
    }

    @Test
    void shouldOperateRequestAttributes() {
        ServletTestSupport.bind();
        SpringUtil.setAttribute("userId", 100L);
        assertEquals(100L, SpringUtil.getAttribute("userId"));
        assertEquals("100", SpringUtil.getAttributeAsString("userId"));
        assertEquals(100L, SpringUtil.getAttributeAsLong("userId"));
        assertTrue(SpringUtil.getAttributeNames().contains("userId"));
        SpringUtil.removeAttribute("userId");
        assertNull(SpringUtil.getAttribute("userId"));
    }

    @Test
    void shouldOperateSessionAttributes() {
        ServletTestSupport.bind();
        assertFalse(SpringUtil.isSessionExists());
        SpringUtil.setSessionAttribute("name", "Ateng");
        assertTrue(SpringUtil.isSessionExists());
        assertEquals("Ateng", SpringUtil.getSessionAttribute("name"));
        assertNotNull(SpringUtil.getSessionId());
        SpringUtil.removeSessionAttribute("name");
        assertNull(SpringUtil.getSessionAttribute("name"));
        SpringUtil.invalidateSession();
    }

    @Test
    void shouldHandleAttributeEdges() {
        ServletTestSupport.bind();
        SpringUtil.setAttribute("bad", "x");
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.getAttributeAsLong("bad"));
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.setAttribute("", "x"));
    }
}
