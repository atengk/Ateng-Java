package io.github.atengk.spring;

import io.github.atengk.utils.spring.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class BeanAutowireRegistrationTest {
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
    void shouldAutowireAndCreateBean() {
        SpringUtilTestSupport.NeedInject target = new SpringUtilTestSupport.NeedInject();
        assertNull(target.testService);
        SpringUtil.autowireBean(target);
        assertNotNull(target.testService);

        Object initialized = SpringUtil.initializeBean(target, "manualNeedInject");
        assertSame(target, initialized);
        assertNotNull(SpringUtil.createBean(SpringUtilTestSupport.NeedInject.class).testService);
        SpringUtil.destroyBean(target);
    }

    @Test
    void shouldRegisterSingleton() {
        SpringUtil.registerSingleton("manualText", "ok");
        assertEquals("ok", SpringUtil.getBean("manualText"));
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.registerSingleton(" ", "x"));
    }

    @Test
    void shouldRemoveBeanDefinition() {
        assertTrue(SpringUtil.containsBeanDefinition("namedText"));
        SpringUtil.removeBeanDefinition("namedText");
        assertFalse(SpringUtil.containsBeanDefinition("namedText"));
    }
}
