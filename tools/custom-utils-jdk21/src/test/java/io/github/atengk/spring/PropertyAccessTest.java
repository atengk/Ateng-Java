package io.github.atengk.spring;

import io.github.atengk.utils.spring.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PropertyAccessTest {
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = SpringUtilTestSupport.createContext();
    }

    @AfterEach
    void tearDown() {
        SpringUtilTestSupport.clear(context);
    }

    @Test
    void shouldReadProperties() {
        assertEquals("spring-util-test", SpringUtil.getProperty("spring.application.name"));
        assertEquals("fallback", SpringUtil.getProperty("missing", "fallback"));
        assertEquals(12, SpringUtil.getProperty("sample.int", Integer.class));
        assertEquals("spring-util-test", SpringUtil.getRequiredProperty("spring.application.name"));
        assertEquals(99L, SpringUtil.getPropertyAsLong("sample.long", 0L));
        assertEquals(Boolean.TRUE, SpringUtil.getPropertyAsBoolean("sample.bool", false));
        assertEquals(List.of("a", "b", "c"), SpringUtil.getPropertyAsList("sample.list"));
        assertTrue(SpringUtil.containsProperty("sample.bool"));
    }

    @Test
    void shouldHandleMissingAndInvalidProperties() {
        assertEquals(7, SpringUtil.getPropertyAsInt("missing.int", 7));
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.getProperty("invalid.int", Integer.class));
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.getRequiredProperty("not.exists", Integer.class));
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.getProperty(" "));
    }
}
