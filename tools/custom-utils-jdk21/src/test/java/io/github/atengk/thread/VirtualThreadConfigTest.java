package io.github.atengk.thread;

import io.github.atengk.utils.thread.VirtualThreadConfig;
import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadConfigTest {

    @AfterEach
    void reset() {
        VirtualThreadUtil.resetConfig();
    }

    @Test
    void configBuilderShouldCreateConfig() {
        VirtualThreadConfig config = VirtualThreadUtil.builder()
                .timeout(Duration.ofSeconds(5))
                .concurrency(3)
                .threadNamePrefix("vt-demo")
                .build();
        assertEquals(Duration.ofSeconds(5), config.getTimeout());
        assertEquals(3, config.getConcurrency());
        assertEquals("vt-demo", config.getThreadNamePrefix());
    }

    @Test
    void defaultConfigShouldBeChangeable() {
        VirtualThreadUtil.setDefaultTimeout(Duration.ofSeconds(2));
        VirtualThreadUtil.setDefaultConcurrency(2);
        VirtualThreadUtil.setDefaultThreadNamePrefix("vt-custom");
        assertEquals(Duration.ofSeconds(2), VirtualThreadUtil.defaultTimeout());
        assertEquals(2, VirtualThreadUtil.defaultConcurrency());
        assertEquals("vt-custom", VirtualThreadUtil.defaultPrefix());
    }

    @Test
    void defaultExecutorShouldUseDefaultPrefix() throws Exception {
        VirtualThreadUtil.setDefaultThreadNamePrefix("vt-default-test");
        try (ExecutorService executor = VirtualThreadUtil.defaultExecutor()) {
            assertTrue(executor.submit(() -> Thread.currentThread().getName().startsWith("vt-default-test")).get());
        }
    }

    @Test
    void configInvalidArgumentsShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> VirtualThreadUtil.builder().timeout(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> VirtualThreadUtil.builder().concurrency(0));
        assertThrows(IllegalArgumentException.class, () -> VirtualThreadUtil.builder().threadNamePrefix(" "));
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.setDefaultTimeout(null));
    }
}
