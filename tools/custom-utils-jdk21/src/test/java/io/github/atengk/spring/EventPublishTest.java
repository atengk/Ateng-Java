package io.github.atengk.spring;

import io.github.atengk.utils.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EventPublishTest {
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
    void shouldPublishEvents() {
        AtomicInteger counter = SpringUtil.getBean(AtomicInteger.class);
        SpringUtil.publishEvent(new SpringUtilTestSupport.DemoEvent("a"));
        assertEquals(1, counter.get());
        assertTrue(SpringUtil.publishEventSafely(new SpringUtilTestSupport.DemoEvent("b")));
        assertEquals(2, counter.get());
    }

    @Test
    void shouldFailSafelyWhenEventInvalid() {
        assertFalse(SpringUtil.publishEventSafely(null));
        assertThrows(NullPointerException.class, () -> SpringUtil.publishEvent((Object) null));
    }
}
