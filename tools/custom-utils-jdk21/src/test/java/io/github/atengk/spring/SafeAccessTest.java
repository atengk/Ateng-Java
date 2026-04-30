package io.github.atengk.spring;

import io.github.atengk.utils.spring.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class SafeAccessTest {
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
    void shouldTryGetBeanAndProperty() {
        assertTrue(SpringUtil.tryGetBean(SpringUtilTestSupport.TestService.class).isPresent());
        assertTrue(SpringUtil.tryGetBean("testService", SpringUtilTestSupport.TestService.class).isPresent());
        assertTrue(SpringUtil.tryGetBean(Number.class).isEmpty());
        assertTrue(SpringUtil.tryGetBean("missing", String.class).isEmpty());
        assertEquals("spring-util-test", SpringUtil.tryGetProperty("spring.application.name").orElseThrow());
        assertTrue(SpringUtil.tryGetProperty("missing").isEmpty());
    }

    @Test
    void shouldTryPublishEvent() {
        assertTrue(SpringUtil.tryPublishEvent(new SpringUtilTestSupport.DemoEvent("ok")));
        assertFalse(SpringUtil.tryPublishEvent(null));
    }
}
