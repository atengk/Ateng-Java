package io.github.atengk.spring;

import io.github.atengk.utils.spring.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class MessageSourceTest {
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
    void shouldReadMessages() {
        assertEquals("简单消息", SpringUtil.getMessage("simple", Locale.CHINA));
        assertEquals("你好 Ateng", SpringUtil.getMessage("hello", new Object[]{"Ateng"}, Locale.CHINA));
        assertEquals("默认", SpringUtil.getMessage("missing", "默认"));
        assertEquals("默认 A", SpringUtil.getMessage("missing.args", new Object[]{"A"}, "默认 A"));
    }

    @Test
    void shouldRejectEmptyCode() {
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.getMessage(""));
    }
}
