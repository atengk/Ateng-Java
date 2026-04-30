package io.github.atengk.spring;

import io.github.atengk.utils.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class ResourceAccessTest {
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
    void shouldReadResources() throws Exception {
        assertTrue(SpringUtil.resourceExists("classpath:demo.txt"));
        assertNotNull(SpringUtil.getResource("classpath:demo.txt"));
        assertTrue(SpringUtil.getResources("classpath*:demo.txt").length >= 1);
        assertEquals("hello spring util", SpringUtil.readResourceAsString("classpath:demo.txt").trim());
        assertTrue(SpringUtil.readResourceAsBytes("classpath:demo.txt").length > 0);
        try (InputStream inputStream = SpringUtil.getResourceAsStream("classpath:demo.txt")) {
            assertNotNull(inputStream);
        }
    }

    @Test
    void shouldRejectBlankLocation() {
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.getResource(" "));
    }
}
