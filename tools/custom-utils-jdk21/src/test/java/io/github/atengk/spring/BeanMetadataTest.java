package io.github.atengk.spring;

import io.github.atengk.utils.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class BeanMetadataTest {
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
    void shouldInspectBeanMetadata() {
        assertTrue(SpringUtil.containsBean("testService"));
        assertTrue(SpringUtil.containsBean(SpringUtilTestSupport.TestService.class));
        assertTrue(SpringUtil.containsBeanDefinition("testService"));
        assertTrue(SpringUtil.isSingleton("testService"));
        assertTrue(SpringUtil.isPrototype("prototypeBean"));
        assertTrue(SpringUtil.isTypeMatch("testService", SpringUtilTestSupport.TestService.class));
        assertEquals(SpringUtilTestSupport.TestService.class, SpringUtil.getType("testService"));
        assertTrue(SpringUtil.getBeanNames().length > 0);
        assertTrue(SpringUtil.getBeanNamesForType(SpringUtilTestSupport.Strategy.class).length >= 3);
    }

    @Test
    void shouldRejectInvalidBeanMetadataArguments() {
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.containsBean(""));
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.getAliases(" "));
    }
}
