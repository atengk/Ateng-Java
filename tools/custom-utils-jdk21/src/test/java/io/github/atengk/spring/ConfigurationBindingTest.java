package io.github.atengk.spring;

import io.github.atengk.utils.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationBindingTest {
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
    void shouldBindProperties() {
        SpringUtilTestSupport.DemoProperties properties = SpringUtil.bind("demo", SpringUtilTestSupport.DemoProperties.class);
        assertNotNull(properties);
        assertEquals("demo-name", properties.getName());
        assertEquals(9000, properties.getPort());

        SpringUtilTestSupport.DemoProperties fallback = new SpringUtilTestSupport.DemoProperties();
        assertSame(fallback, SpringUtil.bindOrDefault("missing.demo", SpringUtilTestSupport.DemoProperties.class, fallback));
        assertEquals(List.of("x", "y"), SpringUtil.bindList("demo.list", String.class));
        assertEquals(Map.of("a", 1, "b", 2), SpringUtil.bindMap("demo.map", String.class, Integer.class));
    }

    @Test
    void shouldRejectInvalidBindingArguments() {
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.bind("", SpringUtilTestSupport.DemoProperties.class));
        assertThrows(NullPointerException.class, () -> SpringUtil.bind("demo", null));
    }
}
