package io.github.atengk.spring;

import io.github.atengk.utils.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationInfoTest {
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
    void shouldReadApplicationInfo() {
        assertEquals("spring-util-test", SpringUtil.getApplicationName());
        assertEquals(8080, SpringUtil.getServerPort());
        assertEquals("/demo", SpringUtil.getContextPath());
        assertNotNull(SpringUtil.getStartupDate());
        assertEquals("spring-util-id", SpringUtil.getApplicationId());
        assertEquals("2.0.0-build", SpringUtil.getApplicationVersion());
        assertNotNull(SpringUtil.getMainClass());
    }
}
