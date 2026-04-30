package io.github.atengk.spring;

import io.github.atengk.utils.SpringUtil;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class LifecycleStatusTest {

    @Test
    void shouldCheckLifecycle() {
        AnnotationConfigApplicationContext context = SpringUtilTestSupport.createContext();
        try {
            assertTrue(SpringUtil.isActive());
            assertFalse(SpringUtil.isClosed());
            assertNotNull(SpringUtil.getStartupShutdownMonitor());
            SpringUtil.closeContext();
            assertTrue(SpringUtil.isClosed());
        } finally {
            SpringUtilTestSupport.clear(context);
        }
    }

    @Test
    void shouldReturnClosedWhenContextMissing() {
        SpringUtil.setApplicationContext(null);
        assertFalse(SpringUtil.isActive());
        assertTrue(SpringUtil.isClosed());
        assertFalse(SpringUtil.isRunning());
    }
}
