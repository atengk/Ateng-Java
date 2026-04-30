package io.github.atengk.spring;

import io.github.atengk.utils.spring.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrderedExtensionTest {
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
    void shouldLoadOrderedBeansAndPrimaryBean() {
        List<SpringUtilTestSupport.Strategy> beans = SpringUtil.getOrderedBeans(SpringUtilTestSupport.Strategy.class);
        assertEquals("high", beans.getFirst().name());
        assertEquals("primary", SpringUtil.getPrimaryBean(SpringUtilTestSupport.Strategy.class).name());
        assertNull(SpringUtil.getPrimaryBean(Number.class));
    }

    @Test
    void shouldLoadBeansByAnnotation() {
        Map<String, Object> beans = SpringUtil.getBeansWithAnnotation(SpringUtilTestSupport.DemoMarker.class);
        assertTrue(beans.containsKey("markedBean"));
        assertEquals(beans, SpringUtil.getBeanByAnnotation(SpringUtilTestSupport.DemoMarker.class));
        assertThrows(NullPointerException.class, () -> SpringUtil.getBeansWithAnnotation(null));
    }
}
