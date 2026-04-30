package io.github.atengk.spring;

import io.github.atengk.utils.spring.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class ProfileEnvironmentTest {
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
    void shouldCheckProfiles() {
        assertTrue(SpringUtil.getActiveProfiles().contains("dev"));
        assertFalse(SpringUtil.getDefaultProfiles().isEmpty());
        assertTrue(SpringUtil.isProfileActive("dev"));
        assertTrue(SpringUtil.isAnyProfileActive("prod", "test"));
        assertTrue(SpringUtil.isAllProfileActive("dev", "test"));
        assertFalse(SpringUtil.isProd());
        assertTrue(SpringUtil.isDev());
        assertTrue(SpringUtil.isTest());
        assertFalse(SpringUtil.isLocal());
    }

    @Test
    void shouldRejectEmptyProfiles() {
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.isProfileActive(""));
        assertThrows(IllegalArgumentException.class, SpringUtil::isAnyProfileActive);
    }
}
