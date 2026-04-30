package io.github.atengk.spring;

import io.github.atengk.utils.spring.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServletRequestParameterTest {

    @AfterEach
    void tearDown() {
        ServletTestSupport.clear();
    }

    @Test
    void shouldReadParameters() {
        ServletTestSupport.ServletPair pair = ServletTestSupport.bind();
        pair.request().addParameter("name", "Ateng");
        pair.request().addParameter("age", "18");
        pair.request().addParameter("flag", "yes");
        pair.request().addParameter("ids", "1,2", "3");

        assertEquals("Ateng", SpringUtil.getParameter("name"));
        assertEquals("default", SpringUtil.getParameter("missing", "default"));
        assertTrue(SpringUtil.getParameterMap().containsKey("name"));
        assertEquals(List.of("1,2", "3"), SpringUtil.getParameterValues("ids"));
        assertEquals(18, SpringUtil.getParameterAsInt("age"));
        assertEquals(18L, SpringUtil.getParameterAsLong("age"));
        assertTrue(SpringUtil.getParameterAsBoolean("flag"));
        assertEquals(List.of("1", "2", "3"), SpringUtil.getParameterAsList("ids"));
    }

    @Test
    void shouldHandleParameterEdges() {
        ServletTestSupport.ServletPair pair = ServletTestSupport.bind();
        pair.request().addParameter("bad", "x");
        assertNull(SpringUtil.getParameter("missing"));
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.getParameter(""));
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.getParameterAsInt("bad"));
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.getParameterAsBoolean("bad"));
    }
}
