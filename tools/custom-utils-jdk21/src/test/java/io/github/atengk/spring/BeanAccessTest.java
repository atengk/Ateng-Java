package io.github.atengk.spring;

import io.github.atengk.utils.spring.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BeanAccessTest {
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
    void shouldGetBeansByTypeAndName() {
        SpringUtilTestSupport.TestService service = SpringUtil.getBean(SpringUtilTestSupport.TestService.class);
        assertEquals("main", service.getName());
        assertEquals("hello", SpringUtil.getBean("namedText"));
        assertEquals("hello", SpringUtil.getBean("namedText", String.class));
        assertNotNull(SpringUtil.getBeanProvider(SpringUtilTestSupport.TestService.class).getIfAvailable());
    }

    @Test
    void shouldReturnNullOrCollections() {
        assertNull(SpringUtil.getBeanOrNull(Number.class));
        assertNull(SpringUtil.getBeanOrNull("notExists"));
        Map<String, SpringUtilTestSupport.Strategy> map = SpringUtil.getBeanMap(SpringUtilTestSupport.Strategy.class);
        List<SpringUtilTestSupport.Strategy> list = SpringUtil.getBeanList(SpringUtilTestSupport.Strategy.class);
        assertFalse(map.isEmpty());
        assertFalse(list.isEmpty());
    }

    @Test
    void shouldRejectInvalidArguments() {
        assertThrows(NullPointerException.class, () -> SpringUtil.getBean((Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.getBean(" "));
    }
}
