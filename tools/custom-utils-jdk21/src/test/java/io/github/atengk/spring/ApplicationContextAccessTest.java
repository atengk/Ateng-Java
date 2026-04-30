package io.github.atengk.spring;

import io.github.atengk.utils.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationContextAccessTest {
    private AnnotationConfigApplicationContext context;

    @AfterEach
    void tearDown() {
        SpringUtilTestSupport.clear(context);
    }

    @Test
    void shouldAccessApplicationContext() {
        context = SpringUtilTestSupport.createContext();
        assertSame(context, SpringUtil.getApplicationContext());
        assertSame(context, SpringUtil.requireApplicationContext());
        assertTrue(SpringUtil.isContextReady());
        assertNotNull(SpringUtil.getBeanFactory());
        assertNotNull(SpringUtil.getAutowireCapableBeanFactory());
        assertNotNull(SpringUtil.getEnvironment());
    }

    @Test
    void shouldThrowWhenContextMissing() {
        SpringUtil.setApplicationContext(null);
        assertNull(SpringUtil.getApplicationContext());
        assertFalse(SpringUtil.isContextReady());
        assertThrows(IllegalStateException.class, SpringUtil::requireApplicationContext);
    }
}
