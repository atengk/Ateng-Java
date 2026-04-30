package io.github.atengk.spring;

import io.github.atengk.utils.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class CacheSupportTest {
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
    void shouldOperateCache() {
        assertNotNull(SpringUtil.getCacheManager());
        assertNotNull(SpringUtil.getCache("users"));
        SpringUtil.putCacheValue("users", "1", "Ateng");
        assertEquals("Ateng", SpringUtil.getCacheValue("users", "1"));
        SpringUtil.evictCacheValue("users", "1");
        assertNull(SpringUtil.getCacheValue("users", "1"));
        SpringUtil.putCacheValue("users", "2", "B");
        SpringUtil.clearCache("users");
        assertNull(SpringUtil.getCacheValue("users", "2"));
    }

    @Test
    void shouldRejectInvalidCacheOperations() {
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.getCache(""));
        assertThrows(NullPointerException.class, () -> SpringUtil.putCacheValue("users", null, "x"));
        assertThrows(Exception.class, () -> SpringUtil.clearCache("missing"));
    }
}
